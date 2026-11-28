package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopDeliveryZoneDTO;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopDeliveryZone;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Pure validation of the online shop settings (docs/LOJA_ONLINE_SPEC.md §3.1, LO-01), without persistence. */
final class OnlineShopSettingsRules {

    static final int CODE_MAX_LENGTH = 50;
    static final int ZONE_NAME_MAX_LENGTH = 100;
    private static final Pattern CODE = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    private OnlineShopSettingsRules() {
    }

    static void requireAdmin(boolean admin) {
        if (!admin) {
            throw new BusinessRuleException("Só o administrador gere a loja online.");
        }
    }

    /** Trims and lower-cases; required when the shop is enabled, validated whenever given. */
    static String publicCode(String raw, boolean enabled) {
        String code = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (code.isEmpty()) {
            if (enabled) {
                throw new BusinessRuleException("Indique o código público da loja.");
            }
            return null;
        }
        if (code.length() > CODE_MAX_LENGTH || !CODE.matcher(code).matches()) {
            throw new BusinessRuleException(
                    "O código público só pode ter letras minúsculas, números e hífen (ex.: mercearia-baixa).");
        }
        return code;
    }

    /** Distinct pickup stores, all among the company's sales warehouses; at least one when enabled. */
    static List<Long> pickupWarehouses(List<Long> raw, Set<Long> salesWarehouseIds, boolean enabled) {
        Set<Long> ids = new LinkedHashSet<>(raw == null ? List.of() : raw);
        ids.remove(null);
        if (ids.isEmpty() && enabled) {
            throw new BusinessRuleException("Escolha pelo menos uma loja de levantamento.");
        }
        if (!salesWarehouseIds.containsAll(ids)) {
            throw new BusinessRuleException("A loja de levantamento escolhida não é um armazém de venda desta empresa.");
        }
        return new ArrayList<>(ids);
    }

    /** Named zones with a fee >= 0 (2 decimals), names unique ignoring case. */
    static List<OnlineShopDeliveryZone> deliveryZones(List<OnlineShopDeliveryZoneDTO> raw) {
        List<OnlineShopDeliveryZone> zones = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (OnlineShopDeliveryZoneDTO zone : raw == null ? List.<OnlineShopDeliveryZoneDTO>of() : raw) {
            String name = zone == null || zone.name() == null ? "" : zone.name().trim();
            if (name.isEmpty()) {
                throw new BusinessRuleException("Indique o nome da zona de entrega.");
            }
            if (name.length() > ZONE_NAME_MAX_LENGTH) {
                throw new BusinessRuleException("O nome da zona de entrega tem no máximo "
                        + ZONE_NAME_MAX_LENGTH + " caracteres.");
            }
            BigDecimal fee = zone.fee();
            if (fee == null) {
                throw new BusinessRuleException("Indique a taxa de entrega da zona " + name + ".");
            }
            if (fee.signum() < 0) {
                throw new BusinessRuleException("A taxa de entrega da zona " + name + " não pode ser negativa.");
            }
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw new BusinessRuleException("A zona de entrega " + name + " está repetida.");
            }
            zones.add(new OnlineShopDeliveryZone(name, fee.setScale(2, RoundingMode.HALF_UP)));
        }
        return zones;
    }

    static int autoCancelDays(Integer raw) {
        if (raw == null) {
            return OnlineShopSettings.DEFAULT_AUTO_CANCEL_DAYS;
        }
        if (raw < 1) {
            throw new BusinessRuleException("O prazo de cancelamento automático tem de ser de pelo menos 1 dia.");
        }
        return raw;
    }

    static String contactPhone(String raw) {
        String phone = raw == null ? "" : raw.trim();
        if (phone.length() > 30) {
            throw new BusinessRuleException("O telefone de contacto tem no máximo 30 caracteres.");
        }
        return phone.isEmpty() ? null : phone;
    }
}
