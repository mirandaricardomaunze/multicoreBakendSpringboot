package mz.multicore.erp.modules.expiry.dto;

import java.math.BigDecimal;
import java.util.List;

/** Lots to watch with the values the manager needs: what already expired, what is at risk this week, losses. */
public record ExpiryBoardDTO(List<ExpiryLotDTO> lots, BigDecimal expiredCostValue, BigDecimal atRiskCostValue,
                             long markedDownLots, BigDecimal expiryLossesThisMonth) {}
