package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.RecordDetailsDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.inventory.PhysicalInventoryPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: Alertas e Orientação de Acções na Interface (GUI_ACTION_ALERT_GUIDANCE_SPEC)")
public class GuiActionAlertGuidanceHarnessTest {

    @Test
    @DisplayName("ALERT-01: RecordDetailsDialog emite Toast informativo quando nenhuma linha está seleccionada")
    void testRecordDetailsDialogShowsToastWhenNoRowSelected() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Nome"}, 0);
            model.addRow(new Object[]{"1", "Teste"});
            JTable table = new JTable(model);
            table.clearSelection();

            AtomicReference<String> toastMessage = new AtomicReference<>();
            AtomicReference<FeedbackType> toastType = new AtomicReference<>();
            ToastManager.setTestListener((type, msg) -> {
                toastType.set(type);
                toastMessage.set(msg);
            });

            try {
                RecordDetailsDialog.show(table);
                assertNotNull(toastMessage.get(), "Deve emitir Toast ao tentar abrir detalhes sem linha seleccionada");
                assertEquals(FeedbackType.INFO, toastType.get(), "Toast deve ser do tipo INFO");
                assertTrue(toastMessage.get().contains("Seleccione uma linha na tabela"),
                        "Mensagem deve orientar a selecção de linha: " + toastMessage.get());
            } finally {
                ToastManager.setTestListener(null);
            }
        });
    }

    @Test
    @DisplayName("ALERT-02: POS Balança avisa operador quando carrinho está vazio ou balança a zero")
    void testPosScaleCaptureGuidance() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            POSPanel pos = new POSPanel(null, null, null, null, null, null, null, null);
            assertNotNull(pos.catalogController);

            // 1. Balança a zero
            pos.catalogController.handleScaleWeightCaptured(BigDecimal.ZERO);
            assertEquals("Balança a zero", pos.feedback.titleText());
            assertEquals(FeedbackType.WARNING, pos.feedback.feedbackType());

            // 2. Carrinho vazio
            pos.catalogController.handleScaleWeightCaptured(new BigDecimal("1.450"));
            assertEquals("Carrinho vazio", pos.feedback.titleText());
            assertEquals(FeedbackType.WARNING, pos.feedback.feedbackType());
        });
    }

    @Test
    @DisplayName("ALERT-03: ManufacturingPanel avisa operador quando nenhuma OP está seleccionada")
    void testManufacturingPanelWarnsWhenNoSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ManufacturingPanel panel = new ManufacturingPanel(null, null, null);
            panel.ordersTable.clearSelection();

            panel.showOrderActions();
            assertEquals("Seleccione uma ordem", panel.feedback.titleText());
            assertEquals(FeedbackType.WARNING, panel.feedback.feedbackType());
            assertTrue(panel.feedback.messageText().contains("Seleccione uma Ordem de Produção"),
                    "Mensagem deve orientar o operador: " + panel.feedback.messageText());
        });
    }

    @Test
    @DisplayName("ALERT-04: FleetPanel avisa operador quando nenhuma viatura está seleccionada")
    void testFleetPanelWarnsWhenNoSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FleetPanel panel = new FleetPanel(null);
            panel.table.clearSelection();

            panel.showFuelHistory();
            assertEquals("Seleccione uma viatura", panel.feedback.titleText());
            assertEquals(FeedbackType.WARNING, panel.feedback.feedbackType());
            assertTrue(panel.feedback.messageText().contains("Seleccione uma viatura na tabela"),
                    "Mensagem deve orientar o operador: " + panel.feedback.messageText());
        });
    }

    @Test
    @DisplayName("ALERT-05: AssetsPanel avisa operador quando nenhum activo está seleccionado")
    void testAssetsPanelWarnsWhenNoSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AssetsPanel panel = new AssetsPanel(null);
            panel.table.clearSelection();

            panel.showAssetDetails();
            assertEquals("Seleccione um activo", panel.feedback.titleText());
            assertEquals(FeedbackType.WARNING, panel.feedback.feedbackType());
            assertTrue(panel.feedback.messageText().contains("Seleccione um activo na tabela"),
                    "Mensagem deve orientar o operador: " + panel.feedback.messageText());
        });
    }

    @Test
    @DisplayName("ALERT-06: BankReconciliationPanel emite Toast avisando quando não há linha seleccionada")
    void testBankReconciliationPanelWarnsWhenNoSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BankReconciliationPanel panel = new BankReconciliationPanel(null, null);
            panel.itemsTable.clearSelection();

            AtomicReference<String> toastMessage = new AtomicReference<>();
            AtomicReference<FeedbackType> toastType = new AtomicReference<>();
            ToastManager.setTestListener((type, msg) -> {
                toastType.set(type);
                toastMessage.set(msg);
            });

            try {
                // 1. Desconciliar sem selecção
                panel.unmatchSelectedItem();
                assertNotNull(toastMessage.get());
                assertEquals(FeedbackType.WARNING, toastType.get());
                assertTrue(toastMessage.get().contains("Seleccione um movimento bancário reconciliado"));

                // 2. Reconciliar manualmente sem selecção
                toastMessage.set(null);
                panel.openManualMatchDialog();
                assertNotNull(toastMessage.get());
                assertEquals(FeedbackType.WARNING, toastType.get());
                assertTrue(toastMessage.get().contains("Seleccione um item do extracto pendente"));

                // 3. Despesa directa sem selecção
                toastMessage.set(null);
                panel.openExpenseDialog();
                assertNotNull(toastMessage.get());
                assertEquals(FeedbackType.WARNING, toastType.get());
                assertTrue(toastMessage.get().contains("Seleccione a linha do extracto"));
            } finally {
                ToastManager.setTestListener(null);
            }
        });
    }

    @Test
    @DisplayName("ALERT-07: PhysicalInventoryPanel avisa operador quando tenta bipar sem sessão aberta")
    void testPhysicalInventoryWarnsWhenNoSession() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PhysicalInventoryPanel panel = new PhysicalInventoryPanel(null);

            AtomicReference<String> toastMessage = new AtomicReference<>();
            AtomicReference<FeedbackType> toastType = new AtomicReference<>();
            ToastManager.setTestListener((type, msg) -> {
                toastType.set(type);
                toastMessage.set(msg);
            });

            try {
                panel.handleBarcodeScan();
                assertNotNull(toastMessage.get(), "Deve emitir Toast ao tentar bipar sem sessão de inventário");
                assertEquals(FeedbackType.WARNING, toastType.get(), "Toast deve ser do tipo WARNING");
                assertTrue(toastMessage.get().contains("sessão de inventário"),
                        "Mensagem deve alertar sobre falta de sessão: " + toastMessage.get());
            } finally {
                ToastManager.setTestListener(null);
            }
        });
    }
}
