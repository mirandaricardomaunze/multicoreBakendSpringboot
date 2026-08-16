package com.phcpro.architecture.version;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Registo de versões por empresa (AC-30..AC-36).
 * Ver docs/ACTUALIZACOES_CLIENTE_SPEC.md §8.
 */
class ClientVersionRegistryTest {

    private ClientVersionSightingRepository repository;
    private ClientVersionRegistry registry;

    @BeforeEach
    void setUp() {
        repository = mock(ClientVersionSightingRepository.class);
        registry = new ClientVersionRegistry(repository);
        when(repository.findByCompanyIdAndClientVersion(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test // AC-30
    void primeiraVezGravaAEmpresaEAVersao() {
        registry.record(7L, "1.4.0", "ana");

        var saved = org.mockito.ArgumentCaptor.forClass(ClientVersionSighting.class);
        verify(repository).save(saved.capture());
        assertEquals(7L, saved.getValue().getCompanyId());
        assertEquals("1.4.0", saved.getValue().getClientVersion());
        assertEquals("ana", saved.getValue().getLastUsername());
        assertNotNull(saved.getValue().getFirstSeenAt());
        assertNotNull(saved.getValue().getLastSeenAt());
    }

    @Test // AC-31
    void pedidosSeguidosNaoVoltamAGravar() {
        // Uma loja faz milhares de pedidos por dia; a versão dela muda uma vez por mês.
        for (int i = 0; i < 50; i++) {
            registry.record(7L, "1.4.0", "ana");
        }

        verify(repository, times(1)).save(any());
    }

    @Test // AC-32
    void empresasEVersoesDiferentesSaoLinhasDiferentes() {
        registry.record(7L, "1.4.0", "ana");
        registry.record(8L, "1.4.0", "beto");
        registry.record(7L, "1.5.0", "ana");

        verify(repository, times(3)).save(any());
    }

    @Test // AC-33
    void versaoJaConhecidaActualizaOUltimoAvistamento() {
        ClientVersionSighting existing = new ClientVersionSighting();
        existing.setCompanyId(7L);
        existing.setClientVersion("1.4.0");
        existing.setFirstSeenAt(LocalDateTime.of(2026, 1, 1, 8, 0));
        existing.setLastSeenAt(LocalDateTime.of(2026, 1, 1, 8, 0));
        when(repository.findByCompanyIdAndClientVersion(7L, "1.4.0")).thenReturn(Optional.of(existing));

        registry.record(7L, "1.4.0", "carla");

        assertEquals(LocalDateTime.of(2026, 1, 1, 8, 0), existing.getFirstSeenAt(),
                "a primeira vez não se reescreve");
        assertTrue(existing.getLastSeenAt().isAfter(existing.getFirstSeenAt()));
        assertEquals("carla", existing.getLastUsername());
    }

    @Test // AC-34
    void dadosIncompletosNaoGravamNada() {
        registry.record(null, "1.4.0", "ana");
        registry.record(7L, null, "ana");
        registry.record(7L, "   ", "ana");

        verify(repository, never()).save(any());
    }

    @Test // AC-35
    void falhaAGravarNuncaRebentaOPedido() {
        // Isto é informação de gestão, não parte da venda: mais vale perder o dado do que
        // impedir a loja de facturar.
        when(repository.save(any())).thenThrow(new RuntimeException("base de dados em baixo"));

        assertDoesNotThrow(() -> registry.record(7L, "1.4.0", "ana"));
    }

    @Test // AC-36
    void depoisDeFalharVoltaATentar() {
        when(repository.save(any())).thenThrow(new RuntimeException("falha transitória"));
        registry.record(7L, "1.4.0", "ana");

        reset(repository);
        when(repository.findByCompanyIdAndClientVersion(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        registry.record(7L, "1.4.0", "ana");

        verify(repository).save(any());
    }

    @Test // AC-37
    void aListaDizQuemEstaEmQue() {
        ClientVersionSighting sighting = new ClientVersionSighting();
        sighting.setCompanyId(7L);
        sighting.setClientVersion("1.4.0");
        sighting.setLastUsername("ana");
        sighting.setFirstSeenAt(LocalDateTime.of(2026, 1, 1, 8, 0));
        sighting.setLastSeenAt(LocalDateTime.of(2026, 3, 1, 8, 0));
        when(repository.findAllByOrderByLastSeenAtDesc()).thenReturn(List.of(sighting));

        List<ClientVersionUsageDTO> usage = registry.listUsage();

        assertEquals(1, usage.size());
        assertEquals(7L, usage.get(0).companyId());
        assertEquals("1.4.0", usage.get(0).clientVersion());
        assertEquals("ana", usage.get(0).lastUsername());
    }
}
