package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness of the screens that were off the shared rules (docs/ECRAS_FORA_DA_REGRA_SPEC.md). */
class OffRuleScreensHarnessTest {

    private static final Path GUI = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("FR-01: tema e densidade são escolhas visíveis num seletor segmentado, sem botões cheios coloridos")
    void fr01_appearanceIsSegmented() throws IOException {
        String config = Files.readString(GUI.resolve("ConfigPanel.java"));
        assertThat(config).contains("SegmentedControl.of(\"Tema\"").contains("SegmentedControl.of(\"Densidade\"")
                .doesNotContain("new ModernButton(\"Diagnóstico").doesNotContain("Clique p/");
    }

    @Test
    @DisplayName("FR-02: o seletor segmentado escolhe uma opção e chama a acção com o índice")
    void fr02_segmentedControlSelects() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger chosen = new AtomicInteger(-1);
            SegmentedControl control = SegmentedControl.of("Tema", new String[]{"Claro", "Escuro", "Alto contraste"}, 0, chosen::set);
            assertThat(control.selectedIndex()).isZero();
            control.options().get(2).doClick();
            assertThat(control.selectedIndex()).isEqualTo(2);
            assertThat(chosen.get()).isEqualTo(2);
        });
    }

    @Test
    @DisplayName("FR-03: Financeiro › Contas e Fluxo de Caixa têm \"Actualizar\" (RH e CRM ficam no limite de 3 botões)")
    void fr03_listingsHaveRefresh() throws IOException {
        assertThat(count(Files.readString(GUI.resolve("FinanceiroPanel.java")))).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("FR-04: a seta das listas fica sobre a cor do campo em todos os temas (sem quadrado cinzento)")
    void fr04_comboArrowOnFieldColour() throws IOException {
        assertThat(Files.readString(GUI.resolve("components/UiThemeAndWindowSupport.java")))
                .contains("\"ComboBox.buttonBackground\", \"ComboBox.buttonEditableBackground\"");
    }

    @Test
    @DisplayName("FR-05: Fecho Cego sem inglês e com o \"Cancelar\" comum")
    void fr05_blindClose() throws IOException {
        String dialog = Files.readString(GUI.resolve("pos/PosBlindCloseDialog.java"));
        assertThat(dialog).doesNotContain("Blind Drop").doesNotContain("Cancelar / Voltar").contains("ButtonStyles.cancelButton()");
    }

    private static int count(String src) {
        return src.split("createRefreshButton\\(", -1).length - 1;
    }
}
