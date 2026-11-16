package mz.multicore.erp.modules.pickup.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.model.NoteStatus;
import mz.multicore.erp.modules.comercial.model.ProductSaleType;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.pickup.dto.*;
import mz.multicore.erp.modules.pickup.model.*;
import mz.multicore.erp.modules.pickup.repository.MaterialPickupRepository;
import mz.multicore.erp.modules.pickup.repository.PickupAccountRepository;
import mz.multicore.erp.modules.pickup.repository.PickupCreditQueryRepository;
import mz.multicore.erp.modules.units.service.ProductUnitService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Paid material collected over time (SA-C07, {@code docs/MATERIAL_POR_LEVANTAR_SPEC.md}).
 *
 * <p>Invariants: the invoice already removed the material from the sellable stock, so pickups never move
 * stock; a slip can only hand over what is invoiced minus approved credit notes minus earlier slips; the
 * account row is locked while a slip is issued; cancelling a slip returns its quantities to the balance.
 */
@Service
public class PickupService {

    private static final int ELIGIBLE_LIMIT = 50;

    private final PickupAccountRepository accountRepository;
    private final MaterialPickupRepository pickupRepository;
    private final PickupCreditQueryRepository creditRepository;
    private final InvoiceLookup invoiceLookup;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;

    public PickupService(PickupAccountRepository accountRepository, MaterialPickupRepository pickupRepository,
                         PickupCreditQueryRepository creditRepository, InvoiceLookup invoiceLookup,
                         DocumentNumberService documentNumberService, AuditLogService auditLogService) {
        this.accountRepository = accountRepository;
        this.pickupRepository = pickupRepository;
        this.creditRepository = creditRepository;
        this.invoiceLookup = invoiceLookup;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }

    // ---- Queries -------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PickupAccountSummaryDTO> list(Long companyId, String status) {
        CurrentUserContext.requireCompany(companyId);
        List<PickupAccount> accounts = status == null || status.isBlank()
                ? accountRepository.findByCompany(companyId)
                : accountRepository.findByCompanyAndStatus(companyId, parseStatus(status));
        return accounts.stream().map(a -> summary(a, balances(a))).toList();
    }

    @Transactional(readOnly = true)
    public PickupAccountDTO get(Long companyId, Long accountId) {
        return detail(requireAccount(companyId, accountId));
    }

    @Transactional(readOnly = true)
    public List<EligibleInvoiceDTO> eligibleInvoices(Long companyId, String search) {
        CurrentUserContext.requireCompany(companyId);
        String term = search == null || search.isBlank() ? "" : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return accountRepository.findEligibleInvoices(companyId, realisedStatuses(), term, PageRequest.of(0, ELIGIBLE_LIMIT))
                .stream()
                .map(i -> new EligibleInvoiceDTO(i.getId(), i.getInvoiceNumber(), i.issueDate(), clientName(i), i.getTotalAmount()))
                .toList();
    }

    /** Sold material still physically in the yard, per product and warehouse, over all open accounts. */
    @Transactional(readOnly = true)
    public List<PendingPickupStockDTO> pendingStock(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        Map<String, BigDecimal> quantities = new LinkedHashMap<>();
        Map<String, Set<Long>> accounts = new HashMap<>();
        Map<String, InvoiceLine> samples = new HashMap<>();
        for (PickupAccount account : accountRepository.findByCompanyAndStatus(companyId, PickupAccountStatus.OPEN)) {
            if (!account.getInvoice().getStatus().isRealisedSale()) continue;
            for (BalanceLine line : balances(account)) {
                if (line.balance().signum() <= 0) continue;
                InvoiceLine il = line.invoiceLine();
                String key = il.getProduct().getId() + "@" + account.getInvoice().getWarehouse().getId();
                quantities.merge(key, line.balance(), BigDecimal::add);
                accounts.computeIfAbsent(key, k -> new HashSet<>()).add(account.getId());
                samples.putIfAbsent(key, il);
            }
        }
        List<PendingPickupStockDTO> result = new ArrayList<>();
        quantities.forEach((key, qty) -> {
            InvoiceLine il = samples.get(key);
            result.add(new PendingPickupStockDTO(il.getProduct().getId(), il.getProduct().getSku(), il.getProduct().getName(),
                    ProductUnitService.baseUnitCodeOf(il.getProduct()), il.getInvoice().getWarehouse().getName(),
                    qty, accounts.get(key).size()));
        });
        result.sort(Comparator.comparing(PendingPickupStockDTO::productName));
        return result;
    }

    /** Sold material of one product still on the floor of one warehouse (used by bulk measurement, SA-C17). */
    @Transactional(readOnly = true)
    public BigDecimal pendingQuantity(Long companyId, Long productId, Long warehouseId) {
        BigDecimal total = BigDecimal.ZERO;
        for (PickupAccount account : accountRepository.findByCompanyAndStatus(companyId, PickupAccountStatus.OPEN)) {
            Invoice invoice = account.getInvoice();
            if (!invoice.getStatus().isRealisedSale() || invoice.getWarehouse() == null
                    || !invoice.getWarehouse().getId().equals(warehouseId)) continue;
            for (BalanceLine line : balances(account)) {
                if (line.invoiceLine().getProduct().getId().equals(productId)) total = total.add(line.balance());
            }
        }
        return total;
    }

    // ---- Commands ------------------------------------------------------------------------------

    @Transactional
    public PickupAccountDTO open(OpenPickupAccountRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireSeller("registar material por levantar");
        Invoice invoice = invoiceLookup.find(request.invoiceId())
                .filter(i -> i.getCompany().getId().equals(request.companyId()))
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada nesta empresa."));
        if (!invoice.getStatus().isRealisedSale()) {
            throw new BusinessRuleException("Só uma fatura emitida (paga ou a crédito) pode ficar com material por levantar.");
        }
        if (invoice.getDeliveryGuideId() != null) {
            throw new BusinessRuleException("A fatura " + invoice.getInvoiceNumber() + " já saiu com guia de remessa.");
        }
        if (accountRepository.existsByInvoiceId(invoice.getId())) {
            throw new BusinessRuleException("A fatura " + invoice.getInvoiceNumber() + " já tem material por levantar registado.");
        }
        if (request.deadline() != null && request.deadline().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("O prazo de levantamento não pode ser anterior a hoje.");
        }
        if (pickupLines(invoice).isEmpty()) {
            throw new BusinessRuleException("A fatura não tem artigos físicos para levantar (só serviços).");
        }
        PickupAccount account = new PickupAccount();
        account.setCompany(invoice.getCompany());
        account.setInvoice(invoice);
        account.setDeadline(request.deadline());
        account.setNotes(blankToNull(request.notes()));
        account.setStatus(PickupAccountStatus.OPEN);
        PickupAccount saved = accountRepository.save(account);
        auditLogService.logCurrent("PICKUP_ACCOUNT_OPEN", "Material por levantar da fatura " + invoice.getInvoiceNumber()
                + (request.deadline() == null ? "" : ", prazo " + request.deadline()));
        return detail(saved);
    }

    @Transactional
    public MaterialPickupDTO issue(IssuePickupRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireSeller("entregar material por levantar");
        String requestKey = blankToNull(request.requestKey());
        if (requestKey != null) {
            Optional<MaterialPickup> previous = pickupRepository.findByCompanyIdAndRequestKey(request.companyId(), requestKey);
            if (previous.isPresent()) return toDto(previous.get());
        }
        PickupAccount account = accountRepository.lockByIdAndCompanyId(request.accountId(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Conta de levantamento não encontrada nesta empresa."));
        Invoice invoice = account.getInvoice();
        if (!invoice.getStatus().isRealisedSale()) {
            throw new BusinessRuleException("A fatura " + invoice.getInvoiceNumber() + " foi anulada: não há material a entregar.");
        }
        if (account.getStatus() != PickupAccountStatus.OPEN) {
            throw new BusinessRuleException("Todo o material da fatura " + invoice.getInvoiceNumber() + " já foi levantado.");
        }
        if (account.getDeadline() != null && account.getDeadline().isBefore(LocalDate.now())) {
            PermissionGuard.requireManagerOrAdmin("entregar material depois do prazo de levantamento");
        }

        List<BalanceLine> balances = balances(account);
        Map<Long, BigDecimal> available = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        Map<Long, InvoiceLine> byId = new HashMap<>();
        for (BalanceLine line : balances) {
            available.put(line.invoiceLine().getId(), line.balance());
            names.put(line.invoiceLine().getId(), line.invoiceLine().getProduct().getName());
            byId.put(line.invoiceLine().getId(), line.invoiceLine());
        }
        List<PickupBalanceRules.Request> requests = request.lines().stream()
                .map(l -> new PickupBalanceRules.Request(l.invoiceLineId(), l.quantity())).toList();
        PickupBalanceRules.validate(requests, available, names);

        MaterialPickup pickup = new MaterialPickup();
        pickup.setCompany(account.getCompany());
        pickup.setAccount(account);
        pickup.setNumber(documentNumberService.next(DocumentSeries.MATERIAL_PICKUP));
        pickup.setPickedAt(LocalDateTime.now());
        pickup.setCollectorName(request.collectorName().trim());
        pickup.setCollectorDocument(blankToNull(request.collectorDocument()));
        pickup.setVehiclePlate(blankToNull(request.vehiclePlate()) == null ? null
                : request.vehiclePlate().trim().toUpperCase(Locale.ROOT));
        pickup.setNotes(blankToNull(request.notes()));
        pickup.setRequestKey(requestKey);
        pickup.setStatus(MaterialPickupStatus.ISSUED);
        String operator = CurrentUserContext.getUsername();
        if (operator != null) pickup.setCreatedBy(operator);
        for (PickupBalanceRules.Request line : requests) {
            if (line.quantity() == null || line.quantity().signum() == 0) continue;
            MaterialPickupLine pickupLine = new MaterialPickupLine();
            pickupLine.setInvoiceLine(byId.get(line.invoiceLineId()));
            pickupLine.setQuantity(line.quantity());
            pickup.addLine(pickupLine);
        }
        MaterialPickup saved = pickupRepository.saveAndFlush(pickup);

        if (PickupBalanceRules.allCollected(balances(account).stream().map(BalanceLine::balance).toList())) {
            account.setStatus(PickupAccountStatus.COMPLETED);
            accountRepository.save(account);
        }
        auditLogService.logCurrent("MATERIAL_PICKUP_ISSUE", "Levantamento " + saved.getNumber() + " da fatura "
                + invoice.getInvoiceNumber() + " por " + saved.getCollectorName()
                + (saved.getVehiclePlate() == null ? "" : ", viatura " + saved.getVehiclePlate()));
        return toDto(saved);
    }

    @Transactional
    public MaterialPickupDTO cancel(Long pickupId, CancelPickupRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireManagerOrAdmin("anular levantamento de material");
        MaterialPickup pickup = pickupRepository.findByIdAndCompanyId(pickupId, request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Levantamento não encontrado nesta empresa."));
        if (pickup.getStatus() == MaterialPickupStatus.CANCELLED) {
            throw new BusinessRuleException("O levantamento " + pickup.getNumber() + " já está anulado.");
        }
        PickupAccount account = accountRepository.lockByIdAndCompanyId(pickup.getAccount().getId(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Conta de levantamento não encontrada."));
        pickup.setStatus(MaterialPickupStatus.CANCELLED);
        pickup.setCancelReason(request.reason().trim());
        pickupRepository.saveAndFlush(pickup);
        if (account.getStatus() == PickupAccountStatus.COMPLETED) {
            account.setStatus(PickupAccountStatus.OPEN);
            accountRepository.save(account);
        }
        auditLogService.logCurrent("MATERIAL_PICKUP_CANCEL", "Levantamento " + pickup.getNumber() + " anulado: "
                + request.reason().trim());
        return toDto(pickup);
    }

    // ---- Support -------------------------------------------------------------------------------

    /** Loads the slip for printing inside the caller's transaction, checking the tenant. */
    @Transactional(readOnly = true)
    public PickupSlip slip(Long companyId, Long pickupId) {
        CurrentUserContext.requireCompany(companyId);
        MaterialPickup pickup = pickupRepository.findByIdAndCompanyId(pickupId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Levantamento não encontrado nesta empresa."));
        return new PickupSlip(pickup, detail(pickup.getAccount()));
    }

    /** A slip together with the account balances after it, for the printed pickup document. */
    public record PickupSlip(MaterialPickup pickup, PickupAccountDTO account) {}

    record BalanceLine(InvoiceLine invoiceLine, BigDecimal credited, BigDecimal picked, BigDecimal balance) {}

    List<BalanceLine> balances(PickupAccount account) {
        Invoice invoice = account.getInvoice();
        Map<Long, BigDecimal> credited = sums(creditRepository.creditedByInvoiceLine(invoice.getId(), NoteStatus.APPROVED));
        Map<Long, BigDecimal> picked = sums(pickupRepository.pickedByInvoiceLine(account.getId(), MaterialPickupStatus.ISSUED));
        List<BalanceLine> lines = new ArrayList<>();
        for (InvoiceLine line : pickupLines(invoice)) {
            BigDecimal c = credited.getOrDefault(line.getId(), BigDecimal.ZERO);
            BigDecimal p = picked.getOrDefault(line.getId(), BigDecimal.ZERO);
            lines.add(new BalanceLine(line, c, p, PickupBalanceRules.balance(line.getQuantity(), c, p)));
        }
        return lines;
    }

    private static List<InvoiceLine> pickupLines(Invoice invoice) {
        return invoice.getLines().stream()
                .filter(l -> l.getProduct() != null && l.getProduct().getSaleType() != ProductSaleType.SERVICE)
                .toList();
    }

    private PickupAccountDTO detail(PickupAccount account) {
        List<BalanceLine> balances = balances(account);
        List<PickupBalanceLineDTO> lines = balances.stream().map(b -> new PickupBalanceLineDTO(
                b.invoiceLine().getId(), b.invoiceLine().getProduct().getId(), b.invoiceLine().getProduct().getSku(),
                b.invoiceLine().getProduct().getName(), ProductUnitService.baseUnitCodeOf(b.invoiceLine().getProduct()),
                b.invoiceLine().getQuantity(), b.credited(), b.picked(), b.balance())).toList();
        List<MaterialPickupDTO> pickups = pickupRepository.findByAccountWithLines(account.getId()).stream()
                .map(this::toDto).toList();
        return new PickupAccountDTO(summary(account, balances), account.getNotes(),
                account.getInvoice().getStatus().isRealisedSale(), lines, pickups);
    }

    private PickupAccountSummaryDTO summary(PickupAccount account, List<BalanceLine> balances) {
        Invoice invoice = account.getInvoice();
        int pending = (int) balances.stream().filter(b -> b.balance().signum() > 0).count();
        boolean overdue = account.getStatus() == PickupAccountStatus.OPEN && account.getDeadline() != null
                && account.getDeadline().isBefore(LocalDate.now());
        PickupAccountStatus status = invoice.getStatus().isRealisedSale() ? account.getStatus() : PickupAccountStatus.CANCELLED;
        return new PickupAccountSummaryDTO(account.getId(), invoice.getId(), invoice.getInvoiceNumber(), invoice.issueDate(),
                invoice.getClient() == null ? null : invoice.getClient().getId(), clientName(invoice),
                invoice.getWarehouse() == null ? null : invoice.getWarehouse().getName(), account.getDeadline(), overdue,
                status.name(), status.label(), balances.size(), pending,
                pickupRepository.lastPickupAt(account.getId(), MaterialPickupStatus.ISSUED));
    }

    private MaterialPickupDTO toDto(MaterialPickup pickup) {
        List<MaterialPickupLineDTO> lines = pickup.getLines().stream().map(l -> new MaterialPickupLineDTO(
                l.getInvoiceLine().getId(), l.getInvoiceLine().getProduct().getSku(), l.getInvoiceLine().getProduct().getName(),
                ProductUnitService.baseUnitCodeOf(l.getInvoiceLine().getProduct()), l.getQuantity())).toList();
        return new MaterialPickupDTO(pickup.getId(), pickup.getAccount().getId(), pickup.getNumber(), pickup.getPickedAt(),
                pickup.getCollectorName(), pickup.getCollectorDocument(), pickup.getVehiclePlate(), pickup.getNotes(),
                pickup.getStatus().name(), pickup.getStatus().label(), pickup.getCancelReason(), pickup.getCreatedBy(), lines);
    }

    private PickupAccount requireAccount(Long companyId, Long accountId) {
        CurrentUserContext.requireCompany(companyId);
        return accountRepository.findByIdAndCompanyId(accountId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Conta de levantamento não encontrada nesta empresa."));
    }

    private static Map<Long, BigDecimal> sums(List<Object[]> rows) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], row[1] == null ? BigDecimal.ZERO : new BigDecimal(row[1].toString()));
        }
        return map;
    }

    private static List<InvoiceStatus> realisedStatuses() {
        return Arrays.stream(InvoiceStatus.values()).filter(InvoiceStatus::isRealisedSale).toList();
    }

    private static PickupAccountStatus parseStatus(String status) {
        try {
            return PickupAccountStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Estado de levantamento desconhecido: " + status);
        }
    }

    private static String clientName(Invoice invoice) {
        if (invoice.getCustomerName() != null && !invoice.getCustomerName().isBlank()) return invoice.getCustomerName();
        return invoice.getClient() == null ? "—" : invoice.getClient().getName();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
