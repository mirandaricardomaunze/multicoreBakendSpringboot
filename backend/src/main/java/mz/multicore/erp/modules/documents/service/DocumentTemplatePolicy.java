package mz.multicore.erp.modules.documents.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.documents.dto.DocumentBankAccountDTO;
import mz.multicore.erp.modules.documents.dto.DocumentTemplateDTO;
import mz.multicore.erp.modules.documents.model.DocumentLogoPosition;

import java.util.ArrayList;
import java.util.List;

/**
 * Regras das opções do modelo de documento (MODELOS_DOCUMENTO_SPEC §3.1, DT-02): posição do logótipo
 * ({@code LEFT} por omissão), notas até 500 caracteres, até 3 contas bancárias completas e o QR code.
 * A cor de destaque segue {@link DocumentAccentColorPolicy} (DT-01).
 */
public final class DocumentTemplatePolicy {

    public static final int MAX_NOTES_LENGTH = 500;
    public static final int MAX_BANK_ACCOUNTS = 3;
    public static final int MAX_BANK_NAME_LENGTH = 100;
    public static final int MAX_HOLDER_LENGTH = 150;
    public static final int MAX_ACCOUNT_NUMBER_LENGTH = 50;

    private DocumentTemplatePolicy() {}

    /**
     * Valida e normaliza o modelo pedido (espaços cortados, vazios a {@code null}).
     *
     * @throws BusinessRuleException quando alguma opção viola as regras da SPEC §3.1
     */
    public static DocumentTemplateDTO normalize(DocumentTemplateDTO template) {
        DocumentTemplateDTO source = template == null ? DocumentTemplateDTO.defaults() : template;
        return new DocumentTemplateDTO(
                DocumentAccentColorPolicy.normalize(source.accentColor()),
                source.logoPosition() == null ? DocumentLogoPosition.LEFT : source.logoPosition(),
                normalizeNotes(source.notes()),
                normalizeAccounts(source.bankAccounts()),
                source.showQrCode());
    }

    private static String normalizeNotes(String notes) {
        String trimmed = blankToNull(notes);
        if (trimmed != null && trimmed.length() > MAX_NOTES_LENGTH) {
            throw new BusinessRuleException(
                    "As notas do documento não podem exceder " + MAX_NOTES_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    private static List<DocumentBankAccountDTO> normalizeAccounts(List<DocumentBankAccountDTO> accounts) {
        if (accounts.size() > MAX_BANK_ACCOUNTS) {
            throw new BusinessRuleException(
                    "O documento só pode mostrar até " + MAX_BANK_ACCOUNTS + " contas bancárias.");
        }
        List<DocumentBankAccountDTO> normalized = new ArrayList<>(accounts.size());
        for (int i = 0; i < accounts.size(); i++) {
            DocumentBankAccountDTO account = accounts.get(i);
            String label = "conta bancária " + (i + 1);
            normalized.add(new DocumentBankAccountDTO(
                    required(account == null ? null : account.bankName(), "o banco", label, MAX_BANK_NAME_LENGTH),
                    required(account == null ? null : account.holder(), "o titular", label, MAX_HOLDER_LENGTH),
                    required(account == null ? null : account.accountNumber(), "o NIB/IBAN", label,
                            MAX_ACCOUNT_NUMBER_LENGTH)));
        }
        return List.copyOf(normalized);
    }

    private static String required(String value, String field, String label, int maxLength) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            throw new BusinessRuleException("Indique " + field + " da " + label + ".");
        }
        if (trimmed.length() > maxLength) {
            throw new BusinessRuleException("Na " + label + ", " + field + " não pode exceder "
                    + maxLength + " caracteres.");
        }
        return trimmed;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
