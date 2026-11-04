package mz.multicore.erp.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** O docker-compose.yml tem de ser YAML válido: um erro de sintaxe só aparecia no VPS. */
class DeployComposeHarnessTest {

    @Test
    @DisplayName("DC-01: docker-compose.yml é YAML válido e só o Caddy publica portas")
    @SuppressWarnings("unchecked")
    void composeParsesAndOnlyCaddyIsPublic() throws Exception {
        Map<String, Object> compose = new Yaml().load(Files.readString(Path.of("../docker-compose.yml")));
        Map<String, Map<String, Object>> services = (Map<String, Map<String, Object>>) compose.get("services");

        assertThat(services).containsOnlyKeys("db", "backend", "caddy");
        assertThat(services.get("db")).doesNotContainKey("ports");
        assertThat(services.get("backend")).doesNotContainKey("ports");
        assertThat(services.get("caddy")).containsKey("ports");
    }

    @Test
    @DisplayName("DC-02: versão mínima do desktop chega ao backend e o instalador é servido em /downloads")
    @SuppressWarnings("unchecked")
    void clientUpdatesAreWired() throws Exception {
        Map<String, Object> compose = new Yaml().load(Files.readString(Path.of("../docker-compose.yml")));
        Map<String, Map<String, Object>> services = (Map<String, Map<String, Object>>) compose.get("services");

        assertThat((Map<String, Object>) services.get("backend").get("environment")).containsKey("APP_CLIENT_MIN_VERSION");
        assertThat((java.util.List<String>) services.get("caddy").get("volumes"))
                .contains("./data/downloads:/srv/downloads:ro");
        assertThat(Files.readString(Path.of("../deployment/Caddyfile"))).contains("handle /downloads/*", "file_server");
        assertThat(Files.readString(Path.of("../scripts/publicar-instalador.sh"))).contains("Multicore-instalador.exe");
    }
}
