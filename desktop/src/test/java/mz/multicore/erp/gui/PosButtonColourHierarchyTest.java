package mz.multicore.erp.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PosButtonColourHierarchyTest {

    @Test
    void posUsesSemanticButtonColours() throws Exception {
        String source = Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/POSPanel.java"))
                + Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/PosWorkspaceBuilder.java"));
        assertThat(source)
                .contains("createSuccessButton(\"Abrir Caixa\")")
                .contains("createDangerButton(\"Fechar Caixa\")")
                .contains("createWarningButton(\"Sangria / Suprimento\")")
                .contains("createSuccessButton(\"Finalizar Venda\")")
                .contains("createDangerButton(\"Remover\")")
                .contains("createPrimaryButton(\"Quantidade\")");
    }
}
