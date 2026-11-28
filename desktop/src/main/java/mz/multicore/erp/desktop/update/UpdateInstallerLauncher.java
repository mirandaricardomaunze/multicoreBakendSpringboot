package mz.multicore.erp.desktop.update;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Starts the hidden PowerShell helper that waits for the ERP to close, re-checks the SHA-256, runs the installer
 * silently and, for "agora", reopens the program (§4.4). The ERP itself never runs the installer: its own files
 * are locked while it runs.
 */
public final class UpdateInstallerLauncher {

    static final String SCRIPT_RESOURCE = "/update/instalar.ps1";
    static final String SCRIPT_NAME = "instalar.ps1";
    static final String LOG_NAME = "instalar.log";

    private final Path updatesDir;

    public UpdateInstallerLauncher(Path updatesDir) {
        this.updatesDir = updatesDir;
    }

    /** Copies the helper next to the installer and returns the command that runs it. */
    List<String> prepare(Path installer, UpdateManifest manifest, Path appExe, boolean relaunch) throws IOException {
        Files.createDirectories(updatesDir);
        Path script = updatesDir.resolve(SCRIPT_NAME);
        try (InputStream in = UpdateInstallerLauncher.class.getResourceAsStream(SCRIPT_RESOURCE)) {
            if (in == null) throw new IOException("Recurso em falta: " + SCRIPT_RESOURCE);
            Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
        }
        List<String> command = new ArrayList<>(List.of("powershell.exe", "-NoProfile", "-NonInteractive",
                "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-File", script.toString(),
                "-Installer", installer.toAbsolutePath().toString(),
                "-Sha256", manifest.sha256(),
                "-AppExe", appExe.toAbsolutePath().toString(),
                "-Log", updatesDir.resolve(LOG_NAME).toString()));
        if (relaunch) command.add("-Relaunch");
        return command;
    }

    /**
     * Windows PowerShell 5.1 with the PSModulePath of a PowerShell 7 parent fails to load its own modules; the
     * ERP opened from a PowerShell 7 terminal would hand it exactly that. Start it with its default path.
     */
    static ProcessBuilder processFor(List<String> command) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().remove("PSModulePath");
        return builder;
    }

    public void start(Path installer, UpdateManifest manifest, Path appExe, boolean relaunch) {
        try {
            processFor(prepare(installer, manifest, appExe, relaunch))
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (IOException error) {
            throw new UpdateException("Não foi possível preparar a instalação da actualização.", error);
        }
    }
}
