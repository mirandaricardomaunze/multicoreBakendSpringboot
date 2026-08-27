package mz.multicore.erp.modules.platform.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.platform.dto.PlatformUserDTO;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes do {@link PlatformUserService}: exige SUPERADMIN, activa/desactiva, revoga acesso
 * (protegendo o último ADMIN) e não desactiva o superadmin. Dependências mockadas.
 */
class PlatformUserServiceTest {

    private AppUserRepository appUserRepository;
    private AppUserCompanyAccessRepository companyAccessRepository;
    private CompanyRepository companyRepository;
    private PlatformUserService service;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        companyAccessRepository = mock(AppUserCompanyAccessRepository.class);
        companyRepository = mock(CompanyRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenAnswer(i -> "enc:" + i.getArgument(0));
        service = new PlatformUserService(appUserRepository, companyAccessRepository, companyRepository,
                encoder, mock(AuditLogService.class));
        CurrentUserContext.setCurrentUser("superadmin", "SUPERADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private Company company(Long id) {
        Company c = new Company();
        c.setId(id);
        c.setName("Empresa " + id);
        return c;
    }

    private AppUser user(String username, boolean platformAdmin) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setName(username);
        u.setRole("ADMIN");
        u.setActive(true);
        u.setPlatformAdmin(platformAdmin);
        return u;
    }

    @Test
    void setUserActive_desactiva() { // SU-01
        AppUser u = user("ana", false);
        when(appUserRepository.findByUsername("ana")).thenReturn(Optional.of(u));
        PlatformUserDTO dto = service.setUserActive("ana", false);
        assertFalse(dto.active());
        verify(appUserRepository).save(u);
    }

    @Test
    void setUserActive_naoDesactivaSuperadmin() { // SU-02
        when(appUserRepository.findByUsername("superadmin")).thenReturn(Optional.of(user("superadmin", true)));
        assertThrows(BusinessRuleException.class, () -> service.setUserActive("superadmin", false));
    }

    @Test
    void revokeAccess_ultimoAdmin_bloqueia() { // SU-03
        AppUser u = user("ana", false);
        u.grantCompany(company(2L), "ADMIN");
        when(appUserRepository.findByUsername("ana")).thenReturn(Optional.of(u));
        when(companyRepository.findById(2L)).thenReturn(Optional.of(company(2L)));
        when(companyAccessRepository.countByCompanyIdAndRoleIgnoreCase(2L, "ADMIN")).thenReturn(1L);

        assertThrows(BusinessRuleException.class, () -> service.revokeAccess("ana", 2L));
        assertTrue(u.hasCompany(2L)); // continua com acesso
    }

    @Test
    void revokeAccess_removeQuandoHaOutroAdmin() { // SU-04
        AppUser u = user("ana", false);
        u.grantCompany(company(2L), "ADMIN");
        when(appUserRepository.findByUsername("ana")).thenReturn(Optional.of(u));
        when(companyRepository.findById(2L)).thenReturn(Optional.of(company(2L)));
        when(companyAccessRepository.countByCompanyIdAndRoleIgnoreCase(2L, "ADMIN")).thenReturn(2L);

        service.revokeAccess("ana", 2L);
        assertFalse(u.hasCompany(2L));
        verify(appUserRepository).save(u);
    }

    @Test
    void listUsers_semSuperadmin_bloqueia() { // SU-05
        CurrentUserContext.setCurrentUser("joao", "MANAGER");
        assertThrows(BusinessRuleException.class, () -> service.listUsers());
        verifyNoInteractions(appUserRepository);
    }
}
