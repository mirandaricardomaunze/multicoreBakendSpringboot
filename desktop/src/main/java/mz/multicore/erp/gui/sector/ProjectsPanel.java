package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.ProjectsApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.projects.dto.CustomerProjectDTO;
import mz.multicore.erp.modules.projects.dto.ProjectStatementDTO;
import mz.multicore.erp.modules.projects.dto.ProjectStatementLineDTO;
import mz.multicore.erp.modules.projects.dto.SaveCustomerProjectRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Client projects or construction sites (SA-C09, {@code docs/OBRAS_CLIENTE_SPEC.md}). Projects on the left; on
 * the right, the statement of the selected site: its orders, the invoices they became and what is still owed.
 * Orders are linked to a project from the order editor.
 */
public class ProjectsPanel extends JPanel {

    private static final String[] PROJECT_COLUMNS = {"Código", "Obra", "Cliente", "Encomendas", "Encomendado", "Em dívida", "Estado"};
    private static final String[] STATEMENT_COLUMNS = {"Encomenda", "Data", "Estado", "Total", "Fatura", "Faturado", "Em dívida"};

    private final ProjectsApiClient api;
    private final ComercialApiClient comercialApi;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DefaultTableModel projectModel = readOnly(PROJECT_COLUMNS);
    private final JTable projectTable = new JTable(projectModel);
    private final DefaultTableModel statementModel = readOnly(STATEMENT_COLUMNS);
    private final JTable statementTable = new JTable(statementModel);
    private final ModernButton newBtn = UIHelper.createSuccessButton("Nova Obra");
    private final ModernButton editBtn = UIHelper.createSecondaryButton("Editar");
    private final JLabel detailTitle = new JLabel(" ");
    private final JLabel detailMeta = new JLabel(" ");
    private final JLabel totals = new JLabel(" ");
    private final CardLayout cards = new CardLayout();
    private final JPanel detail = new JPanel(cards);
    private final List<CustomerProjectDTO> projects = new ArrayList<>();
    private Consumer<List<CustomerProjectDTO>> onLoaded = list -> { };

    public ProjectsPanel(ProjectsApiClient api, ComercialApiClient comercialApi) {
        this.api = api;
        this.comercialApi = comercialApi;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(feedback, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildList(), buildDetail());
        split.setOpaque(false);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setResizeWeight(0.5);
        split.setDividerSize(10);
        ResponsiveSupport.adaptiveSplit(split, 980);
        add(split, BorderLayout.CENTER);
        editBtn.setEnabled(false);
    }

    /** Receives every loaded project list, so the module page can update its indicators. */
    public void onLoaded(Consumer<List<CustomerProjectDTO>> listener) {
        this.onLoaded = listener == null ? list -> { } : listener;
    }

    private JComponent buildList() {
        UIHelper.styleTable(projectTable);
        projectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectTable.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        projectTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.money());
        projectTable.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        projectTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadStatement();
        });
        JScrollPane scroll = new JScrollPane(projectTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);

        JTextField search = TableFilter.searchField("Obra, código ou cliente…");
        search.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> state = TableFilter.combo("Todos os estados", "Activa", "Inactiva");
        UIHelper.styleComboBox(state);
        state.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        state.setSelectedItem("Activa");
        TableFilter.install(projectTable, search, new TableFilter.ColumnFilter(state, 6));
        JPanel filters = TableFilter.bar(search, TableFilter.label("Estado:"), state);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));

        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> openDialog(null));
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editBtn.addActionListener(e -> {
            CustomerProjectDTO selected = selected();
            if (selected != null) openDialog(selected);
        });
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::reload);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(ResponsiveSupport.cardTop("Obras e Projectos", filters, refreshBtn, editBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(projectTable), BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildDetail() {
        ModernPanel empty = new ModernPanel(16);
        empty.setLayout(new GridBagLayout());
        JLabel hint = new JLabel("<html><div style='text-align:center;width:320px'><b>Extracto por obra</b><br>"
                + "Escolha uma obra para ver as encomendas, as faturas e o que o cliente ainda deve nessa obra. "
                + "As encomendas ligam-se à obra no editor de encomendas.</div></html>",
                UIHelper.icon("fas-hard-hat", 36, UIHelper.ACCENT_ORANGE), SwingConstants.CENTER);
        hint.setHorizontalTextPosition(SwingConstants.CENTER);
        hint.setVerticalTextPosition(SwingConstants.BOTTOM);
        hint.setForeground(UIHelper.TEXT_MUTED);
        empty.add(hint);

        UIHelper.styleTable(statementTable);
        for (int c : new int[]{3, 5, 6}) statementTable.getColumnModel().getColumn(c).setCellRenderer(TableCellRenderers.money());
        statementTable.getColumnModel().getColumn(2).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(statementTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        detailTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        detailTitle.setForeground(UIHelper.TEXT_LIGHT);
        detailMeta.setForeground(UIHelper.TEXT_MUTED);
        totals.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        totals.setForeground(UIHelper.TEXT_LIGHT);
        JPanel head = new JPanel(new GridLayout(2, 1, 0, 2));
        head.setOpaque(false);
        head.add(detailTitle);
        head.add(detailMeta);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(head, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(totals, BorderLayout.SOUTH);

        detail.setOpaque(false);
        detail.add(empty, "empty");
        detail.add(card, "statement");
        cards.show(detail, "empty");
        return detail;
    }

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || api == null) return;
        Long keep = selected() == null ? null : selected().id();
        UIHelper.loadAsync(this, () -> api.list(companyId, null), loaded -> apply(loaded, keep),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as obras", error.getMessage(),
                        "Tentar novamente", this::reload));
    }

    private void apply(List<CustomerProjectDTO> loaded, Long keep) {
        projects.clear();
        projects.addAll(loaded);
        projectModel.setRowCount(0);
        for (CustomerProjectDTO p : projects) {
            projectModel.addRow(new Object[]{p.code(), p.name(), p.clientName(), p.orderCount(), p.orderedTotal(),
                    p.outstanding(), p.active() ? "ACTIVA" : "INACTIVA"});
        }
        if (keep != null) {
            for (int i = 0; i < projects.size(); i++) {
                if (!projects.get(i).id().equals(keep)) continue;
                int view = projectTable.convertRowIndexToView(i);
                if (view >= 0) projectTable.setRowSelectionInterval(view, view);
            }
        }
        if (selected() == null) cards.show(detail, "empty");
        onLoaded.accept(List.copyOf(projects));
    }

    private void loadStatement() {
        CustomerProjectDTO project = selected();
        editBtn.setEnabled(project != null);
        if (project == null) {
            cards.show(detail, "empty");
            return;
        }
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        UIHelper.loadAsync(this, () -> api.statement(companyId, project.id()), this::applyStatement,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar o extracto", error.getMessage(),
                        "Tentar novamente", this::loadStatement));
    }

    private void applyStatement(ProjectStatementDTO statement) {
        CustomerProjectDTO p = statement.project();
        if (selected() == null || !selected().id().equals(p.id())) return;
        detailTitle.setText(p.name() + "  ·  " + p.code());
        detailMeta.setText(p.clientName() + (p.address() == null ? "" : "  ·  " + p.address())
                + (p.contact() == null ? "" : "  ·  " + p.contact()));
        statementModel.setRowCount(0);
        for (ProjectStatementLineDTO l : statement.lines()) {
            statementModel.addRow(new Object[]{l.orderNumber(), l.orderDate() == null ? "—" : UIHelper.formatDate(l.orderDate()),
                    l.orderStatus(), l.orderTotal(), l.invoiceNumber() == null ? "Por faturar" : l.invoiceNumber(),
                    l.invoiceTotal() == null ? BigDecimal.ZERO : l.invoiceTotal(), l.outstanding() == null ? BigDecimal.ZERO : l.outstanding()});
        }
        totals.setText("Encomendado " + UIHelper.formatMzn(statement.orderedTotal()) + "   ·   Faturado "
                + UIHelper.formatMzn(statement.invoicedTotal()) + "   ·   Em dívida " + UIHelper.formatMzn(statement.outstanding()));
        totals.setForeground(statement.outstanding().signum() > 0 ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);
        cards.show(detail, "statement");
    }

    private void openDialog(CustomerProjectDTO existing) {
        UIHelper.loadAsync(this, comercialApi::getAllClients, clients -> ProjectFormDialog.show(this, api, clients, existing, saved -> {
            ToastManager.success(this, existing == null ? "Obra " + saved.code() + " criada." : "Obra actualizada.");
            Long companyId = CurrentUserContext.findCurrentCompanyId();
            UIHelper.loadAsync(this, () -> api.list(companyId, null), loaded -> apply(loaded, saved.id()), error -> reload());
        }), error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar os clientes", error.getMessage(), null, null));
    }

    private CustomerProjectDTO selected() {
        int row = TableFilter.selectedModelRow(projectTable);
        return row < 0 || row >= projects.size() ? null : projects.get(row);
    }

    private static DefaultTableModel readOnly(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    /** Create or edit a project: client, name, site address, contact and whether it is still active. */
    static final class ProjectFormDialog {

        private ProjectFormDialog() {
        }

        static void show(JComponent parent, ProjectsApiClient api, List<ClientDTO> clients, CustomerProjectDTO existing,
                         Consumer<CustomerProjectDTO> onSaved) {
            Long companyId = CurrentUserContext.findCurrentCompanyId();
            if (companyId == null) return;
            SearchableComboBox<ClientDTO> client = new SearchableComboBox<>("Pesquisar cliente por nome ou NUIT",
                    c -> c.name() + (c.taxId() == null ? "" : "  ·  NUIT " + c.taxId()), c -> c.name() + " " + c.taxId());
            client.setItems(clients.stream().filter(c -> !"000000000".equals(c.taxId())).toList());
            if (existing != null) client.selectFirst(c -> c.id().equals(existing.clientId()));
            JTextField name = new JTextField(existing == null ? "" : existing.name());
            JTextField address = new JTextField(existing == null || existing.address() == null ? "" : existing.address());
            JTextField contact = new JTextField(existing == null || existing.contact() == null ? "" : existing.contact());
            JCheckBox active = new JCheckBox("Obra activa (aceita encomendas)", existing == null || existing.active());
            active.setOpaque(false);
            active.setForeground(UIHelper.TEXT_LIGHT);
            FormField clientForm = new FormField("Cliente", client, true, null);
            FormField nameForm = new FormField("Nome da obra", name, true, "Ex.: Moradia Triunfo, Armazém da Matola");
            JPanel form = UIHelper.createDialogForm(1, new Object[]{"", clientForm, "", nameForm,
                    "Morada da obra:", address, "Contacto no local:", contact, "Estado:", active});
            ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, existing == null ? "Nova Obra" : "Editar Obra",
                    "fas-hard-hat", "Extracto, entregas e crédito por obra", form);
            CustomerProjectDTO[] result = new CustomerProjectDTO[1];
            dialog.setOnSaveAsync(() -> {
                ClientDTO c = client.selectedValue();
                if (c == null) {
                    clientForm.setError("Escolha o cliente.");
                    throw new IllegalArgumentException("Escolha o cliente.");
                }
                if (!nameForm.validateRequired()) throw new IllegalArgumentException("Indique o nome da obra.");
                SaveCustomerProjectRequest request = new SaveCustomerProjectRequest(existing == null ? null : existing.id(),
                        companyId, c.id(), name.getText().trim(), address.getText().trim(), contact.getText().trim(), active.isSelected());
                return () -> { result[0] = api.save(request); return null; };
            });
            if (dialog.showDialog() && result[0] != null) onSaved.accept(result[0]);
        }
    }
}
