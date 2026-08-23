package mz.multicore.erp.modules.crm.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.architecture.pricing.TaxRates;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.crm.dto.*;
import mz.multicore.erp.modules.crm.model.SupportTicket;
import mz.multicore.erp.modules.crm.model.TicketPriority;
import mz.multicore.erp.modules.crm.model.TicketStatus;
import mz.multicore.erp.modules.crm.model.WorkSheet;
import mz.multicore.erp.modules.crm.repository.SupportTicketRepository;
import mz.multicore.erp.modules.crm.repository.WorkSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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

    /** SKU interno da mão de obra técnica. O preço do produto <b>é</b> a tarifa horária da loja. */
    private static final String LABOUR_SKU = "SERV-TEC";
    /** SKU interno das peças. Preço unitário fixo em 1.00: a quantidade transporta o valor gasto. */
    private static final String PARTS_SKU = "PECAS-SUP";
    private static final BigDecimal PARTS_UNIT_PRICE = BigDecimal.ONE;

    /** Tarifa inicial, usada só quando a loja ainda não tem o produto de mão de obra no catálogo. */
    private static final BigDecimal DEFAULT_HOURLY_RATE = new BigDecimal("45.00");

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

    // ─── Pedidos de assistência ────────────────────────────────────────────────────────────────

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

    /** Atribuição: quem trata o pedido e com que urgência. Não mexe no estado. */
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

    /**
     * Assume, resolve, anula ou reabre um pedido. Antes o único caminho para fechar era registar
     * folha de obra — um pedido resolvido ao telefone ou aberto por engano ficava aberto para sempre.
     */
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
            // Anular apaga o pedido da lista de trabalho. Se já houve trabalho registado e não
            // anulado, o que existe é uma folha para faturar, não um engano.
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
            // Reabrir devolve o pedido ao trabalho: a data e a nota de fecho deixam de fazer sentido.
            ticket.setResolvedAt(null);
            ticket.setClosingNote(null);
        }
        return toDTO(ticketRepository.save(ticket));
    }

    // ─── Folhas de obra ────────────────────────────────────────────────────────────────────────

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

        // Registar trabalho fecha o pedido — continua a ser o caminho normal, agora só um entre vários.
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        if (ticket.getAssignedTechnician() == null) {
            ticket.setAssignedTechnician(request.technicianName());
        }
        ticketRepository.save(ticket);

        return toDTO(ws);
    }

    /** Corrige uma folha ainda por faturar. Depois de faturada o documento fiscal manda. */
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

    /**
     * Anula uma folha por faturar. Não apaga: a folha fica na lista com o motivo à vista. Se o
     * pedido tinha sido fechado por causa desta folha e não sobra mais trabalho, volta a abrir.
     */
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

        // A tarifa do catálogo manda no momento de faturar; a folha acompanha para que o que o
        // cliente assinou e o que a factura cobra sejam sempre o mesmo número.
        ws.setHourlyRate(labour.getUnitPrice());
        applyTotal(ws);

        List<CreateInvoiceLineRequest> lines = new ArrayList<>();
        if (ws.getHoursWorked().compareTo(BigDecimal.ZERO) > 0) {
            // Horas em BigDecimal: meia hora de trabalho é meia hora faturada. Antes ia
            // `hoursWorked().intValue()` e 2,5 h saíam da factura como 2 h.
            lines.add(new CreateInvoiceLineRequest(
                    labour.getId(), ws.getHoursWorked(), TaxRates.STANDARD_VAT,
                    BigDecimal.ZERO, null, null));
        }
        if (ws.getPartsCost().compareTo(BigDecimal.ZERO) > 0) {
            // Preço unitário fixo em 1.00 e o valor das peças na quantidade. Antes gravava-se o
            // custo desta folha no `unitPrice` do produto partilhado — o catálogo ficava com o
            // último valor de quem faturou por último.
            lines.add(new CreateInvoiceLineRequest(
                    parts.getId(), ws.getPartsCost(), TaxRates.STANDARD_VAT,
                    BigDecimal.ZERO, null, null));
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada."));
        Warehouse warehouse = warehouseRepository.findByCompanyId(companyId).stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException(
                        "Nenhum armazém cadastrado para a empresa " + company.getName()));

        comercialService.createInvoice(
                new CreateInvoiceRequest(client.getId(), company.getId(), warehouse.getId(), lines));

        ws.setIsBilled(true);
        workSheetRepository.save(ws);
    }

    // ─── Parâmetros da assistência ─────────────────────────────────────────────────────────────

    /**
     * Tarifa horária em vigor. Era uma constante compilada — a loja não tinha como mudar o preço
     * do seu próprio serviço técnico sem alguém recompilar a aplicação.
     */
    @Transactional
    public CrmSettingsDTO getSettings() {
        Product labour = labourProduct(CurrentUserContext.getCurrentCompanyId());
        return new CrmSettingsDTO(labour.getUnitPrice(), labour.getSku(), labour.getName());
    }

    /**
     * Muda a tarifa horária. Repor o preço do serviço da loja não é trabalho de balcão, por isso
     * exige gerente ou administrador — o mesmo critério dos preços no resto do sistema.
     *
     * <p>Só mexe daqui para a frente: as folhas já registadas guardam a tarifa com que foram
     * calculadas e não se movem.
     */
    @Transactional
    public CrmSettingsDTO updateSettings(UpdateCrmSettingsRequest request) {
        PermissionGuard.requireManagerOrAdmin("alterar a tarifa horária da assistência");

        Product labour = labourProduct(CurrentUserContext.getCurrentCompanyId());
        labour.setUnitPrice(request.hourlyRate().setScale(2, RoundingMode.HALF_UP));
        labour = productRepository.save(labour);

        return new CrmSettingsDTO(labour.getUnitPrice(), labour.getSku(), labour.getName());
    }

    // ─── Leituras ──────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SupportTicketDTO> getAllTickets() {
        return ticketRepository.findByCompanyIdOrderByCreatedAtDesc(CurrentUserContext.getCurrentCompanyId())
                .stream()
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

    @Transactional(readOnly = true)
    public WorkSheetDTO getWorkSheet(Long workSheetId) {
        return toDTO(requireWorkSheet(workSheetId));
    }

    /** Opções para os combos do desktop — o cliente não replica a lista de enums. */
    public List<String> priorityOptions() {
        return Arrays.stream(TicketPriority.values()).map(TicketPriority::label).toList();
    }

    public List<String> statusOptions() {
        return Arrays.stream(TicketStatus.values()).map(TicketStatus::label).toList();
    }

    // ─── Internos ──────────────────────────────────────────────────────────────────────────────

    private SupportTicket requireTicket(Long ticketId) {
        return ticketRepository.findByIdAndCompanyId(ticketId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Pedido de assistência não encontrado."));
    }

    private WorkSheet requireWorkSheet(Long workSheetId) {
        return workSheetRepository.findByIdAndSupportTicketCompanyId(
                        workSheetId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Folha de obra não encontrada."));
    }

    private void assertEditable(WorkSheet ws, String action) {
        if (Boolean.TRUE.equals(ws.getIsBilled())) {
            throw new BusinessRuleException(
                    "Folha já faturada: não pode ser " + action + ". Emita uma nota de crédito da factura.");
        }
        if (ws.isVoided()) {
            throw new BusinessRuleException("Esta folha de obra já está anulada.");
        }
    }

    private void applyTotal(WorkSheet ws) {
        BigDecimal labour = ws.getHoursWorked().multiply(ws.getHourlyRate());
        ws.setTotalValue(labour.add(ws.getPartsCost()).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Produto interno da empresa activa, criado à medida que for preciso.
     *
     * <p>Duas correcções em relação ao que existia: procura-se <b>só dentro da empresa</b> (antes
     * havia um {@code findBySku} global que ia buscar o produto de outro tenant e lhe anexava a
     * empresa actual), e o produto nasce <b>sem controlo de stock</b> — mão de obra e peças
     * genéricas não têm lotes, e o FEFO recusava a saída com "Stock insuficiente", o que fazia
     * rebentar todas as facturações de folhas de obra.
     */
    private Product internalProduct(Long companyId, String sku, String name, BigDecimal unitPrice,
                                    String description) {
        Product product = productRepository.findBySkuAndCompaniesId(sku, companyId)
                .orElseGet(() -> {
                    Product created = new Product();
                    created.setSku(sku);
                    created.setName(name);
                    created.setUnitPrice(unitPrice);
                    created.setDescription(description);
                    created.getCompanies().add(companyRepository.getReferenceById(companyId));
                    created.setStockTracked(false);
                    return created;
                });

        // Reparação de instalações anteriores: os produtos criados pela versão antiga ficaram com
        // controlo de stock ligado e bloqueiam a facturação enquanto não forem corrigidos.
        if (product.isStockTracked()) {
            product.setStockTracked(false);
        }
        return productRepository.save(product);
    }

    private Product labourProduct(Long companyId) {
        return internalProduct(companyId, LABOUR_SKU, "Serviço Técnico Especializado",
                DEFAULT_HOURLY_RATE, "Mão de obra técnica de suporte (preço = tarifa horária)");
    }

    private Product partsProduct(Long companyId) {
        return internalProduct(companyId, PARTS_SKU, "Materiais e Peças de Reposição",
                PARTS_UNIT_PRICE, "Peças e materiais utilizados na assistência (quantidade = valor em MT)");
    }

    private TicketStatus parseStatus(String raw) {
        try {
            return TicketStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BusinessRuleException("Estado de pedido inválido: " + raw);
        }
    }

    private TicketPriority parsePriority(String raw, TicketPriority fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return TicketPriority.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Prioridade inválida: " + raw);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SupportTicketDTO toDTO(SupportTicket t) {
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
                ws.getHourlyRate(),
                ws.getTotalValue(),
                ws.getIsBilled(),
                ws.isVoided(),
                ws.getVoidReason(),
                ws.getCreatedAt() != null ? ws.getCreatedAt() : LocalDateTime.now()
        );
    }
}
