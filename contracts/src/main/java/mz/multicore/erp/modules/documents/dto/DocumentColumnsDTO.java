package mz.multicore.erp.modules.documents.dto;

import jakarta.validation.constraints.Size;

/**
 * Boundary value defining which columns appear in commercial document line tables.
 * Canonical order: barcode, reference, description, expiry, quantity, packages, cases, case
 * percentage, unit price, VAT, and subtotal.
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

    /** Backward-compatible constructor without boxPercentage, which defaults to false. */
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

    /** All columns visible without comments, used when no configuration is stored. */
    public static DocumentColumnsDTO all() {
        return new DocumentColumnsDTO(true, true, true, true, true, true, true, true, true, true, true, null);
    }

    /** Returns true when at least one column is visible. */
    public boolean anyVisible() {
        return barcode || reference || description || expiry || quantity || packages || boxes || boxPercentage
                || unitPrice || tax || subtotal;
    }
}
