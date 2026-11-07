package mz.multicore.erp.gui.tv;

import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Os modais do Painel Executivo têm o tema do painel, também depois de trocar de tema.
 * Ver docs/SALA_DE_REUNIOES_TV_SPEC.md (ET-01 a ET-07).
 */
class ExecutiveThemeConsistencyHarnessTest {

    private static final Path TV = Path.of("src/main/java/mz/multicore/erp/gui/tv");
    private static final List<String> MODALS = List.of(
            "ExecutiveTvDialog.java", "ExecutiveTvGoalsDialog.java", "SellerPerformanceDialog.java", "ExecutiveTvFormControls.java");
    /** Tokens e fábricas que seguem o tema da APLICAÇÃO: dentro de um modal do painel ficavam brancos no escuro. */
    private static final List<String> APP_THEME = List.of("UIHelper.BG_", "UIHelper.TEXT_", "UIHelper.GRID",
            "UIHelper.FIELD_BG", "UIHelper.BORDER", "UIHelper.styleTable", "UIHelper.styleTextField",
            "createSecondaryButton", "ModernFormDialog", "ModernPanel");

    private static String source(String file) throws IOException {
        return Files.readString(TV.resolve(file));
    }

    /** Só o código: os comentários podem explicar o que se deixou de usar sem fazer cair o teste. */
    private static String code(String file) throws IOException {
        return source(file).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    @Test
    @DisplayName("ET-01: os modais do painel não usam o tema da aplicação")
    void modalsUseOnlyPanelTheme() throws IOException {
        for (String file : MODALS) {
            String code = code(file);
            for (String token : APP_THEME) {
                assertFalse(code.contains(token), file + " usa " + token + ": use ExecutiveTvTheme");
            }
        }
        for (String file : List.of("ExecutiveTvGoalsDialog.java", "SellerPerformanceDialog.java")) {
            assertFalse(code(file).contains("new Color("), file + " tem cores à mão: use ExecutiveTvTheme");
        }
    }

    @Test
    @DisplayName("ET-02: Metas e Vendedores abrem na mesma moldura")
    void bothModalsUseExecutiveFrame() throws IOException {
        assertTrue(source("ExecutiveTvGoalsDialog.java").contains("new ExecutiveTvDialog("));
        assertTrue(source("SellerPerformanceDialog.java").contains("new ExecutiveTvDialog("));
        assertTrue(source("BusinessUnitGoalManager.java").contains("ExecutiveTvGoalsDialog.show("));
    }

    @Test
    @DisplayName("ET-03: os modais recebem o tema actual do painel, e não um fixo")
    void dashboardPassesCurrentThemeToModals() throws IOException {
        String dashboard = source("ExecutiveTvDashboardDialog.java");
        assertTrue(dashboard.contains("showGoalsDialog(this, businessUnits, this::renderCurrentView, lightMode)"));
        assertTrue(dashboard.contains("SellerPerformanceDialog.showDialog(this, filtered, currentPeriod.getLabel(), lightMode)"));
        assertTrue(dashboard.contains("onToggleTheme() { applyPanelTheme(!lightMode); }"));
    }

    @Test
    @DisplayName("ET-04: o painel abre no tema da aplicação e acompanha a troca sem fechar")
    void dashboardFollowsAppThemeAndSurvivesRebuild() throws IOException {
        String dashboard = source("ExecutiveTvDashboardDialog.java");
        assertTrue(dashboard.contains("lightMode = UIHelper.isLight()"), "abre no tema da aplicação");
        assertTrue(dashboard.contains("super((Window) null"), "não é filho da janela principal, que a troca de tema descarta");
        assertTrue(dashboard.contains("ThemeChangeListeners.add(appThemeListener)"));
        assertTrue(dashboard.contains("ThemeChangeListeners.remove(appThemeListener)"));
        String theme = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/components/UiThemeAndWindowSupport.java"));
        assertTrue(theme.contains("ThemeChangeListeners.fire(theme)"), "setTheme avisa quem não é reconstruído");
    }

    @Test
    @DisplayName("ET-05: fechar pára os temporizadores; o ecrã inteiro não")
    void closingStopsTimersButFullScreenToggleDoesNot() throws IOException {
        String dashboard = source("ExecutiveTvDashboardDialog.java");
        int dispose = dashboard.indexOf("public void dispose()");
        assertTrue(dispose > 0 && dashboard.indexOf("timerCoordinator.stop()", dispose) > dispose);
        int fullScreen = dashboard.indexOf("public void onToggleFullScreen()");
        int end = dashboard.indexOf("setVisible(true)", fullScreen);
        String toggle = dashboard.substring(fullScreen, end);
        assertTrue(toggle.contains("super.dispose()"), "trocar a moldura não pode parar o rodízio");
    }

    @Test
    @DisplayName("ET-07: a moldura dos modais pinta-se com o fundo do painel em cada tema")
    void modalFrameUsesPanelBackgroundInBothThemes() {
        for (boolean light : new boolean[]{true, false}) {
            ExecutiveTvDialog frame = new ExecutiveTvDialog(null, "Metas", "fas-bullseye", null, light);
            try {
                assertEquals(ExecutiveTvTheme.getBgColor(light), frame.window().getContentPane().getBackground());
                assertEquals(light, frame.lightMode());
            } finally {
                frame.window().dispose();
            }
        }
    }

    @Test
    @DisplayName("ET-06: ranking da força de vendas por faturação, com quota")
    void sellerRankingOrdersByRevenue() {
        List<SellerPerformanceDialog.SellerStats> stats = SellerPerformanceDialog.rank(List.of(
                invoice("ana", "300"), invoice("joao", "100"), invoice("ana", "100"), invoice(" ", "0")));
        assertEquals("ana", stats.get(0).sellerName());
        assertEquals(0, new BigDecimal("400").compareTo(stats.get(0).totalBilled()));
        assertEquals(2, stats.get(0).invoiceCount());
        assertEquals(80.0, stats.get(0).sharePct());
        assertEquals("Balcão / Geral", stats.get(2).sellerName());
    }

    private static InvoiceDTO invoice(String createdBy, String total) {
        InvoiceDTO dto = mock(InvoiceDTO.class);
        when(dto.createdBy()).thenReturn(createdBy);
        when(dto.totalAmount()).thenReturn(new BigDecimal(total));
        return dto;
    }
}
