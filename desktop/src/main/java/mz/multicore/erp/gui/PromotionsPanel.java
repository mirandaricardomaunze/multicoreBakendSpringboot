package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PromotionApiClient;
import mz.multicore.erp.modules.promotions.dto.PromotionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Gestão de promoções de loja: lista, criação e activação/desactivação.
 */
public class PromotionsPanel extends JPanel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PromotionApiClient promotionApiClient;
    private final ComercialApiClient comercialApiClient;

    private final DefaultTableModel model;
    private final JTable table;
    private final ModernButton toggleBtn;
    private final JComboBox<String> promoBusinessUnitCombo;
    private List<PromotionDTO> promotions = new ArrayList<>();
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    public PromotionsPanel(PromotionApiClient promotionApiClient, ComercialApiClient comercialApiClient) {
        this.promotionApiClient = promotionApiClient;
        this.comercialApiClient = comercialApiClient;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton newBtn = UIHelper.createSuccessButton("Nova Promoção");
        newBtn.setIcon(UIHelper.icon("fas-tags", 14));
        newBtn.addActionListener(e -> createPromotionDialog());
        toggleBtn = UIHelper.createSecondaryButton("Activar / Desactivar");
        toggleBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        toggleBtn.addActionListener(e -> toggleSelected());
        ModernButton refreshBtn = UIHelper.createRefreshButton(() -> reload());

        add(feedback, BorderLayout.NORTH);

        String[] cols = {"Nome", "Tipo", "Alcance", "Benefício", "Início", "Fim", "Estado", "Negócio"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        table.getColumnModel().getColumn(7).setPreferredWidth(120);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        JTextField promoSearch = TableFilter.searchField("Nome, alcance ou benefício…");
        promoSearch.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        promoBusinessUnitCombo = TableFilter.combo("Todos os negócios");
        promoBusinessUnitCombo.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> promoTipo = TableFilter.combo("Todos os tipos", "Percentagem", "Leve X, pague Y");
        UIHelper.styleComboBox(promoTipo);
        promoTipo.setPreferredSize(new Dimension(135, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> promoEstado = TableFilter.combo("Todos os estados", "Activa", "Inactiva");
        UIHelper.styleComboBox(promoEstado);
        promoEstado.setPreferredSize(new Dimension(125, UIHelper.FORM_CONTROL_HEIGHT));

        TableFilter.install(table, promoSearch,
                new TableFilter.ColumnFilter(promoTipo, 1),
                new TableFilter.ColumnFilter(promoEstado, 6),
                new TableFilter.ColumnFilter(promoBusinessUnitCombo, 7));

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes da promoção (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar promoção");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel promoFilters = TableFilter.bar(
                promoSearch,
                TableFilter.label("Negócio:"), promoBusinessUnitCombo,
                TableFilter.label("Tipo:"), promoTipo,
                TableFilter.label("Estado:"), promoEstado,
                peekToggleBtn
        );
        promoFilters.setBorder(new EmptyBorder(0, 0, 10, 0));

        UIHelper.ensureHeadersFit(table);
        TableQuickPeekController peekController = TableQuickPeekController.install(table, card,
                (peek, mr) -> PromotionsOperationsCoordinator.populatePeek(peek, model, mr, toggleBtn::doClick));
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        card.add(UIHelper.tableCardTop("Promoções de Loja", promoFilters,
                refreshBtn, toggleBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    public void reload() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(this, () -> promotionApiClient.findByCompany(companyId), this::applyPromotions,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as promoções",
                        error.getMessage(), "Tentar novamente", this::reload));
    }

    private void applyPromotions(List<PromotionDTO> loaded) {
        promotions = loaded;
        model.setRowCount(0);
        Set<String> bus = new TreeSet<>();
        for (PromotionDTO p : promotions) {
            String buName = p.businessUnitName() != null && !p.businessUnitName().isBlank() ? p.businessUnitName() : "—";
            if (!"—".equals(buName)) bus.add(buName);
            model.addRow(new Object[]{
                    p.name(),
                    "PERCENT".equals(p.type()) ? "Percentagem" : "Leve X, pague Y",
                    p.productName() != null ? "Produto: " + p.productName()
                            : p.categoryName() != null ? "Categoria: " + p.categoryName() : "—",
                    PromotionsOperationsCoordinator.benefit(p),
                    p.startDate() != null ? p.startDate().format(FMT) : "—",
                    p.endDate() != null ? p.endDate().format(FMT) : "—",
                    p.active() ? "ACTIVA" : "INACTIVA",
                    buName
            });
        }
        if (promoBusinessUnitCombo != null) {
            Object prev = promoBusinessUnitCombo.getSelectedItem();
            promoBusinessUnitCombo.removeAllItems();
            promoBusinessUnitCombo.addItem("Todos os negócios");
            for (String b : bus) promoBusinessUnitCombo.addItem(b);
            if (prev != null) promoBusinessUnitCombo.setSelectedItem(prev);
        }
    }

    private void toggleSelected() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0 || row >= promotions.size()) {
            feedback.show(FeedbackType.WARNING, "Seleccione uma promoção",
                    "Escolha uma promoção na tabela para continuar.", null, null);
            return;
        }
        PromotionDTO selected = promotions.get(row);
        UIHelper.submitAsync(toggleBtn, () -> {
            promotionApiClient.setActive(selected.id(), !selected.active());
            return null;
        }, ignored -> reload(), error -> feedback.show(FeedbackType.ERROR,
                "Não foi possível actualizar a promoção", error.getMessage(), null, null));
    }

    private void createPromotionDialog() {
        PromotionFormDialog.show(this, promotionApiClient, comercialApiClient, this::reload, feedback);
    }
}
