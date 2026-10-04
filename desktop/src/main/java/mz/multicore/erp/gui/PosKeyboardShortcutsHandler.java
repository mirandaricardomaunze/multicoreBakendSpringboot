package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Gestor e vinculador central de atalhos rápidos de teclado do POS (F1 a F12, ESC, DELETE, +, -).
 */
public final class PosKeyboardShortcutsHandler {

    private PosKeyboardShortcutsHandler() {}

    /**
     * Instala os atalhos de teclado no painel do POS e na tabela do carrinho.
     */
    public static void install(POSPanel panel) {
        if (panel == null) return;

        InputMap input = panel.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actions = panel.getActionMap();

        // Teclas de Função Globais (F1 a F12)
        bind(input, actions, "posHelp", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), () -> showHelp(panel));
        bind(input, actions, "posProductSearch", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), () -> focusProductSearch(panel));
        bind(input, actions, "posBarcodeSearch", KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), () -> focusBarcode(panel));
        bind(input, actions, "posClientSearch", KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), () -> focusClientSearch(panel));
        bind(input, actions, "posDiscount", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), () -> applyDiscountPrompt(panel));
        bind(input, actions, "posEditQuantity", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), () -> editQuantity(panel));
        bind(input, actions, "posQuotation", KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), () -> openQuotationImport(panel));
        bind(input, actions, "posReturn", KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), () -> openReturn(panel));
        bind(input, actions, "posCashMove", KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), () -> manageCash(panel));
        bind(input, actions, "posCheckout", KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0), () -> checkout(panel));
        bind(input, actions, "posToggleView", KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), () -> toggleView(panel));
        bind(input, actions, "posCloseSession", KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), () -> closeSession(panel));

        // Atalhos de Edição e Cancelamento
        bind(input, actions, "posCancelOrClear", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), () -> cancelOrClear(panel));
        bind(input, actions, "posNewSale", KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK), () -> newSale(panel));

        // Atalhos restritos ao foco da tabela do carrinho (não interferem com campos de texto)
        if (panel.cartTable != null) {
            InputMap tableInput = panel.cartTable.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap tableActions = panel.cartTable.getActionMap();

            bind(tableInput, tableActions, "posRemoveLine", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), panel::removeFromCart);
            bind(tableInput, tableActions, "posIncreaseNumPad", KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), () -> panel.changeSelectedQuantity(BigDecimal.ONE));
            bind(tableInput, tableActions, "posIncreasePlus", KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0), () -> panel.changeSelectedQuantity(BigDecimal.ONE));
            bind(tableInput, tableActions, "posDecreaseNumPad", KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), () -> panel.changeSelectedQuantity(BigDecimal.ONE.negate()));
            bind(tableInput, tableActions, "posDecreaseMinus", KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), () -> panel.changeSelectedQuantity(BigDecimal.ONE.negate()));
        }
    }

    public static void showHelp(POSPanel panel) {
        PosShortcutHelpDialog.show(panel);
    }

    public static void focusProductSearch(POSPanel panel) {
        if (panel.productSearchField != null) {
            panel.productSearchField.requestFocusInWindow();
            panel.productSearchField.selectAll();
        }
    }

    public static void focusBarcode(POSPanel panel) {
        if (panel.barcodeField != null) {
            panel.barcodeField.requestFocusInWindow();
            panel.barcodeField.selectAll();
        }
    }

    public static void focusClientSearch(POSPanel panel) {
        if (panel.clientSearchField != null) {
            panel.clientSearchField.requestFocusInWindow();
            panel.clientSearchField.selectAll();
        }
    }

    public static void editQuantity(POSPanel panel) {
        panel.editSelectedCartQuantity();
    }

    public static void openQuotationImport(POSPanel panel) {
        if (panel != null && panel.quotationActions != null) {
            panel.quotationActions.openImportDialog();
        }
    }

    public static void openLoyalty(POSPanel panel) {
        panel.openLoyaltyDialog();
    }

    public static void openReturn(POSPanel panel) {
        panel.showReturnDialog();
    }

    public static void manageCash(POSPanel panel) {
        panel.manageCashMovements();
    }

    public static void checkout(POSPanel panel) {
        panel.runCheckout();
    }

    public static void toggleView(POSPanel panel) {
        panel.selectView(!panel.isHistoryView());
    }

    public static void closeSession(POSPanel panel) {
        panel.closeSession();
    }

    public static void newSale(POSPanel panel) {
        if (panel.cartItems.isEmpty()) {
            focusBarcode(panel);
            return;
        }
        cancelOrClear(panel);
    }

    public static void cancelOrClear(POSPanel panel) {
        if (panel.cartItems.isEmpty()) {
            focusBarcode(panel);
            return;
        }

        int answer = JOptionPane.showConfirmDialog(
                panel,
                "Deseja cancelar o atendimento actual e limpar o carrinho de compras?",
                "Cancelar Venda / Limpar Carrinho",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (answer == JOptionPane.YES_OPTION) {
            panel.clearCart();
            ToastManager.show(panel, FeedbackType.INFO, "Carrinho limpo com sucesso.");
        }
    }

    /**
     * Aplica desconto ágil (%) na linha seleccionada ou no carrinho.
     */
    public static void applyDiscountPrompt(POSPanel panel) {
        if (panel.cartItems.isEmpty()) {
            panel.showPosNotice(FeedbackType.WARNING, "Carrinho Vazio",
                    "Adicione artigos ao carrinho antes de aplicar descontos.");
            return;
        }

        int selectedView = panel.cartTable.getSelectedRow();
        if (selectedView < 0) {
            panel.showPosNotice(FeedbackType.WARNING, "Seleccione uma Linha",
                    "Escolha o artigo no carrinho para aplicar o desconto de linha.");
            return;
        }

        int selectedModel = panel.cartTable.convertRowIndexToModel(selectedView);
        if (selectedModel >= panel.cartItems.size()) return;

        POSPanel.CartItem item = panel.cartItems.get(selectedModel);

        JTextField discountPercentField = new JTextField(item.discount != null && item.discount.signum() > 0
                ? item.discount.stripTrailingZeros().toPlainString() : "0");
        UIHelper.styleTextField(discountPercentField);

        JPanel form = UIHelper.createDialogForm(
                "Artigo:", readOnlyField(item.product.name()),
                "Preço Unitário:", readOnlyField(String.format("%,.2f MT", item.product.unitPrice())),
                "Desconto (%):", discountPercentField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Aplicar Desconto de Linha (F5)",
                "fas-percent", "Introduza a percentagem de desconto a aplicar ao artigo", form)
                .setConfirmButton("Aplicar Desconto", "fas-check")
                .setOnSave(() -> {
                    String text = discountPercentField.getText().trim().replace(',', '.').replace("%", "");
                    BigDecimal percent;
                    try {
                        percent = new BigDecimal(text);
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Introduza uma percentagem válida (ex: 5 ou 10).");
                    }
                    if (percent.signum() < 0 || percent.compareTo(new BigDecimal("100")) > 0) {
                        throw new IllegalArgumentException("O desconto deve estar entre 0% e 100%.");
                    }

                    // Calcula o valor monetário do desconto por unidade
                    BigDecimal discountPerUnit = item.product.unitPrice()
                            .multiply(percent)
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                    item.discount = discountPerUnit;
                })
                .showDialog();

        if (confirmed) {
            panel.updateCartTotal(selectedModel);
            ToastManager.success(panel, "Desconto aplicado ao artigo com sucesso.");
        }
    }

    private static JTextField readOnlyField(String text) {
        JTextField f = new JTextField(text != null ? text : "");
        UIHelper.styleTextField(f);
        f.setEditable(false);
        return f;
    }

    private static void bind(InputMap input, ActionMap actions, String name, KeyStroke key, Runnable command) {
        input.put(key, name);
        actions.put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (command != null) command.run();
            }
        });
    }

    /** Ponto testável e retrocompatível para validar o mapeamento de uma tecla para uma acção. */
    static void bindShortcut(InputMap input, ActionMap actions, String name,
                             KeyStroke key, Runnable command) {
        bind(input, actions, name, key, command);
    }
}
