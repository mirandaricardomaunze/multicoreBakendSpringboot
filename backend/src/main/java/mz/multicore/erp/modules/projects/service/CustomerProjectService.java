package mz.multicore.erp.modules.projects.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.WalkInClientProvider;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.projects.dto.*;
import mz.multicore.erp.modules.projects.model.CustomerProject;
import mz.multicore.erp.modules.projects.model.CustomerProjectOrder;
import mz.multicore.erp.modules.projects.repository.CustomerProjectOrderRepository;
import mz.multicore.erp.modules.projects.repository.CustomerProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Client projects or construction sites (SA-C09, {@code docs/OBRAS_CLIENTE_SPEC.md}). A contractor with several
 * sites gets one statement per site. Orders are linked through a separate table, so order and invoice contracts
 * stay unchanged; the statement follows each order to its invoice and shows what is still owed.
 */
@Service
public class CustomerProjectService {

    private final CustomerProjectRepository projectRepository;
    private final CustomerProjectOrderRepository linkRepository;
    private final ClientLookup clientLookup;
    private final CompanyLookup companyLookup;
    private final OrderRepository orderRepository;
    private final InvoiceLookup invoiceLookup;
    private final AuditLogService auditLogService;

    public CustomerProjectService(CustomerProjectRepository projectRepository, CustomerProjectOrderRepository linkRepository,
                                  ClientLookup clientLookup, CompanyLookup companyLookup,
                                  OrderRepository orderRepository, InvoiceLookup invoiceLookup,
                                  AuditLogService auditLogService) {
        this.projectRepository = projectRepository;
        this.linkRepository = linkRepository;
        this.clientLookup = clientLookup;
        this.companyLookup = companyLookup;
        this.orderRepository = orderRepository;
        this.invoiceLookup = invoiceLookup;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CustomerProjectDTO> list(Long companyId, Long clientId) {
        CurrentUserContext.requireCompany(companyId);
        return projectRepository.findByCompany(companyId, clientId).stream().map(p -> toDto(p, statementLines(p))).toList();
    }

    @Transactional
    public CustomerProjectDTO save(SaveCustomerProjectRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireSeller("registar obra de cliente");
        Client client = clientLookup.requireForCompany(request.clientId(), request.companyId());
        if (WalkInClientProvider.WALK_IN_TAX_ID.equals(client.getTaxId())) {
            throw new BusinessRuleException("Uma obra pertence a um cliente identificado, não ao Consumidor Final.");
        }
        boolean created = request.id() == null;
        CustomerProject project = created ? new CustomerProject() : requireProject(request.companyId(), request.id());
        if (created) {
            project.setCompany(companyLookup.require(request.companyId()));
            project.setCode(String.format("OB-%04d", projectRepository.countByCompanyId(request.companyId()) + 1));
        } else if (!project.getClient().getId().equals(client.getId()) && !linkRepository.findByProjectWithOrder(project.getId()).isEmpty()) {
            throw new BusinessRuleException("A obra já tem encomendas: não pode mudar de cliente.");
        }
        project.setClient(client);
        project.setName(request.name().trim());
        project.setAddress(blankToNull(request.address()));
        project.setContact(blankToNull(request.contact()));
        project.setActive(request.active());
        CustomerProject saved = projectRepository.save(project);
        auditLogService.logCurrent(created ? "CUSTOMER_PROJECT_CREATE" : "CUSTOMER_PROJECT_UPDATE",
                "Obra " + saved.getCode() + " (" + saved.getName() + ") do cliente " + client.getName());
        return toDto(saved, statementLines(saved));
    }

    /** Links an order to a project of the same client; moving it from another project is audited. */
    @Transactional
    public CustomerProjectDTO attachOrder(Long companyId, Long projectId, Long orderId) {
        CurrentUserContext.requireCompany(companyId);
        PermissionGuard.requireSeller("associar encomenda a obra");
        CustomerProject project = requireProject(companyId, projectId);
        if (!project.isActive()) throw new BusinessRuleException("A obra " + project.getName() + " está fechada.");
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new BusinessRuleException("Encomenda não encontrada nesta empresa."));
        if (order.getClient() == null || !order.getClient().getId().equals(project.getClient().getId())) {
            throw new BusinessRuleException("A encomenda " + order.getOrderNumber() + " é de outro cliente; a obra "
                    + project.getName() + " pertence a " + project.getClient().getName() + ".");
        }
        CustomerProjectOrder link = linkRepository.findByOrderId(order.getId()).orElseGet(CustomerProjectOrder::new);
        String previous = link.getProject() == null ? null : link.getProject().getCode();
        link.setProject(project);
        link.setOrder(order);
        linkRepository.save(link);
        auditLogService.logCurrent("CUSTOMER_PROJECT_ORDER_LINK", "Encomenda " + order.getOrderNumber() + " na obra "
                + project.getCode() + (previous == null || previous.equals(project.getCode()) ? "" : " (antes " + previous + ")"));
        return toDto(project, statementLines(project));
    }

    @Transactional
    public void detachOrder(Long companyId, Long orderId) {
        CurrentUserContext.requireCompany(companyId);
        PermissionGuard.requireManagerOrAdmin("retirar encomenda de uma obra");
        linkRepository.findByOrderId(orderId)
                .filter(l -> l.getProject().getCompany().getId().equals(companyId))
                .ifPresent(link -> {
                    linkRepository.delete(link);
                    auditLogService.logCurrent("CUSTOMER_PROJECT_ORDER_UNLINK", "Encomenda " + link.getOrder().getOrderNumber()
                            + " retirada da obra " + link.getProject().getCode());
                });
    }

    @Transactional(readOnly = true)
    public Optional<CustomerProjectDTO> projectOfOrder(Long companyId, Long orderId) {
        CurrentUserContext.requireCompany(companyId);
        return linkRepository.findByOrderId(orderId)
                .filter(l -> l.getProject().getCompany().getId().equals(companyId))
                .map(l -> toDto(l.getProject(), statementLines(l.getProject())));
    }

    @Transactional(readOnly = true)
    public ProjectStatementDTO statement(Long companyId, Long projectId) {
        CustomerProject project = requireProject(companyId, projectId);
        List<ProjectStatementLineDTO> lines = statementLines(project);
        BigDecimal ordered = lines.stream().map(ProjectStatementLineDTO::orderTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal invoiced = lines.stream().map(l -> nz(l.invoiceTotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = lines.stream().map(l -> nz(l.outstanding())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ProjectStatementDTO(toDto(project, lines), lines, ordered, invoiced, outstanding);
    }

    private List<ProjectStatementLineDTO> statementLines(CustomerProject project) {
        List<ProjectStatementLineDTO> lines = new ArrayList<>();
        for (CustomerProjectOrder link : linkRepository.findByProjectWithOrder(project.getId())) {
            Order order = link.getOrder();
            Invoice invoice = invoiceLookup.find(order.getInvoiceId()).orElse(null);
            boolean realised = invoice != null && invoice.getStatus().isRealisedSale();
            lines.add(new ProjectStatementLineDTO(order.getId(), order.getOrderNumber(),
                    order.getCreatedAt() == null ? null : order.getCreatedAt().toLocalDate(), order.getStatus(),
                    nz(order.getTotalAmount()), invoice == null ? null : invoice.getId(),
                    invoice == null ? null : invoice.getInvoiceNumber(),
                    realised ? invoice.getTotalAmount() : null, realised ? invoice.outstandingAmount() : null));
        }
        return lines;
    }

    private CustomerProject requireProject(Long companyId, Long projectId) {
        return projectRepository.findByIdAndCompanyId(projectId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Obra não encontrada nesta empresa."));
    }

    private static CustomerProjectDTO toDto(CustomerProject p, List<ProjectStatementLineDTO> lines) {
        BigDecimal ordered = lines.stream().map(ProjectStatementLineDTO::orderTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = lines.stream().map(l -> nz(l.outstanding())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CustomerProjectDTO(p.getId(), p.getClient().getId(), p.getClient().getName(), p.getCode(), p.getName(),
                p.getAddress(), p.getContact(), p.isActive(), lines.size(), ordered, outstanding);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
