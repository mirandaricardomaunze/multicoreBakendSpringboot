package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver docs/COLLAPSIBLE_SIDEBAR_DECOMPOSITION_SPEC.md. */
class SidebarScrollHintTest {

    @Test
    @DisplayName("A seta só aparece enquanto houver módulos abaixo")
    void hintFollowsScrollPosition() {
        assertThat(SidebarScrollHint.hasMoreBelow(0, 500, 900)).isTrue();
        assertThat(SidebarScrollHint.hasMoreBelow(400, 500, 900)).isFalse();
        assertThat(SidebarScrollHint.hasMoreBelow(0, 900, 900)).isFalse();
    }

    @Test
    @DisplayName("O nome da empresa passa para duas linhas sem interpretar HTML")
    void companyNameIsEscaped() {
        assertThat(SidebarBrandText.wrapped("A & B <Lda>"))
                .contains("A &amp; B &lt;Lda&gt;").doesNotContain("<Lda>");
    }
}
