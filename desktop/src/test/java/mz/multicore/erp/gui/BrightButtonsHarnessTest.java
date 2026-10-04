package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BrightButtonsHarnessTest {

    @Test
    void secondaryAndNeutralButtonColorsAreVibrantNotDark() {
        Color darkGray600 = new Color(75, 85, 99);
        Color darkSlate700 = new Color(51, 65, 85);

        // SECONDARY deve ser luminoso (Sky-700 #0369A1), nunca cinzento escuro
        assertThat(UIHelper.SECONDARY).isNotEqualTo(darkGray600);
        assertThat(UIHelper.SECONDARY).isEqualTo(new Color(3, 105, 161));

        // BUTTON_NEUTRAL deve ser vivo (Indigo-500 #6366F1), nunca ardósia escura
        assertThat(UIHelper.BUTTON_NEUTRAL).isNotEqualTo(darkSlate700);
        assertThat(UIHelper.BUTTON_NEUTRAL).isEqualTo(new Color(99, 102, 241));
    }

    @Test
    void createSecondaryButtonInstantiatesVibrantButton() {
        ModernButton btn = UIHelper.createSecondaryButton("Exportar");
        assertThat(btn.getBackground()).isEqualTo(UIHelper.SECONDARY);
        assertThat(btn.getForeground()).isEqualTo(Color.WHITE);
        assertThat(btn.getText()).isEqualTo("Exportar");
    }

    @Test
    void createRefreshButtonInstantiatesBrightActionButton() {
        ModernButton btn = UIHelper.createRefreshButton(() -> {});
        assertThat(btn.getBackground()).isEqualTo(UIHelper.SECONDARY);
        assertThat(btn.getForeground()).isEqualTo(Color.WHITE);
        assertThat(btn.getIcon()).isNotNull();
        assertThat(btn.getText()).isEqualTo("Actualizar");
    }

    @Test
    void posClassesHaveZeroDarkButtons() throws IOException {
        Path gui = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");
        List<String> posFiles = List.of(
                "POSPanel.java",
                "PosCatalogController.java",
                "PosShortcutBar.java",
                "PosPaymentDialog.java",
                "PosSalesHistoryPanel.java",
                "PosImportQuotationDialog.java",
                "PosReturnDialog.java"
        );

        for (String file : posFiles) {
            Path path = gui.resolve(file);
            if (Files.exists(path)) {
                String content = Files.readString(path);
                assertThat(content)
                        .as("Arquivo %s não deve conter BUTTON_NEUTRAL", file)
                        .doesNotContain("BUTTON_NEUTRAL");
                assertThat(content)
                        .as("Arquivo %s não deve conter createSecondaryButton", file)
                        .doesNotContain("createSecondaryButton");
            }
        }
    }

    @Test
    void keyDialogsDoNotUseSecondaryButtonForCancelOrClose() throws IOException {
        Path gui = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");
        List<String> dialogFiles = List.of(
                "SubscriptionRenewalDialog.java",
                "MobilePaymentModal.java",
                "SystemMonitoringDialog.java",
                "LicenseAcceptanceDialog.java",
                "PosImportQuotationDialog.java"
        );

        for (String file : dialogFiles) {
            Path path = gui.resolve(file);
            if (Files.exists(path)) {
                String content = Files.readString(path);
                assertThat(content)
                        .as("Arquivo %s não deve conter createSecondaryButton(\"Cancelar\")", file)
                        .doesNotContain("createSecondaryButton(\"Cancelar\")");
                assertThat(content)
                        .as("Arquivo %s não deve conter createSecondaryButton(\"Fechar\")", file)
                        .doesNotContain("createSecondaryButton(\"Fechar\")");
            }
        }
    }
}
