package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Utilitário de formatação, colunas e configuração de tabela para pesquisa de produtos.
 */
public final class ProductLookupTableHelper {

    public static final String[] COLUMNS = {
            "Código / SKU", "Referência", "Artigo", "Categoria", "Preço Venda", "Embalagem", "Cód. Barras"
    };

    public static final int COL_SKU = 0;
    public static final int COL_REF = 1;
    public static final int COL_NAME = 2;
    public static final int COL_CATEGORY = 3;
    public static final int COL_PRICE = 4;
    public static final int COL_PACKAGING = 5;
    public static final int COL_BARCODE = 6;

    private ProductLookupTableHelper() {}

    public static DefaultTableModel createTableModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static void configureTable(JTable table, boolean multiSelect) {
        UIHelper.styleTable(table);
        table.setSelectionMode(multiSelect
                ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
                : ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(COL_PRICE).setCellRenderer(TableCellRenderers.money());

        table.getColumnModel().getColumn(COL_SKU).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_REF).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_NAME).setPreferredWidth(260);
        table.getColumnModel().getColumn(COL_CATEGORY).setPreferredWidth(120);
        table.getColumnModel().getColumn(COL_PRICE).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_PACKAGING).setPreferredWidth(100);
        table.getColumnModel().getColumn(COL_BARCODE).setPreferredWidth(110);
    }

    public static void addProductRow(DefaultTableModel model, ProductDTO p) {
        model.addRow(new Object[]{
                firstText(p.sku(), "—"),
                firstText(p.reference(), "—"),
                safe(p.name()),
                firstText(p.categoryName(), "Sem categoria"),
                p.unitPrice() != null ? p.unitPrice() : java.math.BigDecimal.ZERO,
                formatPackaging(p),
                firstText(p.barcode(), "—")
        });
    }

    public static List<ProductLookupDialog.CategoryOption> toOptions(List<ProductCategoryDTO> categories) {
        if (categories == null || categories.isEmpty()) return List.of();
        List<ProductLookupDialog.CategoryOption> options = new ArrayList<>();
        for (ProductCategoryDTO c : categories) {
            if (c != null && c.name() != null) {
                options.add(new ProductLookupDialog.CategoryOption(c.id(), c.name()));
            }
        }
        return options;
    }

    public static String formatPackaging(ProductDTO p) {
        if (p == null) return "—";
        int unitsPerBox = p.unitsPerBox();
        int packages = p.packagesPerBox();
        int unitsPerPkg = p.unitsPerPackage();
        if (unitsPerBox > 1) {
            if (packages > 1 && unitsPerPkg > 1) {
                return packages + " emb x " + unitsPerPkg + " un (" + unitsPerBox + " un/cx)";
            }
            return unitsPerBox + " un/cx";
        }
        return "1 un";
    }

    public static String normalize(String value) {
        String plain = Normalizer.normalize(safe(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return plain.toLowerCase(Locale.ROOT).trim();
    }

    public static String firstText(String primary, String fallback) {
        return (primary != null && !primary.isBlank()) ? primary.trim() : fallback;
    }

    public static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
