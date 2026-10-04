package mz.multicore.erp.gui.components;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

/**
 * Seletor popup de calendário executivo e moderno para campos de data (`DateField`).
 * Totalmente compatível com FlatLaf (modo claro/escuro) e dias da semana em português (Seg a Dom).
 */
public class ModernCalendarPopup extends JPopupMenu {

    private static final String[] MONTH_NAMES = {
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    private static final String[] WEEKDAY_NAMES = {
            "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"
    };

    private final DateField targetField;
    private LocalDate viewingDate;
    private LocalDate initialSelectedDate;

    private final JComboBox<String> monthCombo;
    private final JSpinner yearSpinner;
    private final JPanel gridPanel;
    private final List<DayButton> dayButtons = new ArrayList<>(42);
    private boolean adjusting = false;

    public ModernCalendarPopup(DateField targetField) {
        this.targetField = targetField;
        this.viewingDate = resolveInitialDate();
        this.initialSelectedDate = parseCurrentFieldDate();

        setLightWeightPopupEnabled(true);
        putClientProperty(FlatClientProperties.POPUP_BORDER_CORNER_RADIUS, 8);
        Color bg = UIHelper.isLight() ? Color.WHITE : UIHelper.BG_CARD;
        setBackground(bg);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(bg);
        content.setOpaque(true);

        // 1. Cabeçalho de Navegação (Mês, Ano, Setas ‹ e ›)
        JPanel navPanel = new JPanel(new BorderLayout(6, 0));
        navPanel.setOpaque(false);

        JButton prevBtn = createNavButton("", "Mes anterior");
        prevBtn.setIcon(UIHelper.icon("fas-chevron-left", 11));
        prevBtn.addActionListener(e -> stepMonth(-1));

        JButton nextBtn = createNavButton("", "Mes seguinte");
        nextBtn.setIcon(UIHelper.icon("fas-chevron-right", 11));
        nextBtn.addActionListener(e -> stepMonth(1));

        monthCombo = new JComboBox<>(MONTH_NAMES);
        UIHelper.styleComboBox(monthCombo);
        monthCombo.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        monthCombo.setSelectedIndex(viewingDate.getMonthValue() - 1);
        monthCombo.addActionListener(e -> {
            if (!adjusting) onHeaderChanged();
        });

        yearSpinner = new JSpinner(new SpinnerNumberModel(viewingDate.getYear(), 1900, 2150, 1));
        yearSpinner.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(yearSpinner, "#");
        yearSpinner.setEditor(editor);
        yearSpinner.setPreferredSize(new Dimension(68, 28));
        yearSpinner.addChangeListener(e -> {
            if (!adjusting) onHeaderChanged();
        });

        JPanel selectorsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        selectorsPanel.setOpaque(false);
        selectorsPanel.add(monthCombo);
        selectorsPanel.add(yearSpinner);

        navPanel.add(prevBtn, BorderLayout.WEST);
        navPanel.add(selectorsPanel, BorderLayout.CENTER);
        navPanel.add(nextBtn, BorderLayout.EAST);
        content.add(navPanel);
        content.add(Box.createVerticalStrut(8));

        // 2. Linha dos Dias da Semana (Seg a Dom)
        JPanel weekdaysPanel = new JPanel(new GridLayout(1, 7, 3, 0));
        weekdaysPanel.setOpaque(false);
        for (String wd : WEEKDAY_NAMES) {
            JLabel lbl = new JLabel(wd, SwingConstants.CENTER);
            lbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
            lbl.setForeground(UIHelper.TEXT_MUTED);
            weekdaysPanel.add(lbl);
        }
        content.add(weekdaysPanel);
        content.add(Box.createVerticalStrut(4));

        // 3. Grelha de 42 Dias (6x7)
        gridPanel = new JPanel(new GridLayout(6, 7, 3, 3));
        gridPanel.setOpaque(false);
        for (int i = 0; i < 42; i++) {
            DayButton btn = new DayButton();
            dayButtons.add(btn);
            gridPanel.add(btn);
        }
        content.add(gridPanel);
        content.add(Box.createVerticalStrut(8));

        // 4. Rodapé de Ações Rápidas (Hoje, Limpar, Fechar)
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);

        JButton todayBtn = new JButton("Hoje");
        styleQuickActionButton(todayBtn);
        todayBtn.setToolTipText("Selecionar a data de hoje");
        todayBtn.addActionListener(e -> {
            targetField.setDate(LocalDate.now());
            setVisible(false);
        });

        JButton clearBtn = new JButton("Limpar");
        styleQuickActionButton(clearBtn);
        clearBtn.setToolTipText("Limpar a data deste campo");
        clearBtn.addActionListener(e -> {
            targetField.setText("");
            UIHelper.clearFieldInvalid(targetField);
            setVisible(false);
        });

        JButton closeBtn = new JButton("Fechar");
        styleQuickActionButton(closeBtn);
        closeBtn.addActionListener(e -> setVisible(false));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftActions.setOpaque(false);
        leftActions.add(todayBtn);
        leftActions.add(clearBtn);

        footerPanel.add(leftActions, BorderLayout.WEST);
        footerPanel.add(closeBtn, BorderLayout.EAST);
        content.add(footerPanel);

        add(content);
        rebuildDaysGrid();
    }

    private LocalDate resolveInitialDate() {
        LocalDate current = parseCurrentFieldDate();
        return current != null ? current : LocalDate.now();
    }

    private LocalDate parseCurrentFieldDate() {
        if (targetField == null) return null;
        String text = targetField.getText();
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(text.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private void stepMonth(int delta) {
        this.viewingDate = this.viewingDate.plusMonths(delta);
        syncHeader();
        rebuildDaysGrid();
    }

    private void onHeaderChanged() {
        int month = monthCombo.getSelectedIndex() + 1;
        int year = (Integer) yearSpinner.getValue();
        this.viewingDate = LocalDate.of(year, Month.of(month), 1);
        rebuildDaysGrid();
    }

    private void syncHeader() {
        adjusting = true;
        try {
            monthCombo.setSelectedIndex(viewingDate.getMonthValue() - 1);
            yearSpinner.setValue(viewingDate.getYear());
        } finally {
            adjusting = false;
        }
    }

    private void rebuildDaysGrid() {
        LocalDate selected = parseCurrentFieldDate();
        LocalDate today = LocalDate.now();

        LocalDate firstDayOfMonth = viewingDate.withDayOfMonth(1);
        // Em Moçambique e ISO: 1 = Segunda, 7 = Domingo
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue();
        LocalDate gridStart = firstDayOfMonth.minusDays(firstDayOfWeek - 1);

        for (int i = 0; i < 42; i++) {
            LocalDate cellDate = gridStart.plusDays(i);
            DayButton btn = dayButtons.get(i);
            btn.setDate(cellDate, viewingDate.getMonthValue(), selected, today);
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JButton createNavButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        btn.setPreferredSize(new Dimension(28, 26));
        btn.setFocusable(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(tooltip);
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        return btn;
    }

    private void styleQuickActionButton(JButton btn) {
        btn.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        btn.setFocusable(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }

    /**
     * Botão customizado para cada dia da grelha de calendário.
     */
    private class DayButton extends JButton {
        private LocalDate date;

        public DayButton() {
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            setPreferredSize(new Dimension(34, 28));
            setBorder(new EmptyBorder(2, 2, 2, 2));
            putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);

            addActionListener(e -> {
                if (date != null && targetField != null) {
                    targetField.setDate(date);
                    ModernCalendarPopup.this.setVisible(false);
                }
            });
        }

        public void setDate(LocalDate date, int currentMonth, LocalDate selectedDate, LocalDate today) {
            this.date = date;
            setText(String.valueOf(date.getDayOfMonth()));

            boolean isCurrentMonth = date.getMonthValue() == currentMonth;
            boolean isSelected = selectedDate != null && date.equals(selectedDate);
            boolean isToday = date.equals(today);

            if (isSelected) {
                setBackground(UIHelper.ACCENT_BLUE);
                setForeground(Color.WHITE);
                setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
                setOpaque(true);
                setBorder(BorderFactory.createLineBorder(UIHelper.ACCENT_BLUE, 1, true));
            } else if (isToday) {
                setBackground(UIHelper.isLight() ? UIHelper.ROW_ALT : UIHelper.BG_DARK);
                setForeground(UIHelper.ACCENT_BLUE);
                setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
                setOpaque(true);
                setBorder(BorderFactory.createLineBorder(UIHelper.ACCENT_BLUE, 1, true));
            } else {
                setOpaque(false);
                setBorder(new EmptyBorder(2, 2, 2, 2));
                if (isCurrentMonth) {
                    setForeground(UIHelper.isLight() ? Color.DARK_GRAY : Color.WHITE);
                    setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
                } else {
                    setForeground(UIHelper.TEXT_MUTED);
                    setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
                }
            }
        }

        public LocalDate getDate() {
            return date;
        }
    }

    public List<DayButton> getDayButtons() {
        return dayButtons;
    }

    public LocalDate getViewingDate() {
        return viewingDate;
    }
}
