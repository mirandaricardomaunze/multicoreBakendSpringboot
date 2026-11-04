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
}
