package mz.multicore.erp.modules.users.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.AttachmentCrypto;
import mz.multicore.erp.architecture.security.AuthSessionService;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.hr.model.OccupationalHealthExam;
import mz.multicore.erp.modules.hr.repository.OccupationalHealthExamRepository;
import mz.multicore.erp.modules.hr.service.AttachmentEncryptionMigrator;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionOperations;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Ver docs/SEGURANCA_CONTAS_E_SEGREDOS_SPEC.md. */
class SharedAccountSecurityHarnessTest {

    private final AppUserRepository users = mock(AppUserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final AppUserCompanyAccessRepository accesses = mock(AppUserCompanyAccessRepository.class);
    private final AuthSessionService sessions = mock(AuthSessionService.class);
    private final UserAccountAdministrationService service = new UserAccountAdministrationService(
            users, encoder, new CompanyLookup(companies), accesses, mock(AuditLogService.class), sessions);

    private final Company companyA = company(1L);
    private final Company companyB = company(2L);

    @BeforeEach
    void signIn() {
        CurrentUserContext.setCurrentUser("admin-a", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(1L);
        when(users.save(any(AppUser.class))).thenAnswer(call -> call.getArgument(0));
        when(accesses.countActiveByCompanyIdAndRole(any(), any())).thenReturn(5L);
    }

    @AfterEach
    void signOut() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("SC-01: Administrador de uma empresa não repõe a senha de uma conta partilhada")
    void sc01_sharedPasswordResetRefused() {
        register(account("admin-a", companyA));
        AppUser shared = register(account("partilhada", companyA, companyB));
        shared.setPassword("original");

        assertThatThrownBy(() -> service.resetPassword("partilhada", "NovaSenha2026!"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("plataforma");
        assertThat(shared.getPassword()).isEqualTo("original");
    }

    @Test
    @DisplayName("SC-02: Quem administra todas as empresas da conta pode repor a senha")
    void sc02_adminOfAllCompaniesAllowed() {
        register(account("admin-a", companyA, companyB));
        AppUser shared = register(account("partilhada", companyA, companyB));
        when(encoder.encode(any())).thenReturn("nova");

        service.resetPassword("partilhada", "NovaSenha2026!");

        assertThat(shared.getPassword()).isEqualTo("nova");
        verify(sessions).revokeUser("partilhada");
    }

    @Test
    @DisplayName("SC-03: Desactivar conta partilhada só retira o acesso à empresa activa")
    void sc03_sharedDeactivationIsLocal() {
        register(account("admin-a", companyA));
        AppUser shared = register(account("partilhada", companyA, companyB));
        shared.setActive(true);

        service.toggleUserStatus("partilhada", false);

        assertThat(shared.isActive()).isTrue();
        assertThat(shared.findCompanyAccess(1L)).isEmpty();
        assertThat(shared.findCompanyAccess(2L)).isPresent();
        verify(sessions).revokeUser("partilhada");
    }

    @Test
    @DisplayName("SC-04: Conta só desta empresa continua a ser gerida normalmente")
    void sc04_exclusiveAccountUnchanged() {
        register(account("admin-a", companyA));
        AppUser own = register(account("operador", companyA));
        own.setActive(true);

        service.toggleUserStatus("operador", false);

        assertThat(own.isActive()).isFalse();
    }

    @Test
    @DisplayName("SC-05: Ninguém concede acesso a uma empresa que não administra")
    void sc05_grantRequiresAdminOfTarget() {
        register(account("admin-a", companyA));
        register(account("alvo", companyA));

        assertThatThrownBy(() -> service.grantCompanyAccess("alvo", 2L, "ADMIN"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("destino");
    }

    @Test
    @DisplayName("SC-12: Anexos em claro são cifrados no arranque, sem cifrar duas vezes")
    void sc12_legacyAttachmentsAreEncrypted() throws Exception {
        AttachmentCrypto crypto = new AttachmentCrypto(Base64.getEncoder().encodeToString(new byte[32]));
        OccupationalHealthExamRepository exams = mock(OccupationalHealthExamRepository.class);
        OccupationalHealthExam plain = new OccupationalHealthExam();
        plain.setAttachmentData("exame".getBytes());
        OccupationalHealthExam sealed = new OccupationalHealthExam();
        byte[] alreadySealed = crypto.protect("outro".getBytes());
        sealed.setAttachmentData(alreadySealed);
        when(exams.findIdsWithAttachment()).thenReturn(List.of(1L, 2L));
        when(exams.findById(1L)).thenReturn(Optional.of(plain));
        when(exams.findById(2L)).thenReturn(Optional.of(sealed));

        new AttachmentEncryptionMigrator(exams, crypto, TransactionOperations.withoutTransaction()).run(null);

        assertThat(AttachmentCrypto.isEncrypted(plain.getAttachmentData())).isTrue();
        assertThat(crypto.reveal(plain.getAttachmentData())).isEqualTo("exame".getBytes());
        assertThat(sealed.getAttachmentData()).isSameAs(alreadySealed);
    }

    private AppUser register(AppUser user) {
        when(users.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        return user;
    }

    private static AppUser account(String username, Company... companies) {
        AppUser user = new AppUser();
        user.setUsername(username);
        for (Company company : companies) user.grantCompany(company, "ADMIN");
        return user;
    }

    private static Company company(long id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }
}
