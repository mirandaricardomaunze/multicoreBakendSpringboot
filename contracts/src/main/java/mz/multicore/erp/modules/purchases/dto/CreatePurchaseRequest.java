package mz.multicore.erp.modules.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreatePurchaseRequest(
        @NotNull(message = "Fornecedor é obrigatório.") Long supplierId,
        @NotNull(message = "Armazém é obrigatório.") Long warehouseId,
        @NotNull(message = "Empresa é obrigatória.") Long companyId,
        // Conta de tesouraria para pagamento imediato. Se null, a compra fica a crédito (conta a pagar).
        Long financeAccountId,
        @NotEmpty(message = "A compra deve conter pelo menos uma linha.") @Valid List<CreatePurchaseLineRequest> lines,
        /** Moeda do documento (MZN, USD, ZAR, EUR); vazia = moeda base da empresa. */
        String currency,
        /** Câmbio escrito pelo utilizador; vazio = o proposto pela tabela de câmbios. */
        BigDecimal exchangeRate
) {
    /** Compra na moeda base da empresa (anterior à moeda estrangeira). */
    public CreatePurchaseRequest(Long supplierId, Long warehouseId, Long companyId, Long financeAccountId,
                                 List<CreatePurchaseLineRequest> lines) {
        this(supplierId, warehouseId, companyId, financeAccountId, lines, null, null);
    }
}
