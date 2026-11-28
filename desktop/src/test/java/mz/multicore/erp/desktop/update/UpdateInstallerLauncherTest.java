package mz.multicore.erp.desktop.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** AU-05: the helper waits for the ERP, re-checks the SHA-256, runs /quiet, and relaunches only for "agora". */
class UpdateInstallerLauncherTest {

    @TempDir Path dir;

    @Test
    void command_passesPathsAsArguments_andRelaunchOnlyWhenAsked() throws Exception {
        UpdateInstallerLauncher launcher = new UpdateInstallerLauncher(dir.resolve("updates"));
        UpdateManifest manifest = new UpdateManifest("1.0.4", "d".repeat(64), 10);
        Path exe = dir.resolve("Multicore.exe");

        List<String> later = launcher.prepare(dir.resolve("i.exe"), manifest, exe, false);
        List<String> now = launcher.prepare(dir.resolve("i.exe"), manifest, exe, true);

        assertThat(later).contains("-Sha256", "d".repeat(64), "-AppExe", exe.toAbsolutePath().toString())
                .doesNotContain("-Relaunch");
        assertThat(now).endsWith("-Relaunch");
        assertThat(dir.resolve("updates/instalar.ps1")).exists();
    }

    @Test
    void script_waitsForTheInstalledExe_rechecksHash_runsQuiet() throws Exception {
        String script = new String(UpdateInstallerLauncher.class.getResourceAsStream("/update/instalar.ps1")
                .readAllBytes(), StandardCharsets.UTF_8);

        assertThat(script).contains("$_.Path -ieq $AppExe", "SHA256]::Create()",
                "-ArgumentList \"/quiet\"", "if ($Relaunch)");
        assertThat(script.indexOf("SHA256]::Create()")).isLessThan(script.indexOf("Start-Process -FilePath $Installer"));
        assertThat(script.chars().allMatch(c -> c < 128)).as("PowerShell 5.1 reads the script as ANSI").isTrue();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void script_runsVerifiedInstaller_andRefusesTamperedOne() throws Exception {
        Path updates = dir.resolve("updates");
        Path marker = dir.resolve("instalado.txt");
        Path installer = dir.resolve("fake-installer.cmd");
        Files.writeString(installer, "@echo %* > \"" + marker + "\"\r\n@exit /b 0\r\n");
        UpdateManifest good = new UpdateManifest("1.0.4", InstallerVerifier.sha256(installer), Files.size(installer));
        UpdateInstallerLauncher launcher = new UpdateInstallerLauncher(updates);
        Path notRunning = dir.resolve("nao-esta-a-correr/Multicore.exe");

        int code = run(launcher.prepare(installer, good, notRunning, false));
        assertThat(code).as(() -> readLog(updates)).isZero();
        assertThat(Files.readString(marker).trim()).isEqualTo("/quiet");
        assertThat(installer).as("removed after a successful install").doesNotExist();

        Files.writeString(installer, "@echo adulterado > \"" + marker + "\"\r\n");
        Files.delete(marker);
        assertThat(run(launcher.prepare(installer, good, notRunning, false))).isEqualTo(3);
        assertThat(marker).doesNotExist();
        assertThat(Files.readString(updates.resolve("instalar.log"))).contains("SHA-256 diferente");
    }

    private static String readLog(Path updates) {
        try {
            return Files.readString(updates.resolve("instalar.log"));
        } catch (Exception e) {
            return "(sem instalar.log)";
        }
    }

    private static int run(List<String> command) throws Exception {
        Process process = UpdateInstallerLauncher.processFor(command).redirectErrorStream(true).start();
        process.getInputStream().readAllBytes();
        assertThat(process.waitFor(60, TimeUnit.SECONDS)).isTrue();
        return process.exitValue();
    }
}
