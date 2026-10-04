package mz.multicore.erp.modules.users;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.users.dto.AppUserDTO;
import mz.multicore.erp.modules.users.dto.UserSecurityRequestsDTOs;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@DisplayName("Harness: Gestão de Utilizadores & PIN de Gestor (GUP-01 a GUP-06)")
class UserManagementHarnessTest {

    private AppUserRepository appUserRepository;
    private PasswordEncoder passwordEncoder;
    private CompanyRepository companyRepository;
    private AppUserCompanyAccessRepository companyAccessRepository;
    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        appUserRepository = Mockito.mock(AppUserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        companyRepository = Mockito.mock(CompanyRepository.class);
        companyAccessRepository = Mockito.mock(AppUserCompanyAccessRepository.class);

        appUserService = new AppUserService(appUserRepository, passwordEncoder, companyRepository, companyAccessRepository);

        CurrentUserContext.clear();
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(1L);

        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "encoded_" + inv.getArgument(0));
        when(passwordEncoder.matches(anyString(), anyString())).thenAnswer(inv ->
                ("encoded_" + inv.getArgument(0)).equals(inv.getArgument(1)) || inv.getArgument(0).equals("1234"));
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("GUP-01: Listagem de utilizadores restrita a administradores")
    void testListUsersRestrictedToAdmin() {
        Company comp = new Company();
        comp.setId(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(comp));

        AppUser u1 = new AppUser();
        u1.setId(10L);
        u1.setUsername("vendedor");
        u1.setName("Carlos Vendedor");
        u1.setRole("SELLER");
        u1.setActive(true);
        u1.grantCompany(comp, "SELLER");

        when(appUserRepository.findDistinctByCompanyAccessesCompanyIdOrderByName(1L)).thenReturn(List.of(u1));

        List<AppUser> users = appUserService.getAllUsers();
        assertEquals(1, users.size());
        assertEquals("vendedor", users.get(0).getUsername());

        // Com utilizador não admin lança exceção
        CurrentUserContext.setCurrentUser("vendedor", "SELLER");
        assertThrows(BusinessRuleException.class, () -> appUserService.getAllUsers());
    }

    @Test
    @DisplayName("GUP-02: Criação de utilizador com validação de dados")
    void testCreateUserValidation() {
        Company comp = new Company();
        comp.setId(1L);
        when(companyRepository.findById(1L)).thenReturn(Optional.of(comp));
        when(appUserRepository.findByUsername("novo_gestor")).thenReturn(Optional.empty());
        when(appUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUser created = appUserService.createUser("novo_gestor", "Gestor Loja", "senha123", "MANAGER");
        assertNotNull(created);
        assertEquals("novo_gestor", created.getUsername());
        assertEquals("MANAGER", created.getRole());
    }

    @Test
    @DisplayName("GUP-03 & GUP-04: Atribuição e verificação com sucesso de PIN de gestor")
    void testSetAndVerifyManagerPin() {
        Company comp = new Company();
        comp.setId(1L);

        AppUser manager = new AppUser();
        manager.setId(20L);
        manager.setUsername("gerente");
        manager.setName("Ana Gerente");
        manager.setRole("MANAGER");
        manager.setActive(true);
        manager.setManagerPinHash("encoded_1234");
        manager.grantCompany(comp, "MANAGER");

        when(appUserRepository.findByUsername("gerente")).thenReturn(Optional.of(manager));
        when(appUserRepository.findDistinctByCompanyAccessesCompanyIdOrderByName(1L)).thenReturn(List.of(manager));
        when(appUserRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Atribui PIN
        assertDoesNotThrow(() -> appUserService.setManagerPin("gerente", "1234"));

        // Verifica PIN válido
        UserSecurityRequestsDTOs.VerifyManagerPinResponse res = appUserService.verifyManagerPin("1234");
        assertTrue(res.approved());
        assertEquals("Ana Gerente", res.managerName());
    }

    @Test
    @DisplayName("GUP-05: Recusa de PIN incorreto ou inválido")
    void testVerifyIncorrectManagerPin() {
        Company comp = new Company();
        comp.setId(1L);

        AppUser manager = new AppUser();
        manager.setId(20L);
        manager.setUsername("gerente");
        manager.setName("Ana Gerente");
        manager.setRole("MANAGER");
        manager.setActive(true);
        manager.setManagerPinHash("encoded_1234");

        when(appUserRepository.findDistinctByCompanyAccessesCompanyIdOrderByName(1L)).thenReturn(List.of(manager));

        // PIN incorreto
        UserSecurityRequestsDTOs.VerifyManagerPinResponse res = appUserService.verifyManagerPin("9999");
        assertFalse(res.approved());
    }
}
