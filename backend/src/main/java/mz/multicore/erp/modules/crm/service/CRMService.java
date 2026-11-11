package mz.multicore.erp.modules.crm.service;

import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.crm.dto.ChangeTicketStatusRequest;
import mz.multicore.erp.modules.crm.dto.CreateTicketRequest;
import mz.multicore.erp.modules.crm.dto.CreateWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.CrmSettingsDTO;
import mz.multicore.erp.modules.crm.dto.SupportTicketDTO;
import mz.multicore.erp.modules.crm.dto.UpdateCrmSettingsRequest;
import mz.multicore.erp.modules.crm.dto.UpdateTicketRequest;
import mz.multicore.erp.modules.crm.dto.UpdateWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.VoidWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.WorkSheetDTO;
import mz.multicore.erp.modules.crm.repository.SupportTicketRepository;
import mz.multicore.erp.modules.crm.repository.WorkSheetRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orquestrador de topo para operações de CRM (Pedidos de Assistência e Folhas de Obra).
 * Delega o ciclo de vida para colaboradores especializados respeitando os limites físicos de linhas.
 */
@Service
public class CRMService {

    private final SupportTicketLifecycleService ticketLifecycleService;
    private final WorkSheetService workSheetService;
    private final CRMQueryService queryService;

    @Autowired
    public CRMService(
            SupportTicketLifecycleService ticketLifecycleService,
            WorkSheetService workSheetService,
            CRMQueryService queryService
    ) {
        this.ticketLifecycleService = ticketLifecycleService;
        this.workSheetService = workSheetService;
        this.queryService = queryService;
    }

    /**
     * Construtor de retrocompatibilidade para testes legados e instanciacao manual com repositórios.
     */
    public CRMService(
            SupportTicketRepository ticketRepository,
            WorkSheetRepository workSheetRepository,
            ClientRepository clientRepository,
            ProductLookup productLookup,
            ProductCommandService productCommandService,
            ComercialService comercialService,
            CompanyLookup companyLookup,
            WarehouseRepository warehouseRepository
    ) {
        this.ticketLifecycleService = new SupportTicketLifecycleService(
                ticketRepository, clientRepository, companyLookup, workSheetRepository);
        this.workSheetService = new WorkSheetService(
                workSheetRepository, ticketRepository, productLookup, productCommandService, comercialService,
                companyLookup, warehouseRepository);
        this.queryService = new CRMQueryService(
                ticketRepository, workSheetRepository, productCommandService,
                this.ticketLifecycleService, this.workSheetService);
    }

    // ─── Pedidos de assistência ────────────────────────────────────────────────────────────────

    @Transactional
    public SupportTicketDTO createTicket(CreateTicketRequest request) {
        return ticketLifecycleService.createTicket(request);
    }

    @Transactional
    public SupportTicketDTO updateTicket(Long ticketId, UpdateTicketRequest request) {
        return ticketLifecycleService.updateTicket(ticketId, request);
    }

    @Transactional
    public SupportTicketDTO changeTicketStatus(Long ticketId, ChangeTicketStatusRequest request) {
        return ticketLifecycleService.changeTicketStatus(ticketId, request);
    }

    // ─── Folhas de obra ────────────────────────────────────────────────────────────────────────

    @Transactional
    public WorkSheetDTO createWorkSheet(CreateWorkSheetRequest request) {
        return workSheetService.createWorkSheet(request);
    }

    @Transactional
    public WorkSheetDTO updateWorkSheet(Long workSheetId, UpdateWorkSheetRequest request) {
        return workSheetService.updateWorkSheet(workSheetId, request);
    }

    @Transactional
    public WorkSheetDTO voidWorkSheet(Long workSheetId, VoidWorkSheetRequest request) {
        return workSheetService.voidWorkSheet(workSheetId, request);
    }

    @Transactional
    public void billWorkSheet(Long workSheetId) {
        workSheetService.billWorkSheet(workSheetId);
    }

    // ─── Parâmetros da assistência ─────────────────────────────────────────────────────────────

    @Transactional
    public CrmSettingsDTO getSettings() {
        return queryService.getSettings();
    }

    @Transactional
    public CrmSettingsDTO updateSettings(UpdateCrmSettingsRequest request) {
        return queryService.updateSettings(request);
    }

    // ─── Leituras ──────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getAllTickets() {
        return queryService.getAllTickets();
    }

    @Transactional(readOnly = true)
    public List<WorkSheetDTO> getAllWorkSheets() {
        return queryService.getAllWorkSheets();
    }

    @Transactional(readOnly = true)
    public WorkSheetDTO getWorkSheet(Long workSheetId) {
        return queryService.getWorkSheet(workSheetId);
    }

    public List<String> priorityOptions() {
        return queryService.priorityOptions();
    }

    public List<String> statusOptions() {
        return queryService.statusOptions();
    }
}
