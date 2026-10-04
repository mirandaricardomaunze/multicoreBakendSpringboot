package mz.multicore.erp.gui;

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
        setLayout(new BorderLayout());
        getContentPane().setBackground(UIHelper.BG_DARK);

        // Header
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 20, 12, 20));

        JLabel titleIcon = new JLabel(UIHelper.icon("fas-file-import", 22, UIHelper.ACCENT_CYAN));
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Importar Cotação / Pró-forma");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        title.setForeground(UIHelper.TEXT_LIGHT);

        JLabel subtitle = new JLabel("Selecione uma cotação aberta para preencher o cliente e artigos no carrinho do POS");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitle.setForeground(UIHelper.TEXT_MUTED);

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(subtitle);

        header.add(titleIcon, BorderLayout.WEST);
        header.add(titlePanel, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // Center card
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        String[] cols = {"ID", "Nº Cotação", "Data", "Cliente", "NUIT", "Total", "Validade", "Linhas"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);
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

        ModernButton refreshBtn = UIHelper.createPrimaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 12, Color.WHITE));
        refreshBtn.addActionListener(e -> loadOpenQuotations());
        filterBar.add(refreshBtn, BorderLayout.EAST);

        card.add(filterBar, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(new EmptyBorder(0, 16, 10, 16));
        centerWrapper.add(card, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // Bottom footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(UIHelper.ROW_ALT);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.BORDER));

        ModernButton cancelBtn = UIHelper.createDangerButton("Cancelar");
        cancelBtn.setIcon(UIHelper.icon("fas-times", 13, Color.WHITE));
        cancelBtn.addActionListener(e -> dispose());

        ModernButton importBtn = UIHelper.createSuccessButton("Importar para o Carrinho");
        importBtn.setIcon(UIHelper.icon("fas-file-import", 13));
        importBtn.addActionListener(e -> confirmSelection());

        footer.add(cancelBtn);
        footer.add(importBtn);
        add(footer, BorderLayout.SOUTH);

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
                ex -> JOptionPane.showMessageDialog(this,
                        "Erro ao carregar cotações: " + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void confirmSelection() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione uma cotação para importar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        Long id = (Long) model.getValueAt(modelRow, COL_ID);

        UIHelper.runWithProgress(this, "A obter detalhes da cotação…",
                () -> comercialApiClient.getQuotationById(id),
                fullQuotation -> {
                    if (fullQuotation == null || fullQuotation.lines() == null || fullQuotation.lines().isEmpty()) {
                        JOptionPane.showMessageDialog(this,
                                "A cotação seleccionada não contém artigos para faturar.",
                                "Aviso", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    this.selectedQuotation = fullQuotation;
                    dispose();
                    if (onImportCallback != null) {
                        onImportCallback.accept(fullQuotation);
                    }
                },
                ex -> JOptionPane.showMessageDialog(this,
                        "Falha ao carregar cotação: " + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE)
        );
    }

    public QuotationDTO getSelectedQuotation() {
        return selectedQuotation;
    }
}
