package mz.multicore.erp.modules.hr.model;

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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import mz.multicore.erp.architecture.BaseEntity;
import mz.multicore.erp.modules.company.model.Company;

import java.time.LocalDateTime;

/**
 * O mês de ponto de uma empresa: aberto enquanto se marca, fechado quando se apura.
 * Ver docs/RH_COMPLETO_SPEC.md §B2.
 *
 * <p>Guarda o <b>estado do período</b>, não os totais: as horas apuram-se sempre das marcações, que
 * são a origem. Gravar os totais criaria uma segunda verdade que se desactualiza à primeira
 * correcção — o mesmo erro que a caducidade gravada do contrato evita.
 */
@Entity
@Table(name = "time_sheets")
@Getter
@Setter
public class TimeSheet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "year_reference", nullable = false)
    private int year;

    @Column(name = "month_reference", nullable = false)
    private int month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TimeSheetStatus status = TimeSheetStatus.ABERTA;

    @Column(name = "closed_by", length = 120)
    private String closedBy;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /** Porque foi reaberto. Um fecho que se desfaz sem explicação não é um fecho. */
    @Column(name = "reopen_reason", length = 300)
    private String reopenReason;

    public boolean isClosed() {
        return status.isClosed();
    }
}
