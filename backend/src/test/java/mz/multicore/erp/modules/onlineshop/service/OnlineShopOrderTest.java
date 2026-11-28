package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopFulfilment;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopOrderLineRequest;
import mz.multicore.erp.modules.onlineshop.dto.PlaceOnlineShopOrderRequest;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopDeliveryZone;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Harness LO-05 (docs/LOJA_ONLINE_SPEC.md §3.3): the checkout requires the name and a Mozambican mobile phone
 * (validated like the mobile payment), accepts an optional but valid NUIT, and needs a pickup store of the shop for
 * {@code LEVANTAMENTO} or a zone of the shop and an address for {@code ENTREGA}.
 */
class OnlineShopOrderTest {

    private static final Long PICKUP_A = 10L;
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
        assertRefused("O nome tem no máximo 150 caracteres.", request("A".repeat(151), "841234567", null));
        assertRefused("A morada tem no máximo 255 caracteres.", delivery("Matola", "R".repeat(256)));
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
        OnlineShopSettings shop = new OnlineShopSettings();
        shop.setEnabled(true);
        shop.setPublicCode("mercearia-baixa");
        shop.getPickupWarehouseIds().add(PICKUP_A);
        shop.getDeliveryZones().add(new OnlineShopDeliveryZone("Matola", new BigDecimal("150.00")));
        return shop;
    }
}
