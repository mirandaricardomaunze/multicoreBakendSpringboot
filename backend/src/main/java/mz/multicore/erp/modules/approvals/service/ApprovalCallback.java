package mz.multicore.erp.modules.approvals.service;

/**
 * Implemented by a module that owns an approvable document type, so it can finish or reject that document when
 * its approval request is decided without the approvals module depending on it.
 */
public interface ApprovalCallback {
    boolean supports(String documentType);
    void onApproved(Long documentId);
    void onRejected(Long documentId, String reason);
}
