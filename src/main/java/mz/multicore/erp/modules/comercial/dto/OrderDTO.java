package mz.multicore.erp.modules.comercial.dto;

import mz.multicore.erp.modules.comercial.model.OrderKind;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDTO(
    Long id,
    String orderNumber,
    Long clientId,
    String clientName,
    String clientTaxId,
    String walkInName,
    BigDecimal totalBeforeTax,
    BigDecimal taxAmount,
    BigDecimal totalAmount,
    String status,
    Long invoiceId,
    List<OrderLineDTO> lines,
    LocalDateTime createdAt,
    LocalDateTime printedAt,
    int printCount,
    String lastPrintedBy,
    /** Via da encomenda — decide formato do documento, aprovação e circuito. */
    OrderKind kind,
    /** Rótulo PT-MZ da via, para o desktop não traduzir por conta própria. */
    String kindLabel
) {
    /** Construtor retrocompatível para quem construía o DTO antes da via existir. */
    public OrderDTO(Long id, String orderNumber, Long clientId, String clientName, String clientTaxId,
                    String walkInName, BigDecimal totalBeforeTax, BigDecimal taxAmount, BigDecimal totalAmount,
                    String status, Long invoiceId, List<OrderLineDTO> lines, LocalDateTime createdAt,
                    LocalDateTime printedAt, int printCount, String lastPrintedBy) {
        this(id, orderNumber, clientId, clientName, clientTaxId, walkInName, totalBeforeTax, taxAmount,
                totalAmount, status, invoiceId, lines, createdAt, printedAt, printCount, lastPrintedBy,
                OrderKind.FORMAL_ORDER, OrderKind.FORMAL_ORDER.label());
    }
}
