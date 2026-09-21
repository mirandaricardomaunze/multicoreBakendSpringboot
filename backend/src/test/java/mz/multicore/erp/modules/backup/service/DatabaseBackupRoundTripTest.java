package mz.multicore.erp.modules.backup.service;

import mz.multicore.erp.MulticoreApplication;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "MULTICORE_RESTORE_TEST_PORT", matches = "[0-9]+")
class DatabaseBackupRoundTripTest {
    private final Path runDir = Path.of(System.getenv("MULTICORE_RESTORE_TEST_DIR"));
    private final String baseUrl = "jdbc:postgresql://127.0.0.1:"
            + System.getenv("MULTICORE_RESTORE_TEST_PORT") + "/";

    @Test
    void backupRestore_postgresIsolado_preservaDadosSchemaSequenciasELogin() throws Exception {
        // Recusa uma instancia que nao seja exactamente a criada pelo guiao de validacao.
        try (var connection = connect("postgres"); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SHOW data_directory")) {
                assertTrue(result.next());
                assertEquals(runDir.resolve("cluster").toRealPath(), Path.of(result.getString(1)).toRealPath());
            }
            statement.executeUpdate("CREATE DATABASE multicore_restore_source");
            statement.executeUpdate("CREATE DATABASE multicore_restore_target");
        }
        try (var application = startBackend("multicore_restore_source", true)) {
            assertTrue(application.isActive());
        }
        // Dados que um snapshot JSON parcial perderia: bytes, precisao, FK e sequencia avancada.
        try (var connection = connect("multicore_restore_source"); var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE restore_probe (id bigserial PRIMARY KEY, "
                    + "parent_id bigint REFERENCES restore_probe(id), amount numeric(18,3), payload bytea, label text)");
            statement.executeUpdate("INSERT INTO restore_probe(amount,payload,label) VALUES "
                    + "(123456.789,decode('0001ff80','hex'),'Recuperacao — Mocambique'), (0.001,NULL,'Filho')");
            statement.executeUpdate("UPDATE restore_probe SET parent_id=1 WHERE id=2");
            statement.execute("SELECT setval('restore_probe_id_seq', 50, true)");
        }
        Map<String, List<String>> before = snapshot("multicore_restore_source");
        assertFalse(before.get("table:products").isEmpty());
        assertFalse(before.get("table:stocks").isEmpty());
        String dump;
        CurrentUserContext.setCurrentUser("restore-test", "ADMIN");
        try {
            dump = service("multicore_restore_source").executePhysicalBackup().filePath();
            service("multicore_restore_target").restorePhysicalBackup(dump, true);
        } finally {
            CurrentUserContext.clear();
        }
        var restored = snapshot("multicore_restore_target");
        assertEquals(before.keySet(), restored.keySet());
        before.forEach((name, rows) -> assertTrue(rows.equals(restored.get(name)), "Diferenca em " + name));
        try (var connection = connect("multicore_restore_target"); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("INSERT INTO restore_probe(label) VALUES ('Depois') RETURNING id")) {
                assertTrue(result.next());
                assertEquals(51, result.getLong(1));
            }
            assertThrows(java.sql.SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO restore_probe(parent_id) VALUES (99999)"));
        }
        try (var application = startBackend("multicore_restore_target", false)) {
            int port = application.getWebServer().getPort();
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"maria\",\"password\":\"password\"}"))
                    .build();
            try (var client = HttpClient.newHttpClient()) {
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode(), "Login na base restaurada deve funcionar");
                var body = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body());
                assertFalse(body.path("token").asText().isBlank());
                assertFalse(body.path("companies").isEmpty());
            }
        }
        var evidence = new ArrayList<String>();
        evidence.add("Backup/restore PostgreSQL real: PASS");
        evidence.add("Dump: " + dump);
        evidence.add("Schema validado pelo Hibernate e historico Flyway preservado; login HTTP: 200.");
        evidence.add("Bytes, decimais, FK e proximo ID: PASS. Dados exclusivamente de demonstracao.");
        before.forEach((name, rows) -> evidence.add(name + ": " + rows.size() + " registos identicos"));
        Files.write(runDir.resolve("evidence.txt"), evidence);
    }

    private Connection connect(String database) throws Exception {
        return DriverManager.getConnection(baseUrl + database, "restore_test", "");
    }

    private DatabaseBackupService service(String database) {
        return new DatabaseBackupService(baseUrl + database, "restore_test", "",
                System.getenv("MULTICORE_RESTORE_TEST_PG_BIN"), runDir.toString());
    }

    private ServletWebServerApplicationContext startBackend(String database, boolean seed) {
        return (ServletWebServerApplicationContext) new SpringApplicationBuilder(MulticoreApplication.class)
                .run("--server.address=127.0.0.1", "--server.port=0", "--spring.main.headless=true",
                        "--spring.datasource.url=" + baseUrl + database, "--spring.datasource.username=restore_test",
                        "--spring.datasource.password=", "--spring.datasource.driver-class-name=org.postgresql.Driver",
                        "--spring.jpa.hibernate.ddl-auto=validate", "--spring.flyway.enabled=true",
                        "--spring.h2.console.enabled=false", "--backup.schedule.enabled=false",
                        "--app.seed-demo-data=" + seed, "--app.superadmin.password=",
                        "--logging.level.root=WARN", "--logging.level.org.springframework=WARN",
                        "--logging.level.org.hibernate=WARN", "--logging.level.org.hibernate.SQL=WARN");
    }

    private Map<String, List<String>> snapshot(String database) throws Exception {
        Map<String, List<String>> result = new TreeMap<>();
        try (var connection = connect(database); var statement = connection.createStatement()) {
            List<String> tables = new ArrayList<>();
            try (var rows = statement.executeQuery("SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename")) {
                while (rows.next()) tables.add(rows.getString(1));
            }
            for (String table : tables) {
                List<String> values = new ArrayList<>();
                String quoted = "\"" + table.replace("\"", "\"\"") + "\"";
                try (var rows = statement.executeQuery("SELECT row_to_json(t)::text FROM public." + quoted + " t ORDER BY 1")) {
                    while (rows.next()) values.add(rows.getString(1));
                }
                result.put("table:" + table, values);
            }
            for (String query : List.of(
                    "SELECT sequencename, last_value FROM pg_sequences WHERE schemaname='public' ORDER BY sequencename",
                    "SELECT conrelid::regclass::text, conname, contype, conkey, confrelid::regclass::text, "
                            + "confkey, confupdtype, confdeltype, confmatchtype, condeferrable, condeferred, convalidated "
                            + "FROM pg_constraint WHERE connamespace='public'::regnamespace ORDER BY 1,2")) {
                List<String> values = new ArrayList<>();
                try (var rows = statement.executeQuery(query)) {
                    while (rows.next()) {
                        List<String> columns = new ArrayList<>();
                        for (int i = 1; i <= rows.getMetaData().getColumnCount(); i++) columns.add(rows.getString(i));
                        values.add(columns.toString());
                    }
                }
                result.put(query, values);
            }
        }
        return result;
    }
}
