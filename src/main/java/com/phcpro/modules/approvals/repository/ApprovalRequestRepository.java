package com.phcpro.modules.approvals.repository;

import com.phcpro.modules.approvals.model.ApprovalRequest;
import com.phcpro.modules.approvals.model.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByStatus(ApprovalStatus status);
    List<ApprovalRequest> findBySubmitter(String submitter);
    List<ApprovalRequest> findByCompanyIdAndStatus(Long companyId, ApprovalStatus status);
    List<ApprovalRequest> findByCompanyIdOrderByCreatedAtDesc(Long companyId);
    java.util.Optional<ApprovalRequest> findByIdAndCompanyId(Long id, Long companyId);
}
