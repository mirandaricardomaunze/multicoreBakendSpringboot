package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.comercial.dto.ClientDTO;
import com.phcpro.modules.comercial.service.ComercialService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClientesPanel extends JPanel {

    private final ComercialService comercialService;

    private JTextField searchField;
    private DefaultTableModel model;
    private JTable table;
    private List<ClientDTO> allClients = new ArrayList<>();
    private List<ClientDTO> visibleClients = new ArrayList<>();

    public ClientesPanel(ComercialService comercialService) {
        this.comercialService = comercialService;

        setLayout(new BorderLayout(0, 15));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(25, 25, 25, 25));

        // TOP BAR
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.add(UIHelper.createHeading("Gestão de Clientes"), BorderLayout.WEST);

        ModernButton newBtn = new ModernButton("Novo Cliente", new Color(16, 185, 129), new Color(52, 211, 153));
        newBtn.setIcon(UIHelper.icon("fas-user-plus", 14));
        ModernButton editBtn = new ModernButton("Editar", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE.brighter());
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        ModernButton deleteBtn = new ModernButton("Eliminar", UIHelper.REJECTED_RED, UIHelper.REJECTED_RED.brighter());
        deleteBtn.setIcon(UIHelper.icon("fas-trash", 14));
        ModernButton refreshBtn = new ModernButton("Atualizar", new Color(107, 114, 128), new Color(156, 163, 175));
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

        JPanel searchRow = new JPanel(new BorderLayout(8, 0));
        searchRow.setOpaque(false);
        JLabel searchLbl = new JLabel("Pesquisar:");
        searchLbl.setForeground(UIHelper.TEXT_MUTED);
        searchLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchField = new JTextField();
        UIHelper.styleTextField(searchField);
        searchField.putClientProperty("JTextField.placeholderText",
                "🔍 Filtrar por nome, NUIT, email ou endereço…");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refilter(); }
            @Override public void removeUpdate(DocumentEvent e) { refilter(); }
            @Override public void changedUpdate(DocumentEvent e) { refilter(); }
        });
        searchRow.add(searchLbl, BorderLayout.WEST);
        searchRow.add(searchField, BorderLayout.CENTER);
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

        onPanelSelected();
    }

    public void onPanelSelected() {
        allClients = comercialService.getAllClients();
        refilter();
    }

    private void refilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        visibleClients = allClients.stream()
                .filter(c -> q.isEmpty()
                        || (c.name() != null && c.name().toLowerCase().contains(q))
                        || (c.taxId() != null && c.taxId().toLowerCase().contains(q))
                        || (c.email() != null && c.email().toLowerCase().contains(q))
                        || (c.address() != null && c.address().toLowerCase().contains(q)))
                .toList();
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
        int row = table.getSelectedRow();
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

        JPanel form = UIHelper.createDialogForm(
                "Nome:", nameField,
                "NUIT / NIF:", taxIdField,
                "Email:", emailField,
                "Endereço:", addressField
        );

        String title = existing == null ? "Novo Cliente" : "Editar Cliente — " + existing.name();
        int opt = JOptionPane.showConfirmDialog(this, UIHelper.makeDialogScrollable(form), title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        String taxId = taxIdField.getText().trim();
        String email = emailField.getText().trim();
        String address = addressField.getText().trim();

        if (name.isEmpty() || taxId.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nome, NUIT e Email são obrigatórios.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (existing == null) {
                comercialService.createClient(name, taxId, email, address);
                JOptionPane.showMessageDialog(this, "Cliente '" + name + "' criado.",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                comercialService.updateClient(existing.id(), name, taxId, email, address);
                JOptionPane.showMessageDialog(this, "Cliente atualizado.",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }
            onPanelSelected();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelected() {
        ClientDTO c = selectedClient();
        if (c == null) return;
        int confirm = JOptionPane.showConfirmDialog(this,
                "Eliminar o cliente '" + c.name() + "'? Esta ação não pode ser revertida.",
                "Confirmar Eliminação", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        try {
            comercialService.deleteClient(c.id());
            JOptionPane.showMessageDialog(this, "Cliente eliminado.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            onPanelSelected();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
