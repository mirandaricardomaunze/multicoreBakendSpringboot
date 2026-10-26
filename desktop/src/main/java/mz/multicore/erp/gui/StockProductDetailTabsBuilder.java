package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.RecordDetailsDialog;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.ProductBatchDTO;
import mz.multicore.erp.modules.inventory.dto.StockDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Construtor das abas de detalhe da ficha de artigo (especificações, saldos e lotes).
 */
public final class StockProductDetailTabsBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private StockProductDetailTabsBuilder() {}

    public static JComponent buildTabsSection(StockPanel owner, ProductDTO product) {
        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);

        tabs.addTab("Especificações & Logística", UIHelper.icon("fas-info-circle", 15, UIHelper.ACCENT_BLUE), buildSpecsTab(product));
        tabs.addTab("Saldos por Armazém",        UIHelper.icon("fas-warehouse", 15, UIHelper.ACCENT_CYAN), buildWarehousesTab(owner, product));
        tabs.addTab("Lotes & Validades",         UIHelper.icon("fas-calendar-times", 15, UIHelper.ACCENT_ORANGE), buildBatchesTab(owner, product));

        return tabs;
    }

    public static JComponent buildSpecsTab(ProductDTO product) {
        String boxGrossWeight = (product.grossBoxWeightKg() != null && product.grossBoxWeightKg().signum() > 0)
                ? product.grossBoxWeightKg() + " kg" : "—";

        List<RecordDetailsDialog.PropertyItem> items = List.of(
                new RecordDetailsDialog.PropertyItem("Tipo de Venda Comercial", "WEIGHT".equalsIgnoreCase(product.saleType()) ? "Venda ao Peso (Balança / Kg)" : "Venda por Unidade"),
                new RecordDetailsDialog.PropertyItem("Rastreio Físico de Stock", product.stockTracked() ? "Activo (Controlo rigoroso)" : "Inactivo (Serviço / Sem stock)"),
                new RecordDetailsDialog.PropertyItem("Stock Mínimo de Alerta", (product.minStock() != null ? product.minStock() : "0") + " unidades"),
                new RecordDetailsDialog.PropertyItem("Embalagens Intermédias por Caixa", String.valueOf(product.packagesPerBox())),
                new RecordDetailsDialog.PropertyItem("Unidades por Embalagem", String.valueOf(product.unitsPerPackage())),
                new RecordDetailsDialog.PropertyItem("Total de Unidades por Caixa", String.valueOf(product.unitsPerBox())),
                new RecordDetailsDialog.PropertyItem("Peso Líquido Unitário", product.netUnitWeightKg() != null ? product.netUnitWeightKg() + " kg" : "—"),
                new RecordDetailsDialog.PropertyItem("Peso Bruto Unitário", product.grossUnitWeightKg() != null ? product.grossUnitWeightKg() + " kg" : "—"),
                new RecordDetailsDialog.PropertyItem("Peso Total da Caixa Fechada", boxGrossWeight),
                new RecordDetailsDialog.PropertyItem("Descrição / Notas Técnicas", product.description() != null && !product.description().isBlank() ? product.description() : "Sem notas adicionais.")
        );

        return RecordDetailsDialog.buildPropertySheet(items);
    }

    public static JPanel buildWarehousesTab(StockPanel owner, ProductDTO product) {
        String[] cols = {"Armazém", "Unidades em Stock", "Caixas Calculadas", "Stock Mínimo", "Estado"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        if (owner != null && owner.stocksList != null) {
            for (StockDTO s : owner.stocksList) {
                if (s.productId() != null && s.productId().equals(product.id())) {
                    BigDecimal qty = s.quantity() != null ? s.quantity() : BigDecimal.ZERO;
                    int upb = s.unitsPerBox() > 0 ? s.unitsPerBox() : (product.unitsPerBox() > 0 ? product.unitsPerBox() : 1);
                    long boxes = qty.divide(BigDecimal.valueOf(upb), 0, RoundingMode.DOWN).longValue();
                    String st = qty.signum() <= 0 ? "Esgotado" : (qty.compareTo(s.minStock() != null ? s.minStock() : BigDecimal.ZERO) < 0 ? "Baixo" : "Disponível");
                    model.addRow(new Object[]{s.warehouseName(), qty, boxes, s.minStock(), st});
                }
            }
        }

        JTable table = new JTable(model);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.status());

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(8, 6, 6, 6));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    public static JPanel buildBatchesTab(StockPanel owner, ProductDTO product) {
        String[] cols = {"Lote", "Armazém", "Validade", "Dias Restantes", "Quantidade", "Estado"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        if (owner != null && owner.batchesList != null) {
            List<ProductBatchDTO> batches = owner.batchesList;
            if (batches != null) {
                LocalDate today = LocalDate.now();
                for (ProductBatchDTO b : batches) {
                    if (b.productId() != null && b.productId().equals(product.id())) {
                        String expStr = b.expirationDate() != null ? b.expirationDate().format(DATE_FMT) : "—";
                        String daysStr = "—";
                        String state = "Válido";
                        if (b.expirationDate() != null) {
                            long days = ChronoUnit.DAYS.between(today, b.expirationDate());
                            daysStr = days + " d";
                            if (days < 0) state = "Expirado";
                            else if (days <= 30) state = "Crítico";
                        }
                        model.addRow(new Object[]{b.batchNumber(), b.warehouseName(), expStr, daysStr, b.quantity(), state});
                    }
                }
            }
        }

        JTable table = new JTable(model);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());

        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(8, 6, 6, 6));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }
}
