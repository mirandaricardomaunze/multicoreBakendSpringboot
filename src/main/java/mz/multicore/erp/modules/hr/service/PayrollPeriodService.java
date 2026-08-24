package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.dto.PayrollPeriodDTO;
import mz.multicore.erp.modules.hr.model.PayrollPeriod;
import mz.multicore.erp.modules.hr.repository.PayrollPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fecho do <b>mês da folha salarial</b>. Ver docs/RH_COMPLETO_SPEC.md §B8.6.
 *
 * <p><b>O que fecha:</b> {@code processMonthlyPayroll} corria para qualquer mês, sempre. Um mês já
 * pago, já entregue ao Estado e já contabilizado continuava a aceitar recibos novos — e cada recibo
 * novo desalinhava a retenção já declarada (§B5) sem nada avisar.
 *
 * <p><b>Não confundir com o fecho da folha de ponto.</b> O ponto fecha-se <i>antes</i> de a folha
 * correr, para as horas deixarem de mudar; a folha fecha-se <i>depois</i> de estar paga, para o mês
 * deixar de aceitar recibos. São dois fechos com donos e momentos diferentes, e um não substitui o
 * outro — tê-los juntos obrigaria a escolher qual dos dois problemas ignorar.
 *
 * <p>Um período sem linha nenhuma está <b>aberto</b>: não se exige um passo de abertura que ninguém
 * sabe que existe, exactamente como o {@code TimeSheet} faz.
 */
@Service
public class PayrollPeriodService {

    private final PayrollPeriodRepository periodRepository;
    private final CompanyRepository companyRepository;
    private final AuditLogService auditLogService;

    public PayrollPeriodService(PayrollPeriodRepository periodRepository,
                                CompanyRepository companyRepository,
                                AuditLogService auditLogService) {
        this.periodRepository = periodRepository;
        this.companyRepository = companyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public boolean isClosed(int year, int month) {
        return periodRepository.findByCompanyIdAndYearAndMonth(currentCompanyId(), year, month)
                .map(PayrollPeriod::isClosed).orElse(false);
    }

    /** Recusa mexer num mês fechado, nomeando-o. Chamado por quem emite recibos. */
    @Transactional(readOnly = true)
    public void ensureOpen(int year, int month) {
        if (isClosed(year, month)) {
            throw new BusinessRuleException(String.format(
                    "A folha de %d/%d está fechada. Reabra o período antes de emitir recibos nesse "
                            + "mês — um recibo novo num mês já entregue ao Estado desalinha a "
                            + "retenção declarada.", month, year));
        }
    }

    @Transactional(readOnly = true)
    public List<PayrollPeriodDTO> list() {
        return periodRepository.findByCompanyIdOrderByYearDescMonthDesc(currentCompanyId())
                .stream().map(PayrollPeriodService::toDTO).toList();
    }

    @Transactional
    public PayrollPeriodDTO close(int year, int month) {
        ensureHrManager();
        PayrollPeriod period = findOrCreate(year, month);
        if (period.isClosed()) {
            throw new BusinessRuleException(String.format("A folha de %d/%d já está fechada.", month, year));
        }
        period.setStatus("FECHADO");
        period.setClosedBy(CurrentUserContext.getUsername());
        period.setClosedAt(LocalDateTime.now());
        period.setReopenReason(null);
        PayrollPeriod saved = periodRepository.save(period);
        auditLogService.logCurrent("PAYROLL_PERIOD_CLOSE",
                String.format("Folha salarial de %d/%d fechada por %s",
                        month, year, saved.getClosedBy()));
        return toDTO(saved);
    }

    /**
     * Reabrir <b>exige motivo</b> e fica auditado. Sem isso, o fecho seria um botão que qualquer um
     * desfaz sem deixar rasto — e um fecho que se desfaz em silêncio não protege nada. Mesma regra
     * da reabertura da folha de ponto.
     */
    @Transactional
    public PayrollPeriodDTO reopen(int year, int month, String reason) {
        ensureHrManager();
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Reabrir a folha de um mês exige um motivo.");
        }
        PayrollPeriod period = periodRepository
                .findByCompanyIdAndYearAndMonth(currentCompanyId(), year, month)
                .orElseThrow(() -> new BusinessRuleException(
                        String.format("A folha de %d/%d nunca foi fechada.", month, year)));
        if (!period.isClosed()) {
            throw new BusinessRuleException(String.format("A folha de %d/%d já está aberta.", month, year));
        }
        period.setStatus("ABERTO");
        period.setReopenReason(reason.trim());
        PayrollPeriod saved = periodRepository.save(period);
        auditLogService.logCurrent("PAYROLL_PERIOD_REOPEN", String.format(
                "Folha salarial de %d/%d reaberta por %s. Motivo: %s",
                month, year, CurrentUserContext.getUsername(), reason.trim()));
        return toDTO(saved);
    }

    private PayrollPeriod findOrCreate(int year, int month) {
        Long companyId = currentCompanyId();
        return periodRepository.findByCompanyIdAndYearAndMonth(companyId, year, month)
                .orElseGet(() -> {
                    PayrollPeriod period = new PayrollPeriod();
                    period.setCompany(companyRepository.findById(companyId)
                            .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
                    period.setYear(year);
                    period.setMonth(month);
                    period.setStatus("ABERTO");
                    return period;
                });
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }

    private void ensureHrManager() {
        String role = CurrentUserContext.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new BusinessRuleException(
                    "Apenas gestores ou administradores podem fechar ou reabrir a folha do mês.");
        }
    }

    private static PayrollPeriodDTO toDTO(PayrollPeriod p) {
        return new PayrollPeriodDTO(p.getId(), p.getYear(), p.getMonth(), p.getStatus(),
                p.isClosed() ? "Fechada" : "Aberta", p.getClosedBy(), p.getClosedAt(),
                p.getReopenReason());
    }
}
