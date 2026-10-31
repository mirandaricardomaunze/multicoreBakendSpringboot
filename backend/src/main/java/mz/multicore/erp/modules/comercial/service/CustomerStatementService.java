package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Serviço de geração de extrato de conta corrente de clientes com saldo progressivo.
 * Fachada orquestradora que delega na coleta de movimentos e cálculo financeiro.
 * Ver {@code docs/CUSTOMER_STATEMENT_SERVICE_DECOMPOSITION_SPEC.md}.
 */
@Service
public class CustomerStatementService {

    private final ClientRepository clientRepository;
    private final CustomerStatementCollector collector;
    private final CustomerStatementCalculator calculator;

    @Autowired
    public CustomerStatementService(
            ClientRepository clientRepository,
            CustomerStatementCollector collector,
            CustomerStatementCalculator calculator
    ) {
        this.clientRepository = clientRepository;
        this.collector = collector;
        this.calculator = calculator;
    }

    /** Construtor retrocompatível (compatibilidade com suites de teste unitário via InjectMocks). */
    public CustomerStatementService(
            ClientRepository clientRepository,
            InvoiceRepository invoiceRepository,
            ReceiptRepository receiptRepository,
            CreditNoteRepository creditNoteRepository,
            DebitNoteRepository debitNoteRepository
    ) {
        this(clientRepository,
             new CustomerStatementCollector(invoiceRepository, receiptRepository, creditNoteRepository, debitNoteRepository),
             new CustomerStatementCalculator());
    }

    @Transactional(readOnly = true)
    public CustomerStatementDTO getStatement(Long clientId, LocalDate startDate, LocalDate endDate) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (clientId == null) {
            throw new BusinessRuleException("Identificador de cliente é obrigatório.");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado: " + clientId));

        LocalDate from = startDate != null ? startDate : LocalDate.now().withDayOfYear(1);
        LocalDate to = endDate != null ? endDate : LocalDate.now();

        CustomerStatementCollector.CollectedCustomerMovements collected = collector.collect(companyId, clientId, to);
        return calculator.calculate(client, from, to, collected.movements(), collected.overdueAmount());
    }
}
