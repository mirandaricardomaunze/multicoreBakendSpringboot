package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.FiscalApiClient;
import mz.multicore.erp.modules.fiscal.dto.IvaSummaryDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: Decomposição de FiscalPanel (Fase 11)")
class FiscalPanelDecompositionHarnessTest {

    private static Path resolveGui(String name) {
        Path direct = Path.of("desktop/src/main/java/mz/multicore/erp/gui", name);
        if (Files.exists(direct)) return direct;
        Path nested = Path.of("src/main/java/mz/multicore/erp/gui", name);
        if (Files.exists(nested)) return nested;
        return direct;
    }

    @Test
    @DisplayName("DECOMP-01: Limites físicos estritos de linhas de código")
    void decomp01_strictLineLimits() throws IOException {
        Path mainPath = resolveGui("FiscalPanel.java");
        Path ivaPath = resolveGui("FiscalIvaTab.java");
        Path taxRatesPath = resolveGui("FiscalTaxRatesTab.java");
        Path withPath = resolveGui("FiscalWithholdingsTab.java");
        Path payrollPath = resolveGui("FiscalPayrollTab.java");
        Path declPath = resolveGui("FiscalDeclarationsTab.java");

        assertTrue(Files.exists(mainPath), "FiscalPanel.java deve existir");
        assertTrue(Files.exists(ivaPath), "FiscalIvaTab.java deve existir");
        assertTrue(Files.exists(taxRatesPath), "FiscalTaxRatesTab.java deve existir");
        assertTrue(Files.exists(withPath), "FiscalWithholdingsTab.java deve existir");
        assertTrue(Files.exists(payrollPath), "FiscalPayrollTab.java deve existir");
        assertTrue(Files.exists(declPath), "FiscalDeclarationsTab.java deve existir");

        long mainLines = Files.lines(mainPath).count();
        long ivaLines = Files.lines(ivaPath).count();
        long taxRatesLines = Files.lines(taxRatesPath).count();
        long withLines = Files.lines(withPath).count();
        long payrollLines = Files.lines(payrollPath).count();
        long declLines = Files.lines(declPath).count();

        assertTrue(mainLines <= 160, "FiscalPanel.java excedeu 160 linhas (actual: " + mainLines + ")");
        assertTrue(ivaLines <= 260, "FiscalIvaTab.java excedeu 260 linhas (actual: " + ivaLines + ")");
        assertTrue(taxRatesLines <= 180, "FiscalTaxRatesTab.java excedeu 180 linhas (actual: " + taxRatesLines + ")");
        assertTrue(withLines <= 200, "FiscalWithholdingsTab.java excedeu 200 linhas (actual: " + withLines + ")");
        assertTrue(payrollLines <= 140, "FiscalPayrollTab.java excedeu 140 linhas (actual: " + payrollLines + ")");
        assertTrue(declLines <= 100, "FiscalDeclarationsTab.java excedeu 100 linhas (actual: " + declLines + ")");
    }

    @Test
    @DisplayName("DECOMP-02: Zero diálogos legados JOptionPane nos componentes fiscais")
    void decomp02_zeroLegacyDialogs() throws IOException {
        List<String> files = List.of(
                "FiscalPanel.java",
                "FiscalIvaTab.java",
                "FiscalTaxRatesTab.java",
                "FiscalWithholdingsTab.java",
                "FiscalPayrollTab.java",
                "FiscalDeclarationsTab.java"
        );
        for (String file : files) {
            Path p = resolveGui(file);
            if (Files.exists(p)) {
                String content = Files.readString(p);
                assertFalse(content.contains("JOptionPane.show"),
                        file + " não pode conter chamadas legadas a JOptionPane");
            }
        }
    }

    @Test
    @DisplayName("DECOMP-03: Estrutura canónica das 5 abas fiscais preservada")
    void decomp03_tabStructurePreserved() {
        FiscalApiClient fiscalApi = Mockito.mock(FiscalApiClient.class);
        FiscalPanel panel = new FiscalPanel(fiscalApi);

        JTabbedPane tabs = null;
        for (java.awt.Component comp : panel.getComponents()) {
            if (comp instanceof JTabbedPane tp) {
                tabs = tp;
                break;
            }
        }
        assertNotNull(tabs, "FiscalPanel deve conter um JTabbedPane");
        assertEquals(5, tabs.getTabCount(), "FiscalPanel deve conter exactamente 5 abas corporativas");
        assertEquals("Apuramento IVA", tabs.getTitleAt(0));
        assertEquals("Taxas Fiscais", tabs.getTitleAt(1));
        assertEquals("Retenções na Fonte", tabs.getTitleAt(2));
        assertEquals("IRPS & INSS Salarial", tabs.getTitleAt(3));
        assertEquals("Declarações", tabs.getTitleAt(4));
    }

    @Test
    @DisplayName("DECOMP-04: Ciclo de vida e refreshAll assíncrono seguro")
    void decomp04_lifecycleExecution() {
        FiscalApiClient fiscalApi = Mockito.mock(FiscalApiClient.class);
        Mockito.when(fiscalApi.getAllTaxRates()).thenReturn(Collections.emptyList());
        Mockito.when(fiscalApi.getWithholdings(Mockito.any())).thenReturn(Collections.emptyList());
        IvaSummaryDTO dummyIva = new IvaSummaryDTO(2026, 10, 1L, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                Collections.emptyList(), Collections.emptyList());
        Mockito.when(fiscalApi.ivaSummary(Mockito.any(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any()))
                .thenReturn(dummyIva);

        try {
            CurrentUserContext.setCurrentCompanyId(1L);
            CurrentUserContext.setCurrentUser("admin", "ADMIN");
            FiscalPanel panel = new FiscalPanel(fiscalApi);
            assertDoesNotThrow(panel::onPanelSelected, "onPanelSelected() deve executar sem NPE");
            assertDoesNotThrow(panel::refreshAll, "refreshAll() deve delegar para todas as abas sem NPE");
        } finally {
            CurrentUserContext.clear();
        }
    }
}
