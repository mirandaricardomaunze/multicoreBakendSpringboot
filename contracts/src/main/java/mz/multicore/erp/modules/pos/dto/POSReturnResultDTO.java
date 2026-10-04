package mz.multicore.erp.modules.pos.dto;

import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;

/**
 * Resultado do processamento de uma devolução no POS.
 * Retorna a nota de crédito emitida e, caso o método tenha sido STORE_CREDIT, o vale de compras gerado.
 */
public record POSReturnResultDTO(
        CreditNoteDTO creditNote,
        StoreVoucherDTO voucher,
        String message
) {
    public POSReturnResultDTO(CreditNoteDTO creditNote, StoreVoucherDTO voucher) {
        this(creditNote, voucher, null);
    }

    public POSReturnResultDTO(CreditNoteDTO creditNote) {
        this(creditNote, null, null);
    }
}
