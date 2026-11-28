package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.validation.ValidationPatterns;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopFulfilment;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopOrderLineRequest;
import mz.multicore.erp.modules.onlineshop.dto.PlaceOnlineShopOrderRequest;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopDeliveryZone;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Pure validation of the online shop checkout (docs/LOJA_ONLINE_SPEC.md §3.3, LO-05), without persistence: name and
 * Mozambican mobile phone required (normalised like the mobile payment: optional 258 prefix, 9 digits, 82–87), NUIT
 * optional but valid when given, pickup store among the shop's, delivery zone of the shop and address required for
 * home delivery, and a non-empty cart.
 */
final class OnlineShopCheckoutRules {

    static final int NAME_MAX_LENGTH = 150;
    static final int ADDRESS_MAX_LENGTH = 255;
    static final int NOTES_MAX_LENGTH = 500;
    private static final Pattern MOBILE_MZ = Pattern.compile("8[2-7]\\d{7}");

    private OnlineShopCheckoutRules() {
    }

    static OnlineShopCheckout validate(PlaceOnlineShopOrderRequest request, OnlineShopSettings shop) {
        if (request == null) {
            throw new BusinessRuleException("Preencha os dados da encomenda.");
        }
        String name = required(request.customerName(), "Indique o seu nome.");
        maxLength(name, NAME_MAX_LENGTH, "O nome tem no máximo " + NAME_MAX_LENGTH + " caracteres.");
        String phone = mobilePhone(request.phone());
        String nuit = nuit(request.nuit());
        String notes = optional(request.notes());
        maxLength(notes, NOTES_MAX_LENGTH, "As notas têm no máximo " + NOTES_MAX_LENGTH + " caracteres.");
        List<OnlineShopOrderLineRequest> lines = lines(request.lines());

        OnlineShopFulfilment fulfilment = request.fulfilment();
        if (fulfilment == null) {
            throw new BusinessRuleException("Escolha levantamento na loja ou entrega ao domicílio.");
        }
        if (fulfilment == OnlineShopFulfilment.LEVANTAMENTO) {
            Long pickup = pickupWarehouse(request.pickupWarehouseId(), shop);
            return new OnlineShopCheckout(name, phone, nuit, fulfilment, pickup, null, null, notes, lines);
        }
        OnlineShopDeliveryZone zone = deliveryZone(request.deliveryZone(), shop);
        String address = required(request.deliveryAddress(), "Indique a morada de entrega.");
        maxLength(address, ADDRESS_MAX_LENGTH, "A morada tem no máximo " + ADDRESS_MAX_LENGTH + " caracteres.");
        return new OnlineShopCheckout(name, phone, nuit, fulfilment, null, zone, address, notes, lines);
    }

    /** Same normalisation as the mobile payment (258 prefix dropped, 9 digits), restricted to mobile networks. */
    static String mobilePhone(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessRuleException("O número de telemóvel é obrigatório.");
        }
        String phone = ValidationPatterns.cleanPhoneMozambique(raw);
        if (!MOBILE_MZ.matcher(phone).matches()) {
            throw new BusinessRuleException(
                    "Indique um telemóvel moçambicano com 9 dígitos, começado por 82 a 87 (ex.: 84 123 4567).");
        }
        return phone;
    }

    private static String nuit(String raw) {
        String nuit = optional(raw);
        if (nuit == null) {
            return null;
        }
        nuit = nuit.replace(" ", "").replace("-", "");
        if (!ValidationPatterns.isValidNuit(nuit)) {
            throw new BusinessRuleException("O NUIT indicado não é válido (9 dígitos).");
        }
        return nuit;
    }

    private static Long pickupWarehouse(Long warehouseId, OnlineShopSettings shop) {
        if (warehouseId == null) {
            throw new BusinessRuleException("Escolha a loja de levantamento.");
        }
        if (!shop.getPickupWarehouseIds().contains(warehouseId)) {
            throw new BusinessRuleException("A loja de levantamento escolhida não está disponível.");
        }
        return warehouseId;
    }

    private static OnlineShopDeliveryZone deliveryZone(String raw, OnlineShopSettings shop) {
        String name = required(raw, "Escolha a zona de entrega.").toLowerCase(Locale.ROOT);
        return shop.getDeliveryZones().stream()
                .filter(zone -> zone.getName().toLowerCase(Locale.ROOT).equals(name))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("A zona de entrega escolhida não existe nesta loja."));
    }

    private static List<OnlineShopOrderLineRequest> lines(List<OnlineShopOrderLineRequest> raw) {
        List<OnlineShopOrderLineRequest> lines = new ArrayList<>();
        for (OnlineShopOrderLineRequest line : raw == null ? List.<OnlineShopOrderLineRequest>of() : raw) {
            String sku = line == null ? null : optional(line.sku());
            if (sku == null) {
                throw new BusinessRuleException("Indique o artigo de cada linha do carrinho.");
            }
            if (line.quantity() == null || line.quantity().signum() <= 0) {
                throw new BusinessRuleException("A quantidade do artigo " + sku + " tem de ser maior que zero.");
            }
            lines.add(new OnlineShopOrderLineRequest(sku, line.quantity()));
        }
        if (lines.isEmpty()) {
            throw new BusinessRuleException("O carrinho está vazio.");
        }
        return List.copyOf(lines);
    }

    private static String required(String raw, String message) {
        String value = optional(raw);
        if (value == null) {
            throw new BusinessRuleException(message);
        }
        return value;
    }

    private static String optional(String raw) {
        String value = raw == null ? "" : raw.trim();
        return value.isEmpty() ? null : value;
    }

    private static void maxLength(String value, int max, String message) {
        if (value != null && value.length() > max) {
            throw new BusinessRuleException(message);
        }
    }
}
