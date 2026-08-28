package mz.multicore.erp.desktop;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness do gate desktop descrito em docs/LICENCA_UTILIZADOR_SPEC.md. */
class LicenseGateHarnessTest {

    private static final Path ROOT = Files.isDirectory(Path.of("desktop"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();

    @Test
    void launcherVerificaLicencaAntesDeAbrirJanelaPrincipal() throws Exception {
        String launcher = Files.readString(ROOT.resolve(
                "desktop/src/main/java/mz/multicore/erp/desktop/DesktopLauncher.java"));
        assertThat(launcher.indexOf("ensureLicenseAccepted()"))
                .isLessThan(launcher.indexOf("showMainFrame(null"));
    }

    @Test
    void chamadasRemotasDoDialogoCorremForaDoEdt() throws Exception {
        String dialog = Files.readString(ROOT.resolve(
                "desktop/src/main/java/mz/multicore/erp/gui/LicenseAcceptanceDialog.java"));
        assertThat(dialog).contains("new SwingWorker<LicenseTermsDTO, Void>()",
                "new SwingWorker<Void, Void>()");
    }
}
