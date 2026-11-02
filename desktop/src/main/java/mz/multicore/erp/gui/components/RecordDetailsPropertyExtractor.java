package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.JTable;
import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Extrator semântico de propriedades e resolutor de temas para a ficha de detalhes.
 */
public final class RecordDetailsPropertyExtractor {

    public record HeaderTheme(String icon, Color accent) {}

    private RecordDetailsPropertyExtractor() {}

    public static List<RecordDetailsDialog.PropertyItem> extractProperties(JTable table, int row, ProductDTO product, ClientDTO client, OrderDTO order) {
        List<RecordDetailsDialog.PropertyItem> list = new ArrayList<>();

        if (order != null) {
            list.add(new RecordDetailsDialog.PropertyItem("Nº Encomenda", order.orderNumber()));
            list.add(new RecordDetailsDialog.PropertyItem("Cliente", order.clientName()));
            if (order.clientTaxId() != null) list.add(new RecordDetailsDialog.PropertyItem("NUIT / NIF", order.clientTaxId()));
            if (order.walkInName() != null && !order.walkInName().isBlank()) list.add(new RecordDetailsDialog.PropertyItem("Comprador", order.walkInName()));
            list.add(new RecordDetailsDialog.PropertyItem("Estado", order.statusLabel() != null ? order.statusLabel() : order.status()));
            list.add(new RecordDetailsDialog.PropertyItem("Valor Total", String.format("%.2f MT", order.totalAmount())));
            if (order.kindLabel() != null) list.add(new RecordDetailsDialog.PropertyItem("Via / Tipo de Encomenda", order.kindLabel()));
            if (order.warehouseName() != null) list.add(new RecordDetailsDialog.PropertyItem("Armazém Origem", order.warehouseName()));
            if (order.paymentTerms() != null) list.add(new RecordDetailsDialog.PropertyItem("Termos de Pagamento", order.paymentTerms()));
            if (order.deliveryTerms() != null) list.add(new RecordDetailsDialog.PropertyItem("Termos de Entrega", order.deliveryTerms()));
            if (order.expectedDeliveryDate() != null) {
                String d = order.expectedDeliveryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                list.add(new RecordDetailsDialog.PropertyItem("Data de Entrega Prevista", order.deliveryOverdue() ? d + " (em atraso)" : d));
            }
            if (order.quotationNumber() != null && !order.quotationNumber().isBlank()) list.add(new RecordDetailsDialog.PropertyItem("Cotação de Origem", order.quotationNumber()));
            list.add(new RecordDetailsDialog.PropertyItem("Impressões Oficiais", order.printCount() + " vez(es)"));
            return list;
        }

        for (int col = 0; col < table.getColumnCount(); col++) {
            if (RecordDetailsDialog.isHidden(table, col)) continue;
            String label = table.getColumnName(col);
            Object rawVal = table.getValueAt(row, col);
            String val = RecordDetailsDialog.valueText(rawVal);
            list.add(new RecordDetailsDialog.PropertyItem(label, val.isBlank() ? "—" : val));
        }

        if (product != null) {
            if (product.barcode() != null && !product.barcode().isBlank() && list.stream().noneMatch(p -> p.label().toLowerCase().contains("barra"))) {
                list.add(new RecordDetailsDialog.PropertyItem("Código de Barras (EAN)", product.barcode()));
            }
            if (product.sku() != null && !product.sku().isBlank() && list.stream().noneMatch(p -> p.label().equalsIgnoreCase("sku"))) {
                list.add(new RecordDetailsDialog.PropertyItem("Código SKU", product.sku()));
            }
            if (product.reference() != null && !product.reference().isBlank() && list.stream().noneMatch(p -> p.label().toLowerCase().contains("ref"))) {
                list.add(new RecordDetailsDialog.PropertyItem("Referência de Fabricante", product.reference()));
            }
            if (product.purchasePrice() != null && list.stream().noneMatch(p -> p.label().toLowerCase().contains("custo") || p.label().toLowerCase().contains("compra"))) {
                list.add(new RecordDetailsDialog.PropertyItem("Preço de Custo (Compra)", String.format("%.2f MT", product.purchasePrice())));
            }
            if (product.unitPrice() != null && product.purchasePrice() != null) {
                BigDecimal margin = product.unitPrice().subtract(product.purchasePrice());
                if (product.purchasePrice().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal pct = margin.multiply(BigDecimal.valueOf(100)).divide(product.purchasePrice(), 1, RoundingMode.HALF_UP);
                    list.add(new RecordDetailsDialog.PropertyItem("Margem Bruta Estimada", String.format("%.2f MT (%s%%)", margin, pct.toPlainString())));
                }
            }
            if (product.minStock() != null && list.stream().noneMatch(p -> p.label().toLowerCase().contains("mínimo") || p.label().toLowerCase().contains("minimo"))) {
                list.add(new RecordDetailsDialog.PropertyItem("Stock Mínimo de Segurança", product.minStock().toPlainString() + " un."));
            }
            if (product.taxRateLabel() != null && list.stream().noneMatch(p -> p.label().toLowerCase().contains("taxa") || p.label().toLowerCase().contains("iva"))) {
                list.add(new RecordDetailsDialog.PropertyItem("Taxa de IVA", product.taxRateLabel()));
            }
            if (product.unitsPerBox() > 1 && list.stream().noneMatch(p -> p.label().toLowerCase().contains("caixa") || p.label().toLowerCase().contains("emb"))) {
                list.add(new RecordDetailsDialog.PropertyItem("Composição de Embalagem", product.unitsPerBox() + " un/cx (" + product.packagesPerBox() + " emb/cx)"));
            }
            if (product.description() != null && !product.description().isBlank() && list.stream().noneMatch(p -> p.label().toLowerCase().contains("descriç"))) {
                list.add(new RecordDetailsDialog.PropertyItem("Descrição Detalhada", product.description()));
            }
        } else if (client != null) {
            if (client.taxId() != null && !client.taxId().isBlank() && list.stream().noneMatch(p -> p.label().toLowerCase().contains("nuit") || p.label().toLowerCase().contains("nif"))) {
                list.add(new RecordDetailsDialog.PropertyItem("NUIT / NIF", client.taxId()));
            }
            if (client.email() != null && !client.email().isBlank() && list.stream().noneMatch(p -> p.label().toLowerCase().contains("email"))) {
                list.add(new RecordDetailsDialog.PropertyItem("Correio Electrónico", client.email()));
            }
            if (client.address() != null && !client.address().isBlank() && list.stream().noneMatch(p -> p.label().toLowerCase().contains("morada"))) {
                list.add(new RecordDetailsDialog.PropertyItem("Morada / Sede", client.address()));
            }
            if (client.creditLimit() != null && list.stream().noneMatch(p -> p.label().toLowerCase().contains("limite"))) {
                list.add(new RecordDetailsDialog.PropertyItem("Limite de Crédito", String.format("%.2f MT", client.creditLimit())));
            }
        }

        return list;
    }

    public static HeaderTheme resolveHeaderTheme(JTable table) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            String name = table.getColumnName(i);
            if (name == null) continue;
            String lower = name.toLowerCase();
            if (lower.contains("fatura") || lower.contains("fatura")) return new HeaderTheme("fas-file-invoice-dollar", UIHelper.ACCENT_CYAN);
            if (lower.contains("encomenda") || lower.contains("pedido")) return new HeaderTheme("fas-shopping-cart", UIHelper.ACCENT_BLUE);
            if (lower.contains("cliente")) return new HeaderTheme("fas-user-tie", UIHelper.ACCENT_BLUE);
            if (lower.contains("fornecedor")) return new HeaderTheme("fas-truck", UIHelper.ACCENT_ORANGE);
            if (lower.contains("desperdício") || lower.contains("quebra") || lower.contains("perda")) return new HeaderTheme("fas-trash-alt", UIHelper.REJECTED_RED);
            if (lower.contains("inventário") || lower.contains("contagem")) return new HeaderTheme("fas-clipboard-check", UIHelper.APPROVED_GREEN);
            if (lower.contains("artigo") || lower.contains("produto") || lower.contains("stock")) return new HeaderTheme("fas-boxes", UIHelper.APPROVED_GREEN);
            if (lower.contains("pagamento") || lower.contains("recibo")) return new HeaderTheme("fas-receipt", UIHelper.APPROVED_GREEN);
            if (lower.contains("cotação")) return new HeaderTheme("fas-file-alt", UIHelper.PENDING_YELLOW);
        }
        return new HeaderTheme("fas-file-contract", UIHelper.ACCENT_BLUE);
    }

    public static void configureDialogKpis(ExecutiveDetailDialog dialog, JTable table, int row, OrderDTO order, ProductDTO product) {
        String statusText = null;
        ExecutiveDetailDialog.StatusSeverity severity = ExecutiveDetailDialog.StatusSeverity.INFO;
        int kpiCount = 0;

        for (int col = 0; col < table.getColumnCount(); col++) {
            if (RecordDetailsDialog.isHidden(table, col)) continue;
            String colName = table.getColumnName(col);
            String valStr = RecordDetailsDialog.valueText(table.getValueAt(row, col));
            if (colName == null) continue;
            String colLower = colName.toLowerCase();

            if (colLower.contains("estado") || colLower.contains("status") || colLower.contains("activo") || colLower.contains("ativo")) {
                statusText = UIHelper.humanStatus(valStr);
                String upper = valStr.toUpperCase();
                if (upper.contains("OK") || upper.contains("ACTIV") || upper.contains("APROV") || upper.contains("CONCLU") || upper.contains("PAGO") || upper.contains("EMITID") || upper.contains("FATUR") || upper.contains("FACTUR") || upper.contains("BILLED") || upper.contains("INVOICED")) {
                    severity = ExecutiveDetailDialog.StatusSeverity.SUCCESS;
                } else if (upper.contains("PEND") || upper.contains("ALERTA") || upper.contains("SUSP") || upper.contains("ANALIS") || upper.contains("RASCUNHO")) {
                    severity = ExecutiveDetailDialog.StatusSeverity.WARNING;
                } else if (upper.contains("CANCEL") || upper.contains("ERRO") || upper.contains("INACT") || upper.contains("BLOQ") || upper.contains("REJEIT") || upper.contains("ANUL")) {
                    severity = ExecutiveDetailDialog.StatusSeverity.DANGER;
                }
            }
            if (kpiCount < 4 && (colLower.contains("total") || colLower.contains("valor") || colLower.contains("saldo")
                    || colLower.contains("preço") || colLower.contains("preco") || colLower.contains("qtd")
                    || colLower.contains("quantidade") || colLower.contains("limite"))) {
                Color accent = (colLower.contains("total") || colLower.contains("valor") || colLower.contains("preço") || colLower.contains("saldo"))
                        ? UIHelper.APPROVED_GREEN : UIHelper.ACCENT_CYAN;
                String kpiIcon = colLower.contains("qtd") || colLower.contains("quantidade") ? "fas-boxes" : "fas-money-bill-wave";
                dialog.addKpi(colName, valStr.isBlank() ? "—" : valStr, "Atributo destacado", accent, kpiIcon);
                kpiCount++;
            }
        }

        if (order != null && kpiCount == 0) {
            dialog.addKpi("Valor Total", String.format("%.2f MT", order.totalAmount()), "Total com impostos", UIHelper.APPROVED_GREEN, "fas-money-bill-wave");
            if (order.lines() != null) dialog.addKpi("Linhas de Artigos", String.valueOf(order.lines().size()), "Produtos no documento", UIHelper.ACCENT_BLUE, "fas-boxes");
            if (order.warehouseName() != null) dialog.addKpi("Armazém Origem", order.warehouseName(), "Local de expedição", UIHelper.ACCENT_CYAN, "fas-warehouse");
        } else if (product != null && kpiCount == 0) {
            if (product.unitPrice() != null) dialog.addKpi("Preço de Venda", String.format("%.2f MT", product.unitPrice()), "Preço unitário", UIHelper.APPROVED_GREEN, "fas-money-bill-wave");
            if (product.purchasePrice() != null) dialog.addKpi("Preço de Custo", String.format("%.2f MT", product.purchasePrice()), "Custo de aquisição", UIHelper.ACCENT_CYAN, "fas-coins");
            if (product.minStock() != null) dialog.addKpi("Stock Mínimo", product.minStock().toPlainString() + " un.", "Segurança", UIHelper.PENDING_YELLOW, "fas-cubes");
        }
        dialog.setStatusBadge(statusText != null && !statusText.isBlank() ? statusText : (order != null ? order.status() : "REGISTO"), severity);
    }
}
