package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.financeira.dto.*;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CashFlowForecastService {

    private final TreasuryAccountRepository accountRepository;
    private final InvoiceRepository invoiceRepository;
    private final PurchaseRepository purchaseRepository;

    public CashFlowForecastService(
            TreasuryAccountRepository accountRepository,
            InvoiceRepository invoiceRepository,
            PurchaseRepository purchaseRepository
    ) {
        this.accountRepository = accountRepository;
        this.invoiceRepository = invoiceRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public CashFlowForecastDTO generateForecast() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return generateForecastForCompany(companyId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public CashFlowForecastDTO generateForecastForCompany(Long companyId, LocalDate referenceDate) {
        PermissionGuard.requireManagerOrAdmin("consultar projeção previsional de fluxo de caixa");
        LocalDate today = referenceDate != null ? referenceDate : LocalDate.now();

        // 1. Posição Atual de Tesouraria
        List<TreasuryAccount> accounts = companyId != null
                ? accountRepository.findByCompanyIdOrderByName(companyId)
                : accountRepository.findAll();

        BigDecimal cashBalance = BigDecimal.ZERO;
        BigDecimal bankBalance = BigDecimal.ZERO;

        for (TreasuryAccount acc : accounts) {
            BigDecimal b = acc.getBalance() != null ? acc.getBalance() : BigDecimal.ZERO;
            if (acc.getAccountType() == TreasuryAccountType.CASH) {
                cashBalance = cashBalance.add(b);
            } else {
                bankBalance = bankBalance.add(b);
            }
        }
        BigDecimal availableLiquidity = cashBalance.add(bankBalance);

        // 2. Contas a Receber (Invoices pendentes aprovadas)
        List<Invoice> invoices = companyId != null
                ? invoiceRepository.findByCompanyId(companyId)
                : invoiceRepository.findAll();

        List<CashFlowItemDTO> allReceivables = new ArrayList<>();
        BigDecimal totalReceivables = BigDecimal.ZERO;

        for (Invoice inv : invoices) {
            if (inv.getStatus() == InvoiceStatus.APPROVED && inv.outstandingAmount().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal outstanding = inv.outstandingAmount();
                LocalDate dueDate = inv.effectiveDueDate() != null ? inv.effectiveDueDate() : inv.issueDate();
                if (dueDate == null) dueDate = today;

                String bucket = determineBucket(dueDate, today);
                String clientName = inv.getClient() != null ? inv.getClient().getName() : (inv.getCustomerName() != null ? inv.getCustomerName() : "Cliente");

                allReceivables.add(new CashFlowItemDTO(
                        inv.getId(),
                        "RECEIVABLE",
                        inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "FT-" + inv.getId(),
                        clientName,
                        dueDate,
                        outstanding,
                        bucket
                ));
                totalReceivables = totalReceivables.add(outstanding);
            }
        }

        // 3. Contas a Pagar (Compras a fornecedores pendentes)
        List<Purchase> purchases = companyId != null
                ? purchaseRepository.findByCompanyId(companyId)
                : purchaseRepository.findAll();

        List<CashFlowItemDTO> allPayables = new ArrayList<>();
        BigDecimal totalPayables = BigDecimal.ZERO;

        for (Purchase pur : purchases) {
            if ("COMPLETED".equalsIgnoreCase(pur.getStatus()) && pur.getOutstanding().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal outstanding = pur.getOutstanding();
                LocalDate purchaseDate = pur.getPurchaseDate() != null ? pur.getPurchaseDate().toLocalDate() : today;
                // Vencimento de compras: 30 dias após emissão da compra
                LocalDate dueDate = purchaseDate.plusDays(30);

                String bucket = determineBucket(dueDate, today);
                String supplierName = pur.getSupplier() != null ? pur.getSupplier().getName() : "Fornecedor";

                allPayables.add(new CashFlowItemDTO(
                        pur.getId(),
                        "PAYABLE",
                        pur.getPurchaseNumber() != null ? pur.getPurchaseNumber() : "V/FT-" + pur.getId(),
                        supplierName,
                        dueDate,
                        outstanding,
                        bucket
                ));
                totalPayables = totalPayables.add(outstanding);
            }
        }

        // 4. Agrupamento por Baldes Temporais
        List<BucketDefinition> defs = List.of(
                new BucketDefinition("OVERDUE", "Vencido"),
                new BucketDefinition("TODAY", "Hoje"),
                new BucketDefinition("DAYS_1_7", "1-7 Dias"),
                new BucketDefinition("DAYS_8_15", "8-15 Dias"),
                new BucketDefinition("DAYS_16_30", "16-30 Dias"),
                new BucketDefinition("DAYS_31_60", "31-60 Dias"),
                new BucketDefinition("DAYS_PLUS_60", "+60 Dias")
        );

        Map<String, BigDecimal> inflowsByBucket = new HashMap<>();
        Map<String, BigDecimal> outflowsByBucket = new HashMap<>();
        for (BucketDefinition def : defs) {
            inflowsByBucket.put(def.code(), BigDecimal.ZERO);
            outflowsByBucket.put(def.code(), BigDecimal.ZERO);
        }

        for (CashFlowItemDTO r : allReceivables) {
            inflowsByBucket.merge(r.bucketCode(), r.amount(), BigDecimal::add);
        }
        for (CashFlowItemDTO p : allPayables) {
            outflowsByBucket.merge(p.bucketCode(), p.amount(), BigDecimal::add);
        }

        List<CashFlowBucketDTO> bucketDTOs = new ArrayList<>();
        BigDecimal cumulative = availableLiquidity;
        String firstDeficitBucket = null;
        BigDecimal maxDeficit = BigDecimal.ZERO;

        for (BucketDefinition def : defs) {
            BigDecimal in = inflowsByBucket.get(def.code());
            BigDecimal out = outflowsByBucket.get(def.code());
            BigDecimal net = in.subtract(out);
            cumulative = cumulative.add(net);

            if (cumulative.compareTo(BigDecimal.ZERO) < 0) {
                if (firstDeficitBucket == null) {
                    firstDeficitBucket = def.label();
                }
                BigDecimal deficit = cumulative.abs();
                if (deficit.compareTo(maxDeficit) > 0) {
                    maxDeficit = deficit;
                }
            }

            bucketDTOs.add(new CashFlowBucketDTO(
                    def.code(),
                    def.label(),
                    in,
                    out,
                    net,
                    cumulative
            ));
        }

        // 5. Determinação de Alerta
        CashFlowAlertDTO alert;
        if (firstDeficitBucket != null) {
            boolean isImmediate = "Vencido".equals(firstDeficitBucket) || "Hoje".equals(firstDeficitBucket)
                    || "1-7 Dias".equals(firstDeficitBucket) || "8-15 Dias".equals(firstDeficitBucket);

            String status = isImmediate ? "CRITICAL" : "WARNING";
            String msg = String.format("Atenção: Projeção de défice de caixa de %,.2f MT com início no período '%s'.",
                    maxDeficit, firstDeficitBucket);
            String rec = isImmediate
                    ? "Acelerar cobrança urgente de faturas vencidas e renegociar pagamentos imediatos a fornecedores."
                    : "Planear reforço de fundo de maneio ou antecipação de recebimentos a médio prazo.";

            alert = new CashFlowAlertDTO(status, msg, firstDeficitBucket, maxDeficit, rec);
        } else {
            alert = new CashFlowAlertDTO(
                    "HEALTHY",
                    "Posição de liquidez saudável sem projeção de défice no horizonte de 60 dias.",
                    null,
                    BigDecimal.ZERO,
                    "Manter ritmo corrente de cobranças e conciliações bancárias periódicas."
            );
        }

        // Top 10 Receivables and Payables ordenados por montante decrescente
        List<CashFlowItemDTO> topReceivables = allReceivables.stream()
                .sorted((a, b) -> b.amount().compareTo(a.amount()))
                .limit(10)
                .collect(Collectors.toList());

        List<CashFlowItemDTO> topPayables = allPayables.stream()
                .sorted((a, b) -> b.amount().compareTo(a.amount()))
                .limit(10)
                .collect(Collectors.toList());

        BigDecimal netProjectedPosition = cumulative;

        return new CashFlowForecastDTO(
                today,
                cashBalance,
                bankBalance,
                availableLiquidity,
                totalReceivables,
                totalPayables,
                netProjectedPosition,
                bucketDTOs,
                topReceivables,
                topPayables,
                alert
        );
    }

    private String determineBucket(LocalDate dueDate, LocalDate today) {
        if (dueDate.isBefore(today)) {
            return "OVERDUE";
        }
        if (dueDate.isEqual(today)) {
            return "TODAY";
        }
        if (!dueDate.isAfter(today.plusDays(7))) {
            return "DAYS_1_7";
        }
        if (!dueDate.isAfter(today.plusDays(15))) {
            return "DAYS_8_15";
        }
        if (!dueDate.isAfter(today.plusDays(30))) {
            return "DAYS_16_30";
        }
        if (!dueDate.isAfter(today.plusDays(60))) {
            return "DAYS_31_60";
        }
        return "DAYS_PLUS_60";
    }

    private record BucketDefinition(String code, String label) {}
}
