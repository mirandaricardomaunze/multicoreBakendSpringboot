package mz.multicore.erp.modules.fleet.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FleetManifestServiceDecompositionHarnessTest {
    private static final Path DIR = Path.of("src/main/java/mz/multicore/erp/modules/fleet/service");

    @Test
    void manifestBuildingAndQueriesStayExtractedFromFacade() throws Exception {
        String facade = Files.readString(DIR.resolve("FleetManifestService.java"));
        String builder = Files.readString(DIR.resolve("FleetManifestBuilder.java"));
        String query = Files.readString(DIR.resolve("FleetManifestQueryService.java"));

        assertTrue(facade.contains("manifestBuilder.createManifest"));
        assertTrue(facade.contains("queryService.listManifests"));
        assertTrue(facade.contains("queryService.updateStatus"));
        assertFalse(facade.contains("new FleetManifestStop()"));
        assertTrue(builder.contains("documentNumberService.next(\"MC\")"));
        assertTrue(query.contains("new FleetManifestDTO("));
        assertTrue(Files.readAllLines(DIR.resolve("FleetManifestService.java")).size() <= 75);
        assertTrue(Files.readAllLines(DIR.resolve("FleetManifestBuilder.java")).size() <= 160);
        assertTrue(Files.readAllLines(DIR.resolve("FleetManifestQueryService.java")).size() <= 90);
    }
}
