package com.phcpro.modules.crm.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.pricing.TaxRates;
import com.phcpro.modules.comercial.dto.CreateInvoiceLineRequest;
import com.phcpro.modules.comercial.dto.CreateInvoiceRequest;
import com.phcpro.modules.comercial.model.Client;
import com.phcpro.modules.comercial.model.Product;
import com.phcpro.modules.comercial.repository.ClientRepository;
import com.phcpro.modules.comercial.repository.ProductRepository;
import com.phcpro.modules.comercial.service.ComercialService;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.inventory.model.Warehouse;
import com.phcpro.modules.inventory.repository.WarehouseRepository;
import com.phcpro.modules.crm.dto.*;
import com.phcpro.modules.crm.model.SupportTicket;
import com.phcpro.modules.crm.model.WorkSheet;
import com.phcpro.modules.crm.repository.SupportTicketRepository;
import com.phcpro.modules.crm.repository.WorkSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CRMService {

    private final SupportTicketRepository ticketRepository;
    private final WorkSheetRepository workSheetRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final ComercialService comercialService;
    private final CompanyRepository companyRepository;
    private final WarehouseRepository warehouseRepository;

    // Rate charged per hour of technical work
    private static final BigDecimal HOURLY_RATE = new BigDecimal("45.00");

    public CRMService(
            SupportTicketRepository ticketRepository,
            WorkSheetRepository workSheetRepository,
            ClientRepository clientRepository,
            ProductRepository productRepository,
            ComercialService comercialService,
            CompanyRepository companyRepository,
            WarehouseRepository warehouseRepository
    ) {
        this.ticketRepository = ticketRepository;
        this.workSheetRepository = workSheetRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.comercialService = comercialService;
        this.companyRepository = companyRepository;
        this.warehouseRepository = warehouseRepository;
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
        ticket.setStatus("OPEN");

        ticket = ticketRepository.save(ticket);
        return toDTO(ticket);
    }

    @Transactional
    public WorkSheetDTO createWorkSheet(CreateWorkSheetRequest request) {
        SupportTicket ticket = ticketRepository.findByIdAndCompanyId(
                        request.ticketId(), CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Pedido de assistência não encontrado."));

        WorkSheet ws = new WorkSheet();
        ws.setSupportTicket(ticket);
        ws.setTechnicianName(request.technicianName());
        ws.setHoursWorked(request.hoursWorked());
        ws.setDescription(request.description());
        ws.setPartsUsed(request.partsUsed());

        BigDecimal partsCost = request.partsCost() != null ? request.partsCost() : BigDecimal.ZERO;
        ws.setPartsCost(partsCost);

        // Value = (hours * HOURLY_RATE) + partsCost
        BigDecimal laborCost = request.hoursWorked().multiply(HOURLY_RATE);
        ws.setTotalValue(laborCost.add(partsCost).setScale(2, RoundingMode.HALF_UP));
        ws.setIsBilled(false);

        ws = workSheetRepository.save(ws);

        // Mark ticket as resolved when technical job is recorded
        ticket.setStatus("RESOLVED");
        ticketRepository.save(ticket);

        return toDTO(ws);
    }

    @Transactional
    public void billWorkSheet(Long workSheetId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        WorkSheet ws = workSheetRepository.findByIdAndSupportTicketCompanyId(workSheetId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Folha de obra não encontrada."));

        if (ws.getIsBilled()) {
            throw new BusinessRuleException("Esta folha de obra já foi faturada.");
        }

        Client client = ws.getSupportTicket().getClient();

        // Find or create technical service & parts product in the DB for invoicing
        Product techProduct = productRepository.findBySkuAndCompaniesId("SERV-TEC", companyId)
                .or(() -> productRepository.findBySku("SERV-TEC"))
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setSku("SERV-TEC");
                    p.setName("Serviço Técnico Especializado");
                    p.setUnitPrice(HOURLY_RATE);
                    p.getCompanies().add(companyRepository.getReferenceById(companyId));
                    p.setDescription("Mão de obra técnica de suporte");
                    return productRepository.save(p);
                });

        Product partsProduct = productRepository.findBySkuAndCompaniesId("PECAS-SUP", companyId)
                .or(() -> productRepository.findBySku("PECAS-SUP"))
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setSku("PECAS-SUP");
                    p.setName("Materiais e Peças de Reposição");
                    p.setUnitPrice(BigDecimal.ONE);
                    p.getCompanies().add(companyRepository.getReferenceById(companyId));
                    p.setDescription("Peças e materiais utilizados na assistência");
                    return productRepository.save(p);
                });

        techProduct.getCompanies().add(companyRepository.getReferenceById(companyId));
        partsProduct.getCompanies().add(companyRepository.getReferenceById(companyId));
        techProduct = productRepository.save(techProduct);
        partsProduct = productRepository.save(partsProduct);

        List<CreateInvoiceLineRequest> lines = new ArrayList<>();
        // Add hours line
        lines.add(new CreateInvoiceLineRequest(
                techProduct.getId(),
                ws.getHoursWorked().intValue(), // quantize hours to integer for simplify
                TaxRates.STANDARD_VAT
        ));

        // Add parts line if cost > 0
        if (ws.getPartsCost().compareTo(BigDecimal.ZERO) > 0) {
            // We set quantity to parts cost value, unitPrice is 1.00
            // Or unitPrice = partsCost and quantity = 1
            // Let's modify the parts unit price to match partsCost, and quantity to 1
            partsProduct.setUnitPrice(ws.getPartsCost());
            productRepository.save(partsProduct);

            lines.add(new CreateInvoiceLineRequest(
                    partsProduct.getId(),
                    1,
                    TaxRates.STANDARD_VAT
            ));
        }

        Company company = companyRepository.findById(companyId).stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException("Nenhuma empresa cadastrada no sistema para faturamento."));
        Warehouse warehouse = warehouseRepository.findByCompanyId(company.getId()).stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException("Nenhum armazém cadastrado para a empresa " + company.getName()));

        CreateInvoiceRequest invoiceRequest = new CreateInvoiceRequest(client.getId(), company.getId(), warehouse.getId(), lines);
        comercialService.createInvoice(invoiceRequest);

        ws.setIsBilled(true);
        workSheetRepository.save(ws);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getAllTickets() {
        return ticketRepository.findByCompanyIdOrderByCreatedAtDesc(CurrentUserContext.getCurrentCompanyId()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkSheetDTO> getAllWorkSheets() {
        return workSheetRepository.findBySupportTicketCompanyIdOrderByCreatedAtDesc(
                        CurrentUserContext.getCurrentCompanyId()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private SupportTicketDTO toDTO(SupportTicket t) {
        return new SupportTicketDTO(
                t.getId(),
                t.getClient().getId(),
                t.getClient().getName(),
                t.getSubject(),
                t.getDescription(),
                t.getStatus(),
                t.getCreatedAt() != null ? t.getCreatedAt() : LocalDateTime.now()
        );
    }

    private WorkSheetDTO toDTO(WorkSheet ws) {
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
                ws.getTotalValue(),
                ws.getIsBilled(),
                ws.getCreatedAt() != null ? ws.getCreatedAt() : LocalDateTime.now()
        );
    }
}
