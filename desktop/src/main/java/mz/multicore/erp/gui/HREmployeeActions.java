package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.MoneyField;
import mz.multicore.erp.gui.components.SimpleBarChart;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.AbsenceDTO;
import mz.multicore.erp.modules.hr.dto.CreateSalaryChangeRequest;
import mz.multicore.erp.modules.hr.dto.EmployeeDTO;
import mz.multicore.erp.modules.hr.dto.EmployeeDocumentDTO;
import mz.multicore.erp.modules.hr.dto.SalaryChangeDTO;
import mz.multicore.erp.modules.hr.dto.SaveEmployeeDocumentRequest;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthExamDTO;
import mz.multicore.erp.modules.hr.dto.SaveOccupationalHealthExamRequest;
import mz.multicore.erp.modules.hr.dto.HealthProviderDTO;
import mz.multicore.erp.modules.hr.dto.MissingHealthExamDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthAttachmentDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthCostDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthProviderCostDTO;
import mz.multicore.erp.desktop.session.SignedInUser;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;

/**
 * Acções sobre um colaborador que não cabem mais no {@link HRPanel}: evolução salarial (§B4),
 * documentos com validade (§B8.8) e justificação de faltas (§B2 / RHC-25).
 *
 * <p>São <b>diálogos e não separadores</b> de propósito. Um documento e uma alteração salarial
 * pertencem a <i>uma pessoa</i>: procurá-los numa lista de toda a empresa é a forma errada de os
 * encontrar. E a barra de separadores do RH já está a 1352 px dos 1382 que a
 * {@code TabStripFitsTest} permite — mais um separador e algo desaparecia atrás das setas.
 */
final class HREmployeeActions {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] SALARY_REASONS = {
            "AUMENTO", "PROMOCAO", "REVISAO_ANUAL", "ACORDO", "CORRECCAO"};
    private static final String[] SALARY_REASON_LABELS = {
            "Aumento", "Promoção", "Revisão anual", "Acordo", "Correcção"};

    private static final String[] DOCUMENT_TYPES = {
            "BI", "DIRE", "PASSAPORTE", "NUIT", "CERTIFICADO", "OUTRO"};
    private static final String[] FITNESS_RESULTS = {"FIT", "FIT_WITH_RESTRICTIONS", "UNFIT"};
    private static final String[] FITNESS_LABELS = {"Apto", "Apto com restrições", "Inapto"};
    private static final String NO_PROVIDER = "— sem prestador cadastrado —";

    /** Tipos de falta para justificação. O primeiro é o que a falta gerada pelo ponto deixa de ser. */
    private static final String[] ABSENCE_TYPES = {
            "JUSTIFIED", "SICK", "MATERNITY", "UNJUSTIFIED", "UNPAID_LEAVE"};
    private static final String[] ABSENCE_TYPE_LABELS = {
            "Justificada (remunerada)", "Baixa médica (remunerada)", "Maternidade (remunerada)",
            "Injustificada (desconta)", "Licença sem vencimento (desconta)"};

    private final HRPanel owner;
    private final Supplier<EmployeeDTO> employeeSelection;
    private final Supplier<AbsenceDTO> absenceSelection;
    private final Runnable afterAbsenceChange;

    HREmployeeActions(HRPanel owner,
                      Supplier<EmployeeDTO> employeeSelection,
                      Supplier<AbsenceDTO> absenceSelection,
                      Runnable afterAbsenceChange) {
        this.owner = owner;
        this.employeeSelection = employeeSelection;
        this.absenceSelection = absenceSelection;
        this.afterAbsenceChange = afterAbsenceChange;
    }

    // ─── §B4: evolução salarial ───────────────────────────────────────────────

    /**
     * A série datada do colaborador, em gráfico e em tabela. É o ecrã que responde a "quanto é que
     * esta pessoa ganhava em Março?" — a pergunta que cada recibo faz e que, antes do B4, não tinha
     * resposta possível porque o salário anterior não ficava em lado nenhum.
     */
    void openSalaryHistory() {
        EmployeeDTO employee = employeeSelection.get();
        if (employee == null) {
            return;
        }
        UIHelper.runWithProgress(owner, "A carregar evolução salarial…",
                () -> owner.hrApiClient.getSalaryHistory(employee.id()),
                history -> showSalaryHistory(employee, history), owner::showActionError);
    }

    private void showSalaryHistory(EmployeeDTO employee, List<SalaryChangeDTO> history) {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.setPreferredSize(new Dimension(720, 460));

        SimpleBarChart chart = new SimpleBarChart("Evolução do salário base");
        // A série vem da mais recente para a mais antiga; o gráfico lê-se da esquerda para a
        // direita, pelo que a ordem tem de ser invertida — senão a subida aparece como descida.
        int size = history.size();
        String[] labels = new String[size];
        java.math.BigDecimal[] values = new java.math.BigDecimal[size];
        Color[] colors = new Color[size];
        for (int i = 0; i < size; i++) {
            SalaryChangeDTO change = history.get(size - 1 - i);
            labels[i] = change.effectiveDate().format(DATE_FMT);
            values[i] = change.newSalary() == null ? java.math.BigDecimal.ZERO : change.newSalary();
            colors[i] = UIHelper.KPI_PURPLE_DARK;
        }
        chart.setData(labels, values, colors);
        content.add(chart, BorderLayout.CENTER);

        String[] cols = {"Data de efeito", "Anterior (MT)", "Novo (MT)", "Diferença (MT)",
                "Motivo", "Função", "Aprovado por"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (SalaryChangeDTO change : history) {
            model.addRow(new Object[]{
                    change.effectiveDate().format(DATE_FMT),
                    change.previousSalary(), change.newSalary(), change.difference(),
                    change.reasonLabel(),
                    change.jobTitle() == null ? "-" : change.jobTitle(),
                    change.approvedBy()});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(720, 180));
        content.add(scroll, BorderLayout.SOUTH);

        Object[] options = {"Registar Alteração", "Fechar"};
        int answer = JOptionPane.showOptionDialog(owner, content,
                "Evolução Salarial — " + employee.name(), JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        if (answer == 0) {
            openSalaryChangeDialog(employee);
        }
    }

    private void openSalaryChangeDialog(EmployeeDTO employee) {
        MoneyField salaryField = new MoneyField(
                employee.baseSalary() == null ? "0.00" : employee.baseSalary().toPlainString());
        DateField effectiveField = new DateField(LocalDate.now());
        JComboBox<String> reasonCombo = new JComboBox<>(SALARY_REASON_LABELS);
        UIHelper.styleComboBox(reasonCombo);
        JTextField jobField = new JTextField();
        UIHelper.styleTextField(jobField);
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Novo salário:", salaryField,
                "Data de efeito:", effectiveField,
                "Motivo:", reasonCombo,
                "Nova função (vazio = mantém):", jobField,
                "Observações:", notesField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Alteração Salarial — " + employee.name(), "fas-chart-line",
                "Uma data de efeito futura é um compromisso: não mexe na ficha até lá chegar, mas "
                        + "já manda no recibo desse mês.", form).showDialog();
        if (!confirmed) {
            return;
        }

        CreateSalaryChangeRequest request = new CreateSalaryChangeRequest(
                employee.id(), salaryField.value(), effectiveField.value(),
                SALARY_REASONS[reasonCombo.getSelectedIndex()],
                jobField.getText().trim().isEmpty() ? null : jobField.getText().trim(), null,
                notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());
        UIHelper.runWithProgress(owner, "A registar alteração salarial…",
                () -> owner.hrApiClient.registerSalaryChange(request),
                ignored -> {
                    JOptionPane.showMessageDialog(owner, "Alteração salarial registada.",
                            "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    owner.refreshData();
                }, owner::showActionError);
    }

    // ─── §B8.8: documentos do colaborador ─────────────────────────────────────

    /** Documentos e validades. O DIRE de um trabalhador estrangeiro caducar sem aviso é multa. */
    void openDocuments() {
        EmployeeDTO employee = employeeSelection.get();
        if (employee == null) {
            return;
        }
        UIHelper.runWithProgress(owner, "A carregar documentos…",
                () -> owner.hrApiClient.getEmployeeDocuments(employee.id()),
                documents -> showDocuments(employee, documents), owner::showActionError);
    }

    private void showDocuments(EmployeeDTO employee, List<EmployeeDocumentDTO> documents) {
        String[] cols = {"Tipo", "Número", "Emissão", "Validade", "Situação"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (EmployeeDocumentDTO d : documents) {
            model.addRow(new Object[]{
                    d.documentType(),
                    d.documentNumber() == null ? "-" : d.documentNumber(),
                    d.issueDate() == null ? "-" : d.issueDate().format(DATE_FMT),
                    d.expiryDate() == null ? "Não caduca" : d.expiryDate().format(DATE_FMT),
                    situationOf(d)});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(640, 260));

        Object[] options = {"Novo Documento", "Fechar"};
        int answer = JOptionPane.showOptionDialog(owner, scroll,
                "Documentos — " + employee.name(), JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        if (answer == 0) {
            openDocumentDialog(employee);
        }
    }

    /** Nunca "-": um documento sem validade diz que não caduca, e um caducado diz há quantos dias. */
    private String situationOf(EmployeeDocumentDTO d) {
        if (d.expiryDate() == null) {
            return "Sem validade";
        }
        if (d.expired()) {
            return "Caducado há " + Math.abs(d.daysUntilExpiry()) + " dia(s)";
        }
        return "Válido — faltam " + d.daysUntilExpiry() + " dia(s)";
    }

    private void openDocumentDialog(EmployeeDTO employee) {
        JComboBox<String> typeCombo = new JComboBox<>(DOCUMENT_TYPES);
        UIHelper.styleComboBox(typeCombo);
        JTextField numberField = new JTextField();
        UIHelper.styleTextField(numberField);
        DateField issueField = new DateField(null);
        DateField expiryField = new DateField(null);
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(notesField);

        JPanel form = UIHelper.createDialogForm(
                "Tipo:", typeCombo,
                "Número:", numberField,
                "Emissão:", issueField,
                "Validade (vazio = não caduca):", expiryField,
                "Observações:", notesField);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow,
                "Novo Documento — " + employee.name(), "fas-id-card",
                "Deixar a validade vazia significa que o documento não caduca — não é o mesmo que "
                        + "ainda não a saber.", form).showDialog();
        if (!confirmed) {
            return;
        }
        SaveEmployeeDocumentRequest request = new SaveEmployeeDocumentRequest(
                employee.id(), (String) typeCombo.getSelectedItem(),
                numberField.getText().trim().isEmpty() ? null : numberField.getText().trim(),
                issueField.value(), expiryField.value(),
                notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());
        UIHelper.runWithProgress(owner, "A gravar documento…",
                () -> owner.hrApiClient.saveEmployeeDocument(request),
                ignored -> JOptionPane.showMessageDialog(owner, "Documento registado.",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE),
                owner::showActionError);
    }

    // ─── Saúde ocupacional ───────────────────────────────────────────────────

    void openOccupationalHealth() {
        EmployeeDTO employee = employeeSelection.get();
        if (employee == null) return;
        if (!SignedInUser.isManagerOrAdmin()) {
            JOptionPane.showMessageDialog(owner,
                    "Apenas gestores ou administradores podem consultar dados de saúde ocupacional.",
                    "Acesso restrito", JOptionPane.WARNING_MESSAGE);
            return;
        }
        UIHelper.runWithProgress(owner, "A carregar saúde ocupacional…",
                () -> owner.hrApiClient.getOccupationalHealthHistory(employee.id()),
                history -> showOccupationalHealth(employee, history), owner::showActionError);
    }

    private void showOccupationalHealth(EmployeeDTO employee, List<OccupationalHealthExamDTO> history) {
        String[] cols = {"Exame", "Validade", "Resultado", "Situação", "Prestador", "Custo",
                "Pagamento", "Comprovativo"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        BigDecimal spent = BigDecimal.ZERO;
        BigDecimal owed = BigDecimal.ZERO;
        for (OccupationalHealthExamDTO exam : history) {
            if (exam.cost() != null) {
                spent = spent.add(exam.cost());
                if (!exam.paid()) owed = owed.add(exam.cost());
            }
            model.addRow(new Object[]{exam.examDate().format(DATE_FMT), exam.expiryDate().format(DATE_FMT),
                    fitnessLabel(exam.fitnessResult()), healthSituation(exam),
                    exam.providerName() == null ? "—" : exam.providerName(),
                    exam.cost() == null ? "—" : money(exam.cost()),
                    exam.cost() == null ? "—" : exam.paid()
                            ? "Pago em " + exam.paidAt().format(DATE_FMT) : "Por pagar",
                    exam.hasAttachment() ? "Sim" : "Não"});
        }
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        // Oito colunas em 900 px truncam as que interessam se ninguém decidir a repartição: o
        // resultado de aptidão ficava "Apto com re..." e a situação "Expirado há ...". As datas e o
        // comprovativo têm largura fixa e conhecida; o que sobra vai para os campos de texto.
        int[] widths = {95, 95, 150, 125, 145, 110, 125, 91};
        for (int column = 0; column < widths.length && column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
        }
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        // 240 e não 280: com 280 a linha de totais ficava cortada a meio pela margem do diálogo —
        // o número que diz quanto a empresa gastou era exactamente o que não se lia.
        scroll.setPreferredSize(new Dimension(936, 240));
        // Um rótulo HTML de duas linhas, e não dois rótulos empilhados: dois JLabel num BoxLayout
        // recebiam menos altura do que pediam e a segunda linha — a que diz quanto a empresa gastou —
        // saía cortada a meio. Com HTML a altura preferida é calculada pelo próprio rótulo.
        String totalsLine = spent.signum() == 0
                ? "Sem custos de exames registados para este trabalhador."
                : String.format("Custo suportado pela empresa: <b>%s</b> · por pagar às clínicas: <b>%s</b>",
                        money(spent), money(owed));
        JLabel privacy = new JLabel("<html>Dados clínicos restritos · cada consulta fica registada na "
                + "auditoria · cada renovação mantém o histórico anterior<br>" + totalsLine + "</html>");
        privacy.setForeground(UIHelper.TEXT_MUTED);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.add(privacy, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);

        String action = history.isEmpty() ? "Registar Exame" : "Registar Renovação";
        Object[] options = {action, "Abrir Comprovativo", "Registar Pagamento",
                "Custos e Conformidade", "Fechar"};
        int answer = JOptionPane.showOptionDialog(owner, content,
                "Saúde Ocupacional — " + employee.name(), JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[4]);
        if (answer == 0) openOccupationalHealthForm(employee);
        else if (answer == 1) openOccupationalHealthAttachment(history, table.getSelectedRow());
        else if (answer == 2) payOccupationalHealthExam(employee, history, table.getSelectedRow());
        else if (answer == 3) openOccupationalHealthCompliance();
    }

    /**
     * Abre o comprovativo digitalizado do exame seleccionado.
     *
     * <p>Até esta versão o ficheiro era anexado e nunca mais saía — não havia por onde. Agora sai,
     * e por isso passa a sair pela porta certa: o servidor exige gestor/admin, decifra-o e regista
     * quem o abriu.
     */
    void openOccupationalHealthAttachment(List<OccupationalHealthExamDTO> history, int row) {
        if (row < 0 || row >= history.size()) {
            JOptionPane.showMessageDialog(owner, "Seleccione na lista o exame cujo comprovativo quer abrir.",
                    "Abrir comprovativo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        OccupationalHealthExamDTO exam = history.get(row);
        if (!exam.hasAttachment()) {
            JOptionPane.showMessageDialog(owner, "Este exame não tem comprovativo digitalizado.",
                    "Sem comprovativo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        UIHelper.runWithProgress(owner, "A abrir comprovativo…",
                () -> owner.hrApiClient.getOccupationalHealthAttachment(exam.id()),
                attachment -> {
                    try {
                        String name = attachment.fileName() == null ? "comprovativo" : attachment.fileName();
                        int dot = name.lastIndexOf('.');
                        java.io.File file = java.io.File.createTempFile("multicore-exame-",
                                dot > 0 ? name.substring(dot) : ".pdf");
                        file.deleteOnExit();
                        Files.write(file.toPath(), attachment.content());
                        Desktop.getDesktop().open(file);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(owner,
                                "Não foi possível abrir o comprovativo: " + ex.getMessage(),
                                "Comprovativo", JOptionPane.ERROR_MESSAGE);
                    }
                }, owner::showActionError);
    }

    /**
     * Paga o exame à clínica. O encargo é do empregador — a saída é de tesouraria e não tem
     * contrapartida nenhuma na folha do trabalhador.
     */
    void payOccupationalHealthExam(EmployeeDTO employee,
                                           List<OccupationalHealthExamDTO> history, int row) {
        if (row < 0 || row >= history.size()) {
            JOptionPane.showMessageDialog(owner,
                    "Seleccione na lista o exame cuja factura vai pagar.", "Registar pagamento",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        OccupationalHealthExamDTO exam = history.get(row);
        if (exam.cost() == null) {
            JOptionPane.showMessageDialog(owner,
                    "Este exame não tem custo registado. Registe a factura da clínica antes de pagar.",
                    "Sem custo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (exam.paid()) {
            JOptionPane.showMessageDialog(owner,
                    "Este exame já foi pago em " + exam.paidAt().format(DATE_FMT) + ".",
                    "Já pago", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(owner, String.format(
                "Pagar %s a %s pelo exame de %s?%nA saída sai da tesouraria como encargo da empresa.",
                money(exam.cost()), exam.providerName() == null ? "prestador não identificado"
                        : exam.providerName(), employee.name()),
                "Confirmar pagamento", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        UIHelper.runWithProgress(owner, "A pagar exame ocupacional…",
                () -> owner.hrApiClient.payOccupationalHealthExam(exam.id()),
                ignored -> JOptionPane.showMessageDialog(owner,
                        "Pagamento registado na tesouraria.", "Saúde Ocupacional",
                        JOptionPane.INFORMATION_MESSAGE), owner::showActionError);
    }

    /**
     * Conformidade da empresa, não de uma pessoa: quem está no activo <b>sem exame nenhum</b> e o
     * que a saúde ocupacional custou este ano, por prestador.
     *
     * <p>Vive dentro deste diálogo e não num separador próprio porque a barra do RH já está no
     * limite que a {@code TabStripFitsTest} mede — um separador que não cabe não avisa, desaparece.
     */
    void openOccupationalHealthCompliance() {
        LocalDate from = LocalDate.now().withDayOfYear(1);
        LocalDate to = LocalDate.now();
        UIHelper.runWithProgress(owner, "A apurar conformidade e custos…",
                () -> new HealthCompliance(owner.hrApiClient.getEmployeesMissingHealthExam(),
                        owner.hrApiClient.getOccupationalHealthCosts(from, to)),
                this::showOccupationalHealthCompliance, owner::showActionError);
    }

    private void showOccupationalHealthCompliance(HealthCompliance snapshot) {
        DefaultTableModel missing = new DefaultTableModel(
                new String[]{"Trabalhador", "Função", "Departamento", "Admissão", "Dias sem exame"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (MissingHealthExamDTO item : snapshot.missing()) {
            missing.addRow(new Object[]{item.employeeName(),
                    item.role() == null ? "—" : item.role(),
                    item.department() == null ? "—" : item.department(),
                    item.hireDate() == null ? "—" : item.hireDate().format(DATE_FMT),
                    item.daysSinceHire() == null ? "Admissão por registar" : item.daysSinceHire()});
        }
        DefaultTableModel costs = new DefaultTableModel(
                new String[]{"Prestador", "Exames", "Total", "Por pagar"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        for (OccupationalHealthProviderCostDTO line : snapshot.costs().byProvider()) {
            costs.addRow(new Object[]{line.providerName(), line.examCount(),
                    money(line.total()), money(line.pending())});
        }

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sem exame (" + snapshot.missing().size() + ")",
                UIHelper.icon("fas-exclamation-triangle", 14), complianceTable(missing,
                        "Trabalhadores no activo que nunca fizeram exame de aptidão. Quem nunca fez "
                                + "não aparece nos avisos de validade — é aqui que aparece."));
        tabs.addTab("Custos do ano", UIHelper.icon("fas-coins", 14), complianceTable(costs,
                String.format("De %s a %s · %d exame(s) com custo · total %s · por pagar %s. "
                                + "O exame de aptidão é encargo do empregador, nunca do trabalhador.",
                        snapshot.costs().from().format(DATE_FMT), snapshot.costs().to().format(DATE_FMT),
                        snapshot.costs().examCount(), money(snapshot.costs().total()),
                        money(snapshot.costs().pending()))));

        JOptionPane.showMessageDialog(owner, tabs, "Saúde Ocupacional — Conformidade e Custos",
                JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel complianceTable(DefaultTableModel model, String caption) {
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(860, 300));
        JLabel label = new JLabel("<html><body style=\"width:820px\">" + caption + "</body></html>");
        label.setForeground(UIHelper.TEXT_MUTED);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        panel.add(label, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    void openOccupationalHealthForm(EmployeeDTO employee) {
        UIHelper.runWithProgress(owner, "A carregar prestadores…",
                () -> owner.hrApiClient.getHealthProviders(),
                providers -> showOccupationalHealthForm(employee, providers), owner::showActionError);
    }

    private void showOccupationalHealthForm(EmployeeDTO employee, List<HealthProviderDTO> providers) {
        JTextField cardField = new JTextField();
        DateField examDate = new DateField(LocalDate.now());
        DateField expiryDate = new DateField(LocalDate.now().plusYears(1));
        JComboBox<String> resultCombo = new JComboBox<>(FITNESS_LABELS);
        JComboBox<String> providerCombo = new JComboBox<>();
        providerCombo.addItem(NO_PROVIDER);
        for (HealthProviderDTO provider : providers) providerCombo.addItem(provider.name());
        JTextField clinicField = new JTextField();
        JTextField doctorField = new JTextField();
        JTextField restrictionsField = new JTextField();
        JTextField notesField = new JTextField();
        MoneyField costField = new MoneyField();
        JTextField invoiceField = new JTextField();
        for (JTextField field : new JTextField[]{cardField, clinicField, doctorField, restrictionsField,
                notesField, invoiceField}) {
            UIHelper.styleTextField(field);
        }
        UIHelper.styleComboBox(resultCombo);
        UIHelper.styleComboBox(providerCombo);

        final byte[][] attachment = {null};
        final String[] attachmentName = {null};
        JLabel attachmentLabel = new JLabel("Nenhum comprovativo seleccionado");
        attachmentLabel.setForeground(UIHelper.TEXT_MUTED);
        ModernButton attachmentButton = UIHelper.createSecondaryButton("Anexar comprovativo…");
        attachmentButton.setIcon(UIHelper.icon("fas-paperclip", 14));
        attachmentButton.addActionListener(event -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Escolher comprovativo do exame");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "PDF ou imagem", "pdf", "jpg", "jpeg", "png"));
            if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) return;
            try {
                byte[] data = Files.readAllBytes(chooser.getSelectedFile().toPath());
                if (data.length > 5_000_000) throw new IllegalArgumentException("O ficheiro excede 5 MB.");
                attachment[0] = data;
                attachmentName[0] = chooser.getSelectedFile().getName();
                attachmentLabel.setText(attachmentName[0]);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(owner, ex.getMessage(), "Comprovativo inválido",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        JPanel attachmentPanel = new JPanel(new BorderLayout(8, 0));
        attachmentPanel.setOpaque(false);
        attachmentPanel.add(attachmentButton, BorderLayout.WEST);
        attachmentPanel.add(attachmentLabel, BorderLayout.CENTER);

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
                // Curta de propósito: o subtítulo do diálogo é de uma linha e trunca. A frase que
                // estava aqui cortava em "a lei não permite ao empreg…" — desaparecia exactamente
                // a parte que diz o que não se pode escrever.
                "Registe apenas aptidão e restrições de função: a lei não permite guardar "
                        + "diagnóstico. A renovação preserva o histórico.", form)
                .setSize(860, 760).showDialog();
        if (!confirmed) return;

        int providerIndex = providerCombo.getSelectedIndex();
        Long providerId = providerIndex <= 0 ? null : providers.get(providerIndex - 1).id();
        BigDecimal cost;
        try {
            cost = costField.optionalValue();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(owner, "Introduza um custo válido para o exame.",
                    "Custo inválido", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SaveOccupationalHealthExamRequest request = new SaveOccupationalHealthExamRequest(
                employee.id(), blank(cardField.getText()), examDate.value(), expiryDate.value(),
                FITNESS_RESULTS[resultCombo.getSelectedIndex()], providerId, blank(clinicField.getText()),
                blank(doctorField.getText()), blank(restrictionsField.getText()), blank(notesField.getText()),
                cost, blank(invoiceField.getText()), attachmentName[0], attachment[0]);
        UIHelper.runWithProgress(owner, "A registar exame ocupacional…",
                () -> owner.hrApiClient.registerOccupationalHealthExam(request),
                ignored -> JOptionPane.showMessageDialog(owner,
                        "Exame ocupacional registado. O histórico anterior foi preservado.",
                        "Saúde Ocupacional", JOptionPane.INFORMATION_MESSAGE), owner::showActionError);
    }

    private static String money(BigDecimal value) {
        return String.format("%,.2f MT", value == null ? BigDecimal.ZERO : value);
    }

    private String healthSituation(OccupationalHealthExamDTO exam) {
        return switch (exam.validityStatus()) {
            case "EXPIRED" -> "Expirado há " + Math.abs(exam.daysUntilExpiry()) + " dia(s)";
            case "EXPIRING" -> "Renovar em " + exam.daysUntilExpiry() + " dia(s)";
            default -> "Válido";
        };
    }

    private String fitnessLabel(String value) {
        return switch (value) {
            case "FIT" -> "Apto";
            case "FIT_WITH_RESTRICTIONS" -> "Apto com restrições";
            case "UNFIT" -> "Inapto";
            default -> value;
        };
    }

    /** As duas leituras da conformidade numa só ida ao servidor. */
    private record HealthCompliance(List<MissingHealthExamDTO> missing, OccupationalHealthCostDTO costs) {}

    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    // ─── §B2 (RHC-25): justificar uma falta ───────────────────────────────────

    /**
     * Justificar muda o <b>tipo</b> da falta, com motivo obrigatório. É aqui que se decide se a
     * ausência desconta: uma falta nascida do fecho do ponto nasce por justificar justamente para
     * que essa decisão seja de alguém, e fique com nome.
     */
    void justifyAbsence() {
        AbsenceDTO absence = absenceSelection.get();
        if (absence == null) {
            return;
        }
        JComboBox<String> typeCombo = new JComboBox<>(ABSENCE_TYPE_LABELS);
        UIHelper.styleComboBox(typeCombo);
        JTextField reasonField = new JTextField();
        UIHelper.styleTextField(reasonField);
        JCheckBox documentBox = new JCheckBox("Com documento comprovativo");
        documentBox.setOpaque(false);
        documentBox.setForeground(UIHelper.TEXT_LIGHT);

        JPanel form = UIHelper.createDialogForm(
                "Passa a ser:", typeCombo,
                "Motivo:", reasonField,
                "Documento:", documentBox);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Justificar Falta",
                "fas-user-check",
                String.format("%s — %s (%d dia(s)), hoje %s", absence.employeeName(),
                        absence.startDate().format(DATE_FMT), absence.totalDays(),
                        absence.absenceType()), form).showDialog();
        if (!confirmed) {
            return;
        }
        String reason = reasonField.getText().trim();
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(owner, "Justificar uma falta exige um motivo.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String type = ABSENCE_TYPES[typeCombo.getSelectedIndex()];
        boolean hasDocument = documentBox.isSelected();
        UIHelper.runWithProgress(owner, "A justificar falta…",
                () -> owner.hrApiClient.justifyAbsence(absence.id(), type, reason, hasDocument),
                ignored -> afterAbsenceChange.run(), owner::showActionError);
    }
}
