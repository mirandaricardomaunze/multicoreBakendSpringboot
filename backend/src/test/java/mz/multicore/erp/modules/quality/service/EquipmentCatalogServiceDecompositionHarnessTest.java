package mz.multicore.erp.modules.quality.service;

import mz.multicore.erp.modules.quality.dto.EquipmentCreateRequest;
import mz.multicore.erp.modules.quality.dto.EquipmentDTO;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EquipmentCatalogServiceDecompositionHarnessTest {

    @Test
    void equipmentCatalogServiceDelegatesToCollaboratorsAndRespectsLineLimits() throws Exception {
        EquipmentCatalogMutationService mutationService = mock(EquipmentCatalogMutationService.class);
        EquipmentCatalogQueryService queryService = mock(EquipmentCatalogQueryService.class);

        EquipmentCatalogService service = new EquipmentCatalogService(mutationService, queryService);

        EquipmentCreateRequest req = mock(EquipmentCreateRequest.class);
        EquipmentDTO dto = mock(EquipmentDTO.class);

        when(mutationService.createEquipment(req)).thenReturn(dto);
        assertSame(dto, service.createEquipment(req));
        verify(mutationService).createEquipment(req);

        when(queryService.listActiveEquipments(1L)).thenReturn(List.of(dto));
        assertEqualsList(service.listActiveEquipments(1L), dto);
        verify(queryService).listActiveEquipments(1L);

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/quality/service");
        int facadeLines = Files.readAllLines(base.resolve("EquipmentCatalogService.java")).size();
        int mutationLines = Files.readAllLines(base.resolve("EquipmentCatalogMutationService.java")).size();
        int queryLines = Files.readAllLines(base.resolve("EquipmentCatalogQueryService.java")).size();

        assertTrue(facadeLines <= 75, "EquipmentCatalogService deve ter <= 75 linhas, mas tem " + facadeLines);
        assertTrue(mutationLines <= 115, "EquipmentCatalogMutationService deve ter <= 115 linhas, mas tem " + mutationLines);
        assertTrue(queryLines <= 70, "EquipmentCatalogQueryService deve ter <= 70 linhas, mas tem " + queryLines);
    }

    private void assertEqualsList(List<EquipmentDTO> list, EquipmentDTO expected) {
        assertSame(expected, list.get(0));
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}
