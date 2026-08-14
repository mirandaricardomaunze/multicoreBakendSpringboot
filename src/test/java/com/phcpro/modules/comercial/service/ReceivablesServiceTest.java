package com.phcpro.modules.comercial.service;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.comercial.dto.AgingSummaryDTO;
import com.phcpro.modules.comercial.dto.ClientAgingDTO;
import com.phcpro.modules.comercial.model.AgingBucket;
import com.phcpro.modules.comercial.model.Client;
import com.phcpro.modules.comercial.model.Invoice;
import com.phcpro.modules.comercial.model.InvoiceStatus;
import com.phcpro.modules.comercial.repository.InvoiceRepository;
import com.phcpro.modules.company.model.Company;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Mapa de antiguidade de saldos (VA-20..VA-25). Ver docs/VENCIMENTO_ANTIGUIDADE_SPEC.md.
 */
class ReceivablesServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final LocalDate HOJE = LocalDate.of(2026, 8, 14);

    private InvoiceRepository invoiceRepository;
    private ReceivablesService service;
    private Company company;

    @BeforeEach
    void setUp() {
        invoiceRepository = mock(InvoiceRepository.class);
        service = new ReceivablesService(invoiceRepository);
        company = new Company();
        company.setId(COMPANY_ID);
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private Client client(Long id, String name) {
        Client client = new Client();
        client.setId(id);
        client.setName(name);
        client.setTaxId("NUIT" + id);
        return client;
    }

    private Invoice invoice(Client client, String total, String paid, InvoiceStatus status, LocalDate due) {
        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setClient(client);
        invoice.setTotalAmount(new BigDecimal(total));
        invoice.setAmountPaid(new BigDecimal(paid));
        invoice.setStatus(status);
        invoice.setDueDate(due);
        return invoice;
    }

    @Test // VA-20
    void reparteOSaldoPelosEscaloesCertos() {
        Client cliente = client(5L, "Loja Central");
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                invoice(cliente, "100.00", "0", InvoiceStatus.APPROVED, HOJE.plusDays(10)),   // corrente
                invoice(cliente, "200.00", "0", InvoiceStatus.APPROVED, HOJE.minusDays(10)),  // 1-30
                invoice(cliente, "300.00", "0", InvoiceStatus.APPROVED, HOJE.minusDays(45)),  // 31-60
                invoice(cliente, "400.00", "0", InvoiceStatus.APPROVED, HOJE.minusDays(75)),  // 61-90
                invoice(cliente, "500.00", "0", InvoiceStatus.APPROVED, HOJE.minusDays(200))  // >90
        ));

        AgingSummaryDTO summary = service.getAging(HOJE);

        assertEquals(new BigDecimal("100.00"), amountOf(summary, AgingBucket.CORRENTE));
        assertEquals(new BigDecimal("200.00"), amountOf(summary, AgingBucket.ATE_30));
        assertEquals(new BigDecimal("300.00"), amountOf(summary, AgingBucket.DE_31_A_60));
        assertEquals(new BigDecimal("400.00"), amountOf(summary, AgingBucket.DE_61_A_90));
        assertEquals(new BigDecimal("500.00"), amountOf(summary, AgingBucket.MAIS_DE_90));
        assertEquals(new BigDecimal("1500.00"), summary.total());
        assertEquals(new BigDecimal("1400.00"), summary.overdueTotal(), "só o corrente fica de fora do atraso");
    }

    @Test // VA-21
    void osCincoEscaloesAparecemSempreAindaQueVazios() {
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of());

        AgingSummaryDTO summary = service.getAging(HOJE);

        assertEquals(AgingBucket.values().length, summary.buckets().size());
        assertEquals(BigDecimal.ZERO, summary.total());
        assertEquals(BigDecimal.ZERO, summary.overdueTotal());
        assertTrue(summary.clients().isEmpty());
    }

    @Test // VA-22
    void contaApenasOSaldoPorLiquidar_naoOTotalDaFatura() {
        Client cliente = client(5L, "Loja Central");
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                invoice(cliente, "1000.00", "400.00", InvoiceStatus.PARTIALLY_PAID, HOJE.minusDays(5))));

        AgingSummaryDTO summary = service.getAging(HOJE);

        assertEquals(new BigDecimal("600.00"), summary.total(), "o recibo parcial já entrou na caixa");
        assertEquals(new BigDecimal("600.00"), amountOf(summary, AgingBucket.ATE_30));
    }

    @Test // VA-23
    void faturasNaoCobraveisFicamDeFora() {
        Client cliente = client(5L, "Loja Central");
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                invoice(cliente, "100.00", "100.00", InvoiceStatus.PAID, HOJE.minusDays(300)),
                invoice(cliente, "200.00", "0", InvoiceStatus.CANCELLED, HOJE.minusDays(300)),
                invoice(cliente, "300.00", "0", InvoiceStatus.DRAFT, HOJE.minusDays(300)),
                invoice(cliente, "50.00", "0", InvoiceStatus.APPROVED, HOJE.minusDays(300))));

        AgingSummaryDTO summary = service.getAging(HOJE);

        assertEquals(new BigDecimal("50.00"), summary.total(),
                "paga, anulada e rascunho não são dívida — só a aprovada por receber");
    }

    @Test // VA-24
    void reparteEOrdenaPorCliente_doMaiorDevedorParaOMenor() {
        Client pequeno = client(1L, "Quiosque A");
        Client grande = client(2L, "Distribuidora B");
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                invoice(pequeno, "150.00", "0", InvoiceStatus.APPROVED, HOJE.minusDays(3)),
                invoice(grande, "800.00", "0", InvoiceStatus.APPROVED, HOJE.minusDays(120)),
                invoice(grande, "200.00", "0", InvoiceStatus.APPROVED, HOJE.plusDays(30))));

        AgingSummaryDTO summary = service.getAging(HOJE);

        assertEquals(2, summary.clients().size());
        ClientAgingDTO primeiro = summary.clients().get(0);
        assertEquals("Distribuidora B", primeiro.clientName());
        assertEquals(new BigDecimal("1000.00"), primeiro.total());
        assertEquals(new BigDecimal("800.00"), primeiro.overdue(), "os 200 ainda estão no prazo");
        assertEquals(new BigDecimal("200.00"), primeiro.corrente());
        assertEquals(new BigDecimal("800.00"), primeiro.maisDe90());
        assertEquals(120, primeiro.maxDaysOverdue());
        assertEquals(HOJE.minusDays(120), primeiro.oldestDueDate());
    }

    @Test // VA-25
    void aDataDeReferenciaMandaNoCalculo() {
        Client cliente = client(5L, "Loja Central");
        when(invoiceRepository.findByCompanyId(COMPANY_ID)).thenReturn(List.of(
                invoice(cliente, "100.00", "0", InvoiceStatus.APPROVED, HOJE)));

        assertEquals(BigDecimal.ZERO, service.getAging(HOJE).overdueTotal(), "hoje ainda vence hoje");
        assertEquals(new BigDecimal("100.00"), service.getAging(HOJE.plusDays(40)).overdueTotal());
        assertEquals(new BigDecimal("100.00"),
                amountOf(service.getAging(HOJE.plusDays(15)), AgingBucket.ATE_30));
        assertEquals(new BigDecimal("100.00"),
                amountOf(service.getAging(HOJE.plusDays(40)), AgingBucket.DE_31_A_60),
                "a mesma fatura envelhece de escalão com o passar do tempo");
    }

    private BigDecimal amountOf(AgingSummaryDTO summary, AgingBucket bucket) {
        return summary.buckets().stream()
                .filter(b -> b.bucket() == bucket)
                .findFirst()
                .orElseThrow()
                .amount();
    }
}
