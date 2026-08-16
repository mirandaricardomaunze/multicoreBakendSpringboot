package com.phcpro.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PosButtonColourHierarchyTest {

    @Test
    void posUsesSemanticButtonColours() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/phcpro/gui/POSPanel.java"));
        assertThat(source)
                .contains("createSuccessButton(\"Abrir Caixa\")")
                .contains("createDangerButton(\"Fechar Caixa\")")
                .contains("createWarningButton(\"Sangria / Suprimento\")")
                .contains("createSuccessButton(\"Finalizar Venda (F9)\")")
                .contains("createDangerButton(\"Remover Selecionado\")")
                .contains("createPrimaryButton(\"Quantidade (F6)\")");
    }
}
