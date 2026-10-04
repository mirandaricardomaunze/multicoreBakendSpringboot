package mz.multicore.erp.modules.pos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.pos.dto.MobilePaymentProvider;
import mz.multicore.erp.modules.pos.dto.MobilePaymentStatus;

import java.math.BigDecimal;

/**
 * Registo transacional de uma solicitação de pagamento móvel Push USSD.
 */
@Entity
@Table(name = "mobile_payment_transactions")
@Getter
@Setter
public class MobilePaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private MobilePaymentProvider provider;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "reference", length = 64)
    private String reference;

    @Column(name = "financial_reference", length = 64)
    private String financialReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MobilePaymentStatus status = MobilePaymentStatus.PENDING;

    @Column(name = "message", length = 255)
    private String message;

    @Column(name = "operator", length = 64)
    private String operator;
}
