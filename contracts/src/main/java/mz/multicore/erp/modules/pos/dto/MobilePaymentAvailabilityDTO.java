package mz.multicore.erp.modules.pos.dto;

/** Indica ao POS se pode oferecer o pagamento móvel simulado. */
public record MobilePaymentAvailabilityDTO(boolean simulationEnabled) {}
