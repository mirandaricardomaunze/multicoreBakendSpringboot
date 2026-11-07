package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableQuickPeekController;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.TableExportAction;
import mz.multicore.erp.gui.components.SensitivePrintCoordinator;
import mz.multicore.erp.modules.hr.dto.AbsenceDTO;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.ExpenseClaimDTO;
import mz.multicore.erp.modules.hr.dto.PayslipDTO;
import mz.multicore.erp.modules.hr.dto.VacationDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel principal de Recursos Humanos e Processamento Salarial (RH).
 * Refatorado segundo a arquitetura de decomposição (SPEC-HR-PANEL-DECOMPOSITION-001).
 * Diálogos modais, visão geral e sub-painéis desacoplados.
 */
public class HRPanel extends JPanel {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final String[] ABSENCE_TYPES = {"JUSTIFIED", "UNJUSTIFIED", "SICK", "MATERNITY", "OTHER"};

    final HRApiClient hrApiClient;
    final PrintApiClient printApiClient;
    private final HROverviewTabPanel overviewTabPanel;
    private final HRExpensesPanel expensesPanel;
    private final HRContractsPanel contractsPanel;
    private final HRVacationsPanel vacationsPanel;
    private final HRTimeSheetPanel timeSheetPanel;
    private final HRLiabilitiesPanel liabilitiesPanel;
    private final HRDeductionsPanel deductionsPanel;
    private final HRTerminationsPanel terminationsPanel;
    private final HRPayrollActions payrollActions;
    private final HREmployeeActions employeeActions;
    private final HRBonusActions bonusActions;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    List<EmployeeDTO> employeesList = new ArrayList<>();
    List<PayslipDTO> payslipsList = new ArrayList<>();
    List<AbsenceDTO> absencesList = new ArrayList<>();
    List<VacationDTO> vacationsList = new ArrayList<>();
    List<ExpenseClaimDTO> expensesList = new ArrayList<>();

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
    DefaultTableModel vacationsModel;
    JTable vacationsTable;

    // Expenses tab
    DefaultTableModel expensesModel;
    JTable expensesTable;

    public HRPanel(HRApiClient hrApiClient, PrintApiClient printApiClient) {
        this.hrApiClient = hrApiClient;
        this.printApiClient = printApiClient;
        this.overviewTabPanel = new HROverviewTabPanel(this);
        this.expensesPanel = new HRExpensesPanel(this);
        this.contractsPanel = new HRContractsPanel(this);
        this.vacationsPanel = new HRVacationsPanel(this);
        this.timeSheetPanel = new HRTimeSheetPanel(this);
        this.liabilitiesPanel = new HRLiabilitiesPanel(this);
        this.deductionsPanel = new HRDeductionsPanel(this);
        this.terminationsPanel = new HRTerminationsPanel(this);
        this.payrollActions = new HRPayrollActions(this, this::selectedPayslip, this::loadPayslips);
        this.employeeActions = new HREmployeeActions(this, this::selectedEmployee,
                this::selectedAbsence, this::loadAbsences);
        this.bonusActions = new HRBonusActions(this);

        setLayout(new BorderLayout(0, 8));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel north = new JPanel(); north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        feedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(feedback); add(north, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);

        tabs.addTab("Visão Geral",        UIHelper.icon("fas-chart-pie", 16, UIHelper.ACCENT_BLUE),     overviewTabPanel.buildPanel());
        tabs.addTab("Colaboradores",     UIHelper.icon("fas-users", 16, UIHelper.MODULE_HR),         buildEmployeesTab());
        tabs.addTab("Contratos",         UIHelper.icon("fas-file-signature", 16, UIHelper.ACCENT_SKY), contractsPanel.buildPanel());
        tabs.addTab("Recibos de Salário", UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.APPROVED_GREEN), buildPayslipsTab());
        tabs.addTab("Faltas",            UIHelper.icon("fas-user-times", 16, UIHelper.PENDING_YELLOW),    buildAbsencesTab());
        tabs.addTab("Ponto",             UIHelper.icon("fas-clock", 16, UIHelper.ACCENT_CYAN),        timeSheetPanel.buildPanel());
        tabs.addTab("Férias",            UIHelper.icon("fas-umbrella-beach", 16, UIHelper.ACCENT_ORANGE),buildVacationsTab());
        tabs.addTab("Descontos",         UIHelper.icon("fas-hand-holding-usd", 16, UIHelper.REJECTED_RED), deductionsPanel.buildPanel());
        tabs.addTab("Retenções",         UIHelper.icon("fas-landmark", 16, UIHelper.MODULE_FISCAL),     liabilitiesPanel.buildPanel());
        tabs.addTab("Cessações",         UIHelper.icon("fas-user-slash", 16, UIHelper.REJECTED_RED),   terminationsPanel.buildPanel());
        tabs.addTab("Despesas",          UIHelper.icon("fas-receipt", 16, UIHelper.ACCENT_PINK),       buildExpensesTab());

        add(tabs, BorderLayout.CENTER);
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
        deductionsPanel.load();
        liabilitiesPanel.load();
        terminationsPanel.load();
    }

    void refreshOverview() {
        if (overviewTabPanel != null) overviewTabPanel.refresh();
    }

    // ─── Employees tab ────────────────────────────────────────────────────────

    private JPanel buildEmployeesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton newBtn = UIHelper.createSuccessButton("Novo Colaborador");
        newBtn.setIcon(UIHelper.icon("fas-user-plus", 14));
        newBtn.addActionListener(e -> HREmployeeFormDialog.show(this, null));

        ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Editar Colaborador", UIHelper.icon("fas-edit", 14), this::editSelectedEmployee)
                .addAction("Evolução Salarial", UIHelper.icon("fas-chart-line", 14),
                        employeeActions::openSalaryHistory)
                .addAction("Documentos", UIHelper.icon("fas-id-card", 14), employeeActions::openDocuments)
                .addAction("Saúde Ocupacional", UIHelper.icon("fas-heartbeat", 14),
                        employeeActions::openOccupationalHealth)
                .addAction("Alterar Estado", UIHelper.icon("fas-user-shield", 14), this::changeSelectedEmployeeStatus);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Nº", "Nome", "Email", "Telefone", "Departamento", "Cargo", "Admissão", "Estado", "Salário Base"};
        employeesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        employeesTable = new JTable(employeesModel);
        UIHelper.styleTable(employeesTable);
        UIHelper.ensureHeadersFit(employeesTable);
        employeesTable.setToolTipText("Selecione uma linha e use Ver Perfil para abrir a ficha completa");
        employeesTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.role());
        employeesTable.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.status());
        employeesTable.getColumnModel().getColumn(8).setCellRenderer(TableCellRenderers.money());
        JScrollPane scroll = new JScrollPane(employeesTable);
        UIHelper.styleScrollPane(scroll);

        TableQuickPeekController peekController = TableQuickPeekController.install(employeesTable, card, (peek, mr) -> {
            int row = mr; // o controlador já entrega a linha do modelo
            peek.setHeaderIcon("fas-user-tie", UIHelper.ACCENT_BLUE);
            peek.setTitle(String.valueOf(employeesModel.getValueAt(row, 1)));
            peek.setSubtitle("Nº " + employeesModel.getValueAt(row, 0) + " • " + employeesModel.getValueAt(row, 5));
            peek.setStatus(String.valueOf(employeesModel.getValueAt(row, 7)), UIHelper.ACCENT_BLUE);
            java.util.List<mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem> items = new java.util.ArrayList<>();
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Email", peek.cell(employeesModel.getValueAt(row, 2), 2), false));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Telefone", peek.cell(employeesModel.getValueAt(row, 3), 3), false));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Departamento", peek.cell(employeesModel.getValueAt(row, 4), 4), false));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Admissão", peek.cell(employeesModel.getValueAt(row, 6), 6), false));
            items.add(new mz.multicore.erp.gui.components.QuickPeekPanel.PeekItem("Salário Base", peek.cell(employeesModel.getValueAt(row, 8), 8), true));
            peek.setItems(items);
            peek.getOpenFullButton().setText("Ver Perfil Completo");
            peek.setOnOpenFullAction(v -> openSelectedEmployeeProfile());
        });

        JTextField empSearch = TableFilter.searchField("Nome, email, departamento ou cargo…");
        JComboBox<String> empEstado = TableFilter.combo("Todos os estados",
                "Activo", "Suspenso", "Rescindido");
        TableFilter.install(employeesTable, empSearch,
                new TableFilter.ColumnFilter(empEstado, 7));
        ModernButton refreshEmployeesBtn = UIHelper.createRefreshButton(this::loadEmployees);
        JPanel empBar = TableFilter.bar(empSearch, TableFilter.label("Estado:"), empEstado,
                refreshEmployeesBtn);
        empBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        ActionMenuButton consultationBtn = UIHelper.createActionMenuButton("Consulta")
                .addAction("Alternar Detalhes", UIHelper.icon("fas-columns", 14), peekController::toggle)
                .addAction("Ver Perfil", UIHelper.icon("fas-id-card", 14), this::openSelectedEmployeeProfile);
        card.add(UIHelper.tableCardTop("Quadro de Colaboradores", empBar,
                consultationBtn, moreBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    void loadEmployees() {
        UIHelper.loadAsync(this, hrApiClient::getAllEmployees, this::applyEmployees,
                error -> showLoadError("colaboradores", error));
    }

    private void applyEmployees(List<EmployeeDTO> loaded) {
        employeesList = loaded;
        employeesModel.setRowCount(0);
        for (EmployeeDTO e : employeesList) {
            employeesModel.addRow(new Object[]{
                    e.employeeNumber(), e.name(), e.email(), e.phone() == null ? "-" : e.phone(),
                    e.department(), e.role(),
                    e.hireDate() == null ? "-" : e.hireDate().format(DATE_FMT),
                    e.status(),
                    e.baseSalary()
            });
        }
        refreshOverview();
    }

    private void editSelectedEmployee() {
        EmployeeDTO employee = selectedEmployee();
        if (employee != null) HREmployeeFormDialog.show(this, employee);
    }

    private void changeSelectedEmployeeStatus() {
        EmployeeDTO employee = selectedEmployee();
        if (employee != null) HREmployeeFormDialog.changeStatus(this, employee);
    }

    private void openSelectedEmployeeProfile() {
        EmployeeDTO employee = selectedEmployee();
        if (employee == null) return;
        UIHelper.runWithProgress(this, "A carregar perfil do trabalhador…",
                () -> hrApiClient.getOccupationalHealthSummary(employee.id()),
                health -> HREmployeeProfileDialog.show(SwingUtilities.getWindowAncestor(this), employee,
                        payslipsList, absencesList, vacationsList, health),
                this::showActionError);
    }

    private EmployeeDTO selectedEmployee() {
        int row = employeesTable.getSelectedRow();
        if (row < 0) {
            showNotice(FeedbackType.WARNING, "Seleccione um colaborador",
                    "Escolha um colaborador na tabela para continuar.");
            return null;
        }
        return employeesList.get(employeesTable.convertRowIndexToModel(row));
    }

    // ─── Payslips tab ─────────────────────────────────────────────────────────

    private JPanel buildPayslipsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton newBtn = UIHelper.createSuccessButton("Gerar Recibo");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> HRPayslipFormDialog.openCreateDialog(this));

        ModernButton refreshPayslipsBtn = UIHelper.createRefreshButton(this::loadPayslips);

        ActionMenuButton documentsBtn = UIHelper.createActionMenuButton("Documentos & Processo")
                .addAction("Imprimir PDF", UIHelper.icon("fas-print", 14), this::printSelectedPayslip)
                .addAction("Exportar Lista", UIHelper.icon("fas-file-pdf", 14),
                        () -> exportTable("recibos-salario", "Recibos de Salário", payslipsTable))
                .addAction("Ficheiro de Pagamento", UIHelper.icon("fas-university", 14), payrollActions::bankPaymentFile)
                .addAction("Processar Mês", UIHelper.icon("fas-calculator", 14, UIHelper.ACCENT_BLUE), () -> HRPayslipFormDialog.processMonthlyPayroll(this));

        ActionMenuButton actionsBtn = UIHelper.createActionMenuButton("Folha Salarial")
                .addAction("Aprovar Recibo", UIHelper.icon("fas-clipboard-check", 14), payrollActions::approveSelected)
                .addAction("Marcar Pago", UIHelper.icon("fas-check", 14), this::markSelectedPayslipPaid)
                .addAction("13.º Mês", UIHelper.icon("fas-gift", 14), bonusActions::openThirteenthMonth)
                .addAction("Fechar Mês", UIHelper.icon("fas-lock", 14), payrollActions::closeMonth)
                .addAction("Reabrir Mês", UIHelper.icon("fas-lock-open", 14), payrollActions::reopenMonth);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Nº Recibo", "Colaborador", "Período", "Bruto", "Descontos", "Líquido", "Estado", "Data Pagamento"};
        payslipsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        payslipsTable = new JTable(payslipsModel);
        UIHelper.styleTable(payslipsTable);
        for (int column : new int[]{3, 4, 5}) {
            payslipsTable.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
        payslipsTable.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(payslipsTable);
        UIHelper.styleScrollPane(scroll);

        JTextField psSearch = TableFilter.searchField("Nº recibo, colaborador ou período…");
        JComboBox<String> psEstado = TableFilter.combo("Todos os estados", "Rascunho", "Aprovado", "Paga", "Cancelado");
        JComboBox<String> psPeriodo = TableFilter.periodCombo();
        TableFilter.install(payslipsTable, psSearch,
                java.util.List.of(new TableFilter.ColumnFilter(psEstado, 6)),
                java.util.List.of(new TableFilter.PeriodFilter(psPeriodo, 7)));
        JPanel psBar = TableFilter.toolbar(
                new JComponent[]{psSearch, TableFilter.label("Estado:"), psEstado, TableFilter.label("Data pag.:", "fas-calendar-alt"), psPeriodo, refreshPayslipsBtn},
                null
        );
        psBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(UIHelper.tableCardTop("Recibos de Salário", psBar,
                documentsBtn, actionsBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    void loadPayslips() {
        UIHelper.loadAsync(this, hrApiClient::getAllPayslips, this::applyPayslips,
                error -> showLoadError("recibos de salário", error));
    }

    private void applyPayslips(List<PayslipDTO> loaded) {
        payslipsList = loaded;
        payslipsModel.setRowCount(0);
        for (PayslipDTO p : payslipsList) {
            payslipsModel.addRow(new Object[]{
                    p.payslipNumber(),
                    p.employeeName(),
                    String.format("%02d/%d", p.month(), p.year()),
                    p.grossPay(),
                    p.totalDeductions(),
                    p.netPay(),
                    p.status(),
                    p.paymentDate() != null ? p.paymentDate().format(DATE_FMT) : "-"
            });
        }
        refreshOverview();
    }

    private void markSelectedPayslipPaid() {
        PayslipDTO sel = selectedPayslip();
        if (sel == null) return;
        UIHelper.runWithProgress(this, "A actualizar recibo…", () -> {
            hrApiClient.markPayslipPaid(sel.id());
            return null;
        }, ignored -> {
            showSuccess("Recibo marcado como pago.");
            loadPayslips();
        }, this::showActionError);
    }

    private void printSelectedPayslip() {
        PayslipDTO sel = selectedPayslip();
        if (sel == null) return;
        printPayslip(sel.id(), sel.payslipNumber());
    }

    private void printPayslip(Long id, String number) {
        if (printApiClient == null) {
            UIHelper.runWithProgress(this, "A gerar recibo em PDF…", () -> hrApiClient.renderPayslip(id),
                    pdf -> PrintPreviewDialog.show(this, pdf, "recibo-salario-" + number),
                    error -> showNotice(FeedbackType.ERROR, "Não foi possível gerar o PDF", error.getMessage()));
            return;
        }
        SensitivePrintCoordinator.print(this, printApiClient, SensitiveDocumentType.PAYSLIP,
                id, "Recibo de salário " + number, "recibo-salario-" + number,
                () -> hrApiClient.renderPayslip(id), this::loadPayslips);
    }

    private PayslipDTO selectedPayslip() {
        int row = TableFilter.selectedModelRow(payslipsTable);
        if (row < 0) {
            showNotice(FeedbackType.WARNING, "Seleccione um recibo",
                    "Escolha um recibo na tabela para continuar.");
            return null;
        }
        return payslipsList.get(row);
    }

    // ─── Absences tab ─────────────────────────────────────────────────────────

    private JPanel buildAbsencesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton newBtn = UIHelper.createSuccessButton("Registar Falta");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> HRAbsenceFormDialog.openCreateDialog(this, ABSENCE_TYPES));

        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar PDF");
        exportBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        exportBtn.addActionListener(e -> exportTable("faltas", "Mapa de Faltas", absencesTable));

        ModernButton refreshAbsencesBtn = UIHelper.createRefreshButton(this::loadAbsences);

        ActionMenuButton actionsBtn = UIHelper.createActionMenuButton("Acções da Falta")
                .addAction("Justificar", UIHelper.icon("fas-user-check", 14), employeeActions::justifyAbsence)
                .addAction("Eliminar", UIHelper.icon("fas-trash", 14), this::deleteSelectedAbsence);

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

        JTextField absSearch = TableFilter.searchField("Colaborador ou motivo…");
        JComboBox<String> absTipo = TableFilter.combo("Todos os tipos",
                UIHelper.humanStatus("PENDING_JUSTIFICATION"), UIHelper.humanStatus("JUSTIFIED"),
                UIHelper.humanStatus("UNJUSTIFIED"), UIHelper.humanStatus("SICK"),
                UIHelper.humanStatus("MATERNITY"), UIHelper.humanStatus("UNPAID_LEAVE"));
        JComboBox<String> absPeriodo = TableFilter.periodCombo();
        TableFilter.install(absencesTable, absSearch,
                java.util.List.of(new TableFilter.ColumnFilter(absTipo, 2)),
                java.util.List.of(new TableFilter.PeriodFilter(absPeriodo, 3)));
        JPanel absBar = TableFilter.bar(absSearch,
                TableFilter.label("Tipo:"), absTipo,
                TableFilter.label("Início:", "fas-calendar-alt"), absPeriodo, refreshAbsencesBtn);
        absBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(UIHelper.tableCardTop("Registo de Faltas", absBar,
                exportBtn, actionsBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    void loadAbsences() {
        UIHelper.loadAsync(this, hrApiClient::getAllAbsences, this::applyAbsences,
                error -> showLoadError("faltas", error));
    }

    private void applyAbsences(List<AbsenceDTO> loaded) {
        absencesList = loaded;
        absencesModel.setRowCount(0);
        for (AbsenceDTO a : absencesList) {
            absencesModel.addRow(new Object[]{
                    a.id(), a.employeeName(), UIHelper.humanStatus(a.absenceType()),
                    a.startDate().format(DATE_FMT), a.endDate().format(DATE_FMT),
                    a.totalDays(),
                    a.hasSupportingDocument() ? "Sim" : "Não",
                    a.reason() == null ? "" : a.reason()
            });
        }
        refreshOverview();
    }

    AbsenceDTO selectedAbsence() {
        int row = TableFilter.selectedModelRow(absencesTable);
        if (row < 0) {
            showNotice(FeedbackType.WARNING, "Seleccione uma falta",
                    "Escolha uma falta na tabela para continuar.");
            return null;
        }
        return absencesList.get(row);
    }

    private void deleteSelectedAbsence() {
        AbsenceDTO sel = selectedAbsence();
        if (sel != null) HRAbsenceFormDialog.deleteSelectedAbsence(this, sel);
    }

    // ─── Vacations tab ────────────────────────────────────────────────────────

    private JPanel buildVacationsTab() { return vacationsPanel.buildPanel(); }
    private void loadVacations() { vacationsPanel.load(); }

    // ─── Expenses tab ─────────────────────────────────────────────────────────

    private JPanel buildExpensesTab() { return expensesPanel.buildPanel(); }
    void loadExpenses() { expensesPanel.refresh(); }

    void showLoadError(String area, Throwable error) {
        feedback.show(FeedbackType.ERROR, "Não foi possível carregar " + area,
                error.getMessage(), "Tentar novamente", this::refreshData);
    }

    void showActionError(Throwable error) {
        showNotice(FeedbackType.ERROR, "Não foi possível concluir a operação", error.getMessage());
    }

    void showNotice(FeedbackType type, String title, String message) { feedback.show(type, title, message, null, null); }
    void showSuccess(String message) { ToastManager.success(this, message); }

    void exportTable(String baseName, String title, JTable table) {
        TableExportAction.export(this, printApiClient, table, title, baseName);
    }
}
