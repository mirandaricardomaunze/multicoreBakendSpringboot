package mz.multicore.erp.modules.purchases.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * "Custos de Importação" document (internal series CI, docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.1). It links to
 * one or more received purchases of its company; a purchase may be linked to several CI documents.
 */
@Entity
@Table(name = "landed_cost_documents", uniqueConstraints = @UniqueConstraint(
        name = "uk_landed_cost_documents_company_number", columnNames = {"company_id", "document_number"}))
@Getter
@Setter
public class LandedCostDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_number", nullable = false, length = 40)
    private String documentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "document_date", nullable = false)
    private LocalDate documentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_method", nullable = false, length = 20)
    private LandedCostAllocationMethod allocationMethod = LandedCostAllocationMethod.VALOR;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "landed_cost_document_purchases",
            joinColumns = @JoinColumn(name = "landed_cost_document_id"),
            inverseJoinColumns = @JoinColumn(name = "purchase_id"))
    @OrderColumn(name = "sort_order")
    private List<Purchase> purchases = new ArrayList<>();

    /** Linhas de custo pela ordem escrita; cada uma com a sua dívida ao fornecedor (§4.1). */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "landed_cost_document_id", nullable = false)
    @OrderColumn(name = "sort_order")
    private List<LandedCostDocumentCharge> charges = new ArrayList<>();
}
