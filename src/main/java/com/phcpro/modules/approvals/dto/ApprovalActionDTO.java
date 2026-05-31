package com.phcpro.modules.approvals.dto;

import jakarta.validation.constraints.Size;

public record ApprovalActionDTO(
    @Size(max = 500, message = "Os comentários não podem exceder 500 caracteres.")
    String comments
) {}
