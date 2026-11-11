package mz.multicore.erp.modules.returnables.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.comercial.service.WalkInClientProvider;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.inventory.service.StockMovementRegistrationService;
import mz.multicore.erp.modules.inventory.service.TransitWarehouseProvider;
import mz.multicore.erp.modules.returnables.dto.*;
import mz.multicore.erp.modules.returnables.model.ReturnableItem;
import mz.multicore.erp.modules.returnables.model.ReturnableMovement;
import mz.multicore.erp.modules.returnables.model.ReturnableMovementType;
import mz.multicore.erp.modules.returnables.repository.ReturnableItemRepository;
import mz.multicore.erp.modules.returnables.repository.ReturnableMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Returnables account (SA-C12, {@code docs/VASILHAME_RETORNAVEIS_SPEC.md}): crates, bottles, gas bottles and
 * pallets lent to clients. A delivery moves them to the "Vasilhame em clientes" warehouse, because they still
 * belong to the company; a return brings them back. What will not come back is invoiced with an ordinary
 * invoice (FT) at the product price, which closes that quantity. No deposit is taken in cash.
 */
@Service
public class ReturnableService {

    static final String CLIENTS_WAREHOUSE = "Vasilhame em clientes";
    private static final String MOVEMENT_TYPE = "TRANSFER";

    private final ReturnableItemRepository itemRepository;
    private final ReturnableMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final CompanyLookup companyLookup;
    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final InvoiceRepository invoiceRepository;
    private final StockMovementRegistrationService stockMovements;
    private final TransitWarehouseProvider transitWarehouses;
    private final ComercialService comercialService;
    private final AuditLogService auditLogService;

    public ReturnableService(ReturnableItemRepository itemRepository, ReturnableMovementRepository movementRepository,
                             ProductRepository productRepository, ClientRepository clientRepository,
                             CompanyLookup companyLookup, WarehouseRepository warehouseRepository,
                             StockRepository stockRepository, InvoiceRepository invoiceRepository,
                             StockMovementRegistrationService stockMovements, TransitWarehouseProvider transitWarehouses,
                             ComercialService comercialService, AuditLogService auditLogService) {
        this.itemRepository = itemRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.clientRepository = clientRepository;
        this.companyLookup = companyLookup;
        this.warehouseRepository = warehouseRepository;
        this.stockRepository = stockRepository;
        this.invoiceRepository = invoiceRepository;
        this.stockMovements = stockMovements;
        this.transitWarehouses = transitWarehouses;
        this.comercialService = comercialService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ReturnableItemDTO> items(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        Map<Long, BigDecimal> withClients = new HashMap<>();
        balances(companyId, null).forEach(b -> withClients.merge(b.itemId(), b.balance(), BigDecimal::add));
        return itemRepository.findByCompany(companyId).stream().map(i -> new ReturnableItemDTO(i.getId(), i.getProduct().getId(),
                i.getProduct().getName(), i.getProduct().getSku(), i.getProduct().getUnitPrice(), i.isActive(),
                withClients.getOrDefault(i.getId(), BigDecimal.ZERO))).toList();
    }

    @Transactional
    public ReturnableItemDTO saveItem(SaveReturnableItemRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("definir vasilhame");
        Product product = productRepository.findByIdAndCompaniesId(request.productId(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Artigo não encontrado nesta empresa."));
        if (!product.isStockTracked()) {
            throw new BusinessRuleException("O vasilhame tem de controlar stock: '" + product.getName() + "' não controla.");
        }
        ReturnableItem item = itemRepository.findByCompanyIdAndProductId(request.companyId(), product.getId()).orElseGet(() -> {
            ReturnableItem created = new ReturnableItem();
            created.setCompany(companyLookup.require(request.companyId()));
            created.setProduct(product);
            return created;
        });
        item.setActive(request.active());
        item = itemRepository.save(item);
        auditLogService.logCurrent("RETURNABLE_ITEM_SAVE", "Vasilhame: " + product.getName() + (item.isActive() ? " activo." : " inactivo."));
        return new ReturnableItemDTO(item.getId(), product.getId(), product.getName(), product.getSku(), product.getUnitPrice(),
                item.isActive(), BigDecimal.ZERO);
    }

    /** Open balances, one per client and returnable; {@code clientId} narrows to one client. */
    @Transactional(readOnly = true)
    public List<ReturnableBalanceDTO> balances(Long companyId, Long clientId) {
        CurrentUserContext.requireCompany(companyId);
        Map<String, List<ReturnableMovement>> accounts = new LinkedHashMap<>();
        for (ReturnableMovement m : movementRepository.findByCompany(companyId)) {
            if (clientId != null && !clientId.equals(m.getClient().getId())) continue;
            accounts.computeIfAbsent(m.getClient().getId() + ":" + m.getItem().getId(), k -> new ArrayList<>()).add(m);
        }
        LocalDate today = LocalDate.now();
        List<ReturnableBalanceDTO> result = new ArrayList<>();
        for (List<ReturnableMovement> account : accounts.values()) {
            List<ReturnableBalanceRules.Entry> entries = account.stream()
                    .map(m -> new ReturnableBalanceRules.Entry(m.getCreatedAt().toLocalDate(), m.getType(), m.getQuantity())).toList();
            BigDecimal balance = ReturnableBalanceRules.balance(entries);
            if (balance.signum() <= 0) continue;
            ReturnableMovement last = account.get(account.size() - 1);
            Product product = last.getItem().getProduct();
            LocalDate oldest = ReturnableBalanceRules.oldestOpen(entries);
            BigDecimal price = product.getUnitPrice() == null ? BigDecimal.ZERO : product.getUnitPrice();
            result.add(new ReturnableBalanceDTO(last.getClient().getId(), last.getClient().getName(), last.getItem().getId(),
                    product.getName(), balance, price.multiply(balance).setScale(2, RoundingMode.HALF_UP),
                    last.getCreatedAt().toLocalDate(), oldest == null ? 0 : ChronoUnit.DAYS.between(oldest, today)));
        }
        result.sort(Comparator.comparingLong(ReturnableBalanceDTO::daysOut).reversed().thenComparing(ReturnableBalanceDTO::clientName));
        return result;
    }

    @Transactional(readOnly = true)
    public List<ReturnableMovementDTO> movements(Long companyId, int days) {
        CurrentUserContext.requireCompany(companyId);
        return movementRepository.findRecent(companyId, LocalDate.now().minusDays(Math.max(1, Math.min(days, 366))).atStartOfDay())
                .stream().map(ReturnableService::toDto).toList();
    }

    @Transactional
    public ReturnableMovementDTO record(RecordReturnableRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireSeller("registar vasilhame");
        var existing = movementRepository.findByCompanyIdAndRequestKey(request.companyId(), request.requestKey());
        if (existing.isPresent()) return toDto(existing.get());
        if (request.type() == ReturnableMovementType.CHARGED) {
            throw new BusinessRuleException("Para cobrar vasilhame não devolvido use \"Cobrar não devolvidos\".");
        }
        Client client = requireClient(request.companyId(), request.clientId());
        ReturnableItem item = requireItem(request.companyId(), request.itemId());
        Warehouse shop = warehouseRepository.findById(request.warehouseId())
                .filter(w -> w.getCompany() != null && request.companyId().equals(w.getCompany().getId()))
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado nesta empresa."));
        Warehouse atClients = transitWarehouses.named(shop.getCompany(), CLIENTS_WAREHOUSE, "Grades, garrafas e paletes nos clientes");
        if (atClients.getId().equals(shop.getId())) throw new BusinessRuleException("Escolha o armazém da loja.");
        Product product = item.getProduct();
        String description = (request.type() == ReturnableMovementType.OUT ? "Vasilhame entregue a " : "Vasilhame devolvido por ")
                + client.getName() + (request.reference() == null || request.reference().isBlank() ? "" : " (" + request.reference().trim() + ")");
        if (request.type() == ReturnableMovementType.OUT) {
            BigDecimal stock = stockOf(product.getId(), shop.getId());
            if (stock.compareTo(request.quantity()) < 0) {
                throw new BusinessRuleException("Só há " + DisplayNumbers.qty(stock) + " de " + product.getName() + " em " + shop.getName() + ".");
            }
            stockMovements.registerMovement(product, shop, request.quantity().negate(), MOVEMENT_TYPE, null, null, description, null);
            stockMovements.registerMovement(product, atClients, request.quantity(), MOVEMENT_TYPE, null, null, description, null);
        } else {
            BigDecimal balance = balanceOf(request.companyId(), client.getId(), item.getId());
            if (request.quantity().compareTo(balance) > 0) {
                throw new BusinessRuleException(client.getName() + " só tem " + DisplayNumbers.qty(balance) + " de " + product.getName()
                        + " por devolver.");
            }
            stockMovements.registerMovement(product, atClients, request.quantity().negate(), MOVEMENT_TYPE, null, null, description, null);
            stockMovements.registerMovement(product, shop, request.quantity(), MOVEMENT_TYPE, null, null, description, null);
        }
        ReturnableMovement movement = save(request.companyId(), client, item, request.type(), request.quantity(), request.reference(),
                request.notes(), request.requestKey(), null);
        auditLogService.logCurrent("RETURNABLE_" + request.type().name(), description + ": " + DisplayNumbers.qty(request.quantity())
                + " " + product.getName() + ".");
        return toDto(movement);
    }

    @Transactional
    public ReturnableMovementDTO charge(ChargeReturnablesRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("cobrar vasilhame não devolvido");
        var existing = movementRepository.findByCompanyIdAndRequestKey(request.companyId(), request.requestKey());
        if (existing.isPresent()) return toDto(existing.get());
        Client client = requireClient(request.companyId(), request.clientId());
        ReturnableItem item = requireItem(request.companyId(), request.itemId());
        BigDecimal balance = balanceOf(request.companyId(), client.getId(), item.getId());
        if (request.quantity().compareTo(balance) > 0) {
            throw new BusinessRuleException(client.getName() + " só tem " + DisplayNumbers.qty(balance) + " de "
                    + item.getProduct().getName() + " em aberto.");
        }
        Warehouse atClients = transitWarehouses.named(item.getCompany(), CLIENTS_WAREHOUSE, "Grades, garrafas e paletes nos clientes");
        Product product = item.getProduct();
        InvoiceDTO invoice = comercialService.createInvoice(new CreateInvoiceRequest(client.getId(), request.companyId(), atClients.getId(),
                List.of(new CreateInvoiceLineRequest(product.getId(), request.quantity(), product.effectiveTaxRate(), BigDecimal.ZERO, null, null))));
        ReturnableMovement movement = save(request.companyId(), client, item, ReturnableMovementType.CHARGED, request.quantity(),
                invoice.invoiceNumber(), request.notes(), request.requestKey(), invoiceRepository.findById(invoice.id()).orElseThrow());
        auditLogService.logCurrent("RETURNABLE_CHARGED", DisplayNumbers.qty(request.quantity()) + " " + product.getName()
                + " não devolvido(s) por " + client.getName() + ": fatura " + invoice.invoiceNumber() + ".");
        return toDto(movement);
    }

    private ReturnableMovement save(Long companyId, Client client, ReturnableItem item, ReturnableMovementType type, BigDecimal quantity,
                                    String reference, String notes, String requestKey, mz.multicore.erp.modules.comercial.model.Invoice invoice) {
        ReturnableMovement m = new ReturnableMovement();
        m.setCompany(item.getCompany());
        m.setClient(client);
        m.setItem(item);
        m.setType(type);
        m.setQuantity(quantity);
        m.setReference(reference == null || reference.isBlank() ? null : reference.trim());
        m.setNotes(notes == null || notes.isBlank() ? null : notes.trim());
        m.setRequestKey(requestKey);
        m.setInvoice(invoice);
        m.setCreatedBy(CurrentUserContext.getUsername());
        return movementRepository.save(m);
    }

    private BigDecimal balanceOf(Long companyId, Long clientId, Long itemId) {
        return ReturnableBalanceRules.balance(movementRepository.findAccount(companyId, clientId, itemId).stream()
                .map(m -> new ReturnableBalanceRules.Entry(null, m.getType(), m.getQuantity())).toList());
    }

    private Client requireClient(Long companyId, Long clientId) {
        Client client = clientRepository.findByIdAndCompaniesId(clientId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado nesta empresa."));
        if (WalkInClientProvider.WALK_IN_TAX_ID.equals(client.getTaxId())) {
            throw new BusinessRuleException("O vasilhame fica em nome de um cliente identificado, não do Consumidor Final.");
        }
        return client;
    }

    private ReturnableItem requireItem(Long companyId, Long itemId) {
        ReturnableItem item = itemRepository.findByIdAndCompanyId(itemId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Vasilhame não encontrado."));
        if (!item.isActive()) throw new BusinessRuleException("O vasilhame '" + item.getProduct().getName() + "' está inactivo.");
        return item;
    }

    private BigDecimal stockOf(Long productId, Long warehouseId) {
        return stockRepository.findByProductIdAndWarehouseId(productId, warehouseId).map(Stock::getQuantity).orElse(BigDecimal.ZERO);
    }

    static ReturnableMovementDTO toDto(ReturnableMovement m) {
        return new ReturnableMovementDTO(m.getId(), m.getCreatedAt(), m.getClient().getId(), m.getClient().getName(), m.getItem().getId(),
                m.getItem().getProduct().getName(), m.getType(), m.getQuantity(), m.getReference(),
                m.getInvoice() == null ? null : m.getInvoice().getInvoiceNumber(), m.getNotes(), m.getCreatedBy());
    }
}
