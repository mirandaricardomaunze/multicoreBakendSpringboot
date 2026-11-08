package mz.multicore.erp.gui.platform;

import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.StepIndicator;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.platform.dto.OnboardCompanyRequest;
import mz.multicore.erp.modules.platform.dto.PlatformCompanyDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Step-by-step company form for the platform console.
 *
 * <p>Registration has four steps (identity, address and contacts, legal and tax data with the
 * optional bank account, administrator and plan) and is sent as one onboarding request, so the
 * server creates everything or nothing. Moving forward checks the current step; finished steps
 * can be reopened by clicking them.
 *
 * <p>Editing shows the first three steps, any of which can be opened directly, and "Guardar" is
 * available on every step.
 */
final class CompanyWizardDialog {

    /** What the dialog did. {@code logoFailed}: the company was saved but the logo upload was not. */
    record Outcome(PlatformCompanyDTO company, boolean logoFailed) {}

    private record Step(String label, JComponent panel, Supplier<String> validator) {}

    /**
     * The dialog wraps its content in a scroll pane without horizontal scrolling. A plain panel
     * would be laid out at its preferred width and cut on the right; this one follows the
     * viewport width and only scrolls vertically.
     */
    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        WidthTrackingPanel(LayoutManager layout) {
            super(layout);
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(16, r.height - 32); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Card panel sized to the visible step, so a short step does not inherit the tallest one's height. */
    private static final class CurrentCardPanel extends JPanel {
        CurrentCardPanel(CardLayout layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredSize() {
            for (Component child : getComponents()) {
                if (child.isVisible()) {
                    Insets in = getInsets();
                    Dimension d = child.getPreferredSize();
                    return new Dimension(d.width + in.left + in.right, d.height + in.top + in.bottom);
                }
            }
            return super.getPreferredSize();
        }
    }

    private final PlatformApiClient api;
    private final PlatformCompanyDTO existing;
    private final CompanyFormFields fields;
    private final CompanyAdminStep admin;
    private final List<Step> steps = new ArrayList<>();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new CurrentCardPanel(cards);
    private final StepIndicator indicator;
    private final InlineFeedbackPanel stepFeedback = new InlineFeedbackPanel();
    private final JLabel summary = new JLabel();
    private final ModernFormDialog dialog;
    private final ModernButton backButton = UIHelper.createSecondaryButton("Anterior");
    private final ModernButton nextButton = UIHelper.createPrimaryButton("Seguinte");
    private int current;
    private int furthest;
    private Outcome outcome;

    private CompanyWizardDialog(Window owner, PlatformApiClient api, PlatformCompanyDTO existing) {
        this.api = api;
        this.existing = existing;
        this.fields = new CompanyFormFields(existing);
        this.admin = existing == null ? new CompanyAdminStep() : null;

        cardPanel.setOpaque(false);
        addStep("Identificação", fields.identityStep(owner), fields::validateIdentity);
        addStep("Morada e contactos", fields.addressStep(), () -> null);
        addStep(existing == null ? "Dados legais e banco" : "Dados legais e fiscais",
                fields.legalStep(), fields::validateLegal);
        if (admin != null) addStep("Administrador e plano", adminStepPanel(), admin::validateStep);

        indicator = new StepIndicator(steps.stream().map(Step::label).toList());
        indicator.setNavigable(index -> editing() || index <= furthest, this::goTo);

        JPanel top = CompanyFormFields.column();
        indicator.setAlignmentX(Component.LEFT_ALIGNMENT);
        stepFeedback.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(indicator);
        top.add(Box.createVerticalStrut(6));
        top.add(stepFeedback);

        JPanel content = new WidthTrackingPanel(new BorderLayout(0, 8));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(0, 0, 0, 4));
        content.add(top, BorderLayout.NORTH);
        content.add(cardPanel, BorderLayout.CENTER);

        dialog = existing == null
                ? new ModernFormDialog(owner, "Registar Empresa", "fas-building",
                        "Registo completo: empresa, administrador e assinatura", content)
                        .setConfirmButton("Registar empresa", "fas-check")
                : new ModernFormDialog(owner, "Editar Empresa", "fas-pen",
                        "NUIT " + existing.taxId() + " · o NUIT não pode ser alterado", content)
                        .setConfirmButton("Guardar", "fas-check");

        backButton.setIcon(UIHelper.icon("fas-arrow-left", 14));
        backButton.addActionListener(e -> goTo(current - 1));
        nextButton.setIcon(UIHelper.icon("fas-arrow-right", 14));
        nextButton.setHorizontalTextPosition(SwingConstants.LEFT);
        nextButton.addActionListener(e -> goTo(current + 1));
        // Order: Cancelar · Anterior · Seguinte / Registar — the forward action stays rightmost.
        dialog.addActionButtonBeforeConfirm(backButton);
        dialog.addActionButtonBeforeConfirm(nextButton);
        dialog.setOnSaveAsync(this::prepareSave);
        dialog.setSize(860, 660);
        goTo(0);
    }

    static Outcome showCreate(Window owner, PlatformApiClient api) {
        CompanyWizardDialog wizard = new CompanyWizardDialog(owner, api, null);
        return wizard.dialog.showDialog() ? wizard.outcome : null;
    }

    static Outcome showEdit(Window owner, PlatformApiClient api, PlatformCompanyDTO company) {
        CompanyWizardDialog wizard = new CompanyWizardDialog(owner, api, company);
        return wizard.dialog.showDialog() ? wizard.outcome : null;
    }

    private boolean editing() {
        return existing != null;
    }

    private void addStep(String label, JComponent panel, Supplier<String> validator) {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(panel, BorderLayout.NORTH);
        cardPanel.add(holder, String.valueOf(steps.size()));
        steps.add(new Step(label, holder, validator));
    }

    private JPanel adminStepPanel() {
        summary.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        summary.setForeground(UIHelper.TEXT_LIGHT);
        summary.setIcon(UIHelper.icon("fas-building", 14, UIHelper.ACCENT_BLUE));
        summary.setIconTextGap(8);
        summary.setBorder(new EmptyBorder(2, 14, 10, 14));
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel panel = CompanyFormFields.column();
        panel.add(summary);
        JPanel adminPanel = admin.panel();
        adminPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(adminPanel);
        return panel;
    }

    /** Opens step {@code target}. Going forward in registration first checks every step on the way. */
    private void goTo(int target) {
        if (target < 0 || target >= steps.size()) return;
        if (!editing() && target > current) {
            for (int i = current; i < target; i++) {
                String problem = steps.get(i).validator().get();
                if (problem != null) {
                    show(i);
                    stepFeedback.show(FeedbackType.WARNING, "Falta completar este passo", problem, null, null);
                    return;
                }
            }
        }
        stepFeedback.clear();
        show(target);
    }

    private void show(int index) {
        current = index;
        furthest = Math.max(furthest, index);
        cards.show(cardPanel, String.valueOf(index));
        cardPanel.revalidate();
        cardPanel.repaint();
        indicator.setCurrent(index);
        boolean last = index == steps.size() - 1;
        backButton.setVisible(index > 0);
        nextButton.setVisible(!last);
        dialog.setConfirmVisible(editing() || last);
        if (admin != null && last) {
            admin.suggestUsername(fields.displayName());
            summary.setText(summaryText());
        }
        SwingUtilities.invokeLater(() -> ModernFormDialog.focusFirstComponent(steps.get(index).panel()));
    }

    private String summaryText() {
        String name = fields.displayName() == null ? "" : fields.displayName();
        String nuit = CompanyFormFields.text(fields.taxId);
        String city = CompanyFormFields.text(fields.city);
        StringBuilder text = new StringBuilder("<html>A registar <b>").append(escape(name)).append("</b>");
        if (nuit != null) text.append(" · NUIT ").append(escape(nuit));
        if (city != null) text.append(" · ").append(escape(city));
        if (fields.bankAccountOrNull() != null) text.append(" · com conta bancária");
        return text.append("</html>").toString();
    }

    /** Runs on the EDT: checks every step and captures the request; the returned task does the I/O. */
    private java.util.concurrent.Callable<?> prepareSave() {
        for (int i = 0; i < steps.size(); i++) {
            String problem = steps.get(i).validator().get();
            if (problem != null) {
                show(i);
                throw new IllegalArgumentException(problem);
            }
        }
        stepFeedback.clear();
        byte[] logo = fields.chosenLogo();
        if (editing()) {
            Long id = existing.id();
            var request = fields.toUpdateRequest();
            return () -> finish(api.updateCompany(id, request), logo);
        }
        OnboardCompanyRequest request = new OnboardCompanyRequest(
                fields.toCreateRequest(), admin.administrator(), admin.subscription(), fields.bankAccountOrNull());
        return () -> finish(api.onboardCompany(request), logo);
    }

    /**
     * The logo goes in a second call. If it fails the company is already saved, so the dialog
     * must still close: retrying would register the same NUIT twice.
     */
    private Outcome finish(PlatformCompanyDTO saved, byte[] logo) {
        boolean logoFailed = false;
        if (logo != null) {
            try {
                api.updateCompanyLogo(saved.id(), logo);
            } catch (RuntimeException ex) {
                logoFailed = true;
            }
        }
        outcome = new Outcome(saved, logoFailed);
        return outcome;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
