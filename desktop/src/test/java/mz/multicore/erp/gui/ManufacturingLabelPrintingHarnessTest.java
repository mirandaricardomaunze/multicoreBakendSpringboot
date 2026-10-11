package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ShelfLabelsDialog;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ManufacturingLabelPrintingHarnessTest {

    @Test
    void shelfLabelCard_comLoteEValidade_mantemDimensoesCanonica() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ProductDTO p = new ProductDTO(
                    501L, "SKU-FAR-25", "REF-OP-99", "7909988776655", "Farinha Especial de Milho 25kg",
                    BigDecimal.valueOf(1150.00), BigDecimal.valueOf(900.00), BigDecimal.valueOf(5),
                    BigDecimal.valueOf(1100.00), BigDecimal.valueOf(10), 1, "BAG",
                    true, 1L, "FARINHAS", 1L, BigDecimal.valueOf(16), "IVA 16%",
                    "Farinha produzida em linha", null, BigDecimal.ONE, BigDecimal.ONE
            );

            // Testa com Lote da Ordem de Produção e Validade
            JPanel card = ShelfLabelsDialog.createSingleLabelCard(
                    p,
                    ShelfLabelsDialog.PackagingLevel.UNIT,
                    "OP-2026-0089",
                    "10/2027"
            );

            assertNotNull(card);
            assertEquals(240, card.getPreferredSize().width, "Largura do cartão deve permanecer exatamente 240px");
            assertEquals(130, card.getPreferredSize().height, "Altura do cartão deve permanecer exatamente 130px");
        });
    }

    @Test
    void shelfLabelsDialog_instanciaComLotePreenchidoECopias() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ProductDTO p = new ProductDTO(
                    502L, "SKU-SUMO-01", "REF-OP-10", "7901122334455", "Sumo Natural Manga 1L",
                    BigDecimal.valueOf(65.00), BigDecimal.valueOf(40.00), BigDecimal.valueOf(5),
                    BigDecimal.valueOf(60.00), BigDecimal.valueOf(10), 1, "UNIT",
                    true, 1L, "BEBIDAS", 1L, BigDecimal.valueOf(16), "IVA 16%",
                    "Sumo pasteurizado", null, BigDecimal.ONE, BigDecimal.ONE
            );

            ShelfLabelsDialog dialog = new ShelfLabelsDialog(null, List.of(p), "OP-2026-0090", 25);
            assertThat(dialog.getTitle()).contains("Etiquetas");
            dialog.dispose();
        });
    }
}
