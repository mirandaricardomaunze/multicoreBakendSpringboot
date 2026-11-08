package mz.multicore.erp.modules.platform.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.platform.dto.CreateCompanyRequest;
import mz.multicore.erp.modules.platform.dto.PlatformCompanyDTO;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
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
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    private PlatformCompanyService service;

    @BeforeEach
    void setUp() {
        companyRepository = mock(CompanyRepository.class);
        companyAccessRepository = mock(AppUserCompanyAccessRepository.class);
        auditLogService = mock(AuditLogService.class);
        eventPublisher = mock(org.springframework.context.ApplicationEventPublisher.class);
        service = new PlatformCompanyService(companyRepository, companyAccessRepository, auditLogService, eventPublisher);
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
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void createCompany_publicaFactoParaOPlanoDeContas() { // CD-08
        when(companyRepository.findByTaxId("400123457")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> {
            Company saved = inv.getArgument(0);
            saved.setId(12L);
            return saved;
        });

        service.createCompany(new CreateCompanyRequest("Nova", "400123457", "n@n.mz", "Maputo", "+258 84 000 0000"));

        verify(eventPublisher).publishEvent(new mz.multicore.erp.architecture.events.CompanyCreatedEvent(12L));
    }

    @Test
    void createCompany_gravaPerfilELimpaCamposEmBranco() { // CP-08
        when(companyRepository.findByTaxId("400123458")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));
        mz.multicore.erp.modules.platform.dto.CompanyProfile profile = new mz.multicore.erp.modules.platform.dto.CompanyProfile(
                "  Mercearia Sol ", mz.multicore.erp.modules.company.model.CompanyLegalForm.PRIVATE_LIMITED,
                "   ", null, null, null, mz.multicore.erp.modules.company.model.VatRegime.NORMAL, null,
                null, "Maputo", mz.multicore.erp.modules.company.model.MozambiqueProvince.MAPUTO_CITY, null,
                null, null, null, null);

        PlatformCompanyDTO dto = service.createCompany(
                new CreateCompanyRequest("Sol, Lda", "400123458", "  ", "Av. 1", null, profile));

        assertEquals("Mercearia Sol", dto.profile().tradeName());
        assertNull(dto.profile().registryNumber(), "texto só com espaços grava-se como vazio");
        assertNull(dto.email());
        assertEquals("Maputo", dto.profile().city());
    }

    @Test
    void updateCompany_semPerfil_mantemOPerfilGravado() { // CP-09
        Company c = company(5L, "Sol", true);
        c.setTradeName("Mercearia Sol");
        c.setCity("Beira");
        when(companyRepository.findById(5L)).thenReturn(Optional.of(c));

        PlatformCompanyDTO dto = service.updateCompany(5L,
                new mz.multicore.erp.modules.platform.dto.UpdateCompanyRequest("Sol Novo", null, null, null));

        assertEquals("Sol Novo", dto.name());
        assertEquals("Mercearia Sol", dto.profile().tradeName());
        assertEquals("Beira", dto.profile().city());
    }
}
