package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ecrã de importação e formulários que ela torna coerentes
 * (docs/IMPORTACAO_DADOS_HARNESS.md IMP-UI-01, IMP-UI-02).
 */
class DataImportPanelHarnessTest {

    private static final String VALID_NUIT = "400123452";

    @Test
    void impUi02_formularioDeCliente_aceitaEmailVazioEEnviaNull() {
        ClientFormFields fields = new ClientFormFields(null);
        fields.nameField.setText("Mercearia Sol");
        fields.taxIdField.setText(VALID_NUIT);
        fields.emailField.setText("   ");

        assertTrue(fields.validateAll(), "cliente sem email tem de ser aceite");
        assertNull(fields.email(), "email vazio segue como null para o backend");
        fields.emailField.setText("");
        assertTrue(fields.validateAll(), "campo email totalmente vazio também é aceite");
    }

    @Test
    void impUi02_formularioDeCliente_emailPreenchidoContinuaValidado() {
        ClientFormFields fields = new ClientFormFields(null);
        fields.nameField.setText("Mercearia Sol");
        fields.taxIdField.setText(VALID_NUIT);

        fields.emailField.setText("sem-arroba");
        assertFalse(fields.validateAll(), "email mal formado continua a ser recusado");

        fields.emailField.setText("  geral@sol.co.mz ");
        assertTrue(fields.validateAll());
        assertEquals("geral@sol.co.mz", fields.email());
    }

    @Test
    void impUi02_edicaoDeClienteSemEmail_abreComCampoVazio() {
        ClientDTO existing = new ClientDTO(7L, "Mercearia Sol", VALID_NUIT, null, null, 0, BigDecimal.ZERO);

        ClientFormFields fields = new ClientFormFields(existing);

        assertEquals("", fields.emailField.getText());
        assertTrue(fields.validateAll());
        assertNull(fields.email());
    }

    @Test
    void impUi02_nomeENuitContinuamObrigatorios() {
        ClientFormFields fields = new ClientFormFields(null);
        assertFalse(fields.validateAll(), "sem nome nem NUIT o formulário é recusado");
    }
}
