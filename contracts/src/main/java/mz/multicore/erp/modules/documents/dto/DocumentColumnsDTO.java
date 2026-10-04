package mz.multicore.erp.modules.documents.dto;

import jakarta.validation.constraints.Size;

/**
 * Valor de fronteira: que colunas aparecem na tabela de linhas dos documentos comerciais.
 * Ordem canónica: código de barras, referência, descrição, validade, quantidade, embalagens,
 * caixas, percentagem da caixa, preço unitário, IVA e subtotal.
 */
public record DocumentColumnsDTO(
        boolean barcode,
        boolean reference,
        boolean description,
        boolean expiry,
        boolean quantity,
        boolean packages,
        boolean boxes,
        boolean boxPercentage,
        boolean unitPrice,
        boolean tax,
        boolean subtotal,
        @Size(max = 500, message = "O rodapé do documento não pode exceder 500 caracteres.")
        String footer
) {

    /** Construtor retrocompatível sem boxPercentage (assume false). */
    public DocumentColumnsDTO(
            boolean barcode,
            boolean reference,
            boolean description,
            boolean expiry,
            boolean quantity,
            boolean packages,
            boolean boxes,
            boolean unitPrice,
            boolean tax,
            boolean subtotal,
            String footer
    ) {
        this(barcode, reference, description, expiry, quantity, packages, boxes, false, unitPrice, tax, subtotal, footer);
    }

    /** Todas as colunas visíveis, sem comentário (default quando não há configuração guardada). */
    public static DocumentColumnsDTO all() {
        return new DocumentColumnsDTO(true, true, true, true, true, true, true, true, true, true, true, null);
    }

    /** True se pelo menos uma coluna estiver visível. */
    public boolean anyVisible() {
        return barcode || reference || description || expiry || quantity || packages || boxes || boxPercentage
                || unitPrice || tax || subtotal;
    }
}
