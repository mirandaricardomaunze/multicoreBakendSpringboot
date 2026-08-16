package mz.multicore.erp.modules.approvals.service;

public interface ApprovalCallback {
    boolean supports(String documentType);
    void onApproved(Long documentId);
    void onRejected(Long documentId, String reason);
}
