package mz.multicore.erp.modules.crm.model;

import mz.multicore.erp.architecture.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "crm_work_sheets")
@Getter
@Setter
public class WorkSheet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket supportTicket;

    @Column(name = "technician_name", nullable = false)
    private String technicianName;

    @Column(name = "hours_worked", nullable = false)
    private BigDecimal hoursWorked;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "parts_used", length = 500)
    private String partsUsed;

    @Column(name = "parts_cost", nullable = false)
    private BigDecimal partsCost = BigDecimal.ZERO;

    /**
     * Preço/hora praticado nesta folha, gravado no momento do registo. Antes a tarifa era uma
     * constante no código e o total da folha não explicava como lá chegou — agora sai impresso na
     * folha e no PDF, e o técnico consegue justificá-lo ao cliente.
     */
    @Column(name = "hourly_rate", nullable = false)
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Column(name = "total_value", nullable = false)
    private BigDecimal totalValue = BigDecimal.ZERO;

    @Column(name = "is_billed", nullable = false)
    private Boolean isBilled = false;

    /**
     * Folha anulada. Anular não apaga — uma folha errada tem de continuar visível com o motivo,
     * senão o cliente pergunta pelo trabalho registado na semana passada e não há rasto nenhum.
     * Só folhas <b>por faturar</b> podem ser anuladas.
     */
    @Column(name = "voided", nullable = false)
    private boolean voided = false;

    @Column(name = "void_reason", length = 500)
    private String voidReason;
}
