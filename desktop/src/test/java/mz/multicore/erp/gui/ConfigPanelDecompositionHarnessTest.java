package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness: Decomposição de ConfigPanel (Fase 10)")
class ConfigPanelDecompositionHarnessTest {

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
        Path mainPath = resolveGui("ConfigPanel.java");
        Path backupsPath = resolveGui("ConfigBackupsPanel.java");
        Path colsPath = resolveGui("ConfigDocumentColumnsPanel.java");
        Path subPath = resolveGui("ConfigSubscriptionPanel.java");
        Path supportPath = resolveGui("ConfigSupportPanel.java");

        assertTrue(Files.exists(mainPath), "ConfigPanel.java deve existir");
        assertTrue(Files.exists(backupsPath), "ConfigBackupsPanel.java deve existir");
        assertTrue(Files.exists(colsPath), "ConfigDocumentColumnsPanel.java deve existir");
        assertTrue(Files.exists(subPath), "ConfigSubscriptionPanel.java deve existir");
        assertTrue(Files.exists(supportPath), "ConfigSupportPanel.java deve existir");

        long mainLines = Files.lines(mainPath).count();
        long backupsLines = Files.lines(backupsPath).count();
        long colsLines = Files.lines(colsPath).count();
        long subLines = Files.lines(subPath).count();
        long supportLines = Files.lines(supportPath).count();

        assertTrue(mainLines <= 450, "ConfigPanel.java excedeu 450 linhas (actual: " + mainLines + ")");
        assertTrue(backupsLines <= 220, "ConfigBackupsPanel.java excedeu 220 linhas (actual: " + backupsLines + ")");
        assertTrue(colsLines <= 200, "ConfigDocumentColumnsPanel.java excedeu 200 linhas (actual: " + colsLines + ")");
        assertTrue(subLines <= 75, "ConfigSubscriptionPanel.java excedeu 75 linhas (actual: " + subLines + ")");
        assertTrue(supportLines <= 200, "ConfigSupportPanel.java excedeu 200 linhas (actual: " + supportLines + ")");
    }

    @Test
    @DisplayName("DECOMP-02: Zero diálogos legados JOptionPane nos componentes de configuração")
    void decomp02_zeroLegacyDialogs() throws IOException {
        List<String> files = List.of(
                "ConfigPanel.java",
                "ConfigBackupsPanel.java",
                "ConfigDocumentColumnsPanel.java",
                "ConfigSubscriptionPanel.java",
                "ConfigSupportPanel.java"
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
    @DisplayName("DECOMP-03: Estrutura canónica das abas corporativas preservada")
    void decomp03_tabStructurePreserved() {
        UserApiClient userApi = Mockito.mock(UserApiClient.class);
        AuditApiClient auditApi = Mockito.mock(AuditApiClient.class);
        BackupApiClient backupApi = Mockito.mock(BackupApiClient.class);
        DocumentConfigApiClient docApi = Mockito.mock(DocumentConfigApiClient.class);
        SupportApiClient supportApi = Mockito.mock(SupportApiClient.class);
        MySubscriptionApiClient subApi = Mockito.mock(MySubscriptionApiClient.class);
        SystemMonitoringApiClient monApi = Mockito.mock(SystemMonitoringApiClient.class);

        ConfigPanel panel = new ConfigPanel(userApi, auditApi, backupApi, docApi, supportApi, subApi, monApi);

        JTabbedPane tabbedPane = null;
        for (java.awt.Component c : panel.getComponents()) {
            if (c instanceof JTabbedPane tp) {
                tabbedPane = tp;
                break;
            }
        }

        assertNotNull(tabbedPane, "JTabbedPane deve ser instanciado em ConfigPanel");
        assertEquals(6, tabbedPane.getTabCount(), "Todas as 6 abas de configuração devem estar presentes");
        assertEquals("Log de Auditoria Geral", tabbedPane.getTitleAt(0));
        assertEquals("Cópias de Segurança & Backups", tabbedPane.getTitleAt(1));
        assertEquals("Utilizadores & Permissões", tabbedPane.getTitleAt(2));
        assertEquals("Colunas dos Documentos", tabbedPane.getTitleAt(3));
        assertEquals("Suporte à Plataforma", tabbedPane.getTitleAt(4));
        assertEquals("A Minha Assinatura", tabbedPane.getTitleAt(5));
    }

    @Test
    @DisplayName("DECOMP-04: Barra superior de temas, densidade e saúde preservada")
    void decomp04_appearanceBarPreserved() {
        UserApiClient userApi = Mockito.mock(UserApiClient.class);
        AuditApiClient auditApi = Mockito.mock(AuditApiClient.class);
        BackupApiClient backupApi = Mockito.mock(BackupApiClient.class);
        DocumentConfigApiClient docApi = Mockito.mock(DocumentConfigApiClient.class);
        SupportApiClient supportApi = Mockito.mock(SupportApiClient.class);
        MySubscriptionApiClient subApi = Mockito.mock(MySubscriptionApiClient.class);
        SystemMonitoringApiClient monApi = Mockito.mock(SystemMonitoringApiClient.class);

        ConfigPanel panel = new ConfigPanel(userApi, auditApi, backupApi, docApi, supportApi, subApi, monApi);

        boolean foundMonitor = false;
        boolean foundTheme = false;
        boolean foundDensity = false;

        for (java.awt.Component c : panel.getComponents()) {
            if (c instanceof JPanel bar && bar.getComponentCount() > 0) {
                for (java.awt.Component child : bar.getComponents()) {
                    if (child instanceof JButton btn) {
                        String text = btn.getText();
                        if (text != null) {
                            if (text.contains("Diagnóstico & Saúde")) foundMonitor = true;
                            if (text.contains("Tema:")) foundTheme = true;
                            if (text.contains("Densidade:")) foundDensity = true;
                        }
                    }
                }
            }
        }

        assertTrue(foundMonitor, "Botão de Diagnóstico & Saúde deve existir na barra de aparência");
        assertTrue(foundTheme, "Botão de alternância de Tema deve existir na barra de aparência");
        assertTrue(foundDensity, "Botão de alternância de Densidade deve existir na barra de aparência");
    }
}
