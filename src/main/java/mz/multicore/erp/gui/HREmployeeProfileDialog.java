package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.CircularAvatar;
import mz.multicore.erp.modules.hr.dto.AbsenceDTO;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.PayslipDTO;
import mz.multicore.erp.modules.hr.dto.VacationDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthSummaryDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Ficha consolidada, só-leitura, de um colaborador. */
final class HREmployeeProfileDialog {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private HREmployeeProfileDialog() {}

    static void show(Window parent,
                     EmployeeDTO employee,
                     List<PayslipDTO> payslips,
                     List<AbsenceDTO> absences,
                     List<VacationDTO> vacations,
                     OccupationalHealthSummaryDTO health) {
        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Perfil", UIHelper.icon("fas-id-card", 14, UIHelper.TEXT_LIGHT), profile(employee));
        tabs.addTab("Recibos", UIHelper.icon("fas-file-invoice-dollar", 14, UIHelper.TEXT_LIGHT),
                payslips(employee.id(), payslips));
        tabs.addTab("Faltas", UIHelper.icon("fas-user-times", 14, UIHelper.TEXT_LIGHT),
                absences(employee.id(), absences));
        tabs.addTab("Férias", UIHelper.icon("fas-umbrella-beach", 14, UIHelper.TEXT_LIGHT),
                vacations(employee.id(), vacations));
        tabs.addTab("Saúde", UIHelper.icon("fas-heartbeat", 14, UIHelper.TEXT_LIGHT), health(health));

        new ModernFormDialog(parent, "Perfil do Trabalhador", "fas-id-card",
                employee.name() + " · " + value(employee.employeeNumber()), tabs)
                .asReadOnly("Fechar")
                .setSize(820, 650)
                .showDialog();
    }

    private static JPanel profile(EmployeeDTO employee) {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(8, 4, 8, 4));

        ModernPanel identity = new ModernPanel(16);
        identity.setLayout(new BorderLayout(16, 0));
        identity.setBorder(new EmptyBorder(16, 18, 16, 18));
        CircularAvatar avatar = new CircularAvatar(employee.photo(), CircularAvatar.initials(employee.name()), 82);
        identity.add(avatar, BorderLayout.WEST);

        JPanel names = new JPanel();
        names.setOpaque(false);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(employee.name());
        name.setForeground(UIHelper.TEXT_LIGHT);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 20f));
        JLabel role = new JLabel(roleLabel(employee.role()) + " · " + value(employee.department()));
        role.setForeground(UIHelper.TEXT_MUTED);
        JLabel status = new JLabel(statusLabel(employee.status()));
        status.setForeground(statusColor(employee.status()));
        status.setFont(status.getFont().deriveFont(Font.BOLD));
        names.add(name);
        names.add(Box.createVerticalStrut(5));
        names.add(role);
        names.add(Box.createVerticalStrut(5));
        names.add(status);
        identity.add(names, BorderLayout.CENTER);
        root.add(identity, BorderLayout.NORTH);

        JPanel sections = new JPanel(new GridLayout(2, 2, 12, 12));
        sections.setOpaque(false);
        sections.add(section("Dados pessoais", "fas-user",
                row("Email", employee.email()),
                row("Telefone", employee.phone()),
                row("Dependentes IRPS", String.valueOf(employee.dependentsCount()))));
        sections.add(section("Vínculo laboral", "fas-briefcase",
                row("Número interno", employee.employeeNumber()),
                row("Admissão", date(employee.hireDate())),
                row("Antiguidade", seniority(employee.hireDate())),
                row("Fim do contrato", date(employee.contractEndDate()))));
        sections.add(section("Fiscal e salarial", "fas-coins",
                row("NUIT", employee.taxId()),
                row("Nº INSS", employee.inssNumber()),
                row("Salário base", money(employee.baseSalary()))));
        sections.add(section("Acesso ao sistema", "fas-user-shield",
                row("Perfil", roleLabel(employee.role())),
                row("Conta ligada", employee.username()),
                row("Self-service", employee.username() == null || employee.username().isBlank()
                        ? "Não disponível" : "Disponível"),
                row("Banco", employee.bankName()),
                row("Conta bancária", employee.bankAccount())));
        root.add(sections, BorderLayout.CENTER);
        return root;
    }

    private static JPanel payslips(Long employeeId, List<PayslipDTO> source) {
        DefaultTableModel model = model("Nº Recibo", "Período", "Bruto", "Descontos", "Líquido", "Estado");
        source.stream().filter(p -> employeeId.equals(p.employeeId())).forEach(p -> model.addRow(new Object[]{
                p.payslipNumber(), String.format("%02d/%d", p.month(), p.year()), p.grossPay(),
                p.totalDeductions(), p.netPay(), p.status()}));
        JTable table = table(model);
        table.getColumnModel().getColumn(2).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());
        return historyPanel(table, model.getRowCount(), "Nenhum recibo encontrado para este trabalhador.");
    }

    private static JPanel health(OccupationalHealthSummaryDTO health) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(14, 4, 4, 4));
        if (health == null || !health.hasExam()) {
            panel.add(section("Aptidão médica", "fas-heartbeat",
                    row("Situação", "Sem exame ocupacional registado"),
                    row("Acção", "Agendar exame de admissão ou periódico")), BorderLayout.NORTH);
            return panel;
        }
        panel.add(section("Aptidão médica", "fas-heartbeat",
                row("Resultado", fitnessLabel(health.fitnessResult())),
                row("Data do exame", date(health.examDate())),
                row("Validade", date(health.expiryDate())),
                row("Situação", healthStatusLabel(health.validityStatus(), health.daysUntilExpiry()))),
                BorderLayout.NORTH);
        return panel;
    }

    private static String fitnessLabel(String result) {
        return switch (value(result)) {
            case "FIT" -> "Apto";
            case "FIT_WITH_RESTRICTIONS" -> "Apto com restrições";
            case "UNFIT" -> "Inapto";
            default -> value(result);
        };
    }

    private static String healthStatusLabel(String status, Long days) {
        return switch (value(status)) {
            case "EXPIRED" -> "Expirado há " + Math.abs(days == null ? 0 : days) + " dia(s)";
            case "EXPIRING" -> "Renovar em " + (days == null ? 0 : days) + " dia(s)";
            case "VALID" -> "Válido";
            default -> "Não registado";
        };
    }

    private static JPanel absences(Long employeeId, List<AbsenceDTO> source) {
        DefaultTableModel model = model("Tipo", "Início", "Fim", "Dias", "Motivo", "Documento");
        source.stream().filter(a -> employeeId.equals(a.employeeId())).forEach(a -> model.addRow(new Object[]{
                a.absenceType(), date(a.startDate()), date(a.endDate()), a.totalDays(), value(a.reason()),
                a.hasSupportingDocument() ? "Sim" : "Não"}));
        return historyPanel(table(model), model.getRowCount(), "Nenhuma falta encontrada para este trabalhador.");
    }

    private static JPanel vacations(Long employeeId, List<VacationDTO> source) {
        DefaultTableModel model = model("Início", "Fim", "Dias úteis", "Ano", "Estado", "Decisão");
        source.stream().filter(v -> employeeId.equals(v.employeeId())).forEach(v -> model.addRow(new Object[]{
                date(v.startDate()), date(v.endDate()), v.totalDays(), v.yearReference(), v.status(),
                value(v.decisionBy())}));
        JTable table = table(model);
        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.status());
        return historyPanel(table, model.getRowCount(), "Nenhum pedido de férias encontrado para este trabalhador.");
    }

    private static JPanel historyPanel(JTable table, int count, String emptyMessage) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 4, 4, 4));
        JLabel summary = new JLabel(count == 0 ? emptyMessage : count + " registo(s) encontrado(s)");
        summary.setForeground(UIHelper.TEXT_MUTED);
        panel.add(summary, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        return table;
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static ModernPanel section(String title, String icon, JLabel... rows) {
        ModernPanel panel = new ModernPanel(14);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));
        JLabel heading = new JLabel(title, UIHelper.icon(icon, 14, UIHelper.ACCENT_BLUE), SwingConstants.LEFT);
        heading.setForeground(UIHelper.TEXT_LIGHT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 14f));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading);
        panel.add(Box.createVerticalStrut(10));
        for (JLabel row : rows) {
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(row);
            panel.add(Box.createVerticalStrut(7));
        }
        return panel;
    }

    private static JLabel row(String label, String value) {
        JLabel row = new JLabel("<html><b>" + label + ":</b> " + escape(value(value)) + "</html>");
        row.setForeground(UIHelper.TEXT_MUTED);
        return row;
    }

    private static String seniority(LocalDate hireDate) {
        if (hireDate == null) return "—";
        Period period = Period.between(hireDate, LocalDate.now());
        if (period.isNegative()) return "Admissão futura";
        return period.getYears() + " ano(s) e " + period.getMonths() + " mês(es)";
    }

    private static String money(BigDecimal amount) {
        return amount == null ? "—" : String.format("%,.2f MT", amount);
    }

    private static String date(LocalDate date) { return date == null ? "—" : date.format(DATE_FMT); }
    private static String value(String value) { return value == null || value.isBlank() ? "—" : value; }
    private static String roleLabel(String role) {
        return switch (value(role).toUpperCase()) {
            case "ADMIN" -> "Administrador";
            case "MANAGER" -> "Gestor";
            case "SELLER" -> "Vendedor";
            case "EMPLOYEE" -> "Colaborador";
            default -> value(role);
        };
    }
    private static String statusLabel(String status) {
        return switch (value(status).toUpperCase()) {
            case "ACTIVE" -> "Activo";
            case "SUSPENDED" -> "Suspenso";
            case "TERMINATED" -> "Cessado";
            default -> value(status);
        };
    }
    private static Color statusColor(String status) {
        return switch (value(status).toUpperCase()) {
            case "ACTIVE" -> UIHelper.APPROVED_GREEN;
            case "SUSPENDED" -> UIHelper.PENDING_YELLOW;
            case "TERMINATED" -> UIHelper.REJECTED_RED;
            default -> UIHelper.TEXT_MUTED;
        };
    }
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
