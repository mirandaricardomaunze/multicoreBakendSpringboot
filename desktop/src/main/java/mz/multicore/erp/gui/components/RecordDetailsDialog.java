package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Inspector corporativo universal, DRY e só-leitura para qualquer linha de tabela do Multicore ERP.
 * Segue o padrão de Property Sheet e Data Inspector de grandes ERPs (SAP, Dynamics, Primavera):
 * 1. Grelha de propriedades densa em duas colunas (Propriedade | Valor) com filtragem em tempo real.
 * 2. Seleção de texto nativa (Ctrl+C direto em qualquer célula ou linha).
 * 3. Grelha tabular real para linhas de documentos comerciais quando aplicável.
 * 4. Auditoria técnica e ferramentas de partilha / exportação no rodapé.
 */
public final class RecordDetailsDialog {
    private RecordDetailsDialog() {}

    public static ProductDTO findProduct(JTable table, int row) {
        if (table == null || row < 0 || row >= table.getRowCount()) return null;
        for (int col = 0; col < table.getColumnCount(); col++) {
            Object val = table.getValueAt(row, col);
            if (val instanceof ProductDTO p) return p;
        }
        return null;
    }

    public static ClientDTO findClient(JTable table, int row) {
        if (table == null || row < 0 || row >= table.getRowCount()) return null;
        for (int col = 0; col < table.getColumnCount(); col++) {
            Object val = table.getValueAt(row, col);
            if (val instanceof ClientDTO c) return c;
        }
        return null;
    }

    public static OrderDTO findOrder(JTable table, int row) {
        if (table == null || row < 0 || row >= table.getRowCount()) return null;
        for (int col = 0; col < table.getColumnCount(); col++) {
            Object val = table.getValueAt(row, col);
            if (val instanceof OrderDTO o) return o;
        }
        return null;
    }

    private static final java.util.concurrent.atomic.AtomicBoolean OPENING = new java.util.concurrent.atomic.AtomicBoolean(false);

    public static void show(JTable table) {
        if (table == null) return;
        if (table.getSelectedRow() < 0) {
            ToastManager.show(table, FeedbackType.INFO, "Seleccione uma linha na tabela para inspecionar os detalhes.");
            return;
        }
        if (!OPENING.compareAndSet(false, true)) return;
        try {
            showInternal(table);
        } finally {
            SwingUtilities.invokeLater(() -> OPENING.set(false));
        }
    }

    private static void showInternal(JTable table) {
        int selectedRow = table.getSelectedRow();
        String mainTitle = "Detalhes do Registo";
        String mainSubtitle = "Inspeção de dados da grelha (Linha #" + (selectedRow + 1) + " de " + table.getRowCount() + ")";

        ProductDTO product = findProduct(table, selectedRow);
        ClientDTO client = findClient(table, selectedRow);
        OrderDTO order = findOrder(table, selectedRow);

        if (order != null) {
            mainTitle = "Encomenda " + order.orderNumber();
            mainSubtitle = "Cliente: " + order.clientName() + (order.createdAt() != null ? " • " + order.createdAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        } else if (product != null) {
            mainTitle = product.name() != null && !product.name().isBlank() ? product.name() : "Artigo #" + product.id();
            String refPart = product.reference() != null && !product.reference().isBlank() ? "Ref: " + product.reference() + " • " : "";
            String skuPart = product.sku() != null && !product.sku().isBlank() ? "SKU: " + product.sku() : "";
            String eanPart = product.barcode() != null && !product.barcode().isBlank() ? " • EAN: " + product.barcode() : "";
            mainSubtitle = refPart + skuPart + eanPart;
            if (mainSubtitle.isBlank()) mainSubtitle = "Ficha cadastral do artigo";
        } else if (client != null) {
            mainTitle = client.name() != null && !client.name().isBlank() ? client.name() : "Cliente #" + client.id();
            mainSubtitle = "NUIT: " + (client.taxId() != null ? client.taxId() : "—") + (client.code() != null ? " • Cód: " + client.code() : "");
        } else {
            if (table.getColumnCount() > 0) {
                Object val0 = table.getValueAt(selectedRow, 0);
                String text0 = valueText(val0);
                if (!text0.isBlank()) mainTitle = text0;
                if (table.getColumnCount() > 1) {
                    Object val1 = table.getValueAt(selectedRow, 1);
                    String text1 = valueText(val1);
                    if (!text1.isBlank()) {
                        if ("Detalhes do Registo".equals(mainTitle)) mainTitle = text1;
                        else mainSubtitle = text1 + " • Linha #" + (selectedRow + 1);
                    }
                }
            }
        }

        HeaderTheme theme = resolveHeaderTheme(table);
        if (order != null) theme = new HeaderTheme("fas-shopping-bag", UIHelper.ACCENT_BLUE);
        else if (product != null) theme = new HeaderTheme("fas-box-open", UIHelper.APPROVED_GREEN);
        else if (client != null) theme = new HeaderTheme("fas-user-tie", UIHelper.ACCENT_BLUE);

        ExecutiveDetailDialog dialog = ExecutiveDetailDialog.create(table, "Ficha de Detalhes — " + mainTitle);
        dialog.setTitle(mainTitle)
                .setSubtitle(mainSubtitle)
                .setHeaderIcon(theme.icon(), 28, theme.accent());

        // 1. Extração de KPIs e Estado
        String statusText = null;
        ExecutiveDetailDialog.StatusSeverity severity = ExecutiveDetailDialog.StatusSeverity.INFO;
        int kpiCount = 0;

        for (int col = 0; col < table.getColumnCount(); col++) {
            if (isHidden(table, col)) continue;
            String colName = table.getColumnName(col);
            String valStr = valueText(table.getValueAt(selectedRow, col));
            if (colName == null) continue;
            String colLower = colName.toLowerCase();

            if (colLower.contains("estado") || colLower.contains("status") || colLower.contains("activo") || colLower.contains("ativo")) {
                statusText = valStr;
                String upper = valStr.toUpperCase();
                if (upper.contains("OK") || upper.contains("ACTIV") || upper.contains("APROV") || upper.contains("CONCLU") || upper.contains("PAGO") || upper.contains("EMITID")) {
                    severity = ExecutiveDetailDialog.StatusSeverity.SUCCESS;
                } else if (upper.contains("PEND") || upper.contains("ALERTA") || upper.contains("SUSP") || upper.contains("ANALIS") || upper.contains("RASCUNHO")) {
                    severity = ExecutiveDetailDialog.StatusSeverity.WARNING;
                } else if (upper.contains("CANCEL") || upper.contains("ERRO") || upper.contains("INACT") || upper.contains("BLOQ") || upper.contains("REJEIT") || upper.contains("ANUL")) {
                    severity = ExecutiveDetailDialog.StatusSeverity.DANGER;
                }
            }

            if (kpiCount < 4 && (colLower.contains("total") || colLower.contains("valor") || colLower.contains("saldo")
                    || colLower.contains("preço") || colLower.contains("preco") || colLower.contains("qtd")
                    || colLower.contains("quantidade") || colLower.contains("limite"))) {
                Color accent = (colLower.contains("total") || colLower.contains("valor") || colLower.contains("preço") || colLower.contains("saldo"))
                        ? UIHelper.APPROVED_GREEN : UIHelper.ACCENT_CYAN;
                String kpiIcon = colLower.contains("qtd") || colLower.contains("quantidade") ? "fas-boxes" : "fas-money-bill-wave";
                dialog.addKpi(colName, valStr.isBlank() ? "—" : valStr, "Atributo destacado", accent, kpiIcon);
                kpiCount++;
            }
        }

        if (order != null && kpiCount == 0) {
            dialog.addKpi("Valor Total", String.format("%.2f MT", order.totalAmount()), "Total com impostos", UIHelper.APPROVED_GREEN, "fas-money-bill-wave");
            if (order.lines() != null) {
                dialog.addKpi("Linhas de Artigos", String.valueOf(order.lines().size()), "Produtos no documento", UIHelper.ACCENT_BLUE, "fas-boxes");
            }
            if (order.warehouseName() != null) {
                dialog.addKpi("Armazém Origem", order.warehouseName(), "Local de expedição", UIHelper.ACCENT_CYAN, "fas-warehouse");
            }
        } else if (product != null && kpiCount == 0) {
            if (product.unitPrice() != null) {
                dialog.addKpi("Preço de Venda", String.format("%.2f MT", product.unitPrice()), "Preço unitário", UIHelper.APPROVED_GREEN, "fas-money-bill-wave");
            }
            if (product.purchasePrice() != null) {
                dialog.addKpi("Preço de Custo", String.format("%.2f MT", product.purchasePrice()), "Custo de aquisição", UIHelper.ACCENT_CYAN, "fas-coins");
            }
            if (product.minStock() != null) {
                dialog.addKpi("Stock Mínimo", product.minStock().toPlainString() + " un.", "Segurança", UIHelper.PENDING_YELLOW, "fas-cubes");
            }
        }

        dialog.setStatusBadge(statusText != null && !statusText.isBlank() ? statusText : (order != null ? order.status() : "REGISTO"), severity);

        // 2. Aba 1: Grelha de Propriedades Corporativa (Property Sheet universal)
        List<PropertyItem> properties = extractProperties(table, selectedRow, product, client, order);
        dialog.addTab("Propriedades do Registo", "fas-id-card", UIHelper.ACCENT_BLUE, buildPropertySheet(properties));

        // 3. Se for Encomenda ou contiver linhas: Aba 2 com Tabela Corporativa Real
        if (order != null && order.lines() != null && !order.lines().isEmpty()) {
            dialog.addTab("Itens do Documento", "fas-list", UIHelper.APPROVED_GREEN, buildDocumentLinesTable(order));
        }

        // 4. Aba: Auditoria e Rastreabilidade do Sistema
        dialog.addTab("Rastreabilidade & Auditoria", "fas-shield-alt", UIHelper.APPROVED_GREEN, buildAuditTable(table, selectedRow));

        // 5. Aba: Partilha e Exportação
        dialog.addTab("Partilha Executiva", "fas-share-alt", UIHelper.ACCENT_CYAN, buildSharePreviewTab(table, selectedRow, mainTitle));

        final String finalTitle = mainTitle;

        // 6. Ações Corporativas no Rodapé
        ModernButton copySummaryBtn = new ModernButton("Copiar Ficha", UIHelper.ACCENT_CYAN, UIHelper.ACCENT_CYAN.darker());
        copySummaryBtn.setIcon(UIHelper.icon("fas-copy", 14, Color.WHITE));
        copySummaryBtn.setToolTipText("Copiar ficha completa tabulada para WhatsApp, E-mail ou Relatório");
        copySummaryBtn.addActionListener(e -> {
            String summary = buildExecutiveSummary(table, selectedRow, finalTitle, properties);
            copyToClipboard(summary);
            ToastManager.success(dialog.getDialog(), "Ficha de registo copiada com sucesso.");
        });
        dialog.addLeftAction(copySummaryBtn);

        ModernButton copyJsonBtn = UIHelper.createSecondaryButton("Copiar JSON");
        copyJsonBtn.setIcon(UIHelper.icon("fas-code", 13));
        copyJsonBtn.setToolTipText("Copiar atributos estruturados em formato JSON");
        copyJsonBtn.addActionListener(e -> {
            String json = buildJson(properties);
            copyToClipboard(json);
            ToastManager.success(dialog.getDialog(), "JSON copiado para a área de transferência.");
        });
        dialog.addLeftAction(copyJsonBtn);

        ModernButton exportBtn = UIHelper.createSecondaryButton("Exportar TXT");
        exportBtn.setIcon(UIHelper.icon("fas-file-download", 13));
        exportBtn.setToolTipText("Gravar resumo em ficheiro de texto");
        exportBtn.addActionListener(e -> {
            String summary = buildExecutiveSummary(table, selectedRow, finalTitle, properties);
            String defaultName = "registo-" + sanitizeFileName(finalTitle) + ".txt";
            exportToFile(dialog.getDialog(), summary, defaultName);
        });
        dialog.addLeftAction(exportBtn);

        dialog.showDialog();
    }

    /**
     * Constrói o Property Sheet Corporativo (Grelha densa de 2 colunas: Propriedade | Valor).
     * Padrão canónico de ERPs empresariais como SAP, Oracle e Dynamics.
     */
    public static JComponent buildPropertySheet(List<PropertyItem> items) {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(new EmptyBorder(10, 12, 10, 12));

        String[] cols = {"Propriedade", "Valor Registado"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (PropertyItem item : items) {
            model.addRow(new Object[]{item.label(), item.value()});
        }

        JTable table = new JTable(model);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(table);
        table.setRowHeight(28);
        table.setCellSelectionEnabled(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(240);
        table.getColumnModel().getColumn(0).setMaxWidth(340);
        table.getColumnModel().getColumn(1).setPreferredWidth(520);

        table.getColumnModel().getColumn(0).setCellRenderer((t, val, isSelected, hasFocus, r, c) -> {
            JLabel lbl = new JLabel("  " + String.valueOf(val != null ? val : ""));
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lbl.setForeground(isSelected ? Color.WHITE : UIHelper.TEXT_MUTED);
            lbl.setBackground(isSelected ? UIHelper.ACCENT_BLUE : (r % 2 == 0 ? UIHelper.BG_CARD : UIHelper.BG_DARK));
            lbl.setOpaque(true);
            return lbl;
        });

        table.getColumnModel().getColumn(1).setCellRenderer((t, val, isSelected, hasFocus, r, c) -> {
            String str = String.valueOf(val != null ? val : "—");
            JLabel lbl = new JLabel("  " + str);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            boolean isMoney = str.contains("MT") || str.contains("MZN");
            lbl.setForeground(isSelected ? Color.WHITE : (isMoney ? UIHelper.APPROVED_GREEN : UIHelper.TEXT_LIGHT));
            lbl.setBackground(isSelected ? UIHelper.ACCENT_BLUE : (r % 2 == 0 ? UIHelper.BG_CARD : UIHelper.BG_DARK));
            lbl.setOpaque(true);
            return lbl;
        });

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField searchField = TableFilter.searchField("Filtrar propriedades do registo...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String q = searchField.getText().trim();
                if (q.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q)));
            }
        });

        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setOpaque(false);
        topBar.add(searchField, BorderLayout.CENTER);

        JLabel countLbl = new JLabel(items.size() + " atributos");
        countLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        countLbl.setForeground(UIHelper.TEXT_MUTED);
        countLbl.setBorder(new EmptyBorder(0, 8, 0, 0));
        topBar.add(countLbl, BorderLayout.EAST);

        root.add(topBar, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        root.add(scroll, BorderLayout.CENTER);

        return root;
    }

    /**
     * Tabela corporativa de itens / linhas de documento (quando o registo possui linhas, como Encomenda).
     */
    private static JComponent buildDocumentLinesTable(OrderDTO order) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));

        String[] cols = {"#", "Artigo / Designação", "Lote", "Quantidade", "Embalagem", "Preço Unit.", "Total"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        if (order.lines() != null) {
            int lineNum = 1;
            for (var line : order.lines()) {
                String lote = (line.batchNumber() != null && !line.batchNumber().isBlank() && !line.batchNumber().startsWith("LEGACY"))
                        ? line.batchNumber() : "—";
                String emb = line.unitsPerBox() > 1 ? line.unitsPerBox() + " un/cx" : "Unidade";
                model.addRow(new Object[]{
                        lineNum++,
                        line.productName(),
                        lote,
                        line.quantity() + " un.",
                        emb,
                        String.format("%.2f MT", line.unitPrice()),
                        String.format("%.2f MT", line.lineTotal())
                });
            }
        }

        JTable table = new JTable(model);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.getColumnModel().getColumn(0).setMaxWidth(45);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Grelha corporativa de auditoria e rastreabilidade (metadados de sessão).
     */
    private static JComponent buildAuditTable(JTable table, int row) {
        List<PropertyItem> auditItems = new ArrayList<>();
        auditItems.add(new PropertyItem("Operador em Sessão", CurrentUserContext.getUsername()));
        String role = CurrentUserContext.getRole();
        auditItems.add(new PropertyItem("Perfil / Credencial", role != null && !role.isBlank() ? role : "Operador Autorizado"));
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        auditItems.add(new PropertyItem("Empresa / Tenant", companyId != null ? "Empresa #" + companyId : "Multicore ERP Principal"));
        auditItems.add(new PropertyItem("Carimbo de Data / Hora", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
        auditItems.add(new PropertyItem("Referência de Grelha", "Linha #" + (row + 1) + " de " + table.getRowCount() + " (" + table.getColumnCount() + " colunas)"));
        auditItems.add(new PropertyItem("Modo de Inspeção", "Estritamente Só-Leitura (Garantia de Integridade)"));
        auditItems.add(new PropertyItem("Motor Canónico", "RecordDetailsDialog Tier-1 Enterprise Inspector"));

        return buildPropertySheet(auditItems);
    }

    private static JComponent buildSharePreviewTab(JTable table, int row, String title) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel info = new JLabel("Formato de texto tabulado para conferência, partilha ou envio oficial:");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        info.setForeground(UIHelper.TEXT_MUTED);
        panel.add(info, BorderLayout.NORTH);

        List<PropertyItem> props = extractProperties(table, row, findProduct(table, row), findClient(table, row), findOrder(table, row));
        String summary = buildExecutiveSummary(table, row, title, props);
        JTextArea area = new JTextArea(summary);
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setForeground(UIHelper.TEXT_LIGHT);
        area.setBackground(UIHelper.BG_CARD);
        area.setBorder(new EmptyBorder(12, 12, 12, 12));
        area.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Extrai e desdobra as propriedades de forma 100% DRY.
     */
    private static List<PropertyItem> extractProperties(JTable table, int row, ProductDTO product, ClientDTO client, OrderDTO order) {
        List<PropertyItem> list = new ArrayList<>();

        if (order != null) {
            list.add(new PropertyItem("Nº Encomenda", order.orderNumber()));
            list.add(new PropertyItem("Cliente", order.clientName()));
            if (order.clientTaxId() != null) list.add(new PropertyItem("NUIT / NIF", order.clientTaxId()));
            if (order.walkInName() != null && !order.walkInName().isBlank()) list.add(new PropertyItem("Comprador", order.walkInName()));
            list.add(new PropertyItem("Estado", order.statusLabel() != null ? order.statusLabel() : order.status()));
            list.add(new PropertyItem("Valor Total", String.format("%.2f MT", order.totalAmount())));
            if (order.kindLabel() != null) list.add(new PropertyItem("Via / Tipo de Encomenda", order.kindLabel()));
            if (order.warehouseName() != null) list.add(new PropertyItem("Armazém Origem", order.warehouseName()));
            if (order.paymentTerms() != null) list.add(new PropertyItem("Termos de Pagamento", order.paymentTerms()));
            if (order.deliveryTerms() != null) list.add(new PropertyItem("Termos de Entrega", order.deliveryTerms()));
            if (order.expectedDeliveryDate() != null) {
                String d = order.expectedDeliveryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                list.add(new PropertyItem("Data de Entrega Prevista", order.deliveryOverdue() ? d + " (em atraso)" : d));
            }
            if (order.quotationNumber() != null && !order.quotationNumber().isBlank()) list.add(new PropertyItem("Cotação de Origem", order.quotationNumber()));
            list.add(new PropertyItem("Impressões Oficiais", order.printCount() + " vez(es)"));
            return list;
        }

        for (int col = 0; col < table.getColumnCount(); col++) {
            if (isHidden(table, col)) continue;
            String label = table.getColumnName(col);
            Object rawVal = table.getValueAt(row, col);
            String val = valueText(rawVal);
            list.add(new PropertyItem(label, val.isBlank() ? "—" : val));
        }

        if (product != null) {
            if (product.barcode() != null && !product.barcode().isBlank() && list.stream().noneMatch(p -> p.label.toLowerCase().contains("barra"))) {
                list.add(new PropertyItem("Código de Barras (EAN)", product.barcode()));
            }
            if (product.sku() != null && !product.sku().isBlank() && list.stream().noneMatch(p -> p.label.equalsIgnoreCase("sku"))) {
                list.add(new PropertyItem("Código SKU", product.sku()));
            }
            if (product.reference() != null && !product.reference().isBlank() && list.stream().noneMatch(p -> p.label.toLowerCase().contains("ref"))) {
                list.add(new PropertyItem("Referência de Fabricante", product.reference()));
            }
            if (product.purchasePrice() != null && list.stream().noneMatch(p -> p.label.toLowerCase().contains("custo") || p.label.toLowerCase().contains("compra"))) {
                list.add(new PropertyItem("Preço de Custo (Compra)", String.format("%.2f MT", product.purchasePrice())));
            }
            if (product.unitPrice() != null && product.purchasePrice() != null) {
                BigDecimal margin = product.unitPrice().subtract(product.purchasePrice());
                if (product.purchasePrice().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal pct = margin.multiply(BigDecimal.valueOf(100)).divide(product.purchasePrice(), 1, RoundingMode.HALF_UP);
                    list.add(new PropertyItem("Margem Bruta Estimada", String.format("%.2f MT (%s%%)", margin, pct.toPlainString())));
                }
            }
            if (product.minStock() != null && list.stream().noneMatch(p -> p.label.toLowerCase().contains("mínimo") || p.label.toLowerCase().contains("minimo"))) {
                list.add(new PropertyItem("Stock Mínimo de Segurança", product.minStock().toPlainString() + " un."));
            }
            if (product.taxRateLabel() != null && list.stream().noneMatch(p -> p.label.toLowerCase().contains("taxa") || p.label.toLowerCase().contains("iva"))) {
                list.add(new PropertyItem("Taxa de IVA", product.taxRateLabel()));
            }
            if (product.unitsPerBox() > 1 && list.stream().noneMatch(p -> p.label.toLowerCase().contains("caixa") || p.label.toLowerCase().contains("emb"))) {
                list.add(new PropertyItem("Composição de Embalagem", product.unitsPerBox() + " un/cx (" + product.packagesPerBox() + " emb/cx)"));
            }
            if (product.description() != null && !product.description().isBlank() && list.stream().noneMatch(p -> p.label.toLowerCase().contains("descriç"))) {
                list.add(new PropertyItem("Descrição Detalhada", product.description()));
            }
        } else if (client != null) {
            if (client.taxId() != null && !client.taxId().isBlank() && list.stream().noneMatch(p -> p.label.toLowerCase().contains("nuit") || p.label.toLowerCase().contains("nif"))) {
                list.add(new PropertyItem("NUIT / NIF", client.taxId()));
            }
            if (client.email() != null && !client.email().isBlank() && list.stream().noneMatch(p -> p.label.toLowerCase().contains("email"))) {
                list.add(new PropertyItem("Correio Electrónico", client.email()));
            }
            if (client.address() != null && !client.address().isBlank() && list.stream().noneMatch(p -> p.label.toLowerCase().contains("morada"))) {
                list.add(new PropertyItem("Morada / Sede", client.address()));
            }
            if (client.creditLimit() != null && list.stream().noneMatch(p -> p.label.toLowerCase().contains("limite"))) {
                list.add(new PropertyItem("Limite de Crédito", String.format("%.2f MT", client.creditLimit())));
            }
        }

        return list;
    }

    private static String buildExecutiveSummary(JTable table, int row, String title, List<PropertyItem> properties) {
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("MULTICORE ERP | FICHA CORPORATIVA DE REGISTO\n");
        sb.append("Identificador: ").append(title).append("\n");
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        sb.append("Data da Consulta: ").append(nowStr).append("\n");
        String username = CurrentUserContext.getUsername();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        sb.append("Operador: ").append(username).append(" | Empresa: ").append(companyId != null ? "#" + companyId : "Padrão").append("\n");
        sb.append("====================================================\n");
        for (PropertyItem item : properties) {
            sb.append(String.format("%-28s : %s\n", item.label(), item.value()));
        }
        sb.append("====================================================\n");
        sb.append("Documento emitido para conferência oficial interna.\n");
        sb.append("Multicore Enterprise ERP • Sistema Certificado\n");
        return sb.toString();
    }

    private static String buildJson(List<PropertyItem> properties) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (int i = 0; i < properties.size(); i++) {
            PropertyItem p = properties.get(i);
            sb.append("  \"").append(escapeJson(p.label())).append("\": \"").append(escapeJson(p.value())).append("\"");
            if (i < properties.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static void copyToClipboard(String content) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(content), null);
        } catch (Exception ignored) {}
    }

    private static void exportToFile(Component parent, String content, String defaultFileName) {
        if (GraphicsEnvironment.isHeadless()) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exportar Registo");
        chooser.setSelectedFile(new File(defaultFileName));
        int result = chooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
                ToastManager.success(parent, "Ficheiro exportado para " + file.getName());
            } catch (Exception ex) {
                ToastManager.show(parent, FeedbackType.ERROR, "Erro ao gravar: " + ex.getMessage());
            }
        }
    }

    private static String sanitizeFileName(String name) {
        if (name == null) return "registo";
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    static boolean isHidden(JTable table, int viewColumn) {
        var column = table.getColumnModel().getColumn(viewColumn);
        return column.getWidth() == 0 && column.getMaxWidth() == 0;
    }

    public static String valueText(Object value) {
        if (value == null) return "";
        if (value instanceof ProductDTO p) {
            return p.name() != null && !p.name().isBlank() ? p.name() : (p.reference() != null ? p.reference() : "Produto #" + p.id());
        }
        if (value instanceof ClientDTO c) {
            return c.name() != null && !c.name().isBlank() ? c.name() : "Cliente #" + c.id();
        }
        if (value instanceof OrderDTO o) {
            return o.orderNumber() != null ? o.orderNumber() : "Encomenda #" + o.id();
        }
        if (value instanceof mz.multicore.erp.modules.purchases.dto.SupplierDTO s) {
            return s.name() != null && !s.name().isBlank() ? s.name() : "Fornecedor #" + s.id();
        }
        if (value instanceof mz.multicore.erp.modules.inventory.dto.WarehouseDTO w) {
            return w.name() != null && !w.name().isBlank() ? w.name() : "Armazém #" + w.id();
        }
        String s = String.valueOf(value);
        if (s.startsWith("ProductDTO[") && s.contains("name=")) {
            int idx = s.indexOf("name=");
            int end = s.indexOf(",", idx);
            if (end == -1) end = s.indexOf("]", idx);
            if (end > idx + 5) return s.substring(idx + 5, end).trim();
        }
        if (s.startsWith("ClientDTO[") && s.contains("name=")) {
            int idx = s.indexOf("name=");
            int end = s.indexOf(",", idx);
            if (end == -1) end = s.indexOf("]", idx);
            if (end > idx + 5) return s.substring(idx + 5, end).trim();
        }
        return s;
    }

    private static HeaderTheme resolveHeaderTheme(JTable table) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            String name = table.getColumnName(i);
            if (name == null) continue;
            String lower = name.toLowerCase();
            if (lower.contains("factura") || lower.contains("fatura")) return new HeaderTheme("fas-file-invoice-dollar", UIHelper.ACCENT_CYAN);
            if (lower.contains("encomenda") || lower.contains("pedido")) return new HeaderTheme("fas-shopping-cart", UIHelper.ACCENT_BLUE);
            if (lower.contains("cliente")) return new HeaderTheme("fas-user-tie", UIHelper.ACCENT_BLUE);
            if (lower.contains("fornecedor")) return new HeaderTheme("fas-truck", UIHelper.ACCENT_ORANGE);
            if (lower.contains("desperdício") || lower.contains("quebra") || lower.contains("perda")) return new HeaderTheme("fas-trash-alt", UIHelper.REJECTED_RED);
            if (lower.contains("inventário") || lower.contains("contagem")) return new HeaderTheme("fas-clipboard-check", UIHelper.APPROVED_GREEN);
            if (lower.contains("artigo") || lower.contains("produto") || lower.contains("stock")) return new HeaderTheme("fas-boxes", UIHelper.APPROVED_GREEN);
            if (lower.contains("pagamento") || lower.contains("recibo")) return new HeaderTheme("fas-receipt", UIHelper.APPROVED_GREEN);
            if (lower.contains("cotação")) return new HeaderTheme("fas-file-alt", UIHelper.PENDING_YELLOW);
        }
        return new HeaderTheme("fas-file-contract", UIHelper.ACCENT_BLUE);
    }

    public record PropertyItem(String label, String value) {}
    private record HeaderTheme(String icon, Color accent) {}
}
