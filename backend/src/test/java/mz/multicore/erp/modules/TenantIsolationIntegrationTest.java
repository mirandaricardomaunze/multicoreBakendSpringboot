package mz.multicore.erp.modules;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.crm.service.CRMService;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.fiscal.service.TaxRateService;
import mz.multicore.erp.modules.users.service.AppUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
    void companyCannotLoseItsLastAdministrator() {
        Long firstCompany = companyRepository.findAll().get(0).getId();
        CurrentUserContext.setCurrentUser("ana", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(firstCompany);

        assertThrows(RuntimeException.class, () -> appUserService.updateCompanyRole("ana", "MANAGER"));
    }
}
