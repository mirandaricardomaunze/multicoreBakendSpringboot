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
    void installer_usesPomVersionAndPublishableName() throws Exception {
        String script = Files.readString(ROOT.resolve("scripts/build-windows-installer.ps1"));
        assertThat(script).contains("multicore-parent</artifactId><version>", "$Version -ne $pomVersion",
                mz.multicore.erp.desktop.config.DesktopApiConfig.INSTALLER_FILE);
        assertThat(script).doesNotContain("$Version = \"1.0.0\"");
    }

    @Test
    void apiConfiguration_livesOutsideInstallationDirectory() throws Exception {
        String settings = Files.readString(ROOT.resolve(
                "desktop/src/main/java/mz/multicore/erp/desktop/config/DesktopLocalSettings.java"));
        String config = Files.readString(ROOT.resolve(
                "desktop/src/main/java/mz/multicore/erp/desktop/config/DesktopApiConfig.java"));
        assertThat(settings).contains("LOCALAPPDATA", "resolve(\"MulticoreERP\")",
                "localDataDir().resolve(\"desktop.properties\")");
        assertThat(config).contains("DesktopLocalSettings.readBaseUrl");
        assertThat(Files.readString(ROOT.resolve("scripts/build-windows-installer.ps1")))
                .doesNotContain("desktop.properties");
    }

    /** AU-06: build writes the manifest; publishing checks it and puts the .exe before the manifest. */
    @Test
    void release_writesAndChecksManifest_exeBeforeManifest() throws Exception {
        String build = Files.readString(ROOT.resolve("scripts/build-windows-installer.ps1"));
        assertThat(build).contains("Get-FileHash -Algorithm SHA256",
                mz.multicore.erp.desktop.config.DesktopApiConfig.MANIFEST_FILE,
                "`\"version`\"", "`\"sha256`\"", "`\"size`\"");
        String publish = Files.readString(ROOT.resolve("scripts/publicar-instalador.sh"));
        assertThat(publish).contains("sha256sum", "não corresponde ao .exe");
        assertThat(publish.indexOf("mv \"$DEST/Multicore-instalador.exe.tmp\""))
                .isPositive()
                .isLessThan(publish.indexOf("mv \"$DEST/Multicore-instalador.json.tmp\""));
    }

    /** AC-59: the installer packs only the jar and names its Start-menu folder (it was "Unknown"). */
    @Test
    void installer_packsOnlyTheJarAndNamesMenuGroup() throws Exception {
        String script = Files.readString(ROOT.resolve("scripts/build-windows-installer.ps1"));
        assertThat(script).contains("--input $inputStage", "--win-menu-group \"Multicore\"");
        assertThat(script).doesNotContain("--input $desktopTarget");
    }
}
