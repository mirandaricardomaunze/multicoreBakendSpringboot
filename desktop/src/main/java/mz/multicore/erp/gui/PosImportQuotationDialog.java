package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.DialogChrome;
import mz.multicore.erp.gui.components.UserErrors;
import mz.multicore.erp.gui.components.Spacing;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.Typography;
import mz.multicore.erp.gui.components.ButtonStyles;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableCellRenderers;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Diálogo modal para pesquisa e importação rápida de cotações abertas no Ponto de Venda (POS).
 * Permite ao operador selecionar uma cotação vigente para carregar o cliente, armazém
 * e carrinho com os preços acordados na proposta comercial.
 */
public class PosImportQuotationDialog extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final int COL_ID = 0;
    private static final int COL_NUMBER = 1;
    private static final int COL_TOTAL = 5;

    private final ComercialApiClient comercialApiClient;
    private final Long companyId;
    private final Consumer<QuotationDTO> onImportCallback;

    private final DefaultTableModel model;
    private final JTable table;
    private final List<QuotationDTO> quotationsList = new ArrayList<>();
    private QuotationDTO selectedQuotation;

    public PosImportQuotationDialog(Window owner,
                                    ComercialApiClient comercialApiClient,
                                    Long companyId,
                                    Consumer<QuotationDTO> onImportCallback) {
        super(owner, "Importar Cotação no POS", ModalityType.APPLICATION_MODAL);
        this.comercialApiClient = comercialApiClient;
        this.companyId = companyId;
        this.onImportCallback = onImportCallback;

        setSize(780, 520);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(UIHelper.BG_DARK);


        // Center card
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(Spacing.LG, Spacing.LG, Spacing.LG, Spacing.LG));

        String[] cols = {"ID", "Nº Cotação", "Data", "Cliente", "NUIT", "Total", "Validade", "Linhas"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.getColumnModel().getColumn(COL_TOTAL).setCellRenderer(TableCellRenderers.money());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(32);

        // Esconder ID
        table.getColumnModel().getColumn(COL_ID).setMinWidth(0);
        table.getColumnModel().getColumn(COL_ID).setMaxWidth(0);
        table.getColumnModel().getColumn(COL_ID).setWidth(0);

        JTextField searchField = TableFilter.searchField("Pesquisar por nº de cotação, cliente ou NUIT…");
        TableFilter.install(table, searchField, List.of(), List.of());

        JPanel filterBar = new JPanel(new BorderLayout(8, 0));
        filterBar.setOpaque(false);
        filterBar.add(searchField, BorderLayout.CENTER);

        ModernButton refreshBtn = UIHelper.createRefreshButton(() -> loadOpenQuotations());
        filterBar.add(refreshBtn, BorderLayout.EAST);

        card.add(filterBar, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);

        ModernButton cancelBtn = ButtonStyles.cancelButton();
        cancelBtn.addActionListener(e -> dispose());

        ModernButton importBtn = ButtonStyles.main(UIHelper.createSuccessButton("Importar para o Carrinho"));
        importBtn.setIcon(UIHelper.icon("fas-file-import", 13));
        importBtn.addActionListener(e -> confirmSelection());

        setContentPane(DialogChrome.frame("fas-file-import", "Importar Cotação / Pró-forma",
                "Seleccione uma cotação aberta para preencher o cliente e artigos no carrinho do POS",
                card, DialogChrome.footer(cancelBtn, importBtn)));

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    confirmSelection();
                }
            }
        });

        loadOpenQuotations();
    }

    private void loadOpenQuotations() {
        UIHelper.runWithProgress(this, "A carregar cotações abertas…",
                () -> comercialApiClient.getOpenQuotationsByCompany(companyId),
                quotations -> {
                    quotationsList.clear();
                    model.setRowCount(0);
                    if (quotations != null) {
                        quotationsList.addAll(quotations);
                        for (QuotationDTO q : quotations) {
                            String dateStr = q.quotationDate() != null ? q.quotationDate().format(DATE_TIME) : "—";
                            String validStr = q.validUntil() != null ? q.validUntil().format(DATE_FMT) : "—";
                            int lineCount = q.lines() != null ? q.lines().size() : 0;
                            model.addRow(new Object[]{
                                    q.id(),
                                    q.quotationNumber(),
                                    dateStr,
                                    q.clientName(),
                                    q.clientTaxId() != null && !q.clientTaxId().isBlank() ? q.clientTaxId() : "—",
                                    q.totalAmount(),
                                    validStr,
                                    lineCount + " artigo(s)"
                            });
                        }
                    }
                    if (model.getRowCount() > 0) {
                        table.setRowSelectionInterval(0, 0);
                    }
                },
                ex -> UserErrors.show(this, "carregar cotações", ex)
        );
    }

    private void confirmSelection() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            ModernMessageDialog.show(this, FeedbackType.WARNING, "Aviso",
                    "Seleccione uma cotação para importar.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        Long id = (Long) model.getValueAt(modelRow, COL_ID);

        UIHelper.runWithProgress(this, "A obter detalhes da cotação…",
                () -> comercialApiClient.getQuotationById(id),
                fullQuotation -> {
                    if (fullQuotation == null || fullQuotation.lines() == null || fullQuotation.lines().isEmpty()) {
                        ModernMessageDialog.show(this, FeedbackType.WARNING, "Aviso",
                                "A cotação seleccionada não contém artigos para faturar.");
                        return;
                    }
                    this.selectedQuotation = fullQuotation;
                    dispose();
                    if (onImportCallback != null) {
                        onImportCallback.accept(fullQuotation);
                    }
                },
                ex -> UserErrors.show(this, "carregar cotação", ex)
        );
    }

    public QuotationDTO getSelectedQuotation() {
        return selectedQuotation;
    }
}
