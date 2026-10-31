package mz.multicore.erp.modules.support.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.support.dto.CreateTicketRequest;
import mz.multicore.erp.modules.support.dto.SupportMessageDTO;
import mz.multicore.erp.modules.support.dto.SupportTicketDTO;
import mz.multicore.erp.modules.support.model.SupportTicket;
import mz.multicore.erp.modules.support.model.TicketPriority;
import mz.multicore.erp.modules.support.model.TicketStatus;
import mz.multicore.erp.modules.support.repository.PlatformSupportTicketRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class CompanySupportTicketService {

    private final PlatformSupportTicketRepository ticketRepository;
    private final PlatformSupportAdminService platformAdminService;

    public CompanySupportTicketService(PlatformSupportTicketRepository ticketRepository,
                                       PlatformSupportAdminService platformAdminService) {
        this.ticketRepository = ticketRepository;
        this.platformAdminService = platformAdminService;
    }

    public SupportTicketDTO openTicket(CreateTicketRequest request) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        if (request.subject() == null || request.subject().isBlank()) {
            throw new BusinessRuleException("O assunto é obrigatório.");
        }

        SupportTicket ticket = new SupportTicket();
        ticket.setCompanyId(companyId);
        ticket.setSubject(request.subject().trim());
        ticket.setDescription(request.description());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(parsePriority(request.priority()));
        ticket.setCreatedBy(CurrentUserContext.getUsername());
        ticket = ticketRepository.save(ticket);

        if (request.description() != null && !request.description().isBlank()) {
            platformAdminService.saveMessage(ticket.getId(), CurrentUserContext.getUsername(), false, request.description());
        }
        return platformAdminService.toDto(ticket, false);
    }

    public List<SupportTicketDTO> listCompanyTickets() {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        return ticketRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(t -> platformAdminService.toDto(t, false))
                .toList();
    }

    public List<SupportMessageDTO> listCompanyMessages(Long ticketId) {
        SupportTicket ticket = platformAdminService.requireTicket(ticketId);
        requireSameCompany(ticket);
        return platformAdminService.messages(ticketId);
    }

    public void addCompanyMessage(Long ticketId, String body) {
        SupportTicket ticket = platformAdminService.requireTicket(ticketId);
        requireSameCompany(ticket);
        platformAdminService.requireBody(body);
        if (ticket.getStatus().isTerminal()) {
            throw new BusinessRuleException("O pedido está fechado.");
        }
        platformAdminService.saveMessage(ticketId, CurrentUserContext.getUsername(), false, body);
        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }
    }

    private void requireSameCompany(SupportTicket ticket) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        if (!companyId.equals(ticket.getCompanyId())) {
            throw new BusinessRuleException("O pedido pertence a outra empresa.");
        }
    }

    private TicketPriority parsePriority(String value) {
        if (value == null || value.isBlank()) return TicketPriority.NORMAL;
        try {
            return TicketPriority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Prioridade inválida.");
        }
    }
}
