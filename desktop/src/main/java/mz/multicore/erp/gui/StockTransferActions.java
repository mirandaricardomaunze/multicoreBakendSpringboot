package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.inventory.dto.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Fluxo de criação, decisão e impressão de transferências de stock. */
final class StockTransferActions {
    private final StockPanel owner;
    StockTransferActions(StockPanel owner) { this.owner = owner; }

    void openCreateEditor() {
        if (owner.warehousesList.size() < 2) {
            owner.showStockNotice(FeedbackType.WARNING, "Armazéns insuficientes",
                    "Registe pelo menos dois armazéns para realizar uma transferência.");
            return;
        }
        if (owner.catalogProducts.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Produto necessário",
                    "Registe produtos antes de realizar uma transferência.");
            return;
        }
        owner.transferEditorForm.setOptions(owner.warehousesList, owner.catalogProducts);
        owner.transferEditorForm.prepareCreate();
        owner.transferEditorHost.setEditorTitle("Nova Transferência — Rascunho");
        owner.transferEditorHost.setSaveText("Guardar Rascunho");
        owner.transferEditorHost.setSaveEnabled(true);
        owner.transferPagesLayout.show(owner.transferPages, "editor");
    }

    void openSelectedEditor() {
        StockTransferDTO selected = selected("Seleccione uma guia",
                "Escolha uma guia na tabela para editar ou consultar.");
        if (selected == null) return;
        UIHelper.loadAsync(owner, () -> owner.stockTransferApiClient.findById(selected.id()), transfer -> {
            owner.transferEditorForm.setOptions(owner.warehousesList, owner.catalogProducts);
            owner.transferEditorForm.load(transfer);
            boolean editable = "DRAFT".equals(transfer.status());
            owner.transferEditorHost.setEditorTitle((editable ? "Editar " : "Consultar ")
                    + "Transferência " + transfer.transferNumber());
            owner.transferEditorHost.setSaveText(editable ? "Guardar Alterações" : "Apenas Consulta");
            owner.transferEditorHost.setSaveEnabled(editable);
            owner.transferPagesLayout.show(owner.transferPages, "editor");
        }, owner::showStockError);
    }

    void saveEditor() {
        StockTransferDTO loaded = owner.transferEditorForm.loaded();
        if (loaded != null && !"DRAFT".equals(loaded.status())) {
            owner.showStockNotice(FeedbackType.INFO, "Documento encerrado",
                    "Esta transferência está em modo de consulta e já não pode ser alterada.");
            return;
        }
        try {
            if (loaded == null) {
                CreateStockTransferRequest request = owner.transferEditorForm.createRequest();
                UIHelper.runWithProgress(owner, "A guardar rascunho…",
                        () -> owner.stockTransferApiClient.create(request), this::savedDraft, owner::showStockError);
            } else {
                UpdateStockTransferRequest request = owner.transferEditorForm.updateRequest();
                UIHelper.runWithProgress(owner, "A actualizar rascunho…",
                        () -> owner.stockTransferApiClient.update(loaded.id(), request), this::savedDraft,
                        owner::showStockError);
            }
        } catch (RuntimeException error) {
            owner.showStockNotice(FeedbackType.ERROR, "Não foi possível guardar", error.getMessage());
        }
    }

    private void savedDraft(StockTransferDTO saved) {
        owner.transferEditorForm.markClean();
        showList();
        owner.onPanelSelected();
        owner.showStockSuccess("Rascunho " + saved.transferNumber()
                + " guardado. Submeta-o quando estiver pronto para aprovação.");
    }

    void showList() {
        if (owner.transferPagesLayout != null && owner.transferPages != null) {
            owner.transferPagesLayout.show(owner.transferPages, "list");
        }
    }

    void submitSelectedTransfer() {
        StockTransferDTO selected = selected("Seleccione um rascunho",
                "Escolha uma transferência em rascunho para submeter.");
        if (selected == null) return;
        if (!"DRAFT".equals(selected.status())) {
            owner.showStockNotice(FeedbackType.INFO, "Estado incompatível",
                    "Apenas transferências em rascunho podem ser submetidas.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(owner,
                "Submeter a guia " + selected.transferNumber() + " para aprovação?\n"
                        + "Depois da submissão, o conteúdo fica bloqueado para edição.",
                "Submeter transferência", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        UIHelper.runWithProgress(owner, "A submeter transferência…",
                () -> owner.stockTransferApiClient.submit(selected.id()), ignored -> {
                    owner.onPanelSelected();
                    owner.showStockSuccess("Guia " + selected.transferNumber() + " submetida para aprovação.");
                }, owner::showStockError);
    }

    void cancelSelectedTransfer() {
        StockTransferDTO selected = selected("Seleccione uma guia",
                "Escolha uma transferência em rascunho ou pendente para cancelar.");
        if (selected == null) return;
        if (!"DRAFT".equals(selected.status()) && !"PENDING_APPROVAL".equals(selected.status())) {
            owner.showStockNotice(FeedbackType.INFO, "Estado incompatível",
                    "Apenas transferências em rascunho ou pendentes podem ser canceladas.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(owner,
                "Cancelar a guia " + selected.transferNumber() + "?\nEsta operação não move stock.",
                "Cancelar transferência", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        UIHelper.runWithProgress(owner, "A cancelar transferência…",
                () -> owner.stockTransferApiClient.cancel(selected.id()), ignored -> {
                    owner.onPanelSelected();
                    owner.showStockSuccess("Guia " + selected.transferNumber() + " cancelada.");
                }, owner::showStockError);
    }

    private StockTransferDTO selected(String title, String message) {
        int row = TableFilter.selectedModelRow(owner.transferTable);
        if (row < 0) {
            owner.showStockNotice(FeedbackType.WARNING, title, message);
            return null;
        }
        return owner.transfersList.get(row);
    }

    public void createTransferDialog() {
        if (owner.warehousesList.size() < 2) {
            owner.showStockNotice(FeedbackType.WARNING, "Armazéns insuficientes",
                    "Registe pelo menos dois armazéns para realizar uma transferência.");
            return;
        }
        List<ProductDTO> products = new ArrayList<>(owner.catalogProducts);
        if (products.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Produto necessário",
                    "Registe produtos antes de realizar uma transferência.");
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

        JTextField driverField = new JTextField();
        JTextField vehiclePlateField = new JTextField();
        JTextField responsibleField = new JTextField();
        JTextField vehicleField = new JTextField();
        JTextField notesField = new JTextField();
        UIHelper.styleTextField(driverField);
        UIHelper.styleTextField(vehiclePlateField);
        UIHelper.styleTextField(responsibleField);
        UIHelper.styleTextField(vehicleField);
        UIHelper.styleTextField(notesField);
        driverField.putClientProperty("JTextField.placeholderText", "Nome do motorista (obrigatório)");
        vehiclePlateField.putClientProperty("JTextField.placeholderText", "Matrícula do veículo (obrigatório, Ex: ABC-123-MC)");
        responsibleField.putClientProperty("JTextField.placeholderText", "Responsável pela transferência (opcional)");
        vehicleField.putClientProperty("JTextField.placeholderText", "Viatura / Transporte (opcional)");
        notesField.putClientProperty("JTextField.placeholderText", "Observações sobre a transferência");

        String[] lineCols = {"Produto", "Quantidade", "Embalagem", "Caixa", "% Caixa", "Lote (FEFO)", "Validade (FEFO)"};
        DefaultTableModel linesModel = new DefaultTableModel(lineCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return c == 0 || c == 1; }
        };
        JTable linesTable = new JTable(linesModel);
        UIHelper.styleTable(linesTable);

        ProductSearchComboBox productEditorCombo = new ProductSearchComboBox();
        productEditorCombo.setProducts(products);
        linesTable.getColumnModel().getColumn(0)
                .setCellEditor(new DefaultCellEditor(productEditorCombo));
        linesTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focus, int row, int column) {
                Object text = value instanceof ProductDTO product
                        ? ProductSearchComboBox.displayLabel(product)
                        : value;
                return super.getTableCellRendererComponent(table, text, selected, focus, row, column);
            }
        });

        JScrollPane linesScroll = new JScrollPane(linesTable);
        linesScroll.setPreferredSize(new Dimension(740, 200));

        Runnable refreshTransferFEFO = () -> {
            int wIdx = originCombo.getSelectedIndex();
            WarehouseDTO origin = (wIdx >= 0 && wIdx < owner.warehousesList.size()) ? owner.warehousesList.get(wIdx) : null;
            List<ProductDTO> selectedProducts = new ArrayList<>();
            for (int i = 0; i < linesModel.getRowCount(); i++) {
                Object productValue = linesModel.getValueAt(i, 0);
                ProductDTO product = productValue instanceof ProductDTO selected ? selected : null;
                selectedProducts.add(product);

                String qtyStr = String.valueOf(linesModel.getValueAt(i, 1)).trim();
                BigDecimal qty = BigDecimal.ZERO;
                try {
                    qty = new BigDecimal(qtyStr);
                } catch (Exception ignored) {}

                if (product != null && qty.compareTo(BigDecimal.ZERO) > 0) {
                    int pkgsPerBox = product.packagesPerBox() > 0 ? product.packagesPerBox() : 1;
                    int unitsPerPkg = product.unitsPerPackage() > 0 ? product.unitsPerPackage() : 1;
                    long unitsPerBox = (long) pkgsPerBox * unitsPerPkg;

                    BigDecimal pkgs = qty.divide(BigDecimal.valueOf(unitsPerPkg), 2, java.math.RoundingMode.HALF_UP);
                    BigDecimal boxes = qty.divide(BigDecimal.valueOf(unitsPerBox), 2, java.math.RoundingMode.HALF_UP);
                    BigDecimal pct = qty.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(unitsPerBox), 1, java.math.RoundingMode.HALF_UP);

                    linesModel.setValueAt(pkgs.stripTrailingZeros().toPlainString(), i, 2);
                    linesModel.setValueAt(boxes.stripTrailingZeros().toPlainString(), i, 3);
                    linesModel.setValueAt(pct.stripTrailingZeros().toPlainString() + "%", i, 4);
                } else {
                    linesModel.setValueAt("—", i, 2);
                    linesModel.setValueAt("—", i, 3);
                    linesModel.setValueAt("—", i, 4);
                }

                linesModel.setValueAt("A carregar…", i, 5);
                linesModel.setValueAt("", i, 6);
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
                    linesModel.setValueAt(previews.get(i).batch(), i, 5);
                    linesModel.setValueAt(previews.get(i).expiration(), i, 6);
                }
            }, error -> {
                for (int i = 0; i < linesModel.getRowCount(); i++) {
                    linesModel.setValueAt("Falha ao carregar", i, 5);
                    linesModel.setValueAt("", i, 6);
                }
            });
        };

        ModernButton addLineBtn = UIHelper.createAddLineButton();
        ModernButton removeLineBtn = UIHelper.createDangerButton("- Remover");
        addLineBtn.addActionListener(ev -> {
            linesModel.addRow(new Object[]{products.get(0), "1", "—", "—", "—", "", ""});
            refreshTransferFEFO.run();
        });
        removeLineBtn.addActionListener(ev -> {
            int sel = linesTable.getSelectedRow();
            if (sel >= 0) linesModel.removeRow(sel);
        });
        JPanel lineButtons = UIHelper.actionsBar(removeLineBtn, addLineBtn);

        linesModel.addRow(new Object[]{products.get(0), "1", "—", "—", "—", "", ""});
        originCombo.addActionListener(ev -> refreshTransferFEFO.run());
        linesModel.addTableModelListener(ev -> {
            if (ev.getColumn() == 0 || ev.getColumn() == 1) refreshTransferFEFO.run();
        });
        refreshTransferFEFO.run();

        JPanel header = UIHelper.createDialogForm(
                "Armazém de Origem:", originCombo,
                "Armazém de Destino:", destinationCombo,
                "Motorista *:", driverField,
                "Matrícula do Veículo *:", vehiclePlateField,
                "Responsável:", responsibleField,
                "Observações:", notesField
        );

        JPanel dialogPanel = new JPanel(new BorderLayout(0, 10));
        dialogPanel.setOpaque(false);
        dialogPanel.add(header, BorderLayout.NORTH);
        JPanel linesWrap = new JPanel(new BorderLayout(0, 6));
        linesWrap.setOpaque(false);
        JPanel linesHeader = new JPanel(new BorderLayout(8, 0));
        linesHeader.setOpaque(false);
        linesHeader.add(new JLabel("Linhas da Transferência:"), BorderLayout.WEST);
        linesHeader.add(lineButtons, BorderLayout.EAST);
        linesWrap.add(linesHeader, BorderLayout.NORTH);
        linesWrap.add(linesScroll, BorderLayout.CENTER);
        dialogPanel.add(linesWrap, BorderLayout.CENTER);

        ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Nova Transferência de Stock", "fas-exchange-alt", "Mova stock entre armazéns", dialogPanel);
        dlg.setOnSave(() -> {
            int originI = originCombo.getSelectedIndex();
            int destI = destinationCombo.getSelectedIndex();
            if (originI == destI) {
                throw new IllegalArgumentException("O armazém de origem e o de destino devem ser diferentes.");
            }
            if (driverField.getText().trim().isEmpty()) {
                driverField.requestFocusInWindow();
                throw new IllegalArgumentException("O nome do motorista é obrigatório para emitir a guia de transferência.");
            }
            if (vehiclePlateField.getText().trim().isEmpty()) {
                vehiclePlateField.requestFocusInWindow();
                throw new IllegalArgumentException("A matrícula do veículo é obrigatória para emitir a guia de transferência.");
            }
            if (linesTable.isEditing()) linesTable.getCellEditor().stopCellEditing();
            if (linesModel.getRowCount() == 0) {
                throw new IllegalArgumentException("Adicione pelo menos um produto à transferência.");
            }
        });
        boolean confirmed = dlg.showDialog();
        if (!confirmed) return;

        int originIdx = originCombo.getSelectedIndex();
        int destIdx = destinationCombo.getSelectedIndex();
        if (originIdx == destIdx) {
            owner.showStockNotice(FeedbackType.ERROR, "Armazéns inválidos",
                    "O armazém de origem e o de destino devem ser diferentes.");
            return;
        }
        if (linesTable.isEditing()) linesTable.getCellEditor().stopCellEditing();
        if (linesModel.getRowCount() == 0) {
            owner.showStockNotice(FeedbackType.WARNING, "Transferência vazia",
                    "Adicione pelo menos um produto à transferência.");
            return;
        }

        List<CreateStockTransferLineRequest> lines = new ArrayList<>();
        try {
            for (int i = 0; i < linesModel.getRowCount(); i++) {
                Object productValue = linesModel.getValueAt(i, 0);
                String qtyStr = String.valueOf(linesModel.getValueAt(i, 1)).trim();
                BigDecimal qty = new BigDecimal(qtyStr);
                if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new NumberFormatException("Quantidade deve ser positiva na linha " + (i + 1));
                }
                if (!(productValue instanceof ProductDTO product)) {
                    throw new IllegalArgumentException("Pesquise e seleccione o produto da linha " + (i + 1) + ".");
                }
                lines.add(new CreateStockTransferLineRequest(product.id(), qty));
            }
        } catch (NumberFormatException ex) {
            owner.showStockNotice(FeedbackType.ERROR, "Quantidade inválida", ex.getMessage());
            return;
        } catch (Exception ex) {
            owner.showStockNotice(FeedbackType.ERROR, "Não foi possível preparar a transferência", ex.getMessage());
            return;
        }

        WarehouseDTO origin = owner.warehousesList.get(originIdx);
        WarehouseDTO destination = owner.warehousesList.get(destIdx);

        String driver = driverField.getText().trim();
        String plate = vehiclePlateField.getText().trim();
        if (driver.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Motorista obrigatório",
                    "O nome do motorista é obrigatório para emitir a guia de transferência.");
            return;
        }
        if (plate.isEmpty()) {
            owner.showStockNotice(FeedbackType.WARNING, "Matrícula obrigatória",
                    "A matrícula do veículo é obrigatória para emitir a guia de transferência.");
            return;
        }
        String resp = responsibleField.getText().trim();
        String vehicle = vehicleField.getText().trim();
        if (vehicle.isEmpty() && !plate.isEmpty()) {
            vehicle = plate;
        }
        String notes = notesField.getText().trim();

        CreateStockTransferRequest request = new CreateStockTransferRequest(
                    CurrentUserContext.getCurrentCompanyId(),
                    origin.id(),
                    destination.id(),
                    resp.isEmpty() ? null : resp,
                    vehicle.isEmpty() ? null : vehicle,
                    notes.isEmpty() ? null : notes,
                    lines,
                    driver.isEmpty() ? null : driver,
                    plate.isEmpty() ? null : plate);
        UIHelper.runWithProgress(owner, "A criar transferência…", () -> owner.stockTransferApiClient.create(request), created -> {
            owner.onPanelSelected();

            int print = JOptionPane.showConfirmDialog(owner,
                    "Guia " + created.transferNumber() + " registada e PENDENTE DE APROVAÇÃO.\n"
                            + "O stock só sai do armazém de origem após aprovação de um Gestor ou Administrador.\n\n"
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
            owner.showStockNotice(FeedbackType.WARNING, "Seleccione uma guia",
                    "Escolha uma guia na tabela para continuar.");
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
            owner.showStockSuccess("Guia " + selected.transferNumber() + " aprovada · stock movimentado.");
        }, owner::showStockError);
    }

    public void rejectSelectedTransfer() {
        int row = TableFilter.selectedModelRow(owner.transferTable);
        if (row < 0) {
            owner.showStockNotice(FeedbackType.WARNING, "Seleccione uma guia",
                    "Escolha uma guia na tabela para continuar.");
            return;
        }
        StockTransferDTO selected = owner.transfersList.get(row);
        String reason = UIHelper.promptRequiredText("Rejeitar Guia", "fas-times-circle",
                "Guia " + selected.transferNumber(), "Motivo da rejeição:");
        if (reason == null) return;
        UIHelper.runWithProgress(owner, "A rejeitar transferência…", () -> owner.stockTransferApiClient.reject(selected.id(), reason), ignored -> {
            owner.onPanelSelected();
            owner.showStockSuccess("Guia " + selected.transferNumber() + " rejeitada · nenhum stock movimentado.");
        }, owner::showStockError);
    }

    public void printSelectedTransfer() {
        int row = TableFilter.selectedModelRow(owner.transferTable);
        if (row < 0) {
            owner.showStockNotice(FeedbackType.WARNING, "Seleccione uma transferência",
                    "Escolha uma transferência na tabela para continuar.");
            return;
        }
        StockTransferDTO selected = owner.transfersList.get(row);
        printTransfer(selected.id(), selected.transferNumber());
    }

    /**
     * Regista a encomenda de reposição em falta a partir de uma transferência já aprovada.
     *
     * <p>Para o armazém que transferiu sem pedido formal: é <b>registo</b>, não compromisso. A
     * mercadoria já mudou de armazém, pelo que a encomenda nasce cumprida e nada se move — mover
     * seria contar a mesma saída duas vezes. Ver docs/REPOSICAO_INTERNA_SPEC.md §5.
     */
    public void recordOrderForSelectedTransfer() {
        int row = TableFilter.selectedModelRow(owner.transferTable);
        if (row < 0) {
            owner.showStockNotice(FeedbackType.WARNING, "Seleccione uma transferência",
                    "Escolha uma transferência na tabela para continuar.");
            return;
        }
        StockTransferDTO selected = owner.transfersList.get(row);
        if (selected.orderId() != null) {
            owner.showStockNotice(FeedbackType.INFO, "Encomenda já registada",
                    "A transferência " + selected.transferNumber() + " já tem a encomenda "
                            + selected.orderNumber() + " registada.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(owner,
                "Registar a encomenda de reposição em falta para " + selected.transferNumber() + "?\n\n"
                        + "A mercadoria já mudou de armazém: a encomenda nasce cumprida e não move "
                        + "stock nenhum.\nServe para ficar registo de que a loja pediu.",
                "Registar encomenda em falta", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        UIHelper.runWithProgress(owner, "A registar encomenda…",
                () -> owner.stockTransferApiClient.recordOrder(selected.id()), order -> {
                    owner.onPanelSelected();
                    owner.showStockSuccess("Encomenda " + order.orderNumber() + " registada para a transferência "
                            + selected.transferNumber() + ".");
                }, owner::showStockError);
    }

    public void viewSelectedTransferLines() {
        int row = TableFilter.selectedModelRow(owner.transferTable);
        if (row < 0) {
            owner.showStockNotice(FeedbackType.WARNING, "Seleccione uma guia",
                    "Escolha uma guia na tabela para consultar as linhas.");
            return;
        }
        StockTransferDTO selected = owner.transfersList.get(row);
        UIHelper.loadAsync(owner, () -> owner.stockTransferApiClient.findById(selected.id()),
                this::displayTransferLinesModal, owner::showStockError);
    }

    public void displayTransferLinesModal(StockTransferDTO transfer) {
        if (transfer == null) return;
        List<StockTransferLineDTO> lines = transfer.lines() != null ? transfer.lines() : List.of();

        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        // Cartão resumo de transporte e armazéns
        ModernPanel infoCard = new ModernPanel(12);
        infoCard.setLayout(new GridLayout(2, 3, 14, 8));
        infoCard.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        String driver = transfer.driverName() != null && !transfer.driverName().isBlank()
                ? transfer.driverName()
                : (transfer.responsible() != null && !transfer.responsible().isBlank() ? transfer.responsible() : "—");
        String plate = transfer.vehiclePlate() != null && !transfer.vehiclePlate().isBlank()
                ? transfer.vehiclePlate()
                : (transfer.vehicle() != null && !transfer.vehicle().isBlank() ? transfer.vehicle() : "—");
        String dateStr = transfer.transferDate() != null
                ? transfer.transferDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "—";

        infoCard.add(createMetaItem("Armazém de Origem", transfer.originWarehouseName()));
        infoCard.add(createMetaItem("Armazém de Destino", transfer.destinationWarehouseName()));
        infoCard.add(createMetaItem("Data da Transferência", dateStr));
        infoCard.add(createMetaItem("Motorista", driver));
        infoCard.add(createMetaItem("Matrícula do Veículo", plate));
        infoCard.add(createMetaItem("Estado", transfer.status() != null ? transfer.status() : "—"));

        // Tabela com as 9 colunas canónicas: Referência, Cód. Barras, Produto, Quantidade, Embalagem, Caixa, % da Caixa, Valor Unitário, IVA
        String[] cols = {"Referência", "Cód. Barras", "Produto", "Quantidade", "Embalagem", "Caixa", "% da Caixa", "Valor Unitário", "IVA"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (StockTransferLineDTO line : lines) {
            String ref = line.reference() != null && !line.reference().isBlank()
                    ? line.reference()
                    : (line.productSku() != null ? line.productSku() : "—");
            String barcode = line.barcode() != null && !line.barcode().isBlank() ? line.barcode() : "—";
            String prod = line.productName() != null ? line.productName() : "—";
            if (line.batchNumber() != null && !line.batchNumber().isBlank() && !"-".equals(line.batchNumber())) {
                prod += " [Lote: " + line.batchNumber() + "]";
            }

            BigDecimal qty = line.quantity() != null ? line.quantity() : BigDecimal.ZERO;
            int pkgsPerBox = line.safePackagesPerBox();
            int unitsPerPkg = line.safeUnitsPerPackage();
            long unitsPerBox = (long) pkgsPerBox * unitsPerPkg;

            BigDecimal pkgs = unitsPerPkg > 0
                    ? qty.divide(BigDecimal.valueOf(unitsPerPkg), 2, java.math.RoundingMode.HALF_UP)
                    : qty;
            BigDecimal boxes = unitsPerBox > 0
                    ? qty.divide(BigDecimal.valueOf(unitsPerBox), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            BigDecimal boxPct = unitsPerBox > 0
                    ? qty.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(unitsPerBox), 1, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal unitPrice = line.safeUnitPrice();
            BigDecimal taxRate = line.safeTaxRate();

            BigDecimal lineNet = unitPrice.multiply(qty);
            BigDecimal lineTax = lineNet.multiply(taxRate);

            totalQty = totalQty.add(qty);
            totalNet = totalNet.add(lineNet);
            totalTax = totalTax.add(lineTax);

            model.addRow(new Object[]{
                    ref,
                    barcode,
                    prod,
                    qty.stripTrailingZeros().toPlainString(),
                    pkgs.stripTrailingZeros().toPlainString(),
                    boxes.stripTrailingZeros().toPlainString(),
                    boxPct.stripTrailingZeros().toPlainString() + "%",
                    unitPrice,
                    taxRate.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%"
            });
        }

        JTable table = new JTable(model);
        UIHelper.styleTable(table);

        javax.swing.table.DefaultTableCellRenderer rightRenderer = new javax.swing.table.DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        scroll.setPreferredSize(new Dimension(880, 260));

        // Resumo de totais
        totalNet = totalNet.setScale(2, java.math.RoundingMode.HALF_UP);
        totalTax = totalTax.setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal totalGross = totalNet.add(totalTax);

        ModernPanel totalsCard = new ModernPanel(12);
        totalsCard.setLayout(new FlowLayout(FlowLayout.RIGHT, 18, 8));
        totalsCard.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        totalsCard.add(createTotalBadge("Linhas", String.valueOf(lines.size())));
        totalsCard.add(createTotalBadge("Qtd Total", totalQty.stripTrailingZeros().toPlainString()));
        totalsCard.add(createTotalBadge("Total Líquido", MultiCurrencyEngine.formatCurrency(totalNet, MultiCurrencyEngine.Currency.MZN)));
        totalsCard.add(createTotalBadge("Total IVA", MultiCurrencyEngine.formatCurrency(totalTax, MultiCurrencyEngine.Currency.MZN)));
        totalsCard.add(createTotalBadge("Total Geral", MultiCurrencyEngine.formatCurrency(totalGross, MultiCurrencyEngine.Currency.MZN)));

        panel.add(infoCard, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(totalsCard, BorderLayout.SOUTH);

        ModernFormDialog dlg = new ModernFormDialog(
                UIHelper.mainWindow,
                "Guia de Transferência " + transfer.transferNumber(),
                "fas-truck",
                "Consulta de artigos, caixas e valores da transferência",
                panel
        );
        dlg.asReadOnly("Fechar");

        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir Guia");
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> {
            dlg.close();
            printTransfer(transfer.id(), transfer.transferNumber());
        });
        dlg.addActionButton(printBtn);

        dlg.setSize(940, 540);
        dlg.showDialog();
    }

    private JPanel createMetaItem(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        l.setForeground(UIHelper.TEXT_MUTED);
        JLabel v = new JLabel(value != null && !value.isBlank() ? value : "—");
        v.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        v.setForeground(UIHelper.TEXT_LIGHT);
        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private JPanel createTotalBadge(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        l.setForeground(UIHelper.TEXT_MUTED);
        JLabel v = new JLabel(value);
        v.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        v.setForeground(UIHelper.ACCENT_BLUE);
        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private void printTransfer(Long transferId, String transferNumber) {
        UIHelper.runWithProgress(owner, "A gerar guia de transferência…",
                () -> owner.stockTransferApiClient.renderTransfer(transferId),
                pdf -> PrintPreviewDialog.show(owner, pdf, "transferencia-" + transferNumber),
                owner::showStockError);
    }

    private record FefoPreview(String batch, String expiration) {}

}
