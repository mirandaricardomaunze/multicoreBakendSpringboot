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
        assertThat(DesktopLocalSettings.settingsFile().toString())
                .contains("Multicore")
                .endsWith("desktop.properties");
    }
}
