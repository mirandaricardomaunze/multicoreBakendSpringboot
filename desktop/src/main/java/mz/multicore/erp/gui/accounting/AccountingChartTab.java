package mz.multicore.erp.gui.accounting;

import mz.multicore.erp.desktop.client.AccountingApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.accounting.dto.AccountDTO;
import mz.multicore.erp.modules.accounting.dto.SaveAccountRequest;
import mz.multicore.erp.modules.accounting.model.AccountNature;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Aba de Gestão do Plano de Contas (PGC-NIRF).
 */
public final class AccountingChartTab extends JPanel {

    private final AccountingApiClient accountingApiClient;
    private final InlineFeedbackPanel feedback;
    private final DefaultTableModel accountsModel = AccountingUiHelper.createTableModel(
            "Código", "Nome", "Classe", "Natureza", "Movimentável"
    );

    private final JLabel emptyChartWarning = UIHelper.createWarningBanner(
            "Esta empresa não tem plano de contas. Enquanto assim for, as faturas, os recibos e os "
                    + "salários pagos NÃO são escriturados — e não fica registo de que foram "
                    + "saltados. Use \"Repor Contas em Falta\" para criar o plano.");

    public AccountingChartTab(AccountingApiClient accountingApiClient, InlineFeedbackPanel feedback) {
        this.accountingApiClient = accountingApiClient;
        this.feedback = feedback;
        setLayout(new BorderLayout());
        setOpaque(false);

        JTable table = AccountingUiHelper.styledTable(accountsModel, false);
        ModernPanel card = AccountingUiHelper.createTableCard(table);

        ModernButton seed = UIHelper.createSecondaryButton("Repor Contas em Falta");
        seed.setIcon(UIHelper.icon("fas-seedling", 14));
        seed.addActionListener(e -> seedChart());

        ModernButton create = UIHelper.createSuccessButton("Nova Conta");
        create.setIcon(UIHelper.icon("fas-plus", 14));
        create.addActionListener(e -> openAccountDialog());

        ModernButton refresh = UIHelper.createRefreshButton(() -> loadAccounts());

        JPanel chartToolbar = UIHelper.tableCardTop("Plano de Contas", null, refresh, create, seed);
        JPanel chartNorth = new JPanel(new BorderLayout(0, 8));
        chartNorth.setOpaque(false);
        chartNorth.setBorder(new EmptyBorder(0, 0, 10, 0));
        chartNorth.add(emptyChartWarning, BorderLayout.NORTH);
        chartNorth.add(chartToolbar, BorderLayout.CENTER);
        card.add(chartNorth, BorderLayout.NORTH);

        add(AccountingUiHelper.wrapTabCard(card), BorderLayout.CENTER);
    }

    public void onPanelSelected() {
        loadAccounts();
    }

    public void loadAccounts() {
        UIHelper.loadAsync(this, accountingApiClient::getAccounts, accounts -> {
            accountsModel.setRowCount(0);
            for (AccountDTO account : accounts) {
                accountsModel.addRow(new Object[]{
                        account.code(), account.name(), account.classLabel(),
                        account.nature() == null ? "-" : account.nature().name(),
                        account.postable() ? "Sim" : "Não (conta-mãe)"});
            }
            emptyChartWarning.setVisible(accounts.isEmpty());
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar o plano de contas", error.getMessage(), null, null));
    }

    private void seedChart() {
        UIHelper.runWithProgress(this, "A semear o plano de contas…",
                accountingApiClient::seedChart,
                created -> {
                    ToastManager.success(this, created == 0
                            ? "Esta empresa já tem plano de contas; nada foi alterado."
                            : created + " contas criadas no plano PGC-NIRF.");
                    loadAccounts();
                },
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível semear o plano de contas", error.getMessage(), null, null));
    }

    private void openAccountDialog() {
        JTextField code = new JTextField();
        JTextField name = new JTextField();
        UIHelper.styleTextField(code);
        UIHelper.styleTextField(name);
        JComboBox<String> nature = new JComboBox<>(new String[]{"DEVEDORA", "CREDORA"});
        UIHelper.styleComboBox(nature);
        JCheckBox postable = new JCheckBox("Aceita lançamentos (conta folha)", true);
        postable.setOpaque(false);
        postable.setForeground(UIHelper.TEXT_LIGHT);

        FormField codeForm = new FormField("Código", code, true, "A classe vem do 1.º dígito (PGC-NIRF).");
        FormField nameForm = new FormField("Nome", name, true, null);
        JPanel form = UIHelper.createDialogForm("", codeForm, "", nameForm,
                "Natureza:", nature, "", postable);

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Nova Conta",
                "fas-plus", "Conta do plano da empresa activa", form);
        dialog.setOnSaveAsync(() -> {
            if (!(codeForm.validateRequired() & nameForm.validateRequired())) {
                throw new IllegalArgumentException("Corrija os campos assinalados.");
            }
            SaveAccountRequest request = new SaveAccountRequest(
                    code.getText().trim(), name.getText().trim(),
                    AccountNature.valueOf(String.valueOf(nature.getSelectedItem())),
                    postable.isSelected());
            return () -> accountingApiClient.createAccount(request);
        });
        if (dialog.showDialog()) loadAccounts();
    }
}
