package mz.multicore.erp.gui.components;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Input canónico de data ISO usado pelos contratos HTTP (`yyyy-MM-dd`) com seletor de calendário interativo.
 */
public class DateField extends JTextField {

    private final JButton calendarButton;
    private ModernCalendarPopup calendarPopup;

    public DateField() {
        this(null);
    }

    public DateField(LocalDate value) {
        super(value == null ? "" : value.toString());
        // Dez colunas ANTES de estilizar para garantir largura adequada
        setColumns(10);
        UIHelper.styleTextField(this);
        setToolTipText("Formato: yyyy-MM-dd (F4 para abrir calendário)");
        getAccessibleContext().setAccessibleDescription("Data no formato ano-mês-dia");

        calendarButton = new JButton(UIHelper.icon("fas-calendar-alt", 13, UIHelper.ACCENT_BLUE));
        calendarButton.setFocusable(false);
        calendarButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        calendarButton.setToolTipText("Abrir calendário (F4 ou Alt+Seta Para Baixo)");
        calendarButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        calendarButton.addActionListener(e -> openCalendarPopup());

        putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, calendarButton);

        // Atalhos de teclado: F4 e Alt+Down para abrir o calendário
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "openCalendar");
        getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK), "openCalendar");
        getActionMap().put("openCalendar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openCalendarPopup();
            }
        });
    }

    /**
     * Exibe o popup de calendário posicionado logo abaixo deste campo de data.
     */
    public void openCalendarPopup() {
        if (!isEditable() || !isEnabled()) return;
        if (calendarPopup == null || !calendarPopup.isVisible()) {
            calendarPopup = new ModernCalendarPopup(this);
            if (isShowing()) {
                calendarPopup.show(this, 0, getHeight() + 2);
            }
        }
    }

    public JButton getCalendarButton() {
        return calendarButton;
    }

    public ModernCalendarPopup getCalendarPopup() {
        return calendarPopup;
    }

    /**
     * Retorna a data preenchida como {@link LocalDate} ou {@code null} se o campo estiver vazio ou inválido.
     */
    public LocalDate getDate() {
        String txt = getText();
        if (txt == null || txt.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(txt.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /**
     * Define a data e limpa qualquer estado de erro no campo.
     */
    public void setDate(LocalDate date) {
        setText(date == null ? "" : date.toString());
        UIHelper.clearFieldInvalid(this);
    }

    /**
     * Só ISO, de propósito — e o {@code CanonicalFormFieldsTest} carrega essa decisão.
     *
     * <p>Aceitar também dd/MM/yyyy parece simpático e é uma armadilha: o formato humano é ambíguo
     * entre dia e mês para quem integra, e o campo existe para alimentar contratos HTTP que são
     * ISO. Quem mostra datas ao utilizador formata-as; quem as recebe exige o formato canónico.
     */
    public LocalDate value() {
        try {
            LocalDate parsed = LocalDate.parse(getText().trim());
            UIHelper.clearFieldInvalid(this);
            return parsed;
        } catch (DateTimeParseException ex) {
            String message = "Introduza uma data válida no formato yyyy-MM-dd.";
            UIHelper.markFieldInvalid(this, message);
            throw new IllegalArgumentException(message);
        }
    }
}
