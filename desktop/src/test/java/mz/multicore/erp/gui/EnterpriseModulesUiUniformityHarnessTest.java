package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação visual e padrões de UI Swing para os módulos de expansão:
 * Garante que qualquer novo painel de Activos, Produção, Frotas ou Contratos segue
 * à risca o Design System corporativo e as regras não negociáveis do AGENTS.md.
 */
class EnterpriseModulesUiUniformityHarnessTest {

    private static final Path ROOT = Files.isDirectory(Path.of("desktop"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();

    @Test
    @DisplayName("Padrão canónico de KPI Card deve respeitar altura uniforme de 96px")
    void kpiCardsMustRespectStandardCardHeight() {
        assertThat(KpiCard.STANDARD_CARD_HEIGHT)
                .as("A altura padrão dos cartões de KPI deve ser estritamente 96px em todo o ERP")
                .isEqualTo(96);

        ModernPanel card = KpiCard.createCard("Activos Totais", "1.250.000 MT", "45 itens", "fas-boxes", UIHelper.ACCENT_BLUE);
        assertThat(card.getPreferredSize().height).isEqualTo(96);
    }

    @Test
    @DisplayName("Cabeçalho canónico tableCardTop mantém título e acções na primeira fila")
    void tableCardTopMustPlaceTitleInCenter() {
        JPanel filters = new JPanel();
        JButton action = new JButton("Actualizar");

        JPanel cardTop = UIHelper.tableCardTop("Activos Fixos Patrimoniais", filters, action);
        BorderLayout layout = (BorderLayout) cardTop.getLayout();
        Container header = (Container) layout.getLayoutComponent(BorderLayout.NORTH);

        assertThat(header).isNotNull();
        assertThat(((BorderLayout) header.getLayout()).getLayoutComponent(BorderLayout.CENTER))
                .as("O título deve ocupar o CENTER para proteção estrutural contra colisão por botões")
                .isInstanceOf(JLabel.class);

        assertThat(((BorderLayout) header.getLayout()).getLayoutComponent(BorderLayout.EAST))
                .as("As acções devem ocupar a área direita da primeira fila")
                .isInstanceOf(JPanel.class);
        assertThat(layout.getLayoutComponent(BorderLayout.CENTER)).isSameAs(filters);
    }

    @Test
    @DisplayName("Código-fonte dos módulos deve respeitar convenções pt-MZ e proibir cores soltas ou emojis")
    void uiPanelsMustFollowConventionsAndForbidUnicodeEmojis() throws IOException {
        Path guiDir = ROOT.resolve(Path.of("desktop", "src", "main", "java", "mz", "multicore", "erp", "gui"));
        assertThat(guiDir).isDirectory();

        try (var stream = Files.walk(guiDir)) {
            List<Path> files = stream.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.toString().contains("components"))
                    .filter(p -> !p.getFileName().toString().equals("LoginDialog.java"))
                    .toList();

            for (Path file : files) {
                String source = Files.readString(file);

                // Proíbe termos proibidos pelo FinalUiUniformityHarnessTest
                assertThat(source.toLowerCase())
                        .as("Ficheiro %s não deve conter cores locais ou termos em pt-BR", file.getFileName())
                        .doesNotContain("new color(", "atualizar", "cadastrar");

                // Proíbe emojis Unicode que renderizam como retângulos/quadrinhos no Windows
                assertThat(source)
                        .as("Ficheiro %s não deve conter emojis Unicode — usar ícones FontAwesome", file.getFileName())
                        .doesNotMatch("(?s).*[⭐✅❌🚗🏭🚛🏢].*");
            }
        }
    }
}
