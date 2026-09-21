package mz.multicore.erp.desktop;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness AC-55/AC-56 do instalador Windows. */
class WindowsInstallerHarnessTest {

    private static final Path ROOT = Files.isDirectory(Path.of("desktop")) ? Path.of("") : Path.of("..");

    @Test
    void installer_isPerUserAndUsesDesktopJar() throws Exception {
        String script = Files.readString(ROOT.resolve("scripts/build-windows-installer.ps1"));
        assertThat(script).contains("--win-per-user-install", "multicore-desktop-*",
                "org.springframework.boot.loader.launch.JarLauncher", "--license-file");
    }

    @Test
    void apiConfiguration_livesOutsideInstallationDirectory() throws Exception {
        String settings = Files.readString(ROOT.resolve(
                "desktop/src/main/java/mz/multicore/erp/desktop/config/DesktopLocalSettings.java"));
        String config = Files.readString(ROOT.resolve(
                "desktop/src/main/java/mz/multicore/erp/desktop/config/DesktopApiConfig.java"));
        assertThat(settings).contains("LOCALAPPDATA", "Multicore", "desktop.properties");
        assertThat(config).contains("DesktopLocalSettings.readBaseUrl");
        assertThat(Files.readString(ROOT.resolve("scripts/build-windows-installer.ps1")))
                .doesNotContain("desktop.properties");
    }
}
