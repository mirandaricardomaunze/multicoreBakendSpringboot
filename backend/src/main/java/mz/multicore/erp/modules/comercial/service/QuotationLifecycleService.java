package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateQuotationLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateQuotationRequest;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.comercial.dto.UpdateQuotationRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.QuotationLine;
import mz.multicore.erp.modules.comercial.model.QuotationStatus;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gestão do ciclo de vida e negociação de propostas comerciais (criação, actualização, envio, decisão).
 */
@Service
public class QuotationLifecycleService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final QuotationRepository quotationRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final WarehouseRepository warehouseRepository;
    private final WalkInClientProvider walkInClientProvider;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;
    private final QuotationQueryService queryService;

    public QuotationLifecycleService(QuotationRepository quotationRepository,
                                     ClientRepository clientRepository,
                                     ProductRepository productRepository,
                                     CompanyRepository companyRepository,
                                     WarehouseRepository warehouseRepository,
                                     WalkInClientProvider walkInClientProvider,
                                     DocumentNumberService documentNumberService,
                                     AuditLogService auditLogService,
                                     QuotationQueryService queryService) {
        this.quotationRepository = quotationRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
        this.warehouseRepository = warehouseRepository;
        this.walkInClientProvider = walkInClientProvider;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
        this.queryService = queryService;
    }

    @Transactional
    public QuotationDTO create(CreateQuotationRequest request) {
        CurrentUserContext.requireCompany(request.companyId());

        Client client = request.clientId() != null
                ? clientRepository.findByIdAndCompaniesId(request.clientId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."))
                : walkInClientProvider.getOrCreate();
        Company company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
        if (!request.companyId().equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }

        Quotation quotation = new Quotation();
        quotation.setCompany(company);
        quotation.setClient(client);
        quotation.setWarehouse(warehouse);
        quotation.setQuotationDate(LocalDateTime.now());
        quotation.assignValidity(LocalDate.now(), request.validityDays());
        quotation.setStatus(QuotationStatus.DRAFT);
        quotation.setPaymentTerms(blankToNull(request.paymentTerms()));
        quotation.setDeliveryTerms(blankToNull(request.deliveryTerms()));
        quotation.setDeliveryDays(request.deliveryDays());
        quotation.setNotes(blankToNull(request.notes()));
        if (request.walkInName() != null && !request.walkInName().isBlank()) {
            quotation.setWalkInName(request.walkInName().trim());
        }

        populateLines(quotation, request.lines(), request.companyId(), false);
        quotation.setQuotationNumber(documentNumberService.next(DocumentSeries.QUOTATION));
        quotation.setCreatedBy(CurrentUserContext.getUsername());

        Quotation saved = quotationRepository.save(quotation);
        auditLogService.logCurrent("QUOTATION_CREATE",
                "Cotação " + saved.getQuotationNumber() + " emitida para " + saved.clientLabel()
                        + ". Total: " + saved.getTotalAmount() + " MT. Válida até "
                        + saved.getValidUntil().format(DATE_FMT) + ".");
        return queryService.toDTO(saved);
    }

    @Transactional
    public QuotationDTO update(Long id, UpdateQuotationRequest request) {
        Quotation quotation = queryService.load(id);
        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new BusinessRuleException("Apenas cotações em rascunho podem ser alteradas. Estado actual: "
                    + quotation.getStatus().getLabel() + ".");
        }
        if (request.version() == null || request.version() != quotation.getVersion()) {
            throw new BusinessRuleException(
                    "A cotação foi alterada por outro utilizador. Recarregue o documento antes de guardar.");
        }

        Long companyId = quotation.getCompany().getId();
        Client client = request.clientId() == null
                ? walkInClientProvider.getOrCreate()
                : clientRepository.findByIdAndCompaniesId(request.clientId(), companyId)
                    .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new BusinessRuleException("Armazém não encontrado."));
        if (warehouse.getCompany() == null || !companyId.equals(warehouse.getCompany().getId())) {
            throw new BusinessRuleException("O armazém não pertence à empresa ativa.");
        }

        quotation.setClient(client);
        quotation.setWalkInName(blankToNull(request.walkInName()));
        quotation.setWarehouse(warehouse);
        quotation.assignValidity(LocalDate.now(), request.validityDays());
        quotation.setPaymentTerms(blankToNull(request.paymentTerms()));
        quotation.setDeliveryTerms(blankToNull(request.deliveryTerms()));
        quotation.setDeliveryDays(request.deliveryDays());
        quotation.setNotes(blankToNull(request.notes()));
        quotation.getLines().clear();
        populateLines(quotation, request.lines(), companyId, true);

        Quotation saved = quotationRepository.saveAndFlush(quotation);
        auditLogService.logCurrent("QUOTATION_UPDATE",
                "Cotação " + saved.getQuotationNumber() + " actualizada em rascunho. Total: "
                        + saved.getTotalAmount() + " MT.");
        return queryService.toDTO(saved);
    }

    @Transactional
    public QuotationDTO send(Long id) {
        Quotation quotation = queryService.load(id);
        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new BusinessRuleException("Apenas cotações em rascunho podem ser marcadas como enviadas. "
                    + "Estado actual: " + quotation.getStatus().getLabel() + ".");
        }
        quotation.setStatus(QuotationStatus.SENT);
        quotation.setSentAt(LocalDateTime.now());
        Quotation saved = quotationRepository.save(quotation);
        auditLogService.logCurrent("QUOTATION_SEND",
                "Cotação " + saved.getQuotationNumber() + " marcada como enviada ao cliente.");
        return queryService.toDTO(saved);
    }

    @Transactional
    public QuotationDTO accept(Long id) {
        Quotation quotation = queryService.load(id);
        requireOpen(quotation, "aceitar");
        quotation.setStatus(QuotationStatus.ACCEPTED);
        stampDecision(quotation);
        Quotation saved = quotationRepository.save(quotation);
        auditLogService.logCurrent("QUOTATION_ACCEPT",
                "Cotação " + saved.getQuotationNumber() + " aceite pelo cliente.");
        return queryService.toDTO(saved);
    }

    @Transactional
    public QuotationDTO reject(Long id, String reason) {
        Quotation quotation = queryService.load(id);
        requireOpen(quotation, "recusar");
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("É obrigatório indicar o motivo da recusa.");
        }
        quotation.setStatus(QuotationStatus.REJECTED);
        quotation.setRejectionReason(reason.trim());
        stampDecision(quotation);
        Quotation saved = quotationRepository.save(quotation);
        auditLogService.logCurrent("QUOTATION_REJECT",
                "Cotação " + saved.getQuotationNumber() + " recusada. Motivo: " + reason.trim());
        return queryService.toDTO(saved);
    }

    @Transactional
    public QuotationDTO cancel(Long id) {
        Quotation quotation = queryService.load(id);
        requireOpen(quotation, "cancelar");
        quotation.setStatus(QuotationStatus.CANCELLED);
        Quotation saved = quotationRepository.save(quotation);
        auditLogService.logCurrent("QUOTATION_CANCEL",
                "Cotação " + saved.getQuotationNumber() + " cancelada.");
        return queryService.toDTO(saved);
    }

    @Transactional
    public QuotationDTO extendValidity(Long id, LocalDate newValidUntil) {
        Quotation quotation = queryService.load(id);
        PermissionGuard.requireManagerOrAdmin("estender a validade de uma cotação");
        requireOpen(quotation, "estender");
        if (newValidUntil == null) {
            throw new BusinessRuleException("A nova data de validade é obrigatória.");
        }
        LocalDate previous = quotation.getValidUntil();
        if (!newValidUntil.isAfter(previous)) {
            throw new BusinessRuleException("A nova validade tem de ser posterior à actual ("
                    + previous.format(DATE_FMT) + ").");
        }
        quotation.setValidUntil(newValidUntil);
        Quotation saved = quotationRepository.save(quotation);
        auditLogService.logCurrent("QUOTATION_EXTEND",
                "Validade da cotação " + saved.getQuotationNumber() + " estendida de "
                        + previous.format(DATE_FMT) + " para " + newValidUntil.format(DATE_FMT) + ".");
        return queryService.toDTO(saved);
    }

    private void populateLines(Quotation quotation, List<CreateQuotationLineRequest> lineRequests,
                               Long companyId, boolean isUpdate) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        for (CreateQuotationLineRequest lineReq : lineRequests) {
            Product product = productRepository.findByIdAndCompaniesId(lineReq.productId(), companyId)
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));
            BigDecimal discount = lineReq.discountPercentage() == null
                    ? BigDecimal.ZERO : lineReq.discountPercentage();
            if (isUpdate) {
                if (discount.signum() < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new BusinessRuleException("O desconto de linha deve ficar entre 0 e 100%.");
                }
            } else if (discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessRuleException("O desconto de linha não pode exceder 100%.");
            }
            BigDecimal unitPrice = product.effectiveUnitPrice(lineReq.quantity());
            BigDecimal taxRate = product.effectiveTaxRate();
            LineCalculator.LineAmounts amounts = LineCalculator.compute(
                    unitPrice, lineReq.quantity(), discount, taxRate);

            QuotationLine line = new QuotationLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(unitPrice);
            line.setTaxRate(taxRate);
            line.setDiscountPercentage(discount);
            line.setLineTotal(amounts.total());
            quotation.addLine(line);
            subtotal = subtotal.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
        }
        quotation.setTotalBeforeTax(subtotal.setScale(2, RoundingMode.HALF_UP));
        quotation.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        quotation.setTotalAmount(subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP));
    }

    public void requireOpen(Quotation quotation, String action) {
        if (!quotation.getStatus().isOpen()) {
            throw new BusinessRuleException("Não é possível " + action + " uma cotação "
                    + quotation.getStatus().getLabel().toLowerCase() + ".");
        }
    }

    public void stampDecision(Quotation quotation) {
        quotation.setDecidedAt(LocalDateTime.now());
        quotation.setDecidedBy(CurrentUserContext.getUsername());
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
