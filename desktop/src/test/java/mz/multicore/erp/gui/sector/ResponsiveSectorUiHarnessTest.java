package mz.multicore.erp.gui.sector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Sector screens fit a laptop as well as a wide monitor: no horizontal scroll, wrapping buttons and text. */
class ResponsiveSectorUiHarnessTest {

    @Test
    @DisplayName("RS-01: The card grid fits as many columns as the width allows, never fewer than one")
    void columnsFollowTheWidth() {
        assertThat(ResponsiveSupport.columnsFor(1200, 290, 12)).isEqualTo(4);
        assertThat(ResponsiveSupport.columnsFor(1000, 290, 12)).isEqualTo(3);
        assertThat(ResponsiveSupport.columnsFor(700, 290, 12)).isEqualTo(2);
        assertThat(ResponsiveSupport.columnsFor(250, 290, 12)).isEqualTo(1);
        assertThat(ResponsiveSupport.columnsFor(0, 290, 12)).isEqualTo(1);
    }

    @Test
    @DisplayName("RS-02: Scrolling content takes the viewport width, so nothing is cut on the left or right")
    void contentTracksViewportWidth() {
        JPanel content = ResponsiveSupport.widthTracking(new BorderLayout());
        assertThat(content).isInstanceOf(Scrollable.class);
        assertThat(((Scrollable) content).getScrollableTracksViewportWidth()).isTrue();
        assertThat(ResponsiveSupport.verticalScroll(content).getHorizontalScrollBarPolicy())
                .isEqualTo(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    }

    @Test
    @DisplayName("RS-03: Action buttons move to a second line when the card is narrow")
    void buttonsWrap() {
        JPanel bar = new JPanel(new WrapLayout(FlowLayout.RIGHT, 6, 0));
        for (int i = 0; i < 6; i++) {
            JButton button = new JButton("Acção " + i);
            button.setPreferredSize(new Dimension(120, 30));
            bar.add(button);
        }
        bar.setSize(new Dimension(1000, 40));
        int oneRow = bar.getPreferredSize().height;
        bar.setSize(new Dimension(300, 40));
        assertThat(bar.getPreferredSize().height).isGreaterThan(oneRow);
    }

    @Test
    @DisplayName("RS-04: Descriptions wrap by words and are read-only")
    void textWraps() {
        JTextArea text = ResponsiveSupport.wrappingText("Venda em caixa, fardo e palete a revendedores, com crédito.",
                new Font(Font.SANS_SERIF, Font.PLAIN, 12), Color.GRAY);
        assertThat(text.getLineWrap()).isTrue();
        assertThat(text.getWrapStyleWord()).isTrue();
        assertThat(text.isEditable()).isFalse();
    }

    @Test
    @DisplayName("RS-05: Sector screens use the responsive card header and adaptive list/detail split")
    void sectorScreensUseResponsiveBlocks() throws Exception {
        Path dir = Path.of("src/main/java/mz/multicore/erp/gui/sector");
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertThat(source).as(file.getFileName().toString()).doesNotContain("UIHelper.tableCardTop(");
                if (source.contains("new JSplitPane(")) {
                    assertThat(source).as(file.getFileName() + " adapts its split").contains("ResponsiveSupport.adaptiveSplit(");
                }
            }
        }
    }
}
