package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.pos.model.MobilePaymentTransaction;
import mz.multicore.erp.modules.pos.model.PaymentEntry;
import mz.multicore.erp.modules.pos.model.StoreVoucher;
import mz.multicore.erp.modules.pos.repository.MobilePaymentRepository;
import mz.multicore.erp.modules.pos.repository.PaymentEntryRepository;
import mz.multicore.erp.modules.pos.repository.StoreVoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura dos pagamentos de vendas POS para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2):
 * linhas de pagamento, e — por serem do agregado mais próximo — vales de compras e pagamentos móveis.
 * Checkout, devoluções e cobranças móveis passam pelos serviços do POS. As entidades devolvidas servem
 * só para uso dentro do backend.
 */
@Service
@Transactional(readOnly = true)
public class PosPaymentLookup {

    private final PaymentEntryRepository paymentEntryRepository;
    private final StoreVoucherRepository storeVoucherRepository;
    private final MobilePaymentRepository mobilePaymentRepository;

    public PosPaymentLookup(PaymentEntryRepository paymentEntryRepository,
                            StoreVoucherRepository storeVoucherRepository,
                            MobilePaymentRepository mobilePaymentRepository) {
        this.paymentEntryRepository = paymentEntryRepository;
        this.storeVoucherRepository = storeVoucherRepository;
        this.mobilePaymentRepository = mobilePaymentRepository;
    }

    /** Pagamentos de uma factura, por ordem cronológica. */
    public List<PaymentEntry> listForInvoice(Long invoiceId) {
        if (invoiceId == null) {
            return List.of();
        }
        return paymentEntryRepository.findByInvoiceIdOrderByPaidAtAsc(invoiceId);
    }

    /** Pagamentos de facturas da empresa pagos no intervalo [from, to]. */
    public List<PaymentEntry> listPaidForCompanyBetween(Long companyId, LocalDateTime from, LocalDateTime to) {
        return paymentEntryRepository.findByInvoiceCompanyIdAndPaidAtBetween(companyId, from, to);
    }

    /** Vale de compras pelo id; "Vale de compras não encontrado." se não existir. */
    public StoreVoucher requireVoucher(Long voucherId) {
        Optional<StoreVoucher> voucher = voucherId == null ? Optional.empty() : storeVoucherRepository.findById(voucherId);
        return voucher.orElseThrow(() -> new BusinessRuleException("Vale de compras não encontrado."));
    }

    /** Vale de compras pelo código (sem filtro de empresa: o chamador valida o tenant). */
    public Optional<StoreVoucher> findVoucherByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return storeVoucherRepository.findByCode(code);
    }

    /** Transacção de pagamento móvel da empresa pelo id da transacção do operador. */
    public Optional<MobilePaymentTransaction> findMobilePaymentForCompany(String transactionId, Long companyId) {
        if (transactionId == null) {
            return Optional.empty();
        }
        return mobilePaymentRepository.findByTransactionIdAndCompanyId(transactionId, companyId);
    }
}
