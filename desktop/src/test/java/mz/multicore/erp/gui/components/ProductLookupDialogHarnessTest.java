package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductLookupDialogHarnessTest {

    @Test
    void tabela_comPaginacao_apresentaProdutosEFiltragemCorrectamente() throws Exception {
        List<ProductDTO> products = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            products.add(createProduct((long) i, "SKU-" + String.format("%03d", i),
                    "REF-" + i, "560000" + i,
                    i % 2 == 0 ? "Açúcar Branco Tipo " + i : "Arroz Agulha Tipo " + i,
                    i % 2 == 0 ? "Mercearia" : "Cereais",
                    BigDecimal.valueOf(50 + i)));
        }

        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(
                    new String[]{"Código / SKU", "Referência", "Artigo", "Categoria", "Preço Venda", "Embalagem", "Cód. Barras"}, 0);
            for (ProductDTO p : products) {
                model.addRow(new Object[]{p.sku(), p.reference(), p.name(), p.categoryName(), p.unitPrice(), "1 un", p.barcode()});
            }

            JTable table = new JTable(model);
            UIHelper.styleTable(table);
            var pagination = ClientTablePagination.install(table);

            assertNotNull(pagination);
            assertTrue(ClientTablePagination.isInstalled(table));
            assertEquals(60, table.getModel().getRowCount());
            // Na página inicial (tamanho 50), o sorter mostra 50 linhas
            assertEquals(50, table.getRowCount());

            // Filtro por query "Açúcar"
            ClientTablePagination.setBaseFilter(table, new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    int row = entry.getIdentifier();
                    return products.get(row).name().toLowerCase().contains("açúcar");
                }
            });

            // 30 produtos correspondem a "Açúcar"
            assertEquals(30, table.getRowCount());
        });
    }

    @Test
    void provedorRemoto_devolvePaginaCorrectaComContratos() throws Exception {
        ProductLookupDialog.ProductPageProvider provider = (query, categoryId, page, size) -> {
            List<ProductDTO> items = List.of(
                    createProduct(101L, "SKU-REM-1", "REF-REM", "560999", "Leite Fresco", "Laticínios", BigDecimal.valueOf(95.0))
            );
            return new mz.multicore.erp.architecture.paging.PageResponse<>(items, page, size, 1L, 1);
        };

        var pageResp = provider.fetchPage("Leite", null, 0, 50);
        assertNotNull(pageResp);
        assertEquals(1, pageResp.items().size());
        assertEquals("Leite Fresco", pageResp.items().get(0).name());
        assertEquals(0, pageResp.page());
        assertEquals(1L, pageResp.totalElements());
    }

    private ProductDTO createProduct(Long id, String sku, String ref, String barcode, String name, String category, BigDecimal price) {
        return new ProductDTO(
                id, sku, ref, barcode, name,
                price, BigDecimal.TEN, BigDecimal.ONE,
                price, BigDecimal.ONE, 12, 1, 12,
                "STANDARD", true, 1L, category, 1L, BigDecimal.valueOf(0.16),
                "IVA 16%", "Descrição", null, BigDecimal.ONE, BigDecimal.ONE, 0L
        );
    }
}
