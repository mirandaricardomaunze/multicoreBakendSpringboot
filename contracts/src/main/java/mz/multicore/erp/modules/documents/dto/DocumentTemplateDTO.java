package mz.multicore.erp.modules.documents.dto;

import mz.multicore.erp.modules.documents.model.DocumentLogoPosition;

import java.util.List;

/**
 * Opções visuais do modelo de documento por empresa e tipo (MODELOS_DOCUMENTO_SPEC §3.1), além das
 * colunas e do rodapé de {@link DocumentColumnsDTO}. {@code accentColor} nulo = cor do tema por omissão.
 */
public record DocumentTemplateDTO(
        String accentColor,
        DocumentLogoPosition logoPosition,
        String notes,
        List<DocumentBankAccountDTO> bankAccounts,
        boolean showQrCode
) {

    public DocumentTemplateDTO {
        bankAccounts = bankAccounts == null ? List.of() : List.copyOf(bankAccounts);
    }

    /** Modelo por omissão: sem cor própria, logótipo à esquerda, sem notas nem bancos, QR quando existir. */
    public static DocumentTemplateDTO defaults() {
        return new DocumentTemplateDTO(null, DocumentLogoPosition.LEFT, null, List.of(), true);
    }
}
