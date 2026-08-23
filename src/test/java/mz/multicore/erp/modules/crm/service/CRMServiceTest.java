package mz.multicore.erp.modules.crm.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.crm.dto.ChangeTicketStatusRequest;
import mz.multicore.erp.modules.crm.dto.CreateTicketRequest;
import mz.multicore.erp.modules.crm.dto.CreateWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.SupportTicketDTO;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes do {@link CRMService}. Cobrem sobretudo a facturação da folha de obra, que estava partida
 * de três maneiras ao mesmo tempo — serviço tratado como mercadoria com stock, horas truncadas para
 * inteiro, e o preço do produto partilhado de peças reescrito a cada factura — e o ciclo de vida do
 * pedido, que antes só sabia fechar registando folha de obra.
 */
class CRMServiceTest {

    private static final Long COMPANY = 7L;

    private SupportTicketRepository ticketRepository;
    private WorkSheetRepository workSheetRepository;
    private ClientRepository clientRepository;
    private ProductRepository productRepository;
    private ComercialService comercialService;
    private CompanyRepository companyRepository;
    private WarehouseRepository warehouseRepository;
    private CRMService service;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(SupportTicketRepository.class);
        workSheetRepository = mock(WorkSheetRepository.class);
        clientRepository = mock(ClientRepository.class);
        productRepository = mock(ProductRepository.class);
        comercialService = mock(ComercialService.class);
        companyRepository = mock(CompanyRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);

        service = new CRMService(ticketRepository, workSheetRepository, clientRepository,
                productRepository, comercialService, companyRepository, warehouseRepository);

        when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(i -> {
            SupportTicket t = i.getArgument(0);
            if (t.getId() == null) t.setId(50L);
            return t;
        });
        when(workSheetRepository.save(any(WorkSheet.class))).thenAnswer(i -> {
            WorkSheet ws = i.getArgument(0);
            if (ws.getId() == null) ws.setId(60L);
            return ws;
        });
        when(productRepository.save(any(Product.class))).thenAnswer(i -> {
            Product p = i.getArgument(0);
            if (p.getId() == null) p.setId("SERV-TEC".equals(p.getSku()) ? 900L : 901L);
            return p;
        });
        when(companyRepository.getReferenceById(COMPANY)).thenReturn(company());
        when(companyRepository.findById(COMPANY)).thenReturn(Optional.of(company()));
        when(warehouseRepository.findByCompanyId(COMPANY)).thenReturn(List.of(warehouse()));
        when(workSheetRepository.findBySupportTicketId(anyLong())).thenReturn(List.of());

        CurrentUserContext.setCurrentUser("tecnico", "EMPLOYEE");
        CurrentUserContext.setCurrentCompanyId(COMPANY);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    // ─── Facturação da folha de obra ───────────────────────────────────────────────────────────

    @Test
    void billWorkSheet_naoTruncaHoras() {
        WorkSheet ws = persistedWorkSheet(new BigDecimal("2.5"), new BigDecimal("100.00"));
        service.billWorkSheet(ws.getId());

        CreateInvoiceRequest request = capturedInvoice();
        CreateInvoiceLineRequest labour = request.lines().get(0);
        assertEquals(0, new BigDecimal("2.5").compareTo(labour.quantity()),
                "2,5 horas têm de chegar inteiras à factura; antes iam como 2");
    }

    @Test
    void billWorkSheet_produtosInternosNascemSemControloDeStock() {
        WorkSheet ws = persistedWorkSheet(new BigDecimal("1"), BigDecimal.ZERO);
        service.billWorkSheet(ws.getId());

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, atLeastOnce()).save(saved.capture());
        assertTrue(saved.getAllValues().stream().noneMatch(Product::isStockTracked),
                "mão de obra não tem lotes: com stock ligado o FEFO recusa a saída e a facturação rebenta");
    }

    @Test
    void billWorkSheet_reparaProdutoAntigoComStockLigado() {
        Product legacy = product(900L, "SERV-TEC", new BigDecimal("45.00"));
        legacy.setStockTracked(true);
        when(productRepository.findBySkuAndCompaniesId("SERV-TEC", COMPANY)).thenReturn(Optional.of(legacy));

        WorkSheet ws = persistedWorkSheet(new BigDecimal("1"), BigDecimal.ZERO);
        service.billWorkSheet(ws.getId());

        assertFalse(legacy.isStockTracked(), "instalações antigas têm de ser corrigidas ao faturar");
    }

    @Test
    void billWorkSheet_naoReescreveOPrecoDoProdutoDePecas() {
        Product parts = product(901L, "PECAS-SUP", BigDecimal.ONE);
        when(productRepository.findBySkuAndCompaniesId("PECAS-SUP", COMPANY)).thenReturn(Optional.of(parts));

        WorkSheet ws = persistedWorkSheet(new BigDecimal("1"), new BigDecimal("250.00"));
        service.billWorkSheet(ws.getId());

        assertEquals(0, BigDecimal.ONE.compareTo(parts.getUnitPrice()),
                "o catálogo não pode ficar com o custo das peças desta folha");
        CreateInvoiceLineRequest partsLine = capturedInvoice().lines().get(1);
        assertEquals(0, new BigDecimal("250.00").compareTo(partsLine.quantity()),
                "o valor das peças viaja na quantidade, com preço unitário fixo em 1,00");
    }

    @Test
    void billWorkSheet_naoProcuraProdutoForaDaEmpresa() {
        WorkSheet ws = persistedWorkSheet(new BigDecimal("1"), BigDecimal.ZERO);
        service.billWorkSheet(ws.getId());

        verify(productRepository, never()).findBySku(anyString());
    }

    @Test
    void billWorkSheet_folhaAnulada_recusa() {
        WorkSheet ws = persistedWorkSheet(new BigDecimal("1"), BigDecimal.ZERO);
        ws.setVoided(true);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.billWorkSheet(ws.getId()));
        assertTrue(ex.getMessage().contains("anulada"));
        verify(comercialService, never()).createInvoice(any());
    }

    @Test
    void billWorkSheet_semHorasNemPecas_recusa() {
        WorkSheet ws = persistedWorkSheet(BigDecimal.ZERO, BigDecimal.ZERO);
        assertThrows(BusinessRuleException.class, () -> service.billWorkSheet(ws.getId()));
    }

    // ─── Ciclo de vida do pedido ───────────────────────────────────────────────────────────────

    @Test
    void changeTicketStatus_resolveSemFolhaDeObra() {
        SupportTicket ticket = persistedTicket(TicketStatus.OPEN);

        SupportTicketDTO dto = service.changeTicketStatus(ticket.getId(),
                new ChangeTicketStatusRequest("RESOLVED", "Resolvido ao telefone"));

        assertEquals("RESOLVED", dto.status());
        assertEquals("Resolvido", dto.statusLabel());
        assertNotNull(dto.resolvedAt());
        assertEquals("Resolvido ao telefone", dto.closingNote());
    }

    @Test
    void changeTicketStatus_anularSemMotivo_recusa() {
        SupportTicket ticket = persistedTicket(TicketStatus.OPEN);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.changeTicketStatus(ticket.getId(), new ChangeTicketStatusRequest("CANCELLED", "  ")));
        assertTrue(ex.getMessage().contains("motivo"));
    }

    @Test
    void changeTicketStatus_anularComTrabalhoRegistado_recusa() {
        SupportTicket ticket = persistedTicket(TicketStatus.OPEN);
        WorkSheet live = new WorkSheet();
        live.setSupportTicket(ticket);
        when(workSheetRepository.findBySupportTicketId(ticket.getId())).thenReturn(List.of(live));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.changeTicketStatus(ticket.getId(),
                        new ChangeTicketStatusRequest("CANCELLED", "Engano")));
        assertTrue(ex.getMessage().contains("folhas de obra"));
    }

    @Test
    void changeTicketStatus_reabrir_limpaFecho() {
        SupportTicket ticket = persistedTicket(TicketStatus.RESOLVED);
        ticket.setResolvedAt(java.time.LocalDateTime.now());
        ticket.setClosingNote("nota antiga");

        SupportTicketDTO dto = service.changeTicketStatus(ticket.getId(),
                new ChangeTicketStatusRequest("OPEN", null));

        assertEquals("OPEN", dto.status());
        assertNull(dto.resolvedAt());
        assertNull(dto.closingNote());
    }

    @Test
    void changeTicketStatus_estadoInvalido_recusa() {
        SupportTicket ticket = persistedTicket(TicketStatus.OPEN);
        assertThrows(BusinessRuleException.class, () -> service.changeTicketStatus(ticket.getId(),
                new ChangeTicketStatusRequest("ARQUIVADO", null)));
    }

    @Test
    void createTicket_gravaPrioridadeETecnico() {
        Client client = client();
        when(clientRepository.findByIdAndCompaniesId(5L, COMPANY)).thenReturn(Optional.of(client));

        SupportTicketDTO dto = service.createTicket(
                new CreateTicketRequest(5L, "Impressora", "Não imprime", "URGENT", "Mário"));

        assertEquals("URGENT", dto.priority());
        assertEquals("Urgente", dto.priorityLabel());
        assertEquals("Mário", dto.assignedTechnician());
        assertEquals("OPEN", dto.status());
    }

    @Test
    void createWorkSheet_pedidoAnulado_recusa() {
        SupportTicket ticket = persistedTicket(TicketStatus.CANCELLED);
        assertThrows(BusinessRuleException.class, () -> service.createWorkSheet(
                new CreateWorkSheetRequest(ticket.getId(), "Mário", BigDecimal.ONE, "Reparação", null, null)));
    }

    @Test
    void createWorkSheet_gravaTarifaDoCatalogo() {
        SupportTicket ticket = persistedTicket(TicketStatus.OPEN);
        when(productRepository.findBySkuAndCompaniesId("SERV-TEC", COMPANY))
                .thenReturn(Optional.of(product(900L, "SERV-TEC", new BigDecimal("120.00"))));

        WorkSheetDTO dto = service.createWorkSheet(new CreateWorkSheetRequest(
                ticket.getId(), "Mário", new BigDecimal("2"), "Reparação", "Cabo", new BigDecimal("50")));

        assertEquals(0, new BigDecimal("120.00").compareTo(dto.hourlyRate()));
        assertEquals(0, new BigDecimal("290.00").compareTo(dto.totalValue()), "2 × 120 + 50");
    }

    // ─── Correcção e anulação de folhas ────────────────────────────────────────────────────────

    @Test
    void updateWorkSheet_jaFaturada_recusa() {
        WorkSheet ws = persistedWorkSheet(new BigDecimal("1"), BigDecimal.ZERO);
        ws.setIsBilled(true);

        assertThrows(BusinessRuleException.class, () -> service.updateWorkSheet(ws.getId(),
                new UpdateWorkSheetRequest("Mário", BigDecimal.ONE, "x", null, BigDecimal.ZERO)));
    }

    @Test
    void updateWorkSheet_recalculaTotal() {
        WorkSheet ws = persistedWorkSheet(new BigDecimal("1"), BigDecimal.ZERO);
        ws.setHourlyRate(new BigDecimal("50.00"));

        WorkSheetDTO dto = service.updateWorkSheet(ws.getId(), new UpdateWorkSheetRequest(
                "Mário", new BigDecimal("3.5"), "Reparação longa", "Fusível", new BigDecimal("25.00")));

        assertEquals(0, new BigDecimal("200.00").compareTo(dto.totalValue()), "3,5 × 50 + 25");
    }

    @Test
    void voidWorkSheet_reabreOPedidoQueTinhaFechado() {
        SupportTicket ticket = persistedTicket(TicketStatus.RESOLVED);
        WorkSheet ws = persistedWorkSheet(new BigDecimal("1"), BigDecimal.ZERO);
        ws.setSupportTicket(ticket);

        WorkSheetDTO dto = service.voidWorkSheet(ws.getId(), new VoidWorkSheetRequest("Lançada no pedido errado"));

        assertTrue(dto.voided());
        assertEquals("Anulada", dto.statusLabel());
        assertEquals(TicketStatus.OPEN, ticket.getStatus());
        assertNull(ticket.getResolvedAt());
    }

    @Test
    void voidWorkSheet_comOutraFolhaViva_mantemPedidoFechado() {
        SupportTicket ticket = persistedTicket(TicketStatus.RESOLVED);
        WorkSheet ws = persistedWorkSheet(new BigDecimal("1"), BigDecimal.ZERO);
        ws.setSupportTicket(ticket);
        WorkSheet other = new WorkSheet();
        other.setSupportTicket(ticket);
        when(workSheetRepository.findBySupportTicketId(ticket.getId())).thenReturn(List.of(ws, other));

        service.voidWorkSheet(ws.getId(), new VoidWorkSheetRequest("Duplicada"));

        assertEquals(TicketStatus.RESOLVED, ticket.getStatus());
    }

    // ─── Fixtures ──────────────────────────────────────────────────────────────────────────────

    private CreateInvoiceRequest capturedInvoice() {
        ArgumentCaptor<CreateInvoiceRequest> captor = ArgumentCaptor.forClass(CreateInvoiceRequest.class);
        verify(comercialService).createInvoice(captor.capture());
        return captor.getValue();
    }

    private Company company() {
        Company c = new Company();
        c.setId(COMPANY);
        c.setName("Loja Teste");
        return c;
    }

    private Warehouse warehouse() {
        Warehouse w = new Warehouse();
        w.setId(3L);
        w.setName("Armazém Central");
        return w;
    }

    private Client client() {
        Client c = new Client();
        c.setId(5L);
        c.setName("Padaria Central");
        return c;
    }

    private Product product(Long id, String sku, BigDecimal price) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(sku);
        p.setUnitPrice(price);
        p.setStockTracked(false);
        return p;
    }

    private SupportTicket persistedTicket(TicketStatus status) {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(11L);
        ticket.setClient(client());
        ticket.setCompany(company());
        ticket.setSubject("Impressora avariada");
        ticket.setDescription("Não liga");
        ticket.setStatus(status);
        when(ticketRepository.findByIdAndCompanyId(11L, COMPANY)).thenReturn(Optional.of(ticket));
        return ticket;
    }

    private WorkSheet persistedWorkSheet(BigDecimal hours, BigDecimal partsCost) {
        WorkSheet ws = new WorkSheet();
        ws.setId(60L);
        ws.setSupportTicket(persistedTicket(TicketStatus.RESOLVED));
        ws.setTechnicianName("Mário");
        ws.setHoursWorked(hours);
        ws.setDescription("Reparação");
        ws.setPartsCost(partsCost);
        ws.setHourlyRate(new BigDecimal("45.00"));
        ws.setTotalValue(hours.multiply(new BigDecimal("45.00")).add(partsCost));
        ws.setIsBilled(false);
        when(workSheetRepository.findByIdAndSupportTicketCompanyId(60L, COMPANY)).thenReturn(Optional.of(ws));
        return ws;
    }
}
