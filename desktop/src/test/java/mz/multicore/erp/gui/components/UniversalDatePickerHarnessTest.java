package mz.multicore.erp.gui.components;

import com.formdev.flatlaf.FlatClientProperties;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de testes executivos para o Seletor Universal de Calendário (HARNESS-UDP-001).
 * Validação de UDP-01 a UDP-06 conforme docs/UNIVERSAL_DATE_PICKER_SPEC.md.
 */
class UniversalDatePickerHarnessTest {

    @Test
    void udp01_trailingCalendarButtonInstalledOnDateField() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DateField field = new DateField();
            JButton calBtn = field.getCalendarButton();

            assertNotNull(calBtn, "DateField deve possuir botão de calendário inicializado");
            assertEquals(calBtn, field.getClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT),
                    "O botão deve estar anexado como trailing component FlatLaf");
            assertEquals(Cursor.HAND_CURSOR, calBtn.getCursor().getType());
            assertThat(calBtn.getToolTipText()).contains("F4");
        });
    }

    @Test
    void udp02_modernCalendarPopupInitializesCorrectly() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LocalDate targetDate = LocalDate.of(2026, 10, 15);
            DateField field = new DateField(targetDate);
            field.setSize(120, 32);

            assertDoesNotThrow(field::openCalendarPopup);

            ModernCalendarPopup popup = field.getCalendarPopup();
            assertNotNull(popup, "O popup de calendário deve ser instanciado");
            assertEquals(2026, popup.getViewingDate().getYear());
            assertEquals(10, popup.getViewingDate().getMonthValue());
            assertEquals(42, popup.getDayButtons().size(), "A matriz do calendário deve conter 42 células");

            popup.setVisible(false);
        });
    }

    @Test
    void udp03_daySelectionUpdatesDateFieldWithIsoFormat() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DateField field = new DateField();
            ModernCalendarPopup popup = new ModernCalendarPopup(field);

            // Selecionar o botão que corresponde ao dia 15 do mês visualizado
            JButton targetDayBtn = null;
            LocalDate expectedDate = null;
            for (Component comp : popup.getDayButtons()) {
                if (comp instanceof JButton btn) {
                    try {
                        java.lang.reflect.Method m = btn.getClass().getMethod("getDate");
                        LocalDate d = (LocalDate) m.invoke(btn);
                        if (d.getMonthValue() == popup.getViewingDate().getMonthValue() && d.getDayOfMonth() == 15) {
                            targetDayBtn = btn;
                            expectedDate = d;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }

            assertNotNull(targetDayBtn, "Deve encontrar o botão do dia 15 na matriz");
            assertNotNull(expectedDate);

            targetDayBtn.doClick();

            assertEquals(expectedDate.toString(), field.getText(),
                    "O campo de data deve ser preenchido estritamente no formato ISO AAAA-MM-DD");
            assertEquals(expectedDate, field.value(), "O valor do campo deve ser resolvido sem exceção");
            assertFalse(popup.isVisible(), "O popup de calendário deve fechar após a seleção");
        });
    }

    @Test
    void udp04_todayButtonSetsCurrentDate() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DateField field = new DateField();
            field.setDate(LocalDate.of(2020, 1, 1));
            ModernCalendarPopup popup = new ModernCalendarPopup(field);

            JButton todayBtn = findButtonByText(popup, "Hoje");
            assertNotNull(todayBtn, "O botão 'Hoje' deve estar presente no rodapé do calendário");

            todayBtn.doClick();

            LocalDate today = LocalDate.now();
            assertEquals(today.toString(), field.getText());
            assertEquals(today, field.getDate());
            assertEquals(today, field.value());
            assertFalse(popup.isVisible());
        });
    }

    @Test
    void udp05_clearButtonEmptiesField() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DateField field = new DateField(LocalDate.now());
            ModernCalendarPopup popup = new ModernCalendarPopup(field);

            JButton clearBtn = findButtonByText(popup, "Limpar");
            assertNotNull(clearBtn, "O botão 'Limpar' deve estar presente no rodapé do calendário");

            clearBtn.doClick();

            assertTrue(field.getText().isEmpty(), "O texto do campo deve ficar vazio após clicar em 'Limpar'");
            assertNull(field.getDate(), "getDate() deve retornar null para campo limpo");
            assertFalse(popup.isVisible());
        });
    }

    @Test
    void udp06_preservesStrictIsoContractAndManualTyping() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DateField field = new DateField();

            // Digitação manual ISO
            field.setText("2026-11-20");
            assertEquals(LocalDate.of(2026, 11, 20), field.value());

            // Tentativa de digitação fora de padrão
            field.setText("20/11/2026");
            assertThrows(IllegalArgumentException.class, field::value);

            // Uso de setDate programático
            field.setDate(LocalDate.of(2027, 5, 1));
            assertEquals("2027-05-01", field.getText());
            assertEquals(LocalDate.of(2027, 5, 1), field.getDate());
        });
    }

    private static JButton findButtonByText(Container container, String text) {
        for (Component c : container.getComponents()) {
            if (c instanceof JButton b && text.equals(b.getText())) {
                return b;
            }
            if (c instanceof Container sub) {
                JButton found = findButtonByText(sub, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
