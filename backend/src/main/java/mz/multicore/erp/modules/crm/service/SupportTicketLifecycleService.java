package mz.multicore.erp.modules.crm.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.crm.dto.ChangeTicketStatusRequest;
import mz.multicore.erp.modules.crm.dto.CreateTicketRequest;
import mz.multicore.erp.modules.crm.dto.SupportTicketDTO;
import mz.multicore.erp.modules.crm.dto.UpdateTicketRequest;
import mz.multicore.erp.modules.crm.model.SupportTicket;
import mz.multicore.erp.modules.crm.model.TicketPriority;
import mz.multicore.erp.modules.crm.model.TicketStatus;
import mz.multicore.erp.modules.crm.repository.SupportTicketRepository;
import mz.multicore.erp.modules.crm.repository.WorkSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SupportTicketLifecycleService {

    private final SupportTicketRepository ticketRepository;
    private final ClientRepository clientRepository;
    private final CompanyRepository companyRepository;
    private final WorkSheetRepository workSheetRepository;

    public SupportTicketLifecycleService(
            SupportTicketRepository ticketRepository,
            ClientRepository clientRepository,
            CompanyRepository companyRepository,
            WorkSheetRepository workSheetRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.clientRepository = clientRepository;
        this.companyRepository = companyRepository;
        this.workSheetRepository = workSheetRepository;
    }

    @Transactional
    public SupportTicketDTO createTicket(CreateTicketRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Client client = clientRepository.findByIdAndCompaniesId(request.clientId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."));

        SupportTicket ticket = new SupportTicket();
        ticket.setClient(client);
        ticket.setCompany(companyRepository.getReferenceById(companyId));
        ticket.setSubject(request.subject());
        ticket.setDescription(request.description());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(parsePriority(request.priority(), TicketPriority.NORMAL));
        ticket.setAssignedTechnician(blankToNull(request.assignedTechnician()));
        ticket.setCreatedBy(CurrentUserContext.getUsername());

        ticket = ticketRepository.save(ticket);
        return toDTO(ticket);
    }

    @Transactional
    public SupportTicketDTO updateTicket(Long ticketId, UpdateTicketRequest request) {
        SupportTicket ticket = requireTicket(ticketId);
        if (request.priority() != null && !request.priority().isBlank()) {
            ticket.setPriority(parsePriority(request.priority(), ticket.getPriority()));
        }
        if (request.assignedTechnician() != null) {
            ticket.setAssignedTechnician(blankToNull(request.assignedTechnician()));
        }
        return toDTO(ticketRepository.save(ticket));
    }

    @Transactional
    public SupportTicketDTO changeTicketStatus(Long ticketId, ChangeTicketStatusRequest request) {
        SupportTicket ticket = requireTicket(ticketId);
        TicketStatus target = parseStatus(request.status());
        String note = blankToNull(request.note());

        if (target == ticket.getStatus()) {
            throw new BusinessRuleException("O pedido já se encontra em '" + target.label() + "'.");
        }
        if (target == TicketStatus.CANCELLED) {
            if (note == null) {
                throw new BusinessRuleException("É obrigatório indicar o motivo da anulação.");
            }
            boolean hasLiveWork = workSheetRepository.findBySupportTicketId(ticketId).stream()
                    .anyMatch(ws -> !ws.isVoided());
            if (hasLiveWork) {
                throw new BusinessRuleException(
                        "O pedido tem folhas de obra registadas. Anule as folhas antes de anular o pedido.");
            }
        }

        ticket.setStatus(target);
        if (target.isTerminal()) {
            ticket.setResolvedAt(LocalDateTime.now());
            ticket.setClosingNote(note);
        } else {
            ticket.setResolvedAt(null);
            ticket.setClosingNote(null);
        }
        return toDTO(ticketRepository.save(ticket));
    }

    public SupportTicket requireTicket(Long ticketId) {
        return ticketRepository.findByIdAndCompanyId(ticketId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Pedido de assistência não encontrado."));
    }

    public TicketStatus parseStatus(String raw) {
        try {
            return TicketStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessRuleException("Estado de pedido inválido: " + raw);
        }
    }

    public TicketPriority parsePriority(String raw, TicketPriority fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return TicketPriority.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Prioridade inválida: " + raw);
        }
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public SupportTicketDTO toDTO(SupportTicket t) {
        return new SupportTicketDTO(
                t.getId(),
                t.getClient().getId(),
                t.getClient().getName(),
                t.getSubject(),
                t.getDescription(),
                t.getStatus().name(),
                t.getStatus().label(),
                t.getPriority().name(),
                t.getPriority().label(),
                t.getAssignedTechnician(),
                t.getClosingNote(),
                t.getCreatedAt() != null ? t.getCreatedAt() : LocalDateTime.now(),
                t.getResolvedAt()
        );
    }
}
