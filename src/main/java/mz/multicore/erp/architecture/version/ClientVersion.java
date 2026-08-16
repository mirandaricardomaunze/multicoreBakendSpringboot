package mz.multicore.erp.architecture.version;

import java.io.InputStream;
import java.util.Properties;

/**
 * Versão do programa a correr, resolvida <b>sem Spring</b>.
 *
 * <p>Tem de funcionar nos dois entrypoints: no backend (contexto web) e no desktop (contexto
 * não-web, que só faz scan de {@code mz.multicore.erp.desktop}/{@code mz.multicore.erp.gui} e não veria um
 * bean desta package). Daí ser um utilitário estático que lê o {@code application.properties} do
 * classpath — onde o Maven já substituiu {@code @project.version@} pela versão do {@code pom}.
 *
 * <p>Resolve-se <b>uma vez</b> e fica em memória: é lido a cada pedido HTTP.
 */
public final class ClientVersion {

    /** Marca de versão por resolver. Não é um número de versão real — é um aviso. */
    public static final String UNKNOWN = "0.0.0-dev";

    private static final String RESOLVED = resolve();

    private ClientVersion() {}

    /** Versão desta build, ou {@link #UNKNOWN} quando não foi possível resolvê-la. */
    public static String current() {
        return RESOLVED;
    }

    private static String resolve() {
        // 1) application.properties filtrado pelo Maven (o caminho normal, em dev e em produção).
        try (InputStream stream = ClientVersion.class.getResourceAsStream("/application.properties")) {
            if (stream != null) {
                Properties properties = new Properties();
                properties.load(stream);
                String version = properties.getProperty("app.version");
                // Se ainda tiver o marcador @...@, a filtragem não correu — não serve de versão.
                if (version != null && !version.isBlank() && !version.contains("@")) {
                    return version.trim();
                }
            }
        } catch (Exception ignored) {
            // Sem ficheiro ou ilegível: cai para o manifesto.
        }

        // 2) Manifesto do jar (Implementation-Version), quando empacotado.
        String fromManifest = ClientVersion.class.getPackage() == null
                ? null : ClientVersion.class.getPackage().getImplementationVersion();
        if (fromManifest != null && !fromManifest.isBlank()) {
            return fromManifest.trim();
        }

        return UNKNOWN;
    }
}
