package mz.multicore.erp.modules.purchases.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;

import java.math.BigDecimal;

/**
 * Linha de custo gravada num documento "Custos de Importação" (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.1):
 * tipo, fornecedor, moeda e câmbio (fotografia), valor sem IVA na moeda da linha e taxa de IVA do serviço. Cada linha
 * tem a sua dívida ao fornecedor, um documento {@link PurchaseDocumentKind#LANDED_COST} em {@code purchases} que
 * aparece nas contas a pagar e se paga com a acção normal de pagamento a fornecedor.
 */
@Entity
@Table(name = "landed_cost_charges")
@Getter
@Setter
public class LandedCostDocumentCharge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 30)
    private LandedCostType chargeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "exchange_rate", nullable = false, precision = 18, scale = 4)
    private BigDecimal exchangeRate;

    /** Valor sem IVA na moeda da linha (no IVA aduaneiro, o próprio imposto). */
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    /** Taxa de IVA do serviço em percentagem (16 = 16%); vazia sem IVA e no IVA aduaneiro. */
    @Column(name = "vat_rate_percent", precision = 7, scale = 4)
    private BigDecimal vatRatePercent;

    /** Dívida desta linha ao seu fornecedor. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payable_purchase_id", nullable = false, unique = true)
    private Purchase payable;

    /** Linha a gravar a partir do valor já validado e do fornecedor da empresa. */
    public static LandedCostDocumentCharge of(LandedCostCharge charge, Supplier supplier) {
        LandedCostDocumentCharge line = new LandedCostDocumentCharge();
        line.setChargeType(charge.type());
        line.setSupplier(supplier);
        line.setCurrency(charge.currency().currency());
        line.setExchangeRate(charge.currency().exchangeRate());
        line.setAmount(charge.amount());
        line.setVatRatePercent(charge.vatRatePercent());
        return line;
    }

    /** O valor desta linha com as regras de custo e IVA (§4.2). */
    public LandedCostCharge charge() {
        return new LandedCostCharge(chargeType, new DocumentCurrency(currency, exchangeRate), amount, vatRatePercent);
    }

    /**
     * Cria a dívida desta linha ao fornecedor: número {@code <nº do CI>-<n.º da linha>}, na moeda e ao câmbio da
     * linha, total com IVA e o IVA do serviço (no IVA aduaneiro, o valor todo). Sem armazém nem linhas: não mexe no
     * stock.
     */
    public Purchase createPayable(LandedCostDocument document, int sequence) {
        LandedCostCharge value = charge();
        Purchase debt = new Purchase();
        debt.setDocumentKind(PurchaseDocumentKind.LANDED_COST);
        debt.setCompany(document.getCompany());
        debt.setSupplier(supplier);
        debt.applyDocumentCurrency(value.currency());
        debt.setPurchaseNumber(document.getDocumentNumber() + "-" + sequence);
        debt.setPurchaseDate(document.getDocumentDate().atStartOfDay());
        debt.setDescription("Custos de Importação " + document.getDocumentNumber() + " · " + chargeType.label());
        debt.applyTotal(value.documentTotalAmount());
        debt.setTaxAmount(value.documentVatAmount());
        this.payable = debt;
        return debt;
    }
}
