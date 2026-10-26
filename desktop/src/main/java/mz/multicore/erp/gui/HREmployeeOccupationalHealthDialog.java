package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.HealthProviderDTO;
import mz.multicore.erp.modules.hr.dto.MissingHealthExamDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthCostDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthExamDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthProviderCostDTO;
import mz.multicore.erp.modules.hr.dto.SaveOccupationalHealthExamRequest;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Diálogos modais corporativos de saúde ocupacional, exames de aptidão, comprovativos e custos.
 */
final class HREmployeeOccupationalHealthDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] FITNESS_RESULTS = {"FIT", "FIT_WITH_RESTRICTIONS", "UNFIT"};
    private static final String[] FITNESS_LABELS = {"Apto", "Apto com restrições", "Inapto"};
    private static final String NO_PROVIDER = "Sem prestador cadastrado";

    private HREmployeeOccupationalHealthDialog() {}

    static void openOccupationalHealth(HRPanel owner, EmployeeDTO employee) {
        if (employee == null) return;
        if (!SignedInUser.isManagerOrAdmin()) {
            owner.showNotice(FeedbackType.WARNING, "Acesso restrito",
                    "Apenas gestores ou administradores podem consultar dados de saúde ocupacional.");
            return;
        }
        UIHelper.runWithProgress(owner, "A carregar saúde ocupacional…",
                () -> owner.hrApiClient.getOccupationalHealthHistory(employee.id()),
                history -> showOccupationalHealth(owner, employee, history), owner::showActionError);
    }

    private static void showOccupationalHealth(HRPanel owner, EmployeeDTO employee, List<OccupationalHealthExamDTO> history) {
        String[] cols = {"Exame", "Validade", "Resultado", "Situação", "Prestador", "Custo", "Pagamento", "Comprovativo"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        BigDecimal spent = BigDecimal.ZERO, owed = BigDecimal.ZERO;
        for (OccupationalHealthExamDTO exam : history) {
            if (exam.cost() != null) {
                spent = spent.add(exam.cost());
                if (!exam.paid()) owed = owed.add(exam.cost());
            }
            model.addRow(new Object[]{exam.examDate().format(DATE_FMT), exam.expiryDate().format(DATE_FMT),
                    fitnessLabel(exam.fitnessResult()), healthSituation(exam),
                    exam.providerName() == null ? "—" : exam.providerName(),
                    exam.cost() == null ? "—" : money(exam.cost()),
                    exam.cost() == null ? "—" : exam.paid() ? "Pago em " + exam.paidAt().format(DATE_FMT) : "Por pagar",
                    exam.hasAttachment() ? "Sim" : "Não"});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        int[] widths = {95, 95, 150, 125, 145, 110, 125, 91};
        for (int column = 0; column < widths.length && column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(936, 240));

        String totalsLine = spent.signum() == 0
                ? "Sem custos de exames registados para este trabalhador."
                : String.format("Custo suportado pela empresa: <b>%s</b> · por pagar às clínicas: <b>%s</b>",
                        money(spent), money(owed));
        JLabel privacy = new JLabel("<html>Dados clínicos restritos · cada consulta fica registada na "
                + "auditoria · cada renovação mantém o histórico anterior<br>" + totalsLine + "</html>");
        privacy.setForeground(UIHelper.TEXT_MUTED);

        JPanel actionToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); actionToolbar.setOpaque(false);
        String actionTitle = history.isEmpty() ? "Registar Exame" : "Registar Renovação";
        ModernButton examBtn = UIHelper.createPrimaryButton(actionTitle); examBtn.setIcon(UIHelper.icon("fas-plus", 14)); examBtn.addActionListener(e -> openOccupationalHealthForm(owner, employee));
        ModernButton attachmentBtn = UIHelper.createSecondaryButton("Abrir Comprovativo"); attachmentBtn.setIcon(UIHelper.icon("fas-paperclip", 14)); attachmentBtn.addActionListener(e -> openOccupationalHealthAttachment(owner, history, table.getSelectedRow()));
        ModernButton payBtn = UIHelper.createSecondaryButton("Registar Pagamento"); payBtn.setIcon(UIHelper.icon("fas-money-bill-wave", 14)); payBtn.addActionListener(e -> payOccupationalHealthExam(owner, employee, history, table.getSelectedRow()));
        ModernButton complianceBtn = UIHelper.createSecondaryButton("Custos e Conformidade"); complianceBtn.setIcon(UIHelper.icon("fas-chart-pie", 14)); complianceBtn.addActionListener(e -> openOccupationalHealthCompliance(owner));
        actionToolbar.add(examBtn); actionToolbar.add(attachmentBtn); actionToolbar.add(payBtn); actionToolbar.add(complianceBtn);

        JPanel headerArea = new JPanel(new BorderLayout(0, 10)); headerArea.setOpaque(false);
        headerArea.add(privacy, BorderLayout.NORTH); headerArea.add(actionToolbar, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 10)); content.setOpaque(false);
        content.add(headerArea, BorderLayout.NORTH); content.add(scroll, BorderLayout.CENTER);

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow,
                "Saúde Ocupacional — " + employee.name(), "fas-heartbeat",
                "Ficha médica corporativa com aptidão funcional e controlo de custos.", content);
        dialog.setSize(960, 520);
        dialog.showDialog();
    }

    static void openOccupationalHealthAttachment(HRPanel owner, List<OccupationalHealthExamDTO> history, int row) {
        if (row < 0 || row >= history.size()) {
            owner.showNotice(FeedbackType.WARNING, "Seleccione um exame", "Escolha na lista o exame cujo comprovativo quer abrir.");
            return;
        }
        OccupationalHealthExamDTO exam = history.get(row);
        if (!exam.hasAttachment()) {
            owner.showNotice(FeedbackType.INFO, "Sem comprovativo", "Este exame não tem comprovativo digitalizado.");
            return;
        }
        UIHelper.runWithProgress(owner, "A abrir comprovativo…",
                () -> owner.hrApiClient.getOccupationalHealthAttachment(exam.id()),
                attachment -> {
                    try {
                        String name = attachment.fileName() == null ? "comprovativo" : attachment.fileName();
                        int dot = name.lastIndexOf('.');
                        java.io.File file = java.io.File.createTempFile("multicore-exame-", dot > 0 ? name.substring(dot) : ".pdf");
                        file.deleteOnExit();
                        Files.write(file.toPath(), attachment.content());
                        Desktop.getDesktop().open(file);
                    } catch (Exception ex) {
                        owner.showNotice(FeedbackType.ERROR, "Não foi possível abrir o comprovativo", ex.getMessage());
                    }
                }, owner::showActionError);
    }

    static void payOccupationalHealthExam(HRPanel owner, EmployeeDTO employee,
                                         List<OccupationalHealthExamDTO> history, int row) {
        if (row < 0 || row >= history.size()) {
            owner.showNotice(FeedbackType.WARNING, "Seleccione um exame", "Escolha na lista o exame cuja factura vai pagar.");
            return;
        }
        OccupationalHealthExamDTO exam = history.get(row);
        if (exam.cost() == null) {
            owner.showNotice(FeedbackType.WARNING, "Sem custo registado", "Registe a factura da clínica antes de pagar este exame.");
            return;
        }
        if (exam.paid()) {
            owner.showNotice(FeedbackType.INFO, "Exame já pago", "Este exame foi pago em " + exam.paidAt().format(DATE_FMT) + ".");
            return;
        }
        boolean confirmed = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner), FeedbackType.WARNING,
                "Confirmar Pagamento", String.format("Pagar %s a %s pelo exame de %s?%nA saída sai da tesouraria como encargo da empresa.",
                        money(exam.cost()), exam.providerName() == null ? "prestador não identificado" : exam.providerName(), employee.name()), "Pagar");
        if (!confirmed) return;
        UIHelper.runWithProgress(owner, "A pagar exame ocupacional…",
                () -> owner.hrApiClient.payOccupationalHealthExam(exam.id()),
                ignored -> owner.showSuccess("Pagamento registado na tesouraria."), owner::showActionError);
    }

    static void openOccupationalHealthCompliance(HRPanel owner) {
        LocalDate from = LocalDate.now().withDayOfYear(1), to = LocalDate.now();
        UIHelper.runWithProgress(owner, "A apurar conformidade e custos…",
                () -> new HealthCompliance(owner.hrApiClient.getEmployeesMissingHealthExam(),
                        owner.hrApiClient.getOccupationalHealthCosts(from, to)),
                snapshot -> showOccupationalHealthCompliance(owner, snapshot), owner::showActionError);
    }

    private static void showOccupationalHealthCompliance(HRPanel owner, HealthCompliance snapshot) {
        DefaultTableModel missing = new DefaultTableModel(
                new String[]{"Trabalhador", "Função", "Departamento", "Admissão", "Dias sem exame"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (MissingHealthExamDTO item : snapshot.missing()) {
            missing.addRow(new Object[]{item.employeeName(), item.role() == null ? "—" : item.role(),
                    item.department() == null ? "—" : item.department(),
                    item.hireDate() == null ? "—" : item.hireDate().format(DATE_FMT),
                    item.daysSinceHire() == null ? "Admissão por registar" : item.daysSinceHire()});
        }
        DefaultTableModel costs = new DefaultTableModel(
                new String[]{"Prestador", "Exames", "Total", "Por pagar"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (OccupationalHealthProviderCostDTO line : snapshot.costs().byProvider()) {
            costs.addRow(new Object[]{line.providerName(), line.examCount(), money(line.total()), money(line.pending())});
        }

        JTabbedPane tabs = new JTabbedPane(); UIHelper.styleTabbedPane(tabs);
        tabs.addTab("Sem exame (" + snapshot.missing().size() + ")",
                UIHelper.icon("fas-exclamation-triangle", 14, UIHelper.PENDING_YELLOW), complianceTable(missing,
                        "Trabalhadores no activo que nunca fizeram exame de aptidão. Quem nunca fez "
                                + "não aparece nos avisos de validade — é aqui que aparece."));
        tabs.addTab("Custos do ano", UIHelper.icon("fas-coins", 14, UIHelper.APPROVED_GREEN), complianceTable(costs,
                String.format("De %s a %s · %d exame(s) com custo · total %s · por pagar %s. "
                                + "O exame de aptidão é encargo do empregador, nunca do trabalhador.",
                        snapshot.costs().from().format(DATE_FMT), snapshot.costs().to().format(DATE_FMT),
                        snapshot.costs().examCount(), money(snapshot.costs().total()), money(snapshot.costs().pending()))));

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow,
                "Saúde Ocupacional — Conformidade e Custos", "fas-shield-alt",
                "Conformidade médica do quadro de colaboradores e custos anuais por prestador.", tabs);
        dialog.setSize(920, 520); dialog.showDialog();
    }

    private static JPanel complianceTable(DefaultTableModel model, String caption) {
        JTable table = new JTable(model); UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table); UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(860, 300));
        JLabel label = new JLabel("<html><body style=\"width:820px\">" + caption + "</body></html>");
        label.setForeground(UIHelper.TEXT_MUTED);
        JPanel panel = new JPanel(new BorderLayout(0, 8)); panel.setOpaque(false);
        panel.add(label, BorderLayout.NORTH); panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    static void openOccupationalHealthForm(HRPanel owner, EmployeeDTO employee) {
        UIHelper.runWithProgress(owner, "A carregar prestadores…",
                () -> owner.hrApiClient.getHealthProviders(),
                providers -> showOccupationalHealthForm(owner, employee, providers), owner::showActionError);
    }

    private static void showOccupationalHealthForm(HRPanel owner, EmployeeDTO employee, List<HealthProviderDTO> providers) {
        JTextField cardField = new JTextField(), clinicField = new JTextField(), doctorField = new JTextField();
        JTextField restrictionsField = new JTextField(), notesField = new JTextField(), invoiceField = new JTextField();
        for (JTextField field : new JTextField[]{cardField, clinicField, doctorField, restrictionsField, notesField, invoiceField}) {
            UIHelper.styleTextField(field);
        }
        DateField examDate = new DateField(LocalDate.now()), expiryDate = new DateField(LocalDate.now().plusYears(1));
        JComboBox<String> resultCombo = new JComboBox<>(FITNESS_LABELS); UIHelper.styleComboBox(resultCombo);
        JComboBox<String> providerCombo = new JComboBox<>(); providerCombo.addItem(NO_PROVIDER);
        for (HealthProviderDTO provider : providers) providerCombo.addItem(provider.name());
        UIHelper.styleComboBox(providerCombo);
        MoneyField costField = new MoneyField();

        final byte[][] attachment = {null};
        final String[] attachmentName = {null};
        JLabel attachmentLabel = new JLabel("Nenhum comprovativo seleccionado"); attachmentLabel.setForeground(UIHelper.TEXT_MUTED);
        ModernButton attachmentButton = UIHelper.createSecondaryButton("Anexar Comprovativo"); attachmentButton.setIcon(UIHelper.icon("fas-paperclip", 14));
        attachmentButton.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Escolher comprovativo do exame");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF ou imagem", "pdf", "jpg", "jpeg", "png"));
            if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) return;
            try {
                byte[] data = Files.readAllBytes(chooser.getSelectedFile().toPath());
                if (data.length > 5_000_000) throw new IllegalArgumentException("O ficheiro excede 5 MB.");
                attachment[0] = data; attachmentName[0] = chooser.getSelectedFile().getName();
                attachmentLabel.setText(attachmentName[0]);
            } catch (Exception ex) {
                owner.showNotice(FeedbackType.ERROR, "Comprovativo inválido", ex.getMessage());
            }
        });
        JPanel attachmentPanel = new JPanel(new BorderLayout(8, 0)); attachmentPanel.setOpaque(false);
        attachmentPanel.add(attachmentButton, BorderLayout.WEST); attachmentPanel.add(attachmentLabel, BorderLayout.CENTER);

        JPanel form = UIHelper.createDialogForm(
                "Número do cartão:", cardField,
                "Data do exame:", examDate,
                "Validade:", expiryDate,
                "Resultado:", resultCombo,
                "Prestador (cadastrado):", providerCombo,
                "Clínica (se não cadastrada):", clinicField,
                "Médico responsável:", doctorField,
                "Restrições laborais:", restrictionsField,
                "Observações:", notesField,
                "Custo do exame:", costField,
                "Nº da factura:", invoiceField,
                "Comprovativo (máx. 5 MB):", attachmentPanel);
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Exame de Saúde — " + employee.name(), "fas-heartbeat",
                "Registe apenas aptidão e restrições de função: a lei não permite guardar diagnóstico. A renovação preserva o histórico.", form)
                .setSize(860, 760).showDialog();
        if (!confirmed) return;

        int providerIndex = providerCombo.getSelectedIndex();
        Long providerId = providerIndex <= 0 ? null : providers.get(providerIndex - 1).id();
        BigDecimal cost;
        try {
            cost = costField.optionalValue();
        } catch (RuntimeException ex) {
            owner.showNotice(FeedbackType.ERROR, "Custo inválido", "Introduza um custo válido para o exame.");
            return;
        }
        SaveOccupationalHealthExamRequest request = new SaveOccupationalHealthExamRequest(
                employee.id(), blank(cardField.getText()), examDate.value(), expiryDate.value(),
                FITNESS_RESULTS[resultCombo.getSelectedIndex()], providerId, blank(clinicField.getText()),
                blank(doctorField.getText()), blank(restrictionsField.getText()), blank(notesField.getText()),
                cost, blank(invoiceField.getText()), attachmentName[0], attachment[0]);
        UIHelper.runWithProgress(owner, "A registar exame ocupacional…",
                () -> owner.hrApiClient.registerOccupationalHealthExam(request),
                ignored -> owner.showSuccess("Exame ocupacional registado; o histórico anterior foi preservado."),
                owner::showActionError);
    }

    private static String money(BigDecimal value) {
        return String.format("%,.2f MT", value == null ? BigDecimal.ZERO : value);
    }

    private static String healthSituation(OccupationalHealthExamDTO exam) {
        return switch (exam.validityStatus()) {
            case "EXPIRED" -> "Expirado há " + Math.abs(exam.daysUntilExpiry()) + " dia(s)";
            case "EXPIRING" -> "Renovar em " + exam.daysUntilExpiry() + " dia(s)";
            default -> "Válido";
        };
    }

    private static String fitnessLabel(String value) {
        return switch (value) {
            case "FIT" -> "Apto";
            case "FIT_WITH_RESTRICTIONS" -> "Apto com restrições";
            case "UNFIT" -> "Inapto";
            default -> value;
        };
    }

    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private record HealthCompliance(List<MissingHealthExamDTO> missing, OccupationalHealthCostDTO costs) {}
}
