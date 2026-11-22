package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.modules.purchases.dto.CurrencyBalanceDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementLineDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.Supplier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Builds a supplier's statement for a period from its purchases and payments, with the opening balance and the
 * running balance per movement. Balances are in the base currency (at each document's rate); every movement also
 * carries its amount in the document currency, and the statement closes with the debt per currency (SPEC §3.3).
 */
@Component
public class SupplierStatementMovementBuilder {

    public SupplierStatementDTO buildStatement(Supplier supplier, LocalDate from, LocalDate to, List<Purchase> purchases) {
        Long supplierId = supplier.getId();
        List<RawSupplierMovement> events = new ArrayList<>();

        for (Purchase p : purchases) {
            if (p.getSupplier() == null || !supplierId.equals(p.getSupplier().getId())) {
                continue;
            }
            if ("CANCELLED".equalsIgnoreCase(p.getStatus())) {
                continue;
            }

            LocalDate pDate = p.getPurchaseDate() != null ? p.getPurchaseDate().toLocalDate()
                    : (p.getCreatedAt() != null ? p.getCreatedAt().toLocalDate() : LocalDate.now());
            LocalDateTime ts = p.getCreatedAt() != null ? p.getCreatedAt()
                    : (p.getPurchaseDate() != null ? p.getPurchaseDate() : pDate.atStartOfDay());
            PurchaseBusinessUnit unit = PurchaseBusinessUnit.of(p);

            String pNum = p.getPurchaseNumber() != null ? p.getPurchaseNumber() : "-";
            events.add(new RawSupplierMovement(
                    pDate, "V/FT", pNum, "Factura de Fornecedor / Compra", pNum,
                    BigDecimal.ZERO, p.baseTotal(), ts, 1, unit, p,
                    p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO
            ));

            if (p.getAmountPaid() != null && p.getAmountPaid().signum() > 0) {
                events.add(new RawSupplierMovement(
                        pDate, "PG", "PG/" + pNum, "Pagamento / Liquidação de Factura", pNum,
                        p.getBaseAmountPaid(), BigDecimal.ZERO, ts.plusSeconds(1), 2, unit, p,
                        p.getAmountPaid().negate()
                ));
            }
        }

        events.sort(Comparator.comparing(RawSupplierMovement::date, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(RawSupplierMovement::timestamp, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(RawSupplierMovement::orderPriority));

        BigDecimal openingBalance = BigDecimal.ZERO;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        List<SupplierStatementLineDTO> lines = new ArrayList<>();
        Map<String, CurrencyBalanceDTO> byCurrency = new TreeMap<>();

        for (RawSupplierMovement mov : events) {
            if (mov.date().isBefore(from)) {
                openingBalance = openingBalance.add(mov.baseChange());
            }
            if (!mov.date().isAfter(to)) {
                byCurrency.merge(mov.purchase().getCurrency(), mov.balanceChange(), SupplierStatementMovementBuilder::sum);
            }
        }

        BigDecimal running = openingBalance;
        for (RawSupplierMovement mov : events) {
            if (!mov.date().isBefore(from) && !mov.date().isAfter(to)) {
                running = running.add(mov.baseChange());
                totalDebits = totalDebits.add(mov.debit());
                totalCredits = totalCredits.add(mov.credit());

                lines.add(new SupplierStatementLineDTO(
                        mov.date(), mov.type(), mov.documentNumber(), mov.description(),
                        mov.reference(), mov.debit(), mov.credit(), running,
                        mov.unit().id(), mov.unit().name(), mov.purchase().getCurrency(),
                        mov.purchase().getExchangeRate(), mov.signedDocumentAmount().abs()
                ));
            }
        }

        BigDecimal closingBalance = openingBalance.add(totalCredits).subtract(totalDebits);

        return new SupplierStatementDTO(
                supplier.getId(), supplier.getName(), supplier.getTaxId(),
                supplier.getAddress(), supplier.getEmail(), supplier.getPhone(),
                from, to, openingBalance, totalDebits, totalCredits, closingBalance, lines,
                byCurrency.values().stream().filter(b -> b.outstanding().signum() != 0).toList()
        );
    }

    private static CurrencyBalanceDTO sum(CurrencyBalanceDTO a, CurrencyBalanceDTO b) {
        return new CurrencyBalanceDTO(a.currency(), a.outstanding().add(b.outstanding()),
                a.baseOutstanding().add(b.baseOutstanding()));
    }

    private record RawSupplierMovement(
            LocalDate date, String type, String documentNumber, String description, String reference,
            BigDecimal debit, BigDecimal credit, LocalDateTime timestamp, int orderPriority,
            PurchaseBusinessUnit unit, Purchase purchase, BigDecimal signedDocumentAmount
    ) {
        /** Variação da dívida em moeda base: compras aumentam, pagamentos baixam. */
        BigDecimal baseChange() {
            return credit.subtract(debit);
        }

        CurrencyBalanceDTO balanceChange() {
            return new CurrencyBalanceDTO(purchase.getCurrency(), signedDocumentAmount, baseChange());
        }
    }
}
