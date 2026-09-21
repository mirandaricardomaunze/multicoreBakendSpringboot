package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.financeira.dto.CashFlowBucketDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.printing.CashFlowForecastPrintService;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashFlowForecastHarnessTest {

    @Mock
    private TreasuryAccountRepository accountRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private CompanyService companyService;

    private CashFlowForecastService forecastService;
    private CashFlowForecastPrintService printService;

    private final Long companyId = 1L;
    private final LocalDate today = LocalDate.of(2026, 9, 17);

    @BeforeEach
    void setUp() {
        forecastService = new CashFlowForecastService(accountRepository, invoiceRepository, purchaseRepository);
        printService = new CashFlowForecastPrintService(forecastService, companyService);
    }

    @Test
    @DisplayName("TFC-01: Cálculo da Posição Atual de Tesouraria (Caixa + Banco)")
    void tfc01_calculoPosicaoAtual() {
        TreasuryAccount cash = new TreasuryAccount();
        cash.setName("Caixa Principal");
        cash.setAccountType(TreasuryAccountType.CASH);
        cash.setBalance(new BigDecimal("15000.00"));

        TreasuryAccount bank = new TreasuryAccount();
        bank.setName("Conta Millennium BIM");
        bank.setAccountType(TreasuryAccountType.BANK);
        bank.setBalance(new BigDecimal("85000.00"));

        when(accountRepository.findByCompanyIdOrderByName(companyId)).thenReturn(List.of(cash, bank));
        when(invoiceRepository.findByCompanyId(companyId)).thenReturn(List.of());
        when(purchaseRepository.findByCompanyId(companyId)).thenReturn(List.of());

        CashFlowForecastDTO forecast = forecastService.generateForecastForCompany(companyId, today);

        assertThat(forecast.currentCashBalance()).isEqualByComparingTo("15000.00");
        assertThat(forecast.currentBankBalance()).isEqualByComparingTo("85000.00");
        assertThat(forecast.totalAvailableLiquidity()).isEqualByComparingTo("100000.00");
    }

    @Test
    @DisplayName("TFC-02: Alocação Temporal de Entradas Previsionais por Vencimento")
    void tfc02_alocacaoTemporalEntradas() {
        when(accountRepository.findByCompanyIdOrderByName(companyId)).thenReturn(List.of());
        when(purchaseRepository.findByCompanyId(companyId)).thenReturn(List.of());

        Client c = new Client();
        c.setName("Cliente VIP");

        Invoice invOverdue = createInvoice(1L, "FT-01", c, today.minusDays(5), new BigDecimal("5000.00"));
        Invoice invToday = createInvoice(2L, "FT-02", c, today, new BigDecimal("3000.00"));
        Invoice inv7d = createInvoice(3L, "FT-03", c, today.plusDays(4), new BigDecimal("10000.00"));
        Invoice inv30d = createInvoice(4L, "FT-04", c, today.plusDays(25), new BigDecimal("20000.00"));

        when(invoiceRepository.findByCompanyId(companyId)).thenReturn(List.of(invOverdue, invToday, inv7d, inv30d));

        CashFlowForecastDTO forecast = forecastService.generateForecastForCompany(companyId, today);

        assertThat(forecast.totalReceivables()).isEqualByComparingTo("38000.00");

        CashFlowBucketDTO bOverdue = findBucket(forecast, "OVERDUE");
        assertThat(bOverdue.inflows()).isEqualByComparingTo("5000.00");

        CashFlowBucketDTO bToday = findBucket(forecast, "TODAY");
        assertThat(bToday.inflows()).isEqualByComparingTo("3000.00");

        CashFlowBucketDTO b1_7 = findBucket(forecast, "DAYS_1_7");
        assertThat(b1_7.inflows()).isEqualByComparingTo("10000.00");

        CashFlowBucketDTO b16_30 = findBucket(forecast, "DAYS_16_30");
        assertThat(b16_30.inflows()).isEqualByComparingTo("20000.00");
    }

    @Test
    @DisplayName("TFC-03 e TFC-04: Alocação de Saídas e Evolução do Saldo Cumulativo")
    void tfc03_tfc04_saidasEEvolucaoCumulativa() {
        TreasuryAccount cash = new TreasuryAccount();
        cash.setAccountType(TreasuryAccountType.CASH);
        cash.setBalance(new BigDecimal("10000.00"));
        when(accountRepository.findByCompanyIdOrderByName(companyId)).thenReturn(List.of(cash));

        Client c = new Client();
        c.setName("Cliente A");
        Invoice inv = createInvoice(1L, "FT-10", c, today.plusDays(2), new BigDecimal("25000.00"));
        when(invoiceRepository.findByCompanyId(companyId)).thenReturn(List.of(inv));

        Supplier s = new Supplier();
        s.setName("Fornecedor B");
        // Compra feita há 28 dias -> vence em 2 dias (today + 2)
        Purchase pur = createPurchase(1L, "V/FT-01", s, today.minusDays(28), new BigDecimal("15000.00"));
        when(purchaseRepository.findByCompanyId(companyId)).thenReturn(List.of(pur));

        CashFlowForecastDTO forecast = forecastService.generateForecastForCompany(companyId, today);

        CashFlowBucketDTO b1_7 = findBucket(forecast, "DAYS_1_7");
        assertThat(b1_7.inflows()).isEqualByComparingTo("25000.00");
        assertThat(b1_7.outflows()).isEqualByComparingTo("15000.00");
        assertThat(b1_7.netMovement()).isEqualByComparingTo("10000.00");

        // Saldo inicial 10000 + movimento líquido 10000 = 20000
        assertThat(b1_7.projectedCumulativeBalance()).isEqualByComparingTo("20000.00");
    }

    @Test
    @DisplayName("TFC-05: Deteção de Rutura de Caixa e Alerta Crítico")
    void tfc05_deteccaoRuturaCaixa() {
        TreasuryAccount cash = new TreasuryAccount();
        cash.setAccountType(TreasuryAccountType.CASH);
        cash.setBalance(new BigDecimal("5000.00"));
        when(accountRepository.findByCompanyIdOrderByName(companyId)).thenReturn(List.of(cash));

        when(invoiceRepository.findByCompanyId(companyId)).thenReturn(List.of());

        Supplier s = new Supplier();
        s.setName("Fornecedor C");
        // Compra com vencimento imediato ou em 5 dias de 50.000 MT
        Purchase pur = createPurchase(1L, "V/FT-99", s, today.minusDays(25), new BigDecimal("50000.00"));
        when(purchaseRepository.findByCompanyId(companyId)).thenReturn(List.of(pur));

        CashFlowForecastDTO forecast = forecastService.generateForecastForCompany(companyId, today);

        assertThat(forecast.alert().status()).isEqualTo("CRITICAL");
        assertThat(forecast.alert().firstDeficitBucket()).isEqualTo("1-7 Dias");
        // Saldo inicial 5000 - 50000 = défice de 45000 MT
        assertThat(forecast.alert().maxDeficitAmount()).isEqualByComparingTo("45000.00");
        assertThat(forecast.alert().recommendation()).isNotBlank();
    }

    @Test
    @DisplayName("TFC-06: Geração do Relatório Executivo de Fluxo de Caixa em PDF")
    void tfc06_geracaoPdfExecutivo() {
        Company company = new Company();
        company.setName("Multicore Teste Lda");
        company.setTaxId("123456789");
        when(companyService.getCompanyById(anyLong())).thenReturn(company);

        TreasuryAccount cash = new TreasuryAccount();
        cash.setAccountType(TreasuryAccountType.CASH);
        cash.setBalance(new BigDecimal("50000.00"));
        when(accountRepository.findByCompanyIdOrderByName(companyId)).thenReturn(List.of(cash));

        when(invoiceRepository.findByCompanyId(companyId)).thenReturn(List.of());
        when(purchaseRepository.findByCompanyId(companyId)).thenReturn(List.of());

        byte[] pdfBytes = printService.render(companyId);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(500);
        // Assinatura de PDF
        String header = new String(pdfBytes, 0, 8);
        assertThat(header).startsWith("%PDF-1.");
    }

    private Invoice createInvoice(Long id, String number, Client client, LocalDate dueDate, BigDecimal outstanding) {
        Invoice inv = new Invoice();
        inv.setId(id);
        inv.setInvoiceNumber(number);
        inv.setClient(client);
        inv.setDueDate(dueDate);
        inv.setStatus(InvoiceStatus.APPROVED);
        inv.setTotalAmount(outstanding);
        inv.setAmountPaid(BigDecimal.ZERO);
        inv.setCreatedAt(dueDate.atStartOfDay());
        return inv;
    }

    private Purchase createPurchase(Long id, String number, Supplier supplier, LocalDate purchaseDate, BigDecimal outstanding) {
        Purchase pur = new Purchase();
        pur.setId(id);
        pur.setPurchaseNumber(number);
        pur.setSupplier(supplier);
        pur.setPurchaseDate(purchaseDate.atStartOfDay());
        pur.setStatus("COMPLETED");
        pur.setTotalAmount(outstanding);
        pur.setAmountPaid(BigDecimal.ZERO);
        return pur;
    }

    private CashFlowBucketDTO findBucket(CashFlowForecastDTO forecast, String code) {
        return forecast.buckets().stream()
                .filter(b -> b.bucketCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Bucket not found: " + code));
    }
}
