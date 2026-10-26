package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.pricing.TaxRates;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Controlador de operações do carrinho de vendas do POS:
 * totalização, impostos, alteração de quantidades e gestão de linhas.
 */
public class PosCartController {

    private final POSPanel owner;

    public PosCartController(POSPanel owner) {
        this.owner = owner;
    }

    public void removeFromCart() {
        int selectedView = owner.cartTable.getSelectedRow();
        if (selectedView < 0) {
            owner.showPosNotice(FeedbackType.WARNING, "Seleccione um artigo", "Escolha uma linha do carrinho para remover.");
            return;
        }
        int selected = owner.cartTable.convertRowIndexToModel(selectedView);
        owner.cartItems.remove(selected);
        updateCartTotal(Math.min(selected, owner.cartItems.size() - 1));
    }

    public void editSelectedCartQuantity() {
        int selectedView = owner.cartTable.getSelectedRow();
        if (selectedView < 0) {
            owner.showPosNotice(FeedbackType.WARNING, "Seleccione um artigo", "Escolha uma linha do carrinho para alterar a quantidade.");
            return;
        }
        int selected = owner.cartTable.convertRowIndexToModel(selectedView);
        if (selected >= owner.cartItems.size()) return;

        POSPanel.CartItem item = owner.cartItems.get(selected);
        JTextField quantityField = new JTextField(item.qty.stripTrailingZeros().toPlainString());
        UIHelper.styleTextField(quantityField);
        JPanel form = UIHelper.createDialogForm("Artigo:", readOnlyField(item.product.name()), "Quantidade:", quantityField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Alterar Quantidade",
                "fas-sort-numeric-up", "Actualize a quantidade da linha seleccionada", form)
                .setConfirmButton("Actualizar", "fas-check")
                .setOnSave(() -> {
                    BigDecimal quantity;
                    try {
                        quantity = new BigDecimal(quantityField.getText().trim().replace(',', '.'));
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Introduza uma quantidade válida.");
                    }
                    if (quantity.signum() <= 0) throw new IllegalArgumentException("A quantidade deve ser maior do que zero.");
                    item.qty = quantity;
                })
                .showDialog();
        if (confirmed) updateCartTotal(selected);
    }

    public void updateCartTotal() {
        int selected = owner.cartTable == null || owner.cartTable.getSelectedRow() < 0
                ? -1 : owner.cartTable.convertRowIndexToModel(owner.cartTable.getSelectedRow());
        updateCartTotal(selected);
    }

    public void updateCartTotal(int preferredModelRow) {
        BigDecimal net = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (POSPanel.CartItem item : owner.cartItems) {
            net = net.add(item.getSubtotal());
            tax = tax.add(item.getTax());
            total = total.add(item.getTotal());
        }
        owner.totalLabel.setText(String.format("%,.2f MT", total));
        if (owner.subtotalValueLabel != null) owner.subtotalValueLabel.setText(String.format("%,.2f MT", net));
        if (owner.ivaValueLabel != null) owner.ivaValueLabel.setText(String.format("%,.2f MT", tax));
        owner.rebuildCartRows();
        refreshCartView();
        if (owner.cartItemCountLabel != null) {
            BigDecimal units = owner.cartItems.stream().map(item -> item.qty).reduce(BigDecimal.ZERO, BigDecimal::add);
            String formatted = units.stripTrailingZeros().toPlainString();
            owner.cartItemCountLabel.setText("• " + formatted + (BigDecimal.ONE.compareTo(units) == 0 ? " artigo" : " artigos"));
        }
        owner.catalogController.selectAndRevealCartRow(preferredModelRow);
    }

    public void refreshCartView() {
        if (owner.cartCenter == null) return;
        CardLayout layout = (CardLayout) owner.cartCenter.getLayout();
        layout.show(owner.cartCenter, owner.cartItems.isEmpty() ? "empty" : "table");
    }

    public static String ivaCellLabel(POSPanel.CartItem item) {
        BigDecimal rate = effectiveTaxRate(item.product.taxRate());
        if (rate.compareTo(BigDecimal.ZERO) == 0) return "Isento";
        String pct = rate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString();
        return String.format("%,.2f MT (%s%%)", item.getTax(), pct);
    }

    public static BigDecimal effectiveTaxRate(BigDecimal productRate) {
        return productRate != null ? productRate : TaxRates.STANDARD_VAT;
    }

    public static JPanel buildEmptyStatePanel() {
        JPanel s = new JPanel(new GridBagLayout());
        s.setOpaque(false);
        JPanel i = new JPanel();
        i.setLayout(new BoxLayout(i, BoxLayout.Y_AXIS));
        i.setOpaque(false);
        JLabel icon = new JLabel(UIHelper.icon("fas-shopping-cart", 48, UIHelper.TEXT_MUTED));
        icon.setAlignmentX(0.5f);
        JLabel title = new JLabel("Carrinho vazio");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        title.setForeground(UIHelper.TEXT_MUTED);
        title.setAlignmentX(0.5f);
        JLabel hint = new JLabel("Leia um código de barras ou adicione um artigo.");
        hint.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        hint.setForeground(UIHelper.TEXT_MUTED);
        hint.setAlignmentX(0.5f);
        i.add(icon);
        i.add(Box.createRigidArea(new Dimension(0, 12)));
        i.add(title);
        i.add(Box.createRigidArea(new Dimension(0, 4)));
        i.add(hint);
        s.add(i);
        return s;
    }

    private static JTextField readOnlyField(String value) {
        JTextField field = new JTextField(value == null ? "" : value);
        UIHelper.styleTextField(field);
        field.setEditable(false);
        return field;
    }
}
