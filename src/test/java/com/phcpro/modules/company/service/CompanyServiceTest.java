package com.phcpro.modules.company.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.architecture.security.TenantAccessService;
import com.phcpro.modules.company.model.Company;
import com.phcpro.modules.company.repository.CompanyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyServiceTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final CompanyService service = new CompanyService(
            companyRepository, mock(TenantAccessService.class));

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void getCurrentCompanyReference_empresaActiva_devolveEmpresa() {
        CurrentUserContext.setCurrentCompanyId(7L);
        Company company = new Company();
        company.setId(7L);
        when(companyRepository.findById(7L)).thenReturn(Optional.of(company));

        Company result = service.getCurrentCompanyReference(7L);

        assertSame(company, result);
        verify(companyRepository).findById(7L);
    }

    @Test
    void getCurrentCompanyReference_empresaDiferente_recusaAntesDoRepository() {
        CurrentUserContext.setCurrentCompanyId(7L);

        assertThrows(BusinessRuleException.class,
                () -> service.getCurrentCompanyReference(8L));
    }
}
