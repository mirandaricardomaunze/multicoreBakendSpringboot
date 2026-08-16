package mz.multicore.erp.modules.accounting.model;

import java.math.BigDecimal;

/**
 * Natureza do saldo de uma conta — de que lado é que ela "cresce".
 *
 * <p>É o que decide se um saldo é <b>devedor</b> ou <b>credor</b> no balancete: numa conta de
 * natureza devedora (Caixa, Clientes, Custos) o saldo é {@code débito − crédito}; numa credora
 * (Fornecedores, Capital, Proveitos) é o contrário. Sem isto, metade do balancete sairia com o
 * sinal trocado.
 */
public enum AccountNature {

    /** Cresce a débito: activo e custos. */
    DEVEDORA,

    /** Cresce a crédito: passivo, capital próprio e proveitos. */
    CREDORA;

    /** Saldo da conta, com o sinal certo para a sua natureza. */
    public BigDecimal balanceOf(BigDecimal totalDebit, BigDecimal totalCredit) {
        BigDecimal debit = totalDebit == null ? BigDecimal.ZERO : totalDebit;
        BigDecimal credit = totalCredit == null ? BigDecimal.ZERO : totalCredit;
        return this == DEVEDORA ? debit.subtract(credit) : credit.subtract(debit);
    }
}
