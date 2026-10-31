package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.paging.PageQuery;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.architecture.paging.PageResponseMapper;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryTransactionDTO;
import mz.multicore.erp.modules.financeira.model.TreasuryAccountType;
import mz.multicore.erp.modules.financeira.repository.TreasuryAccountRepository;
import mz.multicore.erp.modules.financeira.repository.TreasuryTransactionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TreasuryQueryService {

    private final TreasuryAccountRepository accountRepository;
    private final TreasuryTransactionRepository transactionRepository;
    private final TreasuryOperationService operationService;

    public TreasuryQueryService(TreasuryAccountRepository accountRepository,
                                TreasuryTransactionRepository transactionRepository,
                                TreasuryOperationService operationService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.operationService = operationService;
    }

    public List<TreasuryAccountDTO> getAllAccounts() {
        return accountRepository.findByCompanyIdOrderByName(CurrentUserContext.getCurrentCompanyId()).stream()
                .map(a -> new TreasuryAccountDTO(a.getId(), a.getName(), a.getAccountNumber(),
                        a.getAccountType(), a.getBalance()))
                .collect(Collectors.toList());
    }

    public List<TreasuryTransactionDTO> getAllTransactions() {
        return transactionRepository.findByTreasuryAccountCompanyIdOrderByTransactionDateDesc(
                        CurrentUserContext.getCurrentCompanyId()).stream()
                .map(operationService::toDTO)
                .collect(Collectors.toList());
    }

    public PageResponse<TreasuryTransactionDTO> getTransactionsPage(Integer page, Integer size) {
        var pageable = PageQuery.of(page, size);
        var transactions = transactionRepository
                .findByTreasuryAccountCompanyIdOrderByTransactionDateDesc(
                        CurrentUserContext.requireCurrentCompanyId(), pageable);
        return PageResponseMapper.from(transactions, operationService::toDTO);
    }

    public TreasuryAccountType accountType(Long accountId) {
        return accountRepository.findByIdAndCompanyId(accountId, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Conta de tesouraria não encontrada."))
                .getAccountType();
    }
}
