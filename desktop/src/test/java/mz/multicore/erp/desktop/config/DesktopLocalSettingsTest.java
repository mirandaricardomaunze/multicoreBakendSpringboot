package mz.multicore.erp.desktop.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesktopLocalSettingsTest {

    @TempDir Path tempDir;

    @Test
    void readBaseUrl_existingExternalFile_returnsPersistedValue() throws Exception {
        Path file = tempDir.resolve("desktop.properties");
        Files.writeString(file, "desktop.api.base-url=https://erp.exemplo.co.mz\n");

        assertThat(DesktopLocalSettings.readBaseUrl(file)).isEqualTo("https://erp.exemplo.co.mz");
    }

    @Test
    void readBaseUrl_missingFile_returnsNull() {
        assertThat(DesktopLocalSettings.readBaseUrl(tempDir.resolve("missing.properties"))).isNull();
    }

    @Test
    void settingsFile_livesOutsideInstalledApplication() {
        Path file = DesktopLocalSettings.settingsFile();
        // %LOCALAPPDATA%\Multicore is the jpackage install folder; an upgrade deletes it (found live, AC-59).
        assertThat(file.getParent().getFileName().toString()).isEqualTo("MulticoreERP");
        assertThat(file.getFileName().toString()).isEqualTo("desktop.properties");
    }

    @Test
    void migrateLegacyFile_copiesOldFileOnce() throws Exception {
        Path legacy = tempDir.resolve("Multicore/desktop.properties");
        Path current = tempDir.resolve("MulticoreERP/desktop.properties");
        Files.createDirectories(legacy.getParent());
        Files.writeString(legacy, "desktop.api.base-url=https://erp.exemplo.co.mz\n");

        DesktopLocalSettings.migrateLegacyFile(legacy, current);

        assertThat(DesktopLocalSettings.readBaseUrl(current)).isEqualTo("https://erp.exemplo.co.mz");
    }

    @Test
    void migrateLegacyFile_neverOverwritesCurrentFile() throws Exception {
        Path legacy = tempDir.resolve("Multicore/desktop.properties");
        Path current = tempDir.resolve("MulticoreERP/desktop.properties");
        Files.createDirectories(legacy.getParent());
        Files.createDirectories(current.getParent());
        Files.writeString(legacy, "desktop.api.base-url=http://antigo\n");
        Files.writeString(current, "desktop.api.base-url=https://actual\n");

        DesktopLocalSettings.migrateLegacyFile(legacy, current);

        assertThat(DesktopLocalSettings.readBaseUrl(current)).isEqualTo("https://actual");
    }

    @Test
    void migrateLegacyFile_withoutLegacyFile_createsNothing() {
        Path current = tempDir.resolve("MulticoreERP/desktop.properties");

        DesktopLocalSettings.migrateLegacyFile(tempDir.resolve("Multicore/desktop.properties"), current);

        assertThat(current).doesNotExist();
    }
}
