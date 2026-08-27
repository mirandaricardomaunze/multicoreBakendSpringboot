package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Submissão e listagem de despesas de colaboradores. */
final class HRExpensesPanel {
    private final HRPanel owner;
    HRExpensesPanel(HRPanel owner) { this.owner = owner; }

    public JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Notas de Despesas"), BorderLayout.WEST);

        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.addActionListener(e -> owner.exportTable("despesas", "Notas de Despesas", owner.expensesTable));
        ModernButton submitBtn = UIHelper.createPrimaryButton("Submeter Despesa");
        submitBtn.setIcon(UIHelper.icon("fas-paper-plane", 14));
        submitBtn.addActionListener(e -> submitExpense());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(exportBtn);
        actions.add(submitBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        String[] cols = {"Colaborador", "Valor", "Categoria", "Estado", "Motivo Rejeição"};
        owner.expensesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.expensesTable = new JTable(owner.expensesModel);
        UIHelper.styleTable(owner.expensesTable);
        owner.expensesTable.getColumnModel().getColumn(1).setCellRenderer(TableCellRenderers.money());
        owner.expensesTable.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(owner.expensesTable);
        UIHelper.styleScrollPane(scroll);

        JTextField expSearch = TableFilter.searchField("Colaborador, categoria ou motivo…");
        JComboBox<String> expEstado = TableFilter.combo("Todos os estados",
                "PENDING_APPROVAL", "APPROVED", "REJECTED");
        TableFilter.install(owner.expensesTable, expSearch,
                new TableFilter.ColumnFilter(expEstado, 3));
        JPanel expBar = TableFilter.bar(expSearch, TableFilter.label("Estado:"), expEstado);
        expBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(expBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    public void refresh() {
        UIHelper.loadAsync(owner, owner.hrApiClient::getAllExpenses, this::applyExpenses,
                error -> owner.showLoadError("notas de despesas", error));
    }

    private void applyExpenses(List<ExpenseClaimDTO> loaded) {
        owner.expensesList = loaded;
        owner.expensesModel.setRowCount(0);
        for (ExpenseClaimDTO c : owner.expensesList) {
            owner.expensesModel.addRow(new Object[]{
                    c.employeeName(),
                    c.amount(),
                    c.category(),
                    c.status().name(),
                    c.rejectionReason() == null ? "" : c.rejectionReason()
            });
        }
        owner.refreshOverview();
    }

    private void submitExpense() {
        if (owner.employeesList.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Cadastre colaboradores primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : owner.employeesList) empCombo.addItem(e.name() + " — " + e.department());

        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{
                "MEALS (Alimentação)", "TRAVEL (Deslocações)", "LODGING (Alojamento)", "OTHER (Outros)"
        });
        UIHelper.styleComboBox(categoryCombo);
        MoneyField amountField = new MoneyField();
        JTextField descField = new JTextField();
        UIHelper.styleTextField(descField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Categoria:", categoryCombo,
                "Valor (MT):", amountField,
                "Descrição:", descField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Submeter Despesa", "fas-receipt",
                "Nota de despesa do colaborador", form)
                .setConfirmButton("Submeter", "fas-paper-plane").showDialog();
        if (!confirmed) return;

        try {
            BigDecimal amount = amountField.value();
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            String desc = descField.getText().trim();
            if (desc.isEmpty()) {
                JOptionPane.showMessageDialog(owner, "Indique uma descrição.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            EmployeeDTO emp = owner.employeesList.get(empCombo.getSelectedIndex());
            String cat = categoryCombo.getSelectedItem().toString().split(" ")[0];
            CreateExpenseClaimRequest request = new CreateExpenseClaimRequest(emp.id(), amount, cat, desc);
            UIHelper.runWithProgress(owner, "A submeter despesa…", () -> owner.hrApiClient.submitExpense(request), ignored -> {
                JOptionPane.showMessageDialog(owner, "Despesa submetida.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                refresh();
            }, owner::showActionError);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(owner, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

}
