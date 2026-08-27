package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.hr.dto.BankPaymentFileDTO;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Ficheiro de pagamento bancário da folha do mês. Ver docs/RH_COMPLETO_SPEC.md §B8.7.
 *
 * <p><b>O problema:</b> numa folha de 30 pessoas, pagava-se uma a uma — 30 transferências manuais,
 * 30 hipóteses de trocar um dígito.
 *
 * <p><b>CSV, e não o formato de um banco em particular.</b> Os formatos de ficheiro de pagamento
 * variam de banco para banco; escolher um deixaria o produto errado para todos os outros clientes.
 * O CSV é o que qualquer banco moçambicano importa ou converte, e é legível por uma pessoa — o que
 * importa quando se está a conferir um pagamento antes de o submeter.
 */
@Service
public class BankPaymentFileService {

    /** Estados cujo líquido entra no ficheiro: já aprovado, ainda não saiu da conta. */
    private static final String APPROVED = "APPROVED";

    private final PayslipRepository payslipRepository;
    private final AuditLogService auditLogService;

    public BankPaymentFileService(PayslipRepository payslipRepository,
                                  AuditLogService auditLogService) {
        this.payslipRepository = payslipRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Gera o ficheiro dos recibos <b>aprovados</b> do período.
     *
     * <p>Só aprovados: um rascunho ainda pode mudar de valor, e um recibo já pago já saiu da conta —
     * incluí-lo pagaria a mesma pessoa duas vezes, que é exactamente o erro que um ficheiro de
     * pagamento em lote torna fácil e caro.
     */
    @Transactional(readOnly = true)
    public BankPaymentFileDTO generate(int year, int month) {
        ensureHrManager();
        List<Payslip> approved = payslipRepository
                .findByCompanyIdAndYearAndMonth(currentCompanyId(), year, month).stream()
                .filter(p -> APPROVED.equals(p.getStatus()))
                .toList();
        if (approved.isEmpty()) {
            throw new BusinessRuleException(String.format(
                    "Não há recibos aprovados em %d/%d. Aprove a folha antes de gerar o ficheiro de "
                            + "pagamento.", month, year));
        }

        List<BankPaymentFileDTO.BankPaymentLineDTO> lines = new ArrayList<>();
        List<String> missingAccount = new ArrayList<>();
        for (Payslip p : approved) {
            String account = p.getEmployee().getBankAccount();
            if (account == null || account.isBlank()) {
                // Fora do ficheiro, mas NUNCA em silêncio: um pagamento que falta é a coisa que
                // menos pode desaparecer sem aviso.
                missingAccount.add(p.getEmployee().getName());
                continue;
            }
            lines.add(new BankPaymentFileDTO.BankPaymentLineDTO(
                    p.getEmployee().getEmployeeNumber(), p.getEmployee().getName(),
                    p.getEmployee().getBankName(), account, p.getPayslipNumber(), p.getNetPay()));
        }

        BigDecimal total = lines.stream()
                .map(BankPaymentFileDTO.BankPaymentLineDTO::netPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        auditLogService.logCurrent("PAYROLL_BANK_FILE", String.format(
                "Ficheiro de pagamento de %d/%d gerado: %d pagamento(s), total %s%s",
                month, year, lines.size(), total,
                missingAccount.isEmpty() ? "" : ", " + missingAccount.size() + " sem conta bancária"));
        return new BankPaymentFileDTO(year, month, total, lines.size(), lines, missingAccount,
                toCsv(year, month, lines));
    }

    private String toCsv(int year, int month, List<BankPaymentFileDTO.BankPaymentLineDTO> lines) {
        StringBuilder csv = new StringBuilder(
                "numero_colaborador;nome;banco;conta;recibo;referencia;valor\n");
        String reference = String.format("SALARIO %02d/%d", month, year);
        for (BankPaymentFileDTO.BankPaymentLineDTO line : lines) {
            csv.append(csvCell(line.employeeNumber())).append(';')
                    .append(csvCell(line.employeeName())).append(';')
                    .append(csvCell(line.bankName())).append(';')
                    .append(csvCell(line.bankAccount())).append(';')
                    .append(csvCell(line.payslipNumber())).append(';')
                    .append(reference).append(';')
                    .append(line.netPay().toPlainString()).append('\n');
        }
        return csv.toString();
    }

    /**
     * O separador é ';' e o nome de um colaborador pode trazê-lo — "Silva; Jr." parte a linha em
     * duas e o banco lê o valor errado na coluna errada. Aspas e escape, como manda o CSV.
     */
    private String csvCell(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private Long currentCompanyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }

    private void ensureHrManager() {
        String role = CurrentUserContext.getRole();
        if (!"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)) {
            throw new BusinessRuleException(
                    "Apenas gestores ou administradores podem gerar o ficheiro de pagamento.");
        }
    }
}
