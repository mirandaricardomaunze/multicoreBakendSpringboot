package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.VariantsApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.variants.dto.AddVariantValuesRequest;
import mz.multicore.erp.modules.variants.dto.CreateVariantFamilyRequest;
import mz.multicore.erp.modules.variants.dto.VariantDTO;
import mz.multicore.erp.modules.variants.dto.VariantFamilyDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Variantes (SA-C04, {@code docs/VARIANTES_ARTIGO_SPEC.md}). Families on the left; on the right the grid of the
 * selected family, one row per value of the first attribute and one column per value of the second, showing the
 * stock of every combination (empty cells in red). Each variant is an ordinary product, sold and counted as usual.
 */
public class VariantsPanel extends JPanel {

    private static final String[] FAMILY_COLUMNS = {"Família", "Prefixo", "Atributos", "Variantes", "Stock total"};

    private final VariantsApiClient api;
    private final ComercialApiClient comercialApi;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DefaultTableModel familyModel = new DefaultTableModel(FAMILY_COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable familyTable = new JTable(familyModel);
    private final JTable grid = new JTable();
    private final JLabel gridTitle = new JLabel(" ");
    private final JLabel gridHint = new JLabel(" ");
    private final ModernButton addValuesBtn = UIHelper.createSecondaryButton("Acrescentar Valores");
    private final ModernButton priceBtn = UIHelper.createSecondaryButton("Preço Único");
    private final CardLayout cards = new CardLayout();
    private final JPanel detail = new JPanel(cards);
    private final List<VariantFamilyDTO> families = new ArrayList<>();
    private Consumer<List<VariantFamilyDTO>> onLoaded = list -> { };

    public VariantsPanel(VariantsApiClient api, ComercialApiClient comercialApi) {
        this.api = api;
        this.comercialApi = comercialApi;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(feedback, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildList(), buildDetail());
        split.setOpaque(false);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setResizeWeight(0.42);
        split.setDividerSize(10);
        ResponsiveSupport.adaptiveSplit(split, 980);
        add(split, BorderLayout.CENTER);
        addValuesBtn.setEnabled(false);
        priceBtn.setEnabled(false);
    }

    public void onLoaded(Consumer<List<VariantFamilyDTO>> listener) {
        this.onLoaded = listener == null ? list -> { } : listener;
    }

    private JComponent buildList() {
        UIHelper.styleTable(familyTable);
        familyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        familyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showGrid();
        });
        JScrollPane scroll = new JScrollPane(familyTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        JTextField search = TableFilter.searchField("Família ou prefixo…");
        search.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(familyTable, search);
        JPanel filters = TableFilter.bar(search);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        ModernButton newBtn = UIHelper.createSuccessButton("Nova Família");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.setToolTipText("Cria um artigo por combinação de tamanho, cor, espessura ou sabor");
        newBtn.addActionListener(e -> openCreate());
        addValuesBtn.setIcon(UIHelper.icon("fas-layer-group", 14));
        addValuesBtn.addActionListener(e -> openAddValues());
        priceBtn.setIcon(UIHelper.icon("fas-tag", 14));
        priceBtn.addActionListener(e -> openPrice());
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(ResponsiveSupport.cardTop("Famílias de Variantes", filters, UIHelper.createRefreshButton(this::reload),
                priceBtn, addValuesBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildDetail() {
        ModernPanel empty = new ModernPanel(16);
        empty.setLayout(new GridBagLayout());
        JLabel hint = new JLabel("<html><div style='text-align:center;width:300px'><b>Grelha de stock</b><br>Escolha uma família "
                + "para ver o stock de cada tamanho e cor. As casas a zero ficam a vermelho.</div></html>",
                UIHelper.icon("fas-th", 36, UIHelper.ACCENT_PINK), SwingConstants.CENTER);
        hint.setHorizontalTextPosition(SwingConstants.CENTER);
        hint.setVerticalTextPosition(SwingConstants.BOTTOM);
        hint.setForeground(UIHelper.TEXT_MUTED);
        empty.add(hint);
        UIHelper.styleTable(grid);
        grid.setDefaultRenderer(Object.class, new StockCellRenderer());
        grid.setRowSelectionAllowed(false);
        JScrollPane scroll = new JScrollPane(grid);
        UIHelper.styleScrollPane(scroll);
        gridTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        gridTitle.setForeground(UIHelper.TEXT_LIGHT);
        gridHint.setForeground(UIHelper.TEXT_MUTED);
        JPanel head = new JPanel(new BorderLayout(0, 2));
        head.setOpaque(false);
        head.add(gridTitle, BorderLayout.NORTH);
        head.add(gridHint, BorderLayout.CENTER);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(head, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        detail.setOpaque(false);
        detail.add(empty, "empty");
        detail.add(card, "grid");
        cards.show(detail, "empty");
        return detail;
    }

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || api == null) return;
        Long keep = selected() == null ? null : selected().id();
        UIHelper.loadAsync(this, () -> api.families(companyId), loaded -> apply(loaded, keep),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as variantes", error.getMessage(), "Tentar novamente", this::reload));
    }

    private void apply(List<VariantFamilyDTO> loaded, Long keep) {
        feedback.clear();
        families.clear();
        families.addAll(loaded);
        familyModel.setRowCount(0);
        for (VariantFamilyDTO f : families) {
            familyModel.addRow(new Object[]{f.name(), f.skuPrefix(), f.attribute1() + (f.attribute2() == null ? "" : " × " + f.attribute2()),
                    f.variants().size(), qty(f.totalStock())});
        }
        if (keep != null) {
            for (int i = 0; i < families.size(); i++) {
                if (!families.get(i).id().equals(keep)) continue;
                int view = familyTable.convertRowIndexToView(i);
                if (view >= 0) familyTable.setRowSelectionInterval(view, view);
            }
        }
        showGrid();
        onLoaded.accept(List.copyOf(families));
    }

    private void showGrid() {
        VariantFamilyDTO f = selected();
        addValuesBtn.setEnabled(f != null);
        priceBtn.setEnabled(f != null);
        if (f == null) {
            cards.show(detail, "empty");
            return;
        }
        List<String> rows = new ArrayList<>(new LinkedHashSet<>(f.variants().stream().map(VariantDTO::value1).toList()));
        List<String> cols = f.attribute2() == null ? List.of("Stock")
                : new ArrayList<>(new LinkedHashSet<>(f.variants().stream().map(VariantDTO::value2).toList()));
        Map<String, VariantDTO> byKey = new HashMap<>();
        f.variants().forEach(v -> byKey.put(v.value1() + "|" + (v.value2() == null ? "Stock" : v.value2()), v));
        String[] header = new String[cols.size() + 1];
        header[0] = f.attribute1();
        for (int i = 0; i < cols.size(); i++) header[i + 1] = cols.get(i);
        DefaultTableModel model = new DefaultTableModel(header, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (String r : rows) {
            Object[] line = new Object[cols.size() + 1];
            line[0] = r;
            for (int i = 0; i < cols.size(); i++) {
                VariantDTO v = byKey.get(r + "|" + cols.get(i));
                line[i + 1] = v == null ? "" : v;
            }
            model.addRow(line);
        }
        grid.setModel(model);
        gridTitle.setText(f.name() + "  ·  " + f.variants().size() + " variantes");
        gridHint.setText("Stock total " + qty(f.totalStock()) + "  ·  modelo " + f.templateProductName()
                + "  ·  passe o rato numa casa para ver o código e o preço");
        cards.show(detail, "grid");
    }

    private void openCreate() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(this, comercialApi::getAllProducts, products -> {
            SearchableComboBox<ProductDTO> template = BreakdownFormDialog.productCombo(products, "Artigo modelo (preço, custo, IVA, categoria)");
            JTextField name = new JTextField();
            UIHelper.styleTextField(name);
            JTextField prefix = new JTextField();
            UIHelper.styleTextField(prefix);
            JTextField attr1 = new JTextField("Tamanho");
            UIHelper.styleTextField(attr1);
            JTextField values1 = new JTextField("S, M, L, XL");
            UIHelper.styleTextField(values1);
            JTextField attr2 = new JTextField("Cor");
            UIHelper.styleTextField(attr2);
            JTextField values2 = new JTextField("Branco, Preto, Azul");
            UIHelper.styleTextField(values2);
            JTextArea preview = ResponsiveSupport.wrappingText(" ", new Font(UIHelper.FONT, Font.PLAIN, 12), UIHelper.TEXT_MUTED);
            Runnable refresh = () -> {
                int a = split(values1.getText()).size();
                int b = attr2.getText().isBlank() ? 1 : Math.max(1, split(values2.getText()).size());
                preview.setText("Vão ser criados " + (a * b) + " artigos, um por combinação, com códigos "
                        + (prefix.getText().isBlank() ? "PREFIXO" : prefix.getText().trim().toUpperCase()) + "-VALOR. "
                        + "Deixe o segundo atributo vazio para variantes de uma só dimensão (ex.: espessura do varão).");
            };
            for (JTextField f : new JTextField[]{values1, values2, attr2, prefix}) BreakdownFormDialog.onChange(f, refresh);
            refresh.run();
            FormField nameForm = new FormField("Nome da família", name, true, "Ex.: Camisa Polo, Varão nervurado, Bolo de festa");
            FormField prefixForm = new FormField("Prefixo do código", prefix, true, "Letras, números e hífen, ex.: POLO");
            FormField templateForm = new FormField("Artigo modelo", template, true, null);
            JPanel form = UIHelper.createDialogForm(1, new Object[]{"", nameForm, "", prefixForm, "", templateForm,
                    "Primeiro atributo:", attr1, "Valores (separados por vírgula):", values1,
                    "Segundo atributo (opcional):", attr2, "Valores (separados por vírgula):", values2, "", preview});
            ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Nova Família de Variantes", "fas-th",
                    "Um artigo por combinação", form).setConfirmButton("Criar Variantes", "fas-check");
            VariantFamilyDTO[] result = new VariantFamilyDTO[1];
            dialog.setOnSaveAsync(() -> {
                if (!nameForm.validateRequired() || !prefixForm.validateRequired()) throw new IllegalArgumentException("Preencha nome e prefixo.");
                ProductDTO t = template.selectedValue();
                if (t == null) {
                    templateForm.setError("Escolha o artigo modelo.");
                    throw new IllegalArgumentException("Escolha o artigo modelo.");
                }
                CreateVariantFamilyRequest request = new CreateVariantFamilyRequest(companyId, name.getText().trim(), prefix.getText().trim(),
                        t.id(), attr1.getText().trim(), split(values1.getText()), attr2.getText().trim(), split(values2.getText()));
                return () -> { result[0] = api.create(request); return null; };
            });
            if (!dialog.showDialog() || result[0] == null) return;
            ToastManager.success(this, result[0].variants().size() + " variantes de " + result[0].name() + " criadas.");
            Long id = result[0].id();
            UIHelper.loadAsync(this, () -> api.families(companyId), loaded -> apply(loaded, id), error -> reload());
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar os artigos", error.getMessage(), null, null));
    }

    private void openAddValues() {
        VariantFamilyDTO f = selected();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (f == null || companyId == null) return;
        JTextField values1 = new JTextField();
        UIHelper.styleTextField(values1);
        JTextField values2 = new JTextField();
        UIHelper.styleTextField(values2);
        values2.setEnabled(f.attribute2() != null);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"Novos valores de " + f.attribute1() + ":", values1,
                f.attribute2() == null ? "(família de um atributo)" : "Novos valores de " + f.attribute2() + ":", values2});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Acrescentar Valores a " + f.name(), "fas-layer-group",
                "Só as combinações que faltam são criadas", form);
        VariantFamilyDTO[] result = new VariantFamilyDTO[1];
        dialog.setOnSaveAsync(() -> {
            AddVariantValuesRequest request = new AddVariantValuesRequest(companyId, split(values1.getText()), split(values2.getText()));
            return () -> { result[0] = api.addValues(f.id(), request); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, (result[0].variants().size() - f.variants().size()) + " variantes novas criadas.");
        reload();
    }

    private void openPrice() {
        VariantFamilyDTO f = selected();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (f == null || companyId == null) return;
        MoneyField price = new MoneyField(f.variants().isEmpty() || f.variants().get(0).unitPrice() == null ? ""
                : f.variants().get(0).unitPrice().toPlainString());
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Preço Único de " + f.name(), "fas-tag",
                "O mesmo preço para as " + f.variants().size() + " variantes",
                UIHelper.createDialogForm(1, new Object[]{"Preço de venda (MT):", price}));
        VariantFamilyDTO[] result = new VariantFamilyDTO[1];
        dialog.setOnSaveAsync(() -> {
            BigDecimal value = price.value();
            if (value == null || value.signum() <= 0) throw new IllegalArgumentException("Indique um preço positivo.");
            return () -> { result[0] = api.setPrice(companyId, f.id(), value); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, "Preço actualizado nas variantes de " + f.name() + ".");
        reload();
    }

    private VariantFamilyDTO selected() {
        int row = TableFilter.selectedModelRow(familyTable);
        return row < 0 || row >= families.size() ? null : families.get(row);
    }

    static List<String> split(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split("[,;]")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String qty(BigDecimal value) {
        return value == null ? "—" : TableCellRenderers.format(value, 3, "");
    }

    /** Shows the stock of each combination; red at zero, empty when the combination does not exist. */
    private static final class StockCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            label.setToolTipText(null);
            label.setHorizontalAlignment(column == 0 ? SwingConstants.LEFT : SwingConstants.CENTER);
            if (value instanceof VariantDTO v) {
                label.setText(qty(v.stock()));
                label.setToolTipText(v.sku() + "  ·  " + UIHelper.formatMzn(v.unitPrice()));
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                if (!selected) label.setForeground(v.stock().signum() > 0 ? UIHelper.TEXT_LIGHT : UIHelper.REJECTED_RED);
            } else if (column > 0) {
                label.setText("—");
                if (!selected) label.setForeground(UIHelper.TEXT_MUTED);
            }
            return label;
        }
    }
}
