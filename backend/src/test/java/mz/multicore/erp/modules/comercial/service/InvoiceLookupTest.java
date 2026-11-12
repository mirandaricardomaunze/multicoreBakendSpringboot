package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** MD-V02 — porta de leitura de facturas para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2). */
class InvoiceLookupTest {

    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final InvoiceLookup lookup = new InvoiceLookup(invoices);

    private static Invoice invoice(long id) {
        Invoice i = new Invoice();
        i.setId(id);
        return i;
    }

    @Test
    void require_existente_devolveFactura() {
        Invoice i = invoice(10L);
        when(invoices.findById(10L)).thenReturn(Optional.of(i));

        assertThat(lookup.require(10L)).isSameAs(i);
    }

    @Test
    void require_inexistente_lancaMensagemNormalizada() {
        when(invoices.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookup.require(99L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Fatura não encontrada.");
    }

    @Test
    void find_idNulo_devolveVazioSemTocarNoRepositorio() {
        assertThat(lookup.find(null)).isEmpty();
        verifyNoInteractions(invoices);
    }

    @Test
    void require_idNulo_lancaMensagemNormalizada() {
        assertThatThrownBy(() -> lookup.require(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Fatura não encontrada.");
    }

    @Test
    void listCreatedBetween_delegaNoFiltroDeEmpresaEPeriodo() {
        LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 11, 30, 23, 59);
        List<Invoice> rows = List.of(invoice(1L), invoice(2L));
        when(invoices.findByCompanyIdAndCreatedAtBetween(5L, from, to)).thenReturn(rows);

        assertThat(lookup.listCreatedBetween(5L, from, to)).isEqualTo(rows);
    }

    @Test
    void ehServicoSoDeLeitura() {
        assertThat(InvoiceLookup.class.isAnnotationPresent(Service.class)).isTrue();
        Transactional tx = InvoiceLookup.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isTrue();
    }
}
