package mz.multicore.erp.architecture.changes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** AA-01 — see docs/ACTUALIZACAO_AUTOMATICA_SPEC.md §3. */
class ChangeVersionRegistryTest {

    @Test
    void aa01_contadorSobeSoNaEmpresaQueMudouEEpochFica() {
        ChangeVersionRegistry registry = new ChangeVersionRegistry();
        String epoch = registry.epoch();

        assertThat(registry.current(1L)).isZero();
        registry.bump(1L);
        registry.bump(1L);

        assertThat(registry.current(1L)).isEqualTo(2L);
        assertThat(registry.current(2L)).isZero();
        assertThat(registry.snapshot(1L)).isEqualTo(new ChangeVersionDTO(epoch, 2L));
        assertThat(registry.epoch()).isEqualTo(epoch);
    }

    @Test
    void aa01_semEmpresaNaoContaNemFalha() {
        ChangeVersionRegistry registry = new ChangeVersionRegistry();
        assertThat(registry.bump(null)).isZero();
        assertThat(registry.current(null)).isZero();
    }

    @Test
    void aa01_cadaArranqueDoServidorTemEpochNovo() {
        assertThat(new ChangeVersionRegistry().epoch()).isNotEqualTo(new ChangeVersionRegistry().epoch());
    }
}
