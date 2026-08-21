package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.events.StockTransferResolvedEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.ConvertOrderToTransferRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.model.OrderLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.inventory.dto.CreateStockTransferRequest;
import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.StockTransferService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Reposição interna — RI-12, RI-15..30 (Mockito puro, sem levantar o Spring).
 *
 * <p>Ver docs/REPOSICAO_INTERNA_HARNESS.md. O foco é o que <b>não</b> acontece: nada de stock se
 * move aqui. A mercadoria muda de armazém uma só vez, na aprovação da transferência.
 */
class InternalReplenishmentServiceTest {

    private OrderRepository orderRepository;
    private StockTransferService stockTransferService;
    private ComercialService comercialService;
    private InternalReplenishmentService service;

    private Company company;
    private Warehouse origin;
    private Warehouse destination;
    private Product product;

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        CurrentUserContext.setCurrentCompanyId(1L);

        orderRepository = mock(OrderRepository.class);
        stockTransferService = mock(StockTransferService.class);
        comercialService = mock(ComercialService.class);
        service = new InternalReplenishmentService(orderRepository, stockTransferService, comercialService);

        company = new Company(); company.setId(1L); company.setName("Multicore");
        origin = new Warehouse(); origin.setId(10L); origin.setName("Armazém Central");
        destination = new Warehouse(); destination.setId(20L); destination.setName("Loja Baixa");
        product = new Product(); product.setId(100L); product.setName("Arroz 25 kg");
    }

    @AfterEach
    void clear() { CurrentUserContext.clear(); }

    // ─── Encomenda → Transferência ──────────────────────────────────────────

    @Test // RI-15 / RI-19 / RI-20 / RI-22
    void converteUmaReposicaoAprovadaEDeixaOsDoisDocumentosLigados() {
        Order order = replenishment("PENDING");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(stockTransferService.create(any())).thenReturn(transfer(55L, "APPROVED"));

        service.convertToTransfer(7L, new ConvertOrderToTransferRequest("Sr. Jofrisse", "AAA-11-11", null));

        ArgumentCaptor<CreateStockTransferRequest> sent = ArgumentCaptor.forClass(CreateStockTransferRequest.class);
        verify(stockTransferService).create(sent.capture());
        CreateStockTransferRequest request = sent.getValue();

        assertEquals(10L, request.originWarehouseId());
        assertEquals(20L, request.destinationWarehouseId(), "a mercadoria tem de ir para quem pediu");
        assertEquals(1, request.lines().size());
        assertEquals(100L, request.lines().get(0).productId());
        assertEquals(0, new BigDecimal("4").compareTo(request.lines().get(0).quantity()));

        assertEquals("TRANSFER_PENDING", order.getStatus(), "a encomenda fica travada (R8)");
        assertEquals(55L, order.getStockTransferId());
        verify(stockTransferService).linkToOrder(55L, 7L, "EC-2026/7");
    }

    @Test // RI-16
    void converteTambemUmaReposicaoJaSeparada() {
        Order order = replenishment("SEPARATED");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(stockTransferService.create(any())).thenReturn(transfer(55L, "PENDING_APPROVAL"));

        service.convertToTransfer(7L, null);

        assertEquals("TRANSFER_PENDING", order.getStatus());
    }

    @Test // RI-12 💰
    void converterNaoMoveStockNenhum() {
        Order order = replenishment("PENDING");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));
        when(stockTransferService.create(any())).thenReturn(transfer(55L, "PENDING_APPROVAL"));

        service.convertToTransfer(7L, null);

        // A transferência nasce por aprovar; o stock só se move quando alguém a aprova.
        verify(stockTransferService, never()).approve(any());
    }

    @Test // RI-17
    void converterAntesDeSepararDizOPassoQueFalta() {
        Order order = replenishment("AWAITING_SEPARATION");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.convertToTransfer(7L, null));

        assertTrue(error.getMessage().contains("lista de separação"), error.getMessage());
        verify(stockTransferService, never()).create(any());
    }

    @Test // RI-18 🔴
    void umaEncomendaDeVendaNaoViraTransferencia() {
        Order order = replenishment("PENDING");
        order.setKind(OrderKind.FORMAL_ORDER);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.convertToTransfer(7L, null));

        assertTrue(error.getMessage().contains("venda a cliente"), error.getMessage());
        verify(stockTransferService, never()).create(any());
    }

    @Test // RI-21
    void naoConverteDuasVezesAMesmaEncomenda() {
        Order order = replenishment("PENDING");
        order.setStockTransferId(55L);
        order.setTransferNumber("TRF-2026/1");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.convertToTransfer(7L, null));

        assertTrue(error.getMessage().contains("TRF-2026/1"), error.getMessage());
        verify(stockTransferService, never()).create(any());
    }

    @Test // RI-14
    void asRecusasNaoMostramCodigosInternos() {
        Order order = replenishment("PENDING");
        order.setKind(OrderKind.FORMAL_ORDER);
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.convertToTransfer(7L, null));

        assertFalse(error.getMessage().contains("INTERNAL_REPLENISHMENT"), error.getMessage());
        assertFalse(error.getMessage().contains("FORMAL_ORDER"), error.getMessage());
    }

    // ─── O que a transferência decide, a encomenda segue ────────────────────

    @Test // RI-23
    void transferenciaAprovadaFechaAEncomenda() {
        Order order = replenishment("TRANSFER_PENDING");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        service.onTransferResolved(new StockTransferResolvedEvent(
                7L, StockTransferResolvedEvent.Outcome.APPROVED));

        assertEquals("TRANSFERRED", order.getStatus());
    }

    @Test // RI-24 🔴
    void transferenciaRejeitadaDevolveAEncomendaAoEstadoConvertivel() {
        Order order = replenishment("TRANSFER_PENDING");
        order.setStockTransferId(55L);
        order.setTransferNumber("TRF-2026/1");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        service.onTransferResolved(new StockTransferResolvedEvent(
                7L, StockTransferResolvedEvent.Outcome.REJECTED));

        // Rejeitar não moveu stock: o pedido continua por cumprir e tem de poder ser convertido
        // outra vez, senão ficava preso sem nunca ter acontecido nada.
        assertEquals("PENDING", order.getStatus());
        assertNull(order.getStockTransferId());
        assertNull(order.getTransferNumber());
    }

    @Test // RI-25
    void transferenciaCanceladaTambemDevolveAEncomenda() {
        Order order = replenishment("TRANSFER_PENDING");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        service.onTransferResolved(new StockTransferResolvedEvent(
                7L, StockTransferResolvedEvent.Outcome.CANCELLED));

        assertEquals("PENDING", order.getStatus());
    }

    @Test // RI-26
    void transferenciaSemEncomendaDeOrigemNaoRebenta() {
        assertDoesNotThrow(() -> service.onTransferResolved(new StockTransferResolvedEvent(
                null, StockTransferResolvedEvent.Outcome.APPROVED)));
        verify(orderRepository, never()).save(any());
    }

    // ─── Transferência → Encomenda ──────────────────────────────────────────

    @Test // RI-29
    void naoRegistaDuasEncomendasParaAMesmaTransferencia() {
        StockTransferDTO already = new StockTransferDTO(55L, "TRF-2026/1", null, 1L, 10L, "Central",
                20L, "Loja", "APPROVED", null, null, null, null, null, null, List.of(), 7L, "EC-2026/7");
        when(stockTransferService.findById(55L)).thenReturn(already);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.recordOrderFromTransfer(55L));

        assertTrue(error.getMessage().contains("EC-2026/7"), error.getMessage());
        verify(comercialService, never()).createReplenishmentRecord(any());
    }

    @Test // RI-30 💰
    void naoRegistaEncomendaDeTransferenciaPorAprovar() {
        when(stockTransferService.findById(55L)).thenReturn(transfer(55L, "PENDING_APPROVAL"));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.recordOrderFromTransfer(55L));

        // Registar o pedido de uma transferência por aprovar inventaria um facto que não aconteceu.
        assertTrue(error.getMessage().contains("não moveu mercadoria"), error.getMessage());
        verify(comercialService, never()).createReplenishmentRecord(any());
    }

    @Test // RI-27 / RI-28 💰
    void registaAEncomendaEmFaltaSemMoverStock() {
        when(stockTransferService.findById(55L)).thenReturn(transfer(55L, "APPROVED"));
        Order recorded = replenishment("TRANSFERRED");
        when(comercialService.createReplenishmentRecord(any())).thenReturn(recorded);

        service.recordOrderFromTransfer(55L);

        verify(comercialService).createReplenishmentRecord(any());
        verify(stockTransferService).linkToOrder(55L, 7L, "EC-2026/7");
        // O registo é escrituração: nada de novo se move nem se cria como transferência.
        verify(stockTransferService, never()).create(any());
        verify(stockTransferService, never()).approve(any());
    }

    // ─── Fixtures ───────────────────────────────────────────────────────────

    private Order replenishment(String status) {
        Order order = new Order();
        order.setId(7L);
        order.setOrderNumber("EC-2026/7");
        order.setCompany(company);
        order.setWarehouse(origin);
        order.setDestinationWarehouse(destination);
        order.setKind(OrderKind.INTERNAL_REPLENISHMENT);
        order.setStatus(status);
        order.setClient(new Client());

        OrderLine line = new OrderLine();
        line.setProduct(product);
        line.setQuantity(new BigDecimal("4"));
        order.addLine(line);
        return order;
    }

    private StockTransferDTO transfer(Long id, String status) {
        return new StockTransferDTO(id, "TRF-2026/1", null, 1L, 10L, "Armazém Central",
                20L, "Loja Baixa", status, null, null, null, null, null, null, List.of(), null, null);
    }
}
