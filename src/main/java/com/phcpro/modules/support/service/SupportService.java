package com.phcpro.modules.support.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.PermissionGuard;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.support.dto.CreateTicketRequest;
import com.phcpro.modules.support.dto.SupportMessageDTO;
import com.phcpro.modules.support.dto.SupportTicketDTO;
import com.phcpro.modules.support.model.SupportMessage;
import com.phcpro.modules.support.model.SupportTicket;
import com.phcpro.modules.support.model.TicketPriority;
import com.phcpro.modules.support.model.TicketStatus;
import com.phcpro.modules.support.repository.SupportMessageRepository;
import com.phcpro.modules.support.repository.SupportTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Assistência empresa↔plataforma. Dois lados no mesmo serviço, com guards distintos: a empresa
 * (MANAGER/ADMIN, limitada à empresa activa) abre tickets e responde; o superadmin vê todos,
 * responde e muda o estado.
 */
@Service
public class SupportService {

    private final SupportTicketRepository ticketRepository;
    private final SupportMessageRepository messageRepository;
    private final CompanyRepository companyRepository;

    public SupportService(SupportTicketRepository ticketRepository,
                          SupportMessageRepository messageRepository,
                          CompanyRepository companyRepository) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.companyRepository = companyRepository;
    }

    // ------------------------------------------------------------------ Lado da empresa (tenant)

    @Transactional
    public SupportTicketDTO openTicket(CreateTicketRequest request) {
        PermissionGuard.requireManagerOrAdmin("abrir um pedido de assistência");
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

        // A descrição inicial fica também como primeira mensagem da conversa.
        if (request.description() != null && !request.description().isBlank()) {
            saveMessage(ticket.getId(), CurrentUserContext.getUsername(), false, request.description());
        }
        return toDto(ticket, false);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> listCompanyTickets() {
        PermissionGuard.requireManagerOrAdmin("consultar os pedidos de assistência");
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        return ticketRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(t -> toDto(t, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupportMessageDTO> listCompanyMessages(Long ticketId) {
        PermissionGuard.requireManagerOrAdmin("consultar a conversa");
        SupportTicket ticket = requireTicket(ticketId);
        requireSameCompany(ticket);
        return messages(ticketId);
    }

    @Transactional
    public void addCompanyMessage(Long ticketId, String body) {
        PermissionGuard.requireManagerOrAdmin("responder ao pedido");
        SupportTicket ticket = requireTicket(ticketId);
        requireSameCompany(ticket);
        requireBody(body);
        if (ticket.getStatus().isTerminal()) {
            throw new BusinessRuleException("O pedido está fechado.");
        }
        saveMessage(ticketId, CurrentUserContext.getUsername(), false, body);
        // Resposta da empresa a um pedido resolvido reabre-o.
        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }
    }

    // ------------------------------------------------------------------ Lado do superadmin

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> listAllTickets() {
        PermissionGuard.requireSuperAdmin("listar pedidos de assistência");
        return ticketRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(t -> toDto(t, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupportMessageDTO> listPlatformMessages(Long ticketId) {
        PermissionGuard.requireSuperAdmin("consultar a conversa");
        requireTicket(ticketId);
        return messages(ticketId);
    }

    @Transactional
    public SupportTicketDTO addSuperAdminReply(Long ticketId, String body) {
        PermissionGuard.requireSuperAdmin("responder ao pedido");
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

    @Transactional
    public SupportTicketDTO changeStatus(Long ticketId, String status) {
        PermissionGuard.requireSuperAdmin("mudar o estado do pedido");
        SupportTicket ticket = requireTicket(ticketId);
        ticket.setStatus(parseStatus(status));
        ticketRepository.save(ticket);
        return toDto(ticket, true);
    }

    // ------------------------------------------------------------------ Auxiliares

    private void saveMessage(Long ticketId, String author, boolean fromSuperAdmin, String body) {
        SupportMessage message = new SupportMessage();
        message.setTicketId(ticketId);
        message.setAuthor(author);
        message.setFromSuperAdmin(fromSuperAdmin);
        message.setBody(body.trim());
        message.setCreatedBy(author);
        messageRepository.save(message);
    }

    private List<SupportMessageDTO> messages(Long ticketId) {
        return messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(m -> new SupportMessageDTO(m.getId(), m.getAuthor(), m.isFromSuperAdmin(),
                        m.getBody(), m.getCreatedAt()))
                .toList();
    }

    private SupportTicket requireTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessRuleException("Pedido de assistência não encontrado."));
    }

    private void requireSameCompany(SupportTicket ticket) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        if (!companyId.equals(ticket.getCompanyId())) {
            throw new BusinessRuleException("O pedido pertence a outra empresa.");
        }
    }

    private void requireBody(String body) {
        if (body == null || body.isBlank()) {
            throw new BusinessRuleException("A mensagem não pode estar vazia.");
        }
    }

    private SupportTicketDTO toDto(SupportTicket t, boolean includeCompanyName) {
        String companyName = includeCompanyName
                ? companyRepository.findById(t.getCompanyId()).map(Company::getName).orElse("—")
                : null;
        return new SupportTicketDTO(t.getId(), t.getCompanyId(), companyName, t.getSubject(),
                t.getDescription(), t.getStatus().name(), t.getStatus().label(),
                t.getPriority().name(), t.getPriority().label(), t.getAssignee(), t.getCreatedBy(),
                t.getCreatedAt(), t.getUpdatedAt(), messageRepository.countByTicketId(t.getId()));
    }

    private TicketPriority parsePriority(String value) {
        if (value == null || value.isBlank()) return TicketPriority.NORMAL;
        try {
            return TicketPriority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Prioridade inválida.");
        }
    }

    private TicketStatus parseStatus(String value) {
        try {
            return TicketStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessRuleException("Estado inválido.");
        }
    }

    // Opções para a UI.
    public List<String> priorityOptions() {
        return java.util.Arrays.stream(TicketPriority.values()).map(Enum::name).toList();
    }

    public List<String> statusOptions() {
        return java.util.Arrays.stream(TicketStatus.values()).map(Enum::name).toList();
    }
}
