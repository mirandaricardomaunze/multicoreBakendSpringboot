package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.pos.model.MobilePaymentTransaction;
import mz.multicore.erp.modules.pos.model.PaymentEntry;
import mz.multicore.erp.modules.pos.model.StoreVoucher;
import mz.multicore.erp.modules.pos.model.TillMovement;
import mz.multicore.erp.modules.pos.model.TillSession;
import mz.multicore.erp.modules.pos.repository.MobilePaymentRepository;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
import mz.multicore.erp.modules.pos.repository.StoreVoucherRepository;
import mz.multicore.erp.modules.pos.repository.TillMovementRepository;
import mz.multicore.erp.modules.pos.repository.TillSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * MD-R04 — sessões de caixa, movimentos, pagamentos, vales e pagamentos móveis do POS lidos fora do
 * módulo só pelos Lookups do POS (docs/MODULE_DECOUPLING_SPEC.md §2).
 */
class PosLookupsTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 11, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 11, 30, 23, 59);

    private final TillSessionRepository sessions = mock(TillSessionRepository.class);
    private final TillMovementRepository movements = mock(TillMovementRepository.class);
    private final PaymentEntryRepository payments = mock(PaymentEntryRepository.class);
    private final StoreVoucherRepository vouchers = mock(StoreVoucherRepository.class);
    private final MobilePaymentRepository mobilePayments = mock(MobilePaymentRepository.class);

    private final TillSessionLookup tillLookup = new TillSessionLookup(sessions, movements);
    private final PosPaymentLookup paymentLookup = new PosPaymentLookup(payments, vouchers, mobilePayments);

    @Test
    void sessoesEMovimentos_listagensDelegam() {
        List<TillSession> s = List.of(new TillSession());
        List<TillMovement> m = List.of(new TillMovement());
        when(sessions.findByCompanyId(1L)).thenReturn(s);
        when(movements.findByTillSessionCompanyIdAndMovementDateBetween(1L, FROM, TO)).thenReturn(m);

        assertThat(tillLookup.listForCompany(1L)).isEqualTo(s);
        assertThat(tillLookup.listMovementsForCompany(1L, FROM, TO)).isEqualTo(m);
    }

    @Test
    void pagamentos_listagensDelegam() {
        List<PaymentEntry> daFactura = List.of(new PaymentEntry());
        List<PaymentEntry> doPeriodo = List.of(new PaymentEntry(), new PaymentEntry());
        when(payments.findByInvoiceIdOrderByPaidAtAsc(7L)).thenReturn(daFactura);
        when(payments.findByInvoiceCompanyIdAndPaidAtBetween(1L, FROM, TO)).thenReturn(doPeriodo);

        assertThat(paymentLookup.listForInvoice(7L)).isEqualTo(daFactura);
        assertThat(paymentLookup.listPaidForCompanyBetween(1L, FROM, TO)).isEqualTo(doPeriodo);
    }

    @Test
    void pagamentoMovel_procuradoPorTransaccaoEEmpresa() {
        MobilePaymentTransaction tx = new MobilePaymentTransaction();
        when(mobilePayments.findByTransactionIdAndCompanyId("TX-1", 1L)).thenReturn(Optional.of(tx));
        when(mobilePayments.findByTransactionIdAndCompanyId("TX-2", 1L)).thenReturn(Optional.empty());

        assertThat(paymentLookup.findMobilePaymentForCompany("TX-1", 1L)).containsSame(tx);
        assertThat(paymentLookup.findMobilePaymentForCompany("TX-2", 1L)).isEmpty();
    }

    @Test
    void vales_requireEFindByCode_mensagemNormalizada() {
        StoreVoucher voucher = new StoreVoucher();
        when(vouchers.findById(3L)).thenReturn(Optional.of(voucher));
        when(vouchers.findById(4L)).thenReturn(Optional.empty());
        when(vouchers.findByCode("VC-1")).thenReturn(Optional.of(voucher));

        assertThat(paymentLookup.requireVoucher(3L)).isSameAs(voucher);
        assertThatThrownBy(() -> paymentLookup.requireVoucher(4L))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Vale de compras não encontrado.");
        assertThat(paymentLookup.findVoucherByCode("VC-1")).containsSame(voucher);
        assertThat(paymentLookup.findVoucherByCode("VC-2")).isEmpty();
    }

    @Test
    void idsNulos_naoTocamNoRepository() {
        PaymentEntryRepository p = mock(PaymentEntryRepository.class);
        StoreVoucherRepository v = mock(StoreVoucherRepository.class);
        MobilePaymentRepository mp = mock(MobilePaymentRepository.class);
        PosPaymentLookup lookup = new PosPaymentLookup(p, v, mp);

        assertThat(lookup.listForInvoice(null)).isEmpty();
        assertThat(lookup.findVoucherByCode(null)).isEmpty();
        assertThat(lookup.findMobilePaymentForCompany(null, 1L)).isEmpty();
        assertThatThrownBy(() -> lookup.requireVoucher(null))
                .isInstanceOf(BusinessRuleException.class).hasMessage("Vale de compras não encontrado.");
        verifyNoInteractions(p, v, mp);
    }

    @Test
    void contrato_lookupsSoDeLeitura() {
        for (Class<?> lookup : List.of(TillSessionLookup.class, PosPaymentLookup.class)) {
            assertThat(lookup).hasAnnotation(Service.class);
            Transactional tx = lookup.getAnnotation(Transactional.class);
            assertThat(tx).as(lookup.getSimpleName()).isNotNull();
            assertThat(tx.readOnly()).as(lookup.getSimpleName()).isTrue();
            List<Method> publicMethods = Arrays.stream(lookup.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers())).toList();
            assertThat(publicMethods).as(lookup.getSimpleName()).isNotEmpty()
                    .allMatch(m -> m.getName().startsWith("find") || m.getName().startsWith("list")
                            || m.getName().startsWith("require"))
                    .noneMatch(m -> m.getReturnType() == void.class);
        }
    }

    /** Nenhum módulo fora do POS importa um repositório do POS. */
    @Test
    void foraDoPos_naoHaRepositoriosDoPos() throws IOException {
        Path main = moduleRoot().resolve("src/main/java/mz/multicore/erp");
        Path pos = main.resolve("modules/pos");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(main)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.startsWith(pos)).toList()) {
                if (Files.readString(file).contains("modules.pos.repository.")) {
                    offenders.add(main.relativize(file).toString());
                }
            }
        }
        assertThat(offenders).as("Sessões, pagamentos e vales do POS lidos fora do POS passam pelos Lookups")
                .isEmpty();
    }

    private static Path moduleRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return Files.isDirectory(cwd.resolve("src/main/java")) ? cwd : cwd.resolve("backend");
    }
}
