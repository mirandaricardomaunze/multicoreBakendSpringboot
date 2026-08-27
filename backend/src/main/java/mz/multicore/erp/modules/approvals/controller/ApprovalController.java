package mz.multicore.erp.modules.approvals.controller;

import mz.multicore.erp.modules.approvals.dto.ApprovalActionDTO;
import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.approvals.service.ApprovalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalRequestDTO>> getPending() {
        return ResponseEntity.ok(approvalService.getPendingRequests());
    }

    @GetMapping
    public ResponseEntity<List<ApprovalRequestDTO>> getAll() {
        return ResponseEntity.ok(approvalService.getAllRequests());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApprovalRequestDTO> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalActionDTO action
    ) {
        String comments = action != null ? action.comments() : "Aprovado via API.";
        return ResponseEntity.ok(approvalService.approveRequest(id, comments));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApprovalRequestDTO> reject(
            @PathVariable Long id,
            @RequestBody @Valid ApprovalActionDTO action
    ) {
        String comments = action != null ? action.comments() : null;
        return ResponseEntity.ok(approvalService.rejectRequest(id, comments));
    }
}
