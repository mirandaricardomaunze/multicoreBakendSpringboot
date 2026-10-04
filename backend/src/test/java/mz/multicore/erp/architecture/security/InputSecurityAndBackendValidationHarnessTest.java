package mz.multicore.erp.architecture.security;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import mz.multicore.erp.architecture.exception.GlobalExceptionHandler;
import mz.multicore.erp.architecture.validation.InputSanitizer;
import mz.multicore.erp.architecture.validation.ValidBiMZ;
import mz.multicore.erp.architecture.validation.ValidNuit;
import mz.multicore.erp.architecture.validation.ValidPhoneMZ;
import mz.multicore.erp.modules.comercial.dto.CancelReasonRequest;
import mz.multicore.erp.modules.comercial.dto.SaveClientRequest;
import mz.multicore.erp.modules.inventory.dto.CreateStockWasteRequest;
import mz.multicore.erp.modules.inventory.dto.CreateWarehouseRequest;
import mz.multicore.erp.modules.inventory.model.WasteReason;
import mz.multicore.erp.modules.platform.dto.CreateCompanyRequest;
import mz.multicore.erp.modules.purchases.dto.CreateSupplierRequest;
import mz.multicore.erp.modules.users.dto.UserSecurityRequestsDTOs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HARNESS-SEC-VAL-001: Validação de Segurança de Inputs e Validação no Backend.
 *
 * <p>Testa a integridade de todas as camadas de segurança de inputs e validações:
 * <ul>
 *   <li>SEC-01: Validação Canónica de NUIT (@ValidNuit com 9 dígitos e Módulo 11)</li>
 *   <li>SEC-02: Validação de Telefones Moçambicanos (@ValidPhoneMZ móveis e fixos)</li>
 *   <li>SEC-03: Validação de B.I. Moçambicano (@ValidBiMZ)</li>
 *   <li>SEC-04: Sanitização de Caracteres de Controlo e Prevenção de Injeção de Scripts (XSS)</li>
 *   <li>SEC-05: Prevenção de Path Traversal em Nomes de Ficheiro</li>
 *   <li>SEC-06: Endurecimento do GlobalExceptionHandler (400 Bad Request sem fuga de detalhes internos/SQL)</li>
 *   <li>SEC-07: Validação Declarativa dos DTOs de Contrato (Clientes, Fornecedores, Quebras, Armazéns, Utilizadores)</li>
 * </ul>
 */
public class InputSecurityAndBackendValidationHarnessTest {

    private static Validator validator;
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // Record de teste para exercitar anotações canónicas directamente
    private record SampleRecord(
            @ValidNuit String standardNuit,
            @ValidNuit(checkModulo11 = true) String strictNuit,
            @ValidPhoneMZ String phone,
            @ValidBiMZ String bi
    ) {}

    @Test
    @DisplayName("SEC-01: @ValidNuit aceita NUITs válidos e rejeita não-numéricos ou tamanhos errados")
    void testValidNuitAnnotation() {
        // Válido: 9 dígitos padrão e nulo/em branco (não bloqueia ausência sem @NotBlank)
        SampleRecord validSample = new SampleRecord("400123456", "999999999", "841234567", "110100234567B");
        Set<ConstraintViolation<SampleRecord>> violations = validator.validate(validSample);
        assertTrue(violations.isEmpty(), "NUITs válidos não devem ter violações: " + violations);

        // Inválido: letras no NUIT
        SampleRecord invalidLetter = new SampleRecord("12345678A", null, null, null);
        Set<ConstraintViolation<SampleRecord>> letterViolations = validator.validate(invalidLetter);
        assertEquals(1, letterViolations.size());
        assertTrue(letterViolations.iterator().next().getMessage().contains("NUIT moçambicano inválido"));

        // Inválido: comprimento menor que 9
        SampleRecord invalidShort = new SampleRecord("12345678", null, null, null);
        assertEquals(1, validator.validate(invalidShort).size());

        // Inválido: comprimento maior que 9
        SampleRecord invalidLong = new SampleRecord("1234567890", null, null, null);
        assertEquals(1, validator.validate(invalidLong).size());

        // Strict: Módulo 11 da AT (100123457 é válido pela fórmula; 999888777 tem dígito de controlo 3 != 7)
        SampleRecord strictValid = new SampleRecord(null, "100123457", null, null);
        assertTrue(validator.validate(strictValid).isEmpty());

        SampleRecord strictInvalid = new SampleRecord(null, "999888777", null, null);
        assertEquals(1, validator.validate(strictInvalid).size());
    }

    @Test
    @DisplayName("SEC-02: @ValidPhoneMZ valida redes móveis (82-87) e fixas nacionais (21-28)")
    void testValidPhoneMZAnnotation() {
        // Redes móveis moçambicanas válidas
        assertTrue(validator.validate(new SampleRecord(null, null, "841234567", null)).isEmpty()); // Vodacom
        assertTrue(validator.validate(new SampleRecord(null, null, "821234567", null)).isEmpty()); // Tmcel
        assertTrue(validator.validate(new SampleRecord(null, null, "861234567", null)).isEmpty()); // Movitel
        assertTrue(validator.validate(new SampleRecord(null, null, "+258 84 123 4567", null)).isEmpty()); // Internacional
        assertTrue(validator.validate(new SampleRecord(null, null, "21123456", null)).isEmpty()); // Linha fixa Maputo

        // Inválido: operadora inexistente (81...)
        assertEquals(1, validator.validate(new SampleRecord(null, null, "811234567", null)).size());
        // Inválido: número curto
        assertEquals(1, validator.validate(new SampleRecord(null, null, "84123", null)).size());
        // Inválido: caracteres alfabéticos
        assertEquals(1, validator.validate(new SampleRecord(null, null, "84123456A", null)).size());
    }

    @Test
    @DisplayName("SEC-03: @ValidBiMZ valida Bilhete de Identidade moçambicano (12 dígitos + 1 letra maiúscula)")
    void testValidBiMZAnnotation() {
        // Válido
        assertTrue(validator.validate(new SampleRecord(null, null, null, "110100234567B")).isEmpty());

        // Inválido: apenas números (sem letra de controlo)
        assertEquals(1, validator.validate(new SampleRecord(null, null, null, "1101002345678")).size());
        // Inválido: carácter especial no lugar da letra
        assertEquals(1, validator.validate(new SampleRecord(null, null, null, "110100234567@")).size());
        // Inválido: tamanho insuficiente
        assertEquals(1, validator.validate(new SampleRecord(null, null, null, "123B")).size());
    }

    @Test
    @DisplayName("SEC-04: InputSanitizer elimina bytes de controlo e script tags preservando UTF-8 e acentos")
    void testInputSanitizerTextAndScript() {
        // Bytes de controlo (null byte \0, \x01, \x1F)
        String dirtyWithControl = "Artigo Especial\u0000 com \u0001bytes\u001F ocultos";
        String clean = InputSanitizer.stripControlCharacters(dirtyWithControl);
        assertEquals("Artigo Especial com bytes ocultos", clean);

        // Preservação de caracteres legítimos em português moçambicano
        String mozambicanText = "Cotação & Facturação: Meticais (MZN) em Maputo — Café & Chá.";
        assertEquals(mozambicanText, InputSanitizer.stripControlCharacters(mozambicanText));

        // Sanitização de notas com injeção de script
        String maliciousNotes = "<script>alert('pwned')</script>Entrega efectuada no armazém central.";
        String safeNotes = InputSanitizer.sanitizeNotes(maliciousNotes, 500);
        assertFalse(safeNotes.contains("<script>"));
        assertFalse(safeNotes.contains("</script>"));
        assertTrue(safeNotes.contains("Entrega efectuada no armazém central."));

        // Sanitização e limitação de queries de pesquisa
        String longSearch = "  pesquisa de produto super longa que excede o limite estabelecido para teste  ";
        String safeSearch = InputSanitizer.sanitizeSearchQuery(longSearch, 20);
        assertTrue(safeSearch.length() <= 20);
        assertEquals("pesquisa de produto", safeSearch);

        // Sanitização automática no DTO CancelReasonRequest
        CancelReasonRequest cancelReq = new CancelReasonRequest("<script>alert('hack')</script>Cancelamento solicitado pelo cliente\u0000");
        assertFalse(cancelReq.reason().contains("<script>"));
        assertFalse(cancelReq.reason().contains("\u0000"));
        assertTrue(cancelReq.reason().contains("Cancelamento solicitado pelo cliente"));
    }

    @Test
    @DisplayName("SEC-05: InputSanitizer previne Path Traversal em nomes de ficheiro")
    void testInputSanitizerPathTraversal() {
        // Tentativas clássicas de directory traversal
        String traversal1 = "../../etc/passwd";
        String safe1 = InputSanitizer.sanitizeFileName(traversal1);
        assertFalse(safe1.contains(".."));
        assertFalse(safe1.contains("/"));
        assertFalse(safe1.contains("\\"));

        String traversal2 = "..\\..\\windows\\system32\\cmd.exe";
        String safe2 = InputSanitizer.sanitizeFileName(traversal2);
        assertFalse(safe2.contains(".."));
        assertFalse(safe2.contains("\\"));

        // Entrada nula ou em branco retorna padrão seguro
        assertEquals("documento", InputSanitizer.sanitizeFileName(null));
        assertEquals("documento", InputSanitizer.sanitizeFileName("   "));
        assertEquals("documento", InputSanitizer.sanitizeFileName("../"));
    }

    @Test
    @DisplayName("SEC-06: GlobalExceptionHandler intercepta erros de validação e integridade sem vazar SQL")
    void testGlobalExceptionHandlerHardening() throws Exception {
        // 1. MethodArgumentNotValidException (Validação de DTOs)
        SampleRecord target = new SampleRecord("invalid", null, null, null);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "sampleRecord");
        bindingResult.addError(new FieldError("sampleRecord", "standardNuit", "NUIT moçambicano inválido."));
        MethodParameter parameter = new MethodParameter(
                InputSecurityAndBackendValidationHarnessTest.class.getDeclaredMethod("dummyMethod", SampleRecord.class), 0);
        MethodArgumentNotValidException valEx = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> valResponse = exceptionHandler.handleValidation(valEx);
        assertEquals(HttpStatus.BAD_REQUEST, valResponse.getStatusCode());
        assertEquals(400, valResponse.getBody().status());
        assertEquals("Validation Error", valResponse.getBody().error());
        assertTrue(valResponse.getBody().message().contains("standardNuit"));

        // 2. DataIntegrityViolationException (Violação de FK ou Unique no DB)
        DataIntegrityViolationException dbEx = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"idx_clients_company_nuit\"\n" +
                "Detail: Key (company_id, tax_id)=(1, 400123456) already exists.");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> dbResponse = exceptionHandler.handleDataIntegrity(dbEx);
        assertEquals(HttpStatus.BAD_REQUEST, dbResponse.getStatusCode());
        assertEquals(400, dbResponse.getBody().status());
        assertEquals("Data Integrity Violation", dbResponse.getBody().error());
        // Garante que internals de SQL e nomes de constraints NÃO vazam para a resposta HTTP
        assertFalse(dbResponse.getBody().message().contains("idx_clients_company_nuit"));
        assertFalse(dbResponse.getBody().message().contains("ERROR: duplicate key"));
        assertTrue(dbResponse.getBody().message().contains("integridade de dados"));

        // 3. IllegalArgumentException
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> argResponse =
                exceptionHandler.handleIllegalArgument(new IllegalArgumentException("Valor de quantidade inválido."));
        assertEquals(HttpStatus.BAD_REQUEST, argResponse.getStatusCode());
        assertEquals("Valor de quantidade inválido.", argResponse.getBody().message());
    }

    @Test
    @DisplayName("SEC-07: DTOs do sistema validam anotações canónicas e rejeitam entradas malformadas")
    void testSystemDtoValidations() {
        // 1. SaveClientRequest: NUIT inválido rejeitado
        SaveClientRequest badClient = new SaveClientRequest("Cliente Teste", "123", "valido@empresa.co.mz", "Maputo");
        Set<ConstraintViolation<SaveClientRequest>> clientViolations = validator.validate(badClient);
        assertFalse(clientViolations.isEmpty());
        assertTrue(clientViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("taxId")));

        // 2. CreateSupplierRequest: NUIT e telefone validados
        CreateSupplierRequest badSupplier = new CreateSupplierRequest("Fornecedor", "400123456", "fornecedor@email.co.mz",
                "Av. 24 Julho", "810000000", "Contacto", 1L); // 81 não é prefixo MZ válido
        Set<ConstraintViolation<CreateSupplierRequest>> supplierViolations = validator.validate(badSupplier);
        assertFalse(supplierViolations.isEmpty());
        assertTrue(supplierViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("phone")));

        // 3. CreateStockWasteRequest: quantidade negativa e campos nulos rejeitados
        CreateStockWasteRequest badWaste = new CreateStockWasteRequest(1L, 1L, 1L, null, BigDecimal.valueOf(-10), WasteReason.EXPIRED, "Observações");
        Set<ConstraintViolation<CreateStockWasteRequest>> wasteViolations = validator.validate(badWaste);
        assertFalse(wasteViolations.isEmpty());
        assertTrue(wasteViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));

        // 4. CreateWarehouseRequest: telefone inválido rejeitado
        CreateWarehouseRequest badWarehouse = new CreateWarehouseRequest("Armazém A", "ARM-01", BigDecimal.TEN, "Matola", 1L, null, true, "Gestor", "12345");
        Set<ConstraintViolation<CreateWarehouseRequest>> whViolations = validator.validate(badWarehouse);
        assertFalse(whViolations.isEmpty());
        assertTrue(whViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("phone")));

        // 5. CreateCompanyRequest: NUIT inválido rejeitado
        CreateCompanyRequest badCompany = new CreateCompanyRequest("Empresa", "123ABC456", "empresa@email.co.mz", "Maputo", "+258 84 123 4567");
        Set<ConstraintViolation<CreateCompanyRequest>> compViolations = validator.validate(badCompany);
        assertFalse(compViolations.isEmpty());
        assertTrue(compViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("taxId")));

        // 6. UserSecurityRequestsDTOs.SetManagerPinRequest: PIN curto rejeitado
        UserSecurityRequestsDTOs.SetManagerPinRequest badPin = new UserSecurityRequestsDTOs.SetManagerPinRequest("12");
        Set<ConstraintViolation<UserSecurityRequestsDTOs.SetManagerPinRequest>> pinViolations = validator.validate(badPin);
        assertFalse(pinViolations.isEmpty());
        assertTrue(pinViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("pin")));
    }

    // Método auxiliar apenas para obter MethodParameter nos testes
    private void dummyMethod(SampleRecord record) {}
}
