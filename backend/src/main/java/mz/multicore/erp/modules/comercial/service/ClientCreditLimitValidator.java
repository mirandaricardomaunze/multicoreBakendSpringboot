package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ClientCreditLimitValidator {

    private final InvoiceRepository invoiceRepository;

    public ClientCreditLimitValidator(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public BigDecimal outstandingTotalFor(Long clientId) {
        if (clientId == null) return BigDecimal.ZERO;
        return invoiceRepository.sumOutstandingByClient(CurrentUserContext.requireCurrentCompanyId(),
                clientId, InvoiceStatus.collectableStatuses());
    }

    public void assertCreditAvailable(Client client, BigDecimal newDebt) {
        if (client == null || !client.hasCreditLimit()) return;
        if (newDebt == null || newDebt.signum() <= 0) return;

        BigDecimal currentDebt = outstandingTotalFor(client.getId());
        if (!client.exceedsCreditLimit(currentDebt, newDebt)) return;

        BigDecimal available = client.creditAvailable(currentDebt);
        throw new BusinessRuleException(String.format(
                "Limite de crédito excedido para %s. Limite: %s MT · Em dívida: %s MT · "
                        + "Disponível: %s MT · Esta venda a crédito: %s MT.",
                client.getName(), money(client.getCreditLimit()), money(currentDebt),
                money(available), money(newDebt)));
    }

    private static String money(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value;
        return DisplayNumbers.money(safe);
    }
}
