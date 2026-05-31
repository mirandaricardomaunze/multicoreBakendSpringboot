package com.phcpro.modules.fiscal.model;

import com.phcpro.architecture.BaseEntity;
import com.phcpro.modules.company.model.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registo de retenção na fonte aplicada ao pagar a um fornecedor /
 * prestador de serviços. O sistema mantém o histórico para a declaração
 * mensal entregue à Autoridade Tributária.
 */
@Entity
@Table(name = "withholding_records")
@Getter
@Setter
public class WithholdingRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate = LocalDate.now();

    @Column(name = "beneficiary_name", nullable = false)
    private String beneficiaryName;       // Quem recebe o pagamento

    @Column(name = "beneficiary_tax_id", length = 30)
    private String beneficiaryTaxId;      // NUIT

    @Column(name = "service_description", nullable = false, length = 300)
    private String serviceDescription;

    @Column(name = "base_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal baseAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate = BigDecimal.ZERO;   // 0.10 = 10%

    @Column(name = "tax_category", length = 50)
    private String taxCategory;           // SERVICES / RENT / NON_RESIDENT / OTHER

    @Column(name = "withheld_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal withheldAmount = BigDecimal.ZERO;

    @Column(name = "net_paid", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPaid = BigDecimal.ZERO;   // baseAmount − withheldAmount

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";    // PENDING / DELIVERED / CANCELLED

    @Column(name = "delivered_at")
    private LocalDate deliveredAt;        // Data em que foi entregue à AT
}
