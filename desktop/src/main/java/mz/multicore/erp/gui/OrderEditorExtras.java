package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.PricingApiClient;
import mz.multicore.erp.desktop.client.ProjectsApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.projects.dto.CustomerProjectDTO;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Extra row of the order editor: promised delivery (days and terms), the client's project and a badge with
 * the price list that will apply. The order request already carried the delivery fields; the editor never
 * filled them, so every direct order showed "Sem data". Project and price clients are optional Swing client
 * properties installed by {@code SectorDesktopWiring}; without them the row only offers delivery.
 */
public final class OrderEditorExtras {

    public static final String PRICING_KEY = "multicore.sector.pricingApiClient";
    public static final String PROJECTS_KEY = "multicore.sector.projectsApiClient";
    private static final String SELF_KEY = "multicore.orderEditorExtras";
    private static final ProjectChoice NO_PROJECT = new ProjectChoice(null, "Sem obra");

    private record ProjectChoice(Long id, String label) {
        @Override public String toString() { return label; }
    }

    private final ComercialPanel owner;
    private final IntegerField deliveryDays = new IntegerField("", 1, 365, "Prazo de entrega em dias");
    private final JTextField deliveryTerms = new JTextField();
    private final JComboBox<ProjectChoice> projectCombo = new JComboBox<>(new ProjectChoice[]{NO_PROJECT});
    private final ModernButton newProjectBtn = UIHelper.createSecondaryButton("");
    private final JLabel priceBadge = new JLabel(" ");

    private OrderEditorExtras(ComercialPanel owner) {
        this.owner = owner;
    }

    /** Builds the row and remembers it on the panel. Called once by the order header form. */
    static JComponent build(ComercialPanel owner) {
        OrderEditorExtras extras = new OrderEditorExtras(owner);
        owner.putClientProperty(SELF_KEY, extras);
        return extras.layout();
    }

    private JComponent layout() {
        UIHelper.styleTextField(deliveryTerms);
        deliveryDays.setToolTipText("Dias a contar de hoje. Vazio: sem data prometida.");
        deliveryDays.putClientProperty("JTextField.placeholderText", "ex.: 3");
        deliveryTerms.putClientProperty("JTextField.placeholderText", "ex.: entrega na obra, descarga pelo cliente");
        UIHelper.styleComboBox(projectCombo);
        projectCombo.setToolTipText("Obra ou projecto do cliente: dá um extracto por obra");
        newProjectBtn.setIcon(UIHelper.icon("fas-plus", 12, UIHelper.APPROVED_GREEN));
        newProjectBtn.setToolTipText("Nova obra para este cliente");
        newProjectBtn.getAccessibleContext().setAccessibleName("Nova obra");
        newProjectBtn.setPreferredSize(new Dimension(38, UIHelper.FORM_CONTROL_HEIGHT));
        newProjectBtn.addActionListener(e -> createProject());
        priceBadge.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        priceBadge.setForeground(UIHelper.TEXT_MUTED);
        UIHelper.onTextChange(deliveryDays, owner::markOrderEditorDirty);
        UIHelper.onTextChange(deliveryTerms, owner::markOrderEditorDirty);
        if (owner.orderClientCombo != null) owner.orderClientCombo.addActionListener(e -> clientChanged());

        JPanel projectRow = new JPanel(new BorderLayout(6, 0));
        projectRow.setOpaque(false);
        projectRow.add(projectCombo, BorderLayout.CENTER);
        projectRow.add(newProjectBtn, BorderLayout.EAST);

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 10);
        g.gridy = 0;
        g.gridx = 0; g.weightx = 0.35; row.add(block("Entregar em (dias):", deliveryDays), g);
        g.gridx = 1; g.weightx = 1.2; row.add(block("Condições de entrega:", deliveryTerms), g);
        g.gridx = 2; g.weightx = 1.2; row.add(block("Obra / projecto:", projectRow), g);
        g.gridx = 3; g.weightx = 1.0; g.insets = new Insets(0, 0, 0, 0); row.add(block("Preços aplicados:", priceBadge), g);
        clientChanged();
        return row;
    }

    // ─── Read by the submission ─────────────────────────────────────────────────

    static Integer deliveryDays(ComercialPanel owner) {
        OrderEditorExtras extras = of(owner);
        if (extras == null || extras.deliveryDays.getText() == null || extras.deliveryDays.getText().isBlank()) return null;
        int days = extras.deliveryDays.value();
        if (days <= 0) throw new IllegalArgumentException("O prazo de entrega tem de ser de pelo menos um dia.");
        return days;
    }

    static String deliveryTerms(ComercialPanel owner) {
        OrderEditorExtras extras = of(owner);
        if (extras == null) return null;
        String text = extras.deliveryTerms.getText().trim();
        return text.isEmpty() ? null : text;
    }

    // ─── Lifecycle hooks ────────────────────────────────────────────────────────

    /** New order: empty delivery, no project. */
    static void reset(ComercialPanel owner) {
        OrderEditorExtras extras = of(owner);
        if (extras == null) return;
        extras.deliveryDays.setText("");
        extras.deliveryTerms.setText("");
        extras.deliveryDays.setEnabled(true);
        extras.deliveryTerms.setEnabled(true);
        extras.clientChanged();
    }

    /** Existing order: delivery is shown read-only (the update request does not change it) and its project is selected. */
    static void loadForOrder(ComercialPanel owner, OrderDTO order) {
        OrderEditorExtras extras = of(owner);
        if (extras == null || order == null) return;
        extras.deliveryDays.setText("");
        extras.deliveryTerms.setText(order.deliveryTerms() == null ? "" : order.deliveryTerms());
        extras.deliveryDays.setEnabled(false);
        extras.deliveryTerms.setEnabled(false);
        String promised = order.expectedDeliveryDate() == null ? "sem data" : UIHelper.formatDate(order.expectedDeliveryDate());
        extras.deliveryDays.setToolTipText("Entrega prevista: " + promised + ". Definida na criação da encomenda.");
        ProjectsApiClient projects = projects(owner);
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (projects == null || companyId == null) return;
        UIHelper.loadAsync(owner, () -> projects.projectOfOrder(companyId, order.id()), project -> {
            if (project != null) extras.select(project.id());
        }, error -> { });
    }

    /** After create or update: links the chosen project to the saved order. */
    static void afterSave(ComercialPanel owner, OrderDTO saved) {
        OrderEditorExtras extras = of(owner);
        ProjectsApiClient projects = projects(owner);
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (extras == null || projects == null || saved == null || companyId == null) return;
        ProjectChoice choice = (ProjectChoice) extras.projectCombo.getSelectedItem();
        if (choice == null || choice.id() == null) return;
        UIHelper.loadAsync(owner, () -> projects.attach(companyId, choice.id(), saved.id()),
                project -> ToastManager.success(owner, "Encomenda " + saved.orderNumber() + " registada na obra " + project.name() + "."),
                error -> owner.showCommercialError("associar a encomenda à obra", error));
    }

    // ─── Client-dependent parts ─────────────────────────────────────────────────

    private void clientChanged() {
        ClientDTO client = selectedClient();
        projectCombo.removeAllItems();
        projectCombo.addItem(NO_PROJECT);
        boolean named = client != null && !"000000000".equals(client.taxId());
        projectCombo.setEnabled(named);
        newProjectBtn.setEnabled(named && projects(owner) != null);
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (!named || companyId == null) {
            priceBadge.setText("Preço normal");
            return;
        }
        ProjectsApiClient projects = projects(owner);
        if (projects != null) {
            UIHelper.loadAsync(owner, () -> projects.list(companyId, client.id()), list -> fillProjects(client, list), error -> { });
        }
        PricingApiClient pricing = pricing(owner);
        if (pricing == null) {
            priceBadge.setText("Preço normal");
            return;
        }
        priceBadge.setText("A verificar…");
        UIHelper.loadAsync(owner, () -> pricing.clientAssignment(companyId, client.id()), assignment -> {
            if (selectedClient() == null || !selectedClient().id().equals(client.id())) return;
            boolean hasList = assignment != null && assignment.priceListId() != null;
            priceBadge.setForeground(hasList ? UIHelper.APPROVED_GREEN : UIHelper.TEXT_MUTED);
            priceBadge.setIcon(hasList ? UIHelper.icon("fas-tags", 13, UIHelper.APPROVED_GREEN) : null);
            priceBadge.setText(hasList ? "Tabela " + assignment.priceListName() : "Preço normal");
        }, error -> priceBadge.setText("Preço normal"));
    }

    private void fillProjects(ClientDTO client, List<CustomerProjectDTO> list) {
        if (selectedClient() == null || !selectedClient().id().equals(client.id())) return;
        for (CustomerProjectDTO p : list) {
            if (p.active()) projectCombo.addItem(new ProjectChoice(p.id(), p.name() + " (" + p.code() + ")"));
        }
    }

    private void select(Long projectId) {
        for (int i = 0; i < projectCombo.getItemCount(); i++) {
            if (projectId.equals(projectCombo.getItemAt(i).id())) {
                projectCombo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void createProject() {
        ClientDTO client = selectedClient();
        ProjectsApiClient projects = projects(owner);
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (client == null || projects == null || companyId == null) return;
        String name = UIHelper.promptRequiredText("Nova Obra", "fas-hard-hat", client.name(), "Nome da obra ou projecto:");
        if (name == null || name.isBlank()) return;
        UIHelper.loadAsync(owner, () -> projects.save(companyId, client.id(), name.trim()), project -> {
            projectCombo.addItem(new ProjectChoice(project.id(), project.name() + " (" + project.code() + ")"));
            select(project.id());
            ToastManager.success(owner, "Obra " + project.code() + " criada.");
        }, error -> owner.showCommercialError("criar obra", error));
    }

    private ClientDTO selectedClient() {
        if (owner.orderClientCombo == null) return null;
        int idx = owner.orderClientCombo.getSelectedIndex();
        return idx > 0 && idx - 1 < owner.clientsList.size() ? owner.clientsList.get(idx - 1) : null;
    }

    private static JPanel block(String label, JComponent input) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        JLabel caption = new JLabel(label);
        caption.setForeground(UIHelper.TEXT_MUTED);
        panel.add(caption, BorderLayout.NORTH);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    private static OrderEditorExtras of(ComercialPanel owner) {
        Object value = owner.getClientProperty(SELF_KEY);
        return value instanceof OrderEditorExtras extras ? extras : null;
    }

    private static ProjectsApiClient projects(JComponent owner) {
        return owner.getClientProperty(PROJECTS_KEY) instanceof ProjectsApiClient c ? c : null;
    }

    private static PricingApiClient pricing(JComponent owner) {
        return owner.getClientProperty(PRICING_KEY) instanceof PricingApiClient c ? c : null;
    }
}
