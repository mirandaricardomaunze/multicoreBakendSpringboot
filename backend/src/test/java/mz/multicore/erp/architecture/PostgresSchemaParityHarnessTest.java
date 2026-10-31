package mz.multicore.erp.architecture;

import mz.multicore.erp.MulticoreApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compara o schema nascido das migrações com o que as entidades exigem, em PostgreSQL real.
 * Corre através de scripts/verify-backup-restore.ps1. Ver POSTGRES_SCHEMA_PARITY_SPEC.
 */
@EnabledIfEnvironmentVariable(named = "MULTICORE_RESTORE_TEST_PORT", matches = "[0-9]+")
class PostgresSchemaParityHarnessTest {
    private static final String MIGRATED = "multicore_parity_migrated";
    private static final String ENTITIES = "multicore_parity_entities";
    /** NOT NULL só na BD: os serviços preenchem sempre estes campos. Ver SPEC §4. */
    private static final Set<String> NOT_NULL_ALLOWED = Set.of(
            "store_vouchers.created_by", "subscriptions.monthly_price");
    private static final Pattern CHECK_COLUMN = Pattern.compile("\\(+(\\w+)\\)::text");
    private static final Pattern CHECK_VALUE = Pattern.compile("'([A-Z_0-9]+)'");

    private final Path runDir = Path.of(System.getenv("MULTICORE_RESTORE_TEST_DIR"));
    private final String baseUrl = "jdbc:postgresql://127.0.0.1:"
            + System.getenv("MULTICORE_RESTORE_TEST_PORT") + "/";

    @Test
    void pg02a05_migracoesCorrespondemAsEntidades() throws Exception {
        // Recusa uma instancia que nao seja exactamente a criada pelo guiao de validacao.
        try (var connection = connect("postgres"); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SHOW data_directory")) {
                assertTrue(result.next());
                assertEquals(runDir.resolve("cluster").toRealPath(), Path.of(result.getString(1)).toRealPath());
            }
            statement.executeUpdate("CREATE DATABASE " + MIGRATED);
            statement.executeUpdate("CREATE DATABASE " + ENTITIES);
        }
        boot(MIGRATED, "validate", true);
        boot(ENTITIES, "create", false);

        Map<String, String[]> migrated = columns(MIGRATED);
        Map<String, String[]> entities = columns(ENTITIES);
        assertThat(entities).isNotEmpty();
        List<String> problems = new ArrayList<>();
        entities.forEach((column, expected) -> {
            String[] actual = migrated.get(column);
            if (actual == null) {
                problems.add("PG-02 coluna em falta: " + column);
            } else if (!actual[0].equals(expected[0])) {
                problems.add("PG-03 tipo de " + column + ": " + actual[0] + " em vez de " + expected[0]);
            } else if ("NO".equals(actual[1]) && "YES".equals(expected[1])
                    && !NOT_NULL_ALLOWED.contains(column)) {
                problems.add("PG-04 NOT NULL sem correspondencia na entidade: " + column);
            }
        });

        Map<String, Set<String>> migratedChecks = enumChecks(MIGRATED);
        Map<String, Set<String>> entityChecks = enumChecks(ENTITIES);
        assertThat(entityChecks).isNotEmpty();
        entityChecks.forEach((column, expected) -> {
            Set<String> actual = migratedChecks.get(column);
            if (actual != null && !actual.containsAll(expected)) {
                Set<String> missing = new TreeSet<>(expected);
                missing.removeAll(actual);
                problems.add("PG-05 CHECK de " + column + " rejeita " + missing);
            }
        });
        assertThat(problems).isEmpty();
    }

    private void boot(String database, String ddlAuto, boolean flyway) {
        try (var application = new SpringApplicationBuilder(MulticoreApplication.class).run(
                "--server.address=127.0.0.1", "--server.port=0", "--spring.main.headless=true",
                "--spring.datasource.url=" + baseUrl + database, "--spring.datasource.username=restore_test",
                "--spring.datasource.password=", "--spring.datasource.driver-class-name=org.postgresql.Driver",
                "--spring.jpa.hibernate.ddl-auto=" + ddlAuto, "--spring.flyway.enabled=" + flyway,
                "--spring.h2.console.enabled=false", "--backup.schedule.enabled=false",
                "--app.seed-demo-data=false", "--app.superadmin.password=",
                "--logging.level.root=WARN", "--logging.level.org.hibernate.SQL=WARN")) {
            assertTrue(application.isActive());
        }
    }

    private Map<String, String[]> columns(String database) throws Exception {
        Map<String, String[]> result = new TreeMap<>();
        try (var connection = connect(database); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT table_name, column_name, data_type, is_nullable "
                     + "FROM information_schema.columns WHERE table_schema='public'")) {
            while (rows.next()) {
                result.put(rows.getString(1) + "." + rows.getString(2),
                        new String[]{rows.getString(3), rows.getString(4)});
            }
        }
        return result;
    }

    private Map<String, Set<String>> enumChecks(String database) throws Exception {
        Map<String, Set<String>> result = new TreeMap<>();
        try (var connection = connect(database); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT conrelid::regclass::text, pg_get_constraintdef(oid) "
                     + "FROM pg_constraint WHERE contype='c' AND connamespace='public'::regnamespace")) {
            while (rows.next()) {
                String definition = rows.getString(2);
                Matcher column = CHECK_COLUMN.matcher(definition);
                if (!column.find()) continue;
                Set<String> values = new TreeSet<>();
                Matcher value = CHECK_VALUE.matcher(definition);
                while (value.find()) values.add(value.group(1));
                result.put(rows.getString(1) + "." + column.group(1), values);
            }
        }
        return result;
    }

    private Connection connect(String database) throws Exception {
        return DriverManager.getConnection(baseUrl + database, "restore_test", "");
    }
}
