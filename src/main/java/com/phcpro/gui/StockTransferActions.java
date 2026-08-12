package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.*;
import com.phcpro.modules.comercial.dto.ProductDTO;
import com.phcpro.modules.inventory.dto.*;
import com.phcpro.modules.printing.PdfFileSaver;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Fluxo de criação, decisão e impressão de transferências de stock. */
final class StockTransferActions {
    private final StockPanel owner;
    StockTransferActions(StockPanel owner) { this.owner = owner; }

    public void createTransferDialog() {
        if (owner.warehousesList.size() < 2) {
            JOptionPane.showMessageDialog(owner,
                    "É necessário pelo menos 2 armazéns para realizar uma transferência.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<ProductDTO> products = new ArrayList<>(owner.catalogProducts);
        if (products.isEmpty()) {
            JOptionPane.showMessageDialog(owner,
                    "É necessário cadastrar produtos antes de transferir.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JComboBox<String> originCombo = new JComboBox<>();
        JComboBox<String> destinationCombo = new JComboBox<>();
        UIHelper.styleComboBox(originCombo);
        UIHelper.styleComboBox(destinationCombo);
        for (WarehouseDTO w : owner.warehousesList) {
            originCombo.addItem(w.name());
            destinationCombo.addItem(w.name());
        }
        if (owner.warehousesList.size() > 1) destinationCombo.setSelectedIndex(1);

        JTextField responsibleField = new JTextField();
        JTextField vehicleField = new JTextField();
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(responsibleField);
        UIHelper.styleTextField(vehicleField);
        UIHelper.styleTextField(notesField);

        String[] lineCols = {"Produto", "Quantidade", "Lote (FEFO)", "Validade (FEFO)"};
        DefaultTableModel linesModel = new DefaultTableModel(lineCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return c == 0 || c == 1; }
        };
        JTable linesTable = new JTable(linesModel);
        UIHelper.styleTable(linesTable);

        JComboBox<String> productEditorCombo = new JComboBox<>();
        for (ProductDTO p : products) productEditorCombo.addItem(p.name());
        linesTable.getColumnModel().getColumn(0)
                .setCellEditor(new DefaultCellEditor(productEditorCombo));

        JScrollPane linesScroll = new JScrollPane(linesTable);
        linesScroll.setPreferredSize(new Dimension(640, 200));

        Runnable refreshTransferFEFO = () -> {
            int wIdx = originCombo.getSelectedIndex();
            WarehouseDTO origin = (wIdx >= 0 && wIdx < owner.warehousesList.size()) ? owner.warehousesList.get(wIdx) : null;
            List<ProductDTO> selectedProducts = new ArrayList<>();
            for (int i = 0; i < linesModel.getRowCount(); i++) {
                String name = String.valueOf(linesModel.getValueAt(i, 0));
                selectedProducts.add(products.stream().filter(x -> x.name().equals(name)).findFirst().orElse(null));
                linesModel.setValueAt("A carregar…", i, 2);
                linesModel.setValueAt("", i, 3);
            }
            if (origin == null) return;
            Long originId = origin.id();
            UIHelper.loadAsync(owner, () -> {
                List<FefoPreview> previews = new ArrayList<>();
                for (ProductDTO product : selectedProducts) {
                    if (product == null) {
                        previews.add(new FefoPreview("", ""));
                        continue;
                    }
                    var opt = owner.inventoryApiClient.findNextFEFO(product.id(), originId);
                    if (opt.isPresent()) {
                        var b = opt.get();
                        previews.add(new FefoPreview(b.batchNumber() == null ? "—" : b.batchNumber(),
                                b.expirationDate() == null ? "—" : b.expirationDate().format(
                                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
                    } else {
                        previews.add(new FefoPreview("Sem stock", "—"));
                    }
                }
                return previews;
            }, previews -> {
                int currentIdx = originCombo.getSelectedIndex();
                if (currentIdx < 0 || !owner.warehousesList.get(currentIdx).id().equals(originId)) return;
                for (int i = 0; i < previews.size() && i < linesModel.getRowCount(); i++) {
                    linesModel.setValueAt(previews.get(i).batch(), i, 2);
                    linesModel.setValueAt(previews.get(i).expiration(), i, 3);
                }
            }, error -> {
                for (int i = 0; i < linesModel.getRowCount(); i++) {
                    linesModel.setValueAt("Falha ao carregar", i, 2);
                    linesModel.setValueAt("", i, 3);
                }
            });
        };

        ModernButton addLineBtn = UIHelper.createAddLineButton();
        ModernButton removeLineBtn = UIHelper.createDangerButton("- Remover");
        addLineBtn.addActionListener(ev -> {
            linesModel.addRow(new Object[]{products.get(0).name(), "1", "", ""});
            refreshTransferFEFO.run();
        });
        removeLineBtn.addActionListener(ev -> {
            int sel = linesTable.getSelectedRow();
            if (sel >= 0) linesModel.removeRow(sel);
        });
        JPanel lineButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        lineButtons.setOpaque(false);
        lineButtons.add(addLineBtn);
        lineButtons.add(removeLineBtn);

        linesModel.addRow(new Object[]{products.get(0).name(), "1", "", ""});
        originCombo.addActionListener(ev -> refreshTransferFEFO.run());
        linesModel.addTableModelListener(ev -> {
            if (ev.getColumn() == 0) refreshTransferFEFO.run();
        });
        refreshTransferFEFO.run();

        JPanel header = UIHelper.createDialogForm(
                "Armazém de Origem:", originCombo,
                "Armazém de Destino:", destinationCombo,
                "Responsável:", responsibleField,
                "Veículo / Transporte:", vehicleField,
                "Observações:", notesField
        );

        JPanel dialogPanel = new JPanel(new BorderLayout(0, 10));
        dialogPanel.setOpaque(false);
        dialogPanel.add(header, BorderLayout.NORTH);
        JPanel linesWrap = new JPanel(new BorderLayout(0, 6));
        linesWrap.setOpaque(false);
        linesWrap.add(new JLabel("Linhas da Transferência:"), BorderLayout.NORTH);
        linesWrap.add(linesScroll, BorderLayout.CENTER);
        linesWrap.add(lineButtons, BorderLayout.SOUTH);
        dialogPanel.add(linesWrap, BorderLayout.CENTER);

        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Nova Transferência de Stock", "fas-exchange-alt", "Mova stock entre armazéns", dialogPanel).showDialog();
        if (!confirmed) return;

        int originIdx = originCombo.getSelectedIndex();
        int destIdx = destinationCombo.getSelectedIndex();
        if (originIdx == destIdx) {
            JOptionPane.showMessageDialog(owner, "Armazém de origem e destino devem ser diferentes.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (linesTable.isEditing()) linesTable.getCellEditor().stopCellEditing();
        if (linesModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(owner, "Adicione pelo menos uma linha.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<CreateStockTransferLineRequest> lines = new ArrayList<>();
        try {
            for (int i = 0; i < linesModel.getRowCount(); i++) {
                String productName = (String) linesModel.getValueAt(i, 0);
                String qtyStr = String.valueOf(linesModel.getValueAt(i, 1)).trim();
                BigDecimal qty = new BigDecimal(qtyStr);
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new NumberFormatException("Quantidade deve ser positiva na linha " + (i + 1));
                }
                ProductDTO product = products.stream()
                        .filter(p -> p.name().equals(productName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + productName));
                lines.add(new CreateStockTransferLineRequest(product.id(), qty));
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(owner, "Quantidade inválida: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(owner, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        WarehouseDTO origin = owner.warehousesList.get(originIdx);
        WarehouseDTO destination = owner.warehousesList.get(destIdx);

        CreateStockTransferRequest request = new CreateStockTransferRequest(
                    CurrentUserContext.getCurrentCompanyId(),
                    origin.id(),
                    destination.id(),
                    responsibleField.getText().trim(),
                    vehicleField.getText().trim(),
                    notesField.getText().trim(),
                    lines);
        UIHelper.runWithProgress(owner, "A criar transferência…", () -> owner.stockTransferApiClient.create(request), created -> {
            owner.onPanelSelected();

            int print = JOptionPane.showConfirmDialog(owner,
                    "Guia " + created.transferNumber() + " registada e PENDENTE DE APROVAÇÃO.\n"
                            + "O stock só sai do armazém de origem após aprovação (MANAGER/ADMIN).\n\n"
                            + "Deseja imprimir a Guia de Transferência agora?",
                    "Sucesso", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (print == JOptionPane.YES_OPTION) {
                printTransfer(created.id(), created.transferNumber());
            }
        }, owner::showStockError);
    }

    public void approveSelectedTransfer() {
        int row = TableFilter.selectedModelRow(owner.transferTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(owner, "Selecione uma guia na tabela primeiro.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StockTransferDTO selected = owner.transfersList.get(row);
        int confirm = JOptionPane.showConfirmDialog(owner,
                "Aprovar a guia " + selected.transferNumber() + "?\n"
                        + "O stock vai sair de '" + selected.originWarehouseName()
                        + "' e entrar em '" + selected.destinationWarehouseName() + "'.",
                "Confirmar aprovação", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        UIHelper.runWithProgress(owner, "A aprovar transferência…", () -> owner.stockTransferApiClient.approve(selected.id()), ignored -> {
            owner.onPanelSelected();
            JOptionPane.showMessageDialog(owner,
                    "Guia " + selected.transferNumber() + " aprovada. Stock movido — ver aba Movimentos.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        }, owner::showStockError);
    }

    public void rejectSelectedTransfer() {
        int row = TableFilter.selectedModelRow(owner.transferTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(owner, "Selecione uma guia na tabela primeiro.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StockTransferDTO selected = owner.transfersList.get(row);
        String reason = UIHelper.promptRequiredText("Rejeitar Guia", "fas-times-circle",
                "Guia " + selected.transferNumber(), "Motivo da rejeição:");
        if (reason == null) return;
        UIHelper.runWithProgress(owner, "A rejeitar transferência…", () -> owner.stockTransferApiClient.reject(selected.id(), reason), ignored -> {
            owner.onPanelSelected();
            JOptionPane.showMessageDialog(owner,
                    "Guia " + selected.transferNumber() + " rejeitada. Nenhum stock foi movido.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        }, owner::showStockError);
    }

    public void printSelectedTransfer() {
        int row = TableFilter.selectedModelRow(owner.transferTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(owner, "Selecione uma transferência na tabela primeiro.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        StockTransferDTO selected = owner.transfersList.get(row);
        printTransfer(selected.id(), selected.transferNumber());
    }

    private void printTransfer(Long transferId, String transferNumber) {
        UIHelper.runWithProgress(owner, "A gerar guia de transferência…",
                () -> owner.stockTransferApiClient.renderTransfer(transferId),
                pdf -> PdfFileSaver.saveAndOpen(pdf, "transferencia-" + transferNumber),
                owner::showStockError);
    }

    private record FefoPreview(String batch, String expiration) {}

}
