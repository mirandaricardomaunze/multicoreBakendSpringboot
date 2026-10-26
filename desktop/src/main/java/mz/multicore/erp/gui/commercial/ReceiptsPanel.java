package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/** Lista e anulação de recibos comerciais. */
public final class ReceiptsPanel extends JPanel {
    private final ComercialApiClient apiClient;
    private final Runnable invoicesRefresh;
    private final DefaultTableModel model;
    private final JTable table;
    private final JComboBox<String> businessUnit;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    public ReceiptsPanel(ComercialApiClient apiClient, Runnable invoicesRefresh) {
        this.apiClient = apiClient;
        this.invoicesRefresh = invoicesRefresh;
        setLayout(new BorderLayout(0, 8));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(4, 0, 0, 0));
        add(feedback, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));

        model = new DefaultTableModel(
                new String[]{"ID", "Nº Recibo", "Negócio", "Fatura", "Cliente", "Montante Pago", "Método Pag.", "Estado", "Data"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());
        hideColumn(0);

        JTextField search = TableFilter.searchField("Nº recibo, fatura ou cliente…");
        search.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        businessUnit = TableFilter.combo("Todos os negócios");
        JComboBox<String> method = TableFilter.combo("Todos os métodos", "Dinheiro", "Transferência", "Cartão", "M-Pesa");
        JComboBox<String> status = TableFilter.combo("Todos os estados", "Concluído", "Cancelado");
        JComboBox<String> period = TableFilter.periodCombo();

        TableFilter.install(table, search,
                List.of(new TableFilter.ColumnFilter(businessUnit, 2),
                        new TableFilter.ColumnFilter(method, 6),
                        new TableFilter.ColumnFilter(status, 7)),
                List.of(new TableFilter.PeriodFilter(period, 8)));

        ModernButton cancel = UIHelper.createDangerButton("Anular Recibo");
        cancel.setIcon(UIHelper.icon("fas-ban", 14));
        cancel.addActionListener(e -> ReceiptsViewCoordinator.cancelSelected(this, table, model, apiClient, feedback, invoicesRefresh, this::refresh));

        ModernButton refresh = UIHelper.createRefreshButton(this::refresh);
        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes do recibo (Espaço)");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filters = TableFilter.bar(search,
                TableFilter.label("Negócio:"), businessUnit,
                TableFilter.label("Método:"), method,
                TableFilter.label("Estado:"), status,
                TableFilter.label("Data:"), period,
                peekToggleBtn);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(UIHelper.tableCardTop("Recibos Emitidos (Liquidações)", filters, refresh, cancel), BorderLayout.NORTH);

        UIHelper.ensureHeadersFit(table);
        TableQuickPeekController peekController = TableQuickPeekController.install(table, card,
                (peek, mr) -> ReceiptsViewCoordinator.renderQuickPeek(peek, mr, model, cancel::doClick));
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    public void openPayment(Long invoiceId, String invoiceNumber, BigDecimal invoiceTotal,
                            List<TreasuryAccountDTO> accounts) {
        ReceiptPaymentDialog.show(this, apiClient, feedback, invoiceId, invoiceNumber, invoiceTotal,
                accounts, this::refresh, invoicesRefresh);
    }

    public void refresh() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> apiClient.getReceiptsByCompany(companyId),
                receipts -> ReceiptsViewCoordinator.populateReceipts(model, businessUnit, receipts),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar recibos", error.getMessage(), null, null));
    }

    private void hideColumn(int index) {
        table.getColumnModel().getColumn(index).setMinWidth(0);
        table.getColumnModel().getColumn(index).setMaxWidth(0);
        table.getColumnModel().getColumn(index).setWidth(0);
    }
}
