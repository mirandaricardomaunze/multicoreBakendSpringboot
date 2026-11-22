package mz.multicore.erp.modules.purchases.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.CurrencyCode;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Purchase document that snapshots its currency and exchange rate (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3).
 * Shared by {@link PurchaseOrder} and {@link Purchase} so both map the same two columns the same way.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class CurrencyDocument extends BaseEntity {

    private static final int MONEY_SCALE = 2;

    /** Moeda do documento (fotografia); preços e totais das linhas estão nesta moeda. */
    @Column(name = "currency", nullable = false, length = 3,
            columnDefinition = "varchar(3) default 'MZN' not null")
    private String currency = CurrencyCode.DEFAULT_CODE;

    /** Câmbio usado no documento (fotografia, nunca ligação à tabela): moeda base vale 1. */
    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 4,
            columnDefinition = "numeric(18,4) default 1 not null")
    private BigDecimal exchangeRate = DocumentCurrency.BASE_RATE;

    /** Total do documento em moeda base = total × câmbio, 2 casas HALF_UP (fotografia, como o câmbio). */
    @Column(name = "base_total_amount", nullable = false, precision = 14, scale = 2,
            columnDefinition = "numeric(14,2) default 0 not null")
    private BigDecimal baseTotalAmount = BigDecimal.ZERO;

    /** Fotografa a moeda e o câmbio decididos para este documento. */
    public void applyDocumentCurrency(DocumentCurrency documentCurrency) {
        this.currency = documentCurrency.currency();
        this.exchangeRate = documentCurrency.exchangeRate();
    }

    /** Valor na moeda do documento convertido para a moeda base ao câmbio do documento (2 casas, HALF_UP). */
    public BigDecimal toBaseAmount(BigDecimal documentAmount) {
        return documentAmount.multiply(exchangeRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Custo unitário em moeda base que entra no stock e no custo médio = preço unitário × câmbio, sem arredondar
     * aqui: o custo médio arredonda uma só vez, no fim da média.
     */
    public BigDecimal toBaseUnitCost(BigDecimal documentUnitPrice) {
        return documentUnitPrice.multiply(exchangeRate);
    }

    /**
     * Grava o total na moeda do documento (2 casas, HALF_UP) e, com ele, o total em moeda base, para que os dois
     * nunca fiquem desencontrados.
     */
    public void applyTotal(BigDecimal documentTotal) {
        setTotalAmount(documentTotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        this.baseTotalAmount = toBaseAmount(getTotalAmount());
    }

    /**
     * Total em moeda base para contas a pagar e extracto. Um documento gravado antes de a coluna existir (H2 de dev
     * sem Flyway, onde ficou 0) converte o total ao seu câmbio.
     */
    public BigDecimal baseTotal() {
        if (baseTotalAmount != null && baseTotalAmount.signum() != 0) return baseTotalAmount;
        return toBaseAmount(getTotalAmount() == null ? BigDecimal.ZERO : getTotalAmount());
    }

    public abstract BigDecimal getTotalAmount();

    public abstract void setTotalAmount(BigDecimal totalAmount);
}
