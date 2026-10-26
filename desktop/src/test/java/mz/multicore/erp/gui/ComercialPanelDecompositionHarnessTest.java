package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.CreditNoteApiClient;
import mz.multicore.erp.desktop.client.DebitNoteApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.FleetApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.MovimentosApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import mz.multicore.erp.desktop.client.PromotionApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test Harness da SPEC-COMERCIAL-PANEL-DECOMPOSITION-001.
 * Valida a integridade arquitetural e limites de linhas da decomposição do ComercialPanel.
 */
class ComercialPanelDecompositionHarnessTest {

    private static final Path GUI_ROOT = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    private Path resolveGuiPath(String filename) {
        Path direct = GUI_ROOT.resolve(filename);
        if (Files.exists(direct)) return direct;
        Path nested = Path.of("desktop").resolve(GUI_ROOT).resolve(filename);
        if (Files.exists(nested)) return nested;
        return Path.of("..", "desktop").resolve(GUI_ROOT).resolve(filename);
    }

    @Test
    @DisplayName("CPD-01: ComercialPanel e colaboradores respeitam os limites de linhas do SPEC")
    void testFileLimits() throws IOException {
        Path panelPath = resolveGuiPath("ComercialPanel.java");
        Path metadataPath = resolveGuiPath("CommercialMetadataCoordinator.java");
        Path invoicePath = resolveGuiPath("CommercialInvoiceOperations.java");
        Path orderPath = resolveGuiPath("CommercialOrderOperations.java");

        assertThat(panelPath).exists();
        assertThat(metadataPath).exists();
        assertThat(invoicePath).exists();
        assertThat(orderPath).exists();

        int panelLines = Files.readAllLines(panelPath).size();
        int metadataLines = Files.readAllLines(metadataPath).size();
        int invoiceLines = Files.readAllLines(invoicePath).size();
        int orderLines = Files.readAllLines(orderPath).size();

        assertThat(panelLines)
                .as("ComercialPanel deve ter no máximo 450 linhas (atual: %d)", panelLines)
                .isLessThanOrEqualTo(450);

        assertThat(metadataLines)
                .as("CommercialMetadataCoordinator deve ter no máximo 150 linhas (atual: %d)", metadataLines)
                .isLessThanOrEqualTo(150);

        assertThat(invoiceLines)
                .as("CommercialInvoiceOperations deve ter no máximo 250 linhas (atual: %d)", invoiceLines)
                .isLessThanOrEqualTo(250);

        assertThat(orderLines)
                .as("CommercialOrderOperations deve ter no máximo 250 linhas (atual: %d)", orderLines)
                .isLessThanOrEqualTo(250);
    }

    @Test
    @DisplayName("CPD-02: Eliminação de diálogos legados JOptionPane em todo o módulo comercial")
    void testZeroJOptionPane() throws IOException {
        String[] files = {
                "ComercialPanel.java",
                "CommercialMetadataCoordinator.java",
                "CommercialInvoiceOperations.java",
                "CommercialOrderOperations.java"
        };

        for (String file : files) {
            Path path = resolveGuiPath(file);
            String content = Files.readString(path);
            assertThat(content)
                    .as("%s não deve conter chamadas JOptionPane", file)
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("CPD-03: Preservação de contratos essenciais e métodos exigidos por harnesses vizinhos")
    void testRequiredContractsPreserved() throws IOException {
        String panelContent = Files.readString(resolveGuiPath("ComercialPanel.java"));

        assertThat(panelContent)
                .as("Deve conter métodos de conversão de documentos para DocumentConversionWorkflowHarnessTest")
                .contains("convertSelectedOrderToGuide")
                .contains("convertSelectedOrderToTransfer")
                .contains("billSelectedOrder");

        assertThat(panelContent)
                .as("Deve conter métodos de editor para OrderEditorHarnessTest")
                .contains("void openOrderEditor()")
                .contains("void openSelectedOrderEditor()")
                .contains("void addDraftOrderLine()")
                .contains("void removeSelectedDraftOrderLine()");

        assertThat(panelContent)
                .as("Deve conter paragem de edição de célula para PhcDocumentEditorHomologationHarnessTest")
                .contains("stopInvoiceCellEditing()");

        assertThat(panelContent)
                .as("Deve conter guarda de perfil de vendedor para SellerExperienceHarnessTest")
                .contains("boolean sellerView")
                .contains("if (\"SELLER\".equalsIgnoreCase(CurrentUserContext.getRole())) return;");
    }

    @Test
    @DisplayName("CPD-04: Três construtores públicos retrocompatíveis preservados no ComercialPanel")
    void testConstructorCompatibility() {
        Constructor<?>[] constructors = ComercialPanel.class.getConstructors();
        assertThat(constructors.length)
                .as("ComercialPanel deve preservar os 3 construtores sobrecarregados para retrocompatibilidade")
                .isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("CPD-05: Instanciação funcional sem falhas do ComercialPanel com colaboradores")
    void testInstantiation() {
        ComercialApiClient comercialApiClient = mock(ComercialApiClient.class);
        InventoryApiClient inventoryApiClient = mock(InventoryApiClient.class);
        FinanceApiClient financeApiClient = mock(FinanceApiClient.class);
        CreditNoteApiClient creditNoteApiClient = mock(CreditNoteApiClient.class);
        DebitNoteApiClient debitNoteApiClient = mock(DebitNoteApiClient.class);
        POSApiClient posApiClient = mock(POSApiClient.class);
        MovimentosApiClient movimentosApiClient = mock(MovimentosApiClient.class);
        PromotionApiClient promotionApiClient = mock(PromotionApiClient.class);
        PrintApiClient printApiClient = mock(PrintApiClient.class);
        ProductionApiClient productionApiClient = mock(ProductionApiClient.class);
        FleetApiClient fleetApiClient = mock(FleetApiClient.class);

        ComercialPanel panel = new ComercialPanel(
                comercialApiClient, inventoryApiClient, financeApiClient, creditNoteApiClient, debitNoteApiClient,
                posApiClient, movimentosApiClient, promotionApiClient, printApiClient,
                productionApiClient, fleetApiClient, () -> {}
        );

        assertThat(panel).isNotNull();
        assertThat(panel.getInventoryApiClient()).isSameAs(inventoryApiClient);
        assertThat(panel.getFinanceApiClient()).isSameAs(financeApiClient);
        assertThat(panel.getReceiptsPanel()).isNotNull();
        assertThat(panel.getBillOrderDialog()).isNotNull();
        assertThat(panel.getCancelOrderDialog()).isNotNull();
    }
}
