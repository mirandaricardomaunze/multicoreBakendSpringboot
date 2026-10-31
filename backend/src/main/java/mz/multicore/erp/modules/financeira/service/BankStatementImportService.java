package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.financeira.dto.BankStatementDTO;
import mz.multicore.erp.modules.financeira.dto.BankStatementItemImportDTO;
import mz.multicore.erp.modules.financeira.dto.ImportBankStatementRequest;
import mz.multicore.erp.modules.financeira.model.BankReconciliationStatus;
import mz.multicore.erp.modules.financeira.model.BankStatement;
import mz.multicore.erp.modules.financeira.model.BankStatementItem;
import mz.multicore.erp.modules.financeira.model.BankStatementItemStatus;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.repository.BankStatementRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Importação e encerramento de extratos bancários de tesouraria.
 */
@Service
public class BankStatementImportService {

    private final BankStatementRepository statementRepository;
    private final TreasuryAccountRepository accountRepository;
    private final CompanyService companyService;
    private final BankReconciliationMatcher matcher;

    public BankStatementImportService(
            BankStatementRepository statementRepository,
            TreasuryAccountRepository accountRepository,
            CompanyService companyService,
            BankReconciliationMatcher matcher
    ) {
        this.statementRepository = statementRepository;
        this.accountRepository = accountRepository;
        this.companyService = companyService;
        this.matcher = matcher;
    }

    @Transactional
    public BankStatementDTO importStatement(ImportBankStatementRequest req) {
        if (req.treasuryAccountId() == null) {
            throw new BusinessRuleException("Conta de tesouraria obrigatória para importar o extracto.");
        }
        if (req.statementReference() == null || req.statementReference().isBlank()) {
            throw new BusinessRuleException("Referência do extracto é obrigatória.");
        }
        if (req.startDate() == null || req.endDate() == null) {
            throw new BusinessRuleException("Período (datas início e fim) obrigatório.");
        }
        if (req.endDate().isBefore(req.startDate())) {
            throw new BusinessRuleException("Data de fim não pode ser anterior à data de início.");
        }
        if (req.items() == null || req.items().isEmpty()) {
            throw new BusinessRuleException("O extracto deve conter pelo menos uma linha de movimento.");
        }

        Long companyId = CurrentUserContext.getCurrentCompanyId();
        TreasuryAccount account = accountRepository.findByIdAndCompanyId(req.treasuryAccountId(), companyId)
                .orElseThrow(() -> new BusinessRuleException("Conta bancária não encontrada na empresa actual."));

        Company company = companyService.getCompanyById(companyId);

        BankStatement statement = new BankStatement();
        statement.setCompany(company);
        statement.setTreasuryAccount(account);
        statement.setStatementReference(req.statementReference().trim());
        statement.setStartDate(req.startDate());
        statement.setEndDate(req.endDate());
        statement.setOpeningBalance(req.openingBalance() != null ? req.openingBalance() : BigDecimal.ZERO);
        statement.setClosingBalance(req.closingBalance() != null ? req.closingBalance() : BigDecimal.ZERO);
        statement.setStatus(BankReconciliationStatus.OPEN);
        statement.setCreatedAt(LocalDateTime.now());
        statement.setCreatedBy(CurrentUserContext.getUsername());

        for (BankStatementItemImportDTO itemReq : req.items()) {
            BankStatementItem item = new BankStatementItem();
            item.setTransactionDate(itemReq.transactionDate() != null ? itemReq.transactionDate() : req.startDate());
            item.setValueDate(itemReq.valueDate());
            item.setDescription(itemReq.description() != null ? itemReq.description().trim() : "(sem descrição)");
            item.setReference(itemReq.reference());
            item.setAmount(itemReq.amount() != null ? itemReq.amount() : BigDecimal.ZERO);
            item.setBalanceAfter(itemReq.balanceAfter());
            item.setStatus(BankStatementItemStatus.UNMATCHED);
            item.setCreatedAt(LocalDateTime.now());
            item.setCreatedBy(CurrentUserContext.getUsername());
            statement.addItem(item);
        }

        statement = statementRepository.save(statement);

        // Executar auto-matching preliminar
        matcher.autoMatchInternal(statement);

        return BankReconciliationQueryService.toDTO(statement);
    }

    @Transactional
    public BankStatementDTO closeStatement(Long statementId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        BankStatement statement = statementRepository.findByIdAndCompanyId(statementId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Extracto não encontrado: " + statementId));

        statement.setStatus(BankReconciliationStatus.CLOSED);
        statement.setUpdatedAt(LocalDateTime.now());
        statement = statementRepository.save(statement);

        return BankReconciliationQueryService.toDTO(statement);
    }
}
