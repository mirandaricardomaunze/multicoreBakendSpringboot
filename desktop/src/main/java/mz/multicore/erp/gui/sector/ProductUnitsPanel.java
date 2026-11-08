package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.UnitsApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.units.dto.ProductUnitDTO;
import mz.multicore.erp.modules.units.dto.ProductUnitsDTO;
import mz.multicore.erp.modules.units.dto.SetBaseUnitRequest;
import mz.multicore.erp.modules.units.dto.UnitOfMeasureDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchase and sales units of a product (SA-C02, {@code docs/UNIDADES_MEDIDA_SPEC.md}): base unit,
 * alternative units (bag, pallet, load, metre, ...) and a quick converter. Stock and prices always stay in the
 * base unit; this panel only defines how other units convert.
 */
public class ProductUnitsPanel extends JPanel {

    private static final String[] COLUMNS = {"Unidade", "Equivale a", "Código de barras", "Venda", "Compra", "Padrão", "Origem"};

    private final UnitsApiClient unitsApiClient;
    private final Long productId;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel baseValue = new JLabel("—");
    private final JLabel lockLabel = new JLabel();
    private final ModernButton changeBaseBtn = UIHelper.createSecondaryButton("Alterar base");
    private final ModernButton addBtn = UIHelper.createSuccessButton("Adicionar unidade");
    private final ModernButton editBtn = UIHelper.createSecondaryButton("Editar");
    private final ModernButton removeBtn = UIHelper.createDangerButton("Remover");
    private final DecimalField convertQty = new DecimalField("1", 3, true);
    private final JComboBox<String> convertUnit = new JComboBox<>();
    private final JLabel convertResult = new JLabel(" ");

    private ProductUnitsDTO current;
    private List<UnitOfMeasureDTO> catalog = List.of();
    private final List<ProductUnitDTO> rows = new ArrayList<>();

    public ProductUnitsPanel(UnitsApiClient unitsApiClient, Long productId) {
        this.unitsApiClient = unitsApiClient;
        this.productId = productId;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(10, 4, 4, 4));

        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(feedback, BorderLayout.NORTH);
        north.add(buildBaseCard(), BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        add(buildTableCard(), BorderLayout.CENTER);
        add(buildConverterCard(), BorderLayout.SOUTH);
        reload();
    }

    private JComponent buildBaseCard() {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel icon = new JLabel(UIHelper.icon("fas-ruler-combined", 22, UIHelper.MODULE_STOCK));
        card.add(icon, BorderLayout.WEST);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        JLabel caption = new JLabel("Unidade base · stock, preços e documentos");
        caption.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        caption.setForeground(UIHelper.TEXT_MUTED);
        baseValue.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        baseValue.setForeground(UIHelper.TEXT_LIGHT);
        text.add(caption);
        text.add(baseValue);
        card.add(text, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        lockLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lockLabel.setForeground(UIHelper.TEXT_MUTED);
        right.add(lockLabel);
        changeBaseBtn.setIcon(UIHelper.icon("fas-exchange-alt", 14));
        changeBaseBtn.setToolTipText("Só possível antes de o produto ter stock, documentos ou outras unidades");
        changeBaseBtn.addActionListener(e -> changeBase());
        right.add(changeBaseBtn);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JComponent buildTableCard() {
        UIHelper.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> syncButtons());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);

        addBtn.setIcon(UIHelper.icon("fas-plus", 14));
        addBtn.setToolTipText("Saco, palete, carga, metro, caixa…");
        addBtn.addActionListener(e -> openUnitDialog(null));
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editBtn.addActionListener(e -> editSelected());
        removeBtn.setIcon(UIHelper.icon("fas-trash-alt", 14));
        removeBtn.addActionListener(e -> removeSelected());

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        card.add(ResponsiveSupport.cardTop("Unidades de compra e venda", null, addBtn, editBtn, removeBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        syncButtons();
        return card;
    }

    private JComponent buildConverterCard() {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        JLabel label = new JLabel("Converter");
        label.setIcon(UIHelper.icon("fas-calculator", 14, UIHelper.ACCENT_BLUE));
        label.setIconTextGap(6);
        label.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        label.setForeground(UIHelper.TEXT_LIGHT);
        convertQty.setPreferredSize(new Dimension(110, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleComboBox(convertUnit);
        convertUnit.setPreferredSize(new Dimension(180, UIHelper.FORM_CONTROL_HEIGHT));
        convertResult.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        convertResult.setForeground(UIHelper.TEXT_LIGHT);
        UIHelper.onTextChange(convertQty, this::updateConversion);
        convertUnit.addActionListener(e -> updateConversion());
        card.add(label);
        card.add(convertQty);
        card.add(convertUnit);
        card.add(convertResult);
        return card;
    }

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || unitsApiClient == null || productId == null) return;
        UIHelper.loadAsync(this, () -> new Loaded(unitsApiClient.units(companyId), unitsApiClient.productUnits(companyId, productId)),
                loaded -> {
                    catalog = loaded.catalog();
                    apply(loaded.units());
                },
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as unidades do produto",
                        error.getMessage(), "Tentar novamente", this::reload));
    }

    private void apply(ProductUnitsDTO units) {
        current = units;
        UnitOfMeasureDTO base = units.baseUnit();
        baseValue.setText(base.name() + " (" + base.symbol() + ")");
        if (units.baseUnitLocked()) {
            lockLabel.setIcon(UIHelper.icon("fas-lock", 12, UIHelper.PENDING_YELLOW));
            lockLabel.setText("Fixa");
            lockLabel.setToolTipText(units.baseUnitLockReason());
        } else {
            lockLabel.setIcon(UIHelper.icon("fas-lock-open", 12, UIHelper.APPROVED_GREEN));
            lockLabel.setText("Ainda pode mudar");
            lockLabel.setToolTipText("O produto ainda não tem stock, documentos nem outras unidades.");
        }
        changeBaseBtn.setEnabled(!units.baseUnitLocked());

        rows.clear();
        rows.addAll(units.units());
        model.setRowCount(0);
        for (ProductUnitDTO u : rows) {
            model.addRow(new Object[]{
                    u.unitName() + " (" + u.unitSymbol() + ")",
                    "1 = " + plain(u.factorToBase()) + " " + base.symbol(),
                    u.barcode() == null ? "—" : u.barcode(),
                    u.sellable() ? "Sim" : "Não",
                    u.purchasable() ? "Sim" : "Não",
                    defaultLabel(u),
                    u.derived() ? "Composição do produto" : "Definida aqui"
            });
        }

        Object previous = convertUnit.getSelectedItem();
        convertUnit.removeAllItems();
        convertUnit.addItem(base.name());
        for (ProductUnitDTO u : rows) convertUnit.addItem(u.unitName());
        if (previous != null) convertUnit.setSelectedItem(previous);
        updateConversion();
        syncButtons();
    }

    private void updateConversion() {
        if (current == null) return;
        int index = convertUnit.getSelectedIndex();
        BigDecimal factor = index <= 0 || index - 1 >= rows.size() ? BigDecimal.ONE : rows.get(index - 1).factorToBase();
        BigDecimal qty = ProductUnitFormDialog.parse(convertQty.getText());
        if (qty != null) {
            UnitOfMeasureDTO base = current.baseUnit();
            BigDecimal result = qty.multiply(factor);
            BigDecimal rounded = result.setScale(base.decimals(), RoundingMode.HALF_UP);
            boolean exact = rounded.compareTo(result) == 0;
            convertResult.setForeground(exact ? UIHelper.TEXT_LIGHT : UIHelper.REJECTED_RED);
            convertResult.setText("= " + plain(result) + " " + base.symbol()
                    + (exact ? "" : "  (a base não aceita fracções)"));
        } else {
            convertResult.setForeground(UIHelper.TEXT_MUTED);
            convertResult.setText("Indique uma quantidade");
        }
    }

    private void syncButtons() {
        ProductUnitDTO selected = selected();
        boolean editable = selected != null && !selected.derived();
        editBtn.setEnabled(editable);
        removeBtn.setEnabled(editable);
        String hint = selected != null && selected.derived()
                ? "A caixa e a embalagem mudam-se na ficha do produto" : null;
        editBtn.setToolTipText(hint);
        removeBtn.setToolTipText(hint);
    }

    private ProductUnitDTO selected() {
        int row = TableFilter.selectedModelRow(table);
        return row < 0 || row >= rows.size() ? null : rows.get(row);
    }

    private void editSelected() {
        ProductUnitDTO selected = selected();
        if (selected == null || selected.derived()) return;
        openUnitDialog(selected);
    }

    private void openUnitDialog(ProductUnitDTO existing) {
        if (current == null) return;
        ProductUnitFormDialog.show(this, unitsApiClient, current, catalog, existing, updated -> {
            apply(updated);
            ToastManager.success(this, existing == null ? "Unidade adicionada." : "Unidade actualizada.");
        });
    }

    private void removeSelected() {
        ProductUnitDTO selected = selected();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (selected == null || selected.derived() || companyId == null) return;
        if (!UIHelper.confirm(this, "Remover a unidade " + selected.unitName() + " deste produto?\n"
                + "O stock e os documentos não mudam: continuam na unidade base.", "Confirmar")) return;
        UIHelper.submitAsync(removeBtn, () -> {
            unitsApiClient.deleteProductUnit(companyId, selected.id());
            return unitsApiClient.productUnits(companyId, productId);
        }, updated -> {
            apply(updated);
            ToastManager.success(this, "Unidade removida.");
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível remover a unidade", error.getMessage(), null, null));
    }

    private void changeBase() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (current == null || companyId == null || current.baseUnitLocked()) return;
        List<UnitOfMeasureDTO> physical = catalog.stream().filter(u -> !"PACKAGE".equals(u.dimension())).toList();
        SearchableComboBox<UnitOfMeasureDTO> combo = new SearchableComboBox<>("Pesquisar unidade",
                u -> u.name() + " (" + u.symbol() + ") · " + u.dimensionLabel(), u -> u.code() + " " + u.name());
        combo.setItems(physical);
        combo.selectFirst(u -> u.code().equals(current.baseUnit().code()));
        JLabel hint = new JLabel("<html>Exemplos: areia e brita em <b>m³</b>, varão em <b>kg</b> ou <b>barra</b>, "
                + "cabo em <b>m</b>. Embalagens como saco ou palete entram como unidade alternativa.</html>");
        hint.setForeground(UIHelper.TEXT_MUTED);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"Nova unidade base:", combo, "", hint});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Alterar unidade base",
                "fas-ruler-combined", current.productName(), form);
        ProductUnitsDTO[] result = new ProductUnitsDTO[1];
        dialog.setOnSaveAsync(() -> {
            UnitOfMeasureDTO unit = combo.selectedValue();
            if (unit == null) throw new IllegalArgumentException("Escolha a unidade base.");
            SetBaseUnitRequest request = new SetBaseUnitRequest(companyId, productId, unit.code());
            return () -> { result[0] = unitsApiClient.setBaseUnit(request); return null; };
        });
        if (dialog.showDialog() && result[0] != null) {
            apply(result[0]);
            ToastManager.success(this, "Unidade base alterada.");
        }
    }

    private static String defaultLabel(ProductUnitDTO u) {
        if (u.defaultSale() && u.defaultPurchase()) return "Venda e compra";
        if (u.defaultSale()) return "Venda";
        if (u.defaultPurchase()) return "Compra";
        return "—";
    }

    /** Quantity with its own decimals (0.001 or 40) in the central Mozambican number format. */
    static String plain(BigDecimal value) {
        if (value == null) return "—";
        BigDecimal stripped = value.stripTrailingZeros();
        int scale = Math.max(0, stripped.scale());
        return TableCellRenderers.format(stripped.setScale(scale, RoundingMode.UNNECESSARY), scale, "");
    }

    private record Loaded(List<UnitOfMeasureDTO> catalog, ProductUnitsDTO units) {}
}
