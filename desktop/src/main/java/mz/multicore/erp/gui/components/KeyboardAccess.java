package mz.multicore.erp.gui.components;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import java.awt.Component;
import java.awt.Container;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;

/**
 * Keyboard and screen-reader access for every dialog (docs/TECLADO_ACESSIBILIDADE_SPEC.md).
 *
 * <p>Applied by {@link DialogKeys} when a dialog opens:
 * <ul>
 *   <li>each visible button with text gets an Alt+letter, never repeated inside the same dialog — the first
 *       letter of a word when free, otherwise the next free letter of its text; a mnemonic set by the screen is
 *       kept;</li>
 *   <li>a button with no text and no accessible name takes its tooltip as accessible name, so a screen reader
 *       does not announce just "botão".</li>
 * </ul>
 * FlatLaf only underlines the letters while Alt is held, so the dialogs keep their clean look.
 */
public final class KeyboardAccess {

    private KeyboardAccess() {
    }

    public static void apply(JDialog dialog) {
        Set<Character> used = new HashSet<>();
        java.util.List<AbstractButton> buttons = new java.util.ArrayList<>();
        collect(dialog.getContentPane(), buttons);
        for (AbstractButton button : buttons) {
            if (button.getMnemonic() != 0) used.add(Character.toUpperCase((char) button.getMnemonic()));
        }
        for (AbstractButton button : buttons) {
            nameFromTooltip(button);
            if (button.getMnemonic() != 0) continue;
            char letter = pick(button.getText(), used);
            if (letter != 0) {
                used.add(letter);
                button.setMnemonic(letter);
                button.setDisplayedMnemonicIndex(indexOf(button.getText(), letter));
            }
        }
    }

    /** First letter of a word that is still free, else any free letter of the text; 0 when none. */
    static char pick(String text, Set<Character> used) {
        if (text == null || text.isBlank()) return 0;
        String plain = plain(text);
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            boolean wordStart = i == 0 || !Character.isLetter(plain.charAt(i - 1));
            if (wordStart && isAsciiLetter(c) && !used.contains(Character.toUpperCase(c))) return Character.toUpperCase(c);
        }
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (isAsciiLetter(c) && !used.contains(Character.toUpperCase(c))) return Character.toUpperCase(c);
        }
        return 0;
    }

    static void nameFromTooltip(AbstractButton button) {
        boolean noText = button.getText() == null || button.getText().isBlank();
        String name = button.getAccessibleContext().getAccessibleName();
        if (noText && (name == null || name.isBlank()) && button.getToolTipText() != null && !button.getToolTipText().isBlank()) {
            button.getAccessibleContext().setAccessibleName(button.getToolTipText());
        }
    }

    private static void collect(Container parent, java.util.List<AbstractButton> out) {
        for (Component child : parent.getComponents()) {
            if (child instanceof AbstractButton button && button.isVisible()) out.add(button);
            if (child instanceof Container container) collect(container, out);
        }
    }

    /** Position of the letter in the original text (accents count as their base letter). */
    private static int indexOf(String text, char letter) {
        String plain = plain(text);
        for (int i = 0; i < plain.length(); i++) {
            boolean wordStart = i == 0 || !Character.isLetter(plain.charAt(i - 1));
            if (wordStart && Character.toUpperCase(plain.charAt(i)) == letter) return i;
        }
        return Math.max(0, plain.toUpperCase().indexOf(letter));
    }

    private static String plain(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < decomposed.length(); i++) {
            char c = decomposed.charAt(i);
            if (Character.getType(c) != Character.NON_SPACING_MARK) out.append(c);
        }
        // NFD keeps one char per base letter, so positions match the original text.
        return out.length() == text.length() ? out.toString() : text;
    }

    private static boolean isAsciiLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}
