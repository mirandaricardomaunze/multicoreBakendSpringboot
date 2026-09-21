package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.Test;

import java.awt.Image;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppIconHarnessTest {

    @Test
    void appIconsAreAvailableAndMultiResolution() {
        List<Image> icons = UIHelper.getAppIcons();
        assertThat(icons).isNotEmpty();
        assertThat(icons.size()).isGreaterThanOrEqualTo(5);

        // Verifica se existem resoluções chave para o Windows
        boolean has16 = icons.stream().anyMatch(img -> img.getWidth(null) == 16);
        boolean has32 = icons.stream().anyMatch(img -> img.getWidth(null) == 32);
        boolean has64 = icons.stream().anyMatch(img -> img.getWidth(null) == 64);
        boolean has256 = icons.stream().anyMatch(img -> img.getWidth(null) == 256);

        assertThat(has16).as("Ícone 16x16 para título de janela").isTrue();
        assertThat(has32).as("Ícone 32x32 para barra de tarefas").isTrue();
        assertThat(has64).as("Ícone 64x64 para ecrã de autenticação").isTrue();
        assertThat(has256).as("Ícone 256x256 para Alt+Tab / Win+Tab").isTrue();
    }

    @Test
    void getAppIconScalesOrRetrievesExactDimension() {
        Image icon64 = UIHelper.getAppIcon(64);
        assertThat(icon64).isNotNull();
        assertThat(icon64.getWidth(null)).isEqualTo(64);
        assertThat(icon64.getHeight(null)).isEqualTo(64);

        Image icon48 = UIHelper.getAppIcon(48);
        assertThat(icon48).isNotNull();
        assertThat(icon48.getWidth(null)).isEqualTo(48);
        assertThat(icon48.getHeight(null)).isEqualTo(48);
    }

    @Test
    void windowsInstallerAndDesktopResourcesIncludeAppIcon() throws IOException {
        Path desktopIco = Path.of("src", "main", "resources", "icons", "app-icon.ico");
        assertThat(desktopIco).as("app-icon.ico em resources").exists();
        assertThat(Files.size(desktopIco)).isGreaterThan(10_000);

        Path installerIco = Path.of("..", "installer", "app-icon.ico");
        assertThat(installerIco).as("app-icon.ico no diretório do instalador").exists();
        assertThat(Files.size(installerIco)).isGreaterThan(10_000);

        Path installerScript = Path.of("..", "scripts", "build-windows-installer.ps1");
        assertThat(installerScript).exists();
        String scriptContent = Files.readString(installerScript);
        assertThat(scriptContent).contains("--icon $icon");
    }
}
