package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.modules.platform.dto.CreateCompanyRequest;
import mz.multicore.erp.modules.platform.dto.PlatformCompanyDTO;
import mz.multicore.erp.modules.platform.dto.UpdateCompanyRequest;
import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.modules.platform.dto.CreatePlatformUserRequest;
import mz.multicore.erp.modules.platform.dto.GrantAccessRequest;
import mz.multicore.erp.modules.platform.dto.PlatformUserDTO;
import mz.multicore.erp.modules.subscription.dto.RecordPaymentRequest;
import mz.multicore.erp.modules.subscription.dto.SaveSubscriptionRequest;
import mz.multicore.erp.modules.subscription.dto.SubscriptionDTO;
import mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentDTO;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.SimplePieChart;
import mz.multicore.erp.modules.support.dto.SupportMessageDTO;
import mz.multicore.erp.modules.support.dto.SupportTicketDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consola da plataforma (superadmin). Fase 1: gestão de empresas (listar, activar/desactivar,
 * criar e editar). Fases seguintes acrescentam abas de Pagamentos, Utilizadores e Assistência.
 */
public class PlataformaPanel extends JPanel {

    private final PlatformApiClient platformApiClient;
    private final mz.multicore.erp.desktop.client.SystemMonitoringApiClient monitoringApiClient;

    private DefaultTableModel companiesModel;
    private JTable companiesTable;
    private List<PlatformCompanyDTO> companies = new ArrayList<>();

    private DefaultTableModel subsModel;
    private JTable subsTable;
    private List<SubscriptionDTO> subscriptions = new ArrayList<>();

    private DefaultTableModel usersModel;
    private JTable usersTable;
    private List<PlatformUserDTO> users = new ArrayList<>();

    private DefaultTableModel ticketsModel;
    private JTable ticketsTable;
    private List<SupportTicketDTO> tickets = new ArrayList<>();

    private DefaultTableModel versionsModel;
    private JTable versionsTable;
    private JLabel versionsSummary;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    private JLabel subMrrLabel;
    private JLabel subActiveLabel;
    private JLabel subRiskLabel;
    private JLabel subPaymentsLabel;
    private SimplePieChart subPlansChart;

    public PlataformaPanel(PlatformApiClient platformApiClient) {
        this(platformApiClient, null);
    }

    public PlataformaPanel(PlatformApiClient platformApiClient, mz.multicore.erp.desktop.client.SystemMonitoringApiClient monitoringApiClient) {
        this.platformApiClient = platformApiClient;
        this.monitoringApiClient = monitoringApiClient;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabbedPane);
        tabbedPane.addTab("Empresas", UIHelper.icon("fas-building", 16, UIHelper.ACCENT_BLUE), createCompaniesTab());
        tabbedPane.addTab("Assinaturas & Pagamentos", UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.APPROVED_GREEN),
                createSubscriptionsTab());
        tabbedPane.addTab("Utilizadores", UIHelper.icon("fas-users-cog", 16, UIHelper.ACCENT), createUsersTab());
        tabbedPane.addTab("Assistência", UIHelper.icon("fas-headset", 16, UIHelper.ACCENT_CYAN), createSupportTab());
        tabbedPane.addTab("Versões dos Clientes", UIHelper.icon("fas-code-branch", 16, UIHelper.ACCENT_ORANGE),
                createVersionsTab());
        if (monitoringApiClient != null) {
            tabbedPane.addTab("Saúde & Diagnóstico", UIHelper.icon("fas-heartbeat", 16, UIHelper.ACCENT_CYAN),
                    createMonitoringTab());
        }
        add(feedback, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        // Carregamento preguiçoso: dados por HTTP em onPanelSelected() (via navigate no arranque do
        // superadmin), não no construtor — arranque resiliente se o backend falhar.
    }

    /** Chamado pela MainFrame quando o painel fica activo. */
    public void onPanelSelected() {
        loadCompanies();
        loadSubscriptions();
        loadUsers();
        loadTickets();
        loadClientVersions();
    }

    /**
     * Que versão do programa cada empresa está a usar.
     *
     * <p>É a lista que se consulta <b>antes</b> de subir a versão mínima no servidor: sem ela,
     * decidir bloquear era às cegas. Ver docs/ACTUALIZACOES_CLIENTE_SPEC.md §8/§9.
     */
    private JPanel createVersionsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        ModernButton refresh = UIHelper.createSecondaryButton("Actualizar");
        refresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refresh.addActionListener(e -> loadClientVersions());
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Empresa", "Versão", "Último acesso", "Utilizador", "Visto pela 1.ª vez"};
        versionsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        versionsTable = new JTable(versionsModel);
        UIHelper.styleTable(versionsTable);
        JScrollPane scroll = new JScrollPane(versionsTable);
        UIHelper.styleScrollPane(scroll);

        JTextField search = TableFilter.searchField("Empresa, versão ou utilizador…");
        TableFilter.install(versionsTable, search);
        JPanel bar = TableFilter.bar(search);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(UIHelper.tableCardTop("Versões dos Clientes — quem está em quê", bar,
                refresh), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        versionsSummary = new JLabel(" ");
        versionsSummary.setForeground(UIHelper.TEXT_LIGHT);
        versionsSummary.setBorder(new EmptyBorder(10, 2, 0, 2));
        card.add(versionsSummary, BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private void loadClientVersions() {
        if (versionsModel == null) return;
        UIHelper.loadAsync(this, platformApiClient::listClientVersions, loaded -> {
            java.time.format.DateTimeFormatter fmt =
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            versionsModel.setRowCount(0);
            java.util.Set<String> distinctVersions = new java.util.TreeSet<>();
            for (var usage : loaded) {
                distinctVersions.add(usage.clientVersion());
                versionsModel.addRow(new Object[]{
                        usage.companyName(),
                        usage.clientVersion(),
                        usage.lastSeenAt() == null ? "—" : usage.lastSeenAt().format(fmt),
                        usage.lastUsername() == null ? "—" : usage.lastUsername(),
                        usage.firstSeenAt() == null ? "—" : usage.firstSeenAt().format(fmt)
                });
            }
            // O número de versões distintas é a informação que decide: com uma só, subir a
            // mínima é seguro; com várias, alguém vai ficar de fora.
            versionsSummary.setText(loaded.isEmpty()
                    ? "Ainda não foi registada nenhuma versão. Os clientes identificam-se ao usar o sistema."
                    : String.format("<html><b>%d</b> registo(s) · <b>%d</b> versão(ões) diferente(s) em uso: %s</html>",
                            loaded.size(), distinctVersions.size(), String.join(", ", distinctVersions)));
        }, error -> showPlatformError("versões dos clientes", error));
    }

    private JPanel createCompaniesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        ModernButton newBtn = UIHelper.createSuccessButton("Nova Empresa");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton editBtn = UIHelper.createPrimaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-pen", 14));
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::loadCompanies);
        mz.multicore.erp.gui.components.ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Activar/Desactivar", UIHelper.icon("fas-power-off", 14), this::toggleSelectedCompany);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Nome", "NUIT", "Email", "Nº Utilizadores", "Estado"};
        companiesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        companiesTable = new JTable(companiesModel);
        UIHelper.styleTable(companiesTable);
        companiesTable.putClientProperty("noRowInspector", Boolean.TRUE);
        companiesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) editSelectedCompany();
            }
        });
        JScrollPane scroll = new JScrollPane(companiesTable);
        UIHelper.styleScrollPane(scroll);
        JTextField cSearch = TableFilter.searchField("Nome, NUIT ou email…");
        JComboBox<String> cEstado = TableFilter.combo("Todos os estados", "ACTIVA", "SUSPENSA");
        TableFilter.install(companiesTable, cSearch, new TableFilter.ColumnFilter(cEstado, 4));
        JPanel cBar = TableFilter.bar(cSearch, TableFilter.label("Estado:"), cEstado);
        cBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(UIHelper.tableCardTop("Empresas da Plataforma", cBar,
                refreshBtn, moreBtn, editBtn, newBtn), BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        newBtn.addActionListener(e -> createCompany());
        editBtn.addActionListener(e -> editSelectedCompany());

        return panel;
    }

    private void loadCompanies() {
        UIHelper.loadAsync(this, platformApiClient::listCompanies, loaded -> {
            companies = loaded;
            companiesModel.setRowCount(0);
            for (PlatformCompanyDTO c : companies) {
                companiesModel.addRow(new Object[]{
                        c.name(), c.taxId(), c.email() == null ? "" : c.email(),
                        c.userCount(), c.active() ? "ACTIVA" : "SUSPENSA"
                });
            }
        }, error -> showPlatformError("empresas", error));
    }

    private PlatformCompanyDTO selectedCompany() {
        int row = TableFilter.selectedModelRow(companiesTable);
        if (row < 0 || row >= companies.size()) {
            showPlatformNotice(FeedbackType.WARNING, "Seleccione uma empresa", "Escolha uma empresa na lista para continuar.");
            return null;
        }
        return companies.get(row);
    }

    private void createCompany() {
        JTextField nameField = new JTextField();
        JTextField taxIdField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField addressField = new JTextField();
        JTextField phoneField = new JTextField();
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(taxIdField);
        UIHelper.styleTextField(emailField);
        UIHelper.styleTextField(addressField);
        UIHelper.styleTextField(phoneField);
        final byte[][] logoHolder = {null};

        JPanel form = UIHelper.createDialogForm(
                "Nome:", nameField,
                "NUIT:", taxIdField,
                "Email:", emailField,
                "Endereço:", addressField,
                "Telefone:", phoneField,
                "Logótipo:", logoPicker(logoHolder, false)
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Nova Empresa",
                "fas-building", "Registar uma empresa na plataforma", form)
                .setConfirmButton("Criar", "fas-plus");
        dlg.setOnSaveAsync(() -> {
            if (nameField.getText().trim().isEmpty() || taxIdField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("Nome e NUIT são obrigatórios.");
            }
            CreateCompanyRequest request = new CreateCompanyRequest(
                    nameField.getText().trim(), taxIdField.getText().trim(),
                    emailField.getText().trim(), addressField.getText().trim(), phoneField.getText().trim());
            byte[] logo = logoHolder[0];
            return () -> {
                PlatformCompanyDTO created = platformApiClient.createCompany(request);
                if (logo != null) platformApiClient.updateCompanyLogo(created.id(), logo);
                return created;
            };
        });

        if (dlg.showDialog()) {
            showPlatformSuccess("Empresa criada com sucesso.");
            loadCompanies();
        }
    }

    private void editSelectedCompany() {
        PlatformCompanyDTO company = selectedCompany();
        if (company == null) return;

        JTextField nameField = new JTextField(company.name());
        JTextField emailField = new JTextField(company.email() == null ? "" : company.email());
        JTextField addressField = new JTextField(company.address() == null ? "" : company.address());
        JTextField phoneField = new JTextField(company.phone() == null ? "" : company.phone());
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(emailField);
        UIHelper.styleTextField(addressField);
        UIHelper.styleTextField(phoneField);
        final byte[][] logoHolder = {null};

        JPanel form = UIHelper.createDialogForm(
                "Nome:", nameField,
                "Email:", emailField,
                "Endereço:", addressField,
                "Telefone:", phoneField,
                "Logótipo:", logoPicker(logoHolder, company.hasLogo())
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Editar Empresa",
                "fas-pen", "NUIT: " + company.taxId(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            if (nameField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("O nome é obrigatório.");
            }
            UpdateCompanyRequest request = new UpdateCompanyRequest(
                    nameField.getText().trim(), emailField.getText().trim(),
                    addressField.getText().trim(), phoneField.getText().trim());
            byte[] logo = logoHolder[0];
            return () -> {
                PlatformCompanyDTO updated = platformApiClient.updateCompany(company.id(), request);
                if (logo != null) platformApiClient.updateCompanyLogo(company.id(), logo);
                return updated;
            };
        });

        if (dlg.showDialog()) {
            showPlatformSuccess("Empresa actualizada.");
            loadCompanies();
        }
    }

    /** Selector de logótipo: botão + pré-visualização. Só define bytes quando o utilizador escolhe um. */
    private javax.swing.JComponent logoPicker(byte[][] holder, boolean hasExisting) {
        javax.swing.JLabel preview = new javax.swing.JLabel(hasExisting ? "(logótipo actual definido)" : "(sem logótipo)");
        javax.swing.JButton pick = new javax.swing.JButton("Escolher Imagem", UIHelper.icon("fas-image", 16));
        pick.addActionListener(e -> {
            javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagens (PNG, JPG)", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
                byte[] bytes = UIHelper.readScaledImage(fc.getSelectedFile(), 320);
                if (bytes == null) {
                    showPlatformNotice(FeedbackType.WARNING, "Imagem inválida", "Seleccione uma imagem válida para o logótipo.");
                    return;
                }
                holder[0] = bytes;
                preview.setText(null);
                preview.setIcon(UIHelper.imageIconFromBytes(bytes, 64, 40));
            }
        });
        JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        p.setOpaque(false);
        p.add(pick);
        p.add(preview);
        return p;
    }

    private void toggleSelectedCompany() {
        PlatformCompanyDTO company = selectedCompany();
        if (company == null) return;

        boolean newState = !company.active();
        String verb = newState ? "activar" : "suspender";
        if (!ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(this),
                newState ? FeedbackType.INFO : FeedbackType.WARNING,
                newState ? "Activar empresa" : "Suspender empresa",
                "Deseja " + verb + " a empresa '" + company.name() + "'?"
                        + (newState ? "" : "\nOs utilizadores desta empresa deixam de poder iniciar sessão."),
                newState ? "Activar" : "Suspender")) return;

        UIHelper.runWithProgress(this, "A actualizar estado da empresa…",
                () -> platformApiClient.setCompanyActive(company.id(), newState), ignored -> loadCompanies(),
                error -> showPlatformError("estado da empresa", error));
    }

    // ---------------------------------------------------------------- Assinaturas & Pagamentos

    private JPanel createSubscriptionsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        ModernButton planBtn = UIHelper.createPrimaryButton("Definir Plano/Validade");
        planBtn.setIcon(UIHelper.icon("fas-sliders-h", 14));
        ModernButton payBtn = UIHelper.createSuccessButton("Registar Pagamento");
        payBtn.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::loadSubscriptions);
        mz.multicore.erp.gui.components.ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Ver Pagamentos", UIHelper.icon("fas-receipt", 14), this::showPayments)
                .addAction("Suspender/Reactivar", UIHelper.icon("fas-power-off", 14), this::toggleSubscriptionStatus);

        // Barra Executiva de KPIs de Subscrições
        JPanel kpiGrid = KpiCard.createGrid(4);
        subMrrLabel = new JLabel("0,00 MT");
        subActiveLabel = new JLabel("0");
        subRiskLabel = new JLabel("0");
        subPaymentsLabel = new JLabel("0");

        kpiGrid.add(KpiCard.createMetricCard("Receita Recorrente (MRR)", subMrrLabel, "Faturação mensal das assinaturas", "fas-money-bill-wave", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createMetricCard("Assinaturas Activas", subActiveLabel, "Empresas regularizadas", "fas-check-circle", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createMetricCard("Em Risco / Expiradas", subRiskLabel, "≤ 7 dias ou vencidas", "fas-exclamation-triangle", UIHelper.REJECTED_RED));
        kpiGrid.add(KpiCard.createMetricCard("Pagamentos Registados", subPaymentsLabel, "Histórico liquidado", "fas-receipt", UIHelper.PENDING_YELLOW));

        panel.add(kpiGrid, BorderLayout.NORTH);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Empresa", "Plano", "Estado", "Válida até", "Preço/mês", "Nº Pagamentos"};
        subsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        subsTable = new JTable(subsModel);
        UIHelper.styleTable(subsTable);
        // Destaque: linhas a vermelho (expirada/suspensa) ou amarelo (expira em ≤7 dias). Delega no
        // renderer do tema (fundo/selecção) e só troca a cor do texto para as linhas em risco.
        javax.swing.table.TableCellRenderer baseRenderer = subsTable.getDefaultRenderer(Object.class);
        subsTable.setDefaultRenderer(Object.class, (table, value, sel, focus, row, col) -> {
            Component c = baseRenderer.getTableCellRendererComponent(table, value, sel, focus, row, col);
            int modelRow = row >= 0 ? subsTable.convertRowIndexToModel(row) : -1;
            if (!sel && modelRow >= 0 && modelRow < subscriptions.size()) {
                int sev = subSeverity(subscriptions.get(modelRow));
                if (sev == -1) c.setForeground(UIHelper.REJECTED_RED);
                else if (sev == 0) c.setForeground(UIHelper.PENDING_YELLOW);
            }
            return c;
        });
        JScrollPane scroll = new JScrollPane(subsTable);
        UIHelper.styleScrollPane(scroll);
        JTextField sSearch = TableFilter.searchField("Empresa…");
        JComboBox<String> sEstado = TableFilter.combo("Todos os estados", "Activa", "Suspensa", "Expirada", "Avaliação", "Sem assinatura");
        JComboBox<String> sPlano = TableFilter.combo("Todos os planos", "Avaliação", "Básico", "Profissional", "Empresarial");
        TableFilter.install(subsTable, sSearch,
                new TableFilter.ColumnFilter(sEstado, 2), new TableFilter.ColumnFilter(sPlano, 1));
        JPanel sBar = TableFilter.bar(sSearch, TableFilter.label("Estado:"), sEstado, TableFilter.label("Plano:"), sPlano);
        sBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(UIHelper.tableCardTop("Assinaturas & Pagamentos", sBar,
                refreshBtn, moreBtn, payBtn, planBtn), BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);

        // Gráfico Donut de Planos
        ModernPanel chartCard = new ModernPanel(16);
        chartCard.setLayout(new BorderLayout());
        chartCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        chartCard.setPreferredSize(new Dimension(320, 0));
        subPlansChart = new SimplePieChart("Distribuição por Plano", true);
        chartCard.add(subPlansChart, BorderLayout.CENTER);

        JPanel centerSplit = new JPanel(new BorderLayout(12, 0));
        centerSplit.setOpaque(false);
        centerSplit.add(listCard, BorderLayout.CENTER);
        centerSplit.add(chartCard, BorderLayout.EAST);

        panel.add(centerSplit, BorderLayout.CENTER);

        planBtn.addActionListener(e -> defineSubscription());
        payBtn.addActionListener(e -> recordPayment());

        return panel;
    }

    private void loadSubscriptions() {
        UIHelper.loadAsync(this, platformApiClient::listOverview, loaded -> {
            subscriptions = loaded;
            subsModel.setRowCount(0);
            BigDecimal totalMrr = BigDecimal.ZERO;
            int activeCount = 0;
            int riskCount = 0;
            long totalPayments = 0;

            Map<String, BigDecimal> planCountMap = new LinkedHashMap<>();
            planCountMap.put("TRIAL", BigDecimal.ZERO);
            planCountMap.put("BASIC", BigDecimal.ZERO);
            planCountMap.put("PRO", BigDecimal.ZERO);
            planCountMap.put("ENTERPRISE", BigDecimal.ZERO);
            planCountMap.put("Sem Plano", BigDecimal.ZERO);

            for (SubscriptionDTO s : subscriptions) {
                subsModel.addRow(new Object[]{
                        s.companyName(),
                        s.planLabel(),
                        s.statusLabel(),
                        s.validUntil() == null ? "—" : s.validUntil().toString(),
                        s.monthlyPrice() == null ? "—" : s.monthlyPrice().toPlainString(),
                        s.paymentCount()
                });

                if (s.hasSubscription()) {
                    if ("ACTIVE".equals(s.status())) {
                        activeCount++;
                        if (s.monthlyPrice() != null) {
                            totalMrr = totalMrr.add(s.monthlyPrice());
                        }
                    }
                    int sev = subSeverity(s);
                    if (sev <= 0) {
                        riskCount++;
                    }
                    totalPayments += s.paymentCount();

                    String pKey = s.plan() != null ? s.plan() : "Sem Plano";
                    planCountMap.put(pKey, planCountMap.getOrDefault(pKey, BigDecimal.ZERO).add(BigDecimal.ONE));
                } else {
                    planCountMap.put("Sem Plano", planCountMap.getOrDefault("Sem Plano", BigDecimal.ZERO).add(BigDecimal.ONE));
                }
            }

            if (subMrrLabel != null) {
                subMrrLabel.setText(String.format("%,.2f MT", totalMrr));
            }
            if (subActiveLabel != null) {
                subActiveLabel.setText(activeCount + " / " + subscriptions.size());
            }
            if (subRiskLabel != null) {
                subRiskLabel.setText(String.valueOf(riskCount));
                subRiskLabel.setForeground(riskCount > 0 ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);
            }
            if (subPaymentsLabel != null) {
                subPaymentsLabel.setText(String.valueOf(totalPayments));
            }

            if (subPlansChart != null) {
                List<String> pLabels = new ArrayList<>();
                List<BigDecimal> pValues = new ArrayList<>();
                for (Map.Entry<String, BigDecimal> entry : planCountMap.entrySet()) {
                    if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                        pLabels.add(entry.getKey());
                        pValues.add(entry.getValue());
                    }
                }
                subPlansChart.setData(pLabels.toArray(new String[0]), pValues.toArray(new BigDecimal[0]), null);
            }
        }, error -> showPlatformError("assinaturas", error));
    }

    /** -1 = expirada/suspensa; 0 = a expirar em ≤7 dias; 1 = ok/sem assinatura. */
    private static int subSeverity(SubscriptionDTO s) {
        if (!s.hasSubscription()) return 1;
        if ("EXPIRED".equals(s.status()) || "SUSPENDED".equals(s.status())) return -1;
        if (s.validUntil() != null) {
            long d = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), s.validUntil());
            if (d >= 0 && d <= 7) return 0;
        }
        return 1;
    }

    private SubscriptionDTO selectedSubscription() {
        int row = TableFilter.selectedModelRow(subsTable);
        if (row < 0 || row >= subscriptions.size()) {
            showPlatformNotice(FeedbackType.WARNING, "Seleccione uma empresa", "Escolha uma empresa na lista para continuar.");
            return null;
        }
        return subscriptions.get(row);
    }

    private void defineSubscription() {
        SubscriptionDTO sub = selectedSubscription();
        if (sub == null) return;

        JComboBox<String> planCombo = new JComboBox<>(new String[]{"TRIAL", "BASIC", "PRO", "ENTERPRISE"});
        UIHelper.styleComboBox(planCombo);
        if (sub.plan() != null) planCombo.setSelectedItem(sub.plan());
        JTextField priceField = new JTextField(sub.monthlyPrice() == null ? "" : sub.monthlyPrice().toPlainString());
        DateField validField = new DateField(sub.validUntil());
        UIHelper.styleTextField(priceField);

        JPanel form = UIHelper.createDialogForm(
                "Plano:", planCombo,
                "Preço mensal (MT):", priceField,
                "Válida até (AAAA-MM-DD):", validField
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Definir Assinatura",
                "fas-sliders-h", sub.companyName(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            SaveSubscriptionRequest request = new SaveSubscriptionRequest((String) planCombo.getSelectedItem(),
                    parseAmount(priceField.getText(), "preço mensal"), parseDate(validField.getText()));
            return () -> platformApiClient.saveSubscription(sub.companyId(), request);
        });

        if (dlg.showDialog()) {
            showPlatformSuccess("Assinatura actualizada.");
            loadSubscriptions();
        }
    }

    private void recordPayment() {
        SubscriptionDTO sub = selectedSubscription();
        if (sub == null) return;

        JTextField amountField = new JTextField();
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"TRANSFERENCIA", "DINHEIRO", "MPESA", "EMOLA", "OUTRO"});
        UIHelper.styleComboBox(methodCombo);
        methodCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if ("TRANSFERENCIA".equals(value)) setText("Transferência Bancária");
                else if ("DINHEIRO".equals(value)) setText("Numerário / Dinheiro");
                else if ("MPESA".equals(value)) setText("M-Pesa");
                else if ("EMOLA".equals(value)) setText("e-Mola");
                else if ("OUTRO".equals(value)) setText("Outro Meio");
                return this;
            }
        });

        JTextField referenceField = new JTextField();
        referenceField.setToolTipText("Ex: Nº talão de depósito, ID de transação M-Pesa/e-Mola, Nº de recibo...");
        UIHelper.styleTextField(referenceField);

        // Campos contextuais para Transferência
        JComboBox<String> bankCombo = new JComboBox<>(new String[]{
                "Millennium BIM", "BCI", "Standard Bank", "Moza Banco", "Absa Bank", "Nedbank", "FNB", "Outro Banco"
        });
        UIHelper.styleComboBox(bankCombo);
        JTextField transferHolderField = new JTextField();
        transferHolderField.setToolTipText("Titular da conta ou Nº de conta bancária de origem");
        UIHelper.styleTextField(transferHolderField);
        JPanel transferPanel = UIHelper.createDialogForm(
                "Banco:", bankCombo,
                "Titular / Conta de Origem:", transferHolderField
        );

        // Campos contextuais para Dinheiro / Numerário
        JComboBox<String> cashLocationCombo = new JComboBox<>(new String[]{
                "Sede / Escritório Principal", "Balcão Comercial", "Cobrança Externa / Caixa", "Outro Local"
        });
        UIHelper.styleComboBox(cashLocationCombo);
        JTextField manualReceiptField = new JTextField();
        manualReceiptField.setToolTipText("Nº do recibo manual ou talão físico entregue ao cliente");
        UIHelper.styleTextField(manualReceiptField);
        JPanel cashPanel = UIHelper.createDialogForm(
                "Local / Caixa de Recebimento:", cashLocationCombo,
                "Nº Recibo Físico / Manual:", manualReceiptField
        );

        // Campos contextuais para Carteiras Móveis (M-Pesa / e-Mola)
        JTextField mobileNumberField = new JTextField();
        mobileNumberField.setToolTipText("Ex: 841234567 ou 851234567");
        UIHelper.styleTextField(mobileNumberField);
        JTextField mobileHolderField = new JTextField();
        mobileHolderField.setToolTipText("Nome do titular da conta móvel");
        UIHelper.styleTextField(mobileHolderField);
        JPanel mobilePanel = UIHelper.createDialogForm(
                "Nº de Telemóvel:", mobileNumberField,
                "Titular da Conta Móvel:", mobileHolderField
        );

        // Campos contextuais para Outro Meio
        JTextField otherDetailField = new JTextField();
        otherDetailField.setToolTipText("Descreva os detalhes e comprovativo do meio de pagamento");
        UIHelper.styleTextField(otherDetailField);
        JPanel otherPanel = UIHelper.createDialogForm(
                "Descrição do Meio:", otherDetailField
        );

        // Painel CardLayout para trocar dinamicamente
        CardLayout cardLayout = new CardLayout();
        JPanel detailsCardPanel = new JPanel(cardLayout);
        detailsCardPanel.setOpaque(false);
        detailsCardPanel.add(transferPanel, "TRANSFERENCIA");
        detailsCardPanel.add(cashPanel, "DINHEIRO");
        detailsCardPanel.add(mobilePanel, "MPESA");
        detailsCardPanel.add(mobilePanel, "EMOLA");
        detailsCardPanel.add(otherPanel, "OUTRO");

        methodCombo.addActionListener(e -> {
            String sel = (String) methodCombo.getSelectedItem();
            cardLayout.show(detailsCardPanel, sel != null ? sel : "TRANSFERENCIA");
            detailsCardPanel.revalidate();
            detailsCardPanel.repaint();
        });

        DateField paidAtField = new DateField(LocalDate.now());
        DateField startField = new DateField();
        DateField endField = new DateField();
        JTextField noteField = new JTextField();
        UIHelper.styleTextField(amountField);
        UIHelper.styleTextField(noteField);

        JPanel mainFields = UIHelper.createDialogForm(
                "Valor (MT) *:", amountField,
                "Método de Pagamento *:", methodCombo,
                "Referência (Opcional):", referenceField,
                "Pago em (AAAA-MM-DD) *:", paidAtField,
                "Período de (AAAA-MM-DD):", startField,
                "Período até (AAAA-MM-DD):", endField,
                "Observações:", noteField
        );

        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.setOpaque(false);
        form.add(mainFields, BorderLayout.NORTH);
        form.add(detailsCardPanel, BorderLayout.CENTER);

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Registar Pagamento",
                "fas-money-bill-wave", sub.companyName() + " — o período até estende a validade", form)
                .setConfirmButton("Registar", "fas-check");
        dlg.setSize(580, 520);
        dlg.setOnSaveAsync(() -> {
            String method = (String) methodCombo.getSelectedItem();
            String details = switch (method != null ? method : "OUTRO") {
                case "TRANSFERENCIA" -> {
                    String bank = (String) bankCombo.getSelectedItem();
                    String holder = transferHolderField.getText().trim();
                    yield "Banco: " + bank + (holder.isEmpty() ? "" : " | Titular/Conta: " + holder);
                }
                case "DINHEIRO" -> {
                    String loc = (String) cashLocationCombo.getSelectedItem();
                    String rec = manualReceiptField.getText().trim();
                    yield "Local: " + loc + (rec.isEmpty() ? "" : " | Recibo: " + rec);
                }
                case "MPESA", "EMOLA" -> {
                    String phone = mobileNumberField.getText().trim();
                    String holder = mobileHolderField.getText().trim();
                    yield (phone.isEmpty() ? "" : "Telemóvel: " + phone)
                            + (holder.isEmpty() ? "" : (phone.isEmpty() ? "" : " | ") + "Titular: " + holder);
                }
                default -> otherDetailField.getText().trim();
            };

            String ref = referenceField.getText().trim();
            RecordPaymentRequest request = new RecordPaymentRequest(
                    parseAmount(amountField.getText(), "valor"),
                    method,
                    parseDate(paidAtField.getText()),
                    parseDate(startField.getText()),
                    parseDate(endField.getText()),
                    ref.isEmpty() ? null : ref,
                    details.isEmpty() ? null : details,
                    noteField.getText().trim()
            );
            return () -> platformApiClient.recordPayment(sub.companyId(), request);
        });

        if (dlg.showDialog()) {
            showPlatformSuccess("Pagamento registado.");
            loadSubscriptions();
        }
    }

    private void showPayments() {
        SubscriptionDTO sub = selectedSubscription();
        if (sub == null) return;

        UIHelper.loadAsync(this, () -> platformApiClient.listPayments(sub.companyId()),
                payments -> showPaymentsDialog(sub, payments), error -> showPlatformError("pagamentos", error));
    }

    private void showPaymentsDialog(SubscriptionDTO sub, List<SubscriptionPaymentDTO> payments) {
        String[] cols = {"Pago em", "Valor", "Método", "Referência", "Detalhes", "Período", "Nota"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (SubscriptionPaymentDTO p : payments) {
            String period = (p.periodStart() == null ? "—" : p.periodStart().toString())
                    + " a " + (p.periodEnd() == null ? "—" : p.periodEnd().toString());
            String ref = p.reference() == null || p.reference().isBlank() ? "—" : p.reference();
            String details = p.paymentDetails() == null || p.paymentDetails().isBlank() ? "—" : p.paymentDetails();
            model.addRow(new Object[]{
                    p.paidAt() == null ? "" : p.paidAt().toString(),
                    p.amount(),
                    p.methodLabel(),
                    ref,
                    details,
                    period,
                    p.note() == null ? "" : p.note()
            });
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(1).setCellRenderer(mz.multicore.erp.gui.components.TableCellRenderers.money());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setMinimumSize(new Dimension(720, 280));

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Histórico de Pagamentos",
                "fas-receipt", sub.companyName(), scroll).asReadOnly("Fechar");
        dlg.setSize(820, 460);
        dlg.showDialog();
    }

    private void toggleSubscriptionStatus() {
        SubscriptionDTO sub = selectedSubscription();
        if (sub == null) return;
        if (!sub.hasSubscription()) {
            showPlatformNotice(FeedbackType.WARNING, "Assinatura necessária", "Defina primeiro a assinatura desta empresa.");
            return;
        }

        boolean suspended = "SUSPENDED".equals(sub.status());
        String target = suspended ? "ACTIVE" : "SUSPENDED";
        String verb = suspended ? "reactivar" : "suspender";
        if (!ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(this),
                suspended ? FeedbackType.INFO : FeedbackType.WARNING,
                suspended ? "Reactivar assinatura" : "Suspender assinatura",
                "Deseja " + verb + " a assinatura de '" + sub.companyName() + "'?"
                        + (suspended ? "" : "\nOs utilizadores desta empresa deixam de poder iniciar sessão."),
                suspended ? "Reactivar" : "Suspender")) return;

        UIHelper.runWithProgress(this, "A actualizar assinatura…",
                () -> platformApiClient.changeSubscriptionStatus(sub.companyId(), target),
                ignored -> loadSubscriptions(), error -> showPlatformError("assinatura", error));
    }

    private BigDecimal parseAmount(String text, String field) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor inválido para " + field + ".");
        }
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(text.trim());
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("Data inválida: use o formato AAAA-MM-DD.");
        }
    }

    // ------------------------------------------------------------------------ Utilizadores globais

    private static final String[] TENANT_ROLES = {"EMPLOYEE", "SELLER", "MANAGER", "ADMIN"};

    private JPanel createUsersTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        ModernButton newBtn = UIHelper.createSuccessButton("Novo Utilizador");
        newBtn.setIcon(UIHelper.icon("fas-user-plus", 14));
        ModernButton editBtn = UIHelper.createPrimaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-pen", 14));
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::loadUsers);
        mz.multicore.erp.gui.components.ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Revogar Acesso", UIHelper.icon("fas-user-slash", 14), this::revokeAccess)
                .addAction("Repor Senha", UIHelper.icon("fas-key", 14), this::resetPassword)
                .addAction("Activar/Desactivar", UIHelper.icon("fas-power-off", 14), this::toggleUserActive)
                .addAction("Conceder/Alterar Acesso", UIHelper.icon("fas-user-shield", 14, UIHelper.ACCENT_BLUE), this::grantAccess);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Utilizador", "Nome", "Empresas & Papéis", "Estado"};
        usersModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        usersTable = new JTable(usersModel);
        UIHelper.styleTable(usersTable);
        usersTable.putClientProperty("noRowInspector", Boolean.TRUE);
        usersTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) editUser();
            }
        });
        JScrollPane scroll = new JScrollPane(usersTable);
        UIHelper.styleScrollPane(scroll);
        JTextField uSearch = TableFilter.searchField("Utilizador, nome ou empresa…");
        JComboBox<String> uEstado = TableFilter.combo("Todos os estados", "ACTIVO", "INATIVO", "SUPERADMIN");
        TableFilter.install(usersTable, uSearch, new TableFilter.ColumnFilter(uEstado, 3));
        JPanel uBar = TableFilter.bar(uSearch, TableFilter.label("Estado:"), uEstado);
        uBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(UIHelper.tableCardTop("Utilizadores de Todas as Empresas", uBar,
                refreshBtn, moreBtn, editBtn, newBtn), BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        newBtn.addActionListener(e -> createPlatformUser());
        editBtn.addActionListener(e -> editUser());
        return panel;
    }

    private void loadUsers() {
        UIHelper.loadAsync(this, platformApiClient::listUsers, loaded -> {
            users = loaded;
            usersModel.setRowCount(0);
            for (PlatformUserDTO u : users) {
                usersModel.addRow(new Object[]{
                        u.username(), u.name(), describeAccesses(u),
                        u.platformAdmin() ? "SUPERADMIN" : (u.active() ? "ACTIVO" : "INATIVO")
                });
            }
        }, error -> showPlatformError("utilizadores", error));
    }

    private String describeAccesses(PlatformUserDTO u) {
        if (u.companies().isEmpty()) return u.platformAdmin() ? "(plataforma)" : "—";
        StringBuilder sb = new StringBuilder();
        for (PlatformUserDTO.CompanyRoleDTO c : u.companies()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(c.companyName()).append(": ").append(UIHelper.humanRole(c.role()));
        }
        return sb.toString();
    }

    private PlatformUserDTO selectedUser() {
        int row = TableFilter.selectedModelRow(usersTable);
        if (row < 0 || row >= users.size()) {
            showPlatformNotice(FeedbackType.WARNING, "Seleccione um utilizador", "Escolha um utilizador na lista para continuar.");
            return null;
        }
        return users.get(row);
    }

    private void editUser() {
        PlatformUserDTO user = selectedUser();
        if (user == null) return;

        JTextField nameField = new JTextField(user.name());
        UIHelper.styleTextField(nameField);
        JPanel form = UIHelper.createDialogForm("Nome completo:", nameField);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Editar Utilizador",
                "fas-pen", "Utilizador: " + user.username(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            if (nameField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("O nome é obrigatório.");
            }
            String name = nameField.getText().trim();
            return () -> platformApiClient.updateUser(user.username(), name);
        });
        if (dlg.showDialog()) {
            showPlatformSuccess("Utilizador actualizado.");
            loadUsers();
        }
    }

    private void createPlatformUser() {
        if (companies.isEmpty()) {
            showPlatformNotice(FeedbackType.WARNING, "Empresa necessária", "Crie primeiro uma empresa.");
            return;
        }
        JTextField usernameField = new JTextField();
        JTextField nameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JComboBox<CompanyItem> companyCombo = companyCombo();
        JComboBox<String> roleCombo = new JComboBox<>(TENANT_ROLES);
        UIHelper.styleTextField(usernameField);
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(passwordField);
        UIHelper.styleComboBox(companyCombo);
        UIHelper.styleComboBox(roleCombo);
        UIHelper.humanizeRoleCombo(roleCombo);

        JPanel form = UIHelper.createDialogForm(
                "Utilizador:", usernameField,
                "Nome completo:", nameField,
                "Senha:", passwordField,
                "Empresa:", companyCombo,
                "Papel:", roleCombo
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Novo Utilizador",
                "fas-user-plus", "Conta ligada a uma empresa", form).setConfirmButton("Criar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            if (usernameField.getText().trim().isEmpty() || nameField.getText().trim().isEmpty()
                    || passwordField.getPassword().length == 0) {
                throw new IllegalArgumentException("Utilizador, nome e senha são obrigatórios.");
            }
            CreatePlatformUserRequest request = new CreatePlatformUserRequest(
                    usernameField.getText().trim(), nameField.getText().trim(),
                    new String(passwordField.getPassword()),
                    ((CompanyItem) companyCombo.getSelectedItem()).id(),
                    (String) roleCombo.getSelectedItem());
            return () -> platformApiClient.createUser(request);
        });

        if (dlg.showDialog()) {
            showPlatformSuccess("Utilizador criado.");
            loadUsers();
        }
    }

    private void grantAccess() {
        PlatformUserDTO user = selectedUser();
        if (user == null) return;
        if (companies.isEmpty()) {
            showPlatformNotice(FeedbackType.WARNING, "Sem empresas", "Crie uma empresa antes de conceder acesso.");
            return;
        }
        JComboBox<CompanyItem> companyCombo = companyCombo();
        JComboBox<String> roleCombo = new JComboBox<>(TENANT_ROLES);
        UIHelper.styleComboBox(companyCombo);
        UIHelper.styleComboBox(roleCombo);
        UIHelper.humanizeRoleCombo(roleCombo);

        JPanel form = UIHelper.createDialogForm("Empresa:", companyCombo, "Papel:", roleCombo);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Conceder/Alterar Acesso",
                "fas-user-shield", "Utilizador: " + user.username(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            GrantAccessRequest request = new GrantAccessRequest(((CompanyItem) companyCombo.getSelectedItem()).id(),
                    (String) roleCombo.getSelectedItem());
            return () -> platformApiClient.grantAccess(user.username(), request);
        });

        if (dlg.showDialog()) {
            showPlatformSuccess("Acesso actualizado.");
            loadUsers();
        }
    }

    private void revokeAccess() {
        PlatformUserDTO user = selectedUser();
        if (user == null) return;
        if (user.companies().isEmpty()) {
            showPlatformNotice(FeedbackType.INFO, "Sem acessos", "O utilizador não tem acessos a revogar.");
            return;
        }
        CompanyItem[] items = user.companies().stream()
                .map(c -> new CompanyItem(c.companyId(), c.companyName()))
                .toArray(CompanyItem[]::new);
        JComboBox<CompanyItem> combo = new JComboBox<>(items);
        UIHelper.styleComboBox(combo);

        JPanel form = UIHelper.createDialogForm("Empresa:", combo);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Revogar Acesso",
                "fas-user-slash", "Utilizador: " + user.username(), form).setConfirmButton("Revogar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            Long companyId = ((CompanyItem) combo.getSelectedItem()).id();
            return () -> {
                platformApiClient.revokeAccess(user.username(), companyId);
                return null;
            };
        });

        if (dlg.showDialog()) {
            showPlatformSuccess("Acesso revogado.");
            loadUsers();
        }
    }

    private void resetPassword() {
        PlatformUserDTO user = selectedUser();
        if (user == null) return;
        JPasswordField passwordField = new JPasswordField();
        UIHelper.styleTextField(passwordField);
        JPanel form = UIHelper.createDialogForm("Nova senha:", passwordField);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Repor Senha",
                "fas-key", "Utilizador: " + user.username(), form).setConfirmButton("Repor", "fas-check");
        dlg.setOnSaveAsync(() -> {
            if (passwordField.getPassword().length == 0) {
                throw new IllegalArgumentException("Indique a nova senha.");
            }
            String password = new String(passwordField.getPassword());
            return () -> {
                platformApiClient.resetPassword(user.username(), password);
                return null;
            };
        });
        if (dlg.showDialog()) {
            showPlatformSuccess("Senha reposta.");
        }
    }

    private void toggleUserActive() {
        PlatformUserDTO user = selectedUser();
        if (user == null) return;
        boolean newState = !user.active();
        UIHelper.runWithProgress(this, "A actualizar utilizador…",
                () -> platformApiClient.setUserActive(user.username(), newState), ignored -> loadUsers(),
                error -> showPlatformError("utilizador", error));
    }

    private JComboBox<CompanyItem> companyCombo() {
        CompanyItem[] items = companies.stream()
                .map(c -> new CompanyItem(c.id(), c.name()))
                .toArray(CompanyItem[]::new);
        return new JComboBox<>(items);
    }

    /** Item de combo: mostra o nome mas transporta o id da empresa. */
    private record CompanyItem(Long id, String name) {
        @Override
        public String toString() { return name; }
    }

    // ---------------------------------------------------------------------------- Assistência

    private JPanel createSupportTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        ModernButton openBtn = UIHelper.createPrimaryButton("Abrir / Responder");
        openBtn.setIcon(UIHelper.icon("fas-comments", 14));
        ModernButton statusBtn = UIHelper.createSecondaryButton("Mudar Estado");
        statusBtn.setIcon(UIHelper.icon("fas-tasks", 14));
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"#", "Empresa", "Assunto", "Prioridade", "Estado", "Responsável", "Mensagens"};
        ticketsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        ticketsTable = new JTable(ticketsModel);
        UIHelper.styleTable(ticketsTable);
        ticketsTable.putClientProperty("noRowInspector", Boolean.TRUE);
        ticketsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) openTicketConversation();
            }
        });
        JScrollPane scroll = new JScrollPane(ticketsTable);
        UIHelper.styleScrollPane(scroll);
        JTextField tSearch = TableFilter.searchField("Empresa ou assunto…");
        JComboBox<String> tEstado = TableFilter.combo("Todos os estados", "Aberto", "Em curso", "Resolvido", "Fechado");
        JComboBox<String> tPrio = TableFilter.combo("Todas as prioridades", "Baixa", "Normal", "Alta", "Urgente");
        TableFilter.install(ticketsTable, tSearch,
                new TableFilter.ColumnFilter(tEstado, 4), new TableFilter.ColumnFilter(tPrio, 3));
        JPanel tBar = TableFilter.bar(tSearch, TableFilter.label("Estado:"), tEstado, TableFilter.label("Prioridade:"), tPrio);
        tBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        listCard.add(UIHelper.tableCardTop("Pedidos de Assistência", tBar,
                refreshBtn, statusBtn, openBtn), BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        openBtn.addActionListener(e -> openTicketConversation());
        statusBtn.addActionListener(e -> changeTicketStatus());
        refreshBtn.addActionListener(e -> loadTickets());

        return panel;
    }

    private void loadTickets() {
        UIHelper.loadAsync(this, platformApiClient::listAllTickets, loaded -> {
            tickets = loaded;
            ticketsModel.setRowCount(0);
            for (SupportTicketDTO t : tickets) {
                ticketsModel.addRow(new Object[]{
                        t.id(), t.companyName(), t.subject(), t.priorityLabel(), t.statusLabel(),
                        t.assignee() == null ? "—" : t.assignee(), t.messageCount()
                });
            }
        }, error -> showPlatformError("pedidos de assistência", error));
    }

    private SupportTicketDTO selectedTicket() {
        int row = TableFilter.selectedModelRow(ticketsTable);
        if (row < 0 || row >= tickets.size()) {
            showPlatformNotice(FeedbackType.WARNING, "Seleccione um pedido", "Escolha um pedido de assistência na lista.");
            return null;
        }
        return tickets.get(row);
    }

    private void openTicketConversation() {
        SupportTicketDTO ticket = selectedTicket();
        if (ticket == null) return;
        UIHelper.loadAsync(this, () -> platformApiClient.listPlatformMessages(ticket.id()),
                messages -> showTicketConversation(ticket, renderThread(messages)),
                error -> showPlatformError("conversa de assistência", error));
    }

    private void showTicketConversation(SupportTicketDTO ticket, String renderedThread) {
        JTextArea thread = new JTextArea(renderedThread);
        thread.setEditable(false);
        thread.setLineWrap(true);
        thread.setWrapStyleWord(true);
        JScrollPane threadScroll = new JScrollPane(thread);
        threadScroll.setMinimumSize(new Dimension(460, 200));

        JTextArea reply = new JTextArea(3, 40);
        reply.setLineWrap(true);
        reply.setWrapStyleWord(true);
        JPanel form = new JPanel(new BorderLayout(0, 8));
        form.setOpaque(false);
        form.add(threadScroll, BorderLayout.CENTER);
        JPanel replyBox = new JPanel(new BorderLayout(0, 4));
        replyBox.setOpaque(false);
        JLabel lbl = new JLabel("Resposta (deixe vazio para só consultar):");
        lbl.setForeground(UIHelper.TEXT_MUTED);
        replyBox.add(lbl, BorderLayout.NORTH);
        replyBox.add(new JScrollPane(reply), BorderLayout.CENTER);
        form.add(replyBox, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Pedido #" + ticket.id() + " — " + ticket.subject(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION && !reply.getText().trim().isEmpty()) {
            String message = reply.getText().trim();
            UIHelper.runWithProgress(this, "A enviar resposta…",
                    () -> platformApiClient.addSuperAdminReply(ticket.id(), message), ignored -> loadTickets(),
                    error -> showPlatformError("resposta de assistência", error));
        }
    }

    private void changeTicketStatus() {
        SupportTicketDTO ticket = selectedTicket();
        if (ticket == null) return;
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"});
        UIHelper.styleComboBox(statusCombo);
        statusCombo.setSelectedItem(ticket.status());
        JPanel form = UIHelper.createDialogForm("Estado:", statusCombo);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Mudar Estado",
                "fas-tasks", "Pedido #" + ticket.id(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSaveAsync(() -> {
            String status = (String) statusCombo.getSelectedItem();
            return () -> platformApiClient.changeTicketStatus(ticket.id(), status);
        });
        if (dlg.showDialog()) {
            loadTickets();
        }
    }

    static String renderThread(List<SupportMessageDTO> messages) {
        if (messages.isEmpty()) return "(sem mensagens)";
        StringBuilder sb = new StringBuilder();
        for (SupportMessageDTO m : messages) {
            String who = m.fromSuperAdmin() ? "Suporte" : "Empresa";
            sb.append("[").append(who).append(" · ").append(m.author()).append("]\n");
            sb.append(m.body()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private void showPlatformError(String area, Throwable error) {
        feedback.show(FeedbackType.ERROR, "Não foi possível processar " + area,
                error.getMessage(), "Tentar novamente", this::onPanelSelected);
    }

    private void showPlatformNotice(FeedbackType type, String title, String message) {
        feedback.show(type, title, message, null, null);
    }

    private void showPlatformSuccess(String message) {
        ToastManager.success(this, message);
    }

    private JPanel createMonitoringTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(UIHelper.BG_DARK);

        mz.multicore.erp.gui.components.ModernPanel card = new mz.multicore.erp.gui.components.ModernPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(36, 48, 36, 48));

        JLabel icon = new JLabel(UIHelper.icon("fas-heartbeat", 48, UIHelper.ACCENT_CYAN));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(icon);
        card.add(javax.swing.Box.createVerticalStrut(16));

        JLabel title = new JLabel("Consola de Observabilidade & Diagnóstico");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UIHelper.TEXT_LIGHT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(javax.swing.Box.createVerticalStrut(8));

        JLabel desc = new JLabel("<html><center>Monitore em tempo real a integridade da persistência, pool HikariCP,<br>memória JVM, espaço em disco e subsistemas de faturação e auditoria.</center></html>");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        desc.setForeground(UIHelper.TEXT_MUTED);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(desc);
        card.add(javax.swing.Box.createVerticalStrut(24));

        mz.multicore.erp.gui.components.ModernButton btn = new mz.multicore.erp.gui.components.ModernButton(
                "Abrir Diagnóstico do Sistema", UIHelper.ACCENT_CYAN, UIHelper.ACCENT_CYAN.darker());
        btn.setIcon(UIHelper.icon("fas-external-link-alt", 14, Color.WHITE));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.addActionListener(e -> new SystemMonitoringDialog(SwingUtilities.getWindowAncestor(this), monitoringApiClient).show());
        card.add(btn);

        panel.add(card);
        return panel;
    }
}
