-- Reason for not charging VAT, frozen on the invoice line at issue (docs/COMERCIAL_DOCUMENTOS_AUDITORIA_SPEC.md,
-- "Texto legal da fatura"). Older lines stay NULL and fall back to the product's current tax rate legal basis.
ALTER TABLE invoice_lines ADD COLUMN IF NOT EXISTS exemption_reason VARCHAR(300);
