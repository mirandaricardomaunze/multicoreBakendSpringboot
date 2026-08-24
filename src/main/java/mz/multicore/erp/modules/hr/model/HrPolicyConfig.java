package mz.multicore.erp.modules.hr.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDate;

/**
 * Os <b>valores legais</b> do RH que a lei fixa e que mudam com ela: direito a férias por
 * antiguidade, prazos de entrega das retenções e aviso prévio.
 * Ver docs/RH_COMPLETO_SPEC.md §6.
 *
 * <p>Molde do {@code PayrollTaxConfig} e do {@code OvertimeRateConfig}: por empresa, com vigência e
 * <b>base legal registada</b>. <b>Não é a IA que decide o número</b> — cada campo é anulável, e o
 * que não estiver configurado é dito como não configurado em vez de ser adivinhado.
 *
 * <p>Uma tabela só para os três assuntos, e não três tabelas, porque a pergunta é sempre a mesma:
 * <i>que valores legais é que esta empresa usa, desde quando, com que fundamento?</i>
 */
@Entity
@Table(name = "hr_policy_configs")
@Getter
@Setter
public class HrPolicyConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    /** Dias de férias no 1.º ano de trabalho. Nulo = por confirmar com o contabilista. */
    @Column(name = "vacation_days_year_1")
    private Integer vacationDaysYear1;

    @Column(name = "vacation_days_year_2")
    private Integer vacationDaysYear2;

    @Column(name = "vacation_days_year_3_plus")
    private Integer vacationDaysYear3Plus;

    /** Dia do mês <b>seguinte</b> ao período em que o IRPS retido tem de estar entregue. */
    @Column(name = "irps_delivery_day")
    private Integer irpsDeliveryDay;

    @Column(name = "inss_delivery_day")
    private Integer inssDeliveryDay;

    @Column(name = "notice_days_employee")
    private Integer noticeDaysEmployee;

    @Column(name = "notice_days_employer")
    private Integer noticeDaysEmployer;

    /** De onde vieram estes valores — contra o quê é que quem audita confere. */
    @Column(name = "legal_basis", length = 500)
    private String legalBasis;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    /** Esta configuração vigora na data dada. */
    public boolean coversDate(LocalDate date) {
        return active
                && date != null
                && !date.isBefore(effectiveFrom)
                && (effectiveTo == null || !date.isAfter(effectiveTo));
    }

    /**
     * Direito anual de férias para uma antiguidade em anos completos. Nulo quando o escalão que se
     * aplica não está configurado — quem chama decide o que fazer com a ausência, em vez de receber
     * um número inventado.
     */
    public Integer vacationDaysForSeniority(int completedYears) {
        if (completedYears <= 0) {
            return vacationDaysYear1;
        }
        if (completedYears == 1) {
            return vacationDaysYear2;
        }
        return vacationDaysYear3Plus;
    }
}
