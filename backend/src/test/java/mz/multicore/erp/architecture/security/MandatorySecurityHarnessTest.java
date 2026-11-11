package mz.multicore.erp.architecture.security;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.backup.service.BackupFileCipher;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductCategoryRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import mz.multicore.erp.modules.pos.dto.POSCheckoutLineRequest;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.pos.service.PosDiscountAuthorizationService;
import mz.multicore.erp.modules.pos.service.PosPriceGuard;
import mz.multicore.erp.modules.promotions.service.PromotionService;
import mz.multicore.erp.modules.users.model.AppUser;
import mz.multicore.erp.modules.users.repository.AppUserRepository;
import mz.multicore.erp.modules.users.service.AccountSecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Ver docs/SEGURANCA_CONTAS_E_SEGREDOS_SPEC.md. */
class MandatorySecurityHarnessTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String STRONG_KEY = Base64.getEncoder().encodeToString("0123456789abcdefghijklmnopqrstuv".getBytes());

    @TempDir Path dir;

    private final ProductRepository products = mock(ProductRepository.class);
    private final PromotionService promotions = mock(PromotionService.class);
    private final PosDiscountAuthorizationService authorizations = new PosDiscountAuthorizationService();
    private final PosPriceGuard guard = new PosPriceGuard(
            new ProductLookup(products, mock(ProductCategoryRepository.class)), mock(QuotationRepository.class), promotions,
            authorizations, mock(AuditLogService.class));

    @BeforeEach
    void signIn() {
        CurrentUserContext.setCurrentUser("joao", "SELLER");
        CurrentUserContext.setCurrentCompanyId(1L);
        Product product = new Product();
        product.setId(7L);
        product.setName("Arroz");
        product.setUnitPrice(new BigDecimal("100.00"));
        when(products.findByIdAndCompaniesId(7L, 1L)).thenReturn(Optional.of(product));
        when(promotions.bestPromotion(any(), any(), any(), any())).thenReturn(Optional.empty());
    }

    @AfterEach
    void signOut() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("SC-13: Caixa pode dar até 10% sem autorização")
    void sc13_sellerLimit() {
        assertThatCode(() -> guard.requireAuthorized(sale("10", null, null))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SC-14: Acima de 10%, a API recusa sem autorização de gerente")
    void sc14_highDiscountRefused() {
        assertThatThrownBy(() -> guard.requireAuthorized(sale("50", null, null)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("PIN");
    }

    @Test
    @DisplayName("SC-15: Autorização vale uma vez, até à percentagem aprovada")
    void sc15_authorizationIsSingleUseAndLimited() {
        String token = authorizations.issue(1L, "Maria", new BigDecimal("20"));
        assertThatCode(() -> guard.requireAuthorized(sale("20", null, token))).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.requireAuthorized(sale("20", null, token))).isInstanceOf(BusinessRuleException.class);

        String small = authorizations.issue(1L, "Maria", new BigDecimal("15"));
        assertThatThrownBy(() -> guard.requireAuthorized(sale("50", null, small))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("SC-16: Promoção activa dispensa o PIN; gerente também")
    void sc16_promotionAndManagerAllowed() {
        when(promotions.bestPromotion(any(), any(), any(), any()))
                .thenReturn(Optional.of(new PromotionService.AppliedPromotion("Feira", new BigDecimal("30"))));
        assertThatCode(() -> guard.requireAuthorized(sale("30", null, null))).doesNotThrowAnyException();

        when(promotions.bestPromotion(any(), any(), any(), any())).thenReturn(Optional.empty());
        CurrentUserContext.setCurrentUser("maria", "MANAGER");
        assertThatCode(() -> guard.requireAuthorized(sale("90", "1.00", null))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SC-17: Caixa não vende abaixo do preço de tabela enviando outro preço")
    void sc17_clientPriceCannotUndercut() {
        assertThatThrownBy(() -> guard.requireAuthorized(sale("0", "1.00", null)))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("preço");
        assertThatCode(() -> guard.requireAuthorized(sale("0", "100.00", null))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SC-18: Senha definida por outra pessoa troca-se com a actual e termina as sessões")
    void sc18_changePassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        AppUserRepository users = mock(AppUserRepository.class);
        AuthSessionService sessions = mock(AuthSessionService.class);
        AppUser user = new AppUser();
        user.setUsername("ana");
        user.setPassword(encoder.encode("Provisoria2026"));
        user.setPasswordChangeRequired(true);
        when(users.findByUsername("ana")).thenReturn(Optional.of(user));
        AccountSecurityService service = new AccountSecurityService(users, encoder, new AttachmentCrypto(""), sessions,
                mock(AuditLogService.class), false);

        assertThatThrownBy(() -> service.changePassword("ana", "errada", "NovaSenha2026")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.changePassword("ana", "Provisoria2026", "Provisoria2026")).isInstanceOf(BusinessRuleException.class);

        service.changePassword("ana", "Provisoria2026", "NovaSenha2026");

        assertThat(user.isPasswordChangeRequired()).isFalse();
        assertThat(encoder.matches("NovaSenha2026", user.getPassword())).isTrue();
        verify(sessions).revokeUser("ana");
    }

    @Test
    @DisplayName("SC-19: Sessão restrita só serve para concluir a segurança da conta")
    void sc19_restrictedSession() throws Exception {
        AppUser user = new AppUser();
        user.setUsername("ana");
        AuthSessionService sessions = new AuthSessionService();
        String token = sessions.create(user, true).token();
        var request = mock(jakarta.servlet.http.HttpServletRequest.class);
        var response = mock(jakarta.servlet.http.HttpServletResponse.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Company-Id")).thenReturn("1");
        when(request.getRequestURI()).thenReturn("/api/comercial/products");

        boolean allowed = new SecurityInterceptor(mock(TenantAccessService.class), sessions).preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        verify(response).sendError(eq(403), contains("segurança da conta"));
    }

    @Test
    @DisplayName("SC-20: Código TOTP segue a RFC 6238 e expira")
    void sc20_totp() {
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        Instant moment = Instant.ofEpochSecond(59);

        assertThat(TotpCodec.verify(secret, "287082", moment)).isTrue();
        assertThat(TotpCodec.verify(secret, "287082", moment.plusSeconds(300))).isFalse();
        assertThat(TotpCodec.verify(secret, "abc", moment)).isFalse();
        assertThat(TotpCodec.newSecret()).matches("[A-Z2-7]{32}");
    }

    @Test
    @DisplayName("SC-21: Só administradores e plataforma são obrigados à dupla autenticação")
    void sc21_twoFactorScope() {
        AccountSecurityService service = new AccountSecurityService(mock(AppUserRepository.class), new BCryptPasswordEncoder(4),
                new AttachmentCrypto(""), mock(AuthSessionService.class), mock(AuditLogService.class), true);
        AppUser platform = new AppUser();
        platform.setPlatformAdmin(true);

        assertThat(service.mustUseTwoFactor(platform)).isTrue();
        assertThat(service.mustUseTwoFactor(new AppUser())).isFalse();
    }

    @Test
    @DisplayName("SC-22: Produção recusa segredos fracos ou de exemplo")
    void sc22_weakSecretsRefused() {
        assertThatThrownBy(() -> ProductionSecretsGuard.validate("TROCAR-por-uma-password-longa", STRONG_KEY, ""))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ProductionSecretsGuard.validate("curta", STRONG_KEY, "")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ProductionSecretsGuard.validate("k8Qz3vT1mN6xW2bR9f", KEY, "")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ProductionSecretsGuard.validate("k8Qz3vT1mN6xW2bR9f", STRONG_KEY, "superadmin")).isInstanceOf(IllegalStateException.class);
        assertThatCode(() -> ProductionSecretsGuard.validate("k8Qz3vT1mN6xW2bR9f", STRONG_KEY, "")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SC-23: Backup cifrado abre igual, sem conteúdo em claro, em ficheiros de vários blocos")
    void sc23_backupRoundTrip() throws Exception {
        BackupFileCipher cipher = new BackupFileCipher(STRONG_KEY);
        byte[] content = new byte[3 * 1024 * 1024 + 17];
        new Random(1).nextBytes(content);
        byte[] marker = "SEGREDO-DO-CLIENTE".getBytes();
        System.arraycopy(marker, 0, content, 100, marker.length);
        Path plain = Files.write(dir.resolve("a.dump"), content);
        Path sealed = dir.resolve("a.dump.enc");
        Path back = dir.resolve("b.dump");

        cipher.encrypt(plain, sealed);
        cipher.verify(sealed);
        cipher.decrypt(sealed, back);

        assertThat(Files.readAllBytes(back)).isEqualTo(content);
        assertThat(new String(Files.readAllBytes(sealed), java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("SEGREDO-DO-CLIENTE");
    }

    @Test
    @DisplayName("SC-24: Backup alterado, cortado ou com outra chave é recusado")
    void sc24_backupTamperDetected() throws Exception {
        BackupFileCipher cipher = new BackupFileCipher(STRONG_KEY);
        Path plain = Files.write(dir.resolve("a.dump"), new byte[2 * 1024 * 1024 + 5]);
        Path sealed = dir.resolve("a.dump.enc");
        cipher.encrypt(plain, sealed);
        byte[] bytes = Files.readAllBytes(sealed);

        assertThatThrownBy(() -> new BackupFileCipher(KEY).verify(sealed)).isInstanceOf(BusinessRuleException.class);

        byte[] changed = bytes.clone();
        changed[50] ^= 1;
        Path altered = Files.write(dir.resolve("b.enc"), changed);
        assertThatThrownBy(() -> cipher.verify(altered)).isInstanceOf(BusinessRuleException.class);

        Path cut = Files.write(dir.resolve("c.enc"), java.util.Arrays.copyOf(bytes, 1024 * 1024 + 100));
        assertThatThrownBy(() -> cipher.verify(cut)).isInstanceOf(BusinessRuleException.class);
    }

    private static POSCheckoutRequest sale(String discount, String price, String token) {
        POSCheckoutLineRequest line = new POSCheckoutLineRequest(7L, BigDecimal.ONE, new BigDecimal(discount),
                null, null, price == null ? null : new BigDecimal(price));
        return new POSCheckoutRequest("joao", 1L, null, "Cliente", 1L, null, List.of(line), null, null, null, token);
    }
}
