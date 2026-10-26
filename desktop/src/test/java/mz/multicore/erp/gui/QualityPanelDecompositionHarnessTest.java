package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.QualityApiClient;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.quality.QualityAssignmentsTab;
import mz.multicore.erp.gui.quality.QualityCatalogTab;
import mz.multicore.erp.gui.quality.QualityExpiredRadarTab;
import mz.multicore.erp.modules.quality.dto.QualityKpiDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("HARNESS-QUALITY-PANEL-DECOMPOSITION-001: Decomposição do Painel de Qualidade e HST")
class QualityPanelDecompositionHarnessTest {

    private QualityApiClient qualityApiClient;
    private HRApiClient hrApiClient;
    private PrintApiClient printApiClient;

    @BeforeEach
    void setUp() {
        qualityApiClient = mock(QualityApiClient.class);
        hrApiClient = mock(HRApiClient.class);
        printApiClient = mock(PrintApiClient.class);

        when(qualityApiClient.getQualityKpis(anyLong())).thenReturn(new QualityKpiDTO(12L, 3L, 2L, 45L, 0L));
        when(qualityApiClient.searchAssignments(anyLong(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 25, 0L, 0));
        when(qualityApiClient.getExpiredWorkerAlerts(anyLong())).thenReturn(List.of());
        when(qualityApiClient.searchEquipments(anyLong(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(), 0, 25, 0L, 0));
    }

    @Test
    @DisplayName("QD-01: Construtores preservados e grelha de 4 KPIs inicializada")
    void testConstructorsAndKpiGrid() {
        QualityPanel panel2Args = new QualityPanel(qualityApiClient, hrApiClient);
        assertNotNull(panel2Args, "Deve instanciar via construtor de 2 argumentos");

        QualityPanel panel3Args = new QualityPanel(qualityApiClient, hrApiClient, printApiClient);
        assertNotNull(panel3Args, "Deve instanciar via construtor de 3 argumentos");

        // Verifica grid de KPIs no topo
        JPanel north = findComponent(panel3Args, JPanel.class);
        assertNotNull(north, "Deve conter container de KPIs no NORTH");

        int modernPanels = 0;
        for (Component c : panel3Args.getComponents()) {
            if (c instanceof JPanel p && p.getLayout() instanceof GridLayout) {
                for (Component sub : p.getComponents()) {
                    if (sub instanceof ModernPanel) modernPanels++;
                }
            }
        }
        assertEquals(4, modernPanels, "Deve possuir exatamente 4 cartões ModernPanel de KPI");
    }

    @Test
    @DisplayName("QD-02: QualityPanel reduzido para <= 130 linhas e 3 abas operacionais")
    void testLineCountAndTabStructure() throws IOException {
        Path path = Path.of("src/main/java/mz/multicore/erp/gui/QualityPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/QualityPanel.java");
        }
        assertTrue(Files.exists(path), "QualityPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 130, "QualityPanel.java deve possuir <= 130 linhas. Atual: " + lines.size());

        QualityPanel panel = new QualityPanel(qualityApiClient, hrApiClient, printApiClient);
        JTabbedPane tabbedPane = findComponent(panel, JTabbedPane.class);
        assertNotNull(tabbedPane, "Painel deve conter JTabbedPane corporativo");
        assertEquals(3, tabbedPane.getTabCount(), "Deve conter exatamente 3 abas");
        assertTrue(tabbedPane.getTitleAt(0).contains("Atribuições a Colaboradores"));
        assertTrue(tabbedPane.getTitleAt(1).contains("Radar de Validades Expiradas"));
        assertTrue(tabbedPane.getTitleAt(2).contains("Catálogo de Equipamentos"));
    }

    @Test
    @DisplayName("QD-03: Abas especializadas desacopladas no pacote mz.multicore.erp.gui.quality")
    void testSpecializedTabsDecoupled() {
        QualityPanel panel = new QualityPanel(qualityApiClient, hrApiClient, printApiClient);

        QualityAssignmentsTab assignmentsTab = findComponent(panel, QualityAssignmentsTab.class);
        assertNotNull(assignmentsTab, "QualityAssignmentsTab deve estar instanciado na primeira aba");

        QualityExpiredRadarTab expiredRadarTab = findComponent(panel, QualityExpiredRadarTab.class);
        assertNotNull(expiredRadarTab, "QualityExpiredRadarTab deve estar instanciado na segunda aba");

        QualityCatalogTab catalogTab = findComponent(panel, QualityCatalogTab.class);
        assertNotNull(catalogTab, "QualityCatalogTab deve estar instanciado na terceira aba");
    }

    @Test
    @DisplayName("QD-04: Erradicação absoluta de JOptionPane em QualityPanel e pacote quality")
    void testZeroJOptionPaneInQualityPackage() throws IOException {
        Path qualityPkg = Path.of("desktop/src/main/java/mz/multicore/erp/gui/quality");
        if (!Files.exists(qualityPkg)) {
            qualityPkg = Path.of("src/main/java/mz/multicore/erp/gui/quality");
        }
        assertTrue(Files.exists(qualityPkg), "Pacote quality deve existir");

        try (Stream<Path> stream = Files.walk(qualityPkg)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        try {
                            List<String> content = Files.readAllLines(p);
                            for (int i = 0; i < content.size(); i++) {
                                String line = content.get(i);
                                assertFalse(line.contains("JOptionPane"),
                                        "Ficheiro " + p.getFileName() + " não pode conter JOptionPane na linha " + (i + 1));
                            }
                        } catch (IOException e) {
                            fail("Falha ao ler " + p);
                        }
                    });
        }
    }

    @Test
    @DisplayName("QD-05: onPanelSelected e refreshAll delegam sem exceções")
    void testRefreshDelegation() {
        QualityPanel panel = new QualityPanel(qualityApiClient, hrApiClient, printApiClient);
        assertDoesNotThrow(panel::refreshAll, "refreshAll deve executar sem erros");
        assertDoesNotThrow(panel::onPanelSelected, "onPanelSelected deve delegar sem erros");
        verify(qualityApiClient, atLeastOnce()).getQualityKpis(anyLong());
    }

    @SuppressWarnings("unchecked")
    private static <T> T findComponent(Container container, Class<T> clazz) {
        for (Component comp : container.getComponents()) {
            if (clazz.isInstance(comp)) return (T) comp;
            if (comp instanceof Container sub) {
                T found = findComponent(sub, clazz);
                if (found != null) return found;
            }
        }
        return null;
    }
}
