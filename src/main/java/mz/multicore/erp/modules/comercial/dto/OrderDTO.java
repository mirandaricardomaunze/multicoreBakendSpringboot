package mz.multicore.erp.modules.comercial.dto;

import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.model.OrderStatusLabel;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    String kindLabel,
    /** Rótulo PT-MZ do estado — o desktop e o PDF nunca mostram a constante. */
    String statusLabel,
    /** Cotação de origem; nulo quando a encomenda não veio de proposta nenhuma. */
    Long quotationId,
    String quotationNumber,
    String paymentTerms,
    String deliveryTerms,
    LocalDate expectedDeliveryDate,
    /** Derivado no servidor — o desktop apresenta, não recalcula. */
    boolean deliveryOverdue,
    /** Reposição interna: para que armazém vai a mercadoria, e a transferência que a cumpre. */
    Long destinationWarehouseId,
    String destinationWarehouseName,
    Long stockTransferId,
    String transferNumber
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

    /** Construtor retrocompatível de quem declara a via mas não a origem/condições. */
    public OrderDTO(Long id, String orderNumber, Long clientId, String clientName, String clientTaxId,
                    String walkInName, BigDecimal totalBeforeTax, BigDecimal taxAmount, BigDecimal totalAmount,
                    String status, Long invoiceId, List<OrderLineDTO> lines, LocalDateTime createdAt,
                    LocalDateTime printedAt, int printCount, String lastPrintedBy,
                    OrderKind kind, String kindLabel) {
        this(id, orderNumber, clientId, clientName, clientTaxId, walkInName, totalBeforeTax, taxAmount,
                totalAmount, status, invoiceId, lines, createdAt, printedAt, printCount, lastPrintedBy,
                kind, kindLabel, OrderStatusLabel.of(status), null, null, null, null, null, false,
                null, null, null, null);
    }
}
