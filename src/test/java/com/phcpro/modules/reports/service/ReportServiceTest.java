package com.phcpro.modules.reports.service;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.approvals.model.ApprovalStatus;
import com.phcpro.modules.approvals.repository.ApprovalRequestRepository;
import com.phcpro.modules.comercial.model.Client;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceLine;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.inventory.repository.StockRepository;
import com.phcpro.modules.pos.repository.PaymentEntryRepository;
import com.phcpro.modules.pos.repository.TillMovementRepository;
import com.phcpro.modules.pos.repository.TillSessionRepository;
import com.phcpro.modules.reports.dto.DailyStoreReportDTO;
import com.phcpro.modules.reports.dto.ProductMarginDTO;
import com.phcpro.modules.reports.dto.StoreDashboardDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes do ReportService. Foco na regressão dos recebimentos: o dashboard tinha uma
 * definição própria de "venda" e de "por cobrar", diferente da usada nas Contas Correntes
 * ({@code ComercialService.getOutstandingInvoicesByCompany}) e no relatório diário — dois
 * ecrãs do mesmo sistema davam números diferentes para a mesma pergunta.
 * Ver docs/RECEBIMENTOS_SALDO_SPEC.md. Dependências mockadas.
 */
class ReportServiceTest {

    private InvoiceRepository invoiceRepository;
    private StockRepository stockRepository;
    private ApprovalRequestRepository approvalRequestRepository;
    private TillSessionRepository tillSessionRepository;
    private PaymentEntryRepository paymentEntryRepository;
    private TillMovementRepository tillMovementRepository;
    private ReportService service;

    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        stockRepository = mock(StockRepository.class);
        approvalRequestRepository = mock(ApprovalRequestRepository.class);
        tillSessionRepository = mock(TillSessionRepository.class);
        paymentEntryRepository = mock(PaymentEntryRepository.class);
        tillMovementRepository = mock(TillMovementRepository.class);

        service = new ReportService(invoiceRepository, stockRepository, approvalRequestRepository,
                tillSessionRepository, paymentEntryRepository, tillMovementRepository);

        when(stockRepository.findByWarehouseCompanyId(COMPANY_ID)).thenReturn(List.of());
        when(approvalRequestRepository.findByCompanyIdAndStatus(COMPANY_ID, ApprovalStatus.PENDING))
                .thenReturn(List.of());
        when(tillSessionRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());
        when(paymentEntryRepository.findByInvoiceCompanyIdAndPaidAtBetween(eq(COMPANY_ID), any(), any()))
                .thenReturn(List.of());
        when(tillMovementRepository.findByTillSessionCompanyIdAndMovementDateBetween(eq(COMPANY_ID), any(), any()))
                .thenReturn(List.of());

        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test // RP-10
    void dashboard_porCobrar_incluiFaturasParcialmentePagas_eContaSoOSaldo() {
        // Fatura de 1000 com 400 já recebidos: em dívida estão 600.
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                invoice(InvoiceStatus.PARTIALLY_PAID, "1000", "400")));

        StoreDashboardDTO dto = service.buildStoreDashboard(COMPANY_ID);

        // Antes do fix: PARTIALLY_PAID não entrava no filtro (só APPROVED) → 0,00.
        // O cliente que pagou metade aparecia como se nada devesse.
        assertEquals(0, dto.unpaidInvoicesAmount().compareTo(new BigDecimal("600")),
                "por cobrar tem de ser o saldo em dívida das parcialmente pagas");
    }

    @Test // RP-11
    void dashboard_porCobrar_desconta_oQueJaFoiRecebido() {
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                invoice(InvoiceStatus.APPROVED, "500", "0"),
                invoice(InvoiceStatus.PARTIALLY_PAID, "300", "100"),
                invoice(InvoiceStatus.PAID, "900", "900"),
                invoice(InvoiceStatus.CANCELLED, "700", "0")));

        StoreDashboardDTO dto = service.buildStoreDashboard(COMPANY_ID);

        // 500 (fiado) + 200 (saldo da parcial). Paga e anulada não contam.
        assertEquals(0, dto.unpaidInvoicesAmount().compareTo(new BigDecimal("700")));
    }

    @Test // RP-12
    void dashboard_vendasDeHoje_incluiFiado_naoSoAsPagas() {
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                invoice(InvoiceStatus.PAID, "100", "100"),
                invoice(InvoiceStatus.APPROVED, "250", "0")));

        StoreDashboardDTO dto = service.buildStoreDashboard(COMPANY_ID);

        // Antes do fix contava só PAID → 100. Uma venda a fiado é uma venda: a mercadoria
        // saiu e o stock baixou; só o recebimento é que ficou por fazer.
        assertEquals(2, dto.salesToday().count());
        assertEquals(0, dto.salesToday().totalAmount().compareTo(new BigDecimal("350")));
    }

    @Test // RP-13
    void dashboard_vendasDeHoje_excluiPorAprovarEAnuladas() {
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                invoice(InvoiceStatus.PAID, "100", "100"),
                invoice(InvoiceStatus.PENDING_DISCOUNT_APPROVAL, "800", "0"),
                invoice(InvoiceStatus.CANCELLED, "400", "0"),
                invoice(InvoiceStatus.REJECTED, "600", "0")));

        StoreDashboardDTO dto = service.buildStoreDashboard(COMPANY_ID);

        // Fatura à espera de aprovação de desconto ainda não é venda — não baixou stock.
        assertEquals(1, dto.salesToday().count());
        assertEquals(0, dto.salesToday().totalAmount().compareTo(new BigDecimal("100")));
    }

    @Test // RP-14
    void dashboard_eRelatorioDiario_dao_oMesmoTotalDeVendas() {
        List<Invoice> hoje = List.of(
                invoice(InvoiceStatus.PAID, "100", "100"),
                invoice(InvoiceStatus.APPROVED, "250", "0"),
                invoice(InvoiceStatus.PENDING_DISCOUNT_APPROVAL, "800", "0"),
                invoice(InvoiceStatus.CANCELLED, "400", "0"));
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(hoje);

        StoreDashboardDTO dashboard = service.buildStoreDashboard(COMPANY_ID);
        DailyStoreReportDTO diario = service.buildDailyStoreReport(COMPANY_ID, LocalDate.now());

        // O bug de confiança: o dashboard contava só PAID (100) e o relatório diário tudo o
        // que não fosse anulado/rejeitado (1150). Quem conferisse perdia a fé em ambos.
        assertEquals(dashboard.salesToday().count(), diario.sales().count(),
                "dashboard e relatório diário têm de contar as mesmas vendas");
        assertEquals(0, dashboard.salesToday().totalAmount().compareTo(diario.sales().totalAmount()),
                "dashboard e relatório diário têm de somar o mesmo");
    }

    // ────────────────────────── helpers ──────────────────────────

    // ────────────────────────── margem: custo do acto da venda ──────────────────────────

    @Test // MC-01
    void margem_usaOCustoGravadoNaLinha_naoOPrecoDeCompraActual() {
        Product produto = produto(1L, "ARROZ", "Arroz 5kg", "80.00");
        // Vendido a 100 quando custava 60. Hoje o fornecedor cobra 80 (o preço no cadastro).
        Invoice venda = vendaComLinha(produto, "2", "200.00", "60.00");
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(venda));

        DailyStoreReportDTO report = service.buildDailyStoreReport(COMPANY_ID, LocalDate.now());
        ProductMarginDTO margem = report.grossMarginByProduct().get(0);

        // Antes do fix: custo = 80 × 2 = 160 → margem 40. A subida do fornecedor reescrevia
        // a margem de uma venda já feita.
        assertEquals(0, margem.estimatedCost().compareTo(new BigDecimal("120.00")),
                "custo = 60 × 2, o que custou na altura");
        assertEquals(0, margem.grossMargin().compareTo(new BigDecimal("80.00")));
    }

    @Test // MC-02
    void margem_semCustoGravado_recorreAoPrecoActual() {
        Product produto = produto(1L, "ARROZ", "Arroz 5kg", "80.00");
        Invoice legado = vendaComLinha(produto, "2", "200.00", /*unitCost*/ null);
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(legado));

        DailyStoreReportDTO report = service.buildDailyStoreReport(COMPANY_ID, LocalDate.now());

        assertEquals(0, report.grossMarginByProduct().get(0).estimatedCost().compareTo(new BigDecimal("160.00")),
                "linha anterior à V37: estimativa pelo preço actual");
    }

    @Test // MC-03
    void margem_semCustoNenhum_naoRebenta() {
        Product semPreco = produto(1L, "SERV", "Serviço", null);
        Invoice venda = vendaComLinha(semPreco, "1", "500.00", null);
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(venda));

        DailyStoreReportDTO report = service.buildDailyStoreReport(COMPANY_ID, LocalDate.now());

        assertEquals(0, report.grossMarginByProduct().get(0).estimatedCost().compareTo(BigDecimal.ZERO));
        assertEquals(0, report.grossMarginByProduct().get(0).grossMargin().compareTo(new BigDecimal("500.00")));
    }

    private Product produto(Long id, String sku, String name, String purchasePrice) {
        Product product = new Product();
        product.setId(id);
        product.setSku(sku);
        product.setName(name);
        if (purchasePrice != null) product.setPurchasePrice(new BigDecimal(purchasePrice));
        return product;
    }

    private Invoice vendaComLinha(Product product, String qty, String lineTotal, String unitCost) {
        Invoice invoice = invoice(InvoiceStatus.PAID, lineTotal, lineTotal);
        InvoiceLine line = new InvoiceLine();
        line.setProduct(product);
        line.setQuantity(new BigDecimal(qty));
        line.setUnitPrice(new BigDecimal(lineTotal).divide(new BigDecimal(qty)));
        line.setTaxRate(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal(lineTotal));
        if (unitCost != null) line.setUnitCost(new BigDecimal(unitCost));
        invoice.addLine(line);
        return invoice;
    }

    private Invoice invoice(InvoiceStatus status, String total, String paid) {
        Invoice invoice = new Invoice();
        invoice.setStatus(status);
        invoice.setTotalAmount(new BigDecimal(total));
        invoice.setAmountPaid(new BigDecimal(paid));
        invoice.setCreatedAt(LocalDateTime.now());
        Client client = new Client();
        client.setName("Cliente Loja");
        invoice.setClient(client);
        return invoice;
    }
}
