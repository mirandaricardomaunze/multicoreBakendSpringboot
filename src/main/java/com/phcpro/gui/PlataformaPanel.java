package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.platform.dto.CreateCompanyRequest;
import com.phcpro.modules.platform.dto.PlatformCompanyDTO;
import com.phcpro.modules.platform.dto.UpdateCompanyRequest;
import com.phcpro.modules.platform.service.PlatformCompanyService;
import com.phcpro.modules.subscription.dto.RecordPaymentRequest;
import com.phcpro.modules.subscription.dto.SaveSubscriptionRequest;
import com.phcpro.modules.subscription.dto.SubscriptionDTO;
import com.phcpro.modules.subscription.dto.SubscriptionPaymentDTO;
import com.phcpro.modules.subscription.service.SubscriptionService;

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

    private final PlatformCompanyService platformCompanyService;
    private final SubscriptionService subscriptionService;

    private DefaultTableModel companiesModel;
    private JTable companiesTable;
    private List<PlatformCompanyDTO> companies = new ArrayList<>();

    private DefaultTableModel subsModel;
    private JTable subsTable;
    private List<SubscriptionDTO> subscriptions = new ArrayList<>();

    public PlataformaPanel(PlatformCompanyService platformCompanyService,
                           SubscriptionService subscriptionService) {
        this.platformCompanyService = platformCompanyService;
        this.subscriptionService = subscriptionService;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPane(tabbedPane);
        tabbedPane.addTab("Empresas", UIHelper.icon("fas-building", 16, UIHelper.TEXT_LIGHT), createCompaniesTab());
        tabbedPane.addTab("Assinaturas & Pagamentos", UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.TEXT_LIGHT),
                createSubscriptionsTab());
        add(tabbedPane, BorderLayout.CENTER);

        onPanelSelected();
    }

    /** Chamado pela MainFrame quando o painel fica activo. */
    public void onPanelSelected() {
        loadCompanies();
        loadSubscriptions();
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
            companies = platformCompanyService.listCompanies();
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
        int row = companiesTable.getSelectedRow();
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
            platformCompanyService.createCompany(new CreateCompanyRequest(
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
            platformCompanyService.updateCompany(company.id(), new UpdateCompanyRequest(
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
            platformCompanyService.setCompanyActive(company.id(), newState);
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
        JScrollPane scroll = new JScrollPane(subsTable);
        UIHelper.styleScrollPane(scroll);
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
            subscriptions = subscriptionService.listOverview();
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

    private SubscriptionDTO selectedSubscription() {
        int row = subsTable.getSelectedRow();
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

        JComboBox<String> planCombo = new JComboBox<>(subscriptionService.planOptions().toArray(new String[0]));
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
        dlg.setOnSave(() -> subscriptionService.saveSubscription(sub.companyId(), new SaveSubscriptionRequest(
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
        JComboBox<String> methodCombo = new JComboBox<>(subscriptionService.methodOptions().toArray(new String[0]));
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
        dlg.setOnSave(() -> subscriptionService.recordPayment(sub.companyId(), new RecordPaymentRequest(
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

        List<SubscriptionPaymentDTO> payments = subscriptionService.listPayments(sub.companyId());
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
            subscriptionService.changeStatus(sub.companyId(), target);
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
}
