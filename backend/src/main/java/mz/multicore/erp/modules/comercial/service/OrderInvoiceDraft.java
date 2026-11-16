package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.pricing.LineCalculator;
import mz.multicore.erp.modules.comercial.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/**
 * Rascunho da fatura de uma encomenda: linhas e totais, sem número fiscal nem IO. O número só é
 * pedido depois de o crédito passar, para uma recusa não queimar um número da série.
 */
final class OrderInvoiceDraft {

    private OrderInvoiceDraft() {
    }

    static Invoice of(Order order, List<OrderLine> billableLines, Function<OrderLine, BigDecimal> billedQuantity) {
        Invoice invoice = new Invoice();
        invoice.setClient(order.getClient());
        if (order.getWalkInName() != null && !order.getWalkInName().isBlank()) {
            invoice.setCustomerName(order.getWalkInName().trim());
        }
        invoice.setCompany(order.getCompany());
        invoice.setWarehouse(order.getWarehouse());
        invoice.setStatus(InvoiceStatus.APPROVED);
        invoice.setSalesChannel(SalesChannel.ORDER);

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        for (OrderLine orderLine : billableLines) {
            BigDecimal quantity = billedQuantity.apply(orderLine);
            LineCalculator.LineAmounts amounts = LineCalculator.compute(
                    orderLine.getUnitPrice(), quantity, orderLine.getDiscountPercentage(), orderLine.getTaxRate());
            subtotal = subtotal.add(amounts.net());
            totalTax = totalTax.add(amounts.tax());

            InvoiceLine invoiceLine = new InvoiceLine();
            invoiceLine.setProduct(orderLine.getProduct());
            invoiceLine.setQuantity(quantity);
            invoiceLine.setUnitPrice(orderLine.getUnitPrice());
            invoiceLine.setTaxRate(orderLine.getTaxRate());
            invoiceLine.setDiscountPercentage(orderLine.getDiscountPercentage());
            invoiceLine.setLineTotal(amounts.total());
            invoiceLine.setBatchNumber(orderLine.getBatchNumber());
            invoiceLine.setSerialNumber(orderLine.getSerialNumber());
            invoiceLine.setUnitCost(orderLine.getProduct().getPurchasePrice());
            invoice.addLine(invoiceLine);
        }
        invoice.setTotalBeforeTax(subtotal.setScale(2, RoundingMode.HALF_UP));
        invoice.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotalAmount(subtotal.add(totalTax).setScale(2, RoundingMode.HALF_UP));
        return invoice;
    }
}
