package mz.multicore.erp.modules.fleet.dto;

import java.math.BigDecimal;

/**
 * Ponto de entrega / parada dentro de um Manifesto de Carga e Rota.
 */
public record FleetManifestStopDTO(
        int sequence,
        String documentType,
        Long documentId,
        String documentNumber,
        Long clientId,
        String clientName,
        String deliveryAddress,
        String contactPhone,
        BigDecimal boxCount,
        BigDecimal packageCount,
        BigDecimal totalAmount,
        BigDecimal amountToCollect,
        String paymentTerms
) {
    public FleetManifestStopDTO {
        if (boxCount == null) boxCount = BigDecimal.ZERO.setScale(2);
        if (packageCount == null) packageCount = BigDecimal.ZERO.setScale(2);
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (amountToCollect == null) amountToCollect = BigDecimal.ZERO;
    }
}
