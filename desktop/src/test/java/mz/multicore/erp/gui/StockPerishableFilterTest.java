package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.modules.inventory.dto.StockDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver docs/PRODUTO_PERECIVEL_VALIDADE_SPEC.md: rever na lista de stock que produtos exigem validade. */
class StockPerishableFilterTest {

    @Test
    @DisplayName("Coluna Validade e filtros Perecíveis / Sem validade nos níveis de stock")
    void perishableColumnAndFilters() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(1L);
            try {
                StockPanel panel = new StockPanel(Mockito.mock(InventoryApiClient.class), Mockito.mock(ComercialApiClient.class),
                        Mockito.mock(StockTransferApiClient.class), Mockito.mock(InventoryCountApiClient.class),
                        Mockito.mock(ProductCategoryApiClient.class), Mockito.mock(PrintApiClient.class),
                        Mockito.mock(StockWasteApiClient.class));
                panel.stocksList = new ArrayList<>(List.of(
                        stock(1L, "Iogurte", true), stock(2L, "Prato de loiça", false)));

                panel.stockStatusCombo.setSelectedIndex(0);
                StockLevelsFilter.filterStocks(panel);
                int validade = panel.stockModel.findColumn("Validade");
                assertThat(panel.stockModel.getRowCount()).isEqualTo(2);
                assertThat(panel.stockModel.getValueAt(0, validade)).isEqualTo("Perecível");
                assertThat(panel.stockModel.getValueAt(1, validade)).isEqualTo("Sem validade");

                panel.stockStatusCombo.setSelectedItem("Perecíveis");
                StockLevelsFilter.filterStocks(panel);
                assertThat(panel.stockRowProductIds).containsExactly(1L);

                panel.stockStatusCombo.setSelectedItem("Sem validade");
                StockLevelsFilter.filterStocks(panel);
                assertThat(panel.stockRowProductIds).containsExactly(2L);
            } finally {
                CurrentUserContext.clear();
            }
        });
    }

    private static StockDTO stock(Long productId, String name, boolean perishable) {
        return new StockDTO(productId, productId, "SKU" + productId, null, null, name, 1L, "Loja",
                BigDecimal.TEN, BigDecimal.ONE, null, BigDecimal.ONE, 1, null, null, perishable);
    }
}
