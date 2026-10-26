package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CollectionRadarItemDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionRadarSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionReminderTemplateDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.CollectionRadarBucket;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Serviço responsável pelo Radar Inteligente de Cobranças e Lembretes Preventivos.
 * Ver {@code docs/RADAR_COBRANCAS_PREVENTIVO_SPEC.md}.
 */
@Service
public class CollectionRadarService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.US));

    private final InvoiceRepository invoiceRepository;

    public CollectionRadarService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository obrigatório");
    }

    @Transactional(readOnly = true)
    public CollectionRadarSummaryDTO getRadarSummary(Long companyId, LocalDate referenceDate) {
        Long resolvedCompanyId = companyId != null ? companyId : CurrentUserContext.requireCurrentCompanyId();
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();

        List<Invoice> collectable = invoiceRepository.findOutstandingWithClient(
                resolvedCompanyId, InvoiceStatus.collectableStatuses()).stream()
                .filter(inv -> inv.getStatus().isCollectable())
                .filter(inv -> inv.outstandingAmount().signum() > 0)
                .toList();

        BigDecimal critTotal = BigDecimal.ZERO;
        int critCount = 0;
        BigDecimal recTotal = BigDecimal.ZERO;
        int recCount = 0;
        BigDecimal todayTotal = BigDecimal.ZERO;
        int todayCount = 0;
        BigDecimal in7Total = BigDecimal.ZERO;
        int in7Count = 0;
        BigDecimal in15Total = BigDecimal.ZERO;
        int in15Count = 0;
        BigDecimal in30Total = BigDecimal.ZERO;
        int in30Count = 0;
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (Invoice invoice : collectable) {
            BigDecimal balance = invoice.outstandingAmount();
            grandTotal = grandTotal.add(balance);
            LocalDate due = invoice.effectiveDueDate();
            int daysUntilDue = due != null ? (int) ChronoUnit.DAYS.between(ref, due) : 0;
            CollectionRadarBucket bucket = CollectionRadarBucket.of(daysUntilDue);

            switch (bucket) {
                case CRITICAL_OVERDUE -> {
                    critTotal = critTotal.add(balance);
                    critCount++;
                }
                case RECENT_OVERDUE -> {
                    recTotal = recTotal.add(balance);
                    recCount++;
                }
                case DUE_TODAY -> {
                    todayTotal = todayTotal.add(balance);
                    todayCount++;
                }
                case DUE_IN_7_DAYS -> {
                    in7Total = in7Total.add(balance);
                    in7Count++;
                }
                case DUE_IN_15_DAYS -> {
                    in15Total = in15Total.add(balance);
                    in15Count++;
                }
                case DUE_IN_30_DAYS -> {
                    in30Total = in30Total.add(balance);
                    in30Count++;
                }
                case FUTURE -> {
                    // Ignorado nos horizontes de curto/médio prazo do resumo
                }
            }
        }

        return new CollectionRadarSummaryDTO(
                resolvedCompanyId,
                ref,
                critTotal, critCount,
                recTotal, recCount,
                todayTotal, todayCount,
                in7Total, in7Count,
                in15Total, in15Count,
                in30Total, in30Count,
                grandTotal,
                collectable.size()
        );
    }

    @Transactional(readOnly = true)
    public List<CollectionRadarItemDTO> getRadarItems(Long companyId, CollectionRadarBucket filterBucket, LocalDate referenceDate) {
        Long resolvedCompanyId = companyId != null ? companyId : CurrentUserContext.requireCurrentCompanyId();
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();

        List<Invoice> collectable = invoiceRepository.findOutstandingWithClient(
                resolvedCompanyId, InvoiceStatus.collectableStatuses()).stream()
                .filter(inv -> inv.getStatus().isCollectable())
                .filter(inv -> inv.outstandingAmount().signum() > 0)
                .toList();

        List<CollectionRadarItemDTO> items = new ArrayList<>();
        for (Invoice invoice : collectable) {
            LocalDate due = invoice.effectiveDueDate();
            int daysUntilDue = due != null ? (int) ChronoUnit.DAYS.between(ref, due) : 0;
            CollectionRadarBucket bucket = CollectionRadarBucket.of(daysUntilDue);

            if (filterBucket != null && bucket != filterBucket) {
                continue;
            }

            Client client = invoice.getClient();
            items.add(new CollectionRadarItemDTO(
                    invoice.getId(),
                    invoice.getInvoiceNumber(),
                    client != null ? client.getId() : null,
                    client != null ? client.getName() : "Cliente sem ficha",
                    client != null ? client.getTaxId() : "-",
                    "-",
                    client != null ? client.getEmail() : "-",
                    invoice.issueDate(),
                    due,
                    invoice.getTotalAmount(),
                    invoice.getAmountPaid(),
                    invoice.outstandingAmount(),
                    daysUntilDue,
                    bucket
            ));
        }

        // Ordenação por prioridade: faturas mais vencidas primeiro, depois faturas a vencer mais próximas
        items.sort(Comparator.comparingInt(CollectionRadarItemDTO::daysUntilDue));
        return items;
    }

    @Transactional(readOnly = true)
    public CollectionReminderTemplateDTO generateReminderTemplate(Long invoiceId, String channel) {
        if (invoiceId == null) throw new BusinessRuleException("ID da fatura obrigatório.");
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada: " + invoiceId));

        Client client = invoice.getClient();
        String clientName = client != null ? client.getName() : "Estimado(a) Cliente";
        String invoiceNum = invoice.getInvoiceNumber();
        BigDecimal balance = invoice.outstandingAmount();
        LocalDate due = invoice.effectiveDueDate();
        LocalDate today = LocalDate.now();
        int daysUntilDue = due != null ? (int) ChronoUnit.DAYS.between(today, due) : 0;
        String dueStr = due != null ? due.format(DATE_FMT) : "a definir";
        String balanceStr = MONEY_FMT.format(balance) + " MT";

        String normChannel = (channel != null ? channel.trim().toUpperCase() : "WHATSAPP");
        String contact = switch (normChannel) {
            case "SMS", "WHATSAPP" -> "";
            case "EMAIL" -> client != null && client.getEmail() != null ? client.getEmail() : "";
            default -> "";
        };

        String subject;
        String message;

        if (daysUntilDue < 0) {
            int overdueDays = Math.abs(daysUntilDue);
            subject = "Aviso de Regularização de Fatura Vencida — " + invoiceNum;
            if ("SMS".equals(normChannel)) {
                message = String.format("Multicore ERP: A fatura %s no valor de %s encontra-se vencida desde %s (%d dias). Agradecemos a regularização.",
                        invoiceNum, balanceStr, dueStr, overdueDays);
            } else if ("WHATSAPP".equals(normChannel)) {
                message = String.format("Olá, *%s*! Esperamos que esteja tudo bem.\n\n" +
                                "Gostaríamos de lembrar que a fatura *%s* no valor em aberto de *%s* venceu a *%s* (%d dias de atraso).\n\n" +
                                "Agradecemos a regularização ou o envio do comprovativo de pagamento caso já tenha sido liquidada. Ficamos ao seu dispor para qualquer esclarecimento.",
                        clientName, invoiceNum, balanceStr, dueStr, overdueDays);
            } else {
                message = String.format("Exmo.(a) Sr.(a) %s,\n\n" +
                                "Esperamos que este contacto o encontre bem.\n\n" +
                                "Informamos que a fatura nº %s, com vencimento a %s, regista à data presente um saldo em aberto de %s (%d dias de atraso).\n\n" +
                                "Solicitamos a gentileza de proceder à regularização do valor em dívida ou de nos remeter o respectivo comprovativo caso a liquidação já tenha sido efectuada.\n\n" +
                                "Para informações adicionais ou envio de comprovativos, por favor responda a esta mensagem.\n\n" +
                                "Com os melhores cumprimentos,\n" +
                                "Departamento Financeiro e Cobranças\n" +
                                "Multicore ERP",
                        clientName, invoiceNum, dueStr, balanceStr, overdueDays);
            }
        } else if (daysUntilDue == 0) {
            subject = "Lembrete de Vencimento de Fatura Hoje — " + invoiceNum;
            if ("SMS".equals(normChannel)) {
                message = String.format("Multicore ERP: Lembramos que a fatura %s no valor de %s vence hoje, %s. Agradecemos o envio do comprovativo.",
                        invoiceNum, balanceStr, dueStr);
            } else if ("WHATSAPP".equals(normChannel)) {
                message = String.format("Olá, *%s*! Esperamos que esteja tudo bem.\n\n" +
                                "Lembramos cordialmente que a fatura *%s* no valor de *%s* tem vencimento agendado para hoje (*%s*).\n\n" +
                                "Caso já tenha efetuado o pagamento, agradecemos o envio do comprovativo. Obrigado!",
                        clientName, invoiceNum, balanceStr, dueStr);
            } else {
                message = String.format("Exmo.(a) Sr.(a) %s,\n\n" +
                                "Lembramos cordialmente que a fatura nº %s no valor em dívida de %s tem o seu vencimento agendado para hoje, %s.\n\n" +
                                "Agradecemos desde já a sua atenção e colaboração na regularização atempada.\n\n" +
                                "Com os melhores cumprimentos,\n" +
                                "Departamento Financeiro\n" +
                                "Multicore ERP",
                        clientName, invoiceNum, balanceStr, dueStr);
            }
        } else {
            subject = "Lembrete Preventivo de Vencimento — " + invoiceNum;
            if ("SMS".equals(normChannel)) {
                message = String.format("Multicore ERP: Lembrete preventivo da fatura %s no valor de %s a vencer em %s (%d dias).",
                        invoiceNum, balanceStr, dueStr, daysUntilDue);
            } else if ("WHATSAPP".equals(normChannel)) {
                message = String.format("Olá, *%s*!\n\n" +
                                "Informamos que a fatura *%s* no valor de *%s* tem vencimento agendado para o próximo dia *%s* (daqui a %d dias).\n\n" +
                                "Este aviso serve como apoio à sua programação de pagamentos. Muito obrigado pela preferência!",
                        clientName, invoiceNum, balanceStr, dueStr, daysUntilDue);
            } else {
                message = String.format("Exmo.(a) Sr.(a) %s,\n\n" +
                                "Informamos que a fatura nº %s, com saldo em aberto de %s, terá o seu vencimento no dia %s (daqui a %d dias).\n\n" +
                                "Enviamos este aviso para suporte à sua gestão preventiva de tesouraria.\n\n" +
                                "Com os melhores cumprimentos,\n" +
                                "Departamento Financeiro\n" +
                                "Multicore ERP",
                        clientName, invoiceNum, balanceStr, dueStr, daysUntilDue);
            }
        }

        return new CollectionReminderTemplateDTO(
                invoiceId,
                invoiceNum,
                clientName,
                normChannel,
                contact,
                subject,
                message
        );
    }
}
