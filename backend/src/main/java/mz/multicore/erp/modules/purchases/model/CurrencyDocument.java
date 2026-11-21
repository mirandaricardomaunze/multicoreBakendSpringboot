package mz.multicore.erp.modules.purchases.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.CurrencyCode;

import java.math.BigDecimal;

/**
 * Purchase document that snapshots its currency and exchange rate (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.3).
 * Shared by {@link PurchaseOrder} and {@link Purchase} so both map the same two columns the same way.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class CurrencyDocument extends BaseEntity {

    /** Moeda do documento (fotografia); preços e totais das linhas estão nesta moeda. */
    @Column(name = "currency", nullable = false, length = 3,
            columnDefinition = "varchar(3) default 'MZN' not null")
    private String currency = CurrencyCode.DEFAULT_CODE;

    /** Câmbio usado no documento (fotografia, nunca ligação à tabela): moeda base vale 1. */
    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 4,
            columnDefinition = "numeric(18,4) default 1 not null")
    private BigDecimal exchangeRate = DocumentCurrency.BASE_RATE;

    /** Fotografa a moeda e o câmbio decididos para este documento. */
    public void applyDocumentCurrency(DocumentCurrency documentCurrency) {
        this.currency = documentCurrency.currency();
        this.exchangeRate = documentCurrency.exchangeRate();
    }
}
