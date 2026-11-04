package mz.multicore.erp.desktop;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Os testes nunca escrevem em ~/.multicore do utilizador real (favoritos, rascunhos, contingência POS). */
class TestHomeIsolationTest {

    @Test
    void pastaPessoalDosTestesFicaDentroDeTarget() {
        Path home = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        assertTrue(home.endsWith(Path.of("target", "test-home")), "user.home dos testes: " + home);
    }
}
