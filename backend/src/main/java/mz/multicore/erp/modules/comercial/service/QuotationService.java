package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateQuotationLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateQuotationRequest;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationLineDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.model.OrderLine;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Cotação ao cliente — a proposta de preço que antecede a encomenda.
 *
 * <p>Duas regras carregam este serviço, e ambas são sobre dinheiro:
 * <ol>
 *   <li><b>O preço cotado é o preço honrado.</b> A conversão copia as linhas da cotação verbatim,
 *       como {@code billOrder} copia as da encomenda. O catálogo pode subir amanhã; a proposta que
 *       o cliente aceitou não sobe com ele.</li>
 *   <li><b>Um preço caducado não se honra em silêncio.</b> Converter uma cotação expirada é
 *       recusado; reviver o preço exige estender a validade — acto separado, de gerente, auditado.</li>
 * </ol>
 *
 * <p>Repare-se no que este serviço <b>não</b> injecta: {@code InventoryService} e
 * {@code FinanceService}. Uma cotação não move stock nem dinheiro, e não ter a dependência à mão é
 * mais forte do que prometer não a usar. Ver docs/COTACAO_SPEC.md.
 */
@Service
public class QuotationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final QuotationRepository quotationRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;
    private final WarehouseRepository warehouseRepository;
    private final WalkInClientProvider walkInClientProvider;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;
    private final ComercialService comercialService;

    public QuotationService(QuotationRepository quotationRepository,
                            ClientRepository clientRepository,
                            ProductRepository productRepository,
                            CompanyRepository companyRepository,
                            WarehouseRepository warehouseRepository,
                            WalkInClientProvider walkInClientProvider,
                            DocumentNumberService documentNumberService,
                            AuditLogService auditLogService,
                            ComercialService comercialService) {
        this.quotationRepository = quotationRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
        this.warehouseRepository = warehouseRepository;
        this.walkInClientProvider = walkInClientProvider;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
        this.comercialService = comercialService;
    }

    /** Emite a cotação em {@code DRAFT}. Nada se move — é só uma proposta. */
    @Transactional
    public QuotationDTO create(CreateQuotationRequest request) {
        CurrentUserContext.requireCompany(request.companyId());

        Client client;
        if (request.clientId() != null) {
            client = clientRepository.findByIdAndCompaniesId(request.clientId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."));
        } else {
            client = walkInClientProvider.getOrCreate();
        }
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

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (CreateQuotationLineRequest lineReq : request.lines()) {
            Product product = productRepository.findByIdAndCompaniesId(lineReq.productId(), request.companyId())
                    .orElseThrow(() -> new BusinessRuleException("Produto não encontrado ID: " + lineReq.productId()));

            // Apreçamento canónico, o mesmo da fatura e da encomenda: preço efectivo do artigo
            // (com grosso quando a quantidade o atinge) e taxa de IVA do artigo. O pedido não tem
            // campo por onde ditar nenhum dos dois — ver CreateQuotationLineRequest.
            BigDecimal unitPrice = product.effectiveUnitPrice(lineReq.quantity());
            BigDecimal taxRate = product.effectiveTaxRate();
            BigDecimal discountPct = lineReq.discountPercentage() == null
                    ? BigDecimal.ZERO : lineReq.discountPercentage();
            if (discountPct.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessRuleException("O desconto de linha não pode exceder 100%.");
            }

            LineCalculator.LineAmounts amounts = LineCalculator.compute(
                    unitPrice, lineReq.quantity(), discountPct, taxRate);

            QuotationLine line = new QuotationLine();
            line.setProduct(product);
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(unitPrice);
            line.setTaxRate(taxRate);
            line.setDiscountPercentage(discountPct);
            line.setLineTotal(amounts.total());
            quotation.addLine(line);

            subtotal = subtotal.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());
        }

        quotation.setTotalBeforeTax(subtotal.setScale(2, RoundingMode.HALF_UP));
        quotation.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        quotation.setTotalAmount(subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP));

        quotation.setQuotationNumber(documentNumberService.next(DocumentSeries.QUOTATION));
        quotation.setCreatedBy(CurrentUserContext.getUsername());

        Quotation saved = quotationRepository.save(quotation);
        auditLogService.logCurrent("QUOTATION_CREATE",
                "Cotação " + saved.getQuotationNumber() + " emitida para " + saved.clientLabel()
                        + ". Total: " + saved.getTotalAmount() + " MT. Válida até "
                        + saved.getValidUntil().format(DATE_FMT) + ".");
        return toDTO(saved);
    }

    /** Marca a proposta como enviada ao cliente. Registo, não cerimónia — ver spec §5. */
    @Transactional
    public QuotationDTO send(Long id) {
        Quotation quotation = load(id);
        if (quotation.getStatus() != QuotationStatus.DRAFT) {
            throw new BusinessRuleException("Apenas cotações em rascunho podem ser marcadas como enviadas. "
                    + "Estado actual: " + quotation.getStatus().getLabel() + ".");
        }
        quotation.setStatus(QuotationStatus.SENT);
        quotation.setSentAt(LocalDateTime.now());
        Quotation saved = quotationRepository.save(quotation);
        auditLogService.logCurrent("QUOTATION_SEND",
                "Cotação " + saved.getQuotationNumber() + " marcada como enviada ao cliente.");
        return toDTO(saved);
    }

    /** Regista que o cliente aceitou a proposta. */
    @Transactional
    public QuotationDTO accept(Long id) {
        Quotation quotation = load(id);
        requireOpen(quotation, "aceitar");
        quotation.setStatus(QuotationStatus.ACCEPTED);
        stampDecision(quotation);
        Quotation saved = quotationRepository.save(quotation);
        auditLogService.logCurrent("QUOTATION_ACCEPT",
                "Cotação " + saved.getQuotationNumber() + " aceite pelo cliente.");
        return toDTO(saved);
    }

    /** Regista a recusa do cliente. Motivo obrigatório — sem ele não se aprende nada com a perda. */
    @Transactional
    public QuotationDTO reject(Long id, String reason) {
        Quotation quotation = load(id);
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
        return toDTO(saved);
    }

    /** Fecha a proposta sem decisão do cliente (engano, duplicada, cliente desistiu de pedir). */
    @Transactional
    public QuotationDTO cancel(Long id) {
        Quotation quotation = load(id);
        requireOpen(quotation, "cancelar");
        quotation.setStatus(QuotationStatus.CANCELLED);
        Quotation saved = quotationRepository.save(quotation);
        auditLogService.logCurrent("QUOTATION_CANCEL",
                "Cotação " + saved.getQuotationNumber() + " cancelada.");
        return toDTO(saved);
    }

    /**
     * Estende a validade — reviver um preço caducado é uma concessão comercial, por isso exige
     * gerente e fica auditado <b>com a validade antiga e a nova</b>. Sem as duas datas no registo,
     * um preço ressuscitado seria indistinguível de um preço que nunca caducou.
     */
    @Transactional
    public QuotationDTO extendValidity(Long id, LocalDate newValidUntil) {
        Quotation quotation = load(id);
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
        return toDTO(saved);
    }

    /**
     * Converte a proposta na encomenda, <b>herdando os preços cotados</b>.
     *
     * <p>Não reapreça pelo catálogo (R2) e não cria a encomenda por conta própria: delega em
     * {@link ComercialService#placeOrder}, a única porta que numera a série {@code EC} e submete à
     * Engine de Aprovações. A encomenda gerada é formal, logo passa pela aprovação por valor como
     * qualquer outra — o cliente ter dito sim não substitui a decisão de dentro de casa.
     */
    @Transactional
    public OrderDTO convert(Long id) {
        Quotation quotation = load(id);
        LocalDate today = LocalDate.now();

        if (quotation.getStatus() == QuotationStatus.CONVERTED) {
            throw new BusinessRuleException("Esta cotação já foi convertida na encomenda "
                    + quotation.getOrderNumber() + ".");
        }
        if (!quotation.getStatus().isOpen()) {
            throw new BusinessRuleException("Apenas cotações em aberto podem ser convertidas. "
                    + "Estado actual: " + quotation.getStatus().getLabel() + ".");
        }
        if (quotation.isExpired(today)) {
            throw new BusinessRuleException("A cotação " + quotation.getQuotationNumber()
                    + " caducou a " + quotation.getValidUntil().format(DATE_FMT)
                    + ". Estenda a validade antes de converter — o preço proposto deixou de estar garantido.");
        }

        List<OrderLine> orderLines = new ArrayList<>();
        for (QuotationLine quoted : quotation.getLines()) {
            OrderLine line = new OrderLine();
            line.setProduct(quoted.getProduct());
            line.setQuantity(quoted.getQuantity());
            // O preço, o IVA e o desconto vêm da COTAÇÃO, não do catálogo de hoje. É esta linha que
            // faz a proposta valer alguma coisa.
            line.setUnitPrice(quoted.getUnitPrice());
            line.setTaxRate(quoted.getTaxRate());
            line.setDiscountPercentage(quoted.getDiscountPercentage());
            line.setLineTotal(quoted.getLineTotal());
            orderLines.add(line);
        }

        // A encomenda leva consigo de onde veio e o que foi acordado — é a cotação quem o declara
        // (agreedTerms), e é aqui, na confirmação, que a data de entrega prometida se fixa.
        OrderDTO order = comercialService.placeOrder(
                quotation.getCompany(),
                quotation.getClient(),
                quotation.getWarehouse(),
                quotation.getWalkInName(),
                orderLines,
                OrderKind.FORMAL_ORDER,
                quotation.agreedTerms());

        // Converter a partir de DRAFT/SENT é aceitar: sem este carimbo perdia-se o "quando é que o
        // cliente disse sim" de quem não usa o passo explícito de aceitação.
        if (quotation.getDecidedAt() == null) {
            stampDecision(quotation);
        }
        quotation.setStatus(QuotationStatus.CONVERTED);
        quotation.setOrderId(order.id());
        quotation.setOrderNumber(order.orderNumber());
        quotationRepository.save(quotation);

        auditLogService.logCurrent("QUOTATION_CONVERT",
                "Cotação " + quotation.getQuotationNumber() + " convertida na encomenda "
                        + order.orderNumber() + ". Total: " + order.totalAmount() + " MT.");
        return order;
    }

    @Transactional(readOnly = true)
    public List<QuotationDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return quotationRepository.findByCompanyIdOrderByQuotationDateDesc(companyId)
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public QuotationDTO findById(Long id) {
        return toDTO(load(id));
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private Quotation load(Long id) {
        return quotationRepository
                .findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Cotação não encontrada."));
    }

    /** Recusa acções sobre propostas já fechadas, nomeando o estado em PT-MZ. */
    private void requireOpen(Quotation quotation, String action) {
        if (!quotation.getStatus().isOpen()) {
            throw new BusinessRuleException("Não é possível " + action + " uma cotação "
                    + quotation.getStatus().getLabel().toLowerCase() + ".");
        }
    }

    private void stampDecision(Quotation quotation) {
        quotation.setDecidedAt(LocalDateTime.now());
        quotation.setDecidedBy(CurrentUserContext.getUsername());
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private QuotationDTO toDTO(Quotation q) {
        LocalDate today = LocalDate.now();
        List<QuotationLineDTO> lines = q.getLines().stream()
                .map(l -> new QuotationLineDTO(
                        l.getId(),
                        l.getProduct().getId(),
                        l.getProduct().getSku(),
                        l.getProduct().getName(),
                        l.getQuantity(),
                        l.getUnitPrice(),
                        l.getTaxRate(),
                        l.getDiscountPercentage(),
                        l.getLineTotal(),
                        Math.max(1, l.getProduct().getUnitsPerBox())))
                .toList();

        return new QuotationDTO(
                q.getId(),
                q.getQuotationNumber(),
                q.getQuotationDate(),
                q.getValidUntil(),
                // Caducidade derivada no servidor. O desktop apresenta; não recalcula.
                q.isExpired(today),
                q.daysUntilExpiry(today),
                q.getCompany() != null ? q.getCompany().getId() : null,
                q.getClient() != null ? q.getClient().getId() : null,
                q.clientLabel(),
                q.getClient() != null ? q.getClient().getTaxId() : null,
                q.getWalkInName(),
                q.getWarehouse() != null ? q.getWarehouse().getId() : null,
                q.getWarehouse() != null ? q.getWarehouse().getName() : null,
                q.getTotalBeforeTax(),
                q.getTaxAmount(),
                q.getTotalAmount(),
                q.getStatus().name(),
                q.getStatus().getLabel(),
                q.getPaymentTerms(),
                q.getDeliveryTerms(),
                q.getDeliveryDays(),
                q.getNotes(),
                q.getSentAt(),
                q.getDecidedAt(),
                q.getDecidedBy(),
                q.getRejectionReason(),
                q.getOrderId(),
                q.getOrderNumber(),
                q.getCreatedBy(),
                lines);
    }
}
