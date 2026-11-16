package mz.multicore.erp.gui.sector;

import mz.multicore.erp.gui.components.UIHelper;

import java.awt.Color;
import java.util.Map;

/**
 * Icon and colour of each activity profile and capability (SA-C01). Single source so the profile screen,
 * summaries and future sector screens show the same signal for the same sector.
 */
final class SectorVisuals {

    record Visual(String iconCode, Color color) {}

    private static final Visual FALLBACK = new Visual("fas-store", UIHelper.ACCENT_BLUE);

    private static final Map<String, Visual> PROFILES = Map.ofEntries(
            Map.entry("GENERAL_RETAIL", new Visual("fas-store", UIHelper.ACCENT_BLUE)),
            Map.entry("GROCERY", new Visual("fas-shopping-basket", UIHelper.APPROVED_GREEN)),
            Map.entry("WHOLESALE", new Visual("fas-warehouse", UIHelper.ACCENT_CYAN)),
            Map.entry("BUTCHER", new Visual("fas-drumstick-bite", UIHelper.REJECTED_RED)),
            Map.entry("BAKERY", new Visual("fas-bread-slice", UIHelper.PENDING_YELLOW)),
            Map.entry("BUILDING_MATERIALS", new Visual("fas-hard-hat", UIHelper.ACCENT_ORANGE)),
            Map.entry("BLOCK_FACTORY", new Visual("fas-th-large", UIHelper.ACCENT_ORANGE)),
            Map.entry("HARDWARE", new Visual("fas-tools", UIHelper.SECONDARY)),
            Map.entry("FRUIT_VEG", new Visual("fas-apple-alt", UIHelper.APPROVED_GREEN)),
            Map.entry("BEVERAGES", new Visual("fas-wine-bottle", UIHelper.ACCENT)),
            Map.entry("GAS", new Visual("fas-fire", UIHelper.ACCENT_ORANGE)),
            Map.entry("FASHION", new Visual("fas-tshirt", UIHelper.ACCENT_PINK)),
            Map.entry("AGRO", new Visual("fas-seedling", UIHelper.APPROVED_GREEN)),
            Map.entry("REPACKING", new Visual("fas-box-open", UIHelper.ACCENT_SKY)),
            Map.entry("SERVICES", new Visual("fas-handshake", UIHelper.ACCENT_BLUE)));

    private static final Map<String, String> CAPABILITY_ICONS = Map.ofEntries(
            Map.entry("MULTI_UNIT_SALES", "fas-ruler-combined"),
            Map.entry("PRICE_LISTS", "fas-tags"),
            Map.entry("WEIGHED_SALES", "fas-balance-scale"),
            Map.entry("PERISHABLES", "fas-calendar-times"),
            Map.entry("PRODUCTION", "fas-industry"),
            Map.entry("MARKDOWNS", "fas-percentage"),
            Map.entry("CUSTOM_ORDERS", "fas-clipboard-list"),
            Map.entry("ADVANCE_PAYMENTS", "fas-hand-holding-usd"),
            Map.entry("PENDING_PICKUP", "fas-people-carry"),
            Map.entry("DELIVERIES", "fas-truck"),
            Map.entry("CUSTOMER_PROJECTS", "fas-building"),
            Map.entry("BULK_STOCK", "fas-mountain"),
            Map.entry("CONSIGNMENT", "fas-route"),
            Map.entry("RETURNABLES", "fas-recycle"),
            Map.entry("VARIANTS", "fas-layer-group"));

    private SectorVisuals() {
    }

    static Visual profile(String code) {
        return PROFILES.getOrDefault(code, FALLBACK);
    }

    static String capabilityIcon(String code) {
        return CAPABILITY_ICONS.getOrDefault(code, "fas-check-circle");
    }
}
