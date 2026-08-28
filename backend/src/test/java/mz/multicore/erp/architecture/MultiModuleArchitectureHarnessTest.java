package mz.multicore.erp.architecture;

import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness permanente do SPEC docs/MULTI_MODULE_ARCHITECTURE_SPEC.md. */
class MultiModuleArchitectureHarnessTest {
    private static final List<String> MODULES = List.of("contracts", "backend", "desktop");
    private static final Path ROOT = Files.isDirectory(Path.of("contracts"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();

    @Test
    void reactorHasTheThreePhysicalModules() throws Exception {
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ROOT.resolve("pom.xml").toFile());
        var nodes = document.getElementsByTagName("module");
        Set<String> modules = java.util.stream.IntStream.range(0, nodes.getLength())
                .mapToObj(index -> nodes.item(index).getTextContent().trim()).collect(Collectors.toSet());
        assertThat(modules).containsExactlyInAnyOrderElementsOf(MODULES);
        assertThat(document.getElementsByTagName("packaging").item(0).getTextContent()).isEqualTo("pom");
        for (String module : MODULES) {
            assertThat(ROOT.resolve(Path.of(module, "pom.xml"))).exists();
            assertThat(ROOT.resolve(Path.of(module, "src", "main", "java"))).isDirectory();
        }
    }

    @Test
    void desktopHasNoBackendOrDatabaseImports() throws Exception {
        assertSourcesDoNotContain(ROOT.resolve(Path.of("desktop", "src", "main", "java")), List.of(
                "jakarta.persistence", "javax.sql.DataSource", "org.springframework.data",
                "org.flywaydb", ".repository.", ".service."));
        String pom = Files.readString(ROOT.resolve(Path.of("desktop", "pom.xml")));
        assertThat(pom).doesNotContain("spring-boot-starter-data-jpa", "postgresql", "flyway-core", "h2");
        assertThat(pom).doesNotContain("<artifactId>multicore-backend</artifactId>");
    }

    @Test
    void backendIsHeadlessAndDoesNotDependOnDesktop() throws Exception {
        assertSourcesDoNotContain(ROOT.resolve(Path.of("backend", "src", "main", "java")), List.of(
                "javax.swing", "mz.multicore.erp.gui", "mz.multicore.erp.desktop"));
        String pom = Files.readString(ROOT.resolve(Path.of("backend", "pom.xml")));
        assertThat(pom).doesNotContain("ikonli-swing", "<artifactId>multicore-desktop</artifactId>");
    }

    /**
     * <b>A autorização não viaja no artefacto que se instala nas máquinas dos clientes.</b>
     *
     * <p>O {@code PermissionGuard} chegou a viver em {@code contracts}, e compilava: 30 ficheiros do
     * backend usavam-no para <i>impor</i> e 3 do desktop para decidir se desenhavam um botão. A
     * mesma classe com dois níveis de confiança convida ao erro que não se vê — "verificar a
     * permissão" no cliente e acreditar que se fez alguma coisa. O cliente decide o que desenha
     * ({@code SignedInUser}); o servidor decide o que é permitido.
     */
    @Test
    void authorisationGuardLivesOnlyInTheBackend() throws Exception {
        assertThat(ROOT.resolve(Path.of("backend", "src", "main", "java", "mz", "multicore", "erp",
                "architecture", "security", "PermissionGuard.java")))
                .as("o PermissionGuard pertence ao backend")
                .exists();
        assertThat(ROOT.resolve(Path.of("contracts", "src", "main", "java", "mz", "multicore", "erp",
                "architecture", "security", "PermissionGuard.java")))
                .as("o PermissionGuard não pode voltar a contracts — vai no jar de cada cliente")
                .doesNotExist();
        assertSourcesDoNotContain(ROOT.resolve(Path.of("desktop", "src", "main", "java")),
                List.of("PermissionGuard.requireManagerOrAdmin", "PermissionGuard.isManagerOrAdmin"));
    }

    @Test
    void contractsRemainFrameworkFree() throws Exception {
        assertSourcesDoNotContain(ROOT.resolve(Path.of("contracts", "src", "main", "java")), List.of(
                "org.springframework", "jakarta.persistence", "javax.swing", "java.awt",
                ".repository.", ".service.", "@Entity"));
    }

    private void assertSourcesDoNotContain(Path root, List<String> forbidden) throws Exception {
        assertThat(root).isDirectory();
        try (var paths = Files.walk(root)) {
            List<Path> sources = paths.filter(path -> path.toString().endsWith(".java")).toList();
            assertThat(sources).isNotEmpty();
            for (Path source : sources) {
                String text = Files.readString(source);
                for (String token : forbidden) {
                    assertThat(text).as("%s não pode conter %s", source, token).doesNotContain(token);
                }
            }
        }
    }
}
