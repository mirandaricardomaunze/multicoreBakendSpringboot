package mz.multicore.erp.gui.landedcost;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.PurchaseApiClient;
import mz.multicore.erp.gui.ComprasPanel;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.modules.fiscal.dto.TaxRateDTO;
import mz.multicore.erp.modules.purchases.dto.*;
import mz.multicore.erp.modules.purchases.model.LandedCostAllocationMethod;
import mz.multicore.erp.modules.purchases.model.LandedCostType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Harness de UI dos custos de importação (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §4.5, LC-UI-01):
 * Compras › Custos de Importação com compras recebidas, linhas de custo, método, pré-visualização por artigo
 * e confirmação.
 */
class LandedCostUiHarnessTest {

    private static PurchaseDTO purchase(long id, String number, String status, Long warehouseId) {
        return new PurchaseDTO(id, number, 9L, "Fornecedor China", warehouseId, 1L, new BigDecimal("1000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, status, LocalDateTime.of(2026, 11, 20, 10, 0), List.of(),
                null, null, BigDecimal.ZERO, "USD", new BigDecimal("63.9055"), new BigDecimal("63905.50"));
    }

    private static LandedCostChargeDraft freight() {
        return new LandedCostChargeDraft(new LandedCostChargeRequest(LandedCostType.FRETE, 5L, "MZN", null,
                new BigDecimal("5000.00"), 3L), "Transportes Lda", "1,0000", "IVA 16%");
    }

    private static LandedCostPanel panel() {
        return new LandedCostPanel(mock(PurchaseApiClient.class), mock(ComercialApiClient.class), null);
    }

    @Test
    @DisplayName("LC-UI-01: Compras ganham o separador Custos de Importação, com ícone, depois das abas existentes")
    void lcUi01_comprasTemSeparadorDeCustosDeImportacao() throws Exception {
        onEdt(() -> {
            ComprasPanel compras = new ComprasPanel(mock(PurchaseApiClient.class), mock(InventoryApiClient.class),
                    mock(ComercialApiClient.class), mock(FinanceApiClient.class), null, null);
            JTabbedPane tabs = firstOf(compras, JTabbedPane.class);
            int index = tabs.indexOfTab("Custos de Importação");
            assertThat(index).isGreaterThan(tabs.indexOfTab("Contas a Pagar"));
            assertThat(tabs.getComponentAt(index)).isInstanceOf(LandedCostPanel.class);
            assertThat(tabs.getIconAt(index)).as("ícone pelo UIHelper.icon").isNotNull();
            assertThat(tabs.getTitleAt(0)).isEqualTo("Faturas de Compra (V/FT)");
        });
    }

    @Test
    @DisplayName("LC-UI-01: Só compras recebidas com armazém entram na lista; dívidas de CI e Saldo Inicial ficam de fora")
    void lcUi01_soComprasRecebidasComArmazem() throws Exception {
        onEdt(() -> {
            LandedCostPanel panel = panel();
            panel.applyPurchases(List.of(
                    purchase(1L, "V/FT 2026/1", "COMPLETED", 2L),
                    purchase(2L, "V/FT 2026/2", "PENDING", 2L),
                    purchase(3L, "CI-2026/1-1", "COMPLETED", null)));

            JTable table = panel.purchasesTable();
            assertThat(table.getColumnName(0)).isEqualTo("Escolher");
            assertThat(table.getColumnName(1)).isEqualTo("Nº Documento");
            assertThat(table.getColumnName(4)).isEqualTo("Moeda");
            assertThat(table.getColumnName(5)).isEqualTo("Total (moeda base)");
            TableModel model = table.getModel();
            assertThat(model.getRowCount()).isEqualTo(1);
            assertThat(model.getValueAt(0, 1)).isEqualTo("V/FT 2026/1");
            assertThat(model.getValueAt(0, 4)).isEqualTo("USD");
            assertThat((BigDecimal) model.getValueAt(0, 5)).isEqualByComparingTo("63905.50");
            assertThat(model.isCellEditable(0, 0)).as("escolher a compra").isTrue();
            assertThat(model.isCellEditable(0, 1)).isFalse();
            assertThat(SwingUtilities.getAncestorOfClass(ModernPanel.class, table)).as("tabela num card").isNotNull();
        });
    }

    @Test
    @DisplayName("LC-UI-01: Sem compras escolhidas ou sem linhas de custo, o aviso aparece sem chamar o backend")
    void lcUi01_avisosSemChamarBackend() throws Exception {
        PurchaseApiClient client = mock(PurchaseApiClient.class);
        onEdt(() -> {
            LandedCostPanel panel = new LandedCostPanel(client, mock(ComercialApiClient.class), null);
            panel.applyPurchases(List.of(purchase(1L, "V/FT 2026/1", "COMPLETED", 2L)));
            assertThatThrownBy(panel::request).hasMessageContaining("compra recebida");

            panel.purchasesTable().getModel().setValueAt(Boolean.TRUE, 0, 0);
            assertThatThrownBy(panel::request).hasMessageContaining("linha de custo");

            panel.previewButton().doClick();
            assertThat(panel.feedback().feedbackType()).isEqualTo(FeedbackType.WARNING);
            assertThat(panel.feedback().messageText()).contains("linha de custo");
        });
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("LC-UI-01: O pedido leva as compras escolhidas, as linhas de custo e o método (Valor por omissão)")
    void lcUi01_pedidoComComprasLinhasEMetodo() throws Exception {
        AtomicReference<LandedCostPanel> created = new AtomicReference<>();
        onEdt(() -> {
            LandedCostPanel panel = panel();
            created.set(panel);
            panel.applyPurchases(List.of(purchase(1L, "V/FT 2026/1", "COMPLETED", 2L),
                    purchase(4L, "V/FT 2026/4", "COMPLETED", 2L)));
            panel.purchasesTable().getModel().setValueAt(Boolean.TRUE, 1, 0);
            panel.addCharge(freight());

            JTable charges = panel.chargesTable();
            assertThat(charges.getColumnName(0)).isEqualTo("Tipo");
            assertThat(charges.getColumnName(5)).isEqualTo("IVA");
            assertThat(charges.getModel().getValueAt(0, 0)).isEqualTo("Frete");
            assertThat(charges.getModel().getValueAt(0, 1)).isEqualTo("Transportes Lda");
            assertThat(charges.getModel().getValueAt(0, 5)).isEqualTo("IVA 16%");

            assertThat(panel.methodCombo().getSelectedItem()).isEqualTo(LandedCostAllocationMethod.VALOR);
            CreateLandedCostRequest request = panel.request();
            assertThat(request.purchaseIds()).containsExactly(4L);
            assertThat(request.allocationMethod()).isEqualTo(LandedCostAllocationMethod.VALOR);
            assertThat(request.charges()).containsExactly(freight().request());

            panel.methodCombo().setSelectedItem(LandedCostAllocationMethod.PESO);
            assertThat(panel.request().allocationMethod()).isEqualTo(LandedCostAllocationMethod.PESO);
        });
        // The paginated table view refreshes on a later EDT turn.
        onEdt(() -> {
            LandedCostPanel panel = created.get();
            JTable charges = panel.chargesTable();
            charges.setRowSelectionInterval(0, 0);
            panel.removeChargeButton().doClick();
            assertThat(charges.getModel().getRowCount()).isZero();
        });
    }

    @Test
    @DisplayName("LC-UI-01: Pré-visualização por artigo (original, acrescentado, final) e resumo; confirmar só depois dela")
    void lcUi01_previsualizacaoActivaConfirmacao() throws Exception {
        onEdt(() -> {
            LandedCostPanel panel = panel();
            panel.applyPurchases(List.of(purchase(1L, "V/FT 2026/1", "COMPLETED", 2L)));
            panel.purchasesTable().getModel().setValueAt(Boolean.TRUE, 0, 0);
            panel.addCharge(freight());
            assertThat(panel.confirmButton().isEnabled()).as("sem pré-visualização").isFalse();

            panel.applyPreview(new LandedCostPreviewDTO(LandedCostAllocationMethod.VALOR, new BigDecimal("5000.00"),
                    new BigDecimal("800.00"), new BigDecimal("5800.00"), List.of(new LandedCostAllocationDTO(
                    11L, "V/FT 2026/1", 7L, "Motor 2HP", new BigDecimal("10"), new BigDecimal("8"),
                    new BigDecimal("5000.00"), new BigDecimal("4000.00"), new BigDecimal("1000.00"),
                    new BigDecimal("6390.55"), new BigDecimal("6890.55")))));

            JTable preview = panel.previewTable();
            assertThat(preview.getColumnName(1)).isEqualTo("Artigo");
            assertThat(preview.getColumnName(3)).isEqualTo("Custo unit. original");
            assertThat(preview.getColumnName(4)).isEqualTo("Custo acrescentado");
            assertThat(preview.getColumnName(5)).isEqualTo("Custo unit. final");
            assertThat(preview.getModel().getValueAt(0, 1)).isEqualTo("Motor 2HP");
            assertThat((BigDecimal) preview.getModel().getValueAt(0, 3)).isEqualByComparingTo("6390.55");
            assertThat((BigDecimal) preview.getModel().getValueAt(0, 4)).isEqualByComparingTo("5000.00");
            assertThat((BigDecimal) preview.getModel().getValueAt(0, 5)).isEqualByComparingTo("6890.55");
            assertThat(panel.previewSummary().getText()).contains("5.000,00").contains("800,00").contains("5.800,00");
            assertThat(panel.confirmButton().isEnabled()).isTrue();

            panel.methodCombo().setSelectedItem(LandedCostAllocationMethod.QUANTIDADE);
            assertThat(panel.confirmButton().isEnabled()).as("mudar o método apaga a pré-visualização").isFalse();
            assertThat(preview.getModel().getRowCount()).isZero();
        });
    }

    @Test
    @DisplayName("LC-UI-01: Confirmado, mostra o CI e as dívidas criadas e limpa o formulário")
    void lcUi01_confirmacaoMostraCiELimpa() throws Exception {
        onEdt(() -> {
            LandedCostPanel panel = panel();
            panel.applyPurchases(List.of(purchase(1L, "V/FT 2026/1", "COMPLETED", 2L)));
            panel.purchasesTable().getModel().setValueAt(Boolean.TRUE, 0, 0);
            panel.addCharge(freight());

            panel.applyConfirmed(new LandedCostDocumentDTO(30L, "CI-2026/3", LocalDate.of(2026, 11, 22),
                    LandedCostAllocationMethod.VALOR, List.of(1L), List.of("V/FT 2026/1"),
                    List.of(new LandedCostChargeDTO(LandedCostType.FRETE, 5L, "Transportes Lda", "MZN", BigDecimal.ONE,
                            new BigDecimal("5000.00"), new BigDecimal("16"), new BigDecimal("800.00"),
                            new BigDecimal("5800.00"), new BigDecimal("5000.00"), new BigDecimal("800.00"),
                            40L, "CI-2026/3-1")), List.of()));

            assertThat(panel.feedback().feedbackType()).isEqualTo(FeedbackType.SUCCESS);
            assertThat(panel.feedback().titleText()).contains("CI-2026/3");
            assertThat(panel.feedback().messageText()).contains("CI-2026/3-1");
            assertThat(panel.chargesTable().getModel().getRowCount()).isZero();
            assertThat(panel.purchasesTable().getModel().getValueAt(0, 0)).isEqualTo(Boolean.FALSE);
            assertThat(panel.confirmButton().isEnabled()).isFalse();
        });
    }

    @Test
    @DisplayName("LC-UI-01: Linha de custo — moeda e câmbio do campo dos documentos, IVA da empresa, IVA aduaneiro sem taxa")
    void lcUi01_dialogoDaLinhaDeCusto() throws Exception {
        onEdt(() -> {
            LandedCostChargeDialog dialog = new LandedCostChargeDialog(
                    List.of(new SupplierDTO(5L, "Transportes Lda", null, null, null, null, null, true, 1L)),
                    List.of(new TaxRateDTO(3L, "IVA16", "IVA 16%", "VAT", new BigDecimal("16"), null, true)), null);
            assertThat(dialog.vatCombo().getItemAt(0).label()).isEqualTo("Sem IVA");
            dialog.vatCombo().setSelectedIndex(1);
            dialog.amountField().setText("5000");
            LandedCostChargeDraft draft = dialog.draft();
            assertThat(draft.request().type()).isEqualTo(LandedCostType.FRETE);
            assertThat(draft.request().supplierId()).isEqualTo(5L);
            assertThat(draft.request().taxRateId()).isEqualTo(3L);
            assertThat(draft.request().amount()).isEqualByComparingTo("5000");
            assertThat(draft.vatLabel()).isEqualTo("IVA 16%");

            dialog.typeCombo().setSelectedItem(LandedCostType.IVA_ADUANEIRO);
            assertThat(dialog.vatCombo().isEnabled()).isFalse();
            assertThat(dialog.draft().request().taxRateId()).as("IVA aduaneiro não leva taxa").isNull();
        });
        String dialog = Files.readString(source("LandedCostChargeDialog.java"));
        assertThat(dialog).contains("new ModernFormDialog(").contains("new DocumentCurrencyField(")
                .doesNotContain("JOptionPane");
        assertThat(Files.readString(source("LandedCostPanel.java"))).contains("UIHelper.icon(")
                .contains("purchaseApiClient.previewLandedCost(").contains("purchaseApiClient.confirmLandedCost(")
                .doesNotContain("JOptionPane");
        Path client = Path.of("src", "main", "java", "mz", "multicore", "erp", "desktop", "client", "PurchaseApiClient.java");
        if (!Files.exists(client)) client = Path.of("desktop").resolve(client);
        assertThat(Files.readString(client)).contains("\"/api/purchases/landed-costs/preview\"")
                .contains("\"/api/purchases/landed-costs\"");
    }

    private static Path source(String file) {
        Path nested = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "landedcost", file);
        return Files.exists(nested) ? nested : Path.of("desktop").resolve(nested);
    }

    private static <T extends Component> T firstOf(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container container) {
                T found = firstOf(container, type);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void onEdt(Runnable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                failure.set(t);
            }
        });
        if (failure.get() instanceof Error error) throw error;
        if (failure.get() instanceof Exception exception) throw exception;
    }
}
