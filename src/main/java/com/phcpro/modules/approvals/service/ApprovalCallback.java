package com.phcpro.modules.approvals.service;

public interface ApprovalCallback {
    boolean supports(String documentType);
    void onApproved(Long documentId);
    void onRejected(Long documentId, String reason);
}
