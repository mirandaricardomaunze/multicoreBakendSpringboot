package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

import static org.assertj.core.api.Assertions.assertThat;

class TablePagerSpacingTest {

    @Test
    void serverPagerSeparatesControlsAndActionsBelow() {
        TablePager pager = new TablePager((page, size) -> { });
        assertThat(pager.getLayout()).isInstanceOf(BorderLayout.class);
        BorderLayout layout = (BorderLayout) pager.getLayout();
        assertThat(layout.getLayoutComponent(BorderLayout.WEST)).isNotNull();
        assertThat(layout.getLayoutComponent(BorderLayout.CENTER)).isNotNull();
        assertThat(layout.getLayoutComponent(BorderLayout.EAST)).isNotNull();
        CompoundBorder border = (CompoundBorder) pager.getBorder();
        EmptyBorder spacing = (EmptyBorder) border.getInsideBorder();
        assertThat(spacing.getBorderInsets(pager).bottom).isEqualTo(TablePager.ACTION_ROW_GAP);
    }
}
