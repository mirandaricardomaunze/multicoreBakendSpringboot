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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alteração salarial (ou de função/departamento) com <b>data de efeito</b>.
 * Ver docs/RH_COMPLETO_SPEC.md §B4.
 *
 * <p>Antes disto, {@code updateEmployee} sobrepunha o {@code baseSalary}: o valor anterior não
 * ficava em lado nenhum, nem quem o mudou, nem porquê, nem a partir de quando. É o mesmo defeito
 * da margem histórica que a V37 corrigiu no comercial, transposto para os salários.
 *
 * <p>A consequência que dói é a segunda: <b>o recibo de Março tem de usar o salário de Março</b>.
 * Sem histórico, reprocessar Março em Setembro pagava ao valor de Setembro — e ninguém reparava,
 * porque o número parecia perfeitamente normal.
 */
@Entity
@Table(name = "salary_changes")
@Getter
@Setter
public class SalaryChange extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** Redundante com {@code employee.company}, mas é por aqui que se filtra a empresa activa. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** O que se ganhava antes. Nulo só na primeira alteração de um colaborador. */
    @Column(name = "previous_salary", precision = 19, scale = 2)
    private BigDecimal previousSalary;

    @Column(name = "new_salary", nullable = false, precision = 19, scale = 2)
    private BigDecimal newSalary;

    /**
     * A partir de quando vale. É o campo que carrega o bloco: sem ele, "o salário" é só o último
     * número escrito, e o passado deixa de ser reconstituível.
     */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20)
    private SalaryChangeReason reason;

    /** Função a partir desta data, quando a alteração também a muda. Nulo mantém a anterior. */
    @Column(name = "job_title", length = 120)
    private String jobTitle;

    @Column(name = "department", length = 120)
    private String department;

    @Column(name = "approved_by", nullable = false, length = 120)
    private String approvedBy;

    @Column(name = "notes", length = 500)
    private String notes;

    /** Esta alteração já produz efeito na data dada. */
    public boolean appliesOn(LocalDate date) {
        return date != null && !date.isBefore(effectiveDate);
    }
}
