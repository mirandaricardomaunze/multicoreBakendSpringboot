package mz.multicore.erp.modules.licensing;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness estrutural do contrato docs/LICENCA_UTILIZADOR_SPEC.md. */
class LicenseAcceptanceHarnessTest {

    private static final Path ROOT = Files.isDirectory(Path.of("backend"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();

    @Test
    void possuiMigrationRecursoContratoEInstalador() throws Exception {
        assertThat(ROOT.resolve("backend/src/main/resources/db/migration/V60__license_acceptances.sql")).exists();
        assertThat(ROOT.resolve("backend/src/main/resources/legal/EULA-1.0.txt")).exists();
        assertThat(ROOT.resolve("installer/LICENSE.txt")).exists();
        assertThat(ROOT.resolve("scripts/build-windows-installer.ps1")).exists();
        assertThat(Files.readString(ROOT.resolve("installer/LICENSE.txt")))
                .isEqualTo(Files.readString(ROOT.resolve("backend/src/main/resources/legal/EULA-1.0.txt")));
    }

    @Test
    void desktopContinuaSemPersistenciaDoBackend() throws Exception {
        String pom = Files.readString(ROOT.resolve("desktop/pom.xml"));
        assertThat(pom).doesNotContain("spring-boot-starter-data-jpa", "postgresql", "flyway-core");
    }
}
