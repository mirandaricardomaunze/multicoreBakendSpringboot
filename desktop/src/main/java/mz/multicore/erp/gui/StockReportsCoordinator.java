package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.CreateStockAdjustmentRequest;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Coordenador de relatórios e diálogos de ajuste/contagem de inventário no {@link StockPanel}.
 */
final class StockReportsCoordinator {

    private StockReportsCoordinator() {
    }

    static void printInventoryReport(StockPanel panel) {
        if (panel.stockTable != null && panel.stockTable.getRowCount() == 0) {
            panel.showStockNotice(FeedbackType.INFO, "Sem dados para imprimir", "A listagem de stock está vazia.");
            return;
        }
        Long warehouseId = null;
        String fileSuffix = "todos";
        if (panel.warehouseFilterCombo != null) {
            int idx = panel.warehouseFilterCombo.getSelectedIndex();
            if (idx > 0 && (idx - 1) < panel.warehousesList.size()) {
                WarehouseDTO selectedWarehouse = panel.warehousesList.get(idx - 1);
                warehouseId = selectedWarehouse.id();
                fileSuffix = selectedWarehouse.name().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
                if (fileSuffix.isBlank()) fileSuffix = "armazem-" + selectedWarehouse.id();
            }
        }
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        Long selectedWarehouseId = warehouseId;
        String suffix = fileSuffix;
        UIHelper.runWithProgress(panel, "A gerar inventário em PDF…",
                () -> panel.inventoryApiClient.renderInventoryReport(companyId, selectedWarehouseId),
                pdf -> PrintPreviewDialog.show(panel, pdf, "inventario-stock-" + suffix),
                panel::showStockError);
    }

    static void printValuedInventoryBook(StockPanel panel) {
        if (panel.printApiClient == null) {
            panel.showStockNotice(FeedbackType.INFO, "Impressão indisponível", "O cliente de impressão não está inicializado.");
            return;
        }
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        int year = LocalDate.now().getYear();
        UIHelper.runWithProgress(panel, "A gerar Livro de Inventário Valorizado (PGC-NIRF)…",
                () -> panel.printApiClient.renderValuedInventoryBook(companyId, year),
                pdf -> PrintPreviewDialog.show(panel, pdf, "livro-inventario-valorizado-" + year),
                panel::showStockError);
    }

    static void printFilteredMovements(StockPanel panel,
                                       JComboBox<String> productFilter,
                                       DateField fromField,
                                       DateField toField) {
        String product = productFilter != null && productFilter.getSelectedIndex() > 0
                ? String.valueOf(productFilter.getSelectedItem()) : null;
        LocalDate fromDate = fromField != null ? fromField.getDate() : null;
        LocalDate toDate = toField != null ? toField.getDate() : null;

        StringBuilder titleBuilder = new StringBuilder("Movimentos de Stock");
        StringBuilder baseNameBuilder = new StringBuilder("movimentos-stock");

        if (product != null && !product.isBlank()) {
            titleBuilder.append(" — Artigo: ").append(product);
            String prodSlug = product.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-|-$", "");
            if (prodSlug.length() > 24) prodSlug = prodSlug.substring(0, 24);
            if (!prodSlug.isBlank()) {
                baseNameBuilder.append("-").append(prodSlug);
            }
        }

        if (fromDate != null || toDate != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter fileFmt = DateTimeFormatter.ofPattern("yyyyMMdd");
            String dateLabel;
            if (fromDate != null && toDate != null) {
                dateLabel = fromDate.format(fmt) + " a " + toDate.format(fmt);
                baseNameBuilder.append("-").append(fromDate.format(fileFmt)).append("-").append(toDate.format(fileFmt));
            } else if (fromDate != null) {
                dateLabel = "desde " + fromDate.format(fmt);
                baseNameBuilder.append("-desde-").append(fromDate.format(fileFmt));
            } else {
                dateLabel = "até " + toDate.format(fmt);
                baseNameBuilder.append("-ate-").append(toDate.format(fileFmt));
            }
            titleBuilder.append(" (").append(dateLabel).append(")");
        }

        panel.exportMovementsTable(titleBuilder.toString(), baseNameBuilder.toString());
    }

    static void createAdjustmentDialog(StockPanel panel) {
        List<ProductDTO> products = new ArrayList<>(panel.catalogProducts);
        if (products.isEmpty() || panel.warehousesList.isEmpty()) {
            panel.showStockNotice(FeedbackType.WARNING, "Configuração incompleta", "Registe produtos e armazéns antes de ajustar o stock.");
            return;
        }

        ProductSearchComboBox prodCombo = new ProductSearchComboBox();
        JComboBox<WarehouseDTO> whCombo = UIHelper.createComboBox(
                panel.warehousesList.toArray(WarehouseDTO[]::new), WarehouseDTO::name);
        JTextField countedField = new JTextField();
        JTextField reasonField = new JTextField("Contagem física de inventário");

        UIHelper.styleTextField(countedField);
        UIHelper.styleTextField(reasonField);

        prodCombo.setProducts(products);
        JPanel dialogPanel = UIHelper.createDialogForm(
                "Selecionar Artigo:", prodCombo,
                "Selecionar Armazém:", whCombo,
                "Quantidade contada:", countedField,
                "Motivo / Descrição:", reasonField
        );

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Contagem / Ajuste de Stock",
                "fas-clipboard-list", "Acerte a quantidade física em stock", dialogPanel);
        dialog.setConfirmButton("Registar ajuste", "fas-check");
        dialog.setOnSaveAsync(() -> {
            WarehouseDTO warehouse = (WarehouseDTO) whCombo.getSelectedItem();
            ProductDTO product = prodCombo.selectedProduct();
            if (product == null || warehouse == null) {
                throw new IllegalArgumentException("Seleccione o artigo e o armazém.");
            }
            BigDecimal counted;
            try {
                counted = new BigDecimal(countedField.getText().trim().replace(",", "."));
                UIHelper.clearFieldInvalid(countedField);
            } catch (NumberFormatException error) {
                UIHelper.markFieldInvalid(countedField, "Introduza uma quantidade válida.");
                throw new IllegalArgumentException("Introduza uma quantidade válida.");
            }
            String reason = reasonField.getText().trim();
            if (reason.isBlank()) {
                UIHelper.markFieldInvalid(reasonField, "Indique o motivo do ajuste.");
                throw new IllegalArgumentException("Indique o motivo do ajuste.");
            }
            UIHelper.clearFieldInvalid(reasonField);
            CreateStockAdjustmentRequest request = new CreateStockAdjustmentRequest(
                    CurrentUserContext.getCurrentCompanyId(), product.id(), warehouse.id(), counted, reason,
                    "ADJUST-" + java.util.UUID.randomUUID());
            return () -> panel.inventoryApiClient.adjustStock(request);
        });
        if (dialog.showDialog()) {
            ToastManager.success(panel, "Contagem de stock registada com sucesso.");
            panel.onPanelSelected();
        }
    }
}
