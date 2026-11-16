package mz.multicore.erp.architecture.version;

import java.io.InputStream;
import java.util.Properties;

/**
 * Resolves the running application version <b>without Spring</b>.
 *
 * <p>Must work from both entry points: the backend web context and the non-web desktop context,
 * which scans only {@code mz.multicore.erp.desktop}/{@code mz.multicore.erp.gui} and would not find
 * a bean in this package. It is therefore a static utility that reads {@code application.properties}
 * from the classpath, where Maven has replaced {@code @project.version@} with the {@code pom} version.
 *
 * <p>Resolved <b>once</b> and retained in memory because it is read on every HTTP request.
 */
public final class ClientVersion {

    /** Marker for an unresolved version. This warning is not a real version number. */
    public static final String UNKNOWN = "0.0.0-dev";

    private static final String RESOLVED = resolve();

    private ClientVersion() {}

    /** Version of this build, or {@link #UNKNOWN} when it cannot be resolved. */
    public static String current() {
        return RESOLVED;
    }

    private static String resolve() {
        // 1) Maven-filtered application.properties, the normal path in development and production.
        try (InputStream stream = ClientVersion.class.getResourceAsStream("/application.properties")) {
            if (stream != null) {
                Properties properties = new Properties();
                properties.load(stream);
                String version = properties.getProperty("app.version");
                // A remaining @...@ marker means filtering did not run, so the value is not a version.
                if (version != null && !version.isBlank() && !version.contains("@")) {
                    return version.trim();
                }
            }
        } catch (Exception ignored) {
            // Fall back to the manifest when the file is missing or unreadable.
        }

        // 2) Packaged JAR manifest (Implementation-Version).
        String fromManifest = ClientVersion.class.getPackage() == null
                ? null : ClientVersion.class.getPackage().getImplementationVersion();
        if (fromManifest != null && !fromManifest.isBlank()) {
            return fromManifest.trim();
        }

        return UNKNOWN;
    }
}
