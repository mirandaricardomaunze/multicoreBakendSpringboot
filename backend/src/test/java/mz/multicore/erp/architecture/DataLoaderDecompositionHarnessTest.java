package mz.multicore.erp.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DataLoaderDecompositionHarnessTest {

    @Test
    void dataLoaderDelegatesToReferenceAndDemoSeedersAndRespectsLineLimits() throws Exception {
        ReferenceAndAdminDataSeeder referenceSeeder = mock(ReferenceAndAdminDataSeeder.class);
        DemoEnterpriseDataSeeder demoSeeder = mock(DemoEnterpriseDataSeeder.class);

        DataLoader prodLoader = new DataLoader(false, referenceSeeder, demoSeeder);
        prodLoader.run();
        verify(referenceSeeder).seedReferenceAndSuperAdmin();
        verify(demoSeeder, never()).seedDemoEnvironment();

        DataLoader demoLoader = new DataLoader(true, referenceSeeder, demoSeeder);
        demoLoader.run();
        verify(demoSeeder).seedDemoEnvironment();

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/architecture");
        int loaderLines = Files.readAllLines(base.resolve("DataLoader.java")).size();
        int referenceLines = Files.readAllLines(base.resolve("ReferenceAndAdminDataSeeder.java")).size();
        int demoLines = Files.readAllLines(base.resolve("DemoEnterpriseDataSeeder.java")).size();

        assertTrue(loaderLines <= 115, "DataLoader deve ter <= 115 linhas, mas tem " + loaderLines);
        assertTrue(referenceLines <= 150, "ReferenceAndAdminDataSeeder deve ter <= 150 linhas, mas tem " + referenceLines);
        assertTrue(demoLines <= 340, "DemoEnterpriseDataSeeder deve ter <= 340 linhas, mas tem " + demoLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}
