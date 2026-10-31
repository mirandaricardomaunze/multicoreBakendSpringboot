package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.modules.comercial.model.Product;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BusinessUnitServiceDecompositionHarnessTest {

    @Test
    void businessUnitServiceDelegatesToCollaboratorsAndRespectsLineLimits() throws Exception {
        BusinessUnitMutationService mutationService = mock(BusinessUnitMutationService.class);
        BusinessUnitQueryAndPolicyService queryAndPolicyService = mock(BusinessUnitQueryAndPolicyService.class);

        BusinessUnitService service = new BusinessUnitService(mutationService, queryAndPolicyService);

        Product product = new Product();
        service.validateProductRestriction(3L, product);
        verify(queryAndPolicyService).validateProductRestriction(3L, product);

        Path base = findModuleRoot().resolve("src/main/java/mz/multicore/erp/modules/purchases/service");
        int facadeLines = Files.readAllLines(base.resolve("BusinessUnitService.java")).size();
        int mutationLines = Files.readAllLines(base.resolve("BusinessUnitMutationService.java")).size();
        int queryLines = Files.readAllLines(base.resolve("BusinessUnitQueryAndPolicyService.java")).size();

        assertTrue(facadeLines <= 85, "BusinessUnitService deve ter <= 85 linhas, mas tem " + facadeLines);
        assertTrue(mutationLines <= 110, "BusinessUnitMutationService deve ter <= 110 linhas, mas tem " + mutationLines);
        assertTrue(queryLines <= 80, "BusinessUnitQueryAndPolicyService deve ter <= 80 linhas, mas tem " + queryLines);
    }

    private Path findModuleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        if (cwd.endsWith("backend")) {
            return cwd;
        }
        return cwd.resolve("backend");
    }
}
