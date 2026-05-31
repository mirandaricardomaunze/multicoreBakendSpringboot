package com.phcpro.gui;

import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.hr.dto.AbsenceDTO;
import com.phcpro.modules.hr.dto.CreateAbsenceRequest;
import com.phcpro.modules.hr.dto.CreateExpenseClaimRequest;
import com.phcpro.modules.hr.dto.CreatePayslipRequest;
import com.phcpro.modules.hr.dto.CreateVacationRequest;
import com.phcpro.modules.hr.dto.EmployeeDTO;
import com.phcpro.modules.hr.dto.ExpenseClaimDTO;
import com.phcpro.modules.hr.dto.PayslipDTO;
import com.phcpro.modules.hr.dto.VacationDTO;
import com.phcpro.modules.hr.service.HRService;
import com.phcpro.modules.printing.PayslipPrintService;
import com.phcpro.modules.printing.PdfFileSaver;
import com.phcpro.modules.printing.TablePdfExporter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HRPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] ABSENCE_TYPES = {"JUSTIFIED", "UNJUSTIFIED", "SICK", "MATERNITY", "OTHER"};

    private final HRService hrService;
    private final PayslipPrintService payslipPrintService;

    private List<EmployeeDTO> employeesList = new ArrayList<>();
    private List<PayslipDTO> payslipsList = new ArrayList<>();
    private List<AbsenceDTO> absencesList = new ArrayList<>();
    private List<VacationDTO> vacationsList = new ArrayList<>();

    // Employees tab
    private DefaultTableModel employeesModel;
    private JTable employeesTable;

    // Payslips tab
    private DefaultTableModel payslipsModel;
    private JTable payslipsTable;

    // Absences tab
    private DefaultTableModel absencesModel;
    private JTable absencesTable;

    // Vacations tab
    private DefaultTableModel vacationsModel;
    private JTable vacationsTable;

    // Expenses tab
    private DefaultTableModel expensesModel;
    private JTable expensesTable;
    private JComboBox<String> expenseEmployeeCombo;
    private JComboBox<String> expenseCategoryCombo;
    private JTextField expenseAmountField;
    private JTextField expenseDescField;

    public HRPanel(HRService hrService, PayslipPrintService payslipPrintService) {
        this.hrService = hrService;
        this.payslipPrintService = payslipPrintService;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(UIHelper.createHeading("Recursos Humanos"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPane(tabs);

        tabs.addTab("Colaboradores",     UIHelper.icon("fas-users", 16, UIHelper.TEXT_LIGHT),         buildEmployeesTab());
        tabs.addTab("Recibos de Salário", UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.TEXT_LIGHT), buildPayslipsTab());
        tabs.addTab("Faltas",            UIHelper.icon("fas-user-times", 16, UIHelper.TEXT_LIGHT),    buildAbsencesTab());
        tabs.addTab("Férias",            UIHelper.icon("fas-umbrella-beach", 16, UIHelper.TEXT_LIGHT),buildVacationsTab());
        tabs.addTab("Notas de Despesas", UIHelper.icon("fas-receipt", 16, UIHelper.TEXT_LIGHT),       buildExpensesTab());

        add(tabs, BorderLayout.CENTER);

        refreshData();
    }

    public void onPanelSelected() {
        refreshData();
    }

    public void refreshData() {
        loadEmployees();
        loadPayslips();
        loadAbsences();
        loadVacations();
        loadExpenses();
    }

    // ─── Employees tab ────────────────────────────────────────────────────────

    private JPanel buildEmployeesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Quadro de Colaboradores"), BorderLayout.WEST);

        ModernButton exportBtn = new ModernButton("Exportar PDF", new Color(99, 102, 241), new Color(129, 140, 248));
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.addActionListener(e -> exportTable("colaboradores", "Colaboradores", employeesTable));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(exportBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Nome", "Email", "Departamento", "Cargo", "Salário Base"};
        employeesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        employeesTable = new JTable(employeesModel);
        UIHelper.styleTable(employeesTable);
        JScrollPane scroll = new JScrollPane(employeesTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadEmployees() {
        employeesList = hrService.getAllEmployees();
        employeesModel.setRowCount(0);
        for (EmployeeDTO e : employeesList) {
            employeesModel.addRow(new Object[]{
                    e.id(), e.name(), e.email(), e.department(), e.role(),
                    String.format("%,.2f MT", e.baseSalary())
            });
        }
    }

    // ─── Payslips tab ─────────────────────────────────────────────────────────

    private JPanel buildPayslipsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Recibos de Salário"), BorderLayout.WEST);

        ModernButton newBtn = new ModernButton("Gerar Recibo", new Color(16, 185, 129), new Color(52, 211, 153));
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton payBtn = new ModernButton("Marcar Pago", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE.brighter());
        payBtn.setIcon(UIHelper.icon("fas-check", 14));
        ModernButton printBtn = new ModernButton("Imprimir PDF", new Color(99, 102, 241), new Color(129, 140, 248));
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        ModernButton exportBtn = new ModernButton("Exportar Lista", new Color(107, 114, 128), new Color(156, 163, 175));
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        newBtn.addActionListener(e -> openCreatePayslipDialog());
        payBtn.addActionListener(e -> markSelectedPayslipPaid());
        printBtn.addActionListener(e -> printSelectedPayslip());
        exportBtn.addActionListener(e -> exportTable("recibos-salario", "Recibos de Salário", payslipsTable));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(exportBtn);
        actions.add(printBtn);
        actions.add(payBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Nº Recibo", "Colaborador", "Período", "Bruto", "Descontos", "Líquido", "Estado", "Data Pagamento"};
        payslipsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        payslipsTable = new JTable(payslipsModel);
        UIHelper.styleTable(payslipsTable);
        JScrollPane scroll = new JScrollPane(payslipsTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadPayslips() {
        payslipsList = hrService.getAllPayslips();
        payslipsModel.setRowCount(0);
        for (PayslipDTO p : payslipsList) {
            payslipsModel.addRow(new Object[]{
                    p.payslipNumber(),
                    p.employeeName(),
                    String.format("%02d/%d", p.month(), p.year()),
                    String.format("%,.2f MT", p.grossPay()),
                    String.format("%,.2f MT", p.totalDeductions()),
                    String.format("%,.2f MT", p.netPay()),
                    p.status(),
                    p.paymentDate() != null ? p.paymentDate().format(DATE_FMT) : "-"
            });
        }
    }

    private void openCreatePayslipDialog() {
        if (employeesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre colaboradores primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : employeesList) empCombo.addItem(e.name() + " — " + e.department());

        LocalDate today = LocalDate.now();
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(today.getYear(), 2000, 2100, 1));
        JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(today.getMonthValue(), 1, 12, 1));

        JTextField allowancesField = new JTextField("0");
        JTextField overtimeField = new JTextField("0");
        JTextField irpsField = new JTextField("0");
        JTextField inssField = new JTextField("0");
        JTextField otherField = new JTextField("0");
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(allowancesField);
        UIHelper.styleTextField(overtimeField);
        UIHelper.styleTextField(irpsField);
        UIHelper.styleTextField(inssField);
        UIHelper.styleTextField(otherField);
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Ano:", yearSpinner,
                "Mês:", monthSpinner,
                "Subsídios / Abonos (MT):", allowancesField,
                "Horas Extras (MT):", overtimeField,
                "IRPS (MT):", irpsField,
                "INSS (MT):", inssField,
                "Outros Descontos (MT):", otherField,
                "Observações:", notesField
        );

        int opt = JOptionPane.showConfirmDialog(this, UIHelper.makeDialogScrollable(form),
                "Gerar Recibo de Salário", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

        try {
            EmployeeDTO emp = employeesList.get(empCombo.getSelectedIndex());
            CreatePayslipRequest req = new CreatePayslipRequest(
                    emp.id(),
                    (Integer) yearSpinner.getValue(),
                    (Integer) monthSpinner.getValue(),
                    new BigDecimal(allowancesField.getText().trim()),
                    new BigDecimal(overtimeField.getText().trim()),
                    new BigDecimal(irpsField.getText().trim()),
                    new BigDecimal(inssField.getText().trim()),
                    new BigDecimal(otherField.getText().trim()),
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim()
            );
            PayslipDTO created = hrService.createPayslip(req);
            loadPayslips();
            int print = JOptionPane.showConfirmDialog(this,
                    "Recibo " + created.payslipNumber() + " gerado. Deseja imprimir agora?",
                    "Sucesso", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (print == JOptionPane.YES_OPTION) {
                printPayslip(created.id(), created.payslipNumber());
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valores numéricos inválidos.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void markSelectedPayslipPaid() {
        PayslipDTO sel = selectedPayslip();
        if (sel == null) return;
        try {
            hrService.markPayslipPaid(sel.id());
            JOptionPane.showMessageDialog(this, "Recibo marcado como pago.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadPayslips();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void printSelectedPayslip() {
        PayslipDTO sel = selectedPayslip();
        if (sel == null) return;
        printPayslip(sel.id(), sel.payslipNumber());
    }

    private void printPayslip(Long id, String number) {
        try {
            byte[] pdf = payslipPrintService.render(id);
            PdfFileSaver.saveAndOpen(pdf, "recibo-salario-" + number);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private PayslipDTO selectedPayslip() {
        int row = payslipsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um recibo na tabela primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return payslipsList.get(row);
    }

    // ─── Absences tab ─────────────────────────────────────────────────────────

    private JPanel buildAbsencesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Registo de Faltas"), BorderLayout.WEST);

        ModernButton newBtn = new ModernButton("Registar Falta", new Color(245, 158, 11), new Color(251, 191, 36));
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton deleteBtn = new ModernButton("Eliminar", UIHelper.REJECTED_RED, UIHelper.REJECTED_RED.brighter());
        deleteBtn.setIcon(UIHelper.icon("fas-trash", 14));
        ModernButton exportBtn = new ModernButton("Exportar PDF", new Color(99, 102, 241), new Color(129, 140, 248));
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        newBtn.addActionListener(e -> openCreateAbsenceDialog());
        deleteBtn.addActionListener(e -> deleteSelectedAbsence());
        exportBtn.addActionListener(e -> exportTable("faltas", "Mapa de Faltas", absencesTable));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(exportBtn);
        actions.add(deleteBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Colaborador", "Tipo", "Início", "Fim", "Dias", "Justificada", "Motivo"};
        absencesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        absencesTable = new JTable(absencesModel);
        UIHelper.styleTable(absencesTable);
        JScrollPane scroll = new JScrollPane(absencesTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadAbsences() {
        absencesList = hrService.getAllAbsences();
        absencesModel.setRowCount(0);
        for (AbsenceDTO a : absencesList) {
            absencesModel.addRow(new Object[]{
                    a.id(), a.employeeName(), a.absenceType(),
                    a.startDate().format(DATE_FMT), a.endDate().format(DATE_FMT),
                    a.totalDays(),
                    a.hasSupportingDocument() ? "Sim" : "Não",
                    a.reason() == null ? "" : a.reason()
            });
        }
    }

    private void openCreateAbsenceDialog() {
        if (employeesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre colaboradores primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : employeesList) empCombo.addItem(e.name() + " — " + e.department());

        JComboBox<String> typeCombo = new JComboBox<>(ABSENCE_TYPES);
        UIHelper.styleComboBox(typeCombo);

        JTextField startField = new JTextField(LocalDate.now().toString());
        JTextField endField = new JTextField(LocalDate.now().toString());
        JTextField reasonField = new JTextField();
        JCheckBox docCheck = new JCheckBox("Possui documento de justificação");
        docCheck.setForeground(UIHelper.TEXT_LIGHT);
        docCheck.setOpaque(false);
        UIHelper.styleTextField(startField);
        UIHelper.styleTextField(endField);
        UIHelper.styleTextField(reasonField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Tipo de Falta:", typeCombo,
                "Data Início (yyyy-MM-dd):", startField,
                "Data Fim (yyyy-MM-dd):", endField,
                "Motivo:", reasonField,
                "Documento:", docCheck
        );

        int opt = JOptionPane.showConfirmDialog(this, UIHelper.makeDialogScrollable(form),
                "Registar Falta", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

        try {
            EmployeeDTO emp = employeesList.get(empCombo.getSelectedIndex());
            CreateAbsenceRequest req = new CreateAbsenceRequest(
                    emp.id(),
                    (String) typeCombo.getSelectedItem(),
                    LocalDate.parse(startField.getText().trim()),
                    LocalDate.parse(endField.getText().trim()),
                    reasonField.getText().trim().isEmpty() ? null : reasonField.getText().trim(),
                    docCheck.isSelected()
            );
            hrService.recordAbsence(req);
            JOptionPane.showMessageDialog(this, "Falta registada.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadAbsences();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedAbsence() {
        int row = absencesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma falta na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        AbsenceDTO sel = absencesList.get(row);
        int ok = JOptionPane.showConfirmDialog(this, "Eliminar a falta selecionada?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            hrService.deleteAbsence(sel.id());
            loadAbsences();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Vacations tab ────────────────────────────────────────────────────────

    private JPanel buildVacationsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Pedidos de Férias"), BorderLayout.WEST);

        ModernButton newBtn = new ModernButton("Novo Pedido", new Color(14, 165, 233), new Color(56, 189, 248));
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton approveBtn = new ModernButton("Aprovar", UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN.brighter());
        approveBtn.setIcon(UIHelper.icon("fas-check", 14));
        ModernButton rejectBtn = new ModernButton("Rejeitar", UIHelper.REJECTED_RED, UIHelper.REJECTED_RED.brighter());
        rejectBtn.setIcon(UIHelper.icon("fas-times", 14));
        ModernButton exportBtn = new ModernButton("Exportar PDF", new Color(99, 102, 241), new Color(129, 140, 248));
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        newBtn.addActionListener(e -> openCreateVacationDialog());
        approveBtn.addActionListener(e -> decideVacation(true));
        rejectBtn.addActionListener(e -> decideVacation(false));
        exportBtn.addActionListener(e -> exportTable("ferias", "Mapa de Férias", vacationsTable));
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
        vacationsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        vacationsTable = new JTable(vacationsModel);
        UIHelper.styleTable(vacationsTable);
        JScrollPane scroll = new JScrollPane(vacationsTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadVacations() {
        vacationsList = hrService.getAllVacations();
        vacationsModel.setRowCount(0);
        for (VacationDTO v : vacationsList) {
            vacationsModel.addRow(new Object[]{
                    v.id(), v.employeeName(),
                    v.startDate().format(DATE_FMT), v.endDate().format(DATE_FMT),
                    v.totalDays(), v.yearReference(), v.status(),
                    v.decisionBy() == null ? "-" : v.decisionBy()
            });
        }
    }

    private void openCreateVacationDialog() {
        if (employeesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre colaboradores primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComboBox<String> empCombo = new JComboBox<>();
        UIHelper.styleComboBox(empCombo);
        for (EmployeeDTO e : employeesList) empCombo.addItem(e.name() + " — " + e.department());

        JTextField startField = new JTextField(LocalDate.now().toString());
        JTextField endField = new JTextField(LocalDate.now().plusDays(15).toString());
        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2100, 1));
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(startField);
        UIHelper.styleTextField(endField);
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Colaborador:", empCombo,
                "Data Início (yyyy-MM-dd):", startField,
                "Data Fim (yyyy-MM-dd):", endField,
                "Ano de Referência:", yearSpinner,
                "Observações:", notesField
        );

        int opt = JOptionPane.showConfirmDialog(this, UIHelper.makeDialogScrollable(form),
                "Novo Pedido de Férias", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

        try {
            EmployeeDTO emp = employeesList.get(empCombo.getSelectedIndex());
            CreateVacationRequest req = new CreateVacationRequest(
                    emp.id(),
                    LocalDate.parse(startField.getText().trim()),
                    LocalDate.parse(endField.getText().trim()),
                    (Integer) yearSpinner.getValue(),
                    notesField.getText().trim().isEmpty() ? null : notesField.getText().trim()
            );
            hrService.submitVacation(req);
            JOptionPane.showMessageDialog(this, "Pedido de férias submetido.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            loadVacations();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void decideVacation(boolean approve) {
        int row = vacationsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um pedido na tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        VacationDTO sel = vacationsList.get(row);
        String reason = null;
        if (!approve) {
            reason = JOptionPane.showInputDialog(this, "Motivo da rejeição:", "Rejeitar Pedido", JOptionPane.QUESTION_MESSAGE);
            if (reason == null) return;
        }
        try {
            String user = com.phcpro.architecture.security.CurrentUserContext.getUsername();
            hrService.decideVacation(sel.id(), approve, user == null ? "SYSTEM" : user, reason);
            loadVacations();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Expenses tab ─────────────────────────────────────────────────────────

    private JPanel buildExpensesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        // Top: submit form
        ModernPanel formCard = new ModernPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.insets = new Insets(4, 6, 4, 6);

        JLabel l1 = new JLabel("Colaborador:");
        JLabel l2 = new JLabel("Categoria:");
        JLabel l3 = new JLabel("Valor (MT):");
        JLabel l4 = new JLabel("Descrição:");
        for (JLabel l : new JLabel[]{l1, l2, l3, l4}) l.setForeground(UIHelper.TEXT_MUTED);

        expenseEmployeeCombo = new JComboBox<>();
        expenseCategoryCombo = new JComboBox<>(new String[]{
                "MEALS (Alimentação)", "TRAVEL (Deslocações)", "LODGING (Alojamento)", "OTHER (Outros)"
        });
        expenseAmountField = new JTextField();
        expenseDescField = new JTextField();
        UIHelper.styleComboBox(expenseEmployeeCombo);
        UIHelper.styleComboBox(expenseCategoryCombo);
        UIHelper.styleTextField(expenseAmountField);
        UIHelper.styleTextField(expenseDescField);

        g.gridx = 0; g.gridy = 0; formCard.add(l1, g);
        g.gridx = 1;              formCard.add(l2, g);
        g.gridx = 0; g.gridy = 1; formCard.add(expenseEmployeeCombo, g);
        g.gridx = 1;              formCard.add(expenseCategoryCombo, g);
        g.gridx = 0; g.gridy = 2; formCard.add(l3, g);
        g.gridx = 1;              formCard.add(l4, g);
        g.gridx = 0; g.gridy = 3; formCard.add(expenseAmountField, g);
        g.gridx = 1;              formCard.add(expenseDescField, g);

        ModernButton submitBtn = new ModernButton("Submeter Despesa");
        submitBtn.setIcon(UIHelper.icon("fas-paper-plane", 14));
        submitBtn.setGradient(UIHelper.ACCENT, UIHelper.ACCENT.darker());
        submitBtn.addActionListener(e -> submitExpense());
        g.gridx = 0; g.gridy = 4; g.gridwidth = 2;
        g.insets = new Insets(14, 6, 4, 6);
        formCard.add(submitBtn, g);

        // Bottom: list with export
        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.setOpaque(false);
        listHeader.add(UIHelper.createSubheading("Histórico de Despesas"), BorderLayout.WEST);
        ModernButton exportBtn = new ModernButton("Exportar PDF", new Color(99, 102, 241), new Color(129, 140, 248));
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.addActionListener(e -> exportTable("despesas", "Notas de Despesas", expensesTable));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(exportBtn);
        listHeader.add(actions, BorderLayout.EAST);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));
        String[] cols = {"Colaborador", "Valor", "Categoria", "Estado", "Motivo Rejeição"};
        expensesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        expensesTable = new JTable(expensesModel);
        UIHelper.styleTable(expensesTable);
        JScrollPane scroll = new JScrollPane(expensesTable);
        UIHelper.styleScrollPane(scroll);
        listCard.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(listHeader, BorderLayout.NORTH);
        bottom.add(listCard, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(UIHelper.createSubheading("Submeter Nova Despesa"), BorderLayout.NORTH);
        top.add(formCard, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.35);
        split.setBorder(null);
        split.setOpaque(false);
        tab.add(split, BorderLayout.CENTER);
        return tab;
    }

    private void loadExpenses() {
        expenseEmployeeCombo.removeAllItems();
        for (EmployeeDTO e : employeesList) expenseEmployeeCombo.addItem(e.name() + " (" + e.department() + ")");

        expensesModel.setRowCount(0);
        List<ExpenseClaimDTO> claims = hrService.getAllExpenses();
        for (ExpenseClaimDTO c : claims) {
            expensesModel.addRow(new Object[]{
                    c.employeeName(),
                    String.format("%,.2f MT", c.amount()),
                    c.category(),
                    c.status().name(),
                    c.rejectionReason() == null ? "" : c.rejectionReason()
            });
        }
    }

    private void submitExpense() {
        if (employeesList.isEmpty()) return;
        int empIdx = expenseEmployeeCombo.getSelectedIndex();
        if (empIdx < 0) return;
        EmployeeDTO emp = employeesList.get(empIdx);
        BigDecimal amount;
        try {
            amount = new BigDecimal(expenseAmountField.getText().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Valor inválido.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String cat = expenseCategoryCombo.getSelectedItem().toString().split(" ")[0];
        String desc = expenseDescField.getText().trim();
        if (desc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Indique uma descrição.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            hrService.submitExpense(new CreateExpenseClaimRequest(emp.id(), amount, cat, desc));
            JOptionPane.showMessageDialog(this, "Despesa submetida.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            expenseAmountField.setText("");
            expenseDescField.setText("");
            loadExpenses();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Shared export helper ─────────────────────────────────────────────────

    private void exportTable(String baseName, String title, JTable table) {
        if (table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Nada para exportar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            com.phcpro.modules.company.model.Company company = resolveCompany();
            byte[] pdf = TablePdfExporter.renderFromSwing(company, title, table);
            PdfFileSaver.saveAndOpen(pdf, baseName + "-export");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private com.phcpro.modules.company.model.Company resolveCompany() {
        com.phcpro.modules.company.model.Company c = new com.phcpro.modules.company.model.Company();
        c.setId(com.phcpro.architecture.security.CurrentUserContext.getCurrentCompanyId());
        return c;
    }
}
