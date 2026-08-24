package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um recibo de vencimento foi pago.
 *
 * <p><b>Porquê um evento e não uma chamada directa:</b> o maior custo fixo de uma empresa de
 * retalho é a folha, e até aqui não chegava ao razão nem ao balancete (declarado em falta na
 * CONTABILIDADE_SPEC §7). Fechar essa lacuna com o {@code HRService} a chamar a contabilidade
 * criaria a dependência que o comercial não tem — e o RH passaria a conhecer um módulo que não é
 * da sua conta. O evento leva <b>números</b>, nunca entidades JPA.
 *
 * <p>Os {@code @EventListener} do Spring correm de forma síncrona e na <b>mesma transacção</b>: se
 * o lançamento falhar, o recibo não fica pago pela metade.
 *
 * @param grossPay          base + subsídios + horas extra
 * @param absenceDeduction  desconto por faltas não remuneradas (reduz o custo do mês)
 * @param irps              IRPS retido ao trabalhador
 * @param employeeInss      INSS descontado ao trabalhador
 * @param employerInss      INSS a cargo da empresa — custo dela, não desconto
 * @param otherDeductions   outros descontos retidos no recibo
 * @param netPay            o que sai da tesouraria para o colaborador
 */
public record PayslipPaidEvent(
        Long companyId,
        Long payslipId,
        String payslipNumber,
        String employeeName,
        LocalDate date,
        BigDecimal grossPay,
        BigDecimal absenceDeduction,
        BigDecimal irps,
        BigDecimal employeeInss,
        BigDecimal employerInss,
        BigDecimal otherDeductions,
        BigDecimal netPay
) {}
