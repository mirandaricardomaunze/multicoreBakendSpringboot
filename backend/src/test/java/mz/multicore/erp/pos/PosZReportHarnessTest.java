package mz.multicore.erp.pos;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.dto.PosSessionSummaryDTO;
import mz.multicore.erp.modules.pos.model.TillMovement;
import mz.multicore.erp.modules.pos.model.TillMovementType;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import mz.multicore.erp.modules.pos.service.POSService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PosZReportHarnessTest {

    @Autowired
    private POSService posService;

    @Autowired
    private TillSessionRepository tillSessionRepository;

    @Autowired
    private TillMovementRepository tillMovementRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Company testCompany;

    @BeforeEach
    void setUp() {
        testCompany = new Company();
        testCompany.setName("Empresa Teste Z Report");
        testCompany.setTaxId("111222333");
        testCompany = companyRepository.save(testCompany);

        CurrentUserContext.setCurrentCompanyId(testCompany.getId());
        CurrentUserContext.setCurrentUser("operador", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void testZReportReconciliationExpectedCash() {
        // Z-01: Opening 100, cash sale 500, suprimento 50, sangria 30, refund 20 -> expected = 600
        TillSession session = posService.openSession("operador", BigDecimal.valueOf(100), testCompany.getId());

        addMovement(session, TillMovementType.SALE, BigDecimal.valueOf(500), "Venda cash");
        addMovement(session, TillMovementType.SUPRIMENTO, BigDecimal.valueOf(50), "Suprimento");
        addMovement(session, TillMovementType.SANGRIA, BigDecimal.valueOf(30), "Sangria");
        addMovement(session, TillMovementType.REFUND, BigDecimal.valueOf(20), "Devolução");

        PosZReportDTO z = posService.buildZReport(session.getId());

        assertEquals(0, BigDecimal.valueOf(100).compareTo(z.openingBalance()));
        assertEquals(0, BigDecimal.valueOf(500).compareTo(z.cashSales()));
        assertEquals(0, BigDecimal.valueOf(50).compareTo(z.suprimentos()));
        assertEquals(0, BigDecimal.valueOf(30).compareTo(z.sangrias()));
        assertEquals(0, BigDecimal.valueOf(20).compareTo(z.refunds()));
        assertEquals(0, BigDecimal.valueOf(600).compareTo(z.expectedCash()));
        assertNull(z.countedCash());
        assertNull(z.difference());
    }

    @Test
    void testZReportClosedSessionWithDifference() {
        // Z-02: Closing session with counted 590 and expected 600 -> diff = -10
        TillSession session = posService.openSession("operador", BigDecimal.valueOf(100), testCompany.getId());
        addMovement(session, TillMovementType.SALE, BigDecimal.valueOf(500), "Venda cash");

        TillSession closed = posService.closeSession(session.getId(), BigDecimal.valueOf(590));

        PosZReportDTO z = posService.buildZReport(closed.getId());

        assertEquals(0, BigDecimal.valueOf(600).compareTo(z.expectedCash()));
        assertEquals(0, BigDecimal.valueOf(590).compareTo(z.countedCash()));
        assertNotNull(z.difference());
        assertEquals(0, BigDecimal.valueOf(-10).compareTo(z.difference()));
        assertEquals("CLOSED", z.status());
    }

    @Test
    void testSessionsHistoryQuery() {
        TillSession s1 = posService.openSession("operador", BigDecimal.valueOf(200), testCompany.getId());
        addMovement(s1, TillMovementType.SALE, BigDecimal.valueOf(300), "Venda 1");
        posService.closeSession(s1.getId(), BigDecimal.valueOf(500));

        List<PosSessionSummaryDTO> history = posService.getSessionsHistory(testCompany.getId(), null, null);
        assertFalse(history.isEmpty());
        assertTrue(history.stream().anyMatch(h -> h.sessionId().equals(s1.getId())));
    }

    private void addMovement(TillSession session, TillMovementType type, BigDecimal amount, String desc) {
        TillMovement m = new TillMovement();
        m.setTillSession(session);
        m.setMovementType(type);
        m.setAmount(amount);
        m.setDescription(desc);
        m.setMovementDate(LocalDateTime.now());
        m.setCreatedBy("operador");
        tillMovementRepository.save(m);
    }
}
