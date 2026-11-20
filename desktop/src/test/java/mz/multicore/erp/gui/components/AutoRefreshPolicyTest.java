package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.changes.ChangeVersionDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** AA-05, AA-06 — see docs/ACTUALIZACAO_AUTOMATICA_SPEC.md §4.1 and §4.3. */
class AutoRefreshPolicyTest {

    private static final AutoRefreshPolicy.UiState IDLE = new AutoRefreshPolicy.UiState(false, 60_000, false, false, false);

    @Test
    void aa05_primeiraLeituraSoMemoriza() {
        assertThat(AutoRefreshPolicy.changed(null, new ChangeVersionDTO("e1", 7))).isFalse();
    }

    @Test
    void aa05_versaoQueSobeOuServidorReiniciadoContamComoAlteracao() {
        ChangeVersionDTO last = new ChangeVersionDTO("e1", 7);
        assertThat(AutoRefreshPolicy.changed(last, new ChangeVersionDTO("e1", 7))).isFalse();
        assertThat(AutoRefreshPolicy.changed(last, new ChangeVersionDTO("e1", 8))).isTrue();
        assertThat(AutoRefreshPolicy.changed(last, new ChangeVersionDTO("e2", 0))).isTrue();
    }

    @Test
    void aa06_livreRecarrega() {
        assertThat(AutoRefreshPolicy.canRefresh(IDLE)).isTrue();
    }

    @Test
    void aa06_ocupadoAdia() {
        assertThat(AutoRefreshPolicy.canRefresh(new AutoRefreshPolicy.UiState(true, 60_000, false, false, false))).isFalse();
        assertThat(AutoRefreshPolicy.canRefresh(new AutoRefreshPolicy.UiState(false, 2_999, false, false, false))).isFalse();
        assertThat(AutoRefreshPolicy.canRefresh(new AutoRefreshPolicy.UiState(false, 60_000, true, false, false))).isFalse();
        assertThat(AutoRefreshPolicy.canRefresh(new AutoRefreshPolicy.UiState(false, 60_000, false, true, false))).isFalse();
        assertThat(AutoRefreshPolicy.canRefresh(new AutoRefreshPolicy.UiState(false, 60_000, false, false, true))).isFalse();
    }

    @Test
    void aa05_verificaACada5sEAguarda3sDeTeclado() {
        assertThat(AutoRefreshPolicy.POLL_INTERVAL_MS).isEqualTo(5_000);
        assertThat(AutoRefreshPolicy.TYPING_QUIET_MS).isEqualTo(3_000);
    }
}
