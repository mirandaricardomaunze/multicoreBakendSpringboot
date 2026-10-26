package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ProductionApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.manufacturing.dto.CreateBomRequest;
import mz.multicore.erp.modules.manufacturing.model.BomType;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Diálogo modal para criação e registo de Fichas Técnicas (BOM).
 */
public final class BomFormDialog {

    private BomFormDialog() {}

    public static void show(Window parent,
                            List<ProductDTO> cachedProducts,
                            ProductionApiClient productionApiClient,
                            InlineFeedbackPanel feedback,
                            Runnable onSuccess) {
        JTextField codeField = new JTextField(10);
        JTextField nameField = new JTextField(20);
        JComboBox<ProductDTO> productCombo = new JComboBox<>(cachedProducts.toArray(new ProductDTO[0]));
        UIHelper.styleComboBox(productCombo);
        DefaultListCellRenderer baseRenderer = new DefaultListCellRenderer();
        productCombo.setRenderer((list, val, idx, isSelected, cellHasFocus) -> {
            Component c = baseRenderer.getListCellRendererComponent(list, val, idx, isSelected, cellHasFocus);
            if (val instanceof ProductDTO p && c instanceof JLabel lbl) {
                lbl.setText(p.sku() + " - " + p.name());
            }
            return c;
        });

        JTextField baseQtyField = new JTextField("1.0", 8);
        JTextField laborCostField = new JTextField("0.00", 10);
        JTextField overheadCostField = new JTextField("0.00", 10);

        JPanel form = UIHelper.createDialogForm(
                "Código da Ficha *:", codeField,
                "Nome da Ficha *:", nameField,
                "Produto Acabado *:", productCombo,
                "Quantidade Base *:", baseQtyField,
                "Mão de Obra Est. (MT):", laborCostField,
                "Custos Gerais Est. (MT):", overheadCostField
        );

        ModernFormDialog dialog = new ModernFormDialog(parent, "Criar Nova Ficha Técnica (BOM)", "fas-file-alt",
                "Definição da fórmula e matérias-primas para fabrico", form);

        dialog.setOnSave(() -> {
            String code = codeField.getText().trim();
            if (code.isEmpty()) {
                codeField.requestFocus();
                throw new IllegalArgumentException("O código da ficha é obrigatório.");
            }
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                nameField.requestFocus();
                throw new IllegalArgumentException("O nome da ficha é obrigatório.");
            }
            ProductDTO prod = (ProductDTO) productCombo.getSelectedItem();
            if (prod == null) throw new IllegalArgumentException("Selecione o produto final.");

            BigDecimal baseQty;
            try {
                baseQty = new BigDecimal(baseQtyField.getText().trim().replace(",", "."));
                if (baseQty.compareTo(BigDecimal.ZERO) <= 0) throw new Exception();
            } catch (Exception ex) {
                baseQtyField.requestFocus();
                throw new IllegalArgumentException("A quantidade base deve ser positiva.");
            }

            BigDecimal labor = BigDecimal.ZERO;
            if (!laborCostField.getText().trim().isEmpty()) {
                labor = new BigDecimal(laborCostField.getText().trim().replace(",", "."));
            }
            BigDecimal overhead = BigDecimal.ZERO;
            if (!overheadCostField.getText().trim().isEmpty()) {
                overhead = new BigDecimal(overheadCostField.getText().trim().replace(",", "."));
            }

            ProductDTO defaultComponent = cachedProducts.stream()
                    .filter(p -> !p.id().equals(prod.id()))
                    .findFirst()
                    .orElse(prod);

            List<CreateBomRequest.CreateBomItemRequest> items = List.of(
                    new CreateBomRequest.CreateBomItemRequest(defaultComponent.id(), BigDecimal.ONE, BigDecimal.ZERO)
            );

            CreateBomRequest req = new CreateBomRequest(
                    code, name, prod.id(), baseQty, BomType.STANDARD, labor, overhead, items
            );
            productionApiClient.createBom(req);
        });

        if (dialog.showDialog()) {
            if (feedback != null) feedback.show(FeedbackType.SUCCESS, "Ficha técnica registada com sucesso.");
            if (onSuccess != null) onSuccess.run();
        }
    }
}
