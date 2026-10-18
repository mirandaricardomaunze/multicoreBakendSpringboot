package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

import java.util.Locale;
import java.util.Set;

/**
 * Política canónica de senhas do Multicore ERP.
 * Define requisitos mínimos de segurança para prevenir senhas triviais e ataques de dicionário.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;

    private static final Set<String> COMMON_BLACKLIST = Set.of(
            "12345678",
            "123456789",
            "password",
            "admin123",
            "admin1234",
            "multicore",
            "multicore123",
            "qwertyuiop",
            "abcdefgh",
            "mudar123"
    );

    private PasswordPolicy() {
    }

    /**
     * Valida a senha contra os requisitos de segurança.
     *
     * @param password Senha em texto simples a validar
     * @param username Nome de utilizador associado (opcional, para prevenir senhas com o próprio utilizador)
     * @throws BusinessRuleException se a senha violar qualquer requisito de segurança
     */
    public static void validate(String password, String username) {
        if (password == null || password.isBlank()) {
            throw new BusinessRuleException("A senha é obrigatória.");
        }

        if (password.length() < MIN_LENGTH) {
            throw new BusinessRuleException(String.format(
                    "A senha deve ter pelo menos %d caracteres.", MIN_LENGTH));
        }

        boolean hasLetter = false;
        boolean hasDigit = false;

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }

        if (!hasLetter || !hasDigit) {
            throw new BusinessRuleException("A senha deve conter pelo menos uma letra e um número.");
        }

        String lowerPassword = password.toLowerCase(Locale.ROOT).trim();

        if (username != null && !username.isBlank()) {
            String lowerUsername = username.toLowerCase(Locale.ROOT).trim();
            if (lowerPassword.equals(lowerUsername) || lowerPassword.contains(lowerUsername)) {
                throw new BusinessRuleException("A senha não pode conter nem ser idêntica ao nome de utilizador.");
            }
        }

        if (COMMON_BLACKLIST.contains(lowerPassword)) {
            throw new BusinessRuleException("A senha escolhida é demasiado comum e insegura. Escolha uma senha mais forte.");
        }
    }

    /**
     * Valida a senha sem contexto de username.
     */
    public static void validate(String password) {
        validate(password, null);
    }
}
