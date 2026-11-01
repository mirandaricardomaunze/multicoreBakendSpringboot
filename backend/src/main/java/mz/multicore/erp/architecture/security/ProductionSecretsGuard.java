package mz.multicore.erp.architecture.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Locale;

/** Em produção, o servidor não arranca com segredos curtos ou deixados com o texto de exemplo. */
@Component
@Profile("prod")
public class ProductionSecretsGuard {

    private static final int MIN_SECRET_LENGTH = 16;

    public ProductionSecretsGuard(@Value("${spring.datasource.password:}") String databasePassword,
                                  @Value("${security.attachment-key:}") String attachmentKey,
                                  @Value("${app.superadmin.password:}") String superAdminPassword) {
        validate(databasePassword, attachmentKey, superAdminPassword);
    }

    static void validate(String databasePassword, String attachmentKey, String superAdminPassword) {
        requireStrong("DB_PASSWORD", databasePassword);
        if (superAdminPassword != null && !superAdminPassword.isBlank()) {
            requireStrong("SUPERADMIN_PASSWORD", superAdminPassword);
        }
        byte[] key = Base64.getDecoder().decode(attachmentKey.trim());
        if (java.util.stream.IntStream.range(0, key.length).allMatch(i -> key[i] == key[0])) {
            throw new IllegalStateException("ATTACHMENT_KEY é um valor de exemplo. Gere-a com: openssl rand -base64 32");
        }
    }

    private static void requireStrong(String name, String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        if (lower.length() < MIN_SECRET_LENGTH || lower.contains("trocar") || lower.contains("changeme")) {
            throw new IllegalStateException(name + " é fraca ou de exemplo: use pelo menos "
                    + MIN_SECRET_LENGTH + " caracteres aleatórios.");
        }
    }
}
