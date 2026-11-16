package mz.multicore.erp.modules.pickup.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.service.CreditNoteLineGuard;
import mz.multicore.erp.modules.pickup.model.MaterialPickupStatus;
import mz.multicore.erp.modules.pickup.model.PickupAccountStatus;
import mz.multicore.erp.modules.pickup.repository.MaterialPickupRepository;
import mz.multicore.erp.modules.pickup.repository.PickupAccountRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * A credit note cannot give back material the client already collected from the yard: only what is still waiting to
 * be picked up can be credited ({@code docs/MATERIAL_POR_LEVANTAR_SPEC.md}). Invoices without a pickup account are
 * not affected.
 */
@Component
public class PickupCreditNoteGuard implements CreditNoteLineGuard {

    private final PickupAccountRepository accountRepository;
    private final MaterialPickupRepository pickupRepository;

    public PickupCreditNoteGuard(PickupAccountRepository accountRepository, MaterialPickupRepository pickupRepository) {
        this.accountRepository = accountRepository;
        this.pickupRepository = pickupRepository;
    }

    @Override
    public void check(Invoice invoice, InvoiceLine line, BigDecimal requested, BigDecimal creditable) {
        var account = accountRepository.findByInvoiceId(invoice.getId())
                .filter(a -> a.getStatus() != PickupAccountStatus.CANCELLED).orElse(null);
        if (account == null) return;
        BigDecimal picked = BigDecimal.ZERO;
        for (Object[] row : pickupRepository.pickedByInvoiceLine(account.getId(), MaterialPickupStatus.ISSUED)) {
            if (line.getId().equals(row[0])) picked = (BigDecimal) row[1];
        }
        BigDecimal stillInYard = creditable.subtract(picked).max(BigDecimal.ZERO);
        if (requested.compareTo(stillInYard) > 0) {
            throw new BusinessRuleException("O cliente já levantou " + DisplayNumbers.qty(picked) + " de '" + line.getProduct().getName()
                    + "'. Só pode creditar o que ainda está por levantar: " + DisplayNumbers.qty(stillInYard) + ".");
        }
    }
}
