package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ActivityApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.activity.dto.ActivityCatalogDTO;
import mz.multicore.erp.modules.activity.dto.ActivityOptionDTO;
import mz.multicore.erp.modules.activity.dto.CompanyActivityDTO;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

/**
 * Company activity profile (SA-C01, {@code docs/SECTORES_ACTIVIDADE_SPEC.md}). The manager chooses one or
 * more sectors (butcher, bakery, building materials, ...) and sees the resulting capabilities before saving.
 * Removing a profile never deletes data.
 */
public class ActivityProfilePanel extends JPanel {

    private static final int COLUMNS = 3;
    /** Narrowest card that still reads well; the grid fits as many columns as the width allows. */
    private static final int MIN_CARD_WIDTH = 290;
    /** Below this width the capabilities summary moves under the cards instead of beside them. */
    private static final int SIDE_SUMMARY_MIN_WIDTH = 900;

    private final ActivityApiClient activityApiClient;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final GridLayout gridLayout = new GridLayout(0, COLUMNS, 12, 12);
    private final JPanel cardsGrid = new JPanel(gridLayout);
    private final JPanel capabilityList = new JPanel();
    private final JLabel summaryCount = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final ModernButton saveBtn = UIHelper.createSuccessButton("Guardar perfil");
    private final ModernButton resetBtn = UIHelper.createSecondaryButton("Repor");

    private final Map<String, ActivityOptionDTO> profiles = new LinkedHashMap<>();
    private final Map<String, ActivityOptionDTO> capabilities = new LinkedHashMap<>();
    private final Map<String, ProfileCard> cards = new LinkedHashMap<>();
    private final Set<String> selected = new LinkedHashSet<>();
    private Set<String> saved = Set.of();
    private java.util.function.Consumer<List<String>> onSaved = capabilities -> { };

    public ActivityProfilePanel(ActivityApiClient activityApiClient) {
        this.activityApiClient = activityApiClient;
        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        add(buildHeader(), BorderLayout.NORTH);

        cardsGrid.setOpaque(false);
        JPanel gridHolder = ResponsiveSupport.widthTracking(new BorderLayout());
        gridHolder.add(cardsGrid, BorderLayout.NORTH);
        ResponsiveSupport.adaptiveColumns(cardsGrid, gridLayout, MIN_CARD_WIDTH);
        JScrollPane scroll = ResponsiveSupport.verticalScroll(gridHolder);

        JPanel center = new JPanel(new BorderLayout(14, 14));
        center.setOpaque(false);
        center.add(scroll, BorderLayout.CENTER);
        JComponent summary = buildSummary();
        center.add(summary, BorderLayout.EAST);
        center.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                placeSummary(center, summary);
            }
        });
        add(center, BorderLayout.CENTER);

        saveBtn.setIcon(UIHelper.icon("fas-save", 14));
        saveBtn.setToolTipText("Guardar os perfis escolhidos para a empresa activa");
        saveBtn.addActionListener(e -> save());
        resetBtn.setIcon(UIHelper.icon("fas-undo-alt", 14));
        resetBtn.setToolTipText("Voltar aos perfis guardados");
        resetBtn.addActionListener(e -> {
            selected.clear();
            selected.addAll(saved);
            refreshSelection();
        });
        refreshSelection();
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(UIHelper.createHeading("Perfil de Actividade da Empresa"), BorderLayout.NORTH);
        JTextArea intro = ResponsiveSupport.wrappingText("Escolha o que a empresa faz. Pode combinar vários sectores, por "
                + "exemplo Estaleiro e Fábrica de blocos. As capacidades de cada sector somam-se. Desligar um sector "
                + "simplifica os ecrãs, mas nunca apaga dados.", new Font(UIHelper.FONT, Font.PLAIN, 13), UIHelper.TEXT_MUTED);
        header.add(intro, BorderLayout.CENTER);
        header.add(feedback, BorderLayout.SOUTH);
        return header;
    }

    private JComponent buildSummary() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        card.setPreferredSize(new Dimension(300, 10));

        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        JLabel title = new JLabel("Capacidades activas");
        title.setIcon(UIHelper.icon("fas-check-double", 16, UIHelper.APPROVED_GREEN));
        title.setIconTextGap(8);
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        title.setForeground(UIHelper.TEXT_LIGHT);
        summaryCount.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        summaryCount.setForeground(UIHelper.TEXT_MUTED);
        top.add(title, BorderLayout.NORTH);
        top.add(summaryCount, BorderLayout.SOUTH);
        card.add(top, BorderLayout.NORTH);

        capabilityList.setOpaque(false);
        capabilityList.setLayout(new BoxLayout(capabilityList, BoxLayout.Y_AXIS));
        JPanel listHolder = ResponsiveSupport.widthTracking(new BorderLayout());
        listHolder.add(capabilityList, BorderLayout.NORTH);
        card.add(ResponsiveSupport.verticalScroll(listHolder), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        statusLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        statusLabel.setForeground(UIHelper.TEXT_MUTED);
        bottom.add(statusLabel, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setOpaque(false);
        actions.add(resetBtn);
        actions.add(saveBtn);
        bottom.add(actions, BorderLayout.SOUTH);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    /** Beside the cards on wide windows; under them, with a fixed height, on narrow ones. */
    private static void placeSummary(JPanel center, JComponent summary) {
        boolean side = center.getWidth() >= SIDE_SUMMARY_MIN_WIDTH;
        Object where = side ? BorderLayout.EAST : BorderLayout.SOUTH;
        if (where.equals(((BorderLayout) center.getLayout()).getConstraints(summary))) return;
        center.remove(summary);
        summary.setPreferredSize(side ? new Dimension(300, 10) : new Dimension(10, 230));
        center.add(summary, where);
        center.revalidate();
        center.repaint();
    }

    /** Called with the active capabilities after every save (the menu uses it to show or hide sector modules). */
    public void onSaved(java.util.function.Consumer<List<String>> listener) {
        this.onSaved = listener == null ? capabilities -> { } : listener;
    }

    /** Loads the catalogue and the active company profiles outside the EDT. */
    public void refresh() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || activityApiClient == null) return;
        UIHelper.loadAsync(this, () -> new Loaded(activityApiClient.catalog(), activityApiClient.company(companyId)),
                this::apply,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar o perfil de actividade",
                        error.getMessage(), "Tentar novamente", this::refresh));
    }

    private void apply(Loaded loaded) {
        profiles.clear();
        capabilities.clear();
        loaded.catalog().profiles().forEach(p -> profiles.put(p.code(), p));
        loaded.catalog().capabilities().forEach(c -> capabilities.put(c.code(), c));
        saved = Set.copyOf(loaded.company().profiles());
        selected.clear();
        selected.addAll(loaded.company().profiles());

        cardsGrid.removeAll();
        cards.clear();
        for (ActivityOptionDTO profile : profiles.values()) {
            ProfileCard card = new ProfileCard(profile);
            cards.put(profile.code(), card);
            cardsGrid.add(card);
        }
        if (!loaded.company().configured()) {
            feedback.show(FeedbackType.INFO, "Perfil ainda não escolhido",
                    "A empresa está como Comércio geral. Escolha os sectores reais e guarde.", null, null);
        }
        refreshSelection();
        cardsGrid.revalidate();
        cardsGrid.repaint();
    }

    private void toggle(String code) {
        if (!selected.remove(code)) selected.add(code);
        refreshSelection();
    }

    private void refreshSelection() {
        cards.forEach((code, card) -> card.setSelected(selected.contains(code)));

        Set<String> active = new TreeSet<>(Comparator.comparing(c -> capabilities.containsKey(c)
                ? capabilities.get(c).label() : c));
        for (String code : selected) {
            ActivityOptionDTO profile = profiles.get(code);
            if (profile != null) active.addAll(profile.capabilities());
        }
        capabilityList.removeAll();
        for (String capability : active) {
            capabilityList.add(capabilityRow(capability));
            capabilityList.add(Box.createVerticalStrut(8));
        }
        if (active.isEmpty()) {
            JTextArea none = ResponsiveSupport.wrappingText("Nenhum sector escolhido. Escolha pelo menos um.",
                    new Font(UIHelper.FONT, Font.PLAIN, 12), UIHelper.TEXT_MUTED);
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            capabilityList.add(none);
        }
        summaryCount.setText(selected.size() + (selected.size() == 1 ? " sector · " : " sectores · ")
                + active.size() + (active.size() == 1 ? " capacidade" : " capacidades"));
        boolean changed = !selected.equals(saved);
        saveBtn.setEnabled(changed && !selected.isEmpty());
        resetBtn.setEnabled(changed);
        statusLabel.setText(selected.isEmpty() ? "Escolha pelo menos um sector."
                : changed ? "Há alterações por guardar." : "Guardado.");
        capabilityList.revalidate();
        capabilityList.repaint();
    }

    private JComponent capabilityRow(String code) {
        ActivityOptionDTO capability = capabilities.get(code);
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel icon = new JLabel(UIHelper.icon(SectorVisuals.capabilityIcon(code), 15, UIHelper.APPROVED_GREEN));
        icon.setVerticalAlignment(SwingConstants.TOP);
        row.add(icon, BorderLayout.WEST);
        String label = capability == null ? code : capability.label();
        String description = capability == null ? "" : capability.description();
        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        JLabel title = new JLabel(label);
        title.setForeground(UIHelper.TEXT_LIGHT);
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        text.add(title, BorderLayout.NORTH);
        text.add(ResponsiveSupport.wrappingText(description, new Font(UIHelper.FONT, Font.PLAIN, 11), UIHelper.TEXT_MUTED),
                BorderLayout.CENTER);
        row.add(text, BorderLayout.CENTER);
        return row;
    }

    private void save() {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || selected.isEmpty()) return;
        List<String> chosen = List.copyOf(selected);
        UIHelper.submitAsync(saveBtn, () -> activityApiClient.update(companyId, chosen), result -> {
            saved = Set.copyOf(result.profiles());
            onSaved.accept(result.capabilities());
            selected.clear();
            selected.addAll(result.profiles());
            refreshSelection();
            ToastManager.success(this, "Perfil de actividade guardado.");
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível guardar o perfil",
                error.getMessage(), null, null));
    }

    private static String escape(String text) {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private record Loaded(ActivityCatalogDTO catalog, CompanyActivityDTO company) {}

    /** Sector card that toggles on click or with the keyboard (Space or Enter). */
    private final class ProfileCard extends ModernPanel {

        private final JLabel check = new JLabel();
        private final Color accent;

        ProfileCard(ActivityOptionDTO profile) {
            super(14);
            SectorVisuals.Visual visual = SectorVisuals.profile(profile.code());
            this.accent = visual.color();
            setLayout(new BorderLayout(12, 8));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFocusable(true);
            getAccessibleContext().setAccessibleName("Sector " + profile.label());
            setToolTipText("Clique ou carregue em Espaço para escolher ou retirar este sector");

            JLabel icon = new JLabel(UIHelper.icon(visual.iconCode(), 26, visual.color()));
            icon.setVerticalAlignment(SwingConstants.TOP);
            add(icon, BorderLayout.WEST);

            JPanel body = new JPanel(new BorderLayout(0, 6));
            body.setOpaque(false);
            JLabel title = new JLabel(profile.label());
            title.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
            title.setForeground(UIHelper.TEXT_LIGHT);
            body.add(title, BorderLayout.NORTH);
            JTextArea description = ResponsiveSupport.wrappingText(profile.description(),
                    new Font(UIHelper.FONT, Font.PLAIN, 12), UIHelper.TEXT_MUTED);
            body.add(description, BorderLayout.CENTER);

            JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
            chips.setOpaque(false);
            for (String capability : profile.capabilities()) {
                ActivityOptionDTO option = capabilities.get(capability);
                JLabel chip = new JLabel(UIHelper.icon(SectorVisuals.capabilityIcon(capability), 12, visual.color()));
                chip.setToolTipText(option == null ? capability : option.label());
                chips.add(chip);
            }
            body.add(chips, BorderLayout.SOUTH);
            add(body, BorderLayout.CENTER);

            check.setVerticalAlignment(SwingConstants.TOP);
            add(check, BorderLayout.EAST);

            MouseAdapter click = new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    requestFocusInWindow();
                    toggle(profile.code());
                }
            };
            addMouseListener(click);
            for (Component child : new Component[]{icon, body, title, description, chips, check}) {
                child.addMouseListener(click);
            }
            InputMap keys = getInputMap(JComponent.WHEN_FOCUSED);
            keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggle");
            keys.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "toggle");
            getActionMap().put("toggle", new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                    toggle(profile.code());
                }
            });
            setSelected(false);
        }

        void setSelected(boolean on) {
            check.setIcon(on ? UIHelper.icon("fas-check-circle", 20, UIHelper.APPROVED_GREEN)
                    : UIHelper.icon("far-circle", 20, UIHelper.BORDER));
            setBorder(new CompoundBorder(UIHelper.fieldBorder(on ? accent : UIHelper.BORDER, on ? 2 : 1),
                    new EmptyBorder(on ? 13 : 14, on ? 13 : 14, on ? 13 : 14, on ? 13 : 14)));
            getAccessibleContext().setAccessibleDescription(on ? "Escolhido" : "Não escolhido");
            repaint();
        }
    }
}
