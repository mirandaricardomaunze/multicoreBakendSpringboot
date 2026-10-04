package mz.multicore.erp.gui.components;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/** Select editável que filtra as opções enquanto o utilizador escreve. */
public class SearchableComboBox<T> extends JComboBox<T> {
    private final List<T> allItems = new ArrayList<>();
    private final Function<T, String> labelProvider;
    private final Function<T, String> searchProvider;
    private final String searchHint;
    private boolean updating;

    public SearchableComboBox(String searchHint,
                              Function<T, String> labelProvider,
                              Function<T, String> searchProvider) {
        this.searchHint = Objects.requireNonNullElse(searchHint, "Pesquisar");
        this.labelProvider = Objects.requireNonNull(labelProvider);
        this.searchProvider = Objects.requireNonNull(searchProvider);
        setEditable(true);
        setMaximumRowCount(12);
        UIHelper.styleComboBox(this);
        setRenderer(createRenderer());
        configureEditor();
    }

    public void setItems(Collection<T> items) {
        T previous = selectedValue();
        allItems.clear();
        if (items != null) allItems.addAll(items);
        replaceModel(allItems, previous, "");
    }

    public T selectedValue() {
        Object selected = super.getSelectedItem();
        if (selected == null) return null;
        for (T item : allItems) {
            if (item == selected || Objects.equals(item, selected)) return item;
        }
        if (selected instanceof String text && !text.isBlank()) {
            String trimmed = text.trim();
            for (T item : allItems) {
                if (trimmed.equalsIgnoreCase(labelFor(item).trim())
                        || trimmed.equalsIgnoreCase(safe(labelProvider.apply(item)).trim())) {
                    return item;
                }
            }
        }
        return null;
    }

    @Override
    public void setSelectedItem(Object item) {
        boolean previousUpdating = updating;
        updating = true;
        try {
            super.setSelectedItem(item);
            JTextField editor = editorField();
            if (editor != null) editor.setText(labelFor(item));
        } finally {
            updating = previousUpdating;
        }
    }

    public boolean selectFirst(Predicate<T> predicate) {
        if (predicate == null) return false;
        for (T item : allItems) {
            if (predicate.test(item)) {
                setSelectedItem(item);
                return true;
            }
        }
        return false;
    }

    public void clearSearch() {
        T previous = selectedValue();
        JTextField editor = editorField();
        boolean previousUpdating = updating;
        updating = true;
        try {
            if (editor != null) editor.setText("");
        } finally {
            updating = previousUpdating;
        }
        replaceModel(allItems, previous, "");
    }

    public List<T> allItems() {
        return List.copyOf(allItems);
    }

    private void configureEditor() {
        JTextField editor = editorField();
        if (editor == null) return;
        editor.putClientProperty("JTextField.placeholderText", searchHint);
        editor.setToolTipText(searchHint);
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { scheduleFilter(); }
            @Override public void removeUpdate(DocumentEvent event) { scheduleFilter(); }
            @Override public void changedUpdate(DocumentEvent event) { scheduleFilter(); }
        });
        editor.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) {
                SwingUtilities.invokeLater(editor::selectAll);
            }
        });
    }

    private void scheduleFilter() {
        if (updating) return;
        SwingUtilities.invokeLater(() -> {
            if (updating) return;
            JTextField editor = editorField();
            if (editor == null) return;
            String query = editor.getText();
            T previous = selectedValue();
            List<T> matches = filter(query);
            replaceModel(matches, previous, query);
            if (isShowing() && editor.hasFocus() && !matches.isEmpty()) setPopupVisible(true);
        });
    }

    private List<T> filter(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isBlank()) return List.copyOf(allItems);
        String[] tokens = normalizedQuery.split("\\s+");
        return allItems.stream().filter(item -> {
            String searchable = normalize(safe(searchProvider.apply(item)) + " "
                    + safe(labelProvider.apply(item)));
            for (String token : tokens) {
                if (!searchable.contains(token)) return false;
            }
            return true;
        }).toList();
    }

    private void replaceModel(Collection<T> items, T preferred, String editorText) {
        updating = true;
        try {
            DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>();
            for (T item : items) model.addElement(item);
            super.setModel(model);
            if (preferred != null && contains(items, preferred)) {
                super.setSelectedItem(preferred);
            } else if (model.getSize() == 1) {
                super.setSelectedIndex(0);
            } else {
                super.setSelectedItem(null);
            }
            JTextField editor = editorField();
            if (editor != null) {
                String text = editorText != null && !editorText.isBlank()
                        ? editorText
                        : labelFor(super.getSelectedItem());
                editor.setText(text);
                editor.setCaretPosition(text.length());
            }
        } finally {
            updating = false;
        }
    }

    private boolean contains(Collection<T> items, T candidate) {
        for (T item : items) {
            if (item == candidate || Objects.equals(item, candidate)) return true;
        }
        return false;
    }

    private ListCellRenderer<? super T> createRenderer() {
        return (list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null ? "" : safe(labelProvider.apply(value)));
            label.setOpaque(true);
            label.setFont(list.getFont());
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 8, 5, 8));
            label.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(selected ? list.getSelectionForeground() : list.getForeground());
            return label;
        };
    }

    private JTextField editorField() {
        Component editor = getEditor() == null ? null : getEditor().getEditorComponent();
        return editor instanceof JTextField field ? field : null;
    }

    private String labelFor(Object value) {
        if (value == null) return "";
        for (T item : allItems) {
            if (item == value || Objects.equals(item, value)) return safe(labelProvider.apply(item));
        }
        return value instanceof String text ? text : "";
    }

    private static String normalize(String value) {
        String plain = Normalizer.normalize(safe(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return plain.toLowerCase(Locale.ROOT).trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
