package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** Picking a record from the list must show its label in the editor, never the record's internal form. */
class SearchableComboBoxRecordLabelTest {

    record Invoice(Long id, String number, String client) {}

    @Test
    void editorShowsTheLabelWhenAnItemIsPicked() throws Exception {
        AtomicReference<String> text = new AtomicReference<>();
        AtomicReference<Invoice> selected = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            SearchableComboBox<Invoice> combo = new SearchableComboBox<>("Pesquisar",
                    i -> i.number() + " · " + i.client(), i -> i.number() + " " + i.client());
            Invoice ft7 = new Invoice(193L, "FT-2026/7", "Miranda");
            combo.setItems(List.of(new Invoice(1L, "FT-2026/1", "Ana"), ft7));
            combo.configureEditor(combo.getEditor(), ft7);
            combo.setSelectedItem(combo.getEditor().getItem());
            text.set(((JTextField) combo.getEditor().getEditorComponent()).getText());
            selected.set(combo.selectedValue());
        });
        assertThat(text.get()).isEqualTo("FT-2026/7 · Miranda").doesNotContain("Invoice[");
        assertThat(selected.get().id()).isEqualTo(193L);
    }
}
