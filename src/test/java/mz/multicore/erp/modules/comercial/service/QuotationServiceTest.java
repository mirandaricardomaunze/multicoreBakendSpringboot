package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateQuotationLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateQuotationRequest;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.QuotationLine;
import mz.multicore.erp.modules.comercial.model.QuotationStatus;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Cotação ao cliente (Mockito puro — não levanta o Spring).
 *
 * <p>Foco nas duas regras de dinheiro: o preço cotado é o preço honrado na conversão, e um preço
 * caducado não se honra. Ver docs/COTACAO_HARNESS.md (CT-08..CT-35).
 */
class QuotationServiceTest {

    private QuotationRepository quotationRepository;
    private ClientRepository clientRepository;
    private ProductRepository productRepository;
    private CompanyRepository companyRepository;
    private WarehouseRepository warehouseRepository;
    private WalkInClientProvider walkInClientProvider;
    private DocumentNumberService documentNumberService;
    private AuditLogService auditLogService;
    private ComercialService comercialService;
    private QuotationService service;

    private Company company;
    private Warehouse warehouse;
    private Product product;
    private Client client;

    @BeforeEach
    void setUp() {
        quotationRepository = mock(QuotationRepository.class);
        clientRepository = mock(ClientRepository.class);
        productRepository = mock(ProductRepository.class);
        companyRepository = mock(CompanyRepository.class);
        warehouseRepository = mock(WarehouseRepository.class);
        walkInClientProvider = mock(WalkInClientProvider.class);
        documentNumberService = mock(DocumentNumberService.class);
        auditLogService = mock(AuditLogService.class);
        comercialService = mock(ComercialService.class);

        service = new QuotationService(quotationRepository, clientRepository, productRepository,
                companyRepository, warehouseRepository, walkInClientProvider, documentNumberService,
                auditLogService, comercialService);

        company = company(1L);
        warehouse = warehouse(10L, "Loja", company);
        product = product(100L, "SKU-1", "Arroz", new BigDecimal("120.00"));
        client = client(200L, "Cliente A");

        CurrentUserContext.setCurrentCompanyId(1L);
        CurrentUserContext.setCurrentUser("joao", "ADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    // ────────────────────────── criação ──────────────────────────

    @Test
    void create_ficaEmRascunho_comNumeroDaSerieCT_eTotaisCalculados() { // CT-08
        stubCreationLookups();

        QuotationDTO dto = service.create(request(
                new CreateQuotationLineRequest(100L, new BigDecimal("2"), BigDecimal.ZERO)));

        assertEquals(QuotationStatus.DRAFT.name(), dto.status());
        assertEquals("Rascunho", dto.statusLabel());
        assertEquals("CT-2026/1", dto.quotationNumber());
        // 2 × 120,00 = 240,00 + 16% = 278,40
        assertEquals(0, new BigDecimal("240.00").compareTo(dto.totalBeforeTax()));
        assertEquals(0, new BigDecimal("38.40").compareTo(dto.taxAmount()));
        assertEquals(0, new BigDecimal("278.40").compareTo(dto.totalAmount()));
        assertEquals(LocalDate.now().plusDays(15), dto.validUntil());
        assertFalse(dto.expired());
    }

    @Test
    void create_artigoIsento_naoCobraIva_mesmoQueOEcraDiscordasse() { // CT-09
        Product isento = product(101L, "SKU-2", "Farinha", new BigDecimal("80.00"));
        isento.setTaxRate(taxRate(BigDecimal.ZERO));
        stubCreationLookups();
        when(productRepository.findByIdAndCompaniesId(101L, 1L)).thenReturn(Optional.of(isento));

        QuotationDTO dto = service.create(request(
                new CreateQuotationLineRequest(101L, BigDecimal.ONE, BigDecimal.ZERO)));

        assertEquals(0, BigDecimal.ZERO.compareTo(dto.taxAmount()));
        assertEquals(0, new BigDecimal("80.00").compareTo(dto.totalAmount()));
    }

    @Test
    void create_quantidadeAtingeAMinimaDeGrosso_usaOPrecoDeGrosso() { // CT-12
        product.setWholesalePrice(new BigDecimal("100.00"));
        product.setWholesaleMinQty(new BigDecimal("10"));
        stubCreationLookups();

        QuotationDTO dto = service.create(request(
                new CreateQuotationLineRequest(100L, new BigDecimal("10"), BigDecimal.ZERO)));

        assertEquals(0, new BigDecimal("100.00").compareTo(dto.lines().get(0).unitPrice()));
    }

    @Test
    void create_semClienteRegistado_usaOClienteDeBalcao_eGuardaORotulo() { // CT-11
        stubCreationLookups();
        Client walkIn = client(999L, "Consumidor Final");
        when(walkInClientProvider.getOrCreate()).thenReturn(walkIn);

        QuotationDTO dto = service.create(new CreateQuotationRequest(
                null, "Sr. Alberto", 1L, 10L, 15, null, null, null,
                List.of(new CreateQuotationLineRequest(100L, BigDecimal.ONE, BigDecimal.ZERO))));

        assertEquals("Sr. Alberto", dto.clientName());
        assertEquals("Sr. Alberto", dto.walkInName());
        assertEquals(999L, dto.clientId());
    }

    @Test
    void create_descontoAcimaDeCem_lancaBusinessRuleException() {
        stubCreationLookups();
        assertThrows(BusinessRuleException.class, () -> service.create(request(
                new CreateQuotationLineRequest(100L, BigDecimal.ONE, new BigDecimal("120")))));
    }

    // ────────────────────────── máquina de estados ──────────────────────────

    @Test
    void send_deRascunho_ficaEnviada_comCarimboDeEnvio() { // CT-13
        Quotation q = openQuotation(QuotationStatus.DRAFT, 10);
        stubLoad(q);

        QuotationDTO dto = service.send(1L);

        assertEquals(QuotationStatus.SENT.name(), dto.status());
        assertNotNull(dto.sentAt());
    }

    @Test
    void send_deJaEnviada_lancaBusinessRuleException() { // CT-14
        stubLoad(openQuotation(QuotationStatus.SENT, 10));
        assertThrows(BusinessRuleException.class, () -> service.send(1L));
    }

    @Test
    void accept_registaQuemDecidiuEQuando() { // CT-15
        Quotation q = openQuotation(QuotationStatus.SENT, 10);
        stubLoad(q);

        QuotationDTO dto = service.accept(1L);

        assertEquals(QuotationStatus.ACCEPTED.name(), dto.status());
        assertEquals("joao", dto.decidedBy());
        assertNotNull(dto.decidedAt());
    }

    @Test
    void reject_semMotivo_lancaBusinessRuleException() { // CT-16
        stubLoad(openQuotation(QuotationStatus.SENT, 10));
        assertThrows(BusinessRuleException.class, () -> service.reject(1L, "   "));
    }

    @Test
    void reject_comMotivo_guardaOMotivo() { // CT-17
        stubLoad(openQuotation(QuotationStatus.SENT, 10));

        QuotationDTO dto = service.reject(1L, "Preço acima do concorrente");

        assertEquals(QuotationStatus.REJECTED.name(), dto.status());
        assertEquals("Preço acima do concorrente", dto.rejectionReason());
    }

    @Test
    void cancel_deRascunho_ficaCancelada() { // CT-18
        stubLoad(openQuotation(QuotationStatus.DRAFT, 10));
        assertEquals(QuotationStatus.CANCELLED.name(), service.cancel(1L).status());
    }

    @Test
    void cancel_deJaConvertida_lancaBusinessRuleException() { // CT-19
        Quotation q = openQuotation(QuotationStatus.CONVERTED, 10);
        q.setOrderNumber("EC-2026/7");
        stubLoad(q);
        assertThrows(BusinessRuleException.class, () -> service.cancel(1L));
    }

    @Test
    void accept_deRecusada_lancaBusinessRuleException() { // CT-20
        stubLoad(openQuotation(QuotationStatus.REJECTED, 10));
        assertThrows(BusinessRuleException.class, () -> service.accept(1L));
    }

    // ────────────────────────── conversão ──────────────────────────

    @Test
    void convert_herdaOPrecoCotado_mesmoComOCatalogoJaMaisCaro() { // CT-21, CT-22
        Quotation q = openQuotation(QuotationStatus.ACCEPTED, 10);
        stubLoad(q);
        stubPlaceOrder();
        // O catálogo subiu de 80,00 (cotado) para 120,00 depois da proposta sair.
        assertEquals(0, new BigDecimal("120.00").compareTo(product.getUnitPrice()));

        service.convert(1L);

        List<OrderLine> lines = capturePlacedLines();
        assertEquals(1, lines.size());
        assertEquals(0, new BigDecimal("80.00").compareTo(lines.get(0).getUnitPrice()));
        assertEquals(0, new BigDecimal("0.05").compareTo(lines.get(0).getTaxRate()));
        assertEquals(0, new BigDecimal("10.00").compareTo(lines.get(0).getDiscountPercentage()));
    }

    @Test
    void convert_cotacaoCaducada_recusa_eNomeiaAData() { // CT-23
        Quotation q = openQuotation(QuotationStatus.ACCEPTED, 10);
        q.setValidUntil(LocalDate.now().minusDays(1));
        stubLoad(q);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.convert(1L));
        assertTrue(ex.getMessage().contains(
                q.getValidUntil().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        verify(comercialService, never()).placeOrder(any(), any(), any(), any(), any(), any());
    }

    @Test
    void convert_segundaVez_recusa_eNomeiaAEncomendaJaCriada() { // CT-24
        Quotation q = openQuotation(QuotationStatus.CONVERTED, 10);
        q.setOrderNumber("EC-2026/7");
        stubLoad(q);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.convert(1L));
        assertTrue(ex.getMessage().contains("EC-2026/7"));
        verify(comercialService, never()).placeOrder(any(), any(), any(), any(), any(), any());
    }

    @Test
    void convert_deRecusadaOuCancelada_recusa() { // CT-25
        stubLoad(openQuotation(QuotationStatus.REJECTED, 10));
        assertThrows(BusinessRuleException.class, () -> service.convert(1L));

        stubLoad(openQuotation(QuotationStatus.CANCELLED, 10));
        assertThrows(BusinessRuleException.class, () -> service.convert(1L));
    }

    @Test
    void convert_deRascunho_aceita_eCarimbaAAceitacao() { // CT-26
        Quotation q = openQuotation(QuotationStatus.DRAFT, 10);
        stubLoad(q);
        stubPlaceOrder();

        service.convert(1L);

        // Quem converte sem passar pelo passo explícito de aceitação não pode perder o "quando".
        assertNotNull(q.getDecidedAt());
        assertEquals("joao", q.getDecidedBy());
    }

    @Test
    void convert_marcaConvertida_eGuardaAEncomendaGerada() { // CT-27
        Quotation q = openQuotation(QuotationStatus.ACCEPTED, 10);
        stubLoad(q);
        stubPlaceOrder();

        OrderDTO order = service.convert(1L);

        assertEquals("EC-2026/9", order.orderNumber());
        assertEquals(QuotationStatus.CONVERTED, q.getStatus());
        assertEquals(500L, q.getOrderId());
        assertEquals("EC-2026/9", q.getOrderNumber());
    }

    @Test
    void convert_geraEncomendaFormal_pelaPortaUnicaDeEncomendas() { // CT-28
        Quotation q = openQuotation(QuotationStatus.ACCEPTED, 10);
        stubLoad(q);
        stubPlaceOrder();

        service.convert(1L);

        // A via formal é o que faz a encomenda nascer PENDING_APPROVAL e ir à Engine de Aprovações,
        // dentro de placeOrder — daí bastar verificar a via com que a porta é chamada.
        verify(comercialService).placeOrder(eq(company), eq(client), eq(warehouse), any(), any(),
                eq(OrderKind.FORMAL_ORDER));
    }

    @Test
    void convert_naoMoveStockNemDinheiro() { // CT-29
        Quotation q = openQuotation(QuotationStatus.ACCEPTED, 10);
        stubLoad(q);
        stubPlaceOrder();

        service.convert(1L);

        // O serviço não tem sequer InventoryService/FinanceService injectados (CT-39); o que se
        // verifica aqui é que a única colaboração de escrita é a criação da encomenda.
        verify(comercialService, times(1)).placeOrder(any(), any(), any(), any(), any(), any());
        verifyNoMoreInteractions(comercialService);
    }

    @Test
    void convert_depoisDeEstenderAValidade_aceita() { // CT-30
        Quotation q = openQuotation(QuotationStatus.ACCEPTED, 10);
        q.setValidUntil(LocalDate.now().minusDays(1));
        stubLoad(q);

        assertThrows(BusinessRuleException.class, () -> service.convert(1L));

        service.extendValidity(1L, LocalDate.now().plusDays(5));
        stubPlaceOrder();
        assertEquals("EC-2026/9", service.convert(1L).orderNumber());
    }

    // ────────────────────────── validade e permissões ──────────────────────────

    @Test
    void extendValidity_semPerfilAutorizado_recusa_eNaoMexeNaValidade() { // CT-31
        CurrentUserContext.setCurrentUser("caixa", "CASHIER");
        Quotation q = openQuotation(QuotationStatus.SENT, 10);
        LocalDate original = q.getValidUntil();
        stubLoad(q);

        assertThrows(BusinessRuleException.class,
                () -> service.extendValidity(1L, LocalDate.now().plusDays(30)));
        assertEquals(original, q.getValidUntil());
    }

    @Test
    void extendValidity_comGerente_estende() { // CT-32
        CurrentUserContext.setCurrentUser("ana", "MANAGER");
        Quotation q = openQuotation(QuotationStatus.SENT, 10);
        stubLoad(q);

        LocalDate nova = LocalDate.now().plusDays(45);
        assertEquals(nova, service.extendValidity(1L, nova).validUntil());
    }

    @Test
    void extendValidity_paraDataAnteriorAActual_recusa() { // CT-07
        Quotation q = openQuotation(QuotationStatus.SENT, 10);
        stubLoad(q);
        assertThrows(BusinessRuleException.class,
                () -> service.extendValidity(1L, LocalDate.now().plusDays(3)));
    }

    @Test
    void extendValidity_auditaAValidadeAntigaEANova() { // CT-33
        Quotation q = openQuotation(QuotationStatus.SENT, 10);
        LocalDate antiga = q.getValidUntil();
        LocalDate nova = LocalDate.now().plusDays(45);
        stubLoad(q);

        service.extendValidity(1L, nova);

        ArgumentCaptor<String> details = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).logCurrent(eq("QUOTATION_EXTEND"), details.capture());
        var fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        assertTrue(details.getValue().contains(antiga.format(fmt)));
        assertTrue(details.getValue().contains(nova.format(fmt)));
    }

    @Test
    void acoesSobreCotacaoDeOutraEmpresa_naoEncontramODocumento() { // CT-35
        when(quotationRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.empty());
        assertThrows(BusinessRuleException.class, () -> service.convert(1L));
        assertThrows(BusinessRuleException.class, () -> service.accept(1L));
        assertThrows(BusinessRuleException.class, () -> service.findById(1L));
    }

    @Test
    void create_semPerfilPrivilegiado_eAceite() { // CT-34
        CurrentUserContext.setCurrentUser("caixa", "CASHIER");
        stubCreationLookups();
        assertNotNull(service.create(request(
                new CreateQuotationLineRequest(100L, BigDecimal.ONE, BigDecimal.ZERO))));
    }

    // ────────────────────────── helpers ──────────────────────────

    private void stubCreationLookups() {
        when(clientRepository.findByIdAndCompaniesId(200L, 1L)).thenReturn(Optional.of(client));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(productRepository.findByIdAndCompaniesId(100L, 1L)).thenReturn(Optional.of(product));
        when(documentNumberService.next(anyString())).thenReturn("CT-2026/1");
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(inv -> {
            Quotation q = inv.getArgument(0);
            q.setId(1L);
            return q;
        });
    }

    private void stubLoad(Quotation quotation) {
        when(quotationRepository.findByIdWithLinesAndCompanyId(1L, 1L)).thenReturn(Optional.of(quotation));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubPlaceOrder() {
        when(comercialService.placeOrder(any(), any(), any(), any(), any(), any()))
                .thenReturn(new OrderDTO(500L, "EC-2026/9", 200L, "Cliente A", "123456789", null,
                        new BigDecimal("72.00"), new BigDecimal("3.60"), new BigDecimal("75.60"),
                        "PENDING_APPROVAL", null, List.of(), null, null, 0, null,
                        OrderKind.FORMAL_ORDER, OrderKind.FORMAL_ORDER.label()));
    }

    @SuppressWarnings("unchecked")
    private List<OrderLine> capturePlacedLines() {
        ArgumentCaptor<List<OrderLine>> captor = ArgumentCaptor.forClass(List.class);
        verify(comercialService).placeOrder(any(), any(), any(), any(), captor.capture(), any());
        return captor.getValue();
    }

    private CreateQuotationRequest request(CreateQuotationLineRequest... lines) {
        return new CreateQuotationRequest(200L, null, 1L, 10L, 15,
                "50% na encomenda", "5 dias úteis", null, List.of(lines));
    }

    /**
     * Cotação persistida com uma linha a 80,00 + IVA 5% e 10% de desconto — números diferentes do
     * catálogo actual (120,00 a 16%), para que herdar o errado seja visível.
     */
    private Quotation openQuotation(QuotationStatus status, int validityDays) {
        Quotation q = new Quotation();
        q.setId(1L);
        q.setQuotationNumber("CT-2026/1");
        q.setQuotationDate(java.time.LocalDateTime.now());
        q.setValidUntil(LocalDate.now().plusDays(validityDays));
        q.setCompany(company);
        q.setClient(client);
        q.setWarehouse(warehouse);
        q.setStatus(status);
        q.setTotalBeforeTax(new BigDecimal("72.00"));
        q.setTaxAmount(new BigDecimal("3.60"));
        q.setTotalAmount(new BigDecimal("75.60"));

        QuotationLine line = new QuotationLine();
        line.setQuotation(q);
        line.setProduct(product);
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("80.00"));
        line.setTaxRate(new BigDecimal("0.05"));
        line.setDiscountPercentage(new BigDecimal("10.00"));
        line.setLineTotal(new BigDecimal("75.60"));
        q.getLines().add(line);
        return q;
    }

    private static Company company(long id) {
        Company c = new Company();
        c.setId(id);
        return c;
    }

    private static Warehouse warehouse(long id, String name, Company company) {
        Warehouse w = new Warehouse();
        w.setId(id);
        w.setName(name);
        w.setCompany(company);
        return w;
    }

    private static Product product(long id, String sku, String name, BigDecimal unitPrice) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(name);
        p.setUnitPrice(unitPrice);
        p.setUnitsPerBox(1);
        return p;
    }

    private static mz.multicore.erp.modules.fiscal.model.TaxRate taxRate(BigDecimal rate) {
        mz.multicore.erp.modules.fiscal.model.TaxRate t = new mz.multicore.erp.modules.fiscal.model.TaxRate();
        t.setRate(rate);
        return t;
    }

    private static Client client(long id, String name) {
        Client c = new Client();
        c.setId(id);
        c.setName(name);
        c.setTaxId("123456789");
        return c;
    }
}
