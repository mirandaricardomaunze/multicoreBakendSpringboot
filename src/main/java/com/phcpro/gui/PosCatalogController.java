package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.*;
import com.phcpro.modules.comercial.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.Locale;

/** Pesquisa e apresentação do catálogo, e sincronização visual do carrinho. */
final class PosCatalogController {
    private final POSPanel owner;
    PosCatalogController(POSPanel owner) { this.owner = owner; }

    public void filterClients(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        owner.filteredClients = owner.clientsList.stream()
                .filter(c -> q.isEmpty()
                        || (c.name() != null && c.name().toLowerCase().contains(q))
                        || (c.taxId() != null && c.taxId().toLowerCase().contains(q)))
                .toList();
        owner.clientCombo.removeAllItems();
        for (ClientDTO c : owner.filteredClients) {
            owner.clientCombo.addItem(c.name() + " (" + c.taxId() + ")");
        }
    }

    public void filterProducts(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        owner.filteredProducts = owner.productsList.stream()
                .filter(p -> q.isEmpty()
                        || (p.sku() != null && p.sku().toLowerCase().contains(q))
                        || (p.reference() != null && p.reference().toLowerCase().contains(q))
                        || (p.barcode() != null && p.barcode().toLowerCase().contains(q))
                        || (p.name() != null && p.name().toLowerCase().contains(q)))
                .toList();
        rebuildProductGrid();
    }

    /** Reconstrói o grid de cards de produto a partir de {@link #owner.filteredProducts}. */
    public void rebuildProductGrid() {
        if (owner.productGrid == null) return;
        owner.productGrid.removeAll();
        if (owner.filteredProducts.isEmpty()) {
            JLabel empty = new JLabel(owner.productsList.isEmpty()
                    ? "Não há produtos cadastrados."
                    : "Nenhum produto corresponde à pesquisa.");
            empty.setForeground(UIHelper.TEXT_MUTED);
            empty.setBorder(new EmptyBorder(20, 8, 20, 8));
            owner.productGrid.add(empty);
        } else {
            for (ProductDTO p : owner.filteredProducts) {
                owner.productGrid.add(productCard(p));
            }
        }
        owner.productGrid.revalidate();
        owner.productGrid.repaint();
    }

    /** Card clicável de um produto: imagem (ou marcador) + nome + preço. Clique adiciona ao carrinho. */
    private JComponent productCard(ProductDTO p) {
        ModernPanel card = new ModernPanel(12);
        card.setLayout(new BorderLayout(0, 6));
        card.setBackground(UIHelper.BG_CARD);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel image = new JLabel("", SwingConstants.CENTER);
        image.setPreferredSize(new Dimension(120, 84));
        javax.swing.Icon img = UIHelper.imageIconFromBytes(p.image(), 120, 84);
        if (img != null) {
            image.setIcon(img);
        } else {
            image.setIcon(UIHelper.icon("fas-box", 40, UIHelper.TEXT_MUTED));
        }
        card.add(image, BorderLayout.NORTH);

        JLabel name = new JLabel("<html><div style='text-align:center'>" + escapeHtml(p.name()) + "</div></html>", SwingConstants.CENTER);
        name.setForeground(UIHelper.TEXT_LIGHT);
        name.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        card.add(name, BorderLayout.CENTER);

        JLabel price = new JLabel(String.format("%,.2f MT", p.unitPrice()), SwingConstants.CENTER);
        price.setForeground(UIHelper.ACCENT_BLUE);
        price.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        card.add(price, BorderLayout.SOUTH);

        card.setToolTipText(productLabel(p));
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { addProductToCart(p); }
        });
        return card;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Legenda (esquerda) do bloco de discriminação Subtotal/IVA. */
    public void addProductToCart(ProductDTO product) {
        if (owner.activeSession == null) {
            JOptionPane.showMessageDialog(owner,
                    "Não é possível adicionar artigos sem caixa aberta.\nClique em \"Abrir Caixa\" primeiro.",
                    "Caixa Fechada", JOptionPane.WARNING_MESSAGE);
            return;
        }
        for (POSPanel.CartItem it : owner.cartItems) {
            if (it.serial == null && it.product.id().equals(product.id())) {
                it.qty = it.qty.add(BigDecimal.ONE);
                owner.updateCartTotal();
                return;
            }
        }
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner,
                () -> owner.promotionApiClient.bestPromotion(
                        companyId, product.id(), product.categoryId(), BigDecimal.ONE),
                promo -> {
                    BigDecimal discount = promo.map(p -> p.discountPercent()).orElse(BigDecimal.ZERO);
                    POSPanel.CartItem item = new POSPanel.CartItem(product, BigDecimal.ONE, discount, null, null);
                    item.note = promo.map(p -> "Promo: " + p.name()).orElse("-");
                    owner.cartItems.add(item);
                    owner.updateCartTotal();
                }, error -> JOptionPane.showMessageDialog(owner,
                        "Não foi possível consultar promoções: " + error.getMessage(),
                        "Erro de ligação", JOptionPane.ERROR_MESSAGE));
    }

    /** Reconstrói todas as linhas da tabela do carrinho a partir de {@link #owner.cartItems}. */
    public void rebuildCartRows() {
        owner.cartModel.setRowCount(0);
        for (POSPanel.CartItem item : owner.cartItems) {
            owner.cartModel.addRow(new Object[]{
                    item.product.name(),
                    String.format("%.2f MT", item.product.unitPrice()),
                    item.qty.stripTrailingZeros().toPlainString(),
                    item.discount.stripTrailingZeros().toPlainString() + "%",
                    item.note != null ? item.note : "-",
                    String.format("%,.2f MT", item.getSubtotal()),
                    POSPanel.ivaCellLabel(item),
                    String.format("%,.2f MT", item.getTotal())
            });
        }
    }

    String productLabel(ProductDTO p) {
        String code = p.barcode() != null && !p.barcode().isBlank()
                ? p.barcode()
                : p.reference() != null && !p.reference().isBlank() ? p.reference() : p.sku();
        return code + " - " + p.name();
    }

    public void createClient() {
        JTextField nameField = new JTextField();
        JTextField taxIdField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField addressField = new JTextField();
        for (JTextField field : new JTextField[]{nameField, taxIdField, emailField, addressField}) {
            UIHelper.styleTextField(field);
        }
        JPanel form = UIHelper.createDialogForm("Nome:", nameField, "NUIT / NIF:", taxIdField,
                "Email:", emailField, "Endereço:", addressField);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Novo Cliente", "fas-address-book",
                "Cadastro rápido de cliente", form).showDialog();
        if (!confirmed) return;
        String name = nameField.getText().trim();
        String taxId = taxIdField.getText().trim();
        String email = emailField.getText().trim();
        String address = addressField.getText().trim();
        if (name.isEmpty() || taxId.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Nome, NUIT e Email são obrigatórios.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        UIHelper.runWithProgress(owner, "A criar cliente…", () -> {
            ClientDTO created = owner.comercialApiClient.createClient(name, taxId, email, address);
            return new CreatedClient(created, owner.comercialApiClient.getAllClients());
        }, result -> {
            owner.clientsList = result.clients();
            owner.clientSearchField.setText(result.created().name());
            filterClients(owner.clientSearchField.getText());
            for (int i = 0; i < owner.filteredClients.size(); i++) {
                if (owner.filteredClients.get(i).id().equals(result.created().id())) {
                    owner.clientCombo.setSelectedIndex(i);
                    break;
                }
            }
            JOptionPane.showMessageDialog(owner, "Cliente '" + result.created().name() + "' criado.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        }, error -> JOptionPane.showMessageDialog(owner,
                "Não foi possível criar o cliente: " + error.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE));
    }

    private record CreatedClient(ClientDTO created, java.util.List<ClientDTO> clients) {}

}
