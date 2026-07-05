package com.phcpro.modules.support.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.support.dto.CreateTicketRequest;
import com.phcpro.modules.support.dto.SupportTicketDTO;
import com.phcpro.modules.support.model.SupportTicket;
import com.phcpro.modules.support.model.TicketStatus;
import com.phcpro.modules.support.repository.SupportMessageRepository;
import com.phcpro.modules.support.repository.PlatformSupportTicketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes do {@link SupportService}: abertura tenant-scoped, resposta do superadmin move OPEN→
 * IN_PROGRESS e assume, isolamento por empresa, e guards de papel.
 */
class SupportServiceTest {

    private PlatformSupportTicketRepository ticketRepository;
    private SupportMessageRepository messageRepository;
    private SupportService service;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(PlatformSupportTicketRepository.class);
        messageRepository = mock(SupportMessageRepository.class);
        service = new SupportService(ticketRepository, messageRepository, mock(CompanyRepository.class));
        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(i -> {
            SupportTicket t = i.getArgument(0);
            if (t.getId() == null) t.setId(100L);
            return t;
        });
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private SupportTicket ticket(Long id, Long companyId, TicketStatus status) {
        SupportTicket t = new SupportTicket();
        t.setId(id);
        t.setCompanyId(companyId);
        t.setSubject("Assunto");
        t.setStatus(status);
        return t;
    }

    @Test
    void openTicket_criaComEmpresaActiva() { // ST-01
        CurrentUserContext.setCurrentUser("ana", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(3L);
        SupportTicketDTO dto = service.openTicket(new CreateTicketRequest("Erro na impressora", "Detalhe", "HIGH"));
        assertEquals(3L, dto.companyId());
        assertEquals("OPEN", dto.status());
        assertEquals("Alta", dto.priorityLabel());
        verify(messageRepository).save(any()); // descrição vira 1ª mensagem
    }

    @Test
    void openTicket_semGestor_bloqueia() { // ST-02
        CurrentUserContext.setCurrentUser("maria", "EMPLOYEE");
        CurrentUserContext.setCurrentCompanyId(3L);
        assertThrows(BusinessRuleException.class, () ->
                service.openTicket(new CreateTicketRequest("X", null, null)));
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void addCompanyMessage_outraEmpresa_bloqueia() { // ST-03
        CurrentUserContext.setCurrentUser("ana", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(3L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket(1L, 999L, TicketStatus.OPEN)));
        assertThrows(BusinessRuleException.class, () -> service.addCompanyMessage(1L, "olá"));
    }

    @Test
    void superAdminReply_assumeEMoveParaEmCurso() { // ST-04
        CurrentUserContext.setCurrentUser("superadmin", "SUPERADMIN");
        SupportTicket t = ticket(1L, 3L, TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(t));
        SupportTicketDTO dto = service.addSuperAdminReply(1L, "Estamos a ver");
        assertEquals("IN_PROGRESS", dto.status());
        assertEquals("superadmin", t.getAssignee());
        verify(messageRepository).save(argThat(m -> m.isFromSuperAdmin()));
    }

    @Test
    void listAllTickets_semSuperadmin_bloqueia() { // ST-05
        CurrentUserContext.setCurrentUser("ana", "ADMIN");
        assertThrows(BusinessRuleException.class, () -> service.listAllTickets());
    }
}
