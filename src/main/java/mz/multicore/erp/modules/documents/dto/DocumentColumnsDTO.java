package mz.multicore.erp.modules.documents.dto;

/**
 * Valor de fronteira: que colunas aparecem na tabela de linhas dos documentos comerciais.
 * Ordem canónica: código de barras, referência, descrição, validade, quantidade, preço unitário, IVA, subtotal.
 */
public record DocumentColumnsDTO(
        boolean barcode,
        boolean reference,
        boolean description,
        boolean expiry,
        boolean quantity,
        boolean unitPrice,
        boolean tax,
        boolean subtotal,
        String footer
) {

    /** Todas as colunas visíveis, sem comentário (default quando não há configuração guardada). */
    public static DocumentColumnsDTO all() {
        return new DocumentColumnsDTO(true, true, true, true, true, true, true, true, null);
    }

    /** True se pelo menos uma coluna estiver visível. */
    public boolean anyVisible() {
        return barcode || reference || description || expiry || quantity || unitPrice || tax || subtotal;
    }
}
