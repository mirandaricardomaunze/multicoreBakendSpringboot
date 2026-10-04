package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductSearchComboBoxHarnessTest {

    @Test
    void pesquisar_porCadaIdentificador_encontraApenasProdutoCorrespondente() throws Exception {
        ProductSearchComboBox combo = createCombo();

        assertSingleMatch(combo, "agua", 1L);
        assertSingleMatch(combo, "AG-01", 1L);
        assertSingleMatch(combo, "REF-AG", 1L);
        assertSingleMatch(combo, "5600001", 1L);
        assertSingleMatch(combo, "hidratacao", 1L);
        assertSingleMatch(combo, "bebidas", 1L);
    }

    @Test
    void pesquisar_nomeSemAcento_filtraProdutoTipado() throws Exception {
        ProductSearchComboBox combo = createCombo();

        JTextField editor = (JTextField) combo.getEditor().getEditorComponent();
        SwingUtilities.invokeAndWait(() -> editor.setText("agua 5600001"));
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(1, combo.getItemCount());
        assertEquals(1L, combo.selectedProduct().id());
    }

    @Test
    void pesquisar_semResultado_deixaSeleccaoVazia() throws Exception {
        ProductSearchComboBox[] holder = new ProductSearchComboBox[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ProductSearchComboBox();
            holder[0].setProducts(List.of(product(1L, "AG-01", "REF-AG", "5600001", "Água", "Bebidas")));
        });

        JTextField editor = (JTextField) holder[0].getEditor().getEditorComponent();
        SwingUtilities.invokeAndWait(() -> editor.setText("produto inexistente"));
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(0, holder[0].getItemCount());
        assertNull(holder[0].selectedProduct());
    }

    @Test
    void seleccionarPorId_eLimparPesquisa_preservaCatalogo() throws Exception {
        ProductSearchComboBox combo = createCombo();
        SwingUtilities.invokeAndWait(() -> assertTrue(combo.selectProduct(2L)));
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(2L, combo.selectedProduct().id());
        SwingUtilities.invokeAndWait(combo::clearSearch);

        assertEquals(2, combo.getItemCount());
        assertEquals(2L, combo.selectedProduct().id());
        assertEquals(UIHelper.FORM_CONTROL_HEIGHT, combo.getPreferredSize().height);
    }

    @Test
    void rotulo_apresentaCodigoBarcodeNomeEPreco() {
        String label = ProductSearchComboBox.displayLabel(
                product(1L, "AG-01", "REF-AG", "5600001", "Água Mineral", "Bebidas"));

        assertTrue(label.contains("REF-AG"));
        assertTrue(label.contains("5600001"));
        assertTrue(label.contains("Água Mineral"));
        assertTrue(label.contains("100.00 MT"));
    }

    private ProductSearchComboBox createCombo() throws Exception {
        ProductSearchComboBox[] holder = new ProductSearchComboBox[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new ProductSearchComboBox();
            holder[0].setProducts(List.of(
                    product(1L, "AG-01", "REF-AG", "5600001", "Água Mineral", "Bebidas",
                            "Bebida para hidratação diária"),
                    product(2L, "AR-02", "REF-AR", "5600002", "Arroz Premium", "Mercearia",
                            "Cereal de grão longo")));
        });
        return holder[0];
    }

    private void assertSingleMatch(ProductSearchComboBox combo, String query, Long expectedId)
            throws Exception {
        JTextField editor = (JTextField) combo.getEditor().getEditorComponent();
        SwingUtilities.invokeAndWait(() -> editor.setText(query));
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(1, combo.getItemCount(), "Pesquisa: " + query);
        assertEquals(expectedId, combo.selectedProduct().id(), "Pesquisa: " + query);
    }

    private ProductDTO product(Long id, String sku, String reference, String barcode,
                               String name, String category) {
        return product(id, sku, reference, barcode, name, category, null);
    }

    private ProductDTO product(Long id, String sku, String reference, String barcode,
                               String name, String category, String description) {
        return new ProductDTO(id, sku, reference, barcode, name,
                new BigDecimal("100.00"), new BigDecimal("60.00"), BigDecimal.ZERO,
                null, null, 12, 2, 6, "UNIT", true, 1L, category,
                1L, new BigDecimal("0.16"), "IVA 16%", description, null,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
