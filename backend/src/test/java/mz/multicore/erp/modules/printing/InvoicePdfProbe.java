package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.comercial.model.Invoice;

import java.math.BigDecimal;
import java.util.List;

/** Dá aos testes de outros pacotes acesso ao renderizador da fatura, que é interno a este. */
public final class InvoicePdfProbe {

    private final LineRowMapper mapper;

    public InvoicePdfProbe(LineRowMapper mapper) {
        this.mapper = mapper;
        org.mockito.Mockito.when(mapper.map(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(call -> new LineItemsTableRenderer.Row(null, null, "Arroz", null, call.getArgument(2),
                        1, 1, call.getArgument(3), call.getArgument(4), call.getArgument(5), (BigDecimal) call.getArgument(6)));
    }

    public byte[] render(Invoice invoice) {
        return new InvoiceA4Renderer(mapper).render(invoice, List.of("1.ª VIA — ORIGINAL (Cliente)"));
    }
}
