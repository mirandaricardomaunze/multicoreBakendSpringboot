package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Dados estruturados para a emissão formal de carta/notificação de cobrança e regularização de dívida.
 */
public record DebtCollectionNoticeDTO(
        Long clientId,
        String clientName,
        String clientTaxId,
        String address,
        String email,
        LocalDate referenceDate,
        String noticeReference,
        BigDecimal totalOverdue,
        int maxDaysOverdue,
        List<OverdueInvoiceItemDTO> overdueInvoices,
        List<String> paymentBankAccounts,
        String companyName,
        String companyTaxId,
        String companyAddress,
        String companyPhone,
        String companyEmail,
        String customMessage
) {}
