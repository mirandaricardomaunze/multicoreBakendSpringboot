package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** MD-V05 — notas de crédito e de débito lidas/gravadas por outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2). */
class InvoiceNoteLookupTest {

    private final CreditNoteRepository credits = mock(CreditNoteRepository.class);
    private final DebitNoteRepository debits = mock(DebitNoteRepository.class);
    private final InvoiceNoteLookup lookup = new InvoiceNoteLookup(credits, debits);
    private final InvoiceNoteCommandService commands = new InvoiceNoteCommandService(credits);

    @Test
    void findCreditNote_devolveANota() {
        CreditNote n = new CreditNote();
        when(credits.findById(5L)).thenReturn(Optional.of(n));

        assertThat(lookup.findCreditNote(5L)).containsSame(n);
    }

    @Test
    void findCreditNote_idNulo_devolveVazioSemIrABase() {
        assertThat(lookup.findCreditNote(null)).isEmpty();
        verifyNoInteractions(credits);
    }

    @Test
    void findCreditNoteWithLinesForCompany_filtraPelaEmpresa() {
        CreditNote n = new CreditNote();
        when(credits.findByIdWithLinesAndCompanyId(5L, 7L)).thenReturn(Optional.of(n));

        assertThat(lookup.findCreditNoteWithLinesForCompany(5L, 7L)).containsSame(n);
        assertThat(lookup.findCreditNoteWithLinesForCompany(5L, 8L)).isEmpty();
    }

    @Test
    void findCreditNoteByPosReturnKey_delegaNaChaveDeIdempotencia() {
        CreditNote n = new CreditNote();
        when(credits.findByInvoiceIdAndPosReturnIdempotencyKey(3L, "k")).thenReturn(Optional.of(n));

        assertThat(lookup.findCreditNoteByPosReturnKey(3L, "k")).containsSame(n);
    }

    @Test
    void listas_porEmpresa() {
        CreditNote c = new CreditNote();
        DebitNote d = new DebitNote();
        when(credits.findByCompanyId(7L)).thenReturn(List.of(c));
        when(debits.findByCompanyId(7L)).thenReturn(List.of(d));

        assertThat(lookup.listCreditNotesForCompany(7L)).containsExactly(c);
        assertThat(lookup.listDebitNotesForCompany(7L)).containsExactly(d);
    }

    @Test
    void markCreditNotePosReturn_gravaChaveEImpressaoDigital() {
        CreditNote n = new CreditNote();
        when(credits.findById(5L)).thenReturn(Optional.of(n));

        commands.markCreditNotePosReturn(5L, "k", "fp");

        assertThat(n.getPosReturnIdempotencyKey()).isEqualTo("k");
        assertThat(n.getPosReturnRequestFingerprint()).isEqualTo("fp");
        verify(credits).save(n);
    }

    @Test
    void markCreditNotePosReturn_notaInexistente_falha() {
        when(credits.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commands.markCreditNotePosReturn(5L, "k", "fp"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Nota de crédito criada não foi encontrada.");
    }

    @Test
    void lookupEhSoDeLeituraEComandoEhDeEscrita() {
        assertThat(InvoiceNoteLookup.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(InvoiceNoteLookup.class.getAnnotation(Transactional.class).readOnly()).isTrue();
        assertThat(InvoiceNoteCommandService.class.isAnnotationPresent(Service.class)).isTrue();
        assertThat(InvoiceNoteCommandService.class.getAnnotation(Transactional.class).readOnly()).isFalse();
    }
}
