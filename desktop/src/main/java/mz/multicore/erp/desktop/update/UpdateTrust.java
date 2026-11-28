package mz.multicore.erp.desktop.update;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Signer the installer must carry, fixed at build time in {@code update/trust.properties} inside the jar. Blank
 * until the code-signing certificate exists; then every update needs a valid signature from it (spec §2).
 */
public final class UpdateTrust {

    static final String RESOURCE = "/update/trust.properties";

    private UpdateTrust() {
    }

    public static String expectedSigner() {
        try (InputStream in = UpdateTrust.class.getResourceAsStream(RESOURCE)) {
            if (in == null) return "";
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("signer", "").trim();
        } catch (IOException error) {
            // An unreadable trust file must not silently relax the rule: refuse everything instead.
            throw new UpdateException("Não foi possível ler a política de confiança das actualizações.", error);
        }
    }
}
