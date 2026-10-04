package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.architecture.paging.PageResponse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Pesquisa e apresentação do catálogo, e sincronização visual do carrinho. */
final class PosCatalogController {
    static final int CARD_IMAGE_WIDTH = 80;
    static final int CARD_IMAGE_HEIGHT = 42;
    static final int CARD_PADDING = 5;
    static final int CARD_CONTENT_GAP = 2;
    static final int PAGE_SIZE = 36;
    private final POSPanel owner;
    private final Timer searchTimer;
    private ModernButton previousButton;
    private ModernButton nextButton;
    private JLabel pageLabel;
    private int requestSequence;
    PosCatalogController(POSPanel owner) { this.owner = owner; }

    {
        searchTimer = new Timer(300, e -> loadCatalogPage(0));
        searchTimer.setRepeats(false);
    }

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

    JPanel buildPaginationBar() {
        previousButton = new ModernButton("Anterior", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
        previousButton.setIcon(UIHelper.icon("fas-chevron-left", 12, Color.WHITE));
        previousButton.setForeground(Color.WHITE);
        previousButton.setPreferredSize(new Dimension(105, UIHelper.FORM_CONTROL_HEIGHT));

        nextButton = new ModernButton("Próximo", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
        nextButton.setIcon(UIHelper.icon("fas-chevron-right", 12, Color.WHITE));
        nextButton.setForeground(Color.WHITE);
        nextButton.setPreferredSize(new Dimension(105, UIHelper.FORM_CONTROL_HEIGHT));

        pageLabel = new JLabel("Página 1", SwingConstants.CENTER);
        pageLabel.setForeground(UIHelper.TEXT_MUTED);
        pageLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        previousButton.addActionListener(e -> loadCatalogPage(currentPage() - 1));
        nextButton.addActionListener(e -> loadCatalogPage(currentPage() + 1));
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(8, 0, 0, 0));
        bar.add(previousButton, BorderLayout.WEST);
        bar.add(pageLabel, BorderLayout.CENTER);
        bar.add(nextButton, BorderLayout.EAST);
        return bar;
    }

    void scheduleCatalogReload() { searchTimer.restart(); }

    void loadCatalogPage(int requestedPage) {
        int page = Math.max(0, requestedPage);
        int sequence = ++requestSequence;
        setNavigationEnabled(false);
        String query = owner.productSearchField != null ? owner.productSearchField.getText() : "";
        Long warehouseId = owner.getSelectedWarehouseId();
        UIHelper.loadAsync(owner,
                () -> owner.comercialApiClient.getPOSCatalogPage(query, !owner.showAllProducts, warehouseId, page, PAGE_SIZE),
                result -> { if (sequence == requestSequence) applyCatalogPage(result); },
                error -> { if (sequence == requestSequence) {
                    setNavigationEnabled(true);
                    owner.showPosLoadError("catálogo de produtos", error);
                }});
    }

    private void applyCatalogPage(PageResponse<POSCatalogItemDTO> page) {
        owner.productsList = page.items().stream().map(POSCatalogItemDTO::product).toList();
        owner.filteredProducts = owner.productsList;
        owner.sellableProductIds = page.items().stream().filter(POSCatalogItemDTO::sellable)
                .map(item -> item.product().id()).collect(Collectors.toSet());
        owner.productStockQuantities.clear();
        for (POSCatalogItemDTO item : page.items()) {
            if (item.stockQuantity() != null && item.product() != null && item.product().id() != null) {
                owner.productStockQuantities.put(item.product().id(), item.stockQuantity());
            }
        }
        if (pageLabel != null) pageLabel.setText(page.totalPages() == 0 ? "Sem resultados"
                : "Página " + (page.page() + 1) + " de " + page.totalPages() + " · " + page.totalElements() + " produtos");
        if (pageLabel != null) pageLabel.putClientProperty("catalogPage", page.page());
        if (previousButton != null) previousButton.setEnabled(page.hasPrevious());
        if (nextButton != null) nextButton.setEnabled(page.hasNext());
        rebuildProductGrid();
    }

    private int currentPage() {
        Object value = pageLabel == null ? null : pageLabel.getClientProperty("catalogPage");
        return value instanceof Integer page ? page : 0;
    }

    private void setNavigationEnabled(boolean enabled) {
        if (previousButton != null) previousButton.setEnabled(enabled);
        if (nextButton != null) nextButton.setEnabled(enabled);
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
        boolean sellable = owner.isProductSellable(p);
        ModernPanel card = new ModernPanel(10);
        card.setLayout(new BorderLayout(0, CARD_CONTENT_GAP));
        card.setBackground(sellable ? UIHelper.BG_CARD : UIHelper.ROW_ALT);
        card.setBorder(new EmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING));
        card.setCursor(Cursor.getPredefinedCursor(sellable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

        JLabel image = new JLabel("", SwingConstants.CENTER);
        image.setPreferredSize(new Dimension(CARD_IMAGE_WIDTH, CARD_IMAGE_HEIGHT));
        javax.swing.Icon img = UIHelper.imageIconFromBytes(p.image(), CARD_IMAGE_WIDTH, CARD_IMAGE_HEIGHT);
        if (img != null) {
            image.setIcon(img);
        } else {
            String iconCode = categoryIcon(p.categoryName());
            Color iconColor = sellable ? categoryColor(p.categoryName()) : UIHelper.TEXT_MUTED;
            image.setIcon(UIHelper.icon(iconCode, 22, iconColor));
        }
        card.add(image, BorderLayout.NORTH);

        JLabel name = new JLabel("<html><div style='text-align:center'>" + escapeHtml(p.name()) + "</div></html>", SwingConstants.CENTER);
        name.setForeground(sellable ? UIHelper.TEXT_LIGHT : UIHelper.TEXT_MUTED);
        name.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        card.add(name, BorderLayout.CENTER);

        JLabel price = new JLabel(String.format("%,.2f MT", p.unitPrice()), SwingConstants.CENTER);
        price.setForeground(sellable ? UIHelper.ACCENT_BLUE : UIHelper.TEXT_MUTED);
        price.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        price.setAlignmentX(Component.CENTER_ALIGNMENT);
        footer.add(price);
        if (!sellable) {
            JLabel unavailable = new JLabel("ESGOTADO");
            unavailable.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
            unavailable.setForeground(UIHelper.REJECTED_RED);
            unavailable.setAlignmentX(Component.CENTER_ALIGNMENT);
            footer.add(Box.createRigidArea(new Dimension(0, 3)));
            footer.add(unavailable);
        } else if (p.stockTracked()) {
            BigDecimal stock = owner.getProductStock(p.id());
            if (stock != null) {
                JLabel stockLbl = new JLabel(UIHelper.formatQty(stock) + " disp.");
                stockLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
                stockLbl.setForeground(UIHelper.APPROVED_GREEN);
                stockLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
                footer.add(Box.createRigidArea(new Dimension(0, 2)));
                footer.add(stockLbl);
            }
        }
        card.add(footer, BorderLayout.SOUTH);

        String whName = owner.getSelectedWarehouseName();
        BigDecimal stock = owner.getProductStock(p.id());
        String stockInfo = !p.stockTracked() ? " (serviço/não rastreado)"
                : (stock != null ? " · " + UIHelper.formatQty(stock) + " disponível" : "");
        String whInfo = whName != null ? " · Armazém: " + whName : "";
        card.setToolTipText(productLabel(p) + whInfo + stockInfo + (sellable ? "" : " — sem stock disponível para venda"));
        if (sellable) {
            card.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { addProductToCart(p); }
                @Override public void mouseEntered(MouseEvent e) {
                    card.setBorder(BorderFactory.createCompoundBorder(
                            new javax.swing.border.LineBorder(UIHelper.ACCENT_BLUE, 1, true),
                            new EmptyBorder(CARD_PADDING - 1, CARD_PADDING - 1, CARD_PADDING - 1, CARD_PADDING - 1)));
                }
                @Override public void mouseExited(MouseEvent e) {
                    card.setBorder(new EmptyBorder(CARD_PADDING, CARD_PADDING, CARD_PADDING, CARD_PADDING));
                }
            });
        }
        return card;
    }

    public static String categoryIcon(String categoryName) {
        if (categoryName == null) return "fas-box";
        String lower = categoryName.toLowerCase();
        if (lower.contains("aliment") || lower.contains("comida") || lower.contains("mercear")) return "fas-shopping-basket";
        if (lower.contains("bebid") || lower.contains("cerveja") || lower.contains("refriger") || lower.contains("vinho")) return "fas-glass-martini-alt";
        if (lower.contains("higien") || lower.contains("limpez") || lower.contains("deterg") || lower.contains("sabão")) return "fas-pump-soap";
        if (lower.contains("padar") || lower.contains("pastel") || lower.contains("pão")) return "fas-bread-slice";
        if (lower.contains("carne") || lower.contains("talho") || lower.contains("peixe") || lower.contains("frio")) return "fas-drumstick-bite";
        return "fas-box";
    }

    public static Color categoryColor(String categoryName) {
        if (categoryName == null) return UIHelper.ACCENT_BLUE;
        String lower = categoryName.toLowerCase();
        if (lower.contains("aliment") || lower.contains("comida") || lower.contains("mercear")) return UIHelper.PENDING_YELLOW;
        if (lower.contains("bebid") || lower.contains("cerveja") || lower.contains("refriger") || lower.contains("vinho")) return UIHelper.ACCENT_BLUE;
        if (lower.contains("higien") || lower.contains("limpez") || lower.contains("deterg") || lower.contains("sabão")) return UIHelper.APPROVED_GREEN;
        if (lower.contains("padar") || lower.contains("pastel") || lower.contains("pão")) return UIHelper.PENDING_YELLOW;
        return UIHelper.ACCENT_BLUE;
    }

    static boolean includeByAvailability(boolean showAll, boolean sellable) {
        return showAll || sellable;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Legenda (esquerda) do bloco de discriminação Subtotal/IVA. */
    public void addProductToCart(ProductDTO product) {
        if (!owner.isProductSellable(product)) {
            mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.getInstance().playAsync(
                    mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.SoundEvent.ERROR);
            owner.showPosNotice(FeedbackType.WARNING, "Sem stock",
                    "O artigo '" + product.name() + "' está esgotado e não pode ser adicionado.");
            return;
        }
        if (owner.activeSession == null) {
            mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.getInstance().playAsync(
                    mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.SoundEvent.WARNING);
            owner.showPosNotice(FeedbackType.WARNING, "Caixa fechado", "Abra o caixa antes de adicionar artigos.");
            return;
        }
        for (POSPanel.CartItem it : owner.cartItems) {
            if (it.serial == null && it.product.id().equals(product.id())) {
                it.qty = it.qty.add(BigDecimal.ONE);
                owner.updateCartTotal(owner.cartItems.indexOf(it));
                mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.getInstance().playAsync(
                        mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.SoundEvent.SUCCESS);
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
                    owner.updateCartTotal(owner.cartItems.size() - 1);
                    mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.getInstance().playAsync(
                            mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.SoundEvent.SUCCESS);
                }, error -> owner.showPosNotice(FeedbackType.ERROR,
                        "Não foi possível consultar promoções", error.getMessage()));
    }

    /** Adiciona artigo de venda ao peso com a quantidade lida da balança. */
    public void addWeighedProductToCart(ProductDTO product, BigDecimal weight) {
        if (!owner.isProductSellable(product)) {
            owner.showPosNotice(FeedbackType.WARNING, "Sem stock",
                    "O artigo '" + product.name() + "' está esgotado e não pode ser adicionado.");
            return;
        }
        if (owner.activeSession == null) {
            owner.showPosNotice(FeedbackType.WARNING, "Caixa fechado", "Abra o caixa antes de adicionar artigos.");
            return;
        }
        BigDecimal qty = weight != null && weight.signum() > 0 ? weight : BigDecimal.ONE;
        for (POSPanel.CartItem it : owner.cartItems) {
            if (it.serial == null && it.product.id().equals(product.id())) {
                it.qty = it.qty.add(qty);
                owner.updateCartTotal(owner.cartItems.indexOf(it));
                return;
            }
        }
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner,
                () -> owner.promotionApiClient.bestPromotion(
                        companyId, product.id(), product.categoryId(), qty),
                promo -> {
                    BigDecimal discount = promo.map(p -> p.discountPercent()).orElse(BigDecimal.ZERO);
                    POSPanel.CartItem item = new POSPanel.CartItem(product, qty, discount, null, null);
                    item.note = promo.map(p -> "Promo: " + p.name()).orElse("Balança: " + qty + " kg");
                    owner.cartItems.add(item);
                    owner.updateCartTotal(owner.cartItems.size() - 1);
                }, error -> owner.showPosNotice(FeedbackType.ERROR,
                        "Não foi possível consultar promoções", error.getMessage()));
    }

    /** Reconstrói todas as linhas da tabela do carrinho a partir de {@link #owner.cartItems}. */
    public void rebuildCartRows() {
        owner.cartModel.setRowCount(0);
        for (POSPanel.CartItem item : owner.cartItems) {
            owner.cartModel.addRow(new Object[]{
                    item.product.name(),
                    item.qty.stripTrailingZeros().toPlainString(),
                    String.format("%.2f MT", item.product.unitPrice()),
                    item.discount.stripTrailingZeros().toPlainString() + "%",
                    POSPanel.ivaCellLabel(item),
                    String.format("%,.2f MT", item.getTotal())
            });
        }
    }

    void changeSelectedQuantity(BigDecimal delta) {
        int selectedView = owner.cartTable.getSelectedRow();
        if (selectedView < 0) {
            owner.showPosNotice(FeedbackType.WARNING, "Seleccione um artigo",
                    "Escolha uma linha do carrinho para alterar a quantidade.");
            return;
        }
        int selected = owner.cartTable.convertRowIndexToModel(selectedView);
        POSPanel.CartItem item = owner.cartItems.get(selected);
        BigDecimal next = item.qty.add(delta);
        if (next.signum() <= 0) {
            owner.cartItems.remove(selected);
            owner.updateCartTotal(Math.min(selected, owner.cartItems.size() - 1));
            return;
        }
        item.qty = next;
        owner.updateCartTotal(selected);
    }

    void selectAndRevealCartRow(int modelRow) {
        if (modelRow < 0 || modelRow >= owner.cartModel.getRowCount()) return;
        int viewRow = owner.cartTable.convertRowIndexToView(modelRow);
        if (viewRow < 0) return;
        owner.cartTable.setRowSelectionInterval(viewRow, viewRow);
        owner.cartTable.scrollRectToVisible(owner.cartTable.getCellRect(viewRow, 0, true));
    }

    String productLabel(ProductDTO p) {
        String code = p.barcode() != null && !p.barcode().isBlank()
                ? p.barcode()
                : p.reference() != null && !p.reference().isBlank() ? p.reference() : p.sku();
        return code + " - " + p.name();
    }

}
