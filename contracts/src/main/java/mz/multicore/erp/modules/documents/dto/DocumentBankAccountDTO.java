package mz.multicore.erp.modules.documents.dto;

/** Conta bancária impressa no bloco de dados bancários do documento (MODELOS_DOCUMENTO_SPEC §3.1). */
public record DocumentBankAccountDTO(
        String bankName,
        String holder,
        String accountNumber
) {
}
