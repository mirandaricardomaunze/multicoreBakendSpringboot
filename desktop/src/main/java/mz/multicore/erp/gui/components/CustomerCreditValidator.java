package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.comercial.dto.ClientDTO;

import java.math.BigDecimal;

/**
 * Validador canónico de disponibilidade de crédito e risco de cliente.
 */
public final class CustomerCreditValidator {

    public enum CreditCheckStatus {
        APPROVED("Crédito aprovado"),
        BLOCKED_OVERDUE("Bloqueado: cliente possui faturas em mora vencidas"),
        BLOCKED_LIMIT_EXCEEDED("Bloqueado: limite de crédito excedido"),
        NO_CREDIT_ALLOWED("Cliente sem autorização para vendas a crédito");

        private final String message;
        CreditCheckStatus(String message) { this.message = message; }
        public String getMessage() { return message; }
    }

    public record CreditAssessment(
            CreditCheckStatus status,
            BigDecimal creditLimit,
            BigDecimal currentDebt,
            BigDecimal availableCredit,
            String details
    ) {
        public boolean isApproved() {
            return status == CreditCheckStatus.APPROVED;
        }
    }

    private CustomerCreditValidator() {}

    /**
     * Avalia a elegibilidade de concessão de crédito para uma nova venda/encomenda.
     *
     * @param creditLimit limite de crédito atribuído ao cliente (pode ser null se ilimitado)
     * @param currentDebt dívida total actual em aberto do cliente
     * @param newAmount valor do novo documento a emitir a crédito
     * @param hasOverdue flag indicando se existem faturas vencidas em mora
     * @return avaliação detalhada com status e saldos
     */
    public static CreditAssessment evaluate(
            BigDecimal creditLimit,
            BigDecimal currentDebt,
            BigDecimal newAmount,
            boolean hasOverdue
    ) {
        BigDecimal debt = currentDebt != null ? currentDebt : BigDecimal.ZERO;
        BigDecimal amount = newAmount != null ? newAmount : BigDecimal.ZERO;

        if (hasOverdue) {
            BigDecimal available = creditLimit != null ? creditLimit.subtract(debt) : BigDecimal.ZERO;
            return new CreditAssessment(
                    CreditCheckStatus.BLOCKED_OVERDUE,
                    creditLimit, debt, available,
                    "Cliente tem faturas pendentes fora do prazo. Regularize os pagamentos em atraso antes de conceder novo crédito."
            );
        }

        if (creditLimit == null) {
            // Sem limite configurado = crédito flexível/ilimitado
            return new CreditAssessment(
                    CreditCheckStatus.APPROVED,
                    null, debt, BigDecimal.valueOf(Double.MAX_VALUE),
                    "Cliente com crédito flexível sem teto restritivo."
            );
        }

        if (creditLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return new CreditAssessment(
                    CreditCheckStatus.NO_CREDIT_ALLOWED,
                    creditLimit, debt, BigDecimal.ZERO,
                    "Cliente configurado exclusivamente para pronto pagamento (limite de crédito 0,00 MT)."
            );
        }

        BigDecimal available = creditLimit.subtract(debt);
        BigDecimal projectedDebt = debt.add(amount);

        if (projectedDebt.compareTo(creditLimit) > 0) {
            return new CreditAssessment(
                    CreditCheckStatus.BLOCKED_LIMIT_EXCEEDED,
                    creditLimit, debt, available,
                    String.format("Limite de crédito excedido. Limite: %,.2f MT | Dívida atual: %,.2f MT | Disponível: %,.2f MT | Requerido: %,.2f MT.",
                            creditLimit, debt, available, amount)
            );
        }

        return new CreditAssessment(
                CreditCheckStatus.APPROVED,
                creditLimit, debt, available.subtract(amount),
                "Crédito disponível suficiente."
        );
    }

    /**
     * Validação preliminar na interface antes de submeter uma venda a crédito.
     * @param client cliente seleccionado (pode ser null)
     * @param saleAmount valor da venda
     */
    public static CreditAssessment validateCreditPreFlight(ClientDTO client, BigDecimal saleAmount) {
        if (client == null) {
            return new CreditAssessment(
                    CreditCheckStatus.NO_CREDIT_ALLOWED,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "Vendas a crédito exigem a seleção de um cliente cadastrado."
            );
        }
        if (client.creditLimit() != null && client.creditLimit().compareTo(BigDecimal.ZERO) <= 0) {
            return new CreditAssessment(
                    CreditCheckStatus.NO_CREDIT_ALLOWED,
                    client.creditLimit(), BigDecimal.ZERO, BigDecimal.ZERO,
                    "O cliente '" + client.name() + "' não tem autorização para vendas a crédito (limite de 0,00 MT)."
            );
        }
        return evaluate(client.creditLimit(), BigDecimal.ZERO, saleAmount, false);
    }
}
