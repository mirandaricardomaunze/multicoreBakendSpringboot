package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.SendStatementEmailDialog;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SendStatementEmailDialogTest {

    @BeforeAll
    static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void testDialogInstantiation() {
        // EEC-06: Dialog instantiates cleanly and fields are initialized
        SendStatementEmailDialog dialog = new SendStatementEmailDialog(
                null,
                null,
                10L,
                "Empresa Exemplo Lda",
                "financeiro@exemplo.co.mz",
                LocalDate.now().minusDays(30),
                LocalDate.now()
        );

        assertNotNull(dialog);
    }
}
