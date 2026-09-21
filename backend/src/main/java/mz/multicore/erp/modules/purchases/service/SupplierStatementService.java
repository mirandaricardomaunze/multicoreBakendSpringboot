package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementLineDTO;
import mz.multicore.erp.modules.purchases.model.Purchase;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.PurchaseRepository;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Serviço de geração de extrato de conta corrente de fornecedores (contas a pagar).
 */
@Service
public class SupplierStatementService {

    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;

    public SupplierStatementService(
            SupplierRepository supplierRepository,
            PurchaseRepository purchaseRepository
    ) {
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
    }

    @Transactional(readOnly = true)
    public SupplierStatementDTO getStatement(Long supplierId, LocalDate startDate, LocalDate endDate) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (supplierId == null) {
            throw new BusinessRuleException("Identificador de fornecedor é obrigatório.");
        }

        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new BusinessRuleException("Fornecedor não encontrado: " + supplierId));

        LocalDate from = startDate != null ? startDate : LocalDate.now().withDayOfYear(1);
        LocalDate to = endDate != null ? endDate : LocalDate.now();

        List<RawSupplierMovement> events = new ArrayList<>();

        List<Purchase> purchases = purchaseRepository.findByCompanyId(companyId);
        for (Purchase p : purchases) {
            if (p.getSupplier() == null || !supplierId.equals(p.getSupplier().getId())) {
                continue;
            }
            if ("CANCELLED".equalsIgnoreCase(p.getStatus())) {
                continue;
            }

            LocalDate pDate = p.getPurchaseDate().toLocalDate();
            LocalDateTime ts = p.getCreatedAt() != null ? p.getCreatedAt() : p.getPurchaseDate();

            // 1. Factura de Compra (Crédito = Dívida a pagar)
            events.add(new RawSupplierMovement(
                    pDate,
                    "V/FT",
                    p.getPurchaseNumber(),
                    "Factura de Fornecedor / Compra",
                    p.getPurchaseNumber(),
                    BigDecimal.ZERO,
                    p.getTotalAmount() != null ? p.getTotalAmount() : BigDecimal.ZERO,
                    ts,
                    1
            ));

            // 2. Pagamento efetuado (Débito = Amortização)
            if (p.getAmountPaid() != null && p.getAmountPaid().signum() > 0) {
                events.add(new RawSupplierMovement(
                        pDate,
                        "PG",
                        "PG/" + p.getPurchaseNumber(),
                        "Pagamento / Liquidação de Factura",
                        p.getPurchaseNumber(),
                        p.getAmountPaid(),
                        BigDecimal.ZERO,
                        ts.plusSeconds(1),
                        2
                ));
            }
        }

        // Ordenação cronológica estrita
        events.sort(Comparator.comparing(RawSupplierMovement::date)
                .thenComparing(RawSupplierMovement::timestamp)
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
                        mov.date(),
                        mov.type(),
                        mov.documentNumber(),
                        mov.description(),
                        mov.reference(),
                        mov.debit(),
                        mov.credit(),
                        running
                ));
            }
        }

        BigDecimal closingBalance = openingBalance.add(totalCredits).subtract(totalDebits);

        return new SupplierStatementDTO(
                supplier.getId(),
                supplier.getName(),
                supplier.getTaxId(),
                supplier.getAddress(),
                supplier.getEmail(),
                supplier.getPhone(),
                from,
                to,
                openingBalance,
                totalDebits,
                totalCredits,
                closingBalance,
                lines
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
            int orderPriority
    ) {}
}
