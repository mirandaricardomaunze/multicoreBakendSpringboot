package mz.multicore.erp.desktop.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Configuração persistente fora da pasta instalada, preservada entre actualizações. */
public final class DesktopLocalSettings {

    static final String API_BASE_URL = "desktop.api.base-url";

    private DesktopLocalSettings() {}

    public static Path settingsFile() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = localAppData == null || localAppData.isBlank()
                ? Path.of(System.getProperty("user.home"), "AppData", "Local")
                : Path.of(localAppData);
        return base.resolve("Multicore").resolve("desktop.properties");
    }

    static String readBaseUrl(Path file) {
        if (file == null || !Files.isRegularFile(file)) return null;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            return properties.getProperty(API_BASE_URL);
        } catch (IOException error) {
            throw new IllegalStateException("Não foi possível ler a configuração local do Multicore.", error);
        }
    }
}
