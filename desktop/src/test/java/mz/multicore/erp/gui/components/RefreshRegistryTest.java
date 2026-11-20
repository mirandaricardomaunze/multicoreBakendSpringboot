package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** AA-07 — see docs/ACTUALIZACAO_AUTOMATICA_SPEC.md §4.2. */
class RefreshRegistryTest {

    @Test
    void aa07_todoOBotaoActualizarFicaRegistadoEOCliqueCorreAAccao() {
        AtomicInteger runs = new AtomicInteger();
        AbstractButton button = UIHelper.createRefreshButton(runs::incrementAndGet);

        assertThat(RefreshRegistry.matching(candidate -> candidate == button)).containsExactly(button);

        RefreshRegistry.matching(candidate -> candidate == button).forEach(b -> b.doClick(0));
        assertThat(runs).hasValue(1);
        assertThat(button.getText()).isEqualTo("Actualizar");
    }

    @Test
    void aa07_soOsBotoesQuePassamOFiltroDeVisibilidadeSaoDevolvidos() {
        AbstractButton visible = UIHelper.createRefreshButton(() -> { });
        AbstractButton hidden = UIHelper.createRefreshButton(() -> { });
        hidden.setEnabled(false);

        assertThat(RefreshRegistry.matching(b -> (b == visible || b == hidden) && b.isEnabled()))
                .containsExactly(visible);
    }

    @Test
    void aa07_botaoForaDeUmaJanelaNaoEstaNoEcra() {
        AbstractButton detached = UIHelper.createRefreshButton(() -> { });
        assertThat(RefreshRegistry.onScreen(null)).doesNotContain(detached);
    }
}
