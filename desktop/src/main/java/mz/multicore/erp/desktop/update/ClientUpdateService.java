package mz.multicore.erp.desktop.update;

import mz.multicore.erp.architecture.version.ClientVersion;
import mz.multicore.erp.desktop.config.DesktopApiConfig;
import mz.multicore.erp.desktop.config.DesktopLocalSettings;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/**
 * One-click update of the installed desktop (docs/ACTUALIZACAO_UM_CLIQUE_SPEC.md): manifest, eligibility,
 * download, verification and the hand-over to the helper that installs after the ERP closes.
 */
public final class ClientUpdateService {

    private final DesktopApiConfig config;
    private final UpdateDownloader downloader;
    private final InstallerVerifier verifier;
    private final Path updatesDir;

    public ClientUpdateService(DesktopApiConfig config) {
        this(config, new UpdateDownloader(), InstallerVerifier.standard(),
                DesktopLocalSettings.localDataDir().resolve("updates"));
    }

    ClientUpdateService(DesktopApiConfig config, UpdateDownloader downloader, InstallerVerifier verifier,
                        Path updatesDir) {
        this.config = config;
        this.downloader = downloader;
        this.verifier = verifier;
        this.updatesDir = updatesDir;
    }

    /** The launcher of the installed program; empty when running from the jar (development). */
    public static Optional<Path> installedAppExe() {
        String path = System.getProperty("jpackage.app-path");
        if (path == null || path.isBlank()) return Optional.empty();
        Path exe = Path.of(path);
        return Files.isRegularFile(exe) ? Optional.of(exe) : Optional.empty();
    }

    public static String installedVersion() {
        return ClientVersion.current();
    }

    public UpdateManifest fetchManifest() {
        return downloader.fetchManifest(URI.create(config.manifestUrl()));
    }

    /** Downloads to a {@code .part} file, verifies it, and only then gives it its final name (§4.3). */
    public Path downloadAndVerify(UpdateManifest manifest, LongConsumer progress, BooleanSupplier cancelled) {
        Path part = updatesDir.resolve("Multicore-" + manifest.version() + ".exe.part");
        Path ready = updatesDir.resolve("Multicore-" + manifest.version() + ".exe");
        downloader.download(URI.create(config.installerUrl()), part, manifest.size(), progress, cancelled);
        verifier.verify(part, manifest);
        try {
            Files.move(part, ready, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException error) {
            InstallerVerifier.deleteQuietly(part);
            throw new UpdateException("Não foi possível guardar o instalador verificado.", error);
        }
        return ready;
    }

    /** Hands the verified installer to the helper. {@code relaunch} = "Reiniciar e instalar agora". */
    public void scheduleInstall(Path installer, UpdateManifest manifest, boolean relaunch) {
        Path appExe = installedAppExe()
                .orElseThrow(() -> new UpdateException("A actualização automática só funciona no programa instalado."));
        new UpdateInstallerLauncher(updatesDir).start(installer, manifest, appExe, relaunch);
    }
}
