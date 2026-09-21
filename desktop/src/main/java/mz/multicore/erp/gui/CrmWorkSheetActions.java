package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.CRMApiClient;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.modules.crm.dto.CreateWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.CrmSettingsDTO;
import mz.multicore.erp.modules.crm.dto.UpdateCrmSettingsRequest;
import mz.multicore.erp.modules.crm.dto.SupportTicketDTO;
import mz.multicore.erp.modules.crm.dto.UpdateWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.VoidWorkSheetRequest;
import mz.multicore.erp.modules.crm.dto.WorkSheetDTO;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.List;
import mz.multicore.erp.gui.components.PrintPreviewDialog;

/** Acções sobre folhas de obra: registar, corrigir, anular, imprimir e faturar. */
final class CrmWorkSheetActions {

    private final CRMPanel owner;
    private final CRMApiClient crmApiClient;

    CrmWorkSheetActions(CRMPanel owner, CRMApiClient crmApiClient) {
        this.owner = owner;
        this.crmApiClient = crmApiClient;
    }

    /** Registo de folha de obra em modal profissional (fecho de ticket). */
    void registerWorkSheet() {
        List<SupportTicketDTO> openTickets = owner.openTickets();
        if (openTickets.isEmpty()) {
            owner.showNotice(FeedbackType.WARNING, "Sem pedidos em aberto",
                    "Não existem pedidos em aberto para registar trabalho.");
            return;
        }

        JComboBox<String> ticketCombo = new JComboBox<>();
        UIHelper.styleComboBox(ticketCombo);
        for (SupportTicketDTO t : openTickets) {
            ticketCombo.addItem("#" + t.id() + " - " + t.clientName() + ": " + t.subject());
        }
        JTextField technicianField = new JTextField();
        JTextField hoursField = new JTextField("1.0");
        JTextField descField = new JTextField();
        JTextField partsField = new JTextField();
        JTextField partsCostField = new JTextField("0.00");

        JPanel form = UIHelper.createDialogForm(
                "Pedido Associado:", ticketCombo,
                "Técnico:", technicianField,
                "Horas Executadas:", hoursField,
                "Descrição do Serviço:", descField,
                "Peças Substituídas:", partsField,
                "Custo Peças (MT):", partsCostField
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Registar Folha de Obra",
                "fas-tools", "Fecho de pedido de assistência", form).setConfirmButton("Gravar", "fas-save");
        dlg.setOnSaveAsync(() -> {
            SupportTicketDTO ticket = openTickets.get(Math.max(0, ticketCombo.getSelectedIndex()));
            CreateWorkSheetRequest request = new CreateWorkSheetRequest(
                    ticket.id(), requireTechnician(technicianField), parseHours(hoursField),
                    requireDescription(descField), partsField.getText().trim(),
                    parseMoney(partsCostField, "O custo das peças"));
            return () -> crmApiClient.createWorkSheet(request);
        });

        if (dlg.showDialog()) {
            owner.showSuccess("Folha de obra gravada; o pedido foi marcado como resolvido.");
            owner.refreshData();
        }
    }

    /** Corrige uma folha por faturar — antes um engano ficava lá para sempre. */
    void editWorkSheet() {
        WorkSheetDTO ws = selected();
        if (ws == null) return;
        if (!assertEditable(ws, "corrigida")) return;

        JTextField technicianField = new JTextField(ws.technicianName());
        JTextField hoursField = new JTextField(ws.hoursWorked().toPlainString());
        JTextField descField = new JTextField(ws.description());
        JTextField partsField = new JTextField(ws.partsUsed() == null ? "" : ws.partsUsed());
        JTextField partsCostField = new JTextField(ws.partsCost().toPlainString());

        JPanel form = UIHelper.createDialogForm(
                "Pedido:", readOnly("#" + ws.ticketId() + " — " + ws.subject()),
                "Cliente:", readOnly(ws.clientName()),
                "Técnico:", technicianField,
                "Horas Executadas:", hoursField,
                "Descrição do Serviço:", descField,
                "Peças Substituídas:", partsField,
                "Custo Peças (MT):", partsCostField
        );

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow,
                "Corrigir Folha de Obra #" + ws.id(), "fas-edit",
                "Só folhas por faturar podem ser corrigidas", form)
                .setConfirmButton("Gravar", "fas-save");
        dlg.setOnSaveAsync(() -> {
            UpdateWorkSheetRequest request = new UpdateWorkSheetRequest(
                    requireTechnician(technicianField), parseHours(hoursField),
                    requireDescription(descField), partsField.getText().trim(),
                    parseMoney(partsCostField, "O custo das peças"));
            return () -> crmApiClient.updateWorkSheet(ws.id(), request);
        });

        if (dlg.showDialog()) {
            owner.showSuccess("Folha de obra corrigida.");
            owner.refreshData();
        }
    }

    /** Anula com motivo. A folha continua na lista, marcada — não se apaga trabalho registado. */
    void voidWorkSheet() {
        WorkSheetDTO ws = selected();
        if (ws == null) return;
        if (!assertEditable(ws, "anulada")) return;

        String reason = UIHelper.promptRequiredText("Anular Folha de Obra", "fas-ban",
                "Folha #" + ws.id(), "Motivo da anulação:");
        if (reason == null) return;

        UIHelper.runWithProgress(owner, "A anular folha de obra…",
                () -> crmApiClient.voidWorkSheet(ws.id(), new VoidWorkSheetRequest(reason)),
                ignored -> {
                    owner.showSuccess("Folha #" + ws.id() + " anulada; o pedido associado voltou a ficar aberto quando aplicável.");
                    owner.refreshData();
                },
                error -> owner.showNotice(FeedbackType.ERROR, "Não foi possível anular a folha", error.getMessage()));
    }

    /** O papel que o técnico deixa assinado no cliente. */
    void printWorkSheet() {
        WorkSheetDTO ws = selected();
        if (ws == null) return;

        UIHelper.runWithProgress(owner, "A gerar folha de obra…",
                () -> crmApiClient.workSheetPdf(ws.id()),
                pdf -> PrintPreviewDialog.show(owner, pdf, "folha-obra-" + ws.id()),
                error -> owner.showNotice(FeedbackType.ERROR, "Não foi possível gerar o PDF", error.getMessage()));
    }

    void billWorkSheet() {
        WorkSheetDTO ws = selected();
        if (ws == null) return;

        if (ws.voided()) {
            owner.showNotice(FeedbackType.WARNING, "Folha anulada", "Esta folha de obra está anulada.");
            return;
        }
        if (Boolean.TRUE.equals(ws.isBilled())) {
            owner.showNotice(FeedbackType.WARNING, "Folha já faturada", "Esta folha de obra já foi faturada.");
            return;
        }

        UIHelper.runWithProgress(owner, "A faturar folha de obra…", () -> {
            crmApiClient.billWorkSheet(ws.id());
            return null;
        }, ignored -> {
            owner.showSuccess("Folha de obra faturada; foi gerada uma fatura comercial para " + ws.clientName() + ".");
            owner.refreshData();
        }, error -> owner.showNotice(FeedbackType.ERROR, "Não foi possível faturar a folha", error.getMessage()));
    }

    /**
     * Tarifa horária da loja. É o preço do produto de mão de obra no catálogo — a mesma tarifa que
     * a factura cobra — mas ninguém tem de saber isso para a mudar.
     */
    void editHourlyRate() {
        UIHelper.runWithProgress(owner, "A ler a tarifa…",
                crmApiClient::getSettings,
                this::showHourlyRateDialog,
                error -> owner.showNotice(FeedbackType.ERROR, "Não foi possível ler a tarifa", error.getMessage()));
    }

    private void showHourlyRateDialog(CrmSettingsDTO settings) {
        JTextField rateField = new JTextField(settings.hourlyRate().toPlainString());

        JPanel form = UIHelper.createDialogForm(
                "Serviço faturado:", readOnly(settings.labourProductName() + " (" + settings.labourSku() + ")"),
                "Tarifa por hora (MT):", rateField);

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Tarifa da Assistência Técnica",
                "fas-money-bill-wave",
                "Aplica-se às folhas registadas a partir de agora; as já gravadas mantêm a tarifa delas",
                form).setConfirmButton("Gravar", "fas-save");
        dlg.setOnSaveAsync(() -> {
            BigDecimal rate = parseMoney(rateField, "A tarifa horária");
            if (rate.signum() <= 0) {
                throw new IllegalArgumentException("A tarifa horária tem de ser maior que zero.");
            }
            return () -> crmApiClient.updateSettings(new UpdateCrmSettingsRequest(rate));
        });

        if (dlg.showDialog()) {
            owner.showSuccess("Tarifa actualizada.");
            owner.refreshData();
        }
    }

    // ─── Internos ──────────────────────────────────────────────────────────────────────────────

    private WorkSheetDTO selected() {
        int row = TableFilter.selectedModelRow(owner.worksheetsTable);
        if (row < 0) {
            owner.showNotice(FeedbackType.WARNING, "Seleccione uma folha de obra",
                    "Escolha uma folha de obra na tabela para continuar.");
            return null;
        }
        return owner.worksheetsList.get(row);
    }

    private boolean assertEditable(WorkSheetDTO ws, String action) {
        if (Boolean.TRUE.equals(ws.isBilled())) {
            owner.showNotice(FeedbackType.WARNING, "Folha já faturada",
                    "A folha não pode ser " + action + ". Emita uma nota de crédito da factura.");
            return false;
        }
        if (ws.voided()) {
            owner.showNotice(FeedbackType.WARNING, "Folha já anulada", "Esta folha de obra já está anulada.");
            return false;
        }
        return true;
    }

    private String requireTechnician(JTextField field) {
        String tech = field.getText().trim();
        if (tech.isEmpty()) throw new IllegalArgumentException("O nome do técnico é obrigatório.");
        return tech;
    }

    private String requireDescription(JTextField field) {
        String desc = field.getText().trim();
        if (desc.isEmpty()) throw new IllegalArgumentException("A descrição do serviço é obrigatória.");
        return desc;
    }

    private BigDecimal parseHours(JTextField field) {
        try {
            BigDecimal hours = new BigDecimal(field.getText().trim());
            if (hours.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            return hours;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("As horas devem ser um número positivo (ex.: 2.5).");
        }
    }

    private BigDecimal parseMoney(JTextField field, String label) {
        String raw = field.getText().trim();
        if (raw.isEmpty()) return BigDecimal.ZERO;
        try {
            BigDecimal value = new BigDecimal(raw);
            if (value.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " não pode ser negativo.");
        }
    }

    private static JTextField readOnly(String value) {
        JTextField f = new JTextField(value == null ? "" : value);
        UIHelper.styleTextField(f);
        UIHelper.setReadOnly(f, true);
        return f;
    }
}
