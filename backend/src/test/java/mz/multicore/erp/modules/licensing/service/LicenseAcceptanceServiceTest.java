package mz.multicore.erp.modules.licensing.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.licensing.dto.AcceptLicenseRequest;
import mz.multicore.erp.modules.licensing.model.LicenseAcceptance;
import mz.multicore.erp.modules.licensing.repository.LicenseAcceptanceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LicenseAcceptanceServiceTest {

    @Mock LicenseAcceptanceRepository repository;
    @Mock AuditLogService auditLogService;
    LicenseAcceptanceService service;

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
        CurrentUserContext.setCurrentCompanyId(7L);
        service = new LicenseAcceptanceService(repository, auditLogService);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void contratoVigenteDevolveConteudoHashEEstadoPendente() {
        when(repository.findByCompanyIdAndLicenseVersion(7L, "1.0")).thenReturn(Optional.empty());

        var terms = service.currentTerms();

        assertThat(terms.content()).contains("CONTRATO DE LICENÇA");
        assertThat(terms.sha256()).hasSize(64);
        assertThat(terms.accepted()).isFalse();
    }

    @Test
    void aceitaComRepresentanteAutorizadoEGravaEvidencia() {
        when(repository.findByCompanyIdAndLicenseVersion(7L, "1.0")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> {
            LicenseAcceptance acceptance = invocation.getArgument(0);
            acceptance.setId(11L);
            return acceptance;
        });
        var terms = service.currentTerms();

        var result = service.accept(new AcceptLicenseRequest("1.0", terms.sha256(), true,
                "Declaro possuir poderes para representar a empresa."), "127.0.0.1", "1.0.0", "JUnit");

        assertThat(result.id()).isEqualTo(11L);
        assertThat(result.companyId()).isEqualTo(7L);
        assertThat(result.licenseSha256()).isEqualTo(terms.sha256());
        verify(auditLogService).logCurrent("LICENSE_ACCEPTED",
                "Contrato 1.0 aceite; SHA-256=" + terms.sha256());
    }

    @Test
    void recusaSemDeclaracaoDePoderes() {
        var request = new AcceptLicenseRequest("1.0", "hash", false, "Declaração");
        assertThatThrownBy(() -> service.accept(request, null, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("poderes");
    }

    @Test
    void recusaPapelSemPoderDeRepresentacao() {
        CurrentUserContext.setCurrentUser("operador", "SELLER");
        var request = new AcceptLicenseRequest("1.0", "hash", true, "Declaração");
        assertThatThrownBy(() -> service.accept(request, null, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("permissão");
    }
}
