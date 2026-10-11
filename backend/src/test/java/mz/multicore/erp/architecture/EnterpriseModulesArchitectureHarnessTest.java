package mz.multicore.erp.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness arquitectural dos módulos de expansão empresarial (Tier-1):
 * - Activos Fixos & Património (assets)
 * - Produção & Transformação / Ficha Técnica BOM (manufacturing)
 * - Gestão de Frotas & Combustível (fleet)
 * - Contratos & Facturação Recorrente (recurring)
 *
 * Valida conformidade das regras SOLID, DRY, separação estrita de camadas e contratos imutáveis.
 */
class EnterpriseModulesArchitectureHarnessTest {

    private static final Path ROOT = Files.isDirectory(Path.of("contracts"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();

    private static final List<String> EXPANSION_MODULES = List.of(
            "assets",
            "manufacturing",
            "fleet",
            "recurring"
    );

    @Test
    @DisplayName("Todos os módulos de expansão possuem especificação física em contracts")
    void allEnterpriseModulesHaveContractsPackages() {
        Path contractsBase = ROOT.resolve(Path.of("contracts", "src", "main", "java", "mz", "multicore", "erp", "modules"));
        assertThat(contractsBase).isDirectory();

        for (String module : EXPANSION_MODULES) {
            Path modDir = contractsBase.resolve(module);
            assertThat(modDir)
                    .as("O módulo de contratos %s deve existir em contracts/src/main/java/.../modules/%s", module, module)
                    .isDirectory();
            assertThat(modDir.resolve("dto"))
                    .as("O módulo %s deve possuir pacote dto", module)
                    .isDirectory();
            assertThat(modDir.resolve("model"))
                    .as("O módulo %s deve possuir pacote model (enums e value objects de contrato)", module)
                    .isDirectory();
        }
    }

    @Test
    @DisplayName("DTOs dos módulos de expansão devem ser estritamente Records imutáveis (sem setters ou estado mutável)")
    void allExpansionDtosAreImmutableRecords() throws ClassNotFoundException {
        List<Class<?>> dtos = List.of(
                mz.multicore.erp.modules.assets.dto.AssetDTO.class,
                mz.multicore.erp.modules.assets.dto.CreateAssetRequest.class,
                mz.multicore.erp.modules.assets.dto.DepreciationScheduleDTO.class,
                mz.multicore.erp.modules.manufacturing.dto.BillOfMaterialsDTO.class,
                mz.multicore.erp.modules.manufacturing.dto.BomItemDTO.class,
                mz.multicore.erp.modules.manufacturing.dto.CreateBomRequest.class,
                mz.multicore.erp.modules.manufacturing.dto.ProductionOrderDTO.class,
                mz.multicore.erp.modules.manufacturing.dto.CreateProductionOrderRequest.class,
                mz.multicore.erp.modules.fleet.dto.VehicleDTO.class,
                mz.multicore.erp.modules.fleet.dto.CreateVehicleRequest.class,
                mz.multicore.erp.modules.fleet.dto.FuelLogDTO.class,
                mz.multicore.erp.modules.fleet.dto.RecordFuelLogRequest.class,
                mz.multicore.erp.modules.recurring.dto.RecurringContractDTO.class,
                mz.multicore.erp.modules.recurring.dto.CreateRecurringContractRequest.class,
                mz.multicore.erp.modules.recurring.dto.BatchBillingResultDTO.class
        );

        for (Class<?> dto : dtos) {
            assertThat(dto.isRecord())
                    .as("O DTO %s deve ser estritamente um Record imutável", dto.getSimpleName())
                    .isTrue();
            assertThat(Modifier.isFinal(dto.getModifiers()))
                    .as("O DTO %s deve ser final", dto.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    @DisplayName("Contracts dos novos módulos permanecem 100% livres de Spring, JPA, Hibernate ou Swing")
    void expansionContractsRemainCompletelyFrameworkFree() throws IOException {
        Path contractsBase = ROOT.resolve(Path.of("contracts", "src", "main", "java", "mz", "multicore", "erp", "modules"));

        for (String module : EXPANSION_MODULES) {
            Path modDir = contractsBase.resolve(module);
            try (var stream = Files.walk(modDir)) {
                List<Path> files = stream.filter(p -> p.toString().endsWith(".java")).toList();
                for (Path file : files) {
                    String content = Files.readString(file);
                    assertThat(content)
                            .as("Ficheiro %s não pode importar JPA/Hibernate", file.getFileName())
                            .doesNotContain("jakarta.persistence", "javax.persistence", "org.hibernate");
                    assertThat(content)
                            .as("Ficheiro %s não pode importar Spring Framework", file.getFileName())
                            .doesNotContain("org.springframework.");
                    assertThat(content)
                            .as("Ficheiro %s não pode importar Swing ou AWT", file.getFileName())
                            .doesNotContain("javax.swing", "java.awt");
                }
            }
        }
    }

    @Test
    @DisplayName("Valores financeiros e quantidades nos DTOs usam estritamente BigDecimal (nunca double ou float)")
    void financialFieldsNeverUseFloatingPointNumbers() throws IOException {
        Path contractsBase = ROOT.resolve(Path.of("contracts", "src", "main", "java", "mz", "multicore", "erp", "modules"));

        for (String module : EXPANSION_MODULES) {
            Path modDir = contractsBase.resolve(module).resolve("dto");
            try (var stream = Files.walk(modDir)) {
                List<Path> files = stream.filter(p -> p.toString().endsWith(".java")).toList();
                for (Path file : files) {
                    String content = Files.readString(file);
                    // Proíbe campos de dinheiro/quantidade do tipo double/float nos DTOs de dados
                    assertThat(content)
                            .as("DTO %s não pode usar float nos campos de dados", file.getFileName())
                            .doesNotMatch("(?s).*\\bfloat\\s+[a-zA-Z0-9_]+.*");
                    assertThat(content)
                            .as("DTO %s deve usar BigDecimal em vez de double nos campos monetários ou quantidades", file.getFileName())
                            .doesNotMatch("(?s).*\\bdouble\\s+(cost|price|amount|quantity|total|residual|depreciation|odometer|liters).*");
                }
            }
        }
    }

    @Test
    @DisplayName("Documento de especificação canónica existe e define a arquitectura completa dos 4 módulos")
    void enterpriseExpansionSpecFileExistsAndCoversAllModules() throws IOException {
        Path specFile = ROOT.resolve(Path.of("docs", "ENTERPRISE_MODULES_EXPANSION_SPEC.md"));
        assertThat(specFile).exists();
        String specContent = Files.readString(specFile);
        assertThat(specContent)
                .contains("Activos Fixos & Património")
                .contains("Produção, Transformação & Ficha Técnica")
                .contains("Gestão de Frotas, Combustível & Viaturas")
                .contains("Contratos & Facturação Recorrente")
                .contains("PGC-NIRF")
                .contains("Bill of Materials")
                .contains("UI_DESIGN_SYSTEM.md");
    }
}
