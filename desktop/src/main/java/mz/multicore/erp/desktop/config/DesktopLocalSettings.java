package mz.multicore.erp.desktop.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Configuração persistente fora da pasta instalada, preservada entre actualizações. */
public final class DesktopLocalSettings {

    static final String API_BASE_URL = "desktop.api.base-url";
    static final String POS_SCALE_ENABLED = "desktop.pos.scale-enabled";

    private DesktopLocalSettings() {}

    public static Path settingsFile() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = localAppData == null || localAppData.isBlank()
                ? Path.of(System.getProperty("user.home"), "AppData", "Local")
                : Path.of(localAppData);
        return base.resolve("Multicore").resolve("desktop.properties");
    }

    static String readBaseUrl(Path file) {
        return read(file, API_BASE_URL);
    }

    /** A balança é opcional e fica oculta nos postos sem esta opção. */
    public static boolean posScaleEnabled() {
        return readPosScaleEnabled(settingsFile());
    }

    static boolean readPosScaleEnabled(Path file) {
        return Boolean.parseBoolean(read(file, POS_SCALE_ENABLED));
    }

    private static String read(Path file, String key) {
        if (file == null || !Files.isRegularFile(file)) return null;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            return properties.getProperty(key);
        } catch (IOException error) {
            throw new IllegalStateException("Não foi possível ler a configuração local do Multicore.", error);
        }
    }
}
