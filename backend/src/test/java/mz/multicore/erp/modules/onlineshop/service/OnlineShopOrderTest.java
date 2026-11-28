package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopFulfilment;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopOrderLineRequest;
import mz.multicore.erp.modules.onlineshop.dto.PlaceOnlineShopOrderRequest;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopDeliveryZone;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.architecture.pricing.TaxRates;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateOrderRequest;
import mz.multicore.erp.modules.comercial.model.ProductSaleType;
import mz.multicore.erp.modules.comercial.service.ProductCommandService;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.OrderLineDTO;
import mz.multicore.erp.modules.comercial.dto.SaveClientRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.ExternalOrderSource;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.model.OrderOrigin;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.comercial.service.ExternalOrderPlacementService;
import mz.multicore.erp.modules.comercial.model.ReservedStock;
import mz.multicore.erp.modules.comercial.service.OrderReservationLookup;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.StockLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopOrderDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopOrderLineDTO;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness LO-05 (docs/LOJA_ONLINE_SPEC.md §3.3): the checkout requires the name and a Mozambican mobile phone
 * (validated like the mobile payment), accepts an optional but valid NUIT, and needs a pickup store of the shop for
 * {@code LEVANTAMENTO} or a zone of the shop and an address for {@code ENTREGA}.
 * Harness LO-06 (§3.3): the order is placed through the public API's placement service — system prices and VAT,
 * reserved stock, client by NUIT or Consumidor Final with the contact on the order, origin {@code LOJA_ONLINE}.
 * Harness LO-07 (§3.3): the order's store is the pickup store, or for delivery the first pickup store with free stock
 * for the whole cart; without stock the order is refused and nothing is created.
 * Harness LO-08 (§3.3): home delivery adds the service line "Taxa de entrega — {zona}" at the zone fee and the
 * standard VAT; pickup is free.
 */
class OnlineShopOrderTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long PICKUP_A = 10L;
    private static final Long PICKUP_B = 11L;
    private static final List<OnlineShopOrderLineRequest> CART =
            List.of(new OnlineShopOrderLineRequest(" ARROZ-5KG ", new BigDecimal("2")));

    @Test
    void lo05_levantamentoValidoNormalizaOsDados() {
        OnlineShopCheckout checkout = OnlineShopCheckoutRules.validate(new PlaceOnlineShopOrderRequest(
                "  Ana Machava ", "+258 84 123 4567", null, OnlineShopFulfilment.LEVANTAMENTO, PICKUP_A,
                "Matola", "Rua X", "  ", CART), shop());

        assertEquals("Ana Machava", checkout.customerName());
        assertEquals("841234567", checkout.phone());
        assertNull(checkout.nuit());
        assertEquals(PICKUP_A, checkout.pickupWarehouseId());
        assertNull(checkout.deliveryZone(), "levantamento não leva zona");
        assertNull(checkout.deliveryAddress(), "levantamento não leva morada");
        assertNull(checkout.notes());
        assertEquals("ARROZ-5KG", checkout.lines().get(0).sku());
    }

    @Test
    void lo05_entregaValidaUsaAZonaDaLoja() {
        OnlineShopCheckout checkout = OnlineShopCheckoutRules.validate(new PlaceOnlineShopOrderRequest(
                "Ana", "86 123 4567", "400000008", OnlineShopFulfilment.ENTREGA, null, "  matola ",
                " Av. Samora Machel, 10 ", "Tocar à campainha", CART), shop());

        assertEquals("Matola", checkout.deliveryZone().getName());
        assertEquals(0, new BigDecimal("150.00").compareTo(checkout.deliveryZone().getFee()));
        assertEquals("Av. Samora Machel, 10", checkout.deliveryAddress());
        assertEquals("400000008", checkout.nuit());
        assertNull(checkout.pickupWarehouseId());
    }

    @Test
    void lo05_nomeObrigatorio() {
        assertRefused("Indique o seu nome.", request("  ", "841234567", null));
    }

    @Test
    void lo05_telemovelObrigatorioEMocambicano() {
        assertRefused("O número de telemóvel é obrigatório.", request("Ana", " ", null));
        for (String invalid : List.of("84123456", "881234567", "211234567", "+351 912 345 678", "abc")) {
            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> OnlineShopCheckoutRules.validate(request("Ana", invalid, null), shop()), invalid);
            assertEquals("Indique um telemóvel moçambicano com 9 dígitos, começado por 82 a 87 (ex.: 84 123 4567).",
                    ex.getMessage());
        }
        for (String valid : List.of("821234567", "83 123 4567", "258851234567", "87-123-4567")) {
            OnlineShopCheckoutRules.validate(request("Ana", valid, null), shop());
        }
    }

    @Test
    void lo05_nuitOpcionalMasValidoQuandoIndicado() {
        assertNull(OnlineShopCheckoutRules.validate(request("Ana", "841234567", "   "), shop()).nuit());
        assertRefused("O NUIT indicado não é válido (9 dígitos).", request("Ana", "841234567", "12345"));
        assertRefused("O NUIT indicado não é válido (9 dígitos).", request("Ana", "841234567", "400000007"));
    }

    @Test
    void lo05_modoObrigatorio() {
        assertRefused("Escolha levantamento na loja ou entrega ao domicílio.", new PlaceOnlineShopOrderRequest(
                "Ana", "841234567", null, null, PICKUP_A, null, null, null, CART));
    }

    @Test
    void lo05_levantamentoNumaLojaDaLojaOnline() {
        assertRefused("Escolha a loja de levantamento.", new PlaceOnlineShopOrderRequest(
                "Ana", "841234567", null, OnlineShopFulfilment.LEVANTAMENTO, null, null, null, null, CART));
        assertRefused("A loja de levantamento escolhida não está disponível.", new PlaceOnlineShopOrderRequest(
                "Ana", "841234567", null, OnlineShopFulfilment.LEVANTAMENTO, 99L, null, null, null, CART));
    }

    @Test
    void lo05_entregaExigeZonaDaLojaEMorada() {
        assertRefused("Escolha a zona de entrega.", delivery(null, "Rua X"));
        assertRefused("A zona de entrega escolhida não existe nesta loja.", delivery("Beira", "Rua X"));
        assertRefused("Indique a morada de entrega.", delivery("Matola", "  "));
    }

    @Test
    void lo05_carrinhoNaoVazioComQuantidadesPositivas() {
        assertRefused("O carrinho está vazio.", new PlaceOnlineShopOrderRequest(
                "Ana", "841234567", null, OnlineShopFulfilment.LEVANTAMENTO, PICKUP_A, null, null, null, List.of()));
        assertRefused("A quantidade do artigo ARROZ-5KG tem de ser maior que zero.", new PlaceOnlineShopOrderRequest(
                "Ana", "841234567", null, OnlineShopFulfilment.LEVANTAMENTO, PICKUP_A, null, null, null,
                List.of(new OnlineShopOrderLineRequest("ARROZ-5KG", BigDecimal.ZERO))));
    }

    @Test
    void lo05_limitesDeTamanho() {
        assertRefused("O nome tem no máximo 120 caracteres.", request("A".repeat(121), "841234567", null));
        assertRefused("A morada tem no máximo 255 caracteres.", delivery("Matola", "R".repeat(256)));
    }

    // ─── LO-06: encomenda pelo serviço da API pública ─────────────────────────

    private final OnlineShopSettingsRepository settingsRepository = mock(OnlineShopSettingsRepository.class);
    private final ProductLookup productLookup = mock(ProductLookup.class);
    private final ClientLookup clientLookup = mock(ClientLookup.class);
    private final ComercialService comercialService = mock(ComercialService.class);
    private final ExternalOrderPlacementService placementService = mock(ExternalOrderPlacementService.class);
    private final StockLookup stockLookup = mock(StockLookup.class);
    private final OrderReservationLookup reservationLookup = mock(OrderReservationLookup.class);
    private final List<Stock> stockLines = new ArrayList<>();
    private final List<ReservedStock> reservations = new ArrayList<>();
    private final ProductCommandService productCommandService = mock(ProductCommandService.class);
    private final OnlineShopOrderService orderService = new OnlineShopOrderService(settingsRepository, productLookup,
            clientLookup, comercialService, placementService, stockLookup, reservationLookup,
            new DeliveryFeeProductProvisioner(productLookup, productCommandService));

    {
        when(stockLookup.listForCompany(COMPANY_ID)).thenReturn(stockLines);
        when(reservationLookup.activeReservationsForCompany(COMPANY_ID)).thenReturn(reservations);
        when(productCommandService.saveProduct(any())).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(900L);
            return saved;
        });
    }

    @Test
    void lo06_consumidorFinalPeloServicoDaApiPublicaComOrigemLojaOnline() {
        stubShop();
        Product arroz = onSale(1L, new BigDecimal("16"));
        when(productLookup.findBySkuForCompany("ARROZ-5KG", COMPANY_ID)).thenReturn(Optional.of(arroz));
        AtomicReference<String> actor = new AtomicReference<>();
        when(placementService.placeReservedFormalOrder(any(), any())).thenAnswer(invocation -> {
            actor.set(CurrentUserContext.getUsername() + "|" + CurrentUserContext.getRole() + "|"
                    + CurrentUserContext.requireCurrentCompanyId());
            return placed("ENC-2026/00007", "Aguarda aprovação", "232.00");
        });

        OnlineShopOrderDTO result = orderService.placeOrder(" Mercearia-Baixa ", new PlaceOnlineShopOrderRequest(
                "Ana Machava", "+258 84 123 4567", null, OnlineShopFulfilment.LEVANTAMENTO, PICKUP_B, null, null,
                "Ligar antes", CART));

        ArgumentCaptor<CreateOrderRequest> order = ArgumentCaptor.forClass(CreateOrderRequest.class);
        ArgumentCaptor<ExternalOrderSource> source = ArgumentCaptor.forClass(ExternalOrderSource.class);
        verify(placementService).placeReservedFormalOrder(order.capture(), source.capture());
        assertNull(order.getValue().clientId(), "sem NUIT é Consumidor Final");
        assertEquals("Ana Machava", order.getValue().walkInName());
        assertEquals(COMPANY_ID, order.getValue().companyId());
        assertEquals(PICKUP_B, order.getValue().warehouseId());
        assertEquals(OrderKind.FORMAL_ORDER, order.getValue().kind());
        assertEquals("Ligar antes", order.getValue().notes());
        assertEquals(1L, order.getValue().lines().get(0).productId());
        assertEquals(0, new BigDecimal("2").compareTo(order.getValue().lines().get(0).quantity()));
        assertEquals(new BigDecimal("16"), order.getValue().lines().get(0).taxRate(), "IVA do artigo no sistema");
        assertNull(order.getValue().lines().get(0).unitPrice(), "o preço nunca vem do pedido");
        assertEquals(OrderOrigin.LOJA_ONLINE, source.getValue().origin());
        assertEquals("Loja online", source.getValue().originName());
        assertEquals("841234567", source.getValue().contactPhone());
        assertEquals("Loja online · mercearia-baixa|SELLER|1", actor.get(), "a loja vende como vendedor da empresa");

        assertEquals(new OnlineShopOrderDTO("ENC-2026/00007", "Aguarda aprovação",
                List.of(new OnlineShopOrderLineDTO("ARROZ-5KG", "Arroz 5 kg", new BigDecimal("2"))),
                new BigDecimal("232.00")), result);
    }

    @Test
    void lo06_clienteExistentePorNuitGuardaOContacto() {
        stubShop();
        when(productLookup.findBySkuForCompany("ARROZ-5KG", COMPANY_ID)).thenReturn(Optional.of(onSale(1L, null)));
        Client client = new Client();
        client.setId(42L);
        when(clientLookup.findForCompanyByTaxId("400000008", COMPANY_ID)).thenReturn(Optional.of(client));
        when(placementService.placeReservedFormalOrder(any(), any())).thenReturn(placed("ENC-1", "Pendente", "1"));

        orderService.placeOrder("mercearia-baixa", new PlaceOnlineShopOrderRequest("Ana", "841234567", "400000008",
                OnlineShopFulfilment.LEVANTAMENTO, PICKUP_A, null, null, null, CART));

        ArgumentCaptor<CreateOrderRequest> order = ArgumentCaptor.forClass(CreateOrderRequest.class);
        ArgumentCaptor<ExternalOrderSource> source = ArgumentCaptor.forClass(ExternalOrderSource.class);
        verify(placementService).placeReservedFormalOrder(order.capture(), source.capture());
        assertEquals(42L, order.getValue().clientId());
        assertNull(order.getValue().walkInName());
        assertEquals("841234567", source.getValue().contactPhone());
        verify(comercialService, never()).createClient(any());
    }

    @Test
    void lo06_nuitNovoRegistaOClienteComONomeDoCheckout() {
        stubShop();
        when(productLookup.findBySkuForCompany("ARROZ-5KG", COMPANY_ID)).thenReturn(Optional.of(onSale(1L, null)));
        when(clientLookup.findForCompanyByTaxId("400000008", COMPANY_ID)).thenReturn(Optional.empty());
        when(comercialService.createClient(any())).thenReturn(
                new ClientDTO(77L, "Ana Machava", "400000008", null, null, 0, null, null, null));
        when(placementService.placeReservedFormalOrder(any(), any())).thenReturn(placed("ENC-1", "Pendente", "1"));

        orderService.placeOrder("mercearia-baixa", new PlaceOnlineShopOrderRequest("Ana Machava", "841234567",
                "400000008", OnlineShopFulfilment.LEVANTAMENTO, PICKUP_A, null, null, null, CART));

        ArgumentCaptor<SaveClientRequest> client = ArgumentCaptor.forClass(SaveClientRequest.class);
        verify(comercialService).createClient(client.capture());
        assertEquals("Ana Machava", client.getValue().name());
        assertEquals("400000008", client.getValue().taxId());
        ArgumentCaptor<CreateOrderRequest> order = ArgumentCaptor.forClass(CreateOrderRequest.class);
        verify(placementService).placeReservedFormalOrder(order.capture(), any());
        assertEquals(77L, order.getValue().clientId());
    }

    @Test
    void lo06_artigoNaoPublicadoNaoSeEncomendaENadaECriado() {
        stubShop();
        Product hidden = onSale(1L, null);
        hidden.setPublishedOnline(false);
        when(productLookup.findBySkuForCompany("ARROZ-5KG", COMPANY_ID)).thenReturn(Optional.of(hidden));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> orderService.placeOrder(
                "mercearia-baixa", request("Ana", "841234567", null)));

        assertEquals("O artigo ARROZ-5KG não está à venda na loja online.", ex.getMessage());
        verify(placementService, never()).placeReservedFormalOrder(any(), any());
    }

    @Test
    void lo06_lojaDesactivadaNaoAceitaEncomendas() {
        OnlineShopSettings closed = shop();
        closed.setEnabled(false);
        when(settingsRepository.findByPublicCode("mercearia-baixa")).thenReturn(Optional.of(closed));

        assertEquals("Loja indisponível.", assertThrows(BusinessRuleException.class,
                () -> orderService.placeOrder("mercearia-baixa", request("Ana", "841234567", null))).getMessage());
        verify(placementService, never()).placeReservedFormalOrder(any(), any());
    }

    @Test
    void lo06_respostaSoComNumeroEstadoResumoETotal() {
        List<String> fields = Arrays.stream(OnlineShopOrderDTO.class.getRecordComponents())
                .map(component -> component.getName()).toList();
        assertEquals(List.of("number", "statusLabel", "lines", "totalAmount"), fields);
    }

    // ─── LO-07: armazém da encomenda e recusa sem stock ──────────────────────

    @Test
    void lo07_levantamentoUsaALojaEscolhidaQuandoTemStock() {
        openShopWithArroz();
        stock(PICKUP_A, "100");
        stock(PICKUP_B, "2");
        when(placementService.placeReservedFormalOrder(any(), any())).thenReturn(placed("ENC-1", "Pendente", "1"));

        orderService.placeOrder("mercearia-baixa", pickup(PICKUP_B, CART));

        assertEquals(PICKUP_B, placedOrder().warehouseId());
    }

    @Test
    void lo07_levantamentoSemStockNaLojaEscolhidaRecusaComOsArtigosENadaECriado() {
        openShopWithArroz();
        Product oleo = product(2L, "OLEO-1L", "Óleo 1 L");
        when(productLookup.findBySkuForCompany("OLEO-1L", COMPANY_ID)).thenReturn(Optional.of(oleo));
        stock(PICKUP_A, "100");
        stock(PICKUP_B, "1");
        stockLines.add(stockLine(oleo, PICKUP_B, "5"));
        reservations.add(new ReservedStock(2L, PICKUP_B, new BigDecimal("5")));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> orderService.placeOrder(
                "mercearia-baixa", pickup(PICKUP_B, List.of(
                        new OnlineShopOrderLineRequest("ARROZ-5KG", new BigDecimal("2")),
                        new OnlineShopOrderLineRequest("OLEO-1L", BigDecimal.ONE)))));

        assertEquals("Sem stock suficiente na loja escolhida para: Arroz 5 kg, Óleo 1 L.", ex.getMessage());
        verify(placementService, never()).placeReservedFormalOrder(any(), any());
        verify(comercialService, never()).createClient(any());
    }

    @Test
    void lo07_linhasRepetidasDoMesmoArtigoSomamAQuantidade() {
        openShopWithArroz();
        stock(PICKUP_A, "3");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> orderService.placeOrder(
                "mercearia-baixa", pickup(PICKUP_A, List.of(
                        new OnlineShopOrderLineRequest("ARROZ-5KG", new BigDecimal("2")),
                        new OnlineShopOrderLineRequest("ARROZ-5KG", new BigDecimal("2"))))));

        assertEquals("Sem stock suficiente na loja escolhida para: Arroz 5 kg.", ex.getMessage());
        verify(placementService, never()).placeReservedFormalOrder(any(), any());
    }

    @Test
    void lo07_entregaUsaAPrimeiraLojaDeLevantamentoComTodoOStock() {
        openShopWithArroz();
        stock(PICKUP_A, "5");
        reservations.add(new ReservedStock(1L, PICKUP_A, new BigDecimal("4")));
        stock(PICKUP_B, "2");
        when(placementService.placeReservedFormalOrder(any(), any())).thenReturn(placed("ENC-1", "Pendente", "1"));

        orderService.placeOrder("mercearia-baixa", delivery("Matola", "Rua X"));

        assertEquals(PICKUP_B, placedOrder().warehouseId(), "na loja A só 1 está livre; a B tem as 2");
    }

    @Test
    void lo07_entregaPrefereAPrimeiraLojaQuandoAmbasTemStock() {
        openShopWithArroz();
        stock(PICKUP_A, "2");
        stock(PICKUP_B, "50");
        when(placementService.placeReservedFormalOrder(any(), any())).thenReturn(placed("ENC-1", "Pendente", "1"));

        orderService.placeOrder("mercearia-baixa", delivery("Matola", "Rua X"));

        assertEquals(PICKUP_A, placedOrder().warehouseId());
    }

    @Test
    void lo07_entregaSemLojaComTodoOStockRecusaENadaECriado() {
        openShopWithArroz();
        stock(PICKUP_A, "1");
        stock(PICKUP_B, "3");
        reservations.add(new ReservedStock(1L, PICKUP_B, new BigDecimal("2")));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> orderService.placeOrder("mercearia-baixa", delivery("Matola", "Rua X")));

        assertEquals("Não há stock suficiente para entrega.", ex.getMessage());
        verify(placementService, never()).placeReservedFormalOrder(any(), any());
    }

    @Test
    void lo07_stockForaDasLojasDeLevantamentoNaoConta() {
        openShopWithArroz();
        stock(99L, "500");

        assertEquals("Não há stock suficiente para entrega.", assertThrows(BusinessRuleException.class,
                () -> orderService.placeOrder("mercearia-baixa", delivery("Matola", "Rua X"))).getMessage());
        verify(placementService, never()).placeReservedFormalOrder(any(), any());
    }

    @Test
    void lo07_servicoSemControloDeStockNaoPrecisaDeStock() {
        openShop();
        Product service = product(3L, "MONTAGEM", "Montagem");
        service.setStockTracked(false);
        when(productLookup.findBySkuForCompany("MONTAGEM", COMPANY_ID)).thenReturn(Optional.of(service));
        when(placementService.placeReservedFormalOrder(any(), any())).thenReturn(placed("ENC-1", "Pendente", "1"));

        orderService.placeOrder("mercearia-baixa", new PlaceOnlineShopOrderRequest("Ana", "841234567", null,
                OnlineShopFulfilment.ENTREGA, null, "Matola", "Rua X", null,
                List.of(new OnlineShopOrderLineRequest("MONTAGEM", BigDecimal.ONE))));

        assertEquals(PICKUP_A, placedOrder().warehouseId());
    }

    // ─── LO-08: taxa de entrega por zona ──────────────────────────────────────

    @Test
    void lo08_entregaAcrescentaALinhaDaTaxaDaZonaComOIvaNormal() {
        openShopWithArroz();
        stock(PICKUP_A, "10");
        when(placementService.placeReservedFormalOrder(any(), any())).thenReturn(placed("ENC-1", "Pendente", "1"));

        orderService.placeOrder("mercearia-baixa", delivery(" matola ", "Rua X"));

        List<CreateInvoiceLineRequest> lines = placedOrder().lines();
        assertEquals(2, lines.size(), "artigo + taxa de entrega");
        CreateInvoiceLineRequest fee = lines.get(1);
        assertEquals(900L, fee.productId());
        assertEquals(0, BigDecimal.ONE.compareTo(fee.quantity()));
        assertEquals(TaxRates.STANDARD_VAT, fee.taxRate(), "IVA da taxa normal");
        assertNull(fee.unitPrice(), "o preço é o do artigo da taxa, nunca do pedido");

        ArgumentCaptor<Product> product = ArgumentCaptor.forClass(Product.class);
        verify(productCommandService).saveProduct(product.capture());
        Product feeProduct = product.getValue();
        assertEquals("Taxa de entrega — Matola", feeProduct.getName());
        assertEquals("LOJA-ENTREGA-1-MATOLA", feeProduct.getSku());
        assertEquals(0, new BigDecimal("150.00").compareTo(feeProduct.getUnitPrice()), "a taxa da zona");
        assertEquals(ProductSaleType.SERVICE, feeProduct.getSaleType());
        assertFalse(feeProduct.isStockTracked(), "serviço sem stock");
        assertFalse(feeProduct.isPublishedOnline(), "nunca aparece na montra");
        assertNull(feeProduct.getTaxRate(), "sem taxa própria: IVA normal");
    }

    @Test
    void lo08_artigoExistenteDaZonaEReutilizadoEAcompanhaATaxa() {
        openShopWithArroz();
        stock(PICKUP_A, "10");
        Product existing = new Product();
        existing.setId(321L);
        existing.setSku("LOJA-ENTREGA-1-MATOLA");
        existing.setName("Taxa de entrega — Matola");
        existing.setUnitPrice(new BigDecimal("100.00"));
        existing.setStockTracked(false);
        when(productLookup.findBySkuForCompany("LOJA-ENTREGA-1-MATOLA", COMPANY_ID)).thenReturn(Optional.of(existing));
        when(placementService.placeReservedFormalOrder(any(), any())).thenReturn(placed("ENC-1", "Pendente", "1"));

        orderService.placeOrder("mercearia-baixa", delivery("Matola", "Rua X"));

        assertEquals(321L, placedOrder().lines().get(1).productId());
        assertEquals(0, new BigDecimal("150.00").compareTo(existing.getUnitPrice()), "preço igual à taxa actual");
        verify(productCommandService).saveProduct(existing);
    }

    @Test
    void lo08_artigoJaAlinhadoNaoEGravadoDeNovo() {
        Product existing = new Product();
        existing.setId(321L);
        existing.setName("Taxa de entrega — Matola");
        existing.setUnitPrice(new BigDecimal("150.00"));
        when(productLookup.findBySkuForCompany("LOJA-ENTREGA-1-MATOLA", COMPANY_ID)).thenReturn(Optional.of(existing));
        OnlineShopSettings shop = shop();

        Product result = new DeliveryFeeProductProvisioner(productLookup, productCommandService)
                .forZone(shop.getCompany(), shop.getDeliveryZones().get(0));

        assertEquals(existing, result);
        verify(productCommandService, never()).saveProduct(any());
    }

    @Test
    void lo08_levantamentoNaoTemTaxa() {
        openShopWithArroz();
        stock(PICKUP_A, "10");
        when(placementService.placeReservedFormalOrder(any(), any())).thenReturn(placed("ENC-1", "Pendente", "1"));

        orderService.placeOrder("mercearia-baixa", pickup(PICKUP_A, CART));

        assertEquals(1, placedOrder().lines().size());
        verify(productCommandService, never()).saveProduct(any());
    }

    @Test
    void lo08_skuDaZonaSemAcentosNemSimbolos() {
        assertEquals("LOJA-ENTREGA-7-MAPUTO-BAIXA-ZONA-1",
                DeliveryFeeProductProvisioner.sku(7L, " Maputo — Baixa, zona 1 "));
        assertEquals("LOJA-ENTREGA-7-ZONA", DeliveryFeeProductProvisioner.sku(7L, "***"));
        assertEquals("LOJA-ENTREGA-7-ZONA-ACUCAR", DeliveryFeeProductProvisioner.sku(7L, "Zona Açúcar"));
    }

    private void openShopWithArroz() {
        openShop();
        when(productLookup.findBySkuForCompany("ARROZ-5KG", COMPANY_ID)).thenReturn(Optional.of(onSale(1L, null)));
    }

    private CreateOrderRequest placedOrder() {
        ArgumentCaptor<CreateOrderRequest> order = ArgumentCaptor.forClass(CreateOrderRequest.class);
        verify(placementService).placeReservedFormalOrder(order.capture(), any());
        return order.getValue();
    }

    private static PlaceOnlineShopOrderRequest pickup(Long warehouseId, List<OnlineShopOrderLineRequest> lines) {
        return new PlaceOnlineShopOrderRequest("Ana", "841234567", null, OnlineShopFulfilment.LEVANTAMENTO,
                warehouseId, null, null, null, lines);
    }

    /** Stock of ARROZ-5KG (product 1) in a store. */
    private void stock(Long warehouseId, String quantity) {
        stockLines.add(stockLine(onSale(1L, null), warehouseId, quantity));
    }

    private static Stock stockLine(Product product, Long warehouseId, String quantity) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        Stock stock = new Stock();
        stock.setProduct(product);
        stock.setWarehouse(warehouse);
        stock.setQuantity(new BigDecimal(quantity));
        return stock;
    }

    private static Product product(Long id, String sku, String name) {
        Product product = onSale(id, null);
        product.setSku(sku);
        product.setName(name);
        return product;
    }

    private void openShop() {
        when(settingsRepository.findByPublicCode("mercearia-baixa")).thenReturn(Optional.of(shop()));
    }

    /** Open shop with plenty of ARROZ-5KG in both pickup stores (LO-06 scenarios). */
    private void stubShop() {
        openShop();
        stock(PICKUP_A, "100");
        stock(PICKUP_B, "100");
    }

    private static Product onSale(Long id, BigDecimal taxRate) {
        Product product = new Product();
        product.setId(id);
        product.setSku("ARROZ-5KG");
        product.setName("Arroz 5 kg");
        product.setUnitPrice(new BigDecimal("100.00"));
        product.setPublishedOnline(true);
        if (taxRate != null) {
            TaxRate rate = new TaxRate();
            rate.setRate(taxRate);
            product.setTaxRate(rate);
        }
        return product;
    }

    private static OrderDTO placed(String number, String statusLabel, String total) {
        OrderLineDTO line = new OrderLineDTO(5L, 1L, "Arroz 5 kg", "ARROZ-5KG", new BigDecimal("2"),
                new BigDecimal("100.00"), new BigDecimal("16"), new BigDecimal("200.00"), BigDecimal.ZERO, null, null,
                1, null, null, null, null, null);
        return new OrderDTO(9L, number, null, null, null, "Ana", null, null, new BigDecimal(total), "PENDING_APPROVAL",
                null, List.of(line), null, null, 0, null, OrderKind.FORMAL_ORDER, "Encomenda", statusLabel, null, null,
                null, null, null, false, PICKUP_A, null, null, null, null, null, 0L);
    }

    private static PlaceOnlineShopOrderRequest request(String name, String phone, String nuit) {
        return new PlaceOnlineShopOrderRequest(name, phone, nuit, OnlineShopFulfilment.LEVANTAMENTO, PICKUP_A,
                null, null, null, CART);
    }

    private static PlaceOnlineShopOrderRequest delivery(String zone, String address) {
        return new PlaceOnlineShopOrderRequest("Ana", "841234567", null, OnlineShopFulfilment.ENTREGA, null, zone,
                address, null, CART);
    }

    private static void assertRefused(String message, PlaceOnlineShopOrderRequest request) {
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> OnlineShopCheckoutRules.validate(request, shop()));
        assertEquals(message, ex.getMessage());
    }

    private static OnlineShopSettings shop() {
        Company company = new Company();
        company.setId(COMPANY_ID);
        OnlineShopSettings shop = new OnlineShopSettings();
        shop.setCompany(company);
        shop.setEnabled(true);
        shop.setPublicCode("mercearia-baixa");
        shop.getPickupWarehouseIds().add(PICKUP_A);
        shop.getPickupWarehouseIds().add(PICKUP_B);
        shop.getDeliveryZones().add(new OnlineShopDeliveryZone("Matola", new BigDecimal("150.00")));
        return shop;
    }
}
