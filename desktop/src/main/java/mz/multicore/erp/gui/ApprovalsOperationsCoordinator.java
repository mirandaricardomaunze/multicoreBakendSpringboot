package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.StockTransferApiClient;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.model.ApprovalStatus;

import javax.swing.*;
import java.awt.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Coordenador de operações, tradução semântica de documentos e métricas de aprovação. */
public final class ApprovalsOperationsCoordinator {

    private ApprovalsOperationsCoordinator() {}

    public static String humanType(String documentType) {
        if (documentType == null) return "";
        return switch (documentType.toUpperCase(Locale.ROOT)) {
            case "ORDER" -> "Encomenda";
            case "INVOICE" -> "Fatura";
            case "EXPENSE", "EXPENSE_CLAIM" -> "Despesa";
            case "STOCK_TRANSFER", "TRANSFER" -> "Transferência de Stock";
            case "STOCK_WASTE", "WASTE" -> "Quebra de Stock";
            case "PURCHASE_ORDER", "PURCHASE" -> "Ordem de Compra";
            default -> documentType;
        };
    }

    public static void updateKpis(List<ApprovalRequestDTO> pendingList, List<ApprovalRequestDTO> historyList,
                                  JLabel kpiPendingCount, JLabel kpiPendingAmount, JLabel kpiApprovedCount, JLabel kpiRejectedCount) {
        if (kpiPendingCount != null && pendingList != null) {
            kpiPendingCount.setText(String.valueOf(pendingList.size()));
        }
        if (kpiPendingAmount != null && pendingList != null) {
            BigDecimal totalPendingAmount = pendingList.stream()
                    .map(ApprovalRequestDTO::amount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            kpiPendingAmount.setText(String.format(Locale.forLanguageTag("pt-MZ"), "%,.2f MT", totalPendingAmount));
        }
        if (kpiApprovedCount != null && historyList != null) {
            long approvedCount = historyList.stream()
                    .filter(r -> r.status() == ApprovalStatus.APPROVED)
                    .count();
            kpiApprovedCount.setText(String.valueOf(approvedCount));
        }
        if (kpiRejectedCount != null && historyList != null) {
            long rejectedCount = historyList.stream()
                    .filter(r -> r.status() == ApprovalStatus.REJECTED)
                    .count();
            kpiRejectedCount.setText(String.valueOf(rejectedCount));
        }
    }

    public static void openDecision(Component parent, ApprovalRequestDTO req, ApprovalApiClient approvalClient,
                                    StockTransferApiClient transferClient, ComercialApiClient comercialClient,
                                    PrintApiClient printClient, Runnable onProcessed) {
        if (req == null) return;
        ApprovalDetailsDialog.show(
                SwingUtilities.getWindowAncestor(parent),
                req,
                approvalClient,
                transferClient,
                comercialClient,
                printClient,
                () -> {
                    if (onProcessed != null) onProcessed.run();
                }
        );
    }
}
