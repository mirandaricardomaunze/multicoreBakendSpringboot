package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.CreatePayrollDeductionRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.PayrollDeductionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Separador dos <b>descontos, adiantamentos e empréstimos</b>. Ver docs/RH_COMPLETO_SPEC.md §B6.
 *
 * <p>O que este ecrã torna visível é o <b>saldo em dívida</b>: até agora um adiantamento saía da
 * caixa e nunca voltava, porque nada o ligava ao recibo do período — e o recibo mostrava um único
 * "Outros Descontos" anónimo, que é a origem clássica da reclamação do trabalhador.
 *
 * <p>O saldo da coluna não está gravado em lado nenhum: apura-se das linhas que os recibos
 * levaram. Anular um recibo põe as prestações de volta em dívida sem ninguém ter de se lembrar.
 */
final class HRDeductionsPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HRPanel owner;

    private DefaultTableModel model;
    private JTable table;
    private List<PayrollDeductionDTO> deductions = List.of();

    HRDeductionsPanel(HRPanel owner) {
        this.owner = owner;
    }

    JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Descontos, Adiantamentos e Empréstimos"), BorderLayout.WEST);

        ModernButton advanceBtn = UIHelper.createPrimaryButton("Adiantamento");
        advanceBtn.setIcon(UIHelper.icon("fas-hand-holding-usd", 14));
        advanceBtn.addActionListener(e -> openDialog("ADIANTAMENTO"));
        ModernButton loanBtn = UIHelper.createPrimaryButton("Empréstimo");
        loanBtn.setIcon(UIHelper.icon("fas-file-invoice-dollar", 14));
        loanBtn.addActionListener(e -> openDialog("EMPRESTIMO"));
        ModernButton recurringBtn = UIHelper.createSecondaryButton("Desconto Recorrente");
        recurringBtn.setIcon(UIHelper.icon("fas-redo", 14));
        recurringBtn.addActionListener(e -> openDialog("RECORRENTE"));
        ModernButton stopBtn = UIHelper.createDangerButton("Desactivar");
        stopBtn.setIcon(UIHelper.icon("fas-ban", 14));
        stopBtn.addActionListener(e -> deactivate());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(stopBtn);
        actions.add(recurringBtn);
        actions.add(loanBtn);
        actions.add(advanceBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Colaborador", "Tipo", "Descrição", "Capital (MT)", "Prestação (MT)",
                "Descontado (MT)", "Em Dívida (MT)", "Início", "Estado"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        for (int column : new int[]{4, 5, 6, 7}) {
            table.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
        table.getColumnModel().getColumn(9).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        JTextField search = TableFilter.searchField("Colaborador ou descrição…");
        JComboBox<String> tipo = TableFilter.combo("Todos os tipos",
                "Adiantamento", "Empréstimo", "Desconto recorrente");
        JComboBox<String> estado = TableFilter.combo("Todos os estados", "Activo", "Liquidado", "Desactivado");
        TableFilter.install(table, search,
                List.of(new TableFilter.ColumnFilter(tipo, 2), new TableFilter.ColumnFilter(estado, 9)),
                List.of());
        JPanel bar = TableFilter.bar(search, TableFilter.label("Tipo:"), tipo,
                TableFilter.label("Estado:"), estado);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));

        card.add(bar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    void load() {
        UIHelper.loadAsync(owner, owner.hrApiClient::getDeductions, this::apply,
                error -> owner.showLoadError("descontos", error));
    }

    private void apply(List<PayrollDeductionDTO> loaded) {
        deductions = loaded;
        model.setRowCount(0);
        for (PayrollDeductionDTO d : deductions) {
            model.addRow(new Object[]{
                    d.id(), d.employeeName(), d.kindLabel(), d.description(),
                    d.principalAmount(), d.installmentAmount(),
                    d.appliedAmount(), d.outstandingAmount(),
                    d.startDate() == null ? "-" : d.startDate().format(DATE_FMT),
                    statusLabel(d)
            });
        }
    }

    /** Três estados que interessam a quem olha: ainda desconta, já pagou tudo, ou foi parado. */
    private String statusLabel(PayrollDeductionDTO d) {
        if (!d.active()) {
            return "Desactivado";
        }
        return d.settled() ? "Liquidado" : "Activo";
    }

    private void openDialog(String kind) {
        if (owner.employeesList.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Cadastre colaboradores primeiro.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean recurring = "RECORRENTE".equals(kind);
        boolean loan = "EMPRESTIMO".equals(kind);

        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : owner.employeesList) {
            empCombo.addItem(e.name() + " — " + e.department());
        }
        JTextField descField = new JTextField(defaultDescription(kind));
        UIHelper.styleTextField(descField);
        MoneyField principalField = new MoneyField("0.00");
        MoneyField installmentField = new MoneyField("0.00");
        JSpinner installmentsSpinner = new JSpinner(new SpinnerNumberModel(loan ? 6 : 1, 1, 120, 1));
        DateField startField = new DateField(LocalDate.now().withDayOfMonth(1));
        DateField endField = new DateField(null);
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = recurring
                ? UIHelper.createDialogForm(
                        "Colaborador:", empCombo,
                        "Descrição:", descField,
                        "Valor por recibo:", installmentField,
                        "Início:", startField,
                        "Fim (vazio = sem fim):", endField,
                        "Observações:", notesField)
                : UIHelper.createDialogForm(
                        "Colaborador:", empCombo,
                        "Descrição:", descField,
                        "Valor entregue:", principalField,
                        loan ? "Nº de prestações:" : "Prestações (1):", installmentsSpinner,
                        "Início do desconto:", startField,
                        "Observações:", notesField);

        String subtitle = recurring
                ? "Desconta enquanto estiver activo e dentro da vigência. Não sai dinheiro da caixa."
                : "O valor sai da tesouraria agora e volta pelos recibos seguintes.";
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, dialogTitle(kind),
                iconFor(kind), subtitle, form).showDialog();
        if (!confirmed) {
            return;
        }

        try {
            EmployeeDTO employee = owner.employeesList.get(empCombo.getSelectedIndex());
            CreatePayrollDeductionRequest request = new CreatePayrollDeductionRequest(
                    employee.id(), kind, descField.getText().trim(),
                    recurring ? null : principalField.value(),
                    recurring ? installmentField.value() : null,
                    recurring ? null : (Integer) installmentsSpinner.getValue(),
                    startField.value(),
                    recurring ? endField.value() : null,
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());
            UIHelper.runWithProgress(owner, "A registar…",
                    () -> owner.hrApiClient.createDeduction(request),
                    ignored -> {
                        JOptionPane.showMessageDialog(owner, confirmationOf(kind),
                                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                        load();
                    }, owner::showActionError);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(owner, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deactivate() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            JOptionPane.showMessageDialog(owner, "Selecione um desconto na tabela.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        PayrollDeductionDTO sel = deductions.get(row);
        BigDecimal outstanding = sel.outstandingAmount() == null ? BigDecimal.ZERO : sel.outstandingAmount();
        // O saldo por liquidar aparece na pergunta: desactivar um empréstimo a meio deixa dinheiro
        // por cobrar, e quem carrega no botão tem de ver isso antes de o fazer.
        int answer = JOptionPane.showConfirmDialog(owner, String.format(
                "Desactivar \"%s\" de %s?%s",
                sel.description(), sel.employeeName(),
                outstanding.signum() > 0
                        ? String.format("%n%nFicam %,.2f MT por liquidar, que deixam de ser descontados.",
                                outstanding)
                        : ""),
                "Desactivar Desconto", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        UIHelper.runWithProgress(owner, "A desactivar…", () -> {
            owner.hrApiClient.deactivateDeduction(sel.id());
            return null;
        }, ignored -> load(), owner::showActionError);
    }

    private String dialogTitle(String kind) {
        return switch (kind) {
            case "ADIANTAMENTO" -> "Novo Adiantamento";
            case "EMPRESTIMO" -> "Novo Empréstimo";
            default -> "Novo Desconto Recorrente";
        };
    }

    private String iconFor(String kind) {
        return switch (kind) {
            case "ADIANTAMENTO" -> "fas-hand-holding-usd";
            case "EMPRESTIMO" -> "fas-file-invoice-dollar";
            default -> "fas-redo";
        };
    }

    private String defaultDescription(String kind) {
        return switch (kind) {
            case "ADIANTAMENTO" -> "Adiantamento de " + LocalDate.now().getMonthValue()
                    + "/" + LocalDate.now().getYear();
            case "EMPRESTIMO" -> "Empréstimo";
            default -> "";
        };
    }

    private String confirmationOf(String kind) {
        return switch (kind) {
            case "ADIANTAMENTO" -> "Adiantamento registado. O valor saiu da tesouraria e será "
                    + "descontado no recibo do período.";
            case "EMPRESTIMO" -> "Empréstimo registado. O capital saiu da tesouraria e as prestações "
                    + "são descontadas nos recibos seguintes.";
            default -> "Desconto recorrente registado.";
        };
    }
}
