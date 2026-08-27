package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.CreateVacationRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.VacationDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Separador de pedidos de férias. Extraído do {@link HRPanel} a 2026-08-23: o painel estava a
 * 998/1000 linhas do {@code UiPanelDecompositionTest} e o ponto (B2) precisava de espaço para o
 * seu próprio separador. Molde do {@link HRExpensesPanel}.
 */
final class HRVacationsPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HRPanel owner;

    HRVacationsPanel(HRPanel owner) { this.owner = owner; }

    JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Pedidos de Férias"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createPrimaryButton("Novo Pedido");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton approveBtn = UIHelper.createSuccessButton("Aprovar");
        approveBtn.setIcon(UIHelper.icon("fas-check", 14));
        ModernButton rejectBtn = UIHelper.createDangerButton("Rejeitar");
        rejectBtn.setIcon(UIHelper.icon("fas-times", 14));
        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        newBtn.addActionListener(e -> openCreateVacationDialog());
        approveBtn.addActionListener(e -> decideVacation(true));
        rejectBtn.addActionListener(e -> decideVacation(false));
        exportBtn.addActionListener(e -> owner.exportTable("ferias", "Mapa de Férias", owner.vacationsTable));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(exportBtn);
        actions.add(rejectBtn);
        actions.add(approveBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Colaborador", "Início", "Fim", "Dias", "Ano Ref.", "Estado", "Decidido Por"};
        owner.vacationsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        owner.vacationsTable = new JTable(owner.vacationsModel);
        UIHelper.styleTable(owner.vacationsTable);
        owner.vacationsTable.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(owner.vacationsTable);
        UIHelper.styleScrollPane(scroll);

        JTextField vacSearch = TableFilter.searchField("Colaborador ou decisor…");
        JComboBox<String> vacEstado = TableFilter.combo("Todos os estados",
                "PENDING", "APPROVED", "REJECTED", "CANCELLED");
        JComboBox<String> vacPeriodo = TableFilter.periodCombo();
        TableFilter.install(owner.vacationsTable, vacSearch,
                java.util.List.of(new TableFilter.ColumnFilter(vacEstado, 6)),
                java.util.List.of(new TableFilter.PeriodFilter(vacPeriodo, 2)));
        JPanel vacBar = TableFilter.bar(vacSearch,
                TableFilter.label("Estado:"), vacEstado,
                TableFilter.label("Início:", "fas-calendar-alt"), vacPeriodo);
        vacBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(vacBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    void load() {
        UIHelper.loadAsync(owner, owner.hrApiClient::getAllVacations, this::applyVacations,
                error -> owner.showLoadError("férias", error));
    }

    private void applyVacations(List<VacationDTO> loaded) {
        owner.vacationsList = loaded;
        owner.vacationsModel.setRowCount(0);
        for (VacationDTO v : owner.vacationsList) {
            owner.vacationsModel.addRow(new Object[]{
                    v.id(), v.employeeName(),
                    v.startDate().format(DATE_FMT), v.endDate().format(DATE_FMT),
                    v.totalDays(), v.yearReference(), v.status(),
                    v.decisionBy() == null ? "-" : v.decisionBy()
            });
        }
        owner.refreshOverview();
    }

    private void openCreateVacationDialog() {
        if (owner.employeesList.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Cadastre colaboradores primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : owner.employeesList) empCombo.addItem(e.name() + " — " + e.department());

        DateField startField = new DateField(LocalDate.now());
        DateField endField = new DateField(LocalDate.now().plusDays(15));
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2100, 1));
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Data Início (yyyy-MM-dd):", startField,
                "Data Fim (yyyy-MM-dd):", endField,
                "Ano de Referência:", yearSpinner,
                "Observações:", notesField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Novo Pedido de Férias", "fas-umbrella-beach", "Marcação de férias do colaborador", form).showDialog();
        if (!confirmed) return;

        try {
            EmployeeDTO emp = owner.employeesList.get(empCombo.getSelectedIndex());
            CreateVacationRequest req = new CreateVacationRequest(
                    emp.id(),
                    startField.value(),
                    endField.value(),
                    (Integer) yearSpinner.getValue(),
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim()
            );
            UIHelper.runWithProgress(owner, "A submeter pedido de férias…", () -> owner.hrApiClient.submitVacation(req), ignored -> {
                JOptionPane.showMessageDialog(owner, "Pedido de férias submetido.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                load();
            }, owner::showActionError);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void decideVacation(boolean approve) {
        int row = TableFilter.selectedModelRow(owner.vacationsTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(owner, "Selecione um pedido na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        VacationDTO sel = owner.vacationsList.get(row);
        String reason = null;
        if (!approve) {
            reason = UIHelper.promptRequiredText("Rejeitar Pedido de Férias", "fas-times-circle",
                    "Colaborador: " + sel.employeeName(), "Motivo da rejeição:");
            if (reason == null) return;
        }
        String decisionReason = reason;
        UIHelper.runWithProgress(owner, approve ? "A aprovar férias…" : "A rejeitar férias…", () -> {
            owner.hrApiClient.decideVacation(sel.id(), approve, decisionReason);
            return null;
        }, ignored -> load(), owner::showActionError);
    }

}
