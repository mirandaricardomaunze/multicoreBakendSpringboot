package mz.multicore.erp.modules.crm.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.pricing.TaxRates;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.crm.dto.CreateWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.UpdateWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.VoidWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.WorkSheetDTO;
import mz.multicore.erp.modules.crm.model.SupportTicket;
import mz.multicore.erp.modules.crm.model.TicketStatus;
import mz.multicore.erp.modules.crm.model.WorkSheet;
import mz.multicore.erp.modules.crm.repository.SupportTicketRepository;
import mz.multicore.erp.modules.crm.repository.WorkSheetRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkSheetService {

    private final WorkSheetRepository workSheetRepository;
    private final SupportTicketRepository ticketRepository;
    private final ProductRepository productRepository;
    private final ComercialService comercialService;
    private final CompanyLookup companyLookup;
    private final WarehouseRepository warehouseRepository;

    public static final String LABOUR_SKU = "SERV-TEC";
    public static final String PARTS_SKU = "PECAS-SUP";
    public static final BigDecimal PARTS_UNIT_PRICE = BigDecimal.ONE;
    public static final BigDecimal DEFAULT_HOURLY_RATE = new BigDecimal("45.00");

    public WorkSheetService(
            WorkSheetRepository workSheetRepository,
            SupportTicketRepository ticketRepository,
            ProductRepository productRepository,
            ComercialService comercialService,
            CompanyLookup companyLookup,
            WarehouseRepository warehouseRepository
    ) {
        this.workSheetRepository = workSheetRepository;
        this.ticketRepository = ticketRepository;
        this.productRepository = productRepository;
        this.comercialService = comercialService;
        this.companyLookup = companyLookup;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public WorkSheetDTO createWorkSheet(CreateWorkSheetRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        SupportTicket ticket = ticketRepository.findByIdAndCompanyId(request.ticketId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Pedido de assistência não encontrado."));

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new BusinessRuleException("Não se regista trabalho num pedido anulado. Reabra o pedido primeiro.");
        }

        WorkSheet ws = new WorkSheet();
        ws.setSupportTicket(ticket);
        ws.setTechnicianName(request.technicianName());
        ws.setHoursWorked(request.hoursWorked());
        ws.setDescription(request.description());
        ws.setPartsUsed(request.partsUsed());
        ws.setPartsCost(request.partsCost() != null ? request.partsCost() : BigDecimal.ZERO);
        ws.setHourlyRate(labourProduct(companyId).getUnitPrice());
        ws.setIsBilled(false);
        ws.setCreatedBy(CurrentUserContext.getUsername());
        applyTotal(ws);

        ws = workSheetRepository.save(ws);

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        if (ticket.getAssignedTechnician() == null) {
            ticket.setAssignedTechnician(request.technicianName());
        }
        ticketRepository.save(ticket);

        return toDTO(ws);
    }

    @Transactional
    public WorkSheetDTO updateWorkSheet(Long workSheetId, UpdateWorkSheetRequest request) {
        WorkSheet ws = requireWorkSheet(workSheetId);
        assertEditable(ws, "corrigida");

        ws.setTechnicianName(request.technicianName());
        ws.setHoursWorked(request.hoursWorked());
        ws.setDescription(request.description());
        ws.setPartsUsed(request.partsUsed());
        ws.setPartsCost(request.partsCost() != null ? request.partsCost() : BigDecimal.ZERO);
        applyTotal(ws);

        return toDTO(workSheetRepository.save(ws));
    }

    @Transactional
    public WorkSheetDTO voidWorkSheet(Long workSheetId, VoidWorkSheetRequest request) {
        WorkSheet ws = requireWorkSheet(workSheetId);
        assertEditable(ws, "anulada");

        ws.setVoided(true);
        ws.setVoidReason(request.reason());
        ws = workSheetRepository.save(ws);

        SupportTicket ticket = ws.getSupportTicket();
        boolean anyLiveWork = workSheetRepository.findBySupportTicketId(ticket.getId()).stream()
                .anyMatch(other -> !other.isVoided());
        if (!anyLiveWork && ticket.getStatus() == TicketStatus.RESOLVED) {
            ticket.setStatus(TicketStatus.OPEN);
            ticket.setResolvedAt(null);
            ticketRepository.save(ticket);
        }

        return toDTO(ws);
    }

    @Transactional
    public void billWorkSheet(Long workSheetId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        WorkSheet ws = requireWorkSheet(workSheetId);

        if (ws.isVoided()) {
            throw new BusinessRuleException("Esta folha de obra está anulada e não pode ser faturada.");
        }
        if (Boolean.TRUE.equals(ws.getIsBilled())) {
            throw new BusinessRuleException("Esta folha de obra já foi faturada.");
        }
        if (ws.getHoursWorked().compareTo(BigDecimal.ZERO) <= 0
                && ws.getPartsCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Folha sem horas nem peças: não há nada para faturar.");
        }

        Client client = ws.getSupportTicket().getClient();
        Product labour = labourProduct(companyId);
        Product parts = partsProduct(companyId);

        ws.setHourlyRate(labour.getUnitPrice());
        applyTotal(ws);

        List<CreateInvoiceLineRequest> lines = new ArrayList<>();
        if (ws.getHoursWorked().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new CreateInvoiceLineRequest(
                    labour.getId(), ws.getHoursWorked(), TaxRates.STANDARD_VAT,
                    BigDecimal.ZERO, null, null));
        }
        if (ws.getPartsCost().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(new CreateInvoiceLineRequest(
                    parts.getId(), ws.getPartsCost(), TaxRates.STANDARD_VAT,
                    BigDecimal.ZERO, null, null));
        }

        Company company = companyLookup.requireActive(companyId);
        Warehouse warehouse = warehouseRepository.findByCompanyId(companyId).stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Nenhum armazém cadastrado para a empresa " + company.getName()));

        comercialService.createInvoice(
                new CreateInvoiceRequest(client.getId(), company.getId(), warehouse.getId(), lines));

        ws.setIsBilled(true);
        workSheetRepository.save(ws);
    }

    @Transactional(readOnly = true)
    public WorkSheetDTO getWorkSheet(Long workSheetId) {
        return toDTO(requireWorkSheet(workSheetId));
    }

    public WorkSheet requireWorkSheet(Long workSheetId) {
        return workSheetRepository.findByIdAndSupportTicketCompanyId(
                        workSheetId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Folha de obra não encontrada."));
    }

    public void assertEditable(WorkSheet ws, String action) {
        if (Boolean.TRUE.equals(ws.getIsBilled())) {
            throw new BusinessRuleException(
                    "Folha já faturada: não pode ser " + action + ". Emita uma nota de crédito da factura.");
        }
        if (ws.isVoided()) {
            throw new BusinessRuleException("Esta folha de obra já está anulada.");
        }
    }

    public void applyTotal(WorkSheet ws) {
        BigDecimal labour = ws.getHoursWorked().multiply(ws.getHourlyRate());
        ws.setTotalValue(labour.add(ws.getPartsCost()).setScale(2, RoundingMode.HALF_UP));
    }

    public Product internalProduct(Long companyId, String sku, String name, BigDecimal unitPrice,
                                   String description) {
        Product product = productRepository.findBySkuAndCompaniesId(sku, companyId)
                .orElseGet(() -> {
                    Product created = new Product();
                    created.setSku(sku);
                    created.setName(name);
                    created.setUnitPrice(unitPrice);
                    created.setDescription(description);
                    created.getCompanies().add(companyLookup.reference(companyId));
                    created.setStockTracked(false);
                    return created;
                });

        if (product.isStockTracked()) {
            product.setStockTracked(false);
        }
        return productRepository.save(product);
    }

    public Product labourProduct(Long companyId) {
        return internalProduct(companyId, LABOUR_SKU, "Serviço Técnico Especializado",
                DEFAULT_HOURLY_RATE, "Mão de obra técnica de suporte (preço = tarifa horária)");
    }

    public Product partsProduct(Long companyId) {
        return internalProduct(companyId, PARTS_SKU, "Materiais e Peças de Reposição",
                PARTS_UNIT_PRICE, "Peças e materiais utilizados na assistência (quantidade = valor em MT)");
    }

    public WorkSheetDTO toDTO(WorkSheet ws) {
        return new WorkSheetDTO(
                ws.getId(),
                ws.getSupportTicket().getId(),
                ws.getSupportTicket().getSubject(),
                ws.getSupportTicket().getClient().getId(),
                ws.getSupportTicket().getClient().getName(),
                ws.getTechnicianName(),
                ws.getHoursWorked(),
                ws.getDescription(),
                ws.getPartsUsed(),
                ws.getPartsCost(),
                ws.getHourlyRate(),
                ws.getTotalValue(),
                ws.getIsBilled(),
                ws.isVoided(),
                ws.getVoidReason(),
                ws.getCreatedAt() != null ? ws.getCreatedAt() : LocalDateTime.now()
        );
    }
}
