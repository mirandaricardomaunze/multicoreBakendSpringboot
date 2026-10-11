package mz.multicore.erp.modules.pos.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness que impede duplo fecho e duplo depósito concorrente de uma sessão POS. */
class PosSessionConcurrencyHarnessTest {

    private static final Path ROOT = Files.isDirectory(Path.of("backend"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();

    @Test
    void closeSessionLocksTenantSessionBeforeStatusAndTreasuryDeposit() throws Exception {
        String repository = read("backend/src/main/java/mz/multicore/erp/modules/pos/repository/TillSessionRepository.java");
        assertThat(repository).contains(
                "@Lock(LockModeType.PESSIMISTIC_WRITE)",
                "Optional<TillSession> lockByIdAndCompanyId",
                "s.id = :id and s.company.id = :companyId");

        String service = read("backend/src/main/java/mz/multicore/erp/modules/pos/service/POSService.java");
        int method = service.indexOf("public TillSession closeSession(Long sessionId, BigDecimal closingBalanceReal, Long depositAccountId, String notes");
        int lock = service.indexOf("tillSessionRepository.lockByIdAndCompanyId", method);
        int statusCheck = service.indexOf("!\"OPEN\".equals(session.getStatus())", lock);
        int deposit = service.indexOf("financeService.registerTransaction", statusCheck);
        assertThat(method).isGreaterThanOrEqualTo(0);
        assertThat(lock).isGreaterThan(method);
        assertThat(statusCheck).isGreaterThan(lock);
        assertThat(deposit).isGreaterThan(statusCheck);
    }

    @Test
    void openSessionUsesDatabaseMutexBeforeCheckingForAnExistingSession() throws Exception {
        String companyRepository = read("backend/src/main/java/mz/multicore/erp/modules/company/repository/CompanyRepository.java");
        assertThat(companyRepository).contains(
                "@Lock(LockModeType.PESSIMISTIC_WRITE)",
                "Optional<Company> lockById");

        String service = read("backend/src/main/java/mz/multicore/erp/modules/pos/service/POSService.java");
        int method = service.indexOf("public TillSession openSession");
        int companyLock = service.indexOf("companyRepository.lockById", method);
        int activeCheck = service.indexOf("findActiveSessionForOperator", companyLock);
        int save = service.indexOf("tillSessionRepository.save(session)", activeCheck);
        assertThat(companyLock).isGreaterThan(method);
        assertThat(activeCheck).isGreaterThan(companyLock);
        assertThat(save).isGreaterThan(activeCheck);
    }

    private String read(String relativePath) throws Exception {
        return Files.readString(ROOT.resolve(relativePath));
    }
}
