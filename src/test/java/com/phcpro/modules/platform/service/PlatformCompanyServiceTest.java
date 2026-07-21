package com.phcpro.modules.platform.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.platform.dto.CreateCompanyRequest;
import com.phcpro.modules.platform.dto.PlatformCompanyDTO;
import com.phcpro.modules.users.repository.AppUserCompanyAccessRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes do {@link PlatformCompanyService}: exige papel SUPERADMIN, muda estado com auditoria,
 * lista com contagem de utilizadores e rejeita NUIT repetido. Dependências mockadas.
 */
class PlatformCompanyServiceTest {

    private CompanyRepository companyRepository;
    private AppUserCompanyAccessRepository companyAccessRepository;
    private AuditLogService auditLogService;
    private PlatformCompanyService service;

    @BeforeEach
    void setUp() {
        companyRepository = mock(CompanyRepository.class);
        companyAccessRepository = mock(AppUserCompanyAccessRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new PlatformCompanyService(companyRepository, companyAccessRepository, auditLogService);
        CurrentUserContext.setCurrentUser("superadmin", "SUPERADMIN");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    private Company company(Long id, String name, boolean active) {
        Company c = new Company();
        c.setId(id);
        c.setName(name);
        c.setTaxId("NUIT-" + id);
        c.setActive(active);
        return c;
    }

    @Test
    void setCompanyActive_desactiva_gravaEAudita() { // SA-01
        Company c = company(7L, "Loja X", true);
        when(companyRepository.findById(7L)).thenReturn(Optional.of(c));
        when(companyAccessRepository.countByCompanyId(7L)).thenReturn(3L);

        PlatformCompanyDTO dto = service.setCompanyActive(7L, false);

        assertFalse(dto.active());
        assertFalse(c.isActive());
        verify(companyRepository).save(c);
        verify(auditLogService).logEvent(eq("superadmin"), eq(7L), eq("PLATFORM_COMPANY_STATUS"), anyString());
    }

    @Test
    void setCompanyActive_semSuperadmin_bloqueia() { // SA-02
        CurrentUserContext.setCurrentUser("ana", "ADMIN");
        assertThrows(BusinessRuleException.class, () -> service.setCompanyActive(1L, false));
        verifyNoInteractions(companyRepository);
    }

    @Test
    void listCompanies_incluiEstadoEContagem() { // SA-03
        when(companyRepository.findAll()).thenReturn(List.of(
                company(1L, "Beta", true), company(2L, "Alfa", false)));
        when(companyAccessRepository.countByCompanyId(1L)).thenReturn(2L);
        when(companyAccessRepository.countByCompanyId(2L)).thenReturn(0L);

        List<PlatformCompanyDTO> result = service.listCompanies();

        assertEquals(2, result.size());
        assertEquals("Alfa", result.get(0).name()); // ordenado por nome
        assertFalse(result.get(0).active());
        assertEquals(0L, result.get(0).userCount());
        assertEquals("Beta", result.get(1).name());
        assertEquals(2L, result.get(1).userCount());
    }

    @Test
    void createCompany_nuitRepetido_rejeita() { // SA-04
        when(companyRepository.findByTaxId("400123456"))
                .thenReturn(Optional.of(company(9L, "Existente", true)));

        assertThrows(BusinessRuleException.class, () -> service.createCompany(
                new CreateCompanyRequest("Nova", "400123456", "n@n.mz", "Maputo", "+258 84 000 0000")));
        verify(companyRepository, never()).save(any());
    }
}
