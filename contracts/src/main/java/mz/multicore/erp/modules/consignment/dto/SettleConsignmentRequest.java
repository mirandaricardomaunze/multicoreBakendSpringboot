package mz.multicore.erp.modules.consignment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record SettleConsignmentRequest(@NotNull @Positive Long companyId, @NotNull @Valid List<SettleLineRequest> lines) {}
