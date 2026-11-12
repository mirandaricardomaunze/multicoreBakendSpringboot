package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** MD-V03 — escrita de facturas vinda de outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2). */
class InvoiceCommandServiceTest {

    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final InvoiceCommandService commands = new InvoiceCommandService(invoices);

    @Test
    void lockForCompany_delegaNoBloqueioComFiltroDeEmpresa() {
        Invoice i = new Invoice();
        when(invoices.lockByIdAndCompanyId(3L, 7L)).thenReturn(Optional.of(i));

        assertThat(commands.lockForCompany(3L, 7L)).containsSame(i);
    }

    @Test
    void lockForCompany_inexistente_devolveVazio() {
        when(invoices.lockByIdAndCompanyId(3L, 7L)).thenReturn(Optional.empty());

        assertThat(commands.lockForCompany(3L, 7L)).isEmpty();
    }

    @Test
    void save_devolveAFacturaGravada() {
        Invoice i = new Invoice();
        Invoice saved = new Invoice();
        when(invoices.save(i)).thenReturn(saved);

        assertThat(commands.save(i)).isSameAs(saved);
    }

    @Test
    void ehServicoTransaccionalDeEscrita() {
        assertThat(InvoiceCommandService.class.isAnnotationPresent(Service.class)).isTrue();
        Transactional tx = InvoiceCommandService.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isFalse();
    }
}
