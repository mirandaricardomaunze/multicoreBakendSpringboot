package mz.multicore.erp.modules.printing;

import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** Contas bancárias que a empresa indica para pagamento, sem dados fixos nos documentos. */
@Component
public class CompanyBankDetails {

    private final TreasuryAccountRepository treasuryAccountRepository;

    public CompanyBankDetails(TreasuryAccountRepository treasuryAccountRepository) {
        this.treasuryAccountRepository = treasuryAccountRepository;
    }

    List<String> forCompany(Long companyId) {
        return treasuryAccountRepository.findByCompanyIdOrderByName(companyId).stream()
                .filter(account -> account.getAccountType() == TreasuryAccountType.BANK)
                .filter(account -> account.getAccountNumber() != null && !account.getAccountNumber().isBlank())
                .map(account -> account.getName() + " · Conta: " + account.getAccountNumber().trim())
                .toList();
    }
}
