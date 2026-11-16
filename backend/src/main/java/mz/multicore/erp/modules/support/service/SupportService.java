package mz.multicore.erp.modules.support.service;

import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.support.dto.CreateTicketRequest;
import mz.multicore.erp.modules.support.dto.SupportMessageDTO;
import mz.multicore.erp.modules.support.dto.SupportTicketDTO;
import mz.multicore.erp.modules.support.model.TicketPriority;
import mz.multicore.erp.modules.support.model.TicketStatus;
import mz.multicore.erp.modules.support.repository.PlatformSupportTicketRepository;
import mz.multicore.erp.modules.support.repository.SupportMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Assistência empresa↔plataforma. Dois lados no mesmo serviço, com guards distintos: a empresa
 * (MANAGER/ADMIN, limitada à empresa activa) abre tickets e responde; o superadmin vê todos,
 * responde e muda o estado.
 */
@Service
public class SupportService {

    private final CompanySupportTicketService companyTicketService;
    private final PlatformSupportAdminService platformAdminService;

    @Autowired
    public SupportService(CompanySupportTicketService companyTicketService,
                          PlatformSupportAdminService platformAdminService) {
        this.companyTicketService = companyTicketService;
        this.platformAdminService = platformAdminService;
    }

    public SupportService(PlatformSupportTicketRepository ticketRepository,
                          SupportMessageRepository messageRepository,
                          CompanyLookup companyLookup) {
        PlatformSupportAdminService admin = new PlatformSupportAdminService(
                ticketRepository, messageRepository, companyLookup);
        this.platformAdminService = admin;
        this.companyTicketService = new CompanySupportTicketService(ticketRepository, admin);
    }

    @Transactional
    public SupportTicketDTO openTicket(CreateTicketRequest request) {
        PermissionGuard.requireManagerOrAdmin("abrir um pedido de assistência");
        return companyTicketService.openTicket(request);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> listCompanyTickets() {
        PermissionGuard.requireManagerOrAdmin("consultar os pedidos de assistência");
        return companyTicketService.listCompanyTickets();
    }

    @Transactional(readOnly = true)
    public List<SupportMessageDTO> listCompanyMessages(Long ticketId) {
        PermissionGuard.requireManagerOrAdmin("consultar a conversa");
        return companyTicketService.listCompanyMessages(ticketId);
    }

    @Transactional
    public void addCompanyMessage(Long ticketId, String body) {
        PermissionGuard.requireManagerOrAdmin("responder ao pedido");
        companyTicketService.addCompanyMessage(ticketId, body);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> listAllTickets() {
        PermissionGuard.requireSuperAdmin("listar pedidos de assistência");
        return platformAdminService.listAllTickets();
    }

    @Transactional(readOnly = true)
    public List<SupportMessageDTO> listPlatformMessages(Long ticketId) {
        PermissionGuard.requireSuperAdmin("consultar a conversa");
        return platformAdminService.listPlatformMessages(ticketId);
    }

    @Transactional
    public SupportTicketDTO addSuperAdminReply(Long ticketId, String body) {
        PermissionGuard.requireSuperAdmin("responder ao pedido");
        return platformAdminService.addSuperAdminReply(ticketId, body);
    }

    @Transactional
    public SupportTicketDTO changeStatus(Long ticketId, String status) {
        PermissionGuard.requireSuperAdmin("mudar o estado do pedido");
        return platformAdminService.changeStatus(ticketId, status);
    }

    public List<String> priorityOptions() {
        return Arrays.stream(TicketPriority.values()).map(Enum::name).toList();
    }

    public List<String> statusOptions() {
        return Arrays.stream(TicketStatus.values()).map(Enum::name).toList();
    }
}
