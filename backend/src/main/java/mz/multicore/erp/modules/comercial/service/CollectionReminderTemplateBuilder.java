package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.dto.CollectionReminderTemplateDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Constrói mensagens preventivas e de regularização multicanal (SMS, WhatsApp e E-mail). */
@Component
public class CollectionReminderTemplateBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));

    public CollectionReminderTemplateDTO build(Invoice invoice, String channel) {
        Client client = invoice.getClient();
        String clientName = client != null ? client.getName() : "Estimado(a) Cliente";
        String invoiceNum = invoice.getInvoiceNumber();
        BigDecimal balance = invoice.outstandingAmount();
        LocalDate due = invoice.effectiveDueDate();
        int daysUntilDue = due != null ? (int) ChronoUnit.DAYS.between(LocalDate.now(), due) : 0;
        String dueStr = due != null ? due.format(DATE_FMT) : "a definir";
        String balanceStr = MONEY_FMT.format(balance) + " MT";

        String normChannel = (channel != null ? channel.trim().toUpperCase() : "WHATSAPP");
        String contact = "EMAIL".equals(normChannel) && client != null && client.getEmail() != null
                ? client.getEmail()
                : "";

        String subject;
        String message;
        if (daysUntilDue < 0) {
            int overdueDays = Math.abs(daysUntilDue);
            subject = "Aviso de Regularização de Fatura Vencida — " + invoiceNum;
            message = buildOverdueMessage(normChannel, clientName, invoiceNum, balanceStr, dueStr, overdueDays);
        } else if (daysUntilDue == 0) {
            subject = "Lembrete de Vencimento de Fatura Hoje — " + invoiceNum;
            message = buildDueTodayMessage(normChannel, clientName, invoiceNum, balanceStr, dueStr);
        } else {
            subject = "Lembrete Preventivo de Vencimento — " + invoiceNum;
            message = buildPreventiveMessage(normChannel, clientName, invoiceNum, balanceStr, dueStr, daysUntilDue);
        }

        return new CollectionReminderTemplateDTO(
                invoice.getId(), invoiceNum, clientName, normChannel, contact, subject, message);
    }

    private String buildOverdueMessage(String channel, String clientName, String invoiceNum,
                                       String balanceStr, String dueStr, int overdueDays) {
        if ("SMS".equals(channel)) {
            return String.format("Multicore ERP: A fatura %s no valor de %s encontra-se vencida desde %s (%d dias). Agradecemos a regularização.",
                    invoiceNum, balanceStr, dueStr, overdueDays);
        }
        if ("WHATSAPP".equals(channel)) {
            return String.format("Olá, *%s*! Esperamos que esteja tudo bem.\n\n"
                            + "Gostaríamos de lembrar que a fatura *%s* no valor em aberto de *%s* venceu a *%s* (%d dias de atraso).\n\n"
                            + "Agradecemos a regularização ou o envio do comprovativo de pagamento caso já tenha sido liquidada. Ficamos ao seu dispor para qualquer esclarecimento.",
                    clientName, invoiceNum, balanceStr, dueStr, overdueDays);
        }
        return String.format("Exmo.(a) Sr.(a) %s,\n\n"
                        + "Esperamos que este contacto o encontre bem.\n\n"
                        + "Informamos que a fatura nº %s, com vencimento a %s, regista à data presente um saldo em aberto de %s (%d dias de atraso).\n\n"
                        + "Solicitamos a gentileza de proceder à regularização do valor em dívida ou de nos remeter o respectivo comprovativo caso a liquidação já tenha sido efectuada.\n\n"
                        + "Para informações adicionais ou envio de comprovativos, por favor responda a esta mensagem.\n\n"
                        + "Com os melhores cumprimentos,\n"
                        + "Departamento Financeiro e Cobranças\n"
                        + "Multicore ERP",
                clientName, invoiceNum, dueStr, balanceStr, overdueDays);
    }

    private String buildDueTodayMessage(String channel, String clientName, String invoiceNum,
                                        String balanceStr, String dueStr) {
        if ("SMS".equals(channel)) {
            return String.format("Multicore ERP: Lembramos que a fatura %s no valor de %s vence hoje, %s. Agradecemos o envio do comprovativo.",
                    invoiceNum, balanceStr, dueStr);
        }
        if ("WHATSAPP".equals(channel)) {
            return String.format("Olá, *%s*! Esperamos que esteja tudo bem.\n\n"
                            + "Lembramos cordialmente que a fatura *%s* no valor de *%s* tem vencimento agendado para hoje (*%s*).\n\n"
                            + "Caso já tenha efetuado o pagamento, agradecemos o envio do comprovativo. Obrigado!",
                    clientName, invoiceNum, balanceStr, dueStr);
        }
        return String.format("Exmo.(a) Sr.(a) %s,\n\n"
                        + "Lembramos cordialmente que a fatura nº %s no valor em dívida de %s tem o seu vencimento agendado para hoje, %s.\n\n"
                        + "Agradecemos desde já a sua atenção e colaboração na regularização atempada.\n\n"
                        + "Com os melhores cumprimentos,\n"
                        + "Departamento Financeiro\n"
                        + "Multicore ERP",
                clientName, invoiceNum, balanceStr, dueStr);
    }

    private String buildPreventiveMessage(String channel, String clientName, String invoiceNum,
                                          String balanceStr, String dueStr, int daysUntilDue) {
        if ("SMS".equals(channel)) {
            return String.format("Multicore ERP: Lembrete preventivo da fatura %s no valor de %s a vencer em %s (%d dias).",
                    invoiceNum, balanceStr, dueStr, daysUntilDue);
        }
        if ("WHATSAPP".equals(channel)) {
            return String.format("Olá, *%s*!\n\n"
                            + "Informamos que a fatura *%s* no valor de *%s* tem vencimento agendado para o próximo dia *%s* (daqui a %d dias).\n\n"
                            + "Este aviso serve como apoio à sua programação de pagamentos. Muito obrigado pela preferência!",
                    clientName, invoiceNum, balanceStr, dueStr, daysUntilDue);
        }
        return String.format("Exmo.(a) Sr.(a) %s,\n\n"
                        + "Informamos que a fatura nº %s, com saldo em aberto de %s, terá o seu vencimento no dia %s (daqui a %d dias).\n\n"
                        + "Enviamos este aviso para suporte à sua gestão preventiva de tesouraria.\n\n"
                        + "Com os melhores cumprimentos,\n"
                        + "Departamento Financeiro\n"
                        + "Multicore ERP",
                clientName, invoiceNum, balanceStr, dueStr, daysUntilDue);
    }
}
