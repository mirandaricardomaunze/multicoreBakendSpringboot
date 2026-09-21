package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalFeedbackHarnessTest {

    @Test
    void inlineFeedback_errorWithRetry_isContextualAndActionable() throws Exception {
        InlineFeedbackPanel panel = new InlineFeedbackPanel();
        AtomicBoolean retried = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> panel.show(FeedbackType.ERROR, "Falha de ligação",
                "Não foi possível carregar os dados.", "Tentar novamente", () -> retried.set(true)));

        assertThat(panel.isVisible()).isTrue();
        assertThat(panel.feedbackType()).isEqualTo(FeedbackType.ERROR);
        assertThat(panel.messageText()).contains("Não foi possível carregar os dados");
        assertThat(panel.hasAction()).isTrue();
        findButton(panel, "Tentar novamente").doClick();
        assertThat(retried).isTrue();
    }

    @Test
    void inlineFeedback_recalculatesSemanticColoursWhenThemeChanges() throws Exception {
        InlineFeedbackPanel panel = new InlineFeedbackPanel();
        try {
            UIHelper.applyTheme(Theme.DARK);
            SwingUtilities.invokeAndWait(() -> panel.show(FeedbackType.SUCCESS,
                    "Ligação restabelecida", "As aprovações foram actualizadas.", null, null));
            Color darkBackground = panel.getBackground();

            UIHelper.applyTheme(Theme.LIGHT);
            SwingUtilities.invokeAndWait(panel::updateUI);

            assertThat(panel.getBackground()).isNotEqualTo(darkBackground);
            assertThat(findLabel(panel, "As aprovações foram actualizadas.").getForeground())
                    .isEqualTo(UIHelper.TEXT_LIGHT);
        } finally {
            UIHelper.applyTheme(Theme.DARK);
        }
    }

    @Test
    void toastContent_hasSemanticIconMessageAndAccessibleName() {
        JPanel content = ToastManager.buildContent(FeedbackType.SUCCESS, "Fatura emitida.");
        assertThat(content.getAccessibleContext().getAccessibleName())
                .isEqualTo("Sucesso: Fatura emitida.");
        assertThat(findLabel(content, "Fatura emitida.")).isNotNull();
    }

    @Test
    void modernMessageBody_usesThemeComponentAndAccessibleDescription() {
        JPanel body = ModernMessageDialog.buildMessageBody(FeedbackType.WARNING,
                "Esta operação exige confirmação.");
        assertThat(body).isInstanceOf(ModernPanel.class);
        assertThat(body.getAccessibleContext().getAccessibleName()).startsWith("Aviso:");
    }

    @Test
    void canonicalFormDialog_reportsSaveErrorsInline() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp",
                "gui", "components", "ModernFormDialog.java"));
        assertThat(source).contains("InlineFeedbackPanel", "showError(error)");
        assertThat(source).doesNotContain("JOptionPane.showMessageDialog(dialog");
    }

    @Test
    void approvalsReferenceFlow_adoptsToastBannerAndRetry() throws Exception {
        String source = Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp",
                "gui", "ApprovalsPanel.java"));
        assertThat(source).contains("ToastManager.success", "InlineFeedbackPanel",
                "\"Tentar novamente\", this::refreshData");
    }

    @Test
    void legacyOptionPanes_canOnlyDecreaseAndPriorityPanelsStayMigrated() throws Exception {
        Path gui = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");
        long total;
        try (Stream<Path> files = Files.walk(gui)) {
            total = files.filter(path -> path.toString().endsWith(".java"))
                    .mapToLong(ProfessionalFeedbackHarnessTest::optionPaneCalls)
                    .sum();
        }
        assertThat(total).as("O inventário legado de JOptionPane só pode diminuir").isLessThanOrEqualTo(49);
        assertThat(optionPaneCalls(gui.resolve("ComercialPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("POSPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("StockPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("ComprasPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("PurchaseOrdersPanel.java"))).isLessThanOrEqualTo(1);
        assertThat(optionPaneCalls(gui.resolve("StockTransferActions.java"))).isLessThanOrEqualTo(5);
        assertThat(optionPaneCalls(gui.resolve("StockProductActions.java"))).isLessThanOrEqualTo(2);
        assertThat(optionPaneCalls(gui.resolve("ConfigPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("PlataformaPanel.java"))).isLessThanOrEqualTo(2);
        assertThat(optionPaneCalls(gui.resolve("HRPanel.java"))).isLessThanOrEqualTo(4);
        assertThat(optionPaneCalls(gui.resolve("HREmployeeActions.java"))).isLessThanOrEqualTo(2);
        assertThat(optionPaneCalls(gui.resolve("CRMPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("CrmWorkSheetActions.java"))).isLessThanOrEqualTo(1);
        assertThat(optionPaneCalls(gui.resolve("CrmTicketActions.java"))).isLessThanOrEqualTo(2);
        assertThat(optionPaneCalls(gui.resolve("PosBarcodeActions.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("PosCashSessionActions.java"))).isLessThanOrEqualTo(2);
        assertThat(optionPaneCalls(gui.resolve("StockInventoryCountActions.java"))).isLessThanOrEqualTo(1);
        assertThat(optionPaneCalls(gui.resolve("commercial/CommercialNotesPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("commercial/ReceiptsPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("commercial/DeliveryGuidesPanel.java"))).isLessThanOrEqualTo(3);
        assertThat(optionPaneCalls(gui.resolve("commercial/QuotationsPanel.java"))).isLessThanOrEqualTo(4);
        assertThat(optionPaneCalls(gui.resolve("ClientesPanel.java"))).isLessThanOrEqualTo(1);
        assertThat(optionPaneCalls(gui.resolve("FiscalPanel.java"))).isLessThanOrEqualTo(2);
        assertThat(optionPaneCalls(gui.resolve("PosReturnDialog.java"))).isLessThanOrEqualTo(1);
        assertThat(optionPaneCalls(gui.resolve("StockWarehousesPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("PurchasePayablesPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("PromotionsPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("HRExpensesPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("HRVacationsPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("HRDeductionsPanel.java"))).isLessThanOrEqualTo(5);
        assertThat(optionPaneCalls(gui.resolve("HRTimeSheetPanel.java"))).isLessThanOrEqualTo(1);
        assertThat(optionPaneCalls(gui.resolve("HRTerminationsPanel.java"))).isLessThanOrEqualTo(2);
        assertThat(optionPaneCalls(gui.resolve("HRContractsPanel.java"))).isLessThanOrEqualTo(1);
        assertThat(optionPaneCalls(gui.resolve("HRLiabilitiesPanel.java"))).isLessThanOrEqualTo(1);
        assertThat(optionPaneCalls(gui.resolve("HRPayrollActions.java"))).isLessThanOrEqualTo(3);
        assertThat(optionPaneCalls(gui.resolve("PosCatalogController.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("OrderToTransferAction.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("CommercialOrderSubmission.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("PosSalesHistoryPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("PurchaseSuppliersPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("StockCategoriesPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("StockBatchesPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("FinanceiroPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("NotificationsPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("CustomerOrderFulfillmentActions.java"))).isLessThanOrEqualTo(2);
        assertThat(optionPaneCalls(gui.resolve("commercial/QuotationEditorDialog.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("commercial/CancelOrderDialog.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("commercial/OutstandingAccountsPanel.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("commercial/BillOrderDialog.java"))).isLessThanOrEqualTo(1);
        assertThat(optionPaneCalls(gui.resolve("components/PrintPreviewDialog.java"))).isZero();
        assertThat(optionPaneCalls(gui.resolve("components/TableExportAction.java"))).isZero();
    }

    @Test
    void remainingLegacyDialogs_areRestrictedToTheReviewedAllowlist() throws Exception {
        Path gui = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");
        Set<String> allowed = Set.of(
                "ClientesPanel.java", "ConfigSupportPanel.java", "CrmTicketActions.java",
                "CustomerOrderFulfillmentActions.java", "FiscalPanel.java", "HRContractsPanel.java",
                "HRDeductionsPanel.java", "HREmployeeActions.java", "HRLiabilitiesPanel.java",
                "HRPanel.java", "HRPayrollActions.java", "HRTerminationsPanel.java", "HRTimeSheetPanel.java",
                "LicenseAcceptanceDialog.java", "MainFrame.java", "PlataformaPanel.java",
                "PosCashSessionActions.java", "PosReturnDialog.java", "PurchaseOrdersPanel.java",
                "StockInventoryCountActions.java", "StockProductActions.java", "StockTransferActions.java",
                "commercial/BillOrderDialog.java", "commercial/DeliveryGuidesPanel.java",
                "commercial/OrderDetailsDialog.java", "commercial/QuotationsPanel.java",
                "components/DocumentEditorHost.java");
        Set<String> actual;
        try (Stream<Path> files = Files.walk(gui)) {
            actual = files.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> optionPaneCalls(path) > 0)
                    .map(gui::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .collect(Collectors.toSet());
        }
        assertThat(actual).isEqualTo(allowed);
    }

    @Test
    void stockAndPurchases_formValidationRemainsInsideTheActiveForm() throws Exception {
        Path gui = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");
        String stock = Files.readString(gui.resolve("StockPanel.java"));
        String purchases = Files.readString(gui.resolve("ComprasPanel.java"));
        assertThat(stock).contains("dialog.setOnSaveAsync", "UIHelper.markFieldInvalid(countedField",
                "UIHelper.markFieldInvalid(reasonField");
        assertThat(purchases).contains("UIHelper.markFieldInvalid(quantityField",
                "UIHelper.markFieldInvalid(priceField", "UIHelper.markFieldInvalid(purchaseVatField");
    }

    private static ModernButton findButton(Component root, String text) {
        if (root instanceof ModernButton button && text.equals(button.getText())) return button;
        if (root instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                ModernButton found = findButton(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static JLabel findLabel(Component root, String text) {
        if (root instanceof JLabel label && label.getText() != null && label.getText().contains(text)) return label;
        if (root instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                JLabel found = findLabel(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static long optionPaneCalls(Path path) {
        try {
            String source = Files.readString(path);
            return java.util.regex.Pattern.compile(
                    "JOptionPane\\.(showMessageDialog|showConfirmDialog|showInputDialog)")
                    .matcher(source).results().count();
        } catch (Exception error) {
            throw new AssertionError("Não foi possível auditar " + path, error);
        }
    }
}
