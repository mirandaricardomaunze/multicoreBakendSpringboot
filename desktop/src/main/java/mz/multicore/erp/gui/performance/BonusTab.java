package mz.multicore.erp.gui.performance;

import mz.multicore.erp.desktop.client.PerformanceApiClient;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.performance.dto.AdjustBonusRequest;
import mz.multicore.erp.modules.performance.dto.ApproveBonusRequest;
import mz.multicore.erp.modules.performance.dto.SalesGoalBonusDTO;
import mz.multicore.erp.modules.performance.model.BonusStatus;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BonusTab extends JPanel {

    private final PerformanceApiClient apiClient;
    private final DesktopSession session;

    private JTable table;
    private DefaultTableModel tableModel;
    private List<SalesGoalBonusDTO> loadedBonuses = new ArrayList<>();

    private JComboBox<String> statusFilter;
    private InlineFeedbackPanel feedbackPanel;


    public BonusTab(PerformanceApiClient apiClient, DesktopSession session) {
        this.apiClient = apiClient;
        this.session = session;

        setLayout(new BorderLayout(0, 8));
        setBackground(UIHelper.BG_DARK);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        buildUI();
    }

    private void buildUI() {
        feedbackPanel = new InlineFeedbackPanel();
        add(feedbackPanel, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Toolbar inside card
        JPanel filterBar = new JPanel(new BorderLayout(10, 0));
        filterBar.setOpaque(false);

        JPanel filtersLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filtersLeft.setOpaque(false);

        statusFilter = PerformanceControls.createSelect(
                new String[]{"Todos", "Pendentes", "Aprovados", "Pagos", "Cancelados"},
                PerformanceControls.FILTER_SELECT_WIDTH
        );
        statusFilter.addActionListener(e -> reload());
        filtersLeft.add(PerformanceControls.createFilterGroup("Estado:", statusFilter));

        ModernButton btnRefresh = UIHelper.createSecondaryButton("Recarregar");
        btnRefresh.setIcon(UIHelper.icon("fas-sync-alt", 14));
        btnRefresh.addActionListener(e -> reload());

        ModernButton btnApprove = UIHelper.createSuccessButton("Aprovar Prémio");
        btnApprove.setIcon(UIHelper.icon("fas-check", 14));
        btnApprove.addActionListener(e -> approveSelectedBonus());

        ActionMenuButton actionsMenu = UIHelper.createActionMenuButton("Mais Ações");
        actionsMenu.addAction("Ajustar Prémio", UIHelper.icon("fas-edit", 12), this::adjustSelectedBonus);
        actionsMenu.addAction("Integrar na Folha", UIHelper.icon("fas-file-invoice-dollar", 12), this::integrateSelectedBonus);

        JPanel actionsRight = UIHelper.actionsBar(btnRefresh, actionsMenu, btnApprove);

        filterBar.add(filtersLeft, BorderLayout.WEST);
        filterBar.add(actionsRight, BorderLayout.EAST);

        card.add(filterBar, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Meta Comercial", "Colaborador", "Calculado (MZN)", "Aprovado (MZN)", "Justificação", "Estado", "Aprovador", "Recibo ID"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(tableModel);
        UIHelper.styleTable(table);

        JScrollPane scrollPane = new JScrollPane(table);
        UIHelper.styleScrollPane(scrollPane);
        TableContextMenu.install(scrollPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER));
        card.add(scrollPane, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);
    }

    public void reload() {
        feedbackPanel.clear();
        Long companyId = session.activeCompanyId();
        if (companyId == null) return;

        BonusStatus status = switch (statusFilter.getSelectedIndex()) {
            case 1 -> BonusStatus.PENDING;
            case 2 -> BonusStatus.APPROVED;
            case 3 -> BonusStatus.PAID;
            case 4 -> BonusStatus.CANCELLED;
            default -> null;
        };

        UIHelper.loadAsync(
                this,
                () -> apiClient.getBonuses(companyId, status),
                bonuses -> {
                    loadedBonuses = bonuses;
                    tableModel.setRowCount(0);
                    for (SalesGoalBonusDTO b : bonuses) {
                        tableModel.addRow(new Object[]{
                                b.id(),
                                b.goalName(),
                                b.employeeName() != null ? b.employeeName() : "Equipa",
                                UIHelper.formatMzn(b.calculatedAmount() != null ? b.calculatedAmount() : BigDecimal.ZERO),
                                UIHelper.formatMzn(b.approvedAmount() != null ? b.approvedAmount() : BigDecimal.ZERO),
                                b.justification() != null ? b.justification() : "-",
                                humanStatus(b.status()),
                                b.approvedBy() != null ? b.approvedBy() : "-",
                                b.payslipId() != null ? "#" + b.payslipId() : "-"
                        });
                    }
                },
                ex -> feedbackPanel.show(FeedbackType.ERROR, "Erro ao carregar prémios: " + ex.getMessage())
        );
    }

    private void approveSelectedBonus() {
        SalesGoalBonusDTO selected = getSelected();
        if (selected == null) return;

        if (selected.status() == BonusStatus.PAID) {
            feedbackPanel.show(FeedbackType.WARNING, "Este prémio já se encontra pago e não pode ser re-aprovado.");
            return;
        }

        Window win = SwingUtilities.getWindowAncestor(this);
        boolean ok = ModernMessageDialog.confirm(
                win,
                FeedbackType.INFO,
                "Aprovar Prémio Comercial",
                String.format("Confirmar aprovação do prémio de %s MZN para %s?",
                        UIHelper.formatMzn(selected.calculatedAmount()), selected.employeeName()),
                "Aprovar"
        );
        if (!ok) return;

        UIHelper.loadAsync(
                this,
                () -> apiClient.approveBonus(selected.id(), new ApproveBonusRequest(selected.calculatedAmount(), "Aprovado via desktop")),
                res -> {
                    ToastManager.success(win, "Prémio aprovado com sucesso!");
                    reload();
                },
                ex -> feedbackPanel.show(FeedbackType.ERROR, "Erro ao aprovar prémio: " + ex.getMessage())
        );
    }

    private void adjustSelectedBonus() {
        SalesGoalBonusDTO selected = getSelected();
        if (selected == null) return;

        if (selected.status() == BonusStatus.PAID) {
            feedbackPanel.show(FeedbackType.WARNING, "Este prémio já se encontra pago e não pode ser ajustado.");
            return;
        }

        Window window = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(window, "Ajustar Prémio Comercial", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(420, 260);
        dialog.setLocationRelativeTo(window);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIHelper.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1.0;

        MoneyField txtAmount = PerformanceControls.createMoneyField(selected.approvedAmount() != null ? selected.approvedAmount().toString() : "", 0);
        form.add(new FormField("Novo Valor Aprovado (MZN)", txtAmount, true, null), gc);

        gc.gridy++;
        JTextField txtJustification = PerformanceControls.createTextInput("", "Justificação do ajuste", 0);
        form.add(new FormField("Justificação do Ajuste", txtJustification, true, null), gc);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(UIHelper.BG_DARK);

        ModernButton btnCancel = UIHelper.createDangerButton("Cancelar");
        btnCancel.setIcon(UIHelper.icon("fas-times", 13, Color.WHITE));
        btnCancel.addActionListener(e -> dialog.dispose());
        footer.add(btnCancel);

        ModernButton btnSave = UIHelper.createPrimaryButton("Guardar Ajuste");
        btnSave.setIcon(UIHelper.icon("fas-check", 14));
        btnSave.addActionListener(e -> {
            try {
                BigDecimal val = txtAmount.value();
                String just = txtJustification.getText().trim();

                UIHelper.loadAsync(
                        (JComponent) dialog.getContentPane(),
                        () -> apiClient.adjustBonus(selected.id(), new AdjustBonusRequest(val, just)),
                        res -> {
                            dialog.dispose();
                            ToastManager.success(window, "Prémio ajustado com sucesso.");
                            reload();
                        },
                        ex -> ModernMessageDialog.show(dialog, FeedbackType.ERROR, "Erro ao ajustar", ex.getMessage())
                );
            } catch (Exception ex) {
                ModernMessageDialog.show(dialog, FeedbackType.ERROR, "Validação", "Indique um valor numérico válido e uma justificação.");
            }
        });
        footer.add(btnSave);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void integrateSelectedBonus() {
        SalesGoalBonusDTO selected = getSelected();
        if (selected == null) return;

        if (selected.status() != BonusStatus.APPROVED) {
            feedbackPanel.show(FeedbackType.WARNING, "Apenas prémios no estado APROVADO podem ser integrados na folha salarial.");
            return;
        }

        Window win = SwingUtilities.getWindowAncestor(this);
        LocalDate now = LocalDate.now();
        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.setOpaque(false);
        JLabel lbl = new JLabel("Indique o ano e mês da folha salarial (formato AAAA/MM):");
        lbl.setForeground(UIHelper.TEXT_LIGHT);
        lbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        form.add(lbl, BorderLayout.NORTH);

        JTextField inputField = new JTextField(now.getYear() + "/" + String.format("%02d", now.getMonthValue()));
        UIHelper.styleTextField(inputField);
        form.add(inputField, BorderLayout.CENTER);

        ModernFormDialog dlg = new ModernFormDialog(win, "Integrar na Folha Salarial", "fas-calendar-alt", form);
        dlg.setConfirmButton("Integrar", "fas-check");
        final String[] result = new String[1];
        dlg.setOnSave(() -> {
            String val = inputField.getText().trim();
            if (val.isBlank()) {
                throw new mz.multicore.erp.architecture.exception.BusinessRuleException("Indique o ano e mês da folha.");
            }
            result[0] = val;
        });
        if (!dlg.showDialog()) return;
        String input = result[0];

        try {
            String[] parts = input.trim().split("/");
            int year = Integer.parseInt(parts[0].trim());
            int month = Integer.parseInt(parts[1].trim());

            UIHelper.loadAsync(
                    this,
                    () -> apiClient.integrateBonusWithPayroll(selected.id(), year, month),
                    res -> {
                        ToastManager.success(win, "Prémio integrado com sucesso no recibo de vencimento!");
                        reload();
                    },
                    ex -> feedbackPanel.show(FeedbackType.ERROR, "Erro na integração: " + ex.getMessage())
            );
        } catch (Exception ex) {
            feedbackPanel.show(FeedbackType.ERROR, "Formato de data inválido. Use AAAA/MM (ex: 2026/10).");
        }
    }

    private SalesGoalBonusDTO getSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= loadedBonuses.size()) {
            feedbackPanel.show(FeedbackType.WARNING, "Selecione um prémio na tabela.");
            return null;
        }
        return loadedBonuses.get(row);
    }

    private String humanStatus(BonusStatus status) {
        if (status == null) return "-";
        return switch (status) {
            case PENDING -> "Pendente";
            case APPROVED -> "Aprovado";
            case PAID -> "Pago";
            case CANCELLED -> "Cancelado";
        };
    }
}
