package mz.multicore.erp.architecture.config;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Drops stale enum CHECK constraints from a file-based H2 database.
 *
 * <p>In dev the schema is managed by {@code ddl-auto=update}. Hibernate creates a
 * {@code CHECK (col IN (...))} for every {@code @Enumerated(STRING)} column when the table is first
 * created and never refreshes it, so enum values added later (e.g. {@code TransferStatus.IN_TRANSIT})
 * fail with a data integrity violation on older local databases. Production uses PostgreSQL with
 * Flyway and is not touched: this runs only for {@code jdbc:h2:file:} URLs, and only drops a
 * constraint whose value list is missing at least one constant of the mapped enum.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class H2EnumCheckConstraintRepair implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(H2EnumCheckConstraintRepair.class);
    private static final Pattern IN_LIST = Pattern.compile("^\\s*\"(\\w+)\"\\s+IN\\s*\\((.*)\\)\\s*$", Pattern.DOTALL);
    private static final Pattern QUOTED = Pattern.compile("'([^']*)'");

    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;

    public H2EnumCheckConstraintRepair(DataSource dataSource, EntityManagerFactory entityManagerFactory) {
        this.dataSource = dataSource;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isFileBasedH2()) {
            return;
        }
        try {
            List<String> dropped = repair();
            if (!dropped.isEmpty()) {
                log.warn("Removed {} stale enum CHECK constraint(s) from the local H2 database: {}", dropped.size(), dropped);
            }
        } catch (RuntimeException e) {
            log.warn("Could not verify enum CHECK constraints on the local H2 database: {}", e.getMessage());
        }
    }

    /** Drops every stale enum CHECK and returns them as {@code TABLE.COLUMN}. Package-private for tests. */
    List<String> repair() {
        Map<String, Set<String>> enumColumns = mappedEnumColumns();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<Map<String, Object>> checks = jdbc.queryForList("""
                SELECT t.TABLE_NAME, c.CONSTRAINT_NAME, c.CHECK_CLAUSE
                FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS c
                JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS t
                  ON t.CONSTRAINT_SCHEMA = c.CONSTRAINT_SCHEMA AND t.CONSTRAINT_NAME = c.CONSTRAINT_NAME
                WHERE t.TABLE_SCHEMA = SCHEMA()""");
        List<String> dropped = new ArrayList<>();
        for (Map<String, Object> row : checks) {
            String table = String.valueOf(row.get("TABLE_NAME"));
            Matcher m = IN_LIST.matcher(String.valueOf(row.get("CHECK_CLAUSE")));
            if (!m.matches()) {
                continue;
            }
            String column = m.group(1);
            Set<String> expected = enumColumns.get(key(table, column));
            if (expected == null) {
                continue;
            }
            Set<String> allowed = new LinkedHashSet<>();
            Matcher q = QUOTED.matcher(m.group(2));
            while (q.find()) {
                allowed.add(q.group(1));
            }
            if (!allowed.containsAll(expected)) {
                jdbc.execute("ALTER TABLE \"" + table + "\" DROP CONSTRAINT \"" + row.get("CONSTRAINT_NAME") + "\"");
                dropped.add(table + "." + column);
            }
        }
        return dropped;
    }

    private Map<String, Set<String>> mappedEnumColumns() {
        Map<String, Set<String>> result = new HashMap<>();
        entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getRuntimeMetamodels().getMappingMetamodel()
                .forEachEntityDescriptor(persister -> persister.forEachAttributeMapping(attribute -> {
                    if (!(attribute instanceof BasicValuedModelPart basic)) {
                        return;
                    }
                    Class<?> type = basic.getJavaType().getJavaTypeClass();
                    if (!type.isEnum()) {
                        return;
                    }
                    Set<String> names = new LinkedHashSet<>();
                    Arrays.stream(type.getEnumConstants()).forEach(c -> names.add(((Enum<?>) c).name()));
                    result.put(key(basic.getContainingTableExpression(), basic.getSelectionExpression()), names);
                }));
        return result;
    }

    private static String key(String table, String column) {
        return (table + "." + column).replace("\"", "").toUpperCase(Locale.ROOT);
    }

    private boolean isFileBasedH2() {
        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            return url != null && url.startsWith("jdbc:h2:file:");
        } catch (SQLException e) {
            return false;
        }
    }
}
