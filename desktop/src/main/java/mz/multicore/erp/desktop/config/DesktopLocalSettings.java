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
    static final String SCALE_PORT = "desktop.scale.port";
    static final String SCALE_BAUD = "desktop.scale.baud";
    static final String SCALE_MODE = "desktop.scale.mode";

    /**
     * Serial scale of this workstation. An empty {@code port} means simulation. {@code mode}: CONTINUOUS (the scale
     * streams the weight), POLL_W (send "W") or POLL_ENQ (send ENQ, 0x05), as the scale manual says.
     */
    public record ScaleSettings(boolean enabled, String port, int baud, String mode) {
        public boolean hasPort() {
            return port != null && !port.isBlank();
        }
    }

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

    public static ScaleSettings scaleSettings() {
        return readScaleSettings(settingsFile());
    }

    static ScaleSettings readScaleSettings(Path file) {
        Properties p = load(file);
        int baud;
        try {
            baud = Integer.parseInt(p.getProperty(SCALE_BAUD, "9600").trim());
        } catch (NumberFormatException e) {
            baud = 9600;
        }
        return new ScaleSettings(Boolean.parseBoolean(p.getProperty(POS_SCALE_ENABLED)), p.getProperty(SCALE_PORT, ""),
                baud, p.getProperty(SCALE_MODE, "CONTINUOUS"));
    }

    /** Saves the scale settings and keeps every other key of the local file. */
    public static void saveScaleSettings(ScaleSettings settings) {
        writeScaleSettings(settingsFile(), settings);
    }

    static void writeScaleSettings(Path file, ScaleSettings settings) {
        Properties p = load(file);
        p.setProperty(POS_SCALE_ENABLED, String.valueOf(settings.enabled()));
        p.setProperty(SCALE_PORT, settings.port() == null ? "" : settings.port().trim());
        p.setProperty(SCALE_BAUD, String.valueOf(settings.baud()));
        p.setProperty(SCALE_MODE, settings.mode() == null ? "CONTINUOUS" : settings.mode());
        try {
            Files.createDirectories(file.getParent());
            try (java.io.OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "Multicore desktop");
            }
        } catch (IOException error) {
            throw new IllegalStateException("Não foi possível gravar a configuração local da balança.", error);
        }
    }

    private static Properties load(Path file) {
        Properties properties = new Properties();
        if (file == null || !Files.isRegularFile(file)) return properties;
        try (InputStream input = Files.newInputStream(file)) {
            properties.load(input);
            return properties;
        } catch (IOException error) {
            throw new IllegalStateException("Não foi possível ler a configuração local do Multicore.", error);
        }
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
