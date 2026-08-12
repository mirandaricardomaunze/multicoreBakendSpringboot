package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.FormField;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.desktop.client.ComercialApiClient;
import com.phcpro.modules.comercial.dto.ClientDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClientesPanel extends JPanel {

    private final ComercialApiClient comercialApiClient;

    private JTextField searchField;
    private DefaultTableModel model;
    private JTable table;
    private List<ClientDTO> allClients = new ArrayList<>();
    private List<ClientDTO> visibleClients = new ArrayList<>();

    public ClientesPanel(ComercialApiClient comercialApiClient) {
        this.comercialApiClient = comercialApiClient;

        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        // TOP BAR
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(UIHelper.createHeading("Gestão de Clientes"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createSuccessButton("Novo Cliente");
        newBtn.setIcon(UIHelper.icon("fas-user-plus", 14));
        ModernButton editBtn = UIHelper.createPrimaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        ModernButton deleteBtn = UIHelper.createDangerButton("Eliminar");
        deleteBtn.setIcon(UIHelper.icon("fas-trash", 14));
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Atualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        actions.add(editBtn);
        actions.add(deleteBtn);
        actions.add(newBtn);
        topBar.add(actions, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // CENTER CARD: search + table
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        searchField = TableFilter.searchField("Filtrar por nome, NUIT, email ou endereço…");
        JPanel searchRow = TableFilter.bar(searchField);
        center.add(searchRow, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Nome", "NUIT / NIF", "Email", "Endereço"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMaxWidth(60);
        }
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableFilter.install(table, searchField);
        card.add(scroll, BorderLayout.CENTER);
        center.add(card, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // LISTENERS
        newBtn.addActionListener(e -> openClientDialog(null));
        editBtn.addActionListener(e -> {
            ClientDTO selected = selectedClient();
            if (selected != null) openClientDialog(selected);
        });
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> onPanelSelected());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent ev) {
                if (ev.getClickCount() == 2) {
                    ClientDTO selected = selectedClient();
                    if (selected != null) openClientDialog(selected);
                }
            }
        });

        // Carregamento preguiçoso: só busca clientes quando o painel é aberto (navegação chama
        // onPanelSelected). Evita chamada HTTP no construtor — que falharia para o superadmin, que
        // não tem empresa activa (sem X-Company-Id o servidor recusa o pedido).
    }

    public void onPanelSelected() {
        UIHelper.loadAsync(this, comercialApiClient::getClients, clients -> {
            allClients = clients;
            refilter();
        }, error -> JOptionPane.showMessageDialog(this,
                "Não foi possível carregar os clientes: " + error.getMessage(),
                "Erro de ligação", JOptionPane.ERROR_MESSAGE));
    }

    private void refilter() {
        // Carrega todos; a pesquisa é aplicada pelo TableFilter (cliente).
        visibleClients = allClients;
        model.setRowCount(0);
        for (ClientDTO c : visibleClients) {
            model.addRow(new Object[]{
                    c.id(),
                    c.name(),
                    c.taxId(),
                    c.email(),
                    c.address() == null ? "" : c.address()
            });
        }
    }

    private ClientDTO selectedClient() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um cliente na tabela.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return visibleClients.get(row);
    }

    private void openClientDialog(ClientDTO existing) {
        JTextField nameField = new JTextField(existing == null ? "" : existing.name());
        JTextField taxIdField = new JTextField(existing == null ? "" : existing.taxId());
        JTextField emailField = new JTextField(existing == null ? "" : existing.email());
        JTextField addressField = new JTextField(existing == null || existing.address() == null ? "" : existing.address());
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(taxIdField);
        UIHelper.styleTextField(emailField);
        UIHelper.styleTextField(addressField);

        FormField nameForm = new FormField("Nome", nameField, true, "Nome completo ou denominação social");
        FormField taxForm = new FormField("NUIT / NIF", taxIdField, true, null);
        FormField emailForm = new FormField("Email", emailField, true, null);
        FormField addressForm = new FormField("Endereço", addressField, false, null);
        JPanel form = UIHelper.createDialogForm(
                "", nameForm, "", taxForm, "", emailForm, "", addressForm);

        String title = existing == null ? "Novo Cliente" : "Editar Cliente — " + existing.name();
        ModernFormDialog dialog = new ModernFormDialog(
                UIHelper.mainWindow, title, null, "Dados de cadastro do cliente", form);
        dialog.setOnSaveAsync(() -> {
            boolean valid = nameForm.validateRequired() & taxForm.validateRequired() & emailForm.validateRequired();
            if (!valid) throw new IllegalArgumentException("Corrija os campos assinalados.");
            String name = nameField.getText().trim();
            String taxId = taxIdField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressField.getText().trim();
            return () -> {
                if (existing == null) comercialApiClient.createClient(name, taxId, email, address);
                else comercialApiClient.updateClient(existing.id(), name, taxId, email, address);
                return null;
            };
        });
        boolean confirmed = dialog.showDialog();
        if (!confirmed) return;
        JOptionPane.showMessageDialog(this, existing == null ? "Cliente criado." : "Cliente atualizado.",
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        onPanelSelected();
    }

    private void deleteSelected() {
        ClientDTO c = selectedClient();
        if (c == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Eliminar o cliente '" + c.name() + "'? Esta ação não pode ser revertida.",
                "Confirmar Eliminação", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            comercialApiClient.deleteClient(c.id());
            JOptionPane.showMessageDialog(this, "Cliente eliminado.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            onPanelSelected();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
