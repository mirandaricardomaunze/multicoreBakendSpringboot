package mz.multicore.erp.modules;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.crm.service.CRMService;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.fiscal.service.TaxRateService;
import mz.multicore.erp.modules.users.service.AppUserService;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class TenantIsolationIntegrationTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private ComercialService comercialService;
    @Autowired private FinanceService financeService;
    @Autowired private CRMService crmService;
    @Autowired private ApprovalService approvalService;
    @Autowired private TaxRateService taxRateService;
    @Autowired private AppUserService appUserService;
    @Autowired private AppUserRepository appUserRepository;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void masterAndOperationalDataAreScopedByActiveCompany() {
        var companies = companyRepository.findAll();
        Long firstCompany = companies.get(0).getId();
        Long secondCompany = companies.get(1).getId();

        CurrentUserContext.setCurrentCompanyId(firstCompany);
        assertFalse(comercialService.getAllClients().isEmpty());
        assertFalse(comercialService.getAllProducts().isEmpty());
        assertEquals(2, crmService.getAllTickets().size());
        assertFalse(approvalService.getAllRequests().isEmpty());
        assertEquals(1, financeService.getAllAccounts().size());
        int firstCompanyTaxRates = taxRateService.getAll().size();

        CurrentUserContext.setCurrentCompanyId(secondCompany);
        assertTrue(comercialService.getAllClients().isEmpty());
        assertFalse(comercialService.getAllProducts().isEmpty());
        assertTrue(crmService.getAllTickets().isEmpty());
        assertTrue(approvalService.getAllRequests().isEmpty());
        assertEquals(1, financeService.getAllAccounts().size());
        assertEquals(firstCompanyTaxRates, taxRateService.getAll().size());
    }

    @Test
    @Transactional
    void companyCannotLoseItsLastAdministrator() {
        var isolatedCompany = new mz.multicore.erp.modules.company.model.Company();
        isolatedCompany.setName("Empresa isolada do harness");
        isolatedCompany.setTaxId("SFS-ADMIN-001");
        isolatedCompany = companyRepository.saveAndFlush(isolatedCompany);

        AppUser soleAdmin = new AppUser();
        soleAdmin.setUsername("sfs_sole_admin");
        soleAdmin.setName("Administrador Único SFS");
        soleAdmin.setPassword("password");
        soleAdmin.setRole("ADMIN");
        soleAdmin.setActive(true);
        soleAdmin.grantCompany(isolatedCompany, "ADMIN");
        appUserRepository.saveAndFlush(soleAdmin);

        CurrentUserContext.setCurrentUser(soleAdmin.getUsername(), "ADMIN");
        CurrentUserContext.setCurrentCompanyId(isolatedCompany.getId());

        assertThrows(RuntimeException.class,
                () -> appUserService.updateCompanyRole(soleAdmin.getUsername(), "MANAGER"));
        assertThrows(RuntimeException.class,
                () -> appUserService.toggleUserStatus(soleAdmin.getUsername(), false));
    }
}
