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

    private final ActivityApiClient activityApiClient;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final JPanel cardsGrid = new JPanel(new GridLayout(0, COLUMNS, 12, 12));
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
        JPanel gridHolder = new JPanel(new BorderLayout());
        gridHolder.setOpaque(false);
        gridHolder.add(cardsGrid, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(gridHolder);
        UIHelper.styleScrollPane(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel center = new JPanel(new BorderLayout(14, 0));
        center.setOpaque(false);
        center.add(scroll, BorderLayout.CENTER);
        center.add(buildSummary(), BorderLayout.EAST);
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
        JLabel intro = new JLabel("<html>Escolha o que a empresa faz. Pode combinar vários sectores, por exemplo "
                + "<b>Estaleiro</b> e <b>Fábrica de blocos</b>. As capacidades de cada sector somam-se. "
                + "Desligar um sector simplifica os ecrãs, mas <b>nunca apaga dados</b>.</html>");
        intro.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        intro.setForeground(UIHelper.TEXT_MUTED);
        header.add(intro, BorderLayout.CENTER);
        header.add(feedback, BorderLayout.SOUTH);
        return header;
    }

    private JComponent buildSummary() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        card.setPreferredSize(new Dimension(320, 10));

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
        JScrollPane listScroll = new JScrollPane(capabilityList);
        UIHelper.styleScrollPane(listScroll);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(listScroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        statusLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        statusLabel.setForeground(UIHelper.TEXT_MUTED);
        bottom.add(statusLabel, BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(resetBtn);
        actions.add(saveBtn);
        bottom.add(actions, BorderLayout.SOUTH);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
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
            JLabel none = new JLabel("<html>Nenhum sector escolhido.<br>Escolha pelo menos um.</html>");
            none.setForeground(UIHelper.TEXT_MUTED);
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
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel icon = new JLabel(UIHelper.icon(SectorVisuals.capabilityIcon(code), 15, UIHelper.APPROVED_GREEN));
        icon.setVerticalAlignment(SwingConstants.TOP);
        row.add(icon, BorderLayout.WEST);
        String label = capability == null ? code : capability.label();
        String description = capability == null ? "" : capability.description();
        JLabel text = new JLabel("<html><b>" + escape(label) + "</b><br><span style='font-size:10px'>"
                + escape(description) + "</span></html>");
        text.setForeground(UIHelper.TEXT_LIGHT);
        text.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        row.add(text, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 4));
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
            JLabel description = new JLabel("<html>" + escape(profile.description()) + "</html>");
            description.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            description.setForeground(UIHelper.TEXT_MUTED);
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
