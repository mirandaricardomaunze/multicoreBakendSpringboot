package mz.multicore.erp.gui.sector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SA-C01: capacidades do perfil escondem funcionalidades dentro dos ecrãs")
class ActivityCapabilitiesTest {

    @AfterEach
    void reset() {
        ActivityCapabilities.resetForTests();
    }

    @Test
    @DisplayName("Sem perfil carregado, tudo continua disponível")
    void everythingEnabledWithoutProfile() {
        assertThat(ActivityCapabilities.enabled(ActivityCapabilities.BULK_STOCK)).isTrue();
        assertThat(ActivityCapabilities.enabled(ActivityCapabilities.WEIGHED_SALES)).isTrue();
    }

    @Test
    @DisplayName("Botão ligado a uma capacidade segue o perfil")
    void bindVisibleFollowsProfile() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel bar = new JPanel();
            JButton freight = new JButton("Frete");
            bar.add(freight);
            ActivityCapabilities.bindVisible(freight, ActivityCapabilities.DELIVERIES);
            assertThat(freight.isVisible()).isTrue();

            ActivityCapabilities.apply(List.of("PRICE_LISTS"));
            assertThat(freight.isVisible()).isFalse();
            assertThat(ActivityCapabilities.enabled(ActivityCapabilities.DELIVERIES)).isFalse();

            ActivityCapabilities.apply(Set.of("PRICE_LISTS", ActivityCapabilities.DELIVERIES));
            assertThat(freight.isVisible()).isTrue();
        });
    }

    @Test
    @DisplayName("Separador sai sem a capacidade e volta com o mesmo título")
    void bindTabRemovesAndRestores() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Stock", new JPanel());
            JPanel bulk = new JPanel();
            ActivityCapabilities.bindTab(tabs, bulk, "Granel", null, "Inventário por medição", ActivityCapabilities.BULK_STOCK);
            assertThat(tabs.indexOfComponent(bulk)).isEqualTo(1);

            tabs.setSelectedComponent(bulk);
            ActivityCapabilities.apply(List.of("PRICE_LISTS"));
            assertThat(tabs.indexOfComponent(bulk)).isEqualTo(-1);
            assertThat(tabs.getSelectedIndex()).isZero();

            ActivityCapabilities.apply(List.of(ActivityCapabilities.BULK_STOCK));
            assertThat(tabs.indexOfTab("Granel")).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("onChange corre já e a cada mudança de perfil")
    void onChangeRunsNowAndOnEveryApply() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger runs = new AtomicInteger();
            JPanel owner = new JPanel();
            ActivityCapabilities.onChange(owner, runs::incrementAndGet);
            ActivityCapabilities.apply(List.of(ActivityCapabilities.WEIGHED_SALES));
            ActivityCapabilities.apply(List.of());
            assertThat(runs.get()).isEqualTo(3);
        });
    }
}
