package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.platform.dto.CreateCompanyRequest;
import com.phcpro.modules.platform.dto.PlatformCompanyDTO;
import com.phcpro.modules.platform.dto.UpdateCompanyRequest;
import com.phcpro.desktop.client.PlatformApiClient;
import com.phcpro.modules.platform.dto.CreatePlatformUserRequest;
import com.phcpro.modules.platform.dto.GrantAccessRequest;
import com.phcpro.modules.platform.dto.PlatformUserDTO;
import com.phcpro.modules.subscription.dto.RecordPaymentRequest;
import com.phcpro.modules.subscription.dto.SaveSubscriptionRequest;
import com.phcpro.modules.subscription.dto.SubscriptionDTO;
import com.phcpro.modules.subscription.dto.SubscriptionPaymentDTO;
import com.phcpro.modules.support.dto.SupportMessageDTO;
import com.phcpro.modules.support.dto.SupportTicketDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Consola da plataforma (superadmin). Fase 1: gestão de empresas (listar, activar/desactivar,
 * criar e editar). Fases seguintes acrescentam abas de Pagamentos, Utilizadores e Assistência.
 */
public class PlataformaPanel extends JPanel {

    private final PlatformApiClient platformApiClient;

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

    public PlataformaPanel(PlatformApiClient platformApiClient) {
        this.platformApiClient = platformApiClient;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPanePHC(tabbedPane);
        tabbedPane.addTab("Empresas", UIHelper.icon("fas-building", 16, UIHelper.TEXT_LIGHT), createCompaniesTab());
        tabbedPane.addTab("Assinaturas & Pagamentos", UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.TEXT_LIGHT),
                createSubscriptionsTab());
        tabbedPane.addTab("Utilizadores", UIHelper.icon("fas-users-cog", 16, UIHelper.TEXT_LIGHT), createUsersTab());
        tabbedPane.addTab("Assistência", UIHelper.icon("fas-headset", 16, UIHelper.TEXT_LIGHT), createSupportTab());
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
    }

    private JPanel createCompaniesTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Empresas da Plataforma"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createSuccessButton("Nova Empresa");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton editBtn = UIHelper.createPrimaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-pen", 14));
        ModernButton toggleBtn = UIHelper.createSecondaryButton("Activar/Desactivar");
        toggleBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        actions.add(toggleBtn);
        actions.add(editBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

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
        listCard.add(cBar, BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        newBtn.addActionListener(e -> createCompany());
        editBtn.addActionListener(e -> editSelectedCompany());
        toggleBtn.addActionListener(e -> toggleSelectedCompany());
        refreshBtn.addActionListener(e -> loadCompanies());

        return panel;
    }

    private void loadCompanies() {
        try {
            companies = platformApiClient.listCompanies();
            companiesModel.setRowCount(0);
            for (PlatformCompanyDTO c : companies) {
                companiesModel.addRow(new Object[]{
                        c.name(), c.taxId(), c.email() == null ? "" : c.email(),
                        c.userCount(), c.active() ? "ACTIVA" : "SUSPENSA"
                });
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Empresas", JOptionPane.ERROR_MESSAGE);
        }
    }

    private PlatformCompanyDTO selectedCompany() {
        int row = TableFilter.selectedModelRow(companiesTable);
        if (row < 0 || row >= companies.size()) {
            JOptionPane.showMessageDialog(this, "Selecione uma empresa na lista.", "Empresas",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return companies.get(row);
    }

    private void createCompany() {
        JTextField nameField = new JTextField();
        JTextField taxIdField = new JTextField();
        JTextField emailField = new JTextField();
        JTextField addressField = new JTextField();
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(taxIdField);
        UIHelper.styleTextField(emailField);
        UIHelper.styleTextField(addressField);

        JPanel form = UIHelper.createDialogForm(
                "Nome:", nameField,
                "NUIT:", taxIdField,
                "Email:", emailField,
                "Endereço:", addressField
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Nova Empresa",
                "fas-building", "Registar uma empresa na plataforma", form)
                .setConfirmButton("Criar", "fas-plus");
        dlg.setOnSave(() -> {
            if (nameField.getText().trim().isEmpty() || taxIdField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("Nome e NUIT são obrigatórios.");
            }
            platformApiClient.createCompany(new CreateCompanyRequest(
                    nameField.getText().trim(), taxIdField.getText().trim(),
                    emailField.getText().trim(), addressField.getText().trim()));
        });

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Empresa criada com sucesso!", "Sucesso",
                    JOptionPane.INFORMATION_MESSAGE);
            loadCompanies();
        }
    }

    private void editSelectedCompany() {
        PlatformCompanyDTO company = selectedCompany();
        if (company == null) return;

        JTextField nameField = new JTextField(company.name());
        JTextField emailField = new JTextField(company.email() == null ? "" : company.email());
        JTextField addressField = new JTextField(company.address() == null ? "" : company.address());
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(emailField);
        UIHelper.styleTextField(addressField);

        JPanel form = UIHelper.createDialogForm(
                "Nome:", nameField,
                "Email:", emailField,
                "Endereço:", addressField
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Editar Empresa",
                "fas-pen", "NUIT: " + company.taxId(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSave(() -> {
            if (nameField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("O nome é obrigatório.");
            }
            platformApiClient.updateCompany(company.id(), new UpdateCompanyRequest(
                    nameField.getText().trim(), emailField.getText().trim(), addressField.getText().trim()));
        });

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Empresa actualizada.", "Sucesso",
                    JOptionPane.INFORMATION_MESSAGE);
            loadCompanies();
        }
    }

    private void toggleSelectedCompany() {
        PlatformCompanyDTO company = selectedCompany();
        if (company == null) return;

        boolean newState = !company.active();
        String verb = newState ? "activar" : "suspender";
        int confirm = JOptionPane.showConfirmDialog(this,
                "Deseja " + verb + " a empresa '" + company.name() + "'?"
                        + (newState ? "" : "\nOs utilizadores desta empresa deixam de poder iniciar sessão."),
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            platformApiClient.setCompanyActive(company.id(), newState);
            loadCompanies();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Empresas", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------- Assinaturas & Pagamentos

    private JPanel createSubscriptionsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Assinaturas & Pagamentos"), BorderLayout.WEST);

        ModernButton planBtn = UIHelper.createPrimaryButton("Definir Plano/Validade");
        planBtn.setIcon(UIHelper.icon("fas-sliders-h", 14));
        ModernButton payBtn = UIHelper.createSuccessButton("Registar Pagamento");
        payBtn.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        ModernButton historyBtn = UIHelper.createSecondaryButton("Ver Pagamentos");
        historyBtn.setIcon(UIHelper.icon("fas-receipt", 14));
        ModernButton toggleBtn = UIHelper.createSecondaryButton("Suspender/Reactivar");
        toggleBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        actions.add(toggleBtn);
        actions.add(historyBtn);
        actions.add(payBtn);
        actions.add(planBtn);
        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

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
        listCard.add(sBar, BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        planBtn.addActionListener(e -> defineSubscription());
        payBtn.addActionListener(e -> recordPayment());
        historyBtn.addActionListener(e -> showPayments());
        toggleBtn.addActionListener(e -> toggleSubscriptionStatus());
        refreshBtn.addActionListener(e -> loadSubscriptions());

        return panel;
    }

    private void loadSubscriptions() {
        try {
            subscriptions = platformApiClient.listOverview();
            subsModel.setRowCount(0);
            for (SubscriptionDTO s : subscriptions) {
                subsModel.addRow(new Object[]{
                        s.companyName(),
                        s.planLabel(),
                        s.statusLabel(),
                        s.validUntil() == null ? "—" : s.validUntil().toString(),
                        s.monthlyPrice() == null ? "—" : s.monthlyPrice().toPlainString(),
                        s.paymentCount()
                });
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Assinaturas", JOptionPane.ERROR_MESSAGE);
        }
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
            JOptionPane.showMessageDialog(this, "Selecione uma empresa na lista.", "Assinaturas",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return subscriptions.get(row);
    }

    private void defineSubscription() {
        SubscriptionDTO sub = selectedSubscription();
        if (sub == null) return;

        JComboBox<String> planCombo = new JComboBox<>(platformApiClient.planOptions().toArray(new String[0]));
        UIHelper.styleComboBox(planCombo);
        if (sub.plan() != null) planCombo.setSelectedItem(sub.plan());
        JTextField priceField = new JTextField(sub.monthlyPrice() == null ? "" : sub.monthlyPrice().toPlainString());
        JTextField validField = new JTextField(sub.validUntil() == null ? "" : sub.validUntil().toString());
        UIHelper.styleTextField(priceField);
        UIHelper.styleTextField(validField);

        JPanel form = UIHelper.createDialogForm(
                "Plano:", planCombo,
                "Preço mensal (MT):", priceField,
                "Válida até (AAAA-MM-DD):", validField
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Definir Assinatura",
                "fas-sliders-h", sub.companyName(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSave(() -> platformApiClient.saveSubscription(sub.companyId(), new SaveSubscriptionRequest(
                (String) planCombo.getSelectedItem(),
                parseAmount(priceField.getText(), "preço mensal"),
                parseDate(validField.getText()))));

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Assinatura actualizada.", "Sucesso",
                    JOptionPane.INFORMATION_MESSAGE);
            loadSubscriptions();
        }
    }

    private void recordPayment() {
        SubscriptionDTO sub = selectedSubscription();
        if (sub == null) return;

        JTextField amountField = new JTextField();
        JComboBox<String> methodCombo = new JComboBox<>(platformApiClient.methodOptions().toArray(new String[0]));
        UIHelper.styleComboBox(methodCombo);
        JTextField paidAtField = new JTextField(LocalDate.now().toString());
        JTextField startField = new JTextField();
        JTextField endField = new JTextField();
        JTextField noteField = new JTextField();
        UIHelper.styleTextField(amountField);
        UIHelper.styleTextField(paidAtField);
        UIHelper.styleTextField(startField);
        UIHelper.styleTextField(endField);
        UIHelper.styleTextField(noteField);

        JPanel form = UIHelper.createDialogForm(
                "Valor (MT):", amountField,
                "Método:", methodCombo,
                "Pago em (AAAA-MM-DD):", paidAtField,
                "Período de (AAAA-MM-DD):", startField,
                "Período até (AAAA-MM-DD):", endField,
                "Nota:", noteField
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Registar Pagamento",
                "fas-money-bill-wave", sub.companyName() + " — o período até estende a validade", form)
                .setConfirmButton("Registar", "fas-check");
        dlg.setOnSave(() -> platformApiClient.recordPayment(sub.companyId(), new RecordPaymentRequest(
                parseAmount(amountField.getText(), "valor"),
                (String) methodCombo.getSelectedItem(),
                parseDate(paidAtField.getText()),
                parseDate(startField.getText()),
                parseDate(endField.getText()),
                noteField.getText().trim())));

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Pagamento registado.", "Sucesso",
                    JOptionPane.INFORMATION_MESSAGE);
            loadSubscriptions();
        }
    }

    private void showPayments() {
        SubscriptionDTO sub = selectedSubscription();
        if (sub == null) return;

        List<SubscriptionPaymentDTO> payments = platformApiClient.listPayments(sub.companyId());
        String[] cols = {"Pago em", "Valor", "Método", "Período", "Nota"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (SubscriptionPaymentDTO p : payments) {
            String period = (p.periodStart() == null ? "—" : p.periodStart().toString())
                    + " a " + (p.periodEnd() == null ? "—" : p.periodEnd().toString());
            model.addRow(new Object[]{
                    p.paidAt() == null ? "" : p.paidAt().toString(),
                    p.amount() == null ? "" : p.amount().toPlainString(),
                    p.methodLabel(), period, p.note() == null ? "" : p.note()
            });
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(680, 320));

        JOptionPane.showMessageDialog(this, scroll, "Pagamentos — " + sub.companyName(),
                JOptionPane.PLAIN_MESSAGE);
    }

    private void toggleSubscriptionStatus() {
        SubscriptionDTO sub = selectedSubscription();
        if (sub == null) return;
        if (!sub.hasSubscription()) {
            JOptionPane.showMessageDialog(this, "Defina primeiro a assinatura desta empresa.",
                    "Assinaturas", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean suspended = "SUSPENDED".equals(sub.status());
        String target = suspended ? "ACTIVE" : "SUSPENDED";
        String verb = suspended ? "reactivar" : "suspender";
        int confirm = JOptionPane.showConfirmDialog(this,
                "Deseja " + verb + " a assinatura de '" + sub.companyName() + "'?"
                        + (suspended ? "" : "\nOs utilizadores desta empresa deixam de poder iniciar sessão."),
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            platformApiClient.changeSubscriptionStatus(sub.companyId(), target);
            loadSubscriptions();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Assinaturas", JOptionPane.ERROR_MESSAGE);
        }
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

    private static final String[] TENANT_ROLES = {"EMPLOYEE", "MANAGER", "ADMIN"};

    private JPanel createUsersTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Utilizadores de Todas as Empresas"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createSuccessButton("Novo Utilizador");
        newBtn.setIcon(UIHelper.icon("fas-user-plus", 14));
        ModernButton editBtn = UIHelper.createPrimaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-pen", 14));
        ModernButton grantBtn = UIHelper.createPrimaryButton("Conceder/Alterar Acesso");
        grantBtn.setIcon(UIHelper.icon("fas-user-shield", 14));
        ModernButton revokeBtn = UIHelper.createSecondaryButton("Revogar Acesso");
        revokeBtn.setIcon(UIHelper.icon("fas-user-slash", 14));
        ModernButton pwdBtn = UIHelper.createSecondaryButton("Repor Senha");
        pwdBtn.setIcon(UIHelper.icon("fas-key", 14));
        ModernButton toggleBtn = UIHelper.createSecondaryButton("Activar/Desactivar");
        toggleBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        actions.add(toggleBtn);
        actions.add(pwdBtn);
        actions.add(revokeBtn);
        actions.add(grantBtn);
        actions.add(editBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

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
        listCard.add(uBar, BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        newBtn.addActionListener(e -> createPlatformUser());
        editBtn.addActionListener(e -> editUser());
        grantBtn.addActionListener(e -> grantAccess());
        revokeBtn.addActionListener(e -> revokeAccess());
        pwdBtn.addActionListener(e -> resetPassword());
        toggleBtn.addActionListener(e -> toggleUserActive());
        refreshBtn.addActionListener(e -> loadUsers());

        return panel;
    }

    private void loadUsers() {
        try {
            users = platformApiClient.listUsers();
            usersModel.setRowCount(0);
            for (PlatformUserDTO u : users) {
                usersModel.addRow(new Object[]{
                        u.username(), u.name(), describeAccesses(u),
                        u.platformAdmin() ? "SUPERADMIN" : (u.active() ? "ACTIVO" : "INATIVO")
                });
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Utilizadores", JOptionPane.ERROR_MESSAGE);
        }
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
            JOptionPane.showMessageDialog(this, "Selecione um utilizador na lista.", "Utilizadores",
                    JOptionPane.WARNING_MESSAGE);
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
        dlg.setOnSave(() -> {
            if (nameField.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("O nome é obrigatório.");
            }
            platformApiClient.updateUser(user.username(), nameField.getText().trim());
        });
        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Utilizador actualizado.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadUsers();
        }
    }

    private void createPlatformUser() {
        if (companies.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Crie primeiro uma empresa.", "Utilizadores",
                    JOptionPane.WARNING_MESSAGE);
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

        JPanel form = UIHelper.createDialogForm(
                "Utilizador:", usernameField,
                "Nome completo:", nameField,
                "Senha:", passwordField,
                "Empresa:", companyCombo,
                "Papel:", roleCombo
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Novo Utilizador",
                "fas-user-plus", "Conta ligada a uma empresa", form).setConfirmButton("Criar", "fas-check");
        dlg.setOnSave(() -> {
            if (usernameField.getText().trim().isEmpty() || nameField.getText().trim().isEmpty()
                    || passwordField.getPassword().length == 0) {
                throw new IllegalArgumentException("Utilizador, nome e senha são obrigatórios.");
            }
            platformApiClient.createUser(new CreatePlatformUserRequest(
                    usernameField.getText().trim(), nameField.getText().trim(),
                    new String(passwordField.getPassword()),
                    ((CompanyItem) companyCombo.getSelectedItem()).id(),
                    (String) roleCombo.getSelectedItem()));
        });

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Utilizador criado.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadUsers();
        }
    }

    private void grantAccess() {
        PlatformUserDTO user = selectedUser();
        if (user == null) return;
        if (companies.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Não há empresas.", "Utilizadores", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<CompanyItem> companyCombo = companyCombo();
        JComboBox<String> roleCombo = new JComboBox<>(TENANT_ROLES);
        UIHelper.styleComboBox(companyCombo);
        UIHelper.styleComboBox(roleCombo);

        JPanel form = UIHelper.createDialogForm("Empresa:", companyCombo, "Papel:", roleCombo);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Conceder/Alterar Acesso",
                "fas-user-shield", "Utilizador: " + user.username(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSave(() -> platformApiClient.grantAccess(user.username(), new GrantAccessRequest(
                ((CompanyItem) companyCombo.getSelectedItem()).id(), (String) roleCombo.getSelectedItem())));

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Acesso actualizado.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadUsers();
        }
    }

    private void revokeAccess() {
        PlatformUserDTO user = selectedUser();
        if (user == null) return;
        if (user.companies().isEmpty()) {
            JOptionPane.showMessageDialog(this, "O utilizador não tem acessos a revogar.", "Utilizadores",
                    JOptionPane.WARNING_MESSAGE);
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
        dlg.setOnSave(() -> platformApiClient.revokeAccess(user.username(),
                ((CompanyItem) combo.getSelectedItem()).id()));

        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Acesso revogado.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
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
        dlg.setOnSave(() -> {
            if (passwordField.getPassword().length == 0) {
                throw new IllegalArgumentException("Indique a nova senha.");
            }
            platformApiClient.resetPassword(user.username(), new String(passwordField.getPassword()));
        });
        if (dlg.showDialog()) {
            JOptionPane.showMessageDialog(this, "Senha reposta.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void toggleUserActive() {
        PlatformUserDTO user = selectedUser();
        if (user == null) return;
        boolean newState = !user.active();
        try {
            platformApiClient.setUserActive(user.username(), newState);
            loadUsers();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Utilizadores", JOptionPane.ERROR_MESSAGE);
        }
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

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Pedidos de Assistência"), BorderLayout.WEST);

        ModernButton openBtn = UIHelper.createPrimaryButton("Abrir / Responder");
        openBtn.setIcon(UIHelper.icon("fas-comments", 14));
        ModernButton statusBtn = UIHelper.createSecondaryButton("Mudar Estado");
        statusBtn.setIcon(UIHelper.icon("fas-tasks", 14));
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        actions.add(statusBtn);
        actions.add(openBtn);
        header.add(actions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

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
        listCard.add(tBar, BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);
        panel.add(listCard, BorderLayout.CENTER);

        openBtn.addActionListener(e -> openTicketConversation());
        statusBtn.addActionListener(e -> changeTicketStatus());
        refreshBtn.addActionListener(e -> loadTickets());

        return panel;
    }

    private void loadTickets() {
        try {
            tickets = platformApiClient.listAllTickets();
            ticketsModel.setRowCount(0);
            for (SupportTicketDTO t : tickets) {
                ticketsModel.addRow(new Object[]{
                        t.id(), t.companyName(), t.subject(), t.priorityLabel(), t.statusLabel(),
                        t.assignee() == null ? "—" : t.assignee(), t.messageCount()
                });
            }
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Assistência", JOptionPane.ERROR_MESSAGE);
        }
    }

    private SupportTicketDTO selectedTicket() {
        int row = TableFilter.selectedModelRow(ticketsTable);
        if (row < 0 || row >= tickets.size()) {
            JOptionPane.showMessageDialog(this, "Selecione um pedido na lista.", "Assistência",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return tickets.get(row);
    }

    private void openTicketConversation() {
        SupportTicketDTO ticket = selectedTicket();
        if (ticket == null) return;

        JTextArea thread = new JTextArea(renderThread(platformApiClient.listPlatformMessages(ticket.id())));
        thread.setEditable(false);
        thread.setLineWrap(true);
        thread.setWrapStyleWord(true);
        JScrollPane threadScroll = new JScrollPane(thread);
        threadScroll.setPreferredSize(new Dimension(560, 260));

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
            try {
                platformApiClient.addSuperAdminReply(ticket.id(), reply.getText().trim());
                loadTickets();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Assistência", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void changeTicketStatus() {
        SupportTicketDTO ticket = selectedTicket();
        if (ticket == null) return;
        JComboBox<String> statusCombo = new JComboBox<>(platformApiClient.statusOptions().toArray(new String[0]));
        UIHelper.styleComboBox(statusCombo);
        statusCombo.setSelectedItem(ticket.status());
        JPanel form = UIHelper.createDialogForm("Estado:", statusCombo);
        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Mudar Estado",
                "fas-tasks", "Pedido #" + ticket.id(), form).setConfirmButton("Guardar", "fas-check");
        dlg.setOnSave(() -> platformApiClient.changeTicketStatus(ticket.id(), (String) statusCombo.getSelectedItem()));
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
}
