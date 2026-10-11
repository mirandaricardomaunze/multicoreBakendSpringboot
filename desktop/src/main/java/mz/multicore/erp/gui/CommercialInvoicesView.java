package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Constrói a vista de listagem/editor de faturação. */
final class CommercialInvoicesView {
    private CommercialInvoicesView() {}

    static JPanel create(ComercialPanel owner) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(4, 0, 0, 0));

        ModernPanel formCard = new ModernPanel(16);
        formCard.setLayout(new FlowLayout(FlowLayout.LEFT, 16, 6));
        formCard.setBorder(new EmptyBorder(10, 14, 10, 14));

        owner.clientCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.clientCombo);

owner.warehouseCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.warehouseCombo);

        owner.invoiceBusinessUnitCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.invoiceBusinessUnitCombo);

        formCard.add(fieldGroup("Cliente:", owner.clientCombo, 320));
        formCard.add(fieldGroup("Armazém de Expedição:", owner.warehouseCombo, 220));
        formCard.add(fieldGroup("Linha de Negócio:", owner.invoiceBusinessUnitCombo, 250));

        owner.productCombo = new ProductSearchComboBox();

        // Editores reutilizados apenas pelas células da grelha.
        owner.discountField = new DecimalField("0", 2, false);
        owner.serialField = new JTextField();
        UIHelper.styleTextField(owner.serialField);

        ModernButton addLineBtn = UIHelper.createAddLineButton();
        addLineBtn.setText("Adicionar linha");
        ModernButton removeLineBtn = UIHelper.createDangerButton("Remover linha");
        removeLineBtn.setIcon(UIHelper.icon("fas-trash", 14));
        removeLineBtn.addActionListener(e -> owner.removeSelectedInvoiceDraftLine());

        // Linhas do rascunho: a acção pertence ao topo do card da tabela.
        String[] lineCols = {"Produto", "Qtd", "Emb.", "Cx.", "% Cx.", "Preço Unit.", "IVA",
                "Desc. %", "Série", "Total"};
        owner.linesTableModel = new DefaultTableModel(lineCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return owner.invoiceGridEditable && (c <= 3 || c == 7 || c == 8);
            }
        };
        owner.linesTable = new JTable(owner.linesTableModel);
        UIHelper.styleTable(owner.linesTable);
        owner.linesTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        owner.linesTable.putClientProperty("noTableFooter", Boolean.TRUE);
        owner.linesTable.getColumnModel().getColumn(0)
                .setCellEditor(ProductSearchComboBox.createTableCellEditor(owner.productCombo));
        owner.linesTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override protected void setValue(Object value) {
                setText(value instanceof ProductDTO product ? product.name() : "Pesquisar produto...");
            }
        });
        for (int column : java.util.List.of(1, 2, 3)) {
            owner.linesTable.getColumnModel().getColumn(column)
                    .setCellEditor(new DefaultCellEditor(new QuantityField("0", false)));
            owner.linesTable.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.quantity());
        }
        owner.linesTable.getColumnModel().getColumn(7)
                .setCellEditor(new DefaultCellEditor(owner.discountField));
        owner.linesTable.getColumnModel().getColumn(8)
                .setCellEditor(new DefaultCellEditor(owner.serialField));
        owner.linesTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        owner.linesTable.getColumnModel().getColumn(9).setCellRenderer(TableCellRenderers.money());
        owner.linesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        owner.linesTable.setSurrendersFocusOnKeystroke(true);
        owner.linesTable.setToolTipText("Edite Produto, Qtd, Emb., Cx., desconto e série directamente.");
        owner.linesTableModel.addTableModelListener(event -> {
            if (owner.syncingInvoiceGrid || event.getFirstRow() < 0
                    || event.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
            owner.syncInvoiceLineFromGrid(event.getFirstRow(), event.getColumn());
        });
        JScrollPane linesScroll = new JScrollPane(owner.linesTable);
        UIHelper.styleEmbeddedTableScrollPane(linesScroll, owner.linesTable, 4);
        // Draft table is placed in its own card below the input form.

        // Row 7: Total summary (a emissão é feita pelo botão Gravar do modal)
        owner.totalLabel = new JLabel("Total Rascunho: 0.00 MT (incl. IVA)");
        owner.totalLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        owner.totalLabel.setForeground(Color.WHITE);

        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setBorder(new EmptyBorder(12, 0, 0, 0));
        totalRow.add(owner.totalLabel, BorderLayout.EAST);

        ModernPanel draftCard = new ModernPanel(16);
        draftCard.setLayout(new BorderLayout(0, 10));
        draftCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        draftCard.setPreferredSize(new Dimension(0, 280));
        JLabel lineHint = new JLabel("Preparação tabular: edite na grelha ou bipe o código de barras (F2).");
        lineHint.setForeground(UIHelper.TEXT_MUTED);
        ModernButton invoiceLookupBtn = UIHelper.createProductLookupButton(owner::openInvoiceProductLookup);
        owner.invoiceBarcodeField = new mz.multicore.erp.gui.components.BarcodeScanField(
                "Bipar código / SKU (F2)...", owner::handleInvoiceBarcodeScanned);
        draftCard.add(UIHelper.tableCardTop("Linhas da Fatura", lineHint,
                owner.invoiceBarcodeField, invoiceLookupBtn, addLineBtn, removeLineBtn), BorderLayout.NORTH);
        draftCard.add(linesScroll, BorderLayout.CENTER);
        draftCard.add(totalRow, BorderLayout.SOUTH);

        // Área de trabalho de página: cabeçalho + grelha de rascunho.
        JPanel formContent = new JPanel(new BorderLayout(0, 12));
        formContent.setOpaque(false);
        formContent.add(formCard, BorderLayout.NORTH);
        JPanel draftWrap = new JPanel(new BorderLayout(0, 8));
        draftWrap.setOpaque(false);
        draftWrap.add(UIHelper.createSubheading("Linhas da Fatura (Rascunho)"), BorderLayout.NORTH);
        draftWrap.add(draftCard, BorderLayout.CENTER);
        formContent.add(draftWrap, BorderLayout.CENTER);
        formContent.registerKeyboardAction(e -> {
            if (owner.invoiceBarcodeField != null && owner.invoiceBarcodeField.isEnabled()) {
                owner.invoiceBarcodeField.requestFocusInWindow();
                owner.invoiceBarcodeField.selectAll();
            }
        }, KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        owner.invoiceFormContent = formContent;

        // TAB: uma única unidade visual, com acções, filtros, tabela e paginação no mesmo card.
        ModernButton refreshBtn = UIHelper.createRefreshButton(owner::loadInvoicesTable);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout(0, 6));
        listCard.setBorder(new EmptyBorder(8, 12, 8, 12));

        String[] invoicesCols = {"ID", "Nº Fatura", "Cliente", "Data", "Negócio", "Estado", "Total", "Em Dívida", "Impressão"};
        owner.invoicesTableModel = new DefaultTableModel(invoicesCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.invoicesTable = new JTable(owner.invoicesTableModel);
        owner.invoicesTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(owner.invoicesTable);
        UIHelper.ensureHeadersFit(owner.invoicesTable);
        owner.invoicesTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        owner.invoicesTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());
        owner.invoicesTable.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.money());
        owner.invoicesTable.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.money());
        owner.invoicesTable.getColumnModel().getColumn(8).setPreferredWidth(118);
        
        // Hide ID column
        owner.invoicesTable.getColumnModel().getColumn(0).setMinWidth(0);
        owner.invoicesTable.getColumnModel().getColumn(0).setMaxWidth(0);
        owner.invoicesTable.getColumnModel().getColumn(0).setWidth(0);

        JScrollPane invoicesScroll = new JScrollPane(owner.invoicesTable);
        UIHelper.styleScrollPane(invoicesScroll);
        JTextField invSearch = TableFilter.searchField("Nº fatura ou cliente…");
        owner.invBusinessUnitCombo = TableFilter.combo("Todos os negócios", "Misto");
        if (owner.businessUnitsList != null) {
            for (mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO bu : owner.businessUnitsList) {
                owner.invBusinessUnitCombo.addItem(bu.name());
            }
        }
        JComboBox<String> invEstado = TableFilter.combo("Todos os estados",
                "Rascunho", "Pendente de aprovação", "Aprovação de desconto", "Aprovado",
                "Parcialmente paga", "Rejeitado", "Paga", "Cancelado");
        JComboBox<String> invPeriodo = TableFilter.periodCombo();
        TableFilter.install(owner.invoicesTable, invSearch,
                java.util.List.of(
                        new TableFilter.ColumnFilter(owner.invBusinessUnitCombo, 4),
                        new TableFilter.ColumnFilter(invEstado, 5)),
                java.util.List.of(new TableFilter.PeriodFilter(invPeriodo, 3)));
        listCard.add(invoicesScroll, BorderLayout.CENTER);

        ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Imprimir PDF", UIHelper.icon("fas-print", 14, UIHelper.ACCENT_BLUE), owner::printSelectedInvoice)
                .addAction("Imprimir Guia", UIHelper.icon("fas-truck", 14, UIHelper.ACCENT), owner::printSelectedGuide)
                .addAction("Exportar Tabela", UIHelper.icon("fas-file-pdf", 14, UIHelper.REJECTED_RED), owner::exportInvoicesTable)
                .addAction("Anular Fatura", UIHelper.icon("fas-ban", 14, UIHelper.REJECTED_RED), owner::cancelSelectedInvoice)
                .addAction("Liquidar (RC)", UIHelper.icon("fas-money-bill-wave", 14, UIHelper.APPROVED_GREEN), owner::paySelectedInvoice);
        ActionMenuButton issueBtn = UIHelper.createActionMenuButton("Emitir")
                .addAction("Nova Fatura", UIHelper.icon("fas-file-invoice", 14, UIHelper.ACCENT_BLUE), owner::openInvoiceEditor)
                .addAction("Faturar Encomenda", UIHelper.icon("fas-file-invoice-dollar", 14, UIHelper.ACCENT), owner::openBillFromOrderDialog);
        issueBtn.setColors(UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
        issueBtn.setToolTipText("Emitir uma nova fatura ou faturar uma encomenda pendente.");

        JPanel cardHeader = new JPanel(new BorderLayout(8, 0));
        cardHeader.setOpaque(false);
        cardHeader.add(UIHelper.createSubheading("Faturas Recentes"), BorderLayout.WEST);
        cardHeader.add(UIHelper.actionsBar(refreshBtn, moreBtn, issueBtn), BorderLayout.EAST);

        JPanel invFilters = UIHelper.filterBar(
                new JComponent[]{invSearch,
                        TableFilter.label("Negócio:"), owner.invBusinessUnitCombo,
                        TableFilter.label("Estado:"), invEstado,
                        TableFilter.label("Período:", "fas-calendar-alt"), invPeriodo},
                null);
        JPanel cardTop = new JPanel(new BorderLayout(0, 10));
        cardTop.setOpaque(false);
        cardTop.add(cardHeader, BorderLayout.NORTH);
        cardTop.add(invFilters, BorderLayout.CENTER);
        listCard.add(cardTop, BorderLayout.NORTH);

        // A tabela vem paginada do servidor; o rodapé fica reservado exclusivamente à paginação.
        owner.invoicesPager = new TablePager(owner::loadInvoicesPage);
        listCard.add(owner.invoicesPager, BorderLayout.SOUTH);

        // Lista de faturas ocupa a tab inteira; o formulário vive no modal.
        panel.add(listCard, BorderLayout.CENTER);

        // LISTENERS
        TableQuickPeekController invPeek = UIHelper.installQuickPeek(owner.invoicesTable, listCard);
        invPeek.setOnOpenFullCallback(modelRow -> owner.printSelectedInvoice());
        UIHelper.installDocumentGridShortcuts(owner.linesTable,
                owner::addDraftLine, owner::removeSelectedInvoiceDraftLine, owner::saveInvoiceFromEditor,
                owner::openInvoiceProductLookup);
        addLineBtn.addActionListener(e -> owner.addDraftLine());
        // Documento em painel completo (substitui o modal): a aba alterna lista <-> editor.
        DocumentEditorHost invoiceEditor = new DocumentEditorHost(
                "Nova Fatura", owner.invoiceFormContent,
                owner::saveInvoiceFromEditor,
                owner::backToInvoicesList,
                () -> !owner.draftLines.isEmpty());
        owner.faturacaoCards = new CardLayout();
        owner.faturacaoHost = new JPanel(owner.faturacaoCards);
        owner.faturacaoHost.setOpaque(false);
        owner.faturacaoHost.add(panel, "list");
        owner.faturacaoHost.add(invoiceEditor, "editor");
        return owner.faturacaoHost;
    }

    private static JPanel fieldGroup(String labelText, JComponent comp, int width) {
        JPanel group = new JPanel(new BorderLayout(0, 4));
        group.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lbl.setForeground(UIHelper.TEXT_MUTED);
        group.add(lbl, BorderLayout.NORTH);
        if (width > 0) {
            comp.setPreferredSize(new Dimension(width, 36));
        }
        group.add(comp, BorderLayout.CENTER);
        return group;
    }
}
