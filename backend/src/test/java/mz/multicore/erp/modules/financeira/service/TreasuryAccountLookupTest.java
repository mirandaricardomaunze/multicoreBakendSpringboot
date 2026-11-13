package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * MD-R05 — contas de tesouraria lidas fora da financeira só pelo {@link TreasuryAccountLookup}
 * e criadas só pelo {@link TreasuryAccountProvisioningService} (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
class TreasuryAccountLookupTest {

    private final TreasuryAccountRepository repository = mock(TreasuryAccountRepository.class);
    private final TreasuryAccountLookup lookup = new TreasuryAccountLookup(repository);

    @Test
    void requireForCompany_devolveContaOuMensagemNormalizada() {
        TreasuryAccount account = new TreasuryAccount();
        when(repository.findByIdAndCompanyId(5L, 1L)).thenReturn(Optional.of(account));
        when(repository.findByIdAndCompanyId(6L, 1L)).thenReturn(Optional.empty());

        assertThat(lookup.requireForCompany(5L, 1L)).isSameAs(account);
        assertThatThrownBy(() -> lookup.requireForCompany(6L, 1L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Conta de tesouraria não encontrada.");
    }

    @Test
    void listForCompany_delegaOrdenadoPorNome() {
        List<TreasuryAccount> accounts = List.of(new TreasuryAccount(), new TreasuryAccount());
        when(repository.findByCompanyIdOrderByName(1L)).thenReturn(accounts);

        assertThat(lookup.listForCompany(1L)).isEqualTo(accounts);
    }

    @Test
    void idsNulos_naoTocamNoRepository() {
        assertThat(lookup.listForCompany(null)).isEmpty();
        assertThatThrownBy(() -> lookup.requireForCompany(null, 1L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Conta de tesouraria não encontrada.");
        assertThatThrownBy(() -> lookup.requireForCompany(5L, null))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Conta de tesouraria não encontrada.");
        verifyNoInteractions(repository);
    }

    @Test
    void aberturaComSaldoInicial_passaPeloServicoDono() {
        TreasuryAccountProvisioningService provisioning = new TreasuryAccountProvisioningService(repository);
        when(repository.save(any(TreasuryAccount.class))).thenAnswer(i -> {
            TreasuryAccount saved = i.getArgument(0);
            saved.setId(9L);
            return saved;
        });
        Company company = new Company();
        company.setId(1L);

        assertThat(provisioning.openBankAccount(company, " BCI ", " 0008 0000 ", new BigDecimal("45000.00")))
                .isEqualTo(9L);

        ArgumentCaptor<TreasuryAccount> captor = ArgumentCaptor.forClass(TreasuryAccount.class);
        verify(repository).save(captor.capture());
        TreasuryAccount saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("BCI");
        assertThat(saved.getAccountNumber()).isEqualTo("0008 0000");
        assertThat(saved.getAccountType()).isEqualTo(TreasuryAccountType.BANK);
        assertThat(saved.getBalance()).isEqualByComparingTo("45000.00");
        assertThat(saved.getCompany()).isSameAs(company);
    }

    @Test
    void contrato_lookupSoDeLeitura() {
        assertThat(TreasuryAccountLookup.class).hasAnnotation(Service.class);
        Transactional tx = TreasuryAccountLookup.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isTrue();
        List<Method> publicMethods = Arrays.stream(TreasuryAccountLookup.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers())).toList();
        assertThat(publicMethods).isNotEmpty()
                .allMatch(m -> m.getName().startsWith("find") || m.getName().startsWith("list")
                        || m.getName().startsWith("require"))
                .noneMatch(m -> m.getReturnType() == void.class);
    }

    /** Nenhum módulo fora da financeira importa um repositório da financeira. */
    @Test
    void foraDaFinanceira_naoHaRepositoriosDaFinanceira() throws IOException {
        Path main = moduleRoot().resolve("src/main/java/mz/multicore/erp");
        Path financeira = main.resolve("modules/financeira");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(main)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.startsWith(financeira)).toList()) {
                if (Files.readString(file).contains("modules.financeira.repository.")) {
                    offenders.add(main.relativize(file).toString());
                }
            }
        }
        assertThat(offenders).as("Contas de tesouraria lidas fora da financeira passam pelo TreasuryAccountLookup")
                .isEmpty();
    }

    private static Path moduleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return Files.isDirectory(cwd.resolve("src/main/java")) ? cwd : cwd.resolve("backend");
    }
}
