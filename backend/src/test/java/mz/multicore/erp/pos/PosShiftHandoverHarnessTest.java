package mz.multicore.erp.pos;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.dto.ShiftHandoverRequest;
import mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO;
import mz.multicore.erp.modules.pos.model.TillMovement;
import mz.multicore.erp.modules.pos.model.TillMovementType;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import mz.multicore.erp.modules.pos.service.POSService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PosShiftHandoverHarnessTest {

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
        testCompany.setName("Empresa Teste Shift Handover");
        testCompany.setTaxId("999888777");
        testCompany = companyRepository.save(testCompany);

        CurrentUserContext.setCurrentCompanyId(testCompany.getId());
        CurrentUserContext.setCurrentUser("operadorA", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("SH-01: Passagem de turno com contagem exacta actualiza operador actual e mantém sessão OPEN")
    void testSuccessfulShiftHandoverExactAmount() {
        TillSession session = posService.openSession("operadorA", BigDecimal.valueOf(200), testCompany.getId());
        addMovement(session, TillMovementType.SALE, BigDecimal.valueOf(800), "Venda cash turno A");

        ShiftHandoverRequest request = new ShiftHandoverRequest(
                session.getId(),
                "operadorA",
                "operadorB",
                BigDecimal.valueOf(1000),
                "[{\"denomination\":500,\"count\":2,\"subtotal\":1000.00}]",
                "Troca de turno normal sem ocorrências"
        );

        ShiftReconciliationDTO result = posService.performShiftHandover(request);

        assertNotNull(result);
        assertEquals("operadorA", result.outgoingOperator());
        assertEquals("operadorB", result.incomingOperator());
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(result.expectedCash()));
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(result.countedCash()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.difference()));

        // Verificar que a sessão permanece OPEN e o operador actual é operadorB
        TillSession updated = tillSessionRepository.findById(session.getId()).orElseThrow();
        assertEquals("OPEN", updated.getStatus());
        assertEquals("operadorA", updated.getOperator());
        assertEquals("operadorB", updated.getCurrentOperator());

        // Operador B agora encontra a sessão via getActiveSession
        Optional<TillSession> activeForB = posService.getActiveSession("operadorB", testCompany.getId());
        assertTrue(activeForB.isPresent());
        assertEquals(session.getId(), activeForB.get().getId());

        // Operador A já não tem a sessão activa como operador actual
        Optional<TillSession> activeForA = posService.getActiveSession("operadorA", testCompany.getId());
        assertFalse(activeForA.isPresent());

        // Z-Report inclui o registo de reconciliação de turno
        PosZReportDTO z = posService.buildZReport(session.getId());
        assertEquals("operadorB", z.currentOperator());
        assertEquals(1, z.shiftReconciliations().size());
        assertEquals("operadorA", z.shiftReconciliations().get(0).outgoingOperator());
        assertEquals("operadorB", z.shiftReconciliations().get(0).incomingOperator());
    }

    @Test
    @DisplayName("SH-02: Passagem de turno com divergência de caixa permitida a perfil ADMIN/MANAGER")
    void testShiftHandoverWithCashDifference() {
        TillSession session = posService.openSession("operadorA", BigDecimal.valueOf(100), testCompany.getId());
        addMovement(session, TillMovementType.SALE, BigDecimal.valueOf(400), "Venda cash");

        ShiftHandoverRequest request = new ShiftHandoverRequest(
                session.getId(),
                "operadorA",
                "operadorB",
                BigDecimal.valueOf(480),
                null,
                "Quebra de 20 MT na gaveta"
        );

        ShiftReconciliationDTO result = posService.performShiftHandover(request);

        assertEquals(0, BigDecimal.valueOf(500).compareTo(result.expectedCash()));
        assertEquals(0, BigDecimal.valueOf(480).compareTo(result.countedCash()));
        assertEquals(0, BigDecimal.valueOf(-20).compareTo(result.difference()));
        assertEquals("Quebra de 20 MT na gaveta", result.notes());
    }

    @Test
    @DisplayName("SH-03: Passagem de turno valida operadores e estado da sessão")
    void testShiftHandoverValidations() {
        TillSession session = posService.openSession("operadorA", BigDecimal.valueOf(100), testCompany.getId());

        // 1. Mesmo operador
        assertThrows(BusinessRuleException.class, () -> posService.performShiftHandover(new ShiftHandoverRequest(
                session.getId(), "operadorA", "operadorA", BigDecimal.valueOf(100), null, null
        )));

        // 2. Operador de saída errado
        assertThrows(BusinessRuleException.class, () -> posService.performShiftHandover(new ShiftHandoverRequest(
                session.getId(), "operadorErrado", "operadorB", BigDecimal.valueOf(100), null, null
        )));

        // 3. Operador de entrada vazio
        assertThrows(BusinessRuleException.class, () -> posService.performShiftHandover(new ShiftHandoverRequest(
                session.getId(), "operadorA", "   ", BigDecimal.valueOf(100), null, null
        )));

        // 4. Sessão fechada
        posService.closeSession(session.getId(), BigDecimal.valueOf(100));
        assertThrows(BusinessRuleException.class, () -> posService.performShiftHandover(new ShiftHandoverRequest(
                session.getId(), "operadorA", "operadorB", BigDecimal.valueOf(100), null, null
        )));
    }

    @Test
    @DisplayName("SH-04: Múltiplas passagens de turno consecutivas numa sessão diária")
    void testMultipleConsecutiveHandovers() {
        TillSession session = posService.openSession("operador1", BigDecimal.valueOf(100), testCompany.getId());

        // Turno 1 -> 2
        addMovement(session, TillMovementType.SALE, BigDecimal.valueOf(200), "Vendas turno 1");
        posService.performShiftHandover(new ShiftHandoverRequest(
                session.getId(), "operador1", "operador2", BigDecimal.valueOf(300), null, "T1->T2"
        ));

        // Turno 2 -> 3
        CurrentUserContext.setCurrentUser("operador2", "ADMIN");
        addMovement(session, TillMovementType.SALE, BigDecimal.valueOf(400), "Vendas turno 2");
        posService.performShiftHandover(new ShiftHandoverRequest(
                session.getId(), "operador2", "operador3", BigDecimal.valueOf(700), null, "T2->T3"
        ));

        List<ShiftReconciliationDTO> recons = posService.getShiftReconciliations(session.getId());
        assertEquals(2, recons.size());
        assertEquals("operador1", recons.get(0).outgoingOperator());
        assertEquals("operador2", recons.get(0).incomingOperator());
        assertEquals("operador2", recons.get(1).outgoingOperator());
        assertEquals("operador3", recons.get(1).incomingOperator());

        TillSession current = tillSessionRepository.findById(session.getId()).orElseThrow();
        assertEquals("operador3", current.getCurrentOperator());
    }

    private void addMovement(TillSession session, TillMovementType type, BigDecimal amount, String desc) {
        TillMovement m = new TillMovement();
        m.setTillSession(session);
        m.setMovementType(type);
        m.setAmount(amount);
        m.setDescription(desc);
        m.setMovementDate(LocalDateTime.now());
        tillMovementRepository.save(m);
    }
}
