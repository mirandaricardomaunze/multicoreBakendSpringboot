package mz.multicore.erp.modules.fleet.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.comercial.model.Client;

import java.math.BigDecimal;

@Entity
@Table(name = "fleet_manifest_stops")
@Getter
@Setter
public class FleetManifestStop extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manifest_id", nullable = false)
    private FleetManifest manifest;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "box_count", nullable = false, precision = 12, scale = 2)
    private BigDecimal boxCount = BigDecimal.ZERO.setScale(2);

    @Column(name = "package_count", nullable = false, precision = 12, scale = 2)
    private BigDecimal packageCount = BigDecimal.ZERO.setScale(2);

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "amount_to_collect", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountToCollect = BigDecimal.ZERO;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;
}
