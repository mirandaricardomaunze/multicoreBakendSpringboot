package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.ExpiryApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;
import mz.multicore.erp.modules.expiry.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Validades (SA-C14, SA-C15, {@code docs/VALIDADES_REMARCACAO_SPEC.md}). Tab "Lotes por Vencer": what expired and
 * what expires in the next two weeks, worst first, with the discount the POS gives today and the one the rules
 * ask for. "Remarcar" creates a store promotion until the lot expires; "Abater Vencido" sends the lot to the stock
 * waste flow. Tab "Regras de Remarcação": "from N days before expiry, X% off", per category or general. When a
 * manager opens the module, the rules are applied once a day.
 */
public class ExpiryPanel extends JPanel {

    private static final String[] COLUMNS = {"Situação", "Validade", "Artigo", "Lote", "Armazém", "Qtd.", "Valor a custo",
            "Desconto hoje", "Sugerido"};
    private static final String[] RULE_COLUMNS = {"Categoria", "A partir de", "Desconto", "Estado"};
    static final String EXPIRED = "VENCIDO";
    static final String TODAY = "VENCE HOJE";
    static final String SOON = "1 A 2 DIAS";
    static final String WEEK = "ESTA SEMANA";
    static final String LATER = "MAIS TARDE";

    private final ExpiryApiClient api;
    private final ComercialApiClient comercialApi;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DefaultTableModel model = readOnly(COLUMNS);
    private final JTable table = new JTable(model);
    private final DefaultTableModel ruleModel = readOnly(RULE_COLUMNS);
    private final JTable ruleTable = new JTable(ruleModel);
    private final ModernButton markdownBtn = UIHelper.createPrimaryButton("Remarcar");
    private final ModernButton writeOffBtn = UIHelper.createSecondaryButton("Abater Vencido");
    private final ModernButton editRuleBtn = UIHelper.createSecondaryButton("Editar");
    private final List<ExpiryLotDTO> lots = new ArrayList<>();
    private final List<MarkdownRuleDTO> rules = new ArrayList<>();
    private Consumer<ExpiryBoardDTO> onLoaded = board -> { };
    private LocalDate rulesAppliedOn;

    public ExpiryPanel(ExpiryApiClient api, ComercialApiClient comercialApi) {
        this.api = api;
        this.comercialApi = comercialApi;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(feedback, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Lotes por Vencer", UIHelper.icon("fas-hourglass-half", 15, UIHelper.PENDING_YELLOW), buildBoard());
        tabs.addTab("Regras de Remarcação", UIHelper.icon("fas-percent", 15, UIHelper.ACCENT_PINK), buildRules());
        add(tabs, BorderLayout.CENTER);
        updateActions();
    }

    public void onLoaded(Consumer<ExpiryBoardDTO> listener) {
        this.onLoaded = listener == null ? board -> { } : listener;
    }

    private JComponent buildBoard() {
        UIHelper.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setCellRenderer(new StageRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.money());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateActions();
        });
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        JTextField search = TableFilter.searchField("Artigo, lote ou armazém…");
        search.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> stage = TableFilter.combo("Todas as situações", EXPIRED, TODAY, SOON, WEEK, LATER);
        UIHelper.styleComboBox(stage);
        stage.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(table, search, new TableFilter.ColumnFilter(stage, 0));
        JPanel filters = TableFilter.bar(search, TableFilter.label("Situação:"), stage);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));

        markdownBtn.setIcon(UIHelper.icon("fas-tag", 14));
        markdownBtn.setToolTipText("Desconto no POS até ao fim da validade do lote (só gestão)");
        markdownBtn.addActionListener(e -> openMarkdown());
        writeOffBtn.setIcon(UIHelper.icon("fas-trash-alt", 14));
        writeOffBtn.setToolTipText("Abate o lote vencido como quebra por validade (aprovação acima da alçada)");
        writeOffBtn.addActionListener(e -> writeOff());
        ModernButton applyBtn = UIHelper.createSecondaryButton("Aplicar Regras");
        applyBtn.setIcon(UIHelper.icon("fas-magic", 14));
        applyBtn.setToolTipText("Remarca agora todos os lotes cuja janela de desconto já começou");
        applyBtn.addActionListener(e -> applyRules(false));
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::reload);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(UIHelper.tableCardTop("Lotes Vencidos e por Vencer (14 dias)", filters, refreshBtn, applyBtn, writeOffBtn, markdownBtn),
                BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildRules() {
        UIHelper.styleTable(ruleTable);
        ruleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ruleTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.status());
        ruleTable.getSelectionModel().addListSelectionListener(e -> editRuleBtn.setEnabled(selectedRule() != null));
        JScrollPane scroll = new JScrollPane(ruleTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        ModernButton newBtn = UIHelper.createSuccessButton("Nova Regra");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> openRule(null));
        editRuleBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editRuleBtn.setEnabled(false);
        editRuleBtn.addActionListener(e -> openRule(selectedRule()));
        JLabel note = new JLabel("Ex.: carne a 1 dia da validade com 40%; pão do dia anterior com 50%. A regra da categoria manda.");
        note.setForeground(UIHelper.TEXT_MUTED);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(UIHelper.tableCardTop("Regras de Remarcação", note, UIHelper.createRefreshButton(this::reload), editRuleBtn, newBtn),
                BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || api == null) return;
        if (isManager() && !LocalDate.now().equals(rulesAppliedOn)) {
            rulesAppliedOn = LocalDate.now();
            applyRules(true);
            return;
        }
        UIHelper.loadAsync(this, () -> new Object[]{api.board(companyId, 14), api.rules(companyId)}, loaded -> {
            apply((ExpiryBoardDTO) loaded[0]);
            @SuppressWarnings("unchecked") List<MarkdownRuleDTO> r = (List<MarkdownRuleDTO>) loaded[1];
            applyRulesTable(r);
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as validades", error.getMessage(), "Tentar novamente", this::reload));
    }

    private void apply(ExpiryBoardDTO board) {
        feedback.clear();
        lots.clear();
        lots.addAll(board.lots());
        model.setRowCount(0);
        for (ExpiryLotDTO l : lots) {
            model.addRow(new Object[]{stageLabel(l.stage()), UIHelper.formatDate(l.expirationDate()) + daysText(l.daysLeft()),
                    l.productName(), l.batchNumber(), l.warehouseName(), TableCellRenderers.format(l.quantity(), 3, ""), l.costValue(),
                    pct(l.markdownPct()), pct(l.suggestedPct())});
        }
        updateActions();
        onLoaded.accept(board);
    }

    private void applyRulesTable(List<MarkdownRuleDTO> loaded) {
        rules.clear();
        rules.addAll(loaded);
        ruleModel.setRowCount(0);
        for (MarkdownRuleDTO r : rules) {
            ruleModel.addRow(new Object[]{r.categoryName() == null ? "Todos os perecíveis" : r.categoryName(),
                    r.daysBefore() == 0 ? "No dia da validade" : r.daysBefore() + (r.daysBefore() == 1 ? " dia antes" : " dias antes"),
                    pct(r.percent()), r.active() ? "ACTIVO" : "INACTIVO"});
        }
    }

    private void applyRules(boolean silent) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(this, () -> api.applyRules(companyId), result -> {
            if (result.created() > 0) ToastManager.success(this, result.created() + " artigo(s) remarcado(s) pelas regras.");
            else if (!silent) ToastManager.success(this, "Nada a remarcar: os lotes já têm o desconto das regras.");
            loadBoardOnly(companyId);
        }, error -> {
            if (!silent) feedback.show(FeedbackType.ERROR, "Não foi possível aplicar as regras", error.getMessage(), null, null);
            loadBoardOnly(companyId);
        });
    }

    private void loadBoardOnly(Long companyId) {
        UIHelper.loadAsync(this, () -> new Object[]{api.board(companyId, 14), api.rules(companyId)}, loaded -> {
            apply((ExpiryBoardDTO) loaded[0]);
            @SuppressWarnings("unchecked") List<MarkdownRuleDTO> r = (List<MarkdownRuleDTO>) loaded[1];
            applyRulesTable(r);
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as validades", error.getMessage(), null, null));
    }

    private void updateActions() {
        ExpiryLotDTO lot = selected();
        markdownBtn.setEnabled(lot != null && lot.daysLeft() >= 0);
        writeOffBtn.setEnabled(lot != null && lot.daysLeft() <= 0);
    }

    private void openMarkdown() {
        ExpiryLotDTO lot = selected();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (lot == null || companyId == null) return;
        BigDecimal start = lot.suggestedPct() != null ? lot.suggestedPct() : new BigDecimal("20");
        DecimalField percent = new DecimalField(start.stripTrailingZeros().toPlainString(), 2, true);
        JLabel info = new JLabel("<html><div style='width:340px'><b>" + lot.productName() + "</b> · lote " + lot.batchNumber()
                + "<br>Validade " + UIHelper.formatDate(lot.expirationDate()) + daysText(lot.daysLeft()) + " · "
                + TableCellRenderers.format(lot.quantity(), 3, "") + " em stock<br><br>O POS aplica o desconto sozinho até ao fim "
                + "da validade. Como a venda sai do lote mais antigo, este lote sai primeiro.</div></html>");
        info.setForeground(UIHelper.TEXT_LIGHT);
        FormField percentForm = new FormField("Desconto (%)", percent, true, "Entre 1 e 90");
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Remarcar Artigo", "fas-tag",
                "Promoção até ao fim da validade", UIHelper.createDialogForm(1, new Object[]{"", info, "", percentForm}))
                .setConfirmButton("Remarcar", "fas-tag");
        ExpiryLotDTO[] result = new ExpiryLotDTO[1];
        dialog.setOnSaveAsync(() -> {
            BigDecimal value = percent.value();
            if (value == null || value.compareTo(BigDecimal.ONE) < 0 || value.compareTo(BigDecimal.valueOf(90)) > 0) {
                percentForm.setError("Indique um desconto entre 1 e 90%.");
                throw new IllegalArgumentException("Indique um desconto entre 1 e 90%.");
            }
            return () -> { result[0] = api.markdown(companyId, lot.batchId(), value); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, lot.productName() + " remarcado com " + pct(result[0].markdownPct()) + " até "
                + UIHelper.formatDate(lot.expirationDate()) + ".");
        loadBoardOnly(companyId);
    }

    private void writeOff() {
        ExpiryLotDTO lot = selected();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (lot == null || companyId == null) return;
        JTextField notes = new JTextField();
        UIHelper.styleTextField(notes);
        JLabel info = new JLabel("<html><div style='width:340px'>Abater <b>" + TableCellRenderers.format(lot.quantity(), 3, "")
                + "</b> de <b>" + lot.productName() + "</b> (lote " + lot.batchNumber() + ", " + UIHelper.formatMzn(lot.costValue())
                + " a custo) como quebra por validade. Acima da alçada fica à espera de aprovação da gestão.</div></html>");
        info.setForeground(UIHelper.TEXT_LIGHT);
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Abater Lote Vencido", "fas-trash-alt",
                "Quebra por validade", UIHelper.createDialogForm(1, new Object[]{"", info, "Observações:", notes}))
                .setConfirmButton("Abater", "fas-trash-alt");
        String key = "EXP-" + UUID.randomUUID();
        Object[] result = new Object[1];
        dialog.setOnSaveAsync(() -> {
            WriteOffExpiredRequest request = new WriteOffExpiredRequest(companyId, lot.batchId(), notes.getText().trim(), key);
            return () -> { result[0] = api.writeOff(request); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, "Lote " + lot.batchNumber() + " enviado para abate por validade.");
        loadBoardOnly(companyId);
    }

    private void openRule(MarkdownRuleDTO existing) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(this, comercialApi::getActiveCategories, categories -> showRuleDialog(companyId, categories, existing),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as categorias", error.getMessage(), null, null));
    }

    private void showRuleDialog(Long companyId, List<ProductCategoryDTO> categories, MarkdownRuleDTO existing) {
        JComboBox<String> category = new JComboBox<>();
        category.addItem("Todos os perecíveis");
        categories.forEach(c -> category.addItem(c.name()));
        UIHelper.styleComboBox(category);
        if (existing != null && existing.categoryId() != null) {
            for (int i = 0; i < categories.size(); i++) if (categories.get(i).id().equals(existing.categoryId())) category.setSelectedIndex(i + 1);
        }
        JSpinner days = new JSpinner(new SpinnerNumberModel(existing == null ? 1 : existing.daysBefore(), 0, 60, 1));
        DecimalField percent = new DecimalField(existing == null ? "30" : existing.percent().stripTrailingZeros().toPlainString(), 2, true);
        JCheckBox active = new JCheckBox("Regra activa", existing == null || existing.active());
        active.setOpaque(false);
        active.setForeground(UIHelper.TEXT_LIGHT);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"Categoria:", category,
                "Dias antes da validade (0 = no próprio dia):", days, "Desconto (%):", percent, "Estado:", active});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, existing == null ? "Nova Regra de Remarcação" : "Editar Regra",
                "fas-percent", "Desconto automático perto da validade", form);
        MarkdownRuleDTO[] result = new MarkdownRuleDTO[1];
        dialog.setOnSaveAsync(() -> {
            BigDecimal value = percent.value();
            if (value == null || value.compareTo(BigDecimal.ONE) < 0 || value.compareTo(BigDecimal.valueOf(90)) > 0) {
                throw new IllegalArgumentException("Indique um desconto entre 1 e 90%.");
            }
            Long categoryId = category.getSelectedIndex() <= 0 ? null : categories.get(category.getSelectedIndex() - 1).id();
            SaveMarkdownRuleRequest request = new SaveMarkdownRuleRequest(existing == null ? null : existing.id(), companyId,
                    categoryId, (Integer) days.getValue(), value, active.isSelected());
            return () -> { result[0] = api.saveRule(request); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, "Regra de remarcação guardada.");
        rulesAppliedOn = null;
        reload();
    }

    private ExpiryLotDTO selected() {
        int row = TableFilter.selectedModelRow(table);
        return row < 0 || row >= lots.size() ? null : lots.get(row);
    }

    private MarkdownRuleDTO selectedRule() {
        int row = TableFilter.selectedModelRow(ruleTable);
        return row < 0 || row >= rules.size() ? null : rules.get(row);
    }

    private static boolean isManager() {
        String role = CurrentUserContext.getRole();
        return "ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
    }

    static String stageLabel(String stage) {
        return switch (stage == null ? "" : stage) {
            case "EXPIRED" -> EXPIRED;
            case "TODAY" -> TODAY;
            case "SOON" -> SOON;
            case "WEEK" -> WEEK;
            default -> LATER;
        };
    }

    static String daysText(long daysLeft) {
        if (daysLeft < 0) return "  (há " + (-daysLeft) + (daysLeft == -1 ? " dia)" : " dias)");
        if (daysLeft == 0) return "  (hoje)";
        return "  (" + daysLeft + (daysLeft == 1 ? " dia)" : " dias)");
    }

    private static String pct(BigDecimal value) {
        return value == null || value.signum() == 0 ? "—" : value.stripTrailingZeros().toPlainString().replace('.', ',') + "%";
    }

    private static DefaultTableModel readOnly(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    /** Stage badge: red for expired and today, amber for the next two days, blue for this week. */
    private static final class StageRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            String text = value == null ? "" : value.toString();
            Color color = EXPIRED.equals(text) || TODAY.equals(text) ? UIHelper.REJECTED_RED
                    : SOON.equals(text) ? UIHelper.PENDING_YELLOW : WEEK.equals(text) ? UIHelper.ACCENT_BLUE : UIHelper.TEXT_MUTED;
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            if (!selected) label.setForeground(color);
            label.setIcon(UIHelper.icon("fas-circle", 8, color));
            label.setIconTextGap(6);
            return label;
        }
    }
}
