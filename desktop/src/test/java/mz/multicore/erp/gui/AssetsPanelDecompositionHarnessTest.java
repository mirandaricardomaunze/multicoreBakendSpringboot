package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Harness de Decomposição Modular: AssetsPanel")
class AssetsPanelDecompositionHarnessTest {

    private static final Path ROOT = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("AST-01: AssetsPanel.java deve possuir <= 250 linhas")
    void testLineCount() throws IOException {
        Path path = ROOT.resolve("AssetsPanel.java");
        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 250,
                () -> "AssetsPanel.java deve possuir <= 250 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("AST-02: Colaboradores devem possuir limites de tamanho estritos")
    void testCollaboratorsLineCounts() throws IOException {
        Path form = ROOT.resolve("AssetFormDialog.java");
        List<String> formLines = Files.readAllLines(form);
        assertTrue(formLines.size() <= 125,
                () -> "AssetFormDialog.java deve possuir <= 125 linhas. Atual: " + formLines.size());

        Path dep = ROOT.resolve("AssetDepreciationCoordinator.java");
        List<String> depLines = Files.readAllLines(dep);
        assertTrue(depLines.size() <= 100,
                () -> "AssetDepreciationCoordinator.java deve possuir <= 100 linhas. Atual: " + depLines.size());
    }

    @Test
    @DisplayName("AST-03: Campos e métodos com visibilidade de pacote devem ser preservados")
    void testPackagePrivateFieldsAndMethodsPreserved() throws NoSuchFieldException, NoSuchMethodException {
        Field tableField = AssetsPanel.class.getDeclaredField("table");
        assertFalse(Modifier.isPrivate(tableField.getModifiers()), "table deve ser visível no pacote");
        assertEquals(JTable.class, tableField.getType());

        Field feedbackField = AssetsPanel.class.getDeclaredField("feedback");
        assertFalse(Modifier.isPrivate(feedbackField.getModifiers()), "feedback deve ser visível no pacote");

        Method showAssetDetails = AssetsPanel.class.getDeclaredMethod("showAssetDetails");
        assertFalse(Modifier.isPrivate(showAssetDetails.getModifiers()), "showAssetDetails deve ser visível no pacote");
    }

    @Test
    @DisplayName("AST-04: Eliminação total de JOptionPane")
    void testNoLegacyDialogs() throws IOException {
        for (String file : List.of("AssetsPanel.java", "AssetFormDialog.java", "AssetDepreciationCoordinator.java")) {
            String content = Files.readString(ROOT.resolve(file));
            assertFalse(content.contains("JOptionPane"),
                    "Ficheiro " + file + " não deve utilizar JOptionPane.");
        }
    }

    @Test
    @DisplayName("AST-05: Construtores públicos e ciclo de vida preservados")
    void testConstructorsAndLifecyclePreserved() throws NoSuchMethodException {
        AssetsPanel.class.getConstructor(mz.multicore.erp.desktop.client.AssetApiClient.class);
        AssetsPanel.class.getConstructor(
                mz.multicore.erp.desktop.client.AssetApiClient.class,
                mz.multicore.erp.desktop.client.PrintApiClient.class
        );
        AssetsPanel.class.getMethod("onPanelSelected");
    }
}
