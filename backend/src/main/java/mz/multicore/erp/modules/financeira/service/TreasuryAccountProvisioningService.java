package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.financeira.model.TreasuryAccount;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Opens treasury accounts for a company. A BANK account with a number is what the invoice
 * renderer prints as payment details ({@code CompanyBankDetails}), so opening one during
 * onboarding makes the first invoice already carry the company's bank data.
 */
@Service
public class TreasuryAccountProvisioningService {

    private final TreasuryAccountRepository treasuryAccountRepository;

    public TreasuryAccountProvisioningService(TreasuryAccountRepository treasuryAccountRepository) {
        this.treasuryAccountRepository = treasuryAccountRepository;
    }

    /** Opens a bank account with a zero balance. The caller is responsible for authorisation. */
    @Transactional
    public Long openBankAccount(Company company, String bankName, String accountNumber) {
        return openBankAccount(company, bankName, accountNumber, BigDecimal.ZERO);
    }

    /** Opens a bank account with the given opening balance (e.g. demo data). */
    @Transactional
    public Long openBankAccount(Company company, String bankName, String accountNumber, BigDecimal openingBalance) {
        if (company == null || company.getId() == null) {
            throw new BusinessRuleException("Empresa não encontrada.");
        }
        if (bankName == null || bankName.isBlank()) {
            throw new BusinessRuleException("Indique o banco.");
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new BusinessRuleException("Indique o número da conta ou NIB.");
        }
        TreasuryAccount account = new TreasuryAccount();
        account.setName(bankName.trim());
        account.setAccountNumber(accountNumber.trim());
        account.setAccountType(TreasuryAccountType.BANK);
        account.setBalance(openingBalance == null ? BigDecimal.ZERO : openingBalance);
        account.setCompany(company);
        account.setCreatedBy(CurrentUserContext.getUsername());
        return treasuryAccountRepository.save(account).getId();
    }
}
