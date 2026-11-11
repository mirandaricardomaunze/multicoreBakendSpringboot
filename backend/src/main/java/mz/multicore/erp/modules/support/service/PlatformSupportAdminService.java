package mz.multicore.erp.modules.support.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.support.dto.SupportMessageDTO;
import mz.multicore.erp.modules.support.dto.SupportTicketDTO;
import mz.multicore.erp.modules.support.model.SupportMessage;
import mz.multicore.erp.modules.support.model.SupportTicket;
import mz.multicore.erp.modules.support.model.TicketStatus;
import mz.multicore.erp.modules.support.repository.PlatformSupportTicketRepository;
import mz.multicore.erp.modules.support.repository.SupportMessageRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class PlatformSupportAdminService {

    private final PlatformSupportTicketRepository ticketRepository;
    private final SupportMessageRepository messageRepository;
    private final CompanyLookup companyLookup;

    public PlatformSupportAdminService(PlatformSupportTicketRepository ticketRepository,
                                       SupportMessageRepository messageRepository,
                                       CompanyLookup companyLookup) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.companyLookup = companyLookup;
    }

    public List<SupportTicketDTO> listAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(t -> toDto(t, true))
                .toList();
    }

    public List<SupportMessageDTO> listPlatformMessages(Long ticketId) {
        requireTicket(ticketId);
        return messages(ticketId);
    }

    public SupportTicketDTO addSuperAdminReply(Long ticketId, String body) {
        SupportTicket ticket = requireTicket(ticketId);
        requireBody(body);
        if (ticket.getStatus().isTerminal()) {
            throw new BusinessRuleException("O pedido está fechado.");
        }
        saveMessage(ticketId, CurrentUserContext.getUsername(), true, body);
        ticket.setAssignee(CurrentUserContext.getUsername());
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        ticketRepository.save(ticket);
        return toDto(ticket, true);
    }

    public SupportTicketDTO changeStatus(Long ticketId, String status) {
        SupportTicket ticket = requireTicket(ticketId);
        ticket.setStatus(parseStatus(status));
        ticketRepository.save(ticket);
        return toDto(ticket, true);
    }

    public void saveMessage(Long ticketId, String author, boolean fromSuperAdmin, String body) {
        SupportMessage message = new SupportMessage();
        message.setTicketId(ticketId);
        message.setAuthor(author);
        message.setFromSuperAdmin(fromSuperAdmin);
        message.setBody(body.trim());
        message.setCreatedBy(author);
        messageRepository.save(message);
    }

    public List<SupportMessageDTO> messages(Long ticketId) {
        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(m -> new SupportMessageDTO(m.getId(), m.getAuthor(), m.isFromSuperAdmin(),
                        m.getBody(), m.getCreatedAt()))
                .toList();
    }

    public SupportTicket requireTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessRuleException("Pedido de assistência não encontrado."));
    }

    public void requireBody(String body) {
        if (body == null || body.isBlank()) {
            throw new BusinessRuleException("A mensagem não pode estar vazia.");
        }
    }

    public SupportTicketDTO toDto(SupportTicket t, boolean includeCompanyName) {
        String companyName = includeCompanyName
                ? companyLookup.find(t.getCompanyId()).map(Company::getName).orElse("—")
                : null;
        return new SupportTicketDTO(t.getId(), t.getCompanyId(), companyName, t.getSubject(),
                t.getDescription(), t.getStatus().name(), t.getStatus().label(),
                t.getPriority().name(), t.getPriority().label(), t.getAssignee(), t.getCreatedBy(),
                t.getCreatedAt(), t.getUpdatedAt(), messageRepository.countByTicketId(t.getId()));
    }

    private TicketStatus parseStatus(String value) {
        try {
            return TicketStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessRuleException("Estado inválido.");
        }
    }
}
