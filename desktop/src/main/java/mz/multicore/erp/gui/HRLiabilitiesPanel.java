package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DateField;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.HrPolicyConfigDTO;
import mz.multicore.erp.modules.hr.dto.PayrollCostDTO;
import mz.multicore.erp.modules.hr.dto.PayrollLiabilityDTO;
import mz.multicore.erp.modules.hr.dto.SaveHrPolicyConfigRequest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Separador das <b>retenções por entregar</b> — o dinheiro que a empresa reteve e ainda deve ao
 * Estado. Ver docs/RH_COMPLETO_SPEC.md §B5.
 *
 * <p>Este ecrã existe porque, até agora, esse dinheiro não estava em lado nenhum: era calculado,
 * impresso no mapa fiscal e desaparecia. Ficava na conta da empresa <b>indistinguível de dinheiro
 * próprio</b>, e quem o gastasse só descobria o buraco no dia da entrega.
 *
 * <p>É também aqui que vivem os <b>valores legais</b> (§6) — prazos de entrega, dias de férias por
 * antiguidade, aviso prévio. Ficam ao lado das retenções porque é a olhar para uma retenção sem
 * prazo que se percebe que faltam.
 */
final class HRLiabilitiesPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HRPanel owner;

    private DefaultTableModel model;
    private JTable table;
    private List<PayrollLiabilityDTO> liabilities = List.of();
    private JLabel costLabel;
    private JSpinner yearSpinner;
    private JSpinner monthSpinner;

    HRLiabilitiesPanel(HRPanel owner) {
        this.owner = owner;
    }

    JPanel buildPanel() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Retenções por Entregar"), BorderLayout.WEST);

        ModernButton deliverBtn = UIHelper.createSuccessButton("Marcar Entregue");
        deliverBtn.setIcon(UIHelper.icon("fas-check-double", 14));
        deliverBtn.addActionListener(e -> deliver());
        ModernButton accrueBtn = UIHelper.createSecondaryButton("Apurar Período");
        accrueBtn.setIcon(UIHelper.icon("fas-calculator", 14));
        accrueBtn.addActionListener(e -> accrue());
        ModernButton policyBtn = UIHelper.createSecondaryButton("Valores Legais");
        policyBtn.setIcon(UIHelper.icon("fas-balance-scale", 14));
        policyBtn.addActionListener(e -> openPolicyDialog());
        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> load());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        actions.add(policyBtn);
        actions.add(accrueBtn);
        actions.add(deliverBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"ID", "Período", "Retenção", "Valor (MT)", "Prazo", "Estado", "Entregue Por", "Referência"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        JTextField search = TableFilter.searchField("Retenção ou período…");
        JComboBox<String> estado = TableFilter.combo("Todos os estados", "Por entregar", "Entregue");
        TableFilter.install(table, search, List.of(new TableFilter.ColumnFilter(estado, 5)), List.of());
        JPanel bar = TableFilter.bar(search, TableFilter.label("Estado:"), estado);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));

        card.add(bar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(buildCostBar(), BorderLayout.SOUTH);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    /**
     * O <b>custo total do trabalhador</b> ao pé das retenções (RHC-55): base + subsídios + extra +
     * INSS patronal. O patronal é custo da empresa e não aparecia em relatório nenhum — quem olhava
     * para a folha via o ilíquido e pensava que era o que a empresa gasta.
     */
    private JPanel buildCostBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(12, 0, 0, 0));

        LocalDate today = LocalDate.now();
        yearSpinner = new JSpinner(new SpinnerNumberModel(today.getYear(), 2000, 2100, 1));
        yearSpinner.setEditor(new JSpinner.NumberEditor(yearSpinner, "#"));
        monthSpinner = new JSpinner(new SpinnerNumberModel(today.getMonthValue(), 1, 12, 1));

        ModernButton costBtn = UIHelper.createSecondaryButton("Custo do Mês");
        costBtn.setIcon(UIHelper.icon("fas-hand-holding-usd", 14));
        costBtn.addActionListener(e -> loadCost());

        costLabel = new JLabel("Custo total do trabalhador: —");
        costLabel.setForeground(UIHelper.TEXT_LIGHT);

        bar.add(TableFilter.label("Ano:"));
        bar.add(yearSpinner);
        bar.add(TableFilter.label("Mês:"));
        bar.add(monthSpinner);
        bar.add(costBtn);
        bar.add(costLabel);
        return bar;
    }

    void load() {
        UIHelper.loadAsync(owner, owner.hrApiClient::getPayrollLiabilities, this::apply,
                error -> owner.showLoadError("retenções da folha", error));
    }

    private void apply(List<PayrollLiabilityDTO> loaded) {
        liabilities = loaded;
        model.setRowCount(0);
        for (PayrollLiabilityDTO l : liabilities) {
            model.addRow(new Object[]{
                    l.id(),
                    String.format("%02d/%d", l.month(), l.year()),
                    l.liabilityTypeLabel(),
                    l.amount(),
                    dueLabel(l),
                    l.statusLabel(),
                    l.deliveredBy() == null ? "-" : l.deliveredBy(),
                    l.paymentReference() == null ? "-" : l.paymentReference()
            });
        }
    }

    /**
     * Uma retenção sem prazo <b>não fica em branco</b>: diz que o prazo está por configurar. Em
     * branco parecia "sem urgência", que é exactamente a leitura errada.
     */
    private String dueLabel(PayrollLiabilityDTO l) {
        if (l.dueDate() == null) {
            return "Prazo por configurar";
        }
        String date = l.dueDate().format(DATE_FMT);
        return l.overdue() ? date + " (em atraso)" : date;
    }

    private void loadCost() {
        int year = (Integer) yearSpinner.getValue();
        int month = (Integer) monthSpinner.getValue();
        UIHelper.runWithProgress(owner, "A apurar o custo do mês…",
                () -> owner.hrApiClient.getPayrollCost(year, month),
                cost -> costLabel.setText(costText(cost)),
                owner::showActionError);
    }

    private String costText(PayrollCostDTO cost) {
        if (cost == null) {
            return "Custo total do trabalhador: —";
        }
        return String.format("Custo total %02d/%d: %s  (ilíquido %s + INSS patronal %s) · líquido pago %s",
                cost.month(), cost.year(), money(cost.totalCost()),
                money(cost.grossPay()), money(cost.employerInss()),
                money(cost.netPay()));
    }

    private void accrue() {
        int year = (Integer) yearSpinner.getValue();
        int month = (Integer) monthSpinner.getValue();
        UIHelper.runWithProgress(owner, "A apurar retenções…",
                () -> owner.hrApiClient.accruePayrollLiabilities(year, month),
                ignored -> load(), owner::showActionError);
    }

    private void deliver() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            JOptionPane.showMessageDialog(owner, "Selecione uma retenção na tabela.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        PayrollLiabilityDTO sel = liabilities.get(row);
        if (!"POR_ENTREGAR".equals(sel.status())) {
            JOptionPane.showMessageDialog(owner, "Esta retenção já foi entregue.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String reference = JOptionPane.showInputDialog(owner, String.format(
                "Entregar %s de %02d/%d, no valor de %s.\nReferência do pagamento (opcional):",
                sel.liabilityTypeLabel(), sel.month(), sel.year(), money(sel.amount())),
                "Entregar Retenção", JOptionPane.QUESTION_MESSAGE);
        if (reference == null) {
            return; // cancelou
        }
        UIHelper.runWithProgress(owner, "A registar a entrega…",
                () -> owner.hrApiClient.deliverPayrollLiability(sel.id(), reference),
                ignored -> load(), owner::showActionError);
    }

    /**
     * Configuração dos valores legais (§6). Todos os campos são <b>opcionais</b>: uma empresa pode
     * confirmar os prazos com o contabilista esta semana e o direito a férias só no mês que vem.
     * Obrigar a preencher tudo de uma vez levaria a preencher à sorte — que é o que esta
     * configuração existe para evitar.
     */
    private void openPolicyDialog() {
        List<HrPolicyConfigDTO> existing;
        try {
            existing = owner.hrApiClient.getHrPolicies();
        } catch (RuntimeException ex) {
            owner.showActionError(ex);
            return;
        }
        HrPolicyConfigDTO current = existing.stream().filter(HrPolicyConfigDTO::active)
                .findFirst().orElse(null);

        JTextField nameField = new JTextField(current == null ? "Valores legais" : current.name());
        UIHelper.styleTextField(nameField);
        DateField fromField = new DateField(LocalDate.now().withDayOfYear(1));
        JSpinner vac1 = optionalSpinner(current == null ? null : current.vacationDaysYear1(), 0, 365);
        JSpinner vac2 = optionalSpinner(current == null ? null : current.vacationDaysYear2(), 0, 365);
        JSpinner vac3 = optionalSpinner(current == null ? null : current.vacationDaysYear3Plus(), 0, 365);
        JSpinner irpsDay = optionalSpinner(current == null ? null : current.irpsDeliveryDay(), 0, 31);
        JSpinner inssDay = optionalSpinner(current == null ? null : current.inssDeliveryDay(), 0, 31);
        JSpinner noticeEmp = optionalSpinner(current == null ? null : current.noticeDaysEmployee(), 0, 365);
        JSpinner noticeEmpr = optionalSpinner(current == null ? null : current.noticeDaysEmployer(), 0, 365);
        JTextField basisField = new JTextField(current == null ? "" : orEmpty(current.legalBasis()));
        UIHelper.styleTextField(basisField);

        JPanel form = UIHelper.createDialogForm(
                "Nome:", nameField,
                "Vigora a partir de:", fromField,
                "Férias — 1.º ano (0 = por confirmar):", vac1,
                "Férias — 2.º ano:", vac2,
                "Férias — 3.º ano em diante:", vac3,
                "IRPS: dia de entrega do mês seguinte:", irpsDay,
                "INSS: dia de entrega do mês seguinte:", inssDay,
                "Aviso prévio — trabalhador (dias):", noticeEmp,
                "Aviso prévio — empregador (dias):", noticeEmpr,
                "Base legal:", basisField
        );

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Valores Legais de RH",
                "fas-balance-scale",
                "Confirme estes valores com o contabilista. O que ficar a zero é tratado como "
                        + "não configurado — e o sistema di-lo em vez de adivinhar.", form).showDialog();
        if (!confirmed) {
            return;
        }

        SaveHrPolicyConfigRequest request = new SaveHrPolicyConfigRequest(
                nameField.getText().trim(), fromField.value(), null,
                optionalValue(vac1), optionalValue(vac2), optionalValue(vac3),
                optionalValue(irpsDay), optionalValue(inssDay),
                optionalValue(noticeEmp), optionalValue(noticeEmpr),
                basisField.getText().trim().isEmpty() ? null : basisField.getText().trim());
        UIHelper.runWithProgress(owner, "A gravar valores legais…",
                () -> owner.hrApiClient.createHrPolicy(request),
                ignored -> {
                    JOptionPane.showMessageDialog(owner,
                            "Valores legais gravados. As retenções passam a ter prazo a partir do "
                                    + "próximo apuramento.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    load();
                }, owner::showActionError);
    }

    /** Zero significa <b>não configurado</b> — o Swing não tem spinner vazio, e nulo tem de caber. */
    private JSpinner optionalSpinner(Integer value, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(value == null ? 0 : value, min, max, 1));
    }

    private Integer optionalValue(JSpinner spinner) {
        int value = (Integer) spinner.getValue();
        return value <= 0 ? null : value;
    }

    /** Formato de dinheiro do projecto: separador de milhares, duas casas, sufixo MT. */
    private static String money(java.math.BigDecimal value) {
        return value == null ? "—" : String.format("%,.2f MT", value);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
