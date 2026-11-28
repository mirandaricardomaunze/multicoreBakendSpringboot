package mz.multicore.erp.desktop.update;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** AU-01. */
class UpdateManifestTest {

    static final String SHA = "a".repeat(64);

    @Test
    void parse_validManifest_readsAllFields() {
        UpdateManifest manifest = UpdateManifest.parse(
                "{\"version\":\"1.0.4\",\"sha256\":\"" + SHA.toUpperCase() + "\",\"size\":81234567}");

        assertThat(manifest.version()).isEqualTo("1.0.4");
        assertThat(manifest.sha256()).isEqualTo(SHA);
        assertThat(manifest.size()).isEqualTo(81234567L);
    }

    @Test
    void parse_invalidHash_isRefused() {
        assertThatThrownBy(() -> UpdateManifest.parse("{\"version\":\"1.0.4\",\"sha256\":\"abc\",\"size\":10}"))
                .isInstanceOf(UpdateException.class).hasMessageContaining("SHA-256");
    }

    @Test
    void parse_zeroOrHugeSize_isRefused() {
        assertThatThrownBy(() -> UpdateManifest.parse("{\"version\":\"1.0.4\",\"sha256\":\"" + SHA + "\",\"size\":0}"))
                .hasMessageContaining("tamanho");
        assertThatThrownBy(() -> UpdateManifest.parse(
                "{\"version\":\"1.0.4\",\"sha256\":\"" + SHA + "\",\"size\":" + (UpdateManifest.MAX_SIZE + 1) + "}"))
                .hasMessageContaining("tamanho");
    }

    @Test
    void parse_missingFieldsOrGarbage_isRefused() {
        assertThatThrownBy(() -> UpdateManifest.parse("{\"sha256\":\"" + SHA + "\",\"size\":10}"))
                .hasMessageContaining("versão");
        assertThatThrownBy(() -> UpdateManifest.parse("<html>404</html>")).hasMessageContaining("legível");
        assertThatThrownBy(() -> UpdateManifest.parse("{\"version\":\"1.0.4; rm\",\"sha256\":\"" + SHA + "\",\"size\":1}"))
                .hasMessageContaining("versão");
    }
}
