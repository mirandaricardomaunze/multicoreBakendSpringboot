package mz.multicore.erp.modules.portal.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.model.ApprovalRequest;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;
import mz.multicore.erp.modules.approvals.service.ApprovalRequestLookup;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.service.TillSessionLookup;
import mz.multicore.erp.modules.portal.dto.PortalOverdueClientDTO;
import mz.multicore.erp.modules.portal.dto.PortalSummaryDTO;
import mz.multicore.erp.modules.reports.dto.LowStockAlertDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** PG-07 — conteúdo do resumo do painel do portal (PORTAL_GESTOR_SPEC §3.3). */
class PortalSummaryServiceTest {

    private static final Long COMPANY = 7L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);

    private final InvoiceLookup invoiceLookup = mock(InvoiceLookup.class);
    private final StockLookup stockLookup = mock(StockLookup.class);
    private final TillSessionLookup tillSessionLookup = mock(TillSessionLookup.class);
    private final ApprovalRequestLookup approvalLookup = mock(ApprovalRequestLookup.class);
    private final PortalSummaryService service =
            new PortalSummaryService(invoiceLookup, stockLookup, tillSessionLookup, approvalLookup);

    @BeforeEach
    void emptyByDefault() {
        when(invoiceLookup.listCreatedBetween(eq(COMPANY), any(), any())).thenReturn(List.of());
        when(invoiceLookup.listByStatus(eq(COMPANY), any())).thenReturn(List.of());
        when(stockLookup.listForCompany(COMPANY)).thenReturn(List.of());
        when(tillSessionLookup.listForCompany(COMPANY)).thenReturn(List.of());
        when(approvalLookup.listForCompanyByStatus(COMPANY, ApprovalStatus.PENDING)).thenReturn(List.of());
    }

    @Test
    void pg07SalesOfTodayAndMonthByStoreOnlyRealised() {
        Warehouse baixa = warehouse("Loja Baixa");
        Warehouse matola = warehouse("Loja Matola");
        when(invoiceLookup.listCreatedBetween(COMPANY, TODAY.withDayOfMonth(1).atStartOfDay(),
                TODAY.plusDays(1).atStartOfDay())).thenReturn(List.of(
                sale(baixa, "100.00", TODAY, InvoiceStatus.PAID),
                sale(baixa, "50.00", TODAY.minusDays(3), InvoiceStatus.PAID),
                sale(matola, "400.00", TODAY.minusDays(1), InvoiceStatus.APPROVED),
                sale(null, "30.00", TODAY, InvoiceStatus.PAID),
                sale(baixa, "999.00", TODAY, InvoiceStatus.DRAFT)));

        PortalSummaryDTO summary = build();

        assertThat(summary.salesToday()).isEqualByComparingTo("130.00");
        assertThat(summary.salesMonth()).isEqualByComparingTo("580.00");
        assertThat(summary.salesByStore()).extracting(s -> s.store())
                .containsExactly("Loja Matola", "Loja Baixa", PortalSummaryService.NO_STORE);
        assertThat(summary.salesByStore().get(1).today()).isEqualByComparingTo("100.00");
        assertThat(summary.salesByStore().get(1).month()).isEqualByComparingTo("150.00");
        assertThat(summary.salesByStore().get(0).today()).isEqualByComparingTo("0");
    }

    @Test
    void pg07OpenTillsWithCurrentOperatorOldestFirst() {
        TillSession recent = till("ana", null, TODAY.atTime(10, 0), "OPEN");
        TillSession older = till("joao", "maria", TODAY.atTime(7, 30), "OPEN");
        TillSession closed = till("rui", null, TODAY.atTime(6, 0), "CLOSED");
        when(tillSessionLookup.listForCompany(COMPANY)).thenReturn(List.of(recent, older, closed));

        PortalSummaryDTO summary = build();

        assertThat(summary.openTills()).extracting(t -> t.operator()).containsExactly("maria", "ana");
        assertThat(summary.openTills().get(0).openedAt()).isEqualTo(TODAY.atTime(7, 30));
    }

    @Test
    void pg07LowStockTwentyMostUrgentByCoverage() {
        List<Stock> stocks = new ArrayList<>();
        IntStream.rangeClosed(1, 25).forEach(i -> stocks.add(stock("P" + i, String.valueOf(i), "30")));
        stocks.add(stock("OK", "50", "30"));
        stocks.add(stock("SEM-MINIMO", "0", null));
        stocks.add(stock("BIG", "1", "10"));
        when(stockLookup.listForCompany(COMPANY)).thenReturn(stocks);

        List<LowStockAlertDTO> low = build().lowStock();

        assertThat(low).hasSize(PortalSummaryService.LOW_STOCK_LIMIT);
        assertThat(low).extracting(LowStockAlertDTO::sku).startsWith("P1", "P2", "P3", "BIG")
                .doesNotContain("OK", "SEM-MINIMO", "P25");
    }

    @Test
    void pg07OverdueTotalAndTopTenClients() {
        List<Invoice> collectable = new ArrayList<>();
        IntStream.rangeClosed(1, 12).forEach(i ->
                collectable.add(receivable(client((long) i, "Cliente " + i), String.valueOf(i * 10), TODAY.minusDays(i))));
        Client big = client(99L, "Grande Lda");
        collectable.add(receivable(big, "500.00", TODAY.minusDays(5)));
        collectable.add(receivable(big, "300.00", TODAY.minusDays(40)));
        collectable.add(receivable(big, "1000.00", TODAY.plusDays(10)));
        when(invoiceLookup.listByStatus(COMPANY, InvoiceStatus.collectableStatuses())).thenReturn(collectable);

        PortalSummaryDTO summary = build();

        assertThat(summary.overdueTotal()).isEqualByComparingTo("1580.00");
        assertThat(summary.overdueClients()).hasSize(PortalSummaryService.OVERDUE_CLIENTS_LIMIT);
        PortalOverdueClientDTO first = summary.overdueClients().get(0);
        assertThat(first.clientName()).isEqualTo("Grande Lda");
        assertThat(first.amount()).isEqualByComparingTo("800.00");
        assertThat(first.invoices()).isEqualTo(2);
        assertThat(first.maxDaysOverdue()).isEqualTo(40);
        assertThat(summary.overdueClients().get(1).clientName()).isEqualTo("Cliente 12");
    }

    @Test
    void pg07CountsPendingApprovals() {
        when(approvalLookup.listForCompanyByStatus(COMPANY, ApprovalStatus.PENDING))
                .thenReturn(List.of(new ApprovalRequest(), new ApprovalRequest(), new ApprovalRequest()));

        assertThat(build().pendingApprovals()).isEqualTo(3);
    }

    private PortalSummaryDTO build() {
        return CurrentUserContext.runAsSystem(COMPANY, () -> service.buildSummary(COMPANY, TODAY));
    }

    private static Warehouse warehouse(String name) {
        Warehouse warehouse = new Warehouse();
        warehouse.setName(name);
        return warehouse;
    }

    private static Invoice sale(Warehouse warehouse, String total, LocalDate day, InvoiceStatus status) {
        Invoice invoice = new Invoice();
        invoice.setWarehouse(warehouse);
        invoice.setTotalAmount(new BigDecimal(total));
        invoice.setStatus(status);
        invoice.setCreatedAt(day.atTime(9, 0));
        return invoice;
    }

    private static Invoice receivable(Client client, String total, LocalDate dueDate) {
        Invoice invoice = sale(null, total, dueDate.minusDays(30), InvoiceStatus.APPROVED);
        invoice.setClient(client);
        invoice.setDueDate(dueDate);
        return invoice;
    }

    private static Client client(Long id, String name) {
        Client client = new Client();
        client.setId(id);
        client.setName(name);
        return client;
    }

    private static TillSession till(String operator, String current, LocalDateTime openedAt, String status) {
        TillSession session = new TillSession();
        session.setOperator(operator);
        session.setCurrentOperator(current);
        session.setOpenDate(openedAt);
        session.setStatus(status);
        return session;
    }

    private static Stock stock(String sku, String quantity, String minimum) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(sku);
        product.setMinStock(minimum == null ? null : new BigDecimal(minimum));
        Stock stock = new Stock();
        stock.setProduct(product);
        stock.setWarehouse(warehouse("Armazém Central"));
        stock.setQuantity(new BigDecimal(quantity));
        return stock;
    }
}
