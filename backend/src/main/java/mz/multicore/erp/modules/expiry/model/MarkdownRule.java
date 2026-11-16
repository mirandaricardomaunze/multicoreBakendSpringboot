package mz.multicore.erp.modules.expiry.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.ProductCategory;
import mz.multicore.erp.modules.company.model.Company;

import java.math.BigDecimal;

/** Markdown rule: from {@code daysBefore} days before expiry, sell with {@code percent} off (SA-C14). */
@Entity
@Table(name = "markdown_rules")
@Getter
@Setter
public class MarkdownRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Column(name = "days_before", nullable = false)
    private int daysBefore;

    @Column(name = "percent_value", nullable = false, precision = 5, scale = 2)
    private BigDecimal percent;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
