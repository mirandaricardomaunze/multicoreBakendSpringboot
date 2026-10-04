package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TableCardContainmentAuditTest {

    private static final Path GUI_ROOT = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    @Test
    @DisplayName("AUDIT-01: Todos os painéis que contêm JTable devem utilizar ModernPanel como card de contenção")
    void auditTableCardContainment() throws IOException {
        List<String> missingModernPanel = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(GUI_ROOT)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .forEach(path -> {
                     try {
                         String fileName = path.getFileName().toString();
                         boolean isPanelOrTab = fileName.endsWith("Panel.java") 
                                 || fileName.endsWith("Tab.java") 
                                 || fileName.endsWith("View.java");
                         if (!isPanelOrTab) {
                             return;
                         }

                         String content = Files.readString(path);
                         boolean instantiatesTable = content.contains("new JTable(");
                         if (instantiatesTable) {
                             boolean hasModernPanel = content.contains("ModernPanel");
                             if (!hasModernPanel) {
                                 missingModernPanel.add(fileName);
                             }
                         }
                     } catch (IOException e) {
                         throw new RuntimeException(e);
                     }
                 });
        }

        System.out.println("=== AUDIT-01: Painéis com JTable sem ModernPanel ===");
        missingModernPanel.forEach(f -> System.out.println(" - " + f));
        assertEquals(0, missingModernPanel.size(), 
            "Os seguintes painéis possuem JTable mas não usam ModernPanel para contenção: " + missingModernPanel);
    }

    @Test
    @DisplayName("AUDIT-02: Auditoria de barras de acção (UIHelper.actionsBar) com 4 ou mais botões abertos")
    void auditHeaderActionsButtonCount() throws IOException {
        List<String> crowdedActionBars = new ArrayList<>();
        Pattern pattern = Pattern.compile("actionsBar\\s*\\(([^;]+)\\)");

        try (Stream<Path> paths = Files.walk(GUI_ROOT)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                 .forEach(path -> {
                     try {
                         List<String> lines = Files.readAllLines(path);
                         for (int i = 0; i < lines.size(); i++) {
                             String line = lines.get(i);
                             Matcher m = pattern.matcher(line);
                             while (m.find()) {
                                 String args = m.group(1);
                                 // Conta quantas vírgulas estão no mesmo nível (fora de parênteses)
                                 int commaCount = 0;
                                 int depth = 0;
                                 for (char c : args.toCharArray()) {
                                     if (c == '(') depth++;
                                     else if (c == ')') depth--;
                                     else if (c == ',' && depth == 0) commaCount++;
                                 }
                                 int buttonCount = commaCount + 1;
                                 if (buttonCount >= 4) {
                                     crowdedActionBars.add(path.getFileName() + ":" + (i + 1) + " (" + buttonCount + " botões: " + args.trim() + ")");
                                 }
                             }
                         }
                     } catch (IOException e) {
                         throw new RuntimeException(e);
                     }
                 });
        }

        System.out.println("=== AUDIT-02: Barras de acção com 4 ou mais botões abertos (potencial sobreposição) ===");
        crowdedActionBars.forEach(b -> System.out.println(" - " + b));
    }
}
