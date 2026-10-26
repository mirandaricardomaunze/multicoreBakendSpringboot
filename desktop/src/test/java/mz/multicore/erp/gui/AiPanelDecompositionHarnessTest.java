package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.AiAssistantApiClient;
import mz.multicore.erp.gui.intelligence.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness de Validação da Decomposição do Assistente de Gestão (SPEC-AI-PANEL-DECOMPOSITION-001)")
class AiPanelDecompositionHarnessTest {

    private static final Path GUI_ROOT = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    @Test
    @DisplayName("DECOMP-01: Limites de linhas por ficheiro para prevenir acoplamento monolítico")
    void verifyLineLimits() throws IOException {
        Map<Path, Integer> limits = Map.of(
                GUI_ROOT.resolve("AiManagementAssistantPanel.java"), 150,
                GUI_ROOT.resolve("intelligence/AiAnalysisView.java"), 250,
                GUI_ROOT.resolve("intelligence/AiOperationalDraftsView.java"), 200,
                GUI_ROOT.resolve("intelligence/AiDocumentCreationView.java"), 200,
                GUI_ROOT.resolve("intelligence/AiDocumentHistoryView.java"), 250,
                GUI_ROOT.resolve("intelligence/AiDocumentComparisonDialog.java"), 100
        );

        for (Map.Entry<Path, Integer> entry : limits.entrySet()) {
            Path file = entry.getKey();
            int maxLines = entry.getValue();
            assertTrue(Files.exists(file), "O ficheiro deve existir: " + file);
            long lineCount;
            try (var lines = Files.lines(file)) {
                lineCount = lines.count();
            }
            assertTrue(lineCount <= maxLines,
                    String.format("O ficheiro %s possui %d linhas, excedendo o teto de %d linhas.",
                            file.getFileName(), lineCount, maxLines));
        }
    }

    @Test
    @DisplayName("DECOMP-02: AiAnalysisView desactiva paginação artificial para garantir altura total à tabela")
    void verifyEvidenceTableHasPaginationDisabled() throws IOException {
        Path analysisViewPath = GUI_ROOT.resolve("intelligence/AiAnalysisView.java");
        String content = Files.readString(analysisViewPath);
        assertTrue(content.contains("ClientTablePagination.DISABLED"),
                "AiAnalysisView deve desativar a paginação artificial na tabela de evidências.");
        assertTrue(content.contains("ModernButton exportBtn = UIHelper.createExportButton"),
                "AiAnalysisView deve incluir botão de exportação tabular.");
    }

    @Test
    @DisplayName("DECOMP-03: Instanciação limpa do orquestrador com as 4 sub-views operacionais")
    void verifyOrchestratorInstantiation() {
        AiAssistantApiClient mockClient = Mockito.mock(AiAssistantApiClient.class);
        AiManagementAssistantPanel panel = new AiManagementAssistantPanel(mockClient, module -> {});

        assertNotNull(panel.getAnalysisView(), "AiAnalysisView deve estar inicializada.");
        assertNotNull(panel.getDraftsView(), "AiOperationalDraftsView deve estar inicializada.");
        assertNotNull(panel.getDocumentsView(), "AiDocumentCreationView deve estar inicializada.");
        assertNotNull(panel.getHistoryView(), "AiDocumentHistoryView deve estar inicializada.");

        // Confirma existência do JTabbedPane com 4 abas
        JTabbedPane tabs = null;
        for (java.awt.Component comp : panel.getComponents()) {
            if (comp instanceof JTabbedPane tp) {
                tabs = tp;
                break;
            }
        }
        assertNotNull(tabs, "O painel principal deve conter um JTabbedPane de navegação.");
        assertEquals(4, tabs.getTabCount(), "O JTabbedPane deve ter exactamente 4 abas corporativas.");
        assertEquals("Consulta & Análise", tabs.getTitleAt(0));
        assertEquals("Rascunho Operacional Controlado", tabs.getTitleAt(1));
        assertEquals("Criar Documento Profissional", tabs.getTitleAt(2));
        assertEquals("Arquivo & Versões", tabs.getTitleAt(3));
    }
}
