package com.phcpro.modules.hr.service;

import com.phcpro.modules.approvals.service.ApprovalCallback;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.hr.model.ExpenseClaim;
import com.phcpro.modules.hr.model.ExpenseStatus;
import com.phcpro.modules.hr.repository.ExpenseClaimRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExpenseApprovalCallback implements ApprovalCallback {

    private final ExpenseClaimRepository expenseClaimRepository;
    private final FinanceService financeService;

    public ExpenseApprovalCallback(
            ExpenseClaimRepository expenseClaimRepository,
            FinanceService financeService
    ) {
        this.expenseClaimRepository = expenseClaimRepository;
        this.financeService = financeService;
    }

    @Override
    public boolean supports(String documentType) {
        return "EXPENSE".equalsIgnoreCase(documentType);
    }

    @Override
    @Transactional
    public void onApproved(Long documentId) {
        expenseClaimRepository.findById(documentId).ifPresent(claim -> {
            claim.setStatus(ExpenseStatus.APPROVED);
            expenseClaimRepository.save(claim);
            // Trigger automatic cash flow outflow (reimbursement payment)
            financeService.registerAutoExpensePayout(
                    claim.getAmount(),
                    "Reembolso Despesa #" + claim.getId() + " - Colaborador: " + claim.getEmployee().getName()
            );
        });
    }

    @Override
    @Transactional
    public void onRejected(Long documentId, String reason) {
        expenseClaimRepository.findById(documentId).ifPresent(claim -> {
            claim.setStatus(ExpenseStatus.REJECTED);
            claim.setRejectionReason(reason);
            expenseClaimRepository.save(claim);
        });
    }
}
