package mz.multicore.erp.gui.performance;

import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.desktop.client.PerformanceApiClient;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.gui.performance.GoalsTab.BusinessUnitOption;
import mz.multicore.erp.modules.performance.dto.CreateSalesGoalRequest;
import mz.multicore.erp.modules.performance.model.*;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Diálogo modal ergonómico para criação de novas metas comerciais no CDC.
 */
public final class GoalCreateDialog {

    private GoalCreateDialog() {}

    public static void show(Window window, PerformanceApiClient apiClient, DesktopSession session, Runnable onSaved) {
        JDialog dialog = new JDialog(window, "Nova Meta Comercial", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(540, 600);
        dialog.setLocationRelativeTo(window);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UIHelper.BG_CARD);
        form.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0; gc.gridy = 0; gc.weightx = 1.0;

        JTextField txtName = PerformanceControls.createTextInput("", "Ex: Meta Vendas Supermercado", 0);
        form.add(new FormField("Nome da Meta", txtName, true, "Ex: Meta Vendas Supermercado"), gc);

        gc.gridy++;
        JComboBox<GoalPeriod> cbPeriod = PerformanceControls.createSelect(GoalPeriod.values(), 0, PerformanceControls::humanPeriod);
        form.add(new FormField("Periodicidade", cbPeriod, true, null), gc);

        gc.gridy++;
        JPanel dateRow = new JPanel(new GridLayout(1, 2, 8, 0));
        dateRow.setOpaque(false);
        DateField txtStart = PerformanceControls.createDateField(LocalDate.now().withDayOfMonth(1), 0);
        DateField txtEnd = PerformanceControls.createDateField(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()), 0);
        dateRow.add(new FormField("Data Início (AAAA-MM-DD)", txtStart, true, null));
        dateRow.add(new FormField("Data Fim (AAAA-MM-DD)", txtEnd, true, null));
        form.add(dateRow, gc);

        gc.gridy++;
        JComboBox<GoalScope> cbScope = PerformanceControls.createSelect(GoalScope.values(), 0, PerformanceControls::humanScope);
        form.add(new FormField("Escopo da Meta", cbScope, true, null), gc);

        gc.gridy++;
        JComboBox<BusinessUnitOption> cbBusinessUnit = new JComboBox<>();
        cbBusinessUnit.addItem(new BusinessUnitOption(null, "Todas as Unidades (Geral)"));
        try {
            List<mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO> buList = apiClient.getActiveBusinessUnits(session.activeCompanyId());
            if (buList != null) for (var bu : buList) cbBusinessUnit.addItem(new BusinessUnitOption(bu.id(), bu.name()));
        } catch (Exception ignored) {}
        UIHelper.styleComboBox(cbBusinessUnit);
        JPanel buRow = new JPanel(new GridLayout(1, 1));
        buRow.setOpaque(false);
        buRow.add(new FormField("Unidade de Negócio / Módulo", cbBusinessUnit, false, "Selecione o negócio associado"));
        form.add(buRow, gc);

        gc.gridy++;
        JPanel sellerRow = new JPanel(new GridLayout(1, 2, 8, 0));
        sellerRow.setOpaque(false);
        JTextField txtSeller = PerformanceControls.createTextInput("", "Nome ou username do vendedor", 0);
        JTextField txtSupervisor = PerformanceControls.createTextInput("", "Responsável / Supervisor de Vendas", 0);
        sellerRow.add(new FormField("Vendedor / Colaborador", txtSeller, false, "Ex: Carlos Silva ou username"));
        sellerRow.add(new FormField("Responsável de Vendas (Supervisor)", txtSupervisor, false, "Ex: Chefe de Vendas"));
        form.add(sellerRow, gc);

        Runnable updateScopeVisibility = () -> {
            GoalScope selectedScope = (GoalScope) cbScope.getSelectedItem();
            sellerRow.setVisible(selectedScope == GoalScope.EMPLOYEE);
            buRow.setVisible(selectedScope == GoalScope.BUSINESS_UNIT || selectedScope == GoalScope.EMPLOYEE);
            form.revalidate(); form.repaint();
        };
        cbScope.addActionListener(e -> updateScopeVisibility.run());
        updateScopeVisibility.run();

        gc.gridy++;
        JPanel targetsRow = new JPanel(new GridLayout(1, 2, 8, 0));
        targetsRow.setOpaque(false);
        MoneyField txtRevenue = PerformanceControls.createMoneyField("", 0);
        MoneyField txtMargin = PerformanceControls.createMoneyField("", 0);
        targetsRow.add(new FormField("Alvo Receita (MT)", txtRevenue, false, null));
        targetsRow.add(new FormField("Alvo Margem Bruta (MT)", txtMargin, false, null));
        form.add(targetsRow, gc);

        gc.gridy++;
        JCheckBox chkBonusOptional = new JCheckBox("Atribuir Bonificação / Compensação de Vendas (Opcional)", false);
        chkBonusOptional.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        chkBonusOptional.setForeground(UIHelper.PENDING_YELLOW);
        chkBonusOptional.setOpaque(false);
        form.add(chkBonusOptional, gc);

        gc.gridy++;
        JComboBox<BonusType> cbBonusType = PerformanceControls.createSelect(BonusType.values(), 0, PerformanceControls::humanBonusType);
        form.add(new FormField("Tipo de Bónus / Comissão", cbBonusType, false, null), gc);

        gc.gridy++;
        JPanel bonusRow = new JPanel(new GridLayout(1, 2, 8, 0));
        bonusRow.setOpaque(false);
        MoneyField txtBonusValue = PerformanceControls.createMoneyField("", 0);
        MoneyField txtBonusCap = PerformanceControls.createMoneyField("", 0);
        bonusRow.add(new FormField("Valor do Bónus (MZN ou %)", txtBonusValue, false, null));
        bonusRow.add(new FormField("Teto Máximo Bónus (MT)", txtBonusCap, false, null));
        form.add(bonusRow, gc);

        gc.gridy++;
        JCheckBox chkAuto = new JCheckBox("Gerar bónus automaticamente ao atingir a meta", true);
        chkAuto.setOpaque(false);
        form.add(chkAuto, gc);

        Runnable updateBonusFields = () -> {
            boolean enabled = chkBonusOptional.isSelected();
            cbBonusType.setEnabled(enabled);
            txtBonusValue.setEnabled(enabled);
            txtBonusCap.setEnabled(enabled);
            chkAuto.setEnabled(enabled);
        };
        chkBonusOptional.addActionListener(e -> updateBonusFields.run());
        updateBonusFields.run();

        JScrollPane scrollForm = new JScrollPane(form);
        scrollForm.setBorder(BorderFactory.createEmptyBorder());
        scrollForm.getViewport().setBackground(UIHelper.BG_CARD);
        scrollForm.getVerticalScrollBar().setUnitIncrement(14);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(UIHelper.BG_DARK);
        ModernButton btnCancel = ButtonStyles.cancelButton();
        btnCancel.setIcon(UIHelper.icon("fas-times", 13, Color.WHITE));
        btnCancel.addActionListener(e -> dialog.dispose());
        footer.add(btnCancel);

        ModernButton btnSave = UIHelper.createSuccessButton("Criar Meta");
        btnSave.setIcon(UIHelper.icon("fas-check", 14));
        btnSave.addActionListener(e -> saveGoal(dialog, apiClient, session, onSaved, txtName, cbPeriod, txtStart, txtEnd,
                cbScope, cbBusinessUnit, txtSeller, txtSupervisor, txtRevenue, txtMargin, chkBonusOptional, cbBonusType,
                txtBonusValue, txtBonusCap, chkAuto));
        footer.add(btnSave);

        dialog.add(scrollForm, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);
        UIHelper.applyStandardModalBounds(dialog, 540, 600);
        dialog.setVisible(true);
    }

    private static void saveGoal(
            JDialog dialog, PerformanceApiClient apiClient, DesktopSession session, Runnable onSaved,
            JTextField txtName, JComboBox<GoalPeriod> cbPeriod, DateField txtStart, DateField txtEnd,
            JComboBox<GoalScope> cbScope, JComboBox<BusinessUnitOption> cbBusinessUnit, JTextField txtSeller,
            JTextField txtSupervisor, MoneyField txtRevenue, MoneyField txtMargin, JCheckBox chkBonusOptional,
            JComboBox<BonusType> cbBonusType, MoneyField txtBonusValue, MoneyField txtBonusCap, JCheckBox chkAuto
    ) {
        try {
            String name = txtName.getText().trim();
            BigDecimal bonusVal = chkBonusOptional.isSelected() && txtBonusValue.optionalValue() != null ? txtBonusValue.optionalValue() : BigDecimal.ZERO;
            BonusType bonusType = chkBonusOptional.isSelected() ? (BonusType) cbBonusType.getSelectedItem() : BonusType.FIXED;
            GoalScope scope = (GoalScope) cbScope.getSelectedItem();
            Long scopeRefId = null;
            String scopeLabel = null;
            BusinessUnitOption selectedBu = (BusinessUnitOption) cbBusinessUnit.getSelectedItem();

            if (scope == GoalScope.BUSINESS_UNIT) {
                if (selectedBu == null || selectedBu.id() == null) {
                    throw new IllegalArgumentException("Por favor selecione uma Unidade de Negócio para este escopo.");
                }
                scopeRefId = selectedBu.id();
                scopeLabel = "Negócio: " + selectedBu.name();
            } else if (scope == GoalScope.EMPLOYEE) {
                String seller = txtSeller.getText().trim();
                String supervisor = txtSupervisor.getText().trim();
                scopeLabel = seller.isEmpty() ? "Vendedor" : seller;
                if (selectedBu != null && selectedBu.id() != null) {
                    scopeRefId = selectedBu.id();
                    scopeLabel += " • " + selectedBu.name();
                }
                if (!supervisor.isEmpty()) scopeLabel += " (Resp: " + supervisor + ")";
            }

            CreateSalesGoalRequest req = new CreateSalesGoalRequest(
                    session.activeCompanyId(), name, (GoalPeriod) cbPeriod.getSelectedItem(),
                    txtStart.value(), txtEnd.value(), scope, scopeRefId, scopeLabel,
                    txtRevenue.optionalValue(), txtMargin.optionalValue(), null,
                    bonusType, bonusVal, chkBonusOptional.isSelected() ? txtBonusCap.optionalValue() : null,
                    chkBonusOptional.isSelected() && chkAuto.isSelected()
            );

            UIHelper.loadAsync((JComponent) dialog.getContentPane(), () -> apiClient.createGoal(req),
                    created -> {
                        dialog.dispose();
                        ToastManager.success(dialog.getOwner(), "Meta criada com sucesso!");
                        if (onSaved != null) onSaved.run();
                    },
                    ex -> ModernMessageDialog.show(dialog, FeedbackType.ERROR, "Erro ao criar meta", ex.getMessage())
            );
        } catch (Exception ex) {
            ModernMessageDialog.show(dialog, FeedbackType.ERROR, "Validação", "Verifique os campos inseridos: " + ex.getMessage());
        }
    }
}
