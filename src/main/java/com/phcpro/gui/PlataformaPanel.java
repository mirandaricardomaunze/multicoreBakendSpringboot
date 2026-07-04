package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.platform.dto.CreateCompanyRequest;
import com.phcpro.modules.platform.dto.PlatformCompanyDTO;
import com.phcpro.modules.platform.dto.UpdateCompanyRequest;
import com.phcpro.modules.platform.service.PlatformCompanyService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Consola da plataforma (superadmin). Fase 1: gestão de empresas (listar, activar/desactivar,
 * criar e editar). Fases seguintes acrescentam abas de Pagamentos, Utilizadores e Assistência.
 */
public class PlataformaPanel extends JPanel {

    private final PlatformCompanyService platformCompanyService;

    private DefaultTableModel companiesModel;
    private JTable companiesTable;
    private List<PlatformCompanyDTO> companies = new ArrayList<>();

    public PlataformaPanel(PlatformCompanyService platformCompanyService) {
        this.platformCompanyService = platformCompanyService;

        setLayout(new BorderLayout());
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JTabbedPane tabbedPane = new JTabbedPane();
        UIHelper.styleTabbedPane(tabbedPane);
        tabbedPane.addTab("Empresas", UIHelper.icon("fas-building", 16, UIHelper.TEXT_LIGHT), createCompaniesTab());
        add(tabbedPane, BorderLayout.CENTER);

        onPanelSelected();
    }

    /** Chamado pela MainFrame quando o painel fica activo. */
    public void onPanelSelected() {
        loadCompanies();
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
}
