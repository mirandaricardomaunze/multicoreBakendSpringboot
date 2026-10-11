package mz.multicore.erp.modules.intelligence.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.intelligence.dto.AiAssistantRequest;
import mz.multicore.erp.modules.intelligence.dto.AiAssistantResponse;
import mz.multicore.erp.modules.intelligence.dto.AiEvidenceDTO;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.purchases.model.PurchaseOrder;
import mz.multicore.erp.modules.purchases.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AiManagementAssistantService {

    public static final String HUMAN_NOTICE = "Confirme os valores nos módulos operacionais antes de decidir.";

    private final InvoiceRepository invoiceRepository;
    private final StockRepository stockRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ManagementAiGateway aiGateway;
    private final AuditLogService auditLogService;

    public AiManagementAssistantService(InvoiceRepository invoiceRepository,
                                        StockRepository stockRepository,
                                        PurchaseOrderRepository purchaseOrderRepository,
                                        ManagementAiGateway aiGateway,
                                        AuditLogService auditLogService) {
        this.invoiceRepository = invoiceRepository;
        this.stockRepository = stockRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.aiGateway = aiGateway;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AiAssistantResponse ask(AiAssistantRequest request) {
        PermissionGuard.requireManagerOrAdmin("consultar o Assistente de Gestão");
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) throw new BusinessRuleException("Seleccione uma empresa para consultar o assistente.");
        if (request == null || request.question() == null || request.question().length() < 3) {
            throw new BusinessRuleException("Escreva uma pergunta válida para o assistente.");
        }

        ManagementAiContext context = aggregate(companyId);
        String external = aiGateway.answer(safeIntent(request.question()), context).orElse(null);
        boolean externalUsed = external != null;
        String answer = externalUsed ? external : localAnswer(request.question(), context);
        auditLogService.logCurrent("AI_MANAGEMENT_QUERY",
                "Consulta agregada ao Assistente de Gestão; tema=" + topic(request.question())
                        + "; modeloExterno=" + externalUsed + "; caracteres=" + request.question().length());
        return new AiAssistantResponse(answer, LocalDateTime.now(), evidence(context), externalUsed, HUMAN_NOTICE);
    }

    private ManagementAiContext aggregate(Long companyId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Invoice> sales = invoiceRepository.findByCompanyIdAndCreatedAtBetween(companyId, since, LocalDateTime.now())
                .stream().filter(invoice -> invoice.getStatus() != null && invoice.getStatus().isRealisedSale()).toList();
        BigDecimal salesTotal = sales.stream().map(Invoice::getTotalAmount).map(AiManagementAssistantService::nz)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sales.isEmpty() ? BigDecimal.ZERO
                : salesTotal.divide(BigDecimal.valueOf(sales.size()), 2, RoundingMode.HALF_UP);

        List<Stock> stocks = stockRepository.findByWarehouseCompanyId(companyId);
        long out = stocks.stream().filter(stock -> nz(stock.getQuantity()).signum() <= 0).count();
        long low = stocks.stream().filter(stock -> nz(stock.getQuantity()).signum() > 0
                && nz(stock.getQuantity()).compareTo(nz(stock.getProduct().getMinStock())) <= 0).count();

        List<PurchaseOrder> openOrders = purchaseOrderRepository.findByCompanyIdOrderByOrderDateDesc(companyId).stream()
                .filter(order -> PurchaseOrder.ORDERED.equals(order.getStatus())
                        || PurchaseOrder.PARTIALLY_RECEIVED.equals(order.getStatus())).toList();
        long overdue = openOrders.stream().filter(order -> order.getExpectedDate() != null
                && order.getExpectedDate().isBefore(LocalDate.now())).count();
        BigDecimal openValue = openOrders.stream().map(PurchaseOrder::getTotalAmount)
                .map(AiManagementAssistantService::nz).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ManagementAiContext(salesTotal, sales.size(), average, low, out,
                openOrders.size(), overdue, openValue);
    }

    private String localAnswer(String question, ManagementAiContext c) {
        return switch (topic(question)) {
            case "stock" -> String.format(Locale.forLanguageTag("pt-MZ"),
                    "Existem %d posições esgotadas e %d abaixo do stock mínimo. Priorize os artigos esgotados e confirme as encomendas abertas antes de repor.",
                    c.outOfStockPositions(), c.lowStockPositions());
            case "compras" -> String.format(Locale.forLanguageTag("pt-MZ"),
                    "Há %d encomendas abertas, no valor de %,.2f MT; %d estão atrasadas. Reveja primeiro as atrasadas e confirme a entrega prevista com os fornecedores.",
                    c.openPurchaseOrders(), c.openPurchaseOrderValue(), c.overduePurchaseOrders());
            default -> String.format(Locale.forLanguageTag("pt-MZ"),
                    "Nos últimos 30 dias foram registadas %d vendas, totalizando %,.2f MT, com valor médio de %,.2f MT. Compare este período com o anterior no painel antes de ajustar metas.",
                    c.salesCount30Days(), c.salesTotal30Days(), c.averageSale30Days());
        };
    }

    private List<AiEvidenceDTO> evidence(ManagementAiContext c) {
        return List.of(
                new AiEvidenceDTO("sales_total", "Vendas", c.salesTotal30Days(), "Últimos 30 dias"),
                new AiEvidenceDTO("sales_count", "Número de vendas", BigDecimal.valueOf(c.salesCount30Days()), "Últimos 30 dias"),
                new AiEvidenceDTO("average_sale", "Valor médio", c.averageSale30Days(), "Últimos 30 dias"),
                new AiEvidenceDTO("low_stock", "Posições abaixo do mínimo", BigDecimal.valueOf(c.lowStockPositions()), "Actual"),
                new AiEvidenceDTO("out_of_stock", "Posições esgotadas", BigDecimal.valueOf(c.outOfStockPositions()), "Actual"),
                new AiEvidenceDTO("open_purchase_orders", "Encomendas abertas", BigDecimal.valueOf(c.openPurchaseOrders()), "Actual"),
                new AiEvidenceDTO("overdue_purchase_orders", "Encomendas atrasadas", BigDecimal.valueOf(c.overduePurchaseOrders()), "Actual"));
    }

    private String topic(String question) {
        String normalized = question.toLowerCase(Locale.ROOT);
        if (normalized.contains("stock") || normalized.contains("ruptura") || normalized.contains("reposição")) return "stock";
        if (normalized.contains("compra") || normalized.contains("fornecedor") || normalized.contains("encomenda")) return "compras";
        return "vendas";
    }

    /** O fornecedor externo recebe a intenção permitida, nunca o texto livre que pode conter PII. */
    private String safeIntent(String question) {
        return switch (topic(question)) {
            case "stock" -> "Analise o risco agregado de ruptura e sugira prioridades de reposição.";
            case "compras" -> "Analise as encomendas agregadas abertas e atrasadas.";
            default -> "Analise o desempenho agregado de vendas dos últimos 30 dias.";
        };
    }

    private static BigDecimal nz(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
