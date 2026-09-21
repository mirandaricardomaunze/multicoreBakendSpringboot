package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.EmailDispatchResultDTO;
import mz.multicore.erp.modules.comercial.dto.SendStatementEmailRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.printing.CustomerStatementPrintService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class CustomerStatementMailHarnessTest {

    private CustomerStatementPrintService printService;
    private ClientRepository clientRepository;
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    private CustomerStatementMailService mailService;

    @BeforeEach
    void setUp() {
        printService = Mockito.mock(CustomerStatementPrintService.class);
        clientRepository = Mockito.mock(ClientRepository.class);
        mailSenderProvider = Mockito.mock(ObjectProvider.class);

        mailService = new CustomerStatementMailService(printService, clientRepository, mailSenderProvider);
        CurrentUserContext.setCurrentCompanyId(1L);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void testRejectsNullOrBlankEmail() {
        // EEC-01: Rejection on missing or invalid email
        SendStatementEmailRequest reqNull = new SendStatementEmailRequest(10L, null, "Assunto", "Nota", null, null);
        assertThrows(BusinessRuleException.class, () -> mailService.sendStatementEmail(reqNull));

        SendStatementEmailRequest reqInvalid = new SendStatementEmailRequest(10L, "invalido-sem-arroba", "Assunto", "Nota", null, null);
        assertThrows(BusinessRuleException.class, () -> mailService.sendStatementEmail(reqInvalid));
    }

    @Test
    void testRejectsNullClient() {
        // EEC-01: Rejection on missing clientId
        SendStatementEmailRequest req = new SendStatementEmailRequest(null, "cliente@empresa.co.mz", "Assunto", "Nota", null, null);
        assertThrows(BusinessRuleException.class, () -> mailService.sendStatementEmail(req));
    }

    @Test
    void testSuccessfulDispatchSimulatedMode() {
        // EEC-04: Dispatch in simulated mode when mail sender is not configured
        Long clientId = 5L;
        Client client = new Client();
        client.setId(clientId);
        client.setName("Auto Mecânica do Sul, Lda");
        client.setEmail("financeiro@automecanica.co.mz");

        when(clientRepository.findByIdAndCompaniesId(eq(clientId), eq(1L))).thenReturn(Optional.of(client));
        when(printService.render(eq(1L), eq(clientId), any(), any())).thenReturn("%PDF-1.4 test document".getBytes());
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        SendStatementEmailRequest req = new SendStatementEmailRequest(
                clientId,
                "financeiro@automecanica.co.mz",
                "Extrato Mensal",
                "Segue o extrato para conferência",
                LocalDate.now().minusDays(30),
                LocalDate.now()
        );

        EmailDispatchResultDTO result = mailService.sendStatementEmail(req);

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals("financeiro@automecanica.co.mz", result.recipientEmail());
        assertNotNull(result.messageId());
        assertTrue(result.messageId().startsWith("SIM-"));
    }
}
