package mz.multicore.erp.modules.hr.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ficheiro de pagamento bancário da folha do mês. Ver docs/RH_COMPLETO_SPEC.md §B8.7.
 *
 * <p>Numa folha de 30 pessoas, pagava-se uma a uma. O ficheiro sai em <b>CSV</b> — o formato que
 * qualquer banco moçambicano importa ou converte — em vez de um formato proprietário de um banco em
 * particular, que ficaria errado para todos os outros clientes.
 *
 * @param missingAccount colaboradores <b>sem conta bancária</b> na ficha. Vêm em lista própria em
 *                       vez de silenciosamente fora do ficheiro: um pagamento que falta é a coisa
 *                       que menos pode desaparecer sem aviso
 */
public record BankPaymentFileDTO(
        int year,
        int month,
        BigDecimal totalAmount,
        int paymentCount,
        List<BankPaymentLineDTO> lines,
        List<String> missingAccount,
        String csv
) {
    public record BankPaymentLineDTO(
            String employeeNumber,
            String employeeName,
            String bankName,
            String bankAccount,
            String payslipNumber,
            BigDecimal netPay
    ) {}
}
