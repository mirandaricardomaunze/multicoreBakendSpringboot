package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Freight by delivery zone (SA-C08). Each zone is a service product ("FRT-" code) so freight is invoiced with
 * VAT, appears in SAF-T and can be priced per client by price lists. The operator picks the zone and the
 * line is added to the document; managers can create a new zone from the same dialog.
 */
final class FreightZoneDialog {

    static final String SKU_PREFIX = "FRT-";

    private FreightZoneDialog() {
    }

    static boolean isFreightZone(ProductDTO product) {
        return product != null && product.sku() != null && product.sku().startsWith(SKU_PREFIX);
    }

    static void show(JComponent parent, ComercialApiClient api, Consumer<ProductDTO> onZone) {
        UIHelper.loadAsync(parent, api::getAllProducts, products -> open(parent, api,
                        products.stream().filter(FreightZoneDialog::isFreightZone)
                                .sorted(Comparator.comparing(ProductDTO::name)).toList(), onZone),
                error -> ToastManager.show(parent, FeedbackType.ERROR, "Não foi possível carregar as zonas: " + error.getMessage()));
    }

    private static void open(JComponent parent, ComercialApiClient api, List<ProductDTO> zones, Consumer<ProductDTO> onZone) {
        boolean manager = !"SELLER".equalsIgnoreCase(CurrentUserContext.getRole());
        SearchableComboBox<ProductDTO> zoneCombo = new SearchableComboBox<>("Pesquisar zona",
                z -> z.name() + "  ·  " + UIHelper.formatMzn(z.unitPrice()), z -> z.name() + " " + z.sku());
        zoneCombo.setItems(zones);
        JTextField newName = new JTextField();
        MoneyField newPrice = new MoneyField();
        UIHelper.styleTextField(newName);
        newName.setEnabled(manager);
        newPrice.setEnabled(manager);
        JLabel hint = new JLabel("<html>" + (zones.isEmpty() ? "Ainda não há zonas de frete. " : "")
                + (manager ? "Para uma zona nova, escreva o nome e o preço (sem IVA, como os outros preços)."
                : "Só a gestão cria zonas novas.") + " O frete entra na fatura como serviço, com IVA.</html>");
        hint.setForeground(UIHelper.TEXT_MUTED);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{
                "Zona existente:", zoneCombo,
                "Ou zona nova:", newName,
                "Preço da zona nova (MT):", newPrice,
                "", hint});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Adicionar Frete", "fas-truck",
                "Frete por zona de entrega", form);
        dialog.setConfirmButton("Adicionar à linha", "fas-plus");
        ProductDTO[] result = new ProductDTO[1];
        dialog.setOnSaveAsync(() -> {
            String name = newName.getText().trim();
            if (name.isEmpty()) {
                ProductDTO zone = zoneCombo.selectedValue();
                if (zone == null) throw new IllegalArgumentException("Escolha a zona ou indique uma zona nova.");
                return () -> { result[0] = zone; return null; };
            }
            BigDecimal price = newPrice.value();
            if (price.signum() <= 0) throw new IllegalArgumentException("O preço do frete tem de ser maior que zero.");
            String label = "Frete — " + name;
            String sku = SKU_PREFIX + slug(name);
            return () -> {
                result[0] = api.createProduct(sku, null, null, label, price, BigDecimal.ZERO, BigDecimal.ZERO, 1, 1, 1,
                        null, "SERVICE", false, null, "Frete para a zona " + name, null, null, null, null);
                return null;
            };
        });
        if (dialog.showDialog() && result[0] != null) onZone.accept(result[0]);
    }

    static String slug(String name) {
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = plain.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (slug.isEmpty()) slug = "ZONA";
        return slug.length() > 24 ? slug.substring(0, 24) : slug;
    }
}
