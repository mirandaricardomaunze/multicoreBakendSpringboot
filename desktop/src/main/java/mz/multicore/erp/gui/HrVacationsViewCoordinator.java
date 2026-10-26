package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.hr.dto.VacationDTO;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Coordenador de apresentação, decisões hierárquicas e Quick Peek para pedidos de férias.
 */
public final class HrVacationsViewCoordinator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private HrVacationsViewCoordinator() {}

    public static void populateQuickPeek(QuickPeekPanel peek, int row, HRPanel owner) {
        peek.setHeaderIcon("fas-plane-departure", UIHelper.ACCENT_BLUE);
        peek.setTitle(String.valueOf(owner.vacationsModel.getValueAt(row, 1)));
        peek.setSubtitle("Férias Ref. " + owner.vacationsModel.getValueAt(row, 5) + " • " + owner.vacationsModel.getValueAt(row, 4) + " dias");
        peek.setStatus(String.valueOf(owner.vacationsModel.getValueAt(row, 6)), UIHelper.ACCENT_BLUE);
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Início", String.valueOf(owner.vacationsModel.getValueAt(row, 2)), false),
                new QuickPeekPanel.PeekItem("Fim", String.valueOf(owner.vacationsModel.getValueAt(row, 3)), false),
                new QuickPeekPanel.PeekItem("Dias Marcados", String.valueOf(owner.vacationsModel.getValueAt(row, 4)), true),
                new QuickPeekPanel.PeekItem("Decidido Por", String.valueOf(owner.vacationsModel.getValueAt(row, 7)), false)
        ));
    }

    public static void applyVacations(List<VacationDTO> loaded, HRPanel owner) {
        owner.vacationsList = loaded;
        owner.vacationsModel.setRowCount(0);
        for (VacationDTO v : owner.vacationsList) {
            owner.vacationsModel.addRow(new Object[]{
                    v.id(), v.employeeName(),
                    v.startDate().format(DATE_FMT), v.endDate().format(DATE_FMT),
                    v.totalDays(), v.yearReference(), v.status(),
                    v.decisionBy() == null ? "-" : v.decisionBy()
            });
        }
        UIHelper.ensureHeadersFit(owner.vacationsTable);
        owner.refreshOverview();
    }

    public static void decideVacation(boolean approve, HRPanel owner, Runnable onDecided) {
        int row = TableFilter.selectedModelRow(owner.vacationsTable);
        if (row < 0) {
            owner.showNotice(FeedbackType.WARNING, "Seleccione um pedido", "Escolha um pedido na tabela para continuar.");
            return;
        }
        VacationDTO sel = owner.vacationsList.get(row);
        String reason = null;
        if (!approve) {
            reason = UIHelper.promptRequiredText("Rejeitar Pedido de Férias", "fas-times-circle",
                    "Colaborador: " + sel.employeeName(), "Motivo da rejeição:");
            if (reason == null) return;
        }
        String decisionReason = reason;
        UIHelper.runWithProgress(owner, approve ? "A aprovar férias…" : "A rejeitar férias…", () -> {
            owner.hrApiClient.decideVacation(sel.id(), approve, decisionReason);
            return null;
        }, ignored -> onDecided.run(), owner::showActionError);
    }

    public static void openVacationAllowance(HRPanel owner) {
        int row = TableFilter.selectedModelRow(owner.vacationsTable);
        if (row < 0) {
            owner.showNotice(FeedbackType.WARNING, "Seleccione um pedido",
                    "Escolha as férias aprovadas cujo subsídio pretende consultar.");
            return;
        }
        new HRBonusActions(owner).openVacationAllowance(owner.vacationsList.get(row));
    }
}
