package mz.multicore.erp.modules.pricing.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.comercial.service.WalkInClientProvider;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.pricing.dto.*;
import mz.multicore.erp.modules.pricing.model.ClientPriceList;
import mz.multicore.erp.modules.pricing.model.PriceList;
import mz.multicore.erp.modules.pricing.model.PriceListItem;
import mz.multicore.erp.modules.pricing.repository.ClientPriceListRepository;
import mz.multicore.erp.modules.pricing.repository.PriceListItemRepository;
import mz.multicore.erp.modules.pricing.repository.PriceListRepository;
import mz.multicore.erp.modules.units.service.ProductUnitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Manages price lists, their prices and their assignment to clients (SA-C03). */
@Service
public class PriceListService {

    private static final Pattern CODE = Pattern.compile("[A-Z0-9_-]{1,30}");

    private final PriceListRepository priceListRepository;
    private final PriceListItemRepository itemRepository;
    private final ClientPriceListRepository assignmentRepository;
    private final CompanyLookup companyLookup;
    private final ProductLookup productLookup;
    private final ClientLookup clientLookup;
    private final AuditLogService auditLogService;

    public PriceListService(PriceListRepository priceListRepository, PriceListItemRepository itemRepository,
                            ClientPriceListRepository assignmentRepository, CompanyLookup companyLookup,
                            ProductLookup productLookup, ClientLookup clientLookup,
                            AuditLogService auditLogService) {
        this.priceListRepository = priceListRepository;
        this.itemRepository = itemRepository;
        this.assignmentRepository = assignmentRepository;
        this.companyLookup = companyLookup;
        this.productLookup = productLookup;
        this.clientLookup = clientLookup;
        this.auditLogService = auditLogService;
    }

    // ---- Lists ---------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PriceListDTO> list(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return priceListRepository.findByCompanyIdOrderByName(companyId).stream().map(this::toDto).toList();
    }

    @Transactional
    public PriceListDTO save(SavePriceListRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        String code = normalizeCode(request.code());
        if (request.validFrom() != null && request.validTo() != null && request.validTo().isBefore(request.validFrom())) {
            throw new BusinessRuleException("A data de fim da tabela não pode ser anterior à data de início.");
        }
        priceListRepository.findByCompanyIdAndCode(request.companyId(), code)
                .filter(other -> !other.getId().equals(request.id()))
                .ifPresent(other -> {
                    throw new BusinessRuleException("Já existe uma tabela com o código " + code + ".");
                });

        PriceList list;
        boolean created = request.id() == null;
        if (created) {
            Company company = companyLookup.require(request.companyId());
            list = new PriceList();
            list.setCompany(company);
        } else {
            list = requireList(request.companyId(), request.id());
        }
        list.setCode(code);
        list.setName(request.name().trim());
        list.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        list.setValidFrom(request.validFrom());
        list.setValidTo(request.validTo());
        list.setActive(request.active());
        PriceList saved = priceListRepository.save(list);
        auditLogService.logCurrent(created ? "PRICE_LIST_CREATE" : "PRICE_LIST_UPDATE",
                "Tabela de preço " + saved.getCode() + " (" + saved.getName() + ")" + (saved.isActive() ? "" : " inactiva"));
        return toDto(saved);
    }

    @Transactional
    public void delete(Long companyId, Long priceListId) {
        PriceList list = requireList(companyId, priceListId);
        long clients = assignmentRepository.countByPriceListId(list.getId());
        if (clients > 0) {
            throw new BusinessRuleException("A tabela " + list.getName() + " está atribuída a " + clients
                    + (clients == 1 ? " cliente" : " clientes") + ". Retire-a desses clientes ou desactive-a.");
        }
        itemRepository.deleteAll(itemRepository.findByPriceListIdWithProduct(list.getId()));
        priceListRepository.delete(list);
        auditLogService.logCurrent("PRICE_LIST_DELETE", "Tabela de preço " + list.getCode() + " eliminada");
    }

    // ---- Prices --------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PriceListItemDTO> items(Long companyId, Long priceListId) {
        PriceList list = requireList(companyId, priceListId);
        return itemRepository.findByPriceListIdWithProduct(list.getId()).stream().map(PriceListService::toDto).toList();
    }

    @Transactional
    public PriceListItemDTO saveItem(SavePriceListItemRequest request) {
        PriceList list = requireList(request.companyId(), request.priceListId());
        Product product = productLookup.findForCompany(request.productId(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Produto não encontrado nesta empresa."));
        BigDecimal price = request.price();
        if (price == null || price.signum() <= 0) {
            throw new BusinessRuleException("O preço da tabela tem de ser maior que zero.");
        }
        price = price.setScale(2, RoundingMode.HALF_UP);
        BigDecimal minQuantity = request.minQuantity() == null ? BigDecimal.ZERO : request.minQuantity();
        if (minQuantity.signum() < 0) {
            throw new BusinessRuleException("A quantidade mínima não pode ser negativa.");
        }
        minQuantity = minQuantity.setScale(3, RoundingMode.HALF_UP);

        PriceListItem item = itemRepository
                .findByPriceListIdAndProductIdAndMinQuantity(list.getId(), product.getId(), minQuantity)
                .orElseGet(PriceListItem::new);
        BigDecimal previous = item.getPrice();
        item.setPriceList(list);
        item.setProduct(product);
        item.setMinQuantity(minQuantity);
        item.setPrice(price);
        PriceListItem saved = itemRepository.save(item);
        auditLogService.logCurrent("PRICE_LIST_ITEM_SAVE", "Tabela " + list.getCode() + ", " + product.getSku()
                + " a partir de " + DisplayNumbers.qty(minQuantity) + ": "
                + (previous == null ? "novo" : DisplayNumbers.money(previous)) + " -> " + DisplayNumbers.money(price));
        return toDto(saved);
    }

    @Transactional
    public void deleteItem(Long companyId, Long itemId) {
        PriceListItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessRuleException("Preço da tabela não encontrado."));
        PriceList list = requireList(companyId, item.getPriceList().getId());
        itemRepository.delete(item);
        auditLogService.logCurrent("PRICE_LIST_ITEM_DELETE", "Tabela " + list.getCode() + ": preço de "
                + item.getProduct().getSku() + " removido");
    }

    // ---- Clients -------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ClientPriceListDTO> clients(Long companyId, Long priceListId) {
        PriceList list = requireList(companyId, priceListId);
        return assignmentRepository.findByPriceListIdWithClient(list.getId()).stream()
                .map(a -> new ClientPriceListDTO(a.getClient().getId(), a.getClient().getName(), list.getId(), list.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClientPriceListDTO clientAssignment(Long companyId, Long clientId) {
        Client client = requireClient(companyId, clientId);
        return assignmentRepository.findAssignment(companyId, client.getId())
                .map(a -> new ClientPriceListDTO(client.getId(), client.getName(), a.getPriceList().getId(), a.getPriceList().getName()))
                .orElse(new ClientPriceListDTO(client.getId(), client.getName(), null, null));
    }

    @Transactional
    public ClientPriceListDTO assign(AssignClientPriceListRequest request) {
        Client client = requireClient(request.companyId(), request.clientId());
        var current = assignmentRepository.findAssignment(request.companyId(), client.getId());
        String before = current.map(a -> a.getPriceList().getName()).orElse("preço normal");
        if (request.priceListId() == null) {
            current.ifPresent(assignmentRepository::delete);
            auditLogService.logCurrent("CLIENT_PRICE_LIST_ASSIGN", "Cliente " + client.getName() + ": " + before + " -> preço normal");
            return new ClientPriceListDTO(client.getId(), client.getName(), null, null);
        }
        if (WalkInClientProvider.WALK_IN_TAX_ID.equals(client.getTaxId())) {
            throw new BusinessRuleException("O " + WalkInClientProvider.WALK_IN_NAME + " usa sempre o preço normal: "
                    + "uma tabela aqui mudaria o preço de todas as vendas ao balcão. Altere o preço no produto.");
        }
        PriceList list = requireList(request.companyId(), request.priceListId());
        ClientPriceList assignment = current.orElseGet(ClientPriceList::new);
        if (assignment.getId() == null) {
            assignment.setCompany(list.getCompany());
            assignment.setClient(client);
        }
        assignment.setPriceList(list);
        assignmentRepository.save(assignment);
        auditLogService.logCurrent("CLIENT_PRICE_LIST_ASSIGN", "Cliente " + client.getName() + ": " + before + " -> " + list.getName());
        return new ClientPriceListDTO(client.getId(), client.getName(), list.getId(), list.getName());
    }

    // ---- Support -------------------------------------------------------------------------------

    private PriceList requireList(Long companyId, Long priceListId) {
        CurrentUserContext.requireCompany(companyId);
        if (priceListId == null) throw new BusinessRuleException("Tabela de preço é obrigatória.");
        return priceListRepository.findByIdAndCompanyId(priceListId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Tabela de preço não encontrada nesta empresa."));
    }

    private Client requireClient(Long companyId, Long clientId) {
        CurrentUserContext.requireCompany(companyId);
        return clientLookup.requireForCompany(clientId, companyId);
    }

    static String normalizeCode(String raw) {
        if (raw == null || raw.isBlank()) throw new BusinessRuleException("O código da tabela é obrigatório.");
        String code = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (!CODE.matcher(code).matches()) {
            throw new BusinessRuleException("O código da tabela só pode ter letras, números, _ e - (até 30).");
        }
        return code;
    }

    private PriceListDTO toDto(PriceList list) {
        return new PriceListDTO(list.getId(), list.getCompany().getId(), list.getCode(), list.getName(),
                list.getDescription(), list.getValidFrom(), list.getValidTo(), list.isActive(),
                itemRepository.countByPriceListId(list.getId()), assignmentRepository.countByPriceListId(list.getId()));
    }

    private static PriceListItemDTO toDto(PriceListItem item) {
        Product p = item.getProduct();
        return new PriceListItemDTO(item.getId(), item.getPriceList().getId(), p.getId(), p.getSku(), p.getName(),
                ProductUnitService.baseUnitCodeOf(p), item.getMinQuantity(), item.getPrice(), p.getUnitPrice());
    }
}
