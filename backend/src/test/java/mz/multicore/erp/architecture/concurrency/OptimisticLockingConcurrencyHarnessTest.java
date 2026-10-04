package mz.multicore.erp.architecture.concurrency;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Version;
import mz.multicore.erp.architecture.exception.GlobalExceptionHandler;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;
import mz.multicore.erp.modules.purchases.model.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness canónico para SPEC-CONC-001 (HARNESS-CONC-001):
 * Controlo Estrito de Concorrência e Bloqueio Otimista Transversal.
 */
class OptimisticLockingConcurrencyHarnessTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("CONC-01: GlobalExceptionHandler mapeia OptimisticLock para HTTP 409 Conflict amigável")
    void testConc01_GlobalExceptionHandlerOptimisticLock() {
        ObjectOptimisticLockingFailureException springEx =
                new ObjectOptimisticLockingFailureException(Client.class, 101L);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp1 = exceptionHandler.handleOptimisticLock(springEx);
        assertNotNull(resp1);
        assertEquals(HttpStatus.CONFLICT, resp1.getStatusCode());
        assertEquals(409, resp1.getBody().status());
        assertEquals("Conflict", resp1.getBody().error());
        assertTrue(resp1.getBody().message().contains("concorrentemente por outro utilizador"));

        OptimisticLockException jpaEx = new OptimisticLockException("Conflito JPA");
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> resp2 = exceptionHandler.handleOptimisticLock(jpaEx);
        assertNotNull(resp2);
        assertEquals(HttpStatus.CONFLICT, resp2.getStatusCode());
        assertEquals(409, resp2.getBody().status());
    }

    @Test
    @DisplayName("CONC-02: Entidade Client possui campo version anotado com @Version")
    void testConc02_ClientOptimisticLocking() throws NoSuchFieldException {
        Field versionField = Client.class.getDeclaredField("version");
        assertNotNull(versionField);
        assertTrue(versionField.isAnnotationPresent(Version.class),
                "O campo version em Client deve estar anotado com @jakarta.persistence.Version");
        assertEquals(Long.class, versionField.getType());

        Client client = new Client();
        client.setVersion(5L);
        assertEquals(5L, client.getVersion());
    }

    @Test
    @DisplayName("CONC-03: Entidade Product possui campo version anotado com @Version")
    void testConc03_ProductOptimisticLocking() throws NoSuchFieldException {
        Field versionField = Product.class.getDeclaredField("version");
        assertNotNull(versionField);
        assertTrue(versionField.isAnnotationPresent(Version.class),
                "O campo version em Product deve estar anotado com @jakarta.persistence.Version");
        assertEquals(Long.class, versionField.getType());

        Product product = new Product();
        product.setVersion(2L);
        assertEquals(2L, product.getVersion());
    }

    @Test
    @DisplayName("CONC-04: Entidade Supplier possui campo version anotado com @Version")
    void testConc04_SupplierOptimisticLocking() throws NoSuchFieldException {
        Field versionField = Supplier.class.getDeclaredField("version");
        assertNotNull(versionField);
        assertTrue(versionField.isAnnotationPresent(Version.class),
                "O campo version em Supplier deve estar anotado com @jakarta.persistence.Version");
        assertEquals(Long.class, versionField.getType());

        Supplier supplier = new Supplier();
        supplier.setVersion(3L);
        assertEquals(3L, supplier.getVersion());
    }

    @Test
    @DisplayName("CONC-05: Retrocompatibilidade de DTOs e mapeamento do campo version")
    void testConc05_DtoBackwardsCompatibilityAndVersionMapping() {
        // 1. ClientDTO
        ClientDTO clientFull = new ClientDTO(
                1L, "Cliente A", "123456789", "a@a.mz", "Maputo", 30,
                new BigDecimal("5000.00"), BigDecimal.ZERO, "COD1", 4L
        );
        assertEquals(4L, clientFull.version());

        ClientDTO clientLegacy = new ClientDTO(
                1L, "Cliente A", "123456789", "a@a.mz", "Maputo", 30, new BigDecimal("5000.00")
        );
        assertEquals(0L, clientLegacy.version());

        // 2. ProductDTO
        ProductDTO productFull = new ProductDTO(
                10L, "SKU1", "REF1", "BAR1", "Produto 1",
                new BigDecimal("100.00"), new BigDecimal("60.00"), new BigDecimal("5.00"),
                null, null, 1, 1, 1, "UNIT", true,
                1L, "Cat", 1L, new BigDecimal("16.00"), "IVA", "Desc",
                null, BigDecimal.ONE, BigDecimal.ONE, 7L
        );
        assertEquals(7L, productFull.version());

        ProductDTO productLegacy = new ProductDTO(
                10L, "SKU1", "REF1", "BAR1", "Produto 1",
                new BigDecimal("100.00"), new BigDecimal("60.00"), new BigDecimal("5.00"),
                null, null, 1, "UNIT", true,
                1L, "Cat", 1L, new BigDecimal("16.00"), "IVA", "Desc",
                null, BigDecimal.ONE, BigDecimal.ONE
        );
        assertEquals(0L, productLegacy.version());

        // 3. SupplierDTO
        SupplierDTO supplierFull = new SupplierDTO(
                20L, "Fornecedor X", "987654321", "x@x.mz", "Matola", "841234567",
                "Gestor X", true, 1L, 9L
        );
        assertEquals(9L, supplierFull.version());

        SupplierDTO supplierLegacy = new SupplierDTO(
                20L, "Fornecedor X", "987654321", "x@x.mz", "Matola", "841234567",
                "Gestor X", true, 1L
        );
        assertEquals(0L, supplierLegacy.version());
    }
}
