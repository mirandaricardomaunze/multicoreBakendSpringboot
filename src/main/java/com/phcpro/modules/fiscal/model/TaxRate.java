package com.phcpro.modules.fiscal.model;

import com.phcpro.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Taxa fiscal configurável (IVA, Retenção, IRPC, ICE).
 * O valor é guardado como fração — por exemplo 0.16 para 16%, 0.05 para 5%.
 */
@Entity
@Table(name = "tax_rates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"code"})
})
@Getter
@Setter
public class TaxRate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;     // Ex: "IVA16", "IVA5", "RF_SERV", "IRPC32"

    @Column(name = "name", nullable = false)
    private String name;     // Ex: "IVA Normal (16%)"

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TaxType type;

    @Column(name = "rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal rate;   // 0.0000 – 9.9999  (16% -> 0.1600)

    @Column(name = "legal_basis", length = 300)
    private String legalBasis; // ex: "DL 8/2024, art. 9"

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
