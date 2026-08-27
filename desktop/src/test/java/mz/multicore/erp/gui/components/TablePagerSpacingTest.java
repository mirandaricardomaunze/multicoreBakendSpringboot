package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.FlowLayout;

import static org.assertj.core.api.Assertions.assertThat;

class TablePagerSpacingTest {

    @Test
    void serverPagerSeparatesControlsAndActionsBelow() {
        TablePager pager = new TablePager((page, size) -> { });
        assertThat(((FlowLayout) pager.getLayout()).getHgap()).isEqualTo(TablePager.CONTROL_GAP);
        CompoundBorder border = (CompoundBorder) pager.getBorder();
        EmptyBorder spacing = (EmptyBorder) border.getInsideBorder();
        assertThat(spacing.getBorderInsets(pager).bottom).isEqualTo(TablePager.ACTION_ROW_GAP);
    }
}
