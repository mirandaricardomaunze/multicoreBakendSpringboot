package mz.multicore.erp.modules.comercial.service;

import jakarta.persistence.Column;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.SaveClientRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IMP-CL-03 — o email do cliente passa a opcional (docs/IMPORTACAO_DADOS_SPEC.md §4.1):
 * pedido sem {@code @NotBlank}, entidade e migração sem {@code NOT NULL}, registo manual aceita vazio.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:client-optional-email;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
class ClientOptionalEmailTest {

    static {
        System.setProperty("java.awt.headless", "false");
    }

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @Autowired
    private ComercialClientAndCreditService clientService;

    @Autowired
    private CompanyRepository companyRepository;

    @BeforeAll
    static void initValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void bindContext() {
        CurrentUserContext.setCurrentUser("admin-teste", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(companyRepository.findAll().get(0).getId());
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void pedidoSemEmailOuComEmailVazioEValido() {
        assertTrue(emailViolations(null).isEmpty());
        assertTrue(emailViolations("").isEmpty());
        assertTrue(emailViolations("   ").isEmpty());
    }

    @Test
    void emailPreenchidoContinuaATerDeSerValido() {
        Set<ConstraintViolation<SaveClientRequest>> violations = emailViolations("nao-e-email");
        assertFalse(violations.isEmpty());
        assertEquals("O email deve ter um formato válido.", violations.iterator().next().getMessage());
        assertTrue(emailViolations("loja@exemplo.co.mz").isEmpty());
    }

    @Test
    void emailVazioOuSoEspacosViraNulo() {
        assertNull(new SaveClientRequest("A", "400000001", "  ", null).email());
        assertNull(new SaveClientRequest("A", "400000001", null, null).email());
        assertEquals("a@b.co.mz", new SaveClientRequest("A", "400000001", " a@b.co.mz ", null).email());
    }

    @Test
    void registoManualAceitaClienteSemEmail() {
        ClientDTO created = clientService.createClient(
                new SaveClientRequest("Cliente Sem Email", uniqueNuit(), "", "Maputo"));
        assertNull(created.email());

        ClientDTO updated = clientService.updateClient(created.id(),
                new SaveClientRequest("Cliente Sem Email", created.taxId(), "   ", "Maputo"));
        assertNull(updated.email());
    }

    @Test
    void entidadeNaoExigeEmail() throws NoSuchFieldException {
        Column column = Client.class.getDeclaredField("email").getAnnotation(Column.class);
        assertTrue(column.nullable(), "Client.email tem de ser nullable (IMP-CL-03)");
    }

    @Test
    void migracaoRetiraNotNullDoEmailDosClientes() throws IOException {
        Path migrations = Path.of("src/main/resources/db/migration");
        try (Stream<Path> files = Files.list(migrations)) {
            List<String> matches = files
                    .map(this::read)
                    .map(sql -> sql.toLowerCase(Locale.ROOT).replaceAll("\\s+", " "))
                    .filter(sql -> sql.contains("alter table clients alter column email drop not null"))
                    .toList();
            assertEquals(1, matches.size(), "uma migração Flyway tem de retirar o NOT NULL de clients.email");
        }
    }

    private Set<ConstraintViolation<SaveClientRequest>> emailViolations(String email) {
        return validator.validateProperty(
                new SaveClientRequest("Cliente", "400000001", email, null), "email");
    }

    private String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String uniqueNuit() {
        return String.format("%09d", Math.floorMod(System.nanoTime(), 1_000_000_000L));
    }
}
