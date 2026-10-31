package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.modules.purchases.dto.SupplierStatementDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementLineDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.Supplier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

            Set<String> bus = p.getLines() != null ? p.getLines().stream()
                    .map(l -> l.getProduct() != null && l.getProduct().getBusinessUnit() != null ? l.getProduct().getBusinessUnit().getName() : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()) : Collections.emptySet();
            String buName = bus.isEmpty() ? null : (bus.size() == 1 ? bus.iterator().next() : "Misto");
            Long buId = bus.size() == 1 && p.getLines() != null ? p.getLines().stream()
                    .map(l -> l.getProduct() != null && l.getProduct().getBusinessUnit() != null ? l.getProduct().getBusinessUnit().getId() : null)
                    .filter(Objects::nonNull).findFirst().orElse(null) : null;

            String pNum = p.getPurchaseNumber() != null ? p.getPurchaseNumber() : "-";
            events.add(new RawSupplierMovement(
                    pDate, "V/FT", pNum, "Factura de Fornecedor / Compra", pNum,
                    BigDecimal.ZERO, p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO,
                    ts, 1, buId, buName
            ));

            if (p.getAmountPaid() != null && p.getAmountPaid().signum() > 0) {
                events.add(new RawSupplierMovement(
                        pDate, "PG", "PG/" + pNum, "Pagamento / Liquidação de Factura", pNum,
                        p.getAmountPaid(), BigDecimal.ZERO, ts.plusSeconds(1), 2, buId, buName
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

        for (RawSupplierMovement mov : events) {
            if (mov.date().isBefore(from)) {
                openingBalance = openingBalance.add(mov.credit()).subtract(mov.debit());
            }
        }

        BigDecimal running = openingBalance;
        for (RawSupplierMovement mov : events) {
            if (!mov.date().isBefore(from) && !mov.date().isAfter(to)) {
                running = running.add(mov.credit()).subtract(mov.debit());
                totalDebits = totalDebits.add(mov.debit());
                totalCredits = totalCredits.add(mov.credit());

                lines.add(new SupplierStatementLineDTO(
                        mov.date(), mov.type(), mov.documentNumber(), mov.description(),
                        mov.reference(), mov.debit(), mov.credit(), running,
                        mov.businessUnitId(), mov.businessUnitName()
                ));
            }
        }

        BigDecimal closingBalance = openingBalance.add(totalCredits).subtract(totalDebits);

        return new SupplierStatementDTO(
                supplier.getId(), supplier.getName(), supplier.getTaxId(),
                supplier.getAddress(), supplier.getEmail(), supplier.getPhone(),
                from, to, openingBalance, totalDebits, totalCredits, closingBalance, lines
        );
    }

    private record RawSupplierMovement(
            LocalDate date,
            String type,
            String documentNumber,
            String description,
            String reference,
            BigDecimal debit,
            BigDecimal credit,
            LocalDateTime timestamp,
            int orderPriority,
            Long businessUnitId,
            String businessUnitName
    ) {}
}
