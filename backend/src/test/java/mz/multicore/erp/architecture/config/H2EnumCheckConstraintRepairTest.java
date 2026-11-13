package mz.multicore.erp.architecture.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An old local H2 database keeps the enum CHECK Hibernate created with the table, so a newer value such
 * as {@code IN_TRANSIT} is rejected. The repair drops only CHECKs missing enum constants.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:enumcheckrepair;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
class H2EnumCheckConstraintRepairTest {

    @Autowired private H2EnumCheckConstraintRepair repair;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void dropsOnlyTheStaleEnumCheck() {
        assertThat(repair.repair()).as("a freshly created schema has no stale CHECK").isEmpty();

        List<String> current = jdbc.queryForList("""
                SELECT c.CONSTRAINT_NAME FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS c
                JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS t ON t.CONSTRAINT_NAME = c.CONSTRAINT_NAME
                WHERE t.TABLE_NAME = 'STOCK_TRANSFERS' AND c.CHECK_CLAUSE LIKE '"STATUS"%'""", String.class);
        current.forEach(name -> jdbc.execute("ALTER TABLE STOCK_TRANSFERS DROP CONSTRAINT \"" + name + "\""));
        jdbc.execute("ALTER TABLE STOCK_TRANSFERS ADD CONSTRAINT STALE_TRANSFER_STATUS "
                + "CHECK (STATUS IN ('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED','CANCELLED'))");
        int before = countChecks();

        assertThat(repair.repair()).containsExactly("STOCK_TRANSFERS.STATUS");
        assertThat(countChecks()).isEqualTo(before - 1);
        assertThat(repair.repair()).isEmpty();
    }

    private int countChecks() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.CHECK_CONSTRAINTS", Integer.class);
    }
}
