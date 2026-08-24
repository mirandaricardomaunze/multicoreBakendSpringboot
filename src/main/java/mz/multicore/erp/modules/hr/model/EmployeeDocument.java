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
import java.time.temporal.ChronoUnit;

/**
 * Um documento do colaborador com data de validade. Ver docs/RH_COMPLETO_SPEC.md §B8.8.
 *
 * <p><b>O DIRE de um trabalhador estrangeiro caducar sem aviso é multa</b> — e não havia sítio
 * nenhum no sistema onde essa data existisse.
 *
 * <p>A caducidade é <b>derivada</b> da data contra hoje, nunca gravada: mesma lição da cotação, do
 * contrato e da retenção. Sem agendador nocturno e sem linhas desactualizadas entre passagens.
 */
@Entity
@Table(name = "employee_documents")
@Getter
@Setter
public class EmployeeDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /** BI · DIRE · PASSAPORTE · NUIT · CERTIFICADO · OUTRO. Texto, não enum: cada país tem os seus. */
    @Column(name = "document_type", nullable = false, length = 40)
    private String documentType;

    @Column(name = "document_number", length = 80)
    private String documentNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    /**
     * Nulo = <b>não caduca</b> (NUIT, BI vitalício). Não é o mesmo que "ainda não preenchi", e por
     * isso o alerta só olha para os que têm data — avisar sobre um documento sem validade seria
     * ruído que ensina a ignorar o sino.
     */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "notes", length = 500)
    private String notes;

    public boolean isExpired(LocalDate today) {
        return expiryDate != null && today != null && today.isAfter(expiryDate);
    }

    /** Dias até caducar; nulo quando o documento não caduca. */
    public Long daysUntilExpiry(LocalDate today) {
        if (expiryDate == null || today == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(today, expiryDate);
    }
}
