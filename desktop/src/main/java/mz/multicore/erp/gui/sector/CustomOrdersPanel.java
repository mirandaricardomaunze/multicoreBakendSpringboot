package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.CustomOrdersApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.customorders.dto.CustomOrderDTO;
import mz.multicore.erp.modules.customorders.dto.CustomOrderLineDTO;
import mz.multicore.erp.modules.customorders.dto.SaveCustomOrderRequest;
import mz.multicore.erp.modules.customorders.model.CustomOrderStage;
import mz.multicore.erp.modules.customorders.model.DeliveryMode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Agenda de Encomendas (SA-C13, {@code docs/ENCOMENDAS_PERSONALIZADAS_SPEC.md}): every order with a delivery date,
 * in delivery order, with the hour, the stage in the kitchen and the advance already paid. On the right, what to
 * write on the cake and the reference photo. Orders are still created in Pedidos; here they get date, hour,
 * personalisation and photo, go to the production plan of the day and print as a kitchen sheet.
 */
public class CustomOrdersPanel extends JPanel {

    private static final String[] COLUMNS = {"Entrega", "Hora", "Encomenda", "Cliente", "Artigos", "Modo", "Estado", "Sinal", "Total", "Produção"};
    private static final String[][] PERIODS = {{"Hoje", "0"}, {"Amanhã", "1"}, {"Próximos 7 dias", "7"}, {"Próximos 14 dias", "14"},
            {"Próximos 30 dias", "30"}};
    static final String[][] STAGES = {{"TO_PRODUCE", "POR PRODUZIR"}, {"IN_PRODUCTION", "EM PRODUÇÃO"}, {"READY", "PRONTA"},
            {"DELIVERED", "ENTREGUE"}};

    private final CustomOrdersApiClient api;
    private final ComercialApiClient comercialApi;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<String> period = new JComboBox<>(java.util.Arrays.stream(PERIODS).map(p -> p[0]).toArray(String[]::new));
    private final ModernButton editBtn = UIHelper.createPrimaryButton("Entrega e Personalização");
    private final ModernButton planBtn = UIHelper.createSecondaryButton("Enviar para Produção");
    private final ModernButton stageBtn = UIHelper.createSecondaryButton("Mudar Estado");
    private final ModernButton printBtn = UIHelper.createSecondaryButton("Ficha da Cozinha");
    private final JLabel detailTitle = new JLabel(" ");
    private final JLabel detailBody = new JLabel(" ");
    private final JLabel photo = new JLabel("", SwingConstants.CENTER);
    private final CardLayout cards = new CardLayout();
    private final JPanel detail = new JPanel(cards);
    private final List<CustomOrderDTO> orders = new ArrayList<>();
    private Consumer<List<CustomOrderDTO>> onLoaded = list -> { };

    public CustomOrdersPanel(CustomOrdersApiClient api, ComercialApiClient comercialApi) {
        this.api = api;
        this.comercialApi = comercialApi;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        add(feedback, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildList(), buildDetail());
        split.setOpaque(false);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setResizeWeight(0.66);
        split.setDividerSize(10);
        ResponsiveSupport.adaptiveSplit(split, 980);
        add(split, BorderLayout.CENTER);
        updateActions();
    }

    public void onLoaded(Consumer<List<CustomOrderDTO>> listener) {
        this.onLoaded = listener == null ? list -> { } : listener;
    }

    private JComponent buildList() {
        UIHelper.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(6).setCellRenderer(TableCellRenderers.status());
        table.getColumnModel().getColumn(7).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(8).setCellRenderer(TableCellRenderers.money());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail();
        });
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        UIHelper.styleComboBox(period);
        period.setSelectedIndex(2);
        period.setPreferredSize(new Dimension(160, UIHelper.FORM_CONTROL_HEIGHT));
        period.addActionListener(e -> reload());
        JTextField search = TableFilter.searchField("Cliente, encomenda ou artigo…");
        search.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> stage = TableFilter.combo("Todos os estados", STAGES[0][1], STAGES[1][1], STAGES[2][1], STAGES[3][1]);
        UIHelper.styleComboBox(stage);
        stage.setPreferredSize(new Dimension(150, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(table, search, new TableFilter.ColumnFilter(stage, 6));
        JPanel filters = TableFilter.bar(TableFilter.label("Período:"), period, search, TableFilter.label("Estado:"), stage);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));

        ModernButton scheduleBtn = UIHelper.createSuccessButton("Agendar Encomenda");
        scheduleBtn.setIcon(UIHelper.icon("fas-calendar-plus", 14));
        scheduleBtn.setToolTipText("Dá data, hora e personalização a uma encomenda criada em Pedidos");
        scheduleBtn.addActionListener(e -> schedule());
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        editBtn.addActionListener(e -> {
            CustomOrderDTO selected = selected();
            if (selected != null) openEditor(selected.orderId(), selected.orderNumber(), selected);
        });
        planBtn.setIcon(UIHelper.icon("fas-bread-slice", 14));
        planBtn.setToolTipText("Soma os artigos ao plano de produção do dia da entrega");
        planBtn.addActionListener(e -> sendToPlan());
        stageBtn.setIcon(UIHelper.icon("fas-tasks", 14));
        stageBtn.addActionListener(e -> changeStage());
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> printSheet());
        ModernButton refreshBtn = UIHelper.createRefreshButton(this::reload);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(ResponsiveSupport.cardTop("Agenda de Encomendas", filters, refreshBtn, printBtn, stageBtn, planBtn, editBtn, scheduleBtn),
                BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildDetail() {
        ModernPanel empty = new ModernPanel(16);
        empty.setLayout(new GridBagLayout());
        JLabel hint = new JLabel("<html><div style='text-align:center;width:280px'><b>Personalização</b><br>Escolha uma encomenda "
                + "para ver o que escrever, a fotografia de referência e o contacto do cliente.</div></html>",
                UIHelper.icon("fas-birthday-cake", 36, UIHelper.ACCENT_PINK), SwingConstants.CENTER);
        hint.setHorizontalTextPosition(SwingConstants.CENTER);
        hint.setVerticalTextPosition(SwingConstants.BOTTOM);
        hint.setForeground(UIHelper.TEXT_MUTED);
        empty.add(hint);
        detailTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        detailTitle.setForeground(UIHelper.TEXT_LIGHT);
        detailBody.setForeground(UIHelper.TEXT_LIGHT);
        detailBody.setVerticalAlignment(SwingConstants.TOP);
        photo.setPreferredSize(new Dimension(260, 220));
        photo.setForeground(UIHelper.TEXT_MUTED);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(detailTitle, BorderLayout.NORTH);
        card.add(new JScrollPane(detailBody) {{ setOpaque(false); getViewport().setOpaque(false); setBorder(null); }}, BorderLayout.CENTER);
        card.add(photo, BorderLayout.SOUTH);
        detail.setOpaque(false);
        detail.add(empty, "empty");
        detail.add(card, "detail");
        cards.show(detail, "empty");
        return detail;
    }

    public void reload() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || api == null) return;
        int days = Integer.parseInt(PERIODS[Math.max(0, period.getSelectedIndex())][1]);
        LocalDate from = days == 1 ? LocalDate.now().plusDays(1) : LocalDate.now();
        LocalDate to = days <= 1 ? from : LocalDate.now().plusDays(days);
        Long keep = selected() == null ? null : selected().orderId();
        UIHelper.loadAsync(this, () -> api.agenda(companyId, from, to), loaded -> apply(loaded, keep),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar a agenda", error.getMessage(), "Tentar novamente", this::reload));
    }

    private void apply(List<CustomOrderDTO> loaded, Long keep) {
        feedback.clear();
        orders.clear();
        orders.addAll(loaded);
        model.setRowCount(0);
        for (CustomOrderDTO o : orders) {
            model.addRow(new Object[]{o.deliveryDate() == null ? "—" : UIHelper.formatDate(o.deliveryDate()),
                    o.deliveryTime() == null ? "—" : o.deliveryTime().toString(), o.orderNumber(), o.clientName(), items(o),
                    o.mode() == DeliveryMode.DELIVERY ? "Entrega" : "Levanta", stageLabel(o.stage()), o.advancePaid(), o.total(),
                    o.sentToPlan() ? "No plano" : "—"});
        }
        if (keep != null) {
            for (int i = 0; i < orders.size(); i++) {
                if (!orders.get(i).orderId().equals(keep)) continue;
                int view = table.convertRowIndexToView(i);
                if (view >= 0) table.setRowSelectionInterval(view, view);
            }
        }
        showDetail();
        onLoaded.accept(List.copyOf(orders));
    }

    private void showDetail() {
        CustomOrderDTO o = selected();
        updateActions();
        if (o == null) {
            cards.show(detail, "empty");
            return;
        }
        detailTitle.setText(o.orderNumber() + "  ·  " + o.clientName());
        BigDecimal due = o.total() == null ? BigDecimal.ZERO : o.total().subtract(o.advancePaid());
        detailBody.setText("<html><div style='width:280px'><b>Entrega:</b> " + (o.deliveryDate() == null ? "—" : UIHelper.formatDate(o.deliveryDate()))
                + (o.deliveryTime() == null ? "" : " às " + o.deliveryTime()) + "<br><b>Modo:</b> "
                + (o.mode() == DeliveryMode.DELIVERY ? "entrega em " + escape(o.deliveryAddress()) : "levanta na loja")
                + "<br><b>Contacto:</b> " + escape(o.contactPhone()) + "<br><b>Sinal pago:</b> " + UIHelper.formatMzn(o.advancePaid())
                + " · <b>falta pagar:</b> " + UIHelper.formatMzn(due.max(BigDecimal.ZERO)) + "<br><br><b>Artigos:</b><br>"
                + o.lines().stream().map(l -> TableCellRenderers.format(l.quantity(), 3, "") + " × " + escape(l.productName()))
                .collect(Collectors.joining("<br>")) + "<br><br><b>Personalização:</b><br>"
                + (o.personalization() == null ? "<i>Sem personalização</i>" : escape(o.personalization()).replace("\n", "<br>"))
                + "</div></html>");
        photo.setIcon(null);
        photo.setText(o.hasImage() ? "A carregar a fotografia…" : "Sem fotografia de referência");
        cards.show(detail, "detail");
        if (o.hasImage()) {
            Long companyId = CurrentUserContext.findCurrentCompanyId();
            UIHelper.loadAsync(this, () -> api.image(companyId, o.orderId()), bytes -> {
                if (selected() == null || !selected().orderId().equals(o.orderId())) return;
                ImageIcon icon = scaled(bytes, 260, 220);
                photo.setText(icon == null ? "Fotografia não legível" : "");
                photo.setIcon(icon);
            }, error -> photo.setText("Fotografia indisponível"));
        }
    }

    private void updateActions() {
        CustomOrderDTO o = selected();
        editBtn.setEnabled(o != null);
        planBtn.setEnabled(o != null && !o.sentToPlan() && o.stage() != CustomOrderStage.DELIVERED);
        stageBtn.setEnabled(o != null);
        printBtn.setEnabled(o != null);
    }

    private void schedule() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        UIHelper.loadAsync(this, () -> comercialApi.getPendingOrdersByCompany(companyId), pending -> {
            if (pending.isEmpty()) {
                feedback.show(FeedbackType.INFO, "Sem encomendas abertas", "Crie primeiro a encomenda em Pedidos e volte aqui para a agendar.", null, null);
                return;
            }
            SearchableComboBox<OrderDTO> order = new SearchableComboBox<>("Pesquisar encomenda por número ou cliente",
                    o -> o.orderNumber() + "  ·  " + o.clientName(), o -> o.orderNumber() + " " + o.clientName());
            order.setItems(pending);
            ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Agendar Encomenda", "fas-calendar-plus",
                    "Escolha a encomenda criada em Pedidos", UIHelper.createDialogForm(1, new Object[]{"Encomenda:", order}))
                    .setConfirmButton("Continuar", "fas-arrow-right");
            OrderDTO[] chosen = new OrderDTO[1];
            dialog.setOnSaveAsync(() -> {
                chosen[0] = order.selectedValue();
                if (chosen[0] == null) throw new IllegalArgumentException("Escolha a encomenda.");
                return () -> null;
            });
            if (dialog.showDialog() && chosen[0] != null) openEditor(chosen[0].id(), chosen[0].orderNumber(), null);
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar as encomendas", error.getMessage(), null, null));
    }

    private void openEditor(Long orderId, String orderNumber, CustomOrderDTO existing) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        DateField date = new DateField(existing != null && existing.deliveryDate() != null ? existing.deliveryDate() : LocalDate.now().plusDays(1));
        JTextField time = new JTextField(existing == null || existing.deliveryTime() == null ? "" : existing.deliveryTime().toString());
        UIHelper.styleTextField(time);
        JComboBox<String> mode = new JComboBox<>(new String[]{"Levanta na loja", "Entrega na morada"});
        UIHelper.styleComboBox(mode);
        if (existing != null && existing.mode() == DeliveryMode.DELIVERY) mode.setSelectedIndex(1);
        JTextField address = new JTextField(existing == null || existing.deliveryAddress() == null ? "" : existing.deliveryAddress());
        UIHelper.styleTextField(address);
        JTextField phone = new JTextField(existing == null || existing.contactPhone() == null ? "" : existing.contactPhone());
        UIHelper.styleTextField(phone);
        JTextArea personalization = new JTextArea(existing == null || existing.personalization() == null ? "" : existing.personalization(), 4, 30);
        personalization.setLineWrap(true);
        personalization.setWrapStyleWord(true);
        JScrollPane personalizationScroll = new JScrollPane(personalization);
        UIHelper.styleScrollPane(personalizationScroll);
        String[] image = new String[1];
        boolean[] remove = new boolean[1];
        JLabel photoState = new JLabel(existing != null && existing.hasImage() ? "Tem fotografia (escolha outra para substituir)" : "Sem fotografia");
        photoState.setForeground(UIHelper.TEXT_MUTED);
        ModernButton choose = UIHelper.createSecondaryButton("Escolher Fotografia");
        choose.setIcon(UIHelper.icon("fas-image", 13));
        choose.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagens JPEG ou PNG", "jpg", "jpeg", "png"));
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            try {
                byte[] bytes = Files.readAllBytes(chooser.getSelectedFile().toPath());
                if (bytes.length > 2_000_000) {
                    photoState.setText("A fotografia tem mais de 2 MB: escolha uma mais pequena.");
                    return;
                }
                image[0] = Base64.getEncoder().encodeToString(bytes);
                remove[0] = false;
                photoState.setText(chooser.getSelectedFile().getName());
            } catch (java.io.IOException error) {
                photoState.setText("Não foi possível ler a fotografia.");
            }
        });
        ModernButton clear = UIHelper.createSecondaryButton("Retirar");
        clear.addActionListener(e -> {
            image[0] = null;
            remove[0] = true;
            photoState.setText("A fotografia será retirada");
        });
        JPanel photoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        photoRow.setOpaque(false);
        photoRow.add(choose);
        photoRow.add(clear);
        photoRow.add(photoState);
        FormField timeForm = new FormField("Hora (HH:MM)", time, false, "Vazio = sem hora marcada");
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"Dia de entrega:", date, "", timeForm, "Modo:", mode,
                "Morada de entrega:", address, "Contacto do cliente:", phone,
                "Personalização (texto no bolo, corte, embalagem):", personalizationScroll, "Fotografia de referência:", photoRow});
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Encomenda " + orderNumber, "fas-birthday-cake",
                "Entrega e personalização", form).setConfirmButton("Guardar", "fas-save");
        CustomOrderDTO[] result = new CustomOrderDTO[1];
        dialog.setOnSaveAsync(() -> {
            LocalTime hour = null;
            if (!time.getText().isBlank()) {
                try {
                    hour = LocalTime.parse(time.getText().trim().length() == 4 ? "0" + time.getText().trim() : time.getText().trim());
                } catch (DateTimeParseException e) {
                    timeForm.setError("Use o formato HH:MM, por exemplo 15:30.");
                    throw new IllegalArgumentException("Hora inválida.");
                }
            }
            if (date.getDate() == null) throw new IllegalArgumentException("Indique o dia de entrega.");
            SaveCustomOrderRequest request = new SaveCustomOrderRequest(companyId, date.getDate(), hour,
                    mode.getSelectedIndex() == 1 ? DeliveryMode.DELIVERY : DeliveryMode.PICKUP, address.getText().trim(),
                    phone.getText().trim(), personalization.getText().trim(), image[0], remove[0]);
            return () -> { result[0] = api.save(orderId, request); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, "Encomenda " + orderNumber + " agendada para " + UIHelper.formatDate(result[0].deliveryDate()) + ".");
        Long id = result[0].orderId();
        int days = Integer.parseInt(PERIODS[Math.max(0, period.getSelectedIndex())][1]);
        LocalDate from = days == 1 ? LocalDate.now().plusDays(1) : LocalDate.now();
        LocalDate to = days <= 1 ? from : LocalDate.now().plusDays(days);
        UIHelper.loadAsync(this, () -> api.agenda(companyId, from, to), loaded -> apply(loaded, id), error -> reload());
    }

    private void sendToPlan() {
        CustomOrderDTO o = selected();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (o == null || companyId == null) return;
        UIHelper.runWithProgress(this, "A enviar para o plano de produção…", () -> api.sendToPlan(companyId, o.orderId()), result -> {
            ToastManager.success(this, "No plano de " + UIHelper.formatDate(o.deliveryDate()) + ": " + String.join(", ", result.planned()) + ".");
            if (!result.withoutRecipe().isEmpty()) {
                feedback.show(FeedbackType.WARNING, "Artigos sem receita", "Não entraram no plano (sem receita activa): "
                        + String.join(", ", result.withoutRecipe()) + ".", null, null);
            }
            reload();
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível enviar para produção", error.getMessage(), null, null));
    }

    private void changeStage() {
        CustomOrderDTO o = selected();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (o == null || companyId == null) return;
        JComboBox<String> stage = new JComboBox<>(java.util.Arrays.stream(STAGES).map(s -> s[1]).toArray(String[]::new));
        UIHelper.styleComboBox(stage);
        for (int i = 0; i < STAGES.length; i++) if (STAGES[i][0].equals(o.stage().name())) stage.setSelectedIndex(i);
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Estado da Encomenda " + o.orderNumber(), "fas-tasks",
                "Onde está a encomenda na cozinha", UIHelper.createDialogForm(1, new Object[]{"Estado:", stage}));
        CustomOrderDTO[] result = new CustomOrderDTO[1];
        dialog.setOnSaveAsync(() -> {
            CustomOrderStage chosen = CustomOrderStage.valueOf(STAGES[stage.getSelectedIndex()][0]);
            return () -> { result[0] = api.stage(companyId, o.orderId(), chosen); return null; };
        });
        if (!dialog.showDialog() || result[0] == null) return;
        ToastManager.success(this, "Encomenda " + o.orderNumber() + ": " + stageLabel(result[0].stage()).toLowerCase() + ".");
        reload();
    }

    private void printSheet() {
        CustomOrderDTO o = selected();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (o == null || companyId == null) return;
        UIHelper.runWithProgress(this, "A gerar a ficha da encomenda…", () -> api.sheet(companyId, o.orderId()),
                pdf -> PrintPreviewDialog.show(this, pdf, "encomenda-" + o.orderNumber().replace('/', '-')),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível gerar a ficha", error.getMessage(), null, null));
    }

    private CustomOrderDTO selected() {
        int row = TableFilter.selectedModelRow(table);
        return row < 0 || row >= orders.size() ? null : orders.get(row);
    }

    static String stageLabel(CustomOrderStage stage) {
        for (String[] s : STAGES) if (s[0].equals(stage == null ? "TO_PRODUCE" : stage.name())) return s[1];
        return STAGES[0][1];
    }

    private static String items(CustomOrderDTO o) {
        List<CustomOrderLineDTO> lines = o.lines();
        if (lines.isEmpty()) return "—";
        String first = TableCellRenderers.format(lines.get(0).quantity(), 3, "") + " × " + lines.get(0).productName();
        return lines.size() == 1 ? first : first + " +" + (lines.size() - 1);
    }

    private static String escape(String text) {
        if (text == null || text.isBlank()) return "—";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static ImageIcon scaled(byte[] bytes, int width, int height) {
        try {
            BufferedImage img = javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) return null;
            double ratio = Math.min((double) width / img.getWidth(), (double) height / img.getHeight());
            int w = Math.max(1, (int) (img.getWidth() * Math.min(1, ratio)));
            int h = Math.max(1, (int) (img.getHeight() * Math.min(1, ratio)));
            return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
