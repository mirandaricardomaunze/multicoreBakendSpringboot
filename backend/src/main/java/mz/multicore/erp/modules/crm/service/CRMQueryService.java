package mz.multicore.erp.modules.crm.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.crm.dto.CrmSettingsDTO;
import mz.multicore.erp.modules.crm.dto.SupportTicketDTO;
import mz.multicore.erp.modules.crm.dto.UpdateCrmSettingsRequest;
import mz.multicore.erp.modules.crm.dto.WorkSheetDTO;
import mz.multicore.erp.modules.crm.model.TicketPriority;
import mz.multicore.erp.modules.crm.model.TicketStatus;
import mz.multicore.erp.modules.crm.repository.SupportTicketRepository;
import mz.multicore.erp.modules.crm.repository.WorkSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CRMQueryService {

    private final SupportTicketRepository ticketRepository;
    private final WorkSheetRepository workSheetRepository;
    private final ProductRepository productRepository;
    private final SupportTicketLifecycleService ticketLifecycleService;
    private final WorkSheetService workSheetService;

    public CRMQueryService(
            SupportTicketRepository ticketRepository,
            WorkSheetRepository workSheetRepository,
            ProductRepository productRepository,
            SupportTicketLifecycleService ticketLifecycleService,
            WorkSheetService workSheetService
    ) {
        this.ticketRepository = ticketRepository;
        this.workSheetRepository = workSheetRepository;
        this.productRepository = productRepository;
        this.ticketLifecycleService = ticketLifecycleService;
        this.workSheetService = workSheetService;
    }

    @Transactional
    public CrmSettingsDTO getSettings() {
        Product labour = workSheetService.labourProduct(CurrentUserContext.getCurrentCompanyId());
        return new CrmSettingsDTO(labour.getUnitPrice(), labour.getSku(), labour.getName());
    }

    @Transactional
    public CrmSettingsDTO updateSettings(UpdateCrmSettingsRequest request) {
        PermissionGuard.requireManagerOrAdmin("alterar a tarifa horária da assistência");

        Product labour = workSheetService.labourProduct(CurrentUserContext.getCurrentCompanyId());
        labour.setUnitPrice(request.hourlyRate().setScale(2, RoundingMode.HALF_UP));
        labour = productRepository.save(labour);

        return new CrmSettingsDTO(labour.getUnitPrice(), labour.getSku(), labour.getName());
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getAllTickets() {
        return ticketRepository.findByCompanyIdOrderByCreatedAtDesc(CurrentUserContext.getCurrentCompanyId())
                .stream()
                .map(ticketLifecycleService::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkSheetDTO> getAllWorkSheets() {
        return workSheetRepository.findBySupportTicketCompanyIdOrderByCreatedAtDesc(
                        CurrentUserContext.getCurrentCompanyId()).stream()
                .map(workSheetService::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WorkSheetDTO getWorkSheet(Long workSheetId) {
        return workSheetService.getWorkSheet(workSheetId);
    }

    public List<String> priorityOptions() {
        return Arrays.stream(TicketPriority.values()).map(TicketPriority::label).toList();
    }

    public List<String> statusOptions() {
        return Arrays.stream(TicketStatus.values()).map(TicketStatus::label).toList();
    }
}
