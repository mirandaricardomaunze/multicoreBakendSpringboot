package com.phcpro.modules.approvals.repository;

import com.phcpro.modules.approvals.model.ApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalHistoryRepository extends JpaRepository<ApprovalHistory, Long> {
    List<ApprovalHistory> findByApprovalRequestId(Long requestId);
}
