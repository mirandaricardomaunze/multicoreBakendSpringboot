package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.gui.components.SidebarNavItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Ver docs/COLLAPSIBLE_SIDEBAR_DECOMPOSITION_SPEC.md. */
class SidebarActiveCompanyHarnessTest {

    private static final DesktopSession.CompanyAccess MOZAMBIQUE =
            new DesktopSession.CompanyAccess(1L, "Multicore Moçambique Lda", "SELLER");
    private static final DesktopSession.CompanyAccess PORTUGAL =
            new DesktopSession.CompanyAccess(2L, "Multicore Portugal Lda", "ADMIN");

    @Test
    @DisplayName("A sidebar mostra a empresa activa, não a primeira da lista")
    void labelFollowsActiveCompany() {
        DesktopSession session = new DesktopSession("t", Instant.now().plusSeconds(60), "ana", "Ana",
                false, List.of(MOZAMBIQUE, PORTUGAL));

        session.selectCompany(PORTUGAL.id());

        assertThat(MainFrameSidebarBuilder.companyLabel(session)).isEqualTo("Multicore Portugal Lda");
        assertThat(session.activeRole()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Sem empresa, a sidebar mostra o nome do produto")
    void labelWithoutCompany() {
        DesktopSession session = new DesktopSession("t", Instant.now().plusSeconds(60), "root", "Root", true, List.of());

        assertThat(MainFrameSidebarBuilder.companyLabel(session)).isEqualTo("ERP Profissional");
    }

    @Test
    @DisplayName("A empresa guardada é escolhida antes de construir a sidebar")
    void companyIsChosenBeforeSidebar() throws IOException {
        String source = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/MainFrame.java"));

        assertThat(source.indexOf("companyCombo = buildCompanyCombo()"))
                .isPositive().isLessThan(source.indexOf("sidebar = buildSidebar()"));
    }

    @Test
    @DisplayName("A Tesouraria fica junto da área financeira")
    void treasuryLivesInFinance() throws IOException {
        String source = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/MainFrameSidebarBuilder.java"));

        assertThat(source.indexOf("\"Tesouraria\"")).isGreaterThan(source.indexOf("addSection(\"Finanças & Fiscal\")"));
        assertThat(source).doesNotContain("Fiscal & Auditoria");
    }

    @Test
    @DisplayName("Cada item mede 36px")
    void itemHeight() {
        assertThat(SidebarNavItem.ITEM_HEIGHT).isEqualTo(36);
    }
}
