package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.WeighbridgeApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.pos.scale.ScaleConnection;
import mz.multicore.erp.modules.pos.scale.SerialScaleReader;
import mz.multicore.erp.modules.weighbridge.dto.FirstWeighingRequest;
import mz.multicore.erp.modules.weighbridge.dto.SecondWeighingRequest;
import mz.multicore.erp.modules.weighbridge.dto.WeighingTicketDTO;
import mz.multicore.erp.modules.weighbridge.model.WeighingDirection;
import mz.multicore.erp.modules.weighbridge.model.WeighingStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Báscula (SA-C18, {@code docs/BASCULA_CAMIAO_SPEC.md}). The live weight of the workstation scale is always on
 * top. "1.ª Pesagem" registers the truck at the entrance; "2.ª Pesagem" at the exit gives the net weight. A weight
 * typed by hand is accepted but marked as manual on the list and on the ticket.
 */
public class WeighbridgePanel extends JPanel {

    private static final String[] COLUMNS = {"Talão", "Estado", "Matrícula", "Sentido", "Cliente / fornecedor", "Artigo",
            "1.ª pesagem", "2.ª pesagem", "Líquido (kg)", "Leitura"};
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    static final String INSIDE = "NO RECINTO";
    static final String DONE = "CONCLUÍDO";

    private final WeighbridgeApiClient api;
    private final ComercialApiClient comercialApi;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel liveWeight = new JLabel("—");
    private final JLabel liveStatus = new JLabel(" ");
    private final ModernButton secondBtn = UIHelper.createPrimaryButton("2.ª Pesagem");
    private final ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir Talão");
    private final List<WeighingTicketDTO> tickets = new ArrayList<>();
    private Consumer<List<WeighingTicketDTO>> onLoaded = list -> { };

    public WeighbridgePanel(WeighbridgeApiClient api, ComercialApiClient comercialApi) {
        this.api = api;
        this.comercialApi = comercialApi;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(feedback, BorderLayout.NORTH);
        north.add(buildLive(), BorderLayout.CENTER);
        add(north, BorderLayout.NORTH);
        add(buildList(), BorderLayout.CENTER);
        secondBtn.setEnabled(false);
        printBtn.setEnabled(false);
        new Timer(400, e -> refreshLive()).start();
    }

    public void onLoaded(Consumer<List<WeighingTicketDTO>> listener) {
        this.onLoaded = listener == null ? list -> { } : listener;
    }

    private JComponent buildLive() {
        liveWeight.setFont(new Font(UIHelper.FONT, Font.BOLD, 30));
        liveWeight.setForeground(UIHelper.TEXT_LIGHT);
        liveWeight.setIcon(UIHelper.icon("fas-truck", 26, UIHelper.ACCENT_BLUE));
        liveWeight.setIconTextGap(12);
        liveStatus.setForeground(UIHelper.TEXT_MUTED);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(14, 2));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));
        card.add(liveWeight, BorderLayout.WEST);
        card.add(liveStatus, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildList() {
        UIHelper.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(1).setCellRenderer(TableCellRenderers.status());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateActions();
        });
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        JTextField search = TableFilter.searchField("Matrícula, talão ou cliente…");
        search.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> state = TableFilter.combo("Todos", INSIDE, DONE);
        UIHelper.styleComboBox(state);
        state.setPreferredSize(new Dimension(140, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(table, search, new TableFilter.ColumnFilter(state, 1));
        JPanel filters = TableFilter.bar(search, TableFilter.label("Estado:"), state);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));
        ModernButton firstBtn = UIHelper.createSuccessButton("1.ª Pesagem");
        firstBtn.setIcon(UIHelper.icon("fas-sign-in-alt", 14));
        firstBtn.setToolTipText("Camião à entrada");
        firstBtn.addActionListener(e -> openFirst());
        secondBtn.setIcon(UIHelper.icon("fas-sign-out-alt", 14));
        secondBtn.setToolTipText("Camião à saída: calcula o peso líquido");
        secondBtn.addActionListener(e -> openSecond());
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> print(selected()));
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(ResponsiveSupport.cardTop("Talões de Pesagem (7 dias)", filters, UIHelper.createRefreshButton(this::reload),
                printBtn, secondBtn, firstBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        return card;
    }

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || api == null) return;
        Long keep = selected() == null ? null : selected().id();
        UIHelper.loadAsync(this, () -> api.tickets(companyId, 7), loaded -> apply(loaded, keep),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar a báscula", error.getMessage(), "Tentar novamente", this::reload));
    }

    private void apply(List<WeighingTicketDTO> loaded, Long keep) {
        feedback.clear();
        tickets.clear();
        tickets.addAll(loaded);
        model.setRowCount(0);
        for (WeighingTicketDTO t : tickets) {
            model.addRow(new Object[]{t.number(), t.status() == WeighingStatus.OPEN ? INSIDE : DONE, t.plate(),
                    t.direction() == WeighingDirection.IN ? "Entrada" : "Saída", t.counterparty() == null ? "—" : t.counterparty(),
                    t.productName() == null ? "—" : t.productName(), kg(t.firstWeight()) + "  ·  " + TIME.format(t.firstAt()),
                    t.secondWeight() == null ? "—" : kg(t.secondWeight()) + "  ·  " + TIME.format(t.secondAt()),
                    t.netWeight() == null ? "—" : kg(t.netWeight()), t.manual() ? "Manual" : "Balança"});
        }
        if (keep != null) {
            for (int i = 0; i < tickets.size(); i++) {
                if (!tickets.get(i).id().equals(keep)) continue;
                int view = table.convertRowIndexToView(i);
                if (view >= 0) table.setRowSelectionInterval(view, view);
            }
        }
        updateActions();
        onLoaded.accept(List.copyOf(tickets));
    }

    private void updateActions() {
        WeighingTicketDTO t = selected();
        secondBtn.setEnabled(t != null && t.status() == WeighingStatus.OPEN);
        printBtn.setEnabled(t != null);
    }

    private void refreshLive() {
        if (!isShowing()) return;
        SerialScaleReader reader = ScaleConnection.connectedReader();
        SerialScaleReader.ScaleReading reading = reader.getCurrentReading();
        liveWeight.setText(kg(reading.weightKg()) + " kg");
        liveStatus.setText(reader.isSimulationMode() ? "Sem balança ligada neste posto: o peso terá de ser escrito (fica marcado como manual)."
                : statusText(reading.status()));
    }

    static String statusText(SerialScaleReader.ScaleStatus status) {
        return switch (status) {
            case STABLE -> "Peso estável: pode registar a pesagem.";
            case UNSTABLE -> "A estabilizar…";
            case OVERLOAD -> "Sobrecarga da báscula.";
            case DISCONNECTED -> "Sem resposta da báscula: confirme o cabo e a configuração do posto.";
        };
    }

    /** Weight field with "Ler báscula"; reports whether the value still is the one read from the scale. */
    private record WeightInput(JPanel panel, DecimalField field, BigDecimal[] lastRead) {
        boolean fromScale() {
            BigDecimal value = field.value();
            return value != null && lastRead[0] != null && value.compareTo(lastRead[0]) == 0;
        }
    }

    private WeightInput weightInput() {
        DecimalField field = new DecimalField("", 3, true);
        BigDecimal[] lastRead = new BigDecimal[1];
        ModernButton read = UIHelper.createSecondaryButton("Ler Báscula");
        read.setIcon(UIHelper.icon("fas-balance-scale", 13));
        read.addActionListener(e -> {
            BigDecimal weight = BreakdownFormDialog.stableWeight(this);
            if (weight == null) return;
            lastRead[0] = weight;
            field.setText(weight.toPlainString());
        });
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.add(field, BorderLayout.CENTER);
        panel.add(read, BorderLayout.EAST);
        return new WeightInput(panel, field, lastRead);
    }

    private void openFirst() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(this, comercialApi::getAllProducts, products -> showFirst(companyId, products),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar os artigos", error.getMessage(), null, null));
    }

    private void showFirst(Long companyId, List<ProductDTO> products) {
        JTextField plate = new JTextField();
        UIHelper.styleTextField(plate);
        JTextField driver = new JTextField();
        UIHelper.styleTextField(driver);
        JComboBox<String> direction = new JComboBox<>(new String[]{"Saída de material (venda)", "Entrada de material (compra)"});
        UIHelper.styleComboBox(direction);
        JTextField counterparty = new JTextField();
        UIHelper.styleTextField(counterparty);
        SearchableComboBox<ProductDTO> product = BreakdownFormDialog.productCombo(products, "Artigo (opcional): areia, brita, cimento…");
        JTextField reference = new JTextField();
        UIHelper.styleTextField(reference);
        WeightInput weight = weightInput();
        FormField plateForm = new FormField("Matrícula", plate, true, "Ex.: AAA-123-MP");
        FormField weightForm = new FormField("Peso à entrada (kg)", weight.panel(), true, "Leia da báscula; escrito à mão fica marcado");
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"", plateForm, "Motorista:", driver, "Sentido:", direction,
                "Cliente / fornecedor:", counterparty, "Artigo:", product, "Documento (fatura, guia):", reference, "", weightForm});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "1.ª Pesagem", "fas-sign-in-alt", "Camião à entrada", form)
                .setConfirmButton("Registar Entrada", "fas-check");
        String key = "TP-" + UUID.randomUUID();
        WeighingTicketDTO[] result = new WeighingTicketDTO[1];
        dialog.setOnSaveAsync(() -> {
            if (!plateForm.validateRequired()) throw new IllegalArgumentException("Indique a matrícula.");
            BigDecimal value = weight.field().value();
            if (value == null || value.signum() <= 0) {
                weightForm.setError("Indique o peso.");
                throw new IllegalArgumentException("Indique o peso.");
            }
            ProductDTO p = product.selectedValue();
            FirstWeighingRequest request = new FirstWeighingRequest(companyId, plate.getText().trim(), driver.getText().trim(),
                    direction.getSelectedIndex() == 0 ? WeighingDirection.OUT : WeighingDirection.IN, counterparty.getText().trim(),
                    p == null ? null : p.id(), reference.getText().trim(), value, weight.fromScale(), null, key);
            return () -> { result[0] = api.first(request); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, "Talão " + result[0].number() + ": " + result[0].plate() + " no recinto.");
        reload();
    }

    private void openSecond() {
        WeighingTicketDTO t = selected();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (t == null || companyId == null) return;
        WeightInput weight = weightInput();
        JTextField reference = new JTextField(t.reference() == null ? "" : t.reference());
        UIHelper.styleTextField(reference);
        JTextArea info = ResponsiveSupport.wrappingText(t.plate() + " entrou com " + kg(t.firstWeight()) + " kg ("
                + (t.direction() == WeighingDirection.OUT ? "vem buscar material: deve sair mais pesado" : "traz material: deve sair mais leve")
                + ").", new Font(UIHelper.FONT, Font.PLAIN, 12), UIHelper.TEXT_LIGHT);
        FormField weightForm = new FormField("Peso à saída (kg)", weight.panel(), true, null);
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "2.ª Pesagem " + t.number(), "fas-sign-out-alt",
                "Camião à saída", UIHelper.createDialogForm(1, new Object[]{"", info, "", weightForm, "Documento (fatura, guia):", reference}))
                .setConfirmButton("Fechar Talão", "fas-check");
        WeighingTicketDTO[] result = new WeighingTicketDTO[1];
        dialog.setOnSaveAsync(() -> {
            BigDecimal value = weight.field().value();
            if (value == null || value.signum() <= 0) {
                weightForm.setError("Indique o peso.");
                throw new IllegalArgumentException("Indique o peso.");
            }
            SecondWeighingRequest request = new SecondWeighingRequest(companyId, value, weight.fromScale(), reference.getText().trim());
            return () -> { result[0] = api.second(t.id(), request); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, "Talão " + result[0].number() + ": líquido " + kg(result[0].netWeight()) + " kg.");
        reload();
        print(result[0]);
    }

    private void print(WeighingTicketDTO t) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (t == null || companyId == null) return;
        UIHelper.runWithProgress(this, "A gerar o talão de pesagem…", () -> api.pdf(companyId, t.id()),
                pdf -> PrintPreviewDialog.show(this, pdf, "pesagem-" + t.number().replace('/', '-')),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível gerar o talão", error.getMessage(), null, null));
    }

    private WeighingTicketDTO selected() {
        int row = TableFilter.selectedModelRow(table);
        return row < 0 || row >= tickets.size() ? null : tickets.get(row);
    }

    private static String kg(BigDecimal value) {
        return value == null ? "—" : TableCellRenderers.format(value, 0, "");
    }
}
