package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cotação na fronteira HTTP.
 *
 * <p>{@code expired} e {@code daysUntilExpiry} são <b>derivados</b> da validade contra a data do
 * servidor, não colunas. O cliente desktop apresenta-os tal como vêm — não recalcula caducidade,
 * senão a regra passava a existir em dois sítios (docs/COTACAO_SPEC.md §4).
 */
public record QuotationDTO(
        Long id,
        String quotationNumber,
        LocalDateTime quotationDate,
        LocalDate validUntil,
        boolean expired,
        long daysUntilExpiry,
        Long companyId,
        Long clientId,
        String clientName,
        String clientTaxId,
        String walkInName,
        Long warehouseId,
        String warehouseName,
        BigDecimal totalBeforeTax,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String status,
        String statusLabel,
        String paymentTerms,
        String deliveryTerms,
        Integer deliveryDays,
        String notes,
        LocalDateTime sentAt,
        LocalDateTime decidedAt,
        String decidedBy,
        String rejectionReason,
        Long orderId,
        String orderNumber,
        String createdBy,
        List<QuotationLineDTO> lines
) {}
