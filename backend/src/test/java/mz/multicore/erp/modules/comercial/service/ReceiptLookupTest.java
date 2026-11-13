package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.Receipt;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** MD-V05 — leitura e escrita de recibos vindas de outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2). */
class ReceiptLookupTest {

    private final ReceiptRepository receipts = mock(ReceiptRepository.class);
    private final ReceiptLookup lookup = new ReceiptLookup(receipts);
    private final ReceiptCommandService commands = new ReceiptCommandService(receipts);

    @Test
    void find_devolveORecibo() {
        Receipt r = new Receipt();
        when(receipts.findById(4L)).thenReturn(Optional.of(r));

        assertThat(lookup.find(4L)).containsSame(r);
    }

    @Test
    void find_idNulo_devolveVazioSemIrABase() {
        assertThat(lookup.find(null)).isEmpty();
        verifyNoInteractions(receipts);
    }

    @Test
    void listForCompany_delegaNoFiltroDeEmpresa() {
        Receipt r = new Receipt();
        when(receipts.findByCompanyId(7L)).thenReturn(List.of(r));

        assertThat(lookup.listForCompany(7L)).containsExactly(r);
    }

    @Test
    void save_devolveOReciboGravado() {
        Receipt r = new Receipt();
        Receipt saved = new Receipt();
        when(receipts.save(r)).thenReturn(saved);

        assertThat(commands.save(r)).isSameAs(saved);
    }

    @Test
    void lookupEhSoDeLeituraEComandoEhDeEscrita() {
        assertThat(ReceiptLookup.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ReceiptLookup.class.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(ReceiptCommandService.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(ReceiptCommandService.class.getAnnotation(Transactional.class).readOnly()).isFalse();
    }
}
