package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.CollectionRadarItemDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionRadarSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionReminderTemplateDTO;
import mz.multicore.erp.modules.comercial.model.CollectionRadarBucket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Diálogo executivo para o Radar Inteligente de Cobranças e Lembretes Preventivos.
 * Ver {@code docs/RADAR_COBRANCAS_PREVENTIVO_SPEC.md}.
 */
public class CollectionRadarDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,##0.00 MT");

    public static void show(Window parent, ComercialApiClient api) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setOpaque(false);
        root.setPreferredSize(new Dimension(980, 580));

        // 1. Grid de 5 KPIs corporativos canónicos (STANDARD_CARD_HEIGHT = 96px)
        JLabel critVal = new JLabel("0.00 MT");
        JLabel critSub = new JLabel("0 faturas");
        ModernPanel critCard = KpiCard.createCard("Crítico (>15d)", critVal, null, critSub, "fas-exclamation-triangle", UIHelper.REJECTED_RED);

        JLabel recVal = new JLabel("0.00 MT");
        JLabel recSub = new JLabel("0 faturas");
        ModernPanel recCard = KpiCard.createCard("Recente (1–15d)", recVal, null, recSub, "fas-clock", UIHelper.ACCENT_ORANGE);

        JLabel todayVal = new JLabel("0.00 MT");
        JLabel todaySub = new JLabel("0 faturas");
        ModernPanel todayCard = KpiCard.createCard("Vencem Hoje", todayVal, null, todaySub, "fas-calendar-day", UIHelper.PENDING_YELLOW);

        JLabel in7Val = new JLabel("0.00 MT");
        JLabel in7Sub = new JLabel("0 faturas");
        ModernPanel in7Card = KpiCard.createCard("A Vencer (7d)", in7Val, null, in7Sub, "fas-bell", UIHelper.ACCENT_BLUE);

        JLabel totalVal = new JLabel("0.00 MT");
        JLabel totalSub = new JLabel("0 faturas");
        ModernPanel totalCard = KpiCard.createCard("Total em Aberto", totalVal, null, totalSub, "fas-money-bill-wave", UIHelper.APPROVED_GREEN);

        JPanel kpiGrid = KpiCard.createGrid(5, 8, 8);
        kpiGrid.add(critCard);
        kpiGrid.add(recCard);
        kpiGrid.add(todayCard);
        kpiGrid.add(in7Card);
        kpiGrid.add(totalCard);
        root.add(kpiGrid, BorderLayout.NORTH);

        // 2. Card com tabela e filtros
        ModernPanel tableCard = new ModernPanel(16);
        tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        String[] cols = {"ID", "Nº Fatura", "Cliente", "NUIT", "Emissão", "Vencimento", "Saldo em Dívida", "Dias", "Horizonte"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);
        table.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(8).setCellRenderer(TableCellRenderers.status());
        UIHelper.ensureHeadersFit(table);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        JTextField searchField = TableFilter.searchField("Nº fatura, cliente ou NUIT...");
        searchField.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));

        JComboBox<String> bucketCombo = TableFilter.combo(
                "Todos os horizontes",
                CollectionRadarBucket.CRITICAL_OVERDUE.label(),
                CollectionRadarBucket.RECENT_OVERDUE.label(),
                CollectionRadarBucket.DUE_TODAY.label(),
                CollectionRadarBucket.DUE_IN_7_DAYS.label(),
                CollectionRadarBucket.DUE_IN_15_DAYS.label(),
                CollectionRadarBucket.DUE_IN_30_DAYS.label()
        );
        bucketCombo.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT));
        UIHelper.styleComboBox(bucketCombo);

        TableFilter.install(table, searchField,
                List.of(new TableFilter.ColumnFilter(bucketCombo, 8)),
                List.of());

        List<CollectionRadarItemDTO> radarItems = new ArrayList<>();

        Runnable loadData = () -> {
            UIHelper.runWithProgress(tableCard, "A analisar radar de cobranças...", () -> {
                CollectionRadarSummaryDTO summary = api.getCollectionRadarSummary(companyId);
                List<CollectionRadarItemDTO> items = api.getCollectionRadarItems(companyId, null);
                return new Object[]{summary, items};
            }, result -> {
                Object[] arr = (Object[]) result;
                CollectionRadarSummaryDTO sum = (CollectionRadarSummaryDTO) arr[0];
                @SuppressWarnings("unchecked")
                List<CollectionRadarItemDTO> itms = (List<CollectionRadarItemDTO>) arr[1];

                if (sum != null) {
                    critVal.setText(CURRENCY_FMT.format(sum.criticalOverdueTotal()));
                    critSub.setText(sum.criticalOverdueCount() + " faturas");

                    recVal.setText(CURRENCY_FMT.format(sum.recentOverdueTotal()));
                    recSub.setText(sum.recentOverdueCount() + " faturas");

                    todayVal.setText(CURRENCY_FMT.format(sum.dueTodayTotal()));
                    todaySub.setText(sum.dueTodayCount() + " faturas");

                    in7Val.setText(CURRENCY_FMT.format(sum.dueIn7DaysTotal()));
                    in7Sub.setText(sum.dueIn7DaysCount() + " faturas");

                    totalVal.setText(CURRENCY_FMT.format(sum.totalReceivable()));
                    totalSub.setText(sum.totalInvoicesCount() + " faturas");
                }

                radarItems.clear();
                radarItems.addAll(itms);
                model.setRowCount(0);
                for (CollectionRadarItemDTO item : itms) {
                    String daysText = item.daysUntilDue() < 0
                            ? Math.abs(item.daysUntilDue()) + "d atraso"
                            : (item.daysUntilDue() == 0 ? "Hoje" : item.daysUntilDue() + "d");

                    model.addRow(new Object[]{
                            item.invoiceId(),
                            item.invoiceNumber(),
                            item.clientName(),
                            item.clientNuit(),
                            item.issueDate() != null ? item.issueDate().format(DATE_FMT) : "-",
                            item.dueDate() != null ? item.dueDate().format(DATE_FMT) : "-",
                            item.remainingBalance(),
                            daysText,
                            item.bucket() != null ? item.bucket().label() : "-"
                    });
                }
            }, err -> ToastManager.show(tableCard, FeedbackType.ERROR, "Erro ao carregar radar: " + err.getMessage()));
        };

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 13));
        refreshBtn.addActionListener(e -> loadData.run());

        ModernButton reminderBtn = UIHelper.createSuccessButton("Gerar Lembrete");
        reminderBtn.setIcon(UIHelper.icon("fas-paper-plane", 13));
        reminderBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                ToastManager.show(tableCard, FeedbackType.WARNING, "Seleccione uma fatura na tabela.");
                return;
            }
            int modelRow = table.convertRowIndexToModel(row);
            Long invId = (Long) model.getValueAt(modelRow, 0);
            showReminderDialog(parent, api, invId);
        });

        JPanel filterBar = TableFilter.bar(
                searchField,
                TableFilter.label("Horizonte:"), bucketCombo,
                refreshBtn,
                reminderBtn
        );

        tableCard.add(UIHelper.tableCardTop("Faturas no Radar de Cobrança e Prevenção", filterBar), BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);
        tableCard.add(ClientTablePagination.install(table), BorderLayout.SOUTH);

        root.add(tableCard, BorderLayout.CENTER);

        ModernFormDialog dialog = new ModernFormDialog(parent, "Radar Inteligente de Cobranças",
                "fas-satellite-dish", "Monitorização preditiva e lembretes preventivos de vencimento", root);

        UIHelper.applyStandardModalBounds(dialog.getDialog(), 1000, 610);
        dialog.setOnSave(dialog::close);

        loadData.run();
        dialog.showDialog();
    }

    private static void showReminderDialog(Window parent, ComercialApiClient api, Long invoiceId) {
        JComboBox<String> channelCombo = new JComboBox<>(new String[]{"WhatsApp", "Email", "SMS"});
        UIHelper.styleComboBox(channelCombo);

        JTextField subjectField = new JTextField();
        UIHelper.styleTextField(subjectField);
        subjectField.setEditable(false);

        JTextArea messageArea = new JTextArea(10, 45);
        messageArea.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBackground(UIHelper.FIELD_BG);
        messageArea.setForeground(UIHelper.TEXT_LIGHT);
        messageArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER),
                new EmptyBorder(8, 8, 8, 8)
        ));

        JScrollPane msgScroll = new JScrollPane(messageArea);
        UIHelper.styleScrollPane(msgScroll);

        Runnable fetchTemplate = () -> {
            String ch = (String) channelCombo.getSelectedItem();
            String norm = "WhatsApp".equals(ch) ? "WHATSAPP" : ("Email".equals(ch) ? "EMAIL" : "SMS");
            UIHelper.runWithProgress(parent, "A gerar modelo de mensagem...", () ->
                    api.getCollectionReminderTemplate(invoiceId, norm),
                    dto -> {
                        subjectField.setText(dto.subject() != null ? dto.subject() : "");
                        messageArea.setText(dto.message() != null ? dto.message() : "");
                        messageArea.setCaretPosition(0);
                    },
                    err -> ToastManager.show(parent, FeedbackType.ERROR, "Erro ao carregar modelo: " + err.getMessage())
            );
        };

        channelCombo.addActionListener(e -> fetchTemplate.run());

        JPanel form = new JPanel(new BorderLayout(0, 10));
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(560, 380));

        JPanel topFields = new JPanel(new GridLayout(2, 1, 0, 8));
        topFields.setOpaque(false);

        JPanel chPanel = new JPanel(new BorderLayout(8, 0));
        chPanel.setOpaque(false);
        JLabel chLbl = new JLabel("Canal de Comunicação:");
        chLbl.setForeground(UIHelper.TEXT_MUTED);
        chPanel.add(chLbl, BorderLayout.WEST);
        chPanel.add(channelCombo, BorderLayout.CENTER);
        topFields.add(chPanel);

        JPanel subPanel = new JPanel(new BorderLayout(8, 0));
        subPanel.setOpaque(false);
        JLabel subLbl = new JLabel("Assunto:");
        subLbl.setForeground(UIHelper.TEXT_MUTED);
        subPanel.add(subLbl, BorderLayout.WEST);
        subPanel.add(subjectField, BorderLayout.CENTER);
        topFields.add(subPanel);

        form.add(topFields, BorderLayout.NORTH);
        form.add(msgScroll, BorderLayout.CENTER);

        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsRow.setOpaque(false);
        ModernButton copyBtn = UIHelper.createSecondaryButton("Copiar Mensagem");
        copyBtn.setIcon(UIHelper.icon("fas-copy", 13));
        copyBtn.addActionListener(e -> {
            String text = messageArea.getText();
            if (text != null && !text.isBlank()) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
                ToastManager.success(parent, "Mensagem copiada para a área de transferência!");
            }
        });
        actionsRow.add(copyBtn);
        form.add(actionsRow, BorderLayout.SOUTH);

        fetchTemplate.run();

        ModernFormDialog reminderDialog = new ModernFormDialog(parent, "Lembrete de Cobrança / Regularização",
                "fas-paper-plane", "Modelo pré-formatado pronto para envio", form);
        UIHelper.applyStandardModalBounds(reminderDialog.getDialog(), 600, 480);
        reminderDialog.setOnSave(reminderDialog::close);
        reminderDialog.showDialog();
    }
}
