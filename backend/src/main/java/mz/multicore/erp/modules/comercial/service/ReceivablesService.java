package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.dto.AgingSummaryDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contas a receber: quem deve, quanto e <b>há quanto tempo</b>.
 */
@Service
public class ReceivablesService {

    private final ClientCreditLimitValidator creditLimitValidator;
    private final ReceivablesAgingCalculator agingCalculator;

    @Autowired
    public ReceivablesService(ClientCreditLimitValidator creditLimitValidator,
                              ReceivablesAgingCalculator agingCalculator) {
        this.creditLimitValidator = creditLimitValidator;
        this.agingCalculator = agingCalculator;
    }

    public ReceivablesService(InvoiceRepository invoiceRepository) {
        this(new ClientCreditLimitValidator(invoiceRepository),
             new ReceivablesAgingCalculator(invoiceRepository));
    }

    @Transactional(readOnly = true)
    public AgingSummaryDTO getAging() {
        return getAging(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public BigDecimal outstandingTotalFor(Long clientId) {
        return creditLimitValidator.outstandingTotalFor(clientId);
    }

    @Transactional(readOnly = true)
    public void assertCreditAvailable(Client client, BigDecimal newDebt) {
        creditLimitValidator.assertCreditAvailable(client, newDebt);
    }

    @Transactional(readOnly = true)
    public AgingSummaryDTO getAging(LocalDate referenceDate) {
        return agingCalculator.getAging(referenceDate);
    }
}
