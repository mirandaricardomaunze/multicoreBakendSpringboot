package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.modules.inventory.dto.StockTransferDTO;
import mz.multicore.erp.modules.inventory.dto.StockTransferLineDTO;
import mz.multicore.erp.modules.inventory.model.StockTransfer;
import mz.multicore.erp.modules.inventory.model.StockTransferLine;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Conversão sem estado {@link StockTransfer} → {@link StockTransferDTO}.
 *
 * <p>Fonte única partilhada por {@link StockTransferService} e {@link StockTransferTransitService},
 * para que nenhum dependa do outro só para devolver um DTO.
 */
public final class StockTransferDtoMapper {

    private StockTransferDtoMapper() {
    }

    public static StockTransferDTO toDTO(StockTransfer t) {
        List<StockTransferLineDTO> lineDTOs = t.getLines().stream()
                .map(l -> {
                    var p = l.getProduct();
                    BigDecimal qty = l.getQuantity() != null ? l.getQuantity() : BigDecimal.ZERO;
                    String ref = p != null && p.getReference() != null && !p.getReference().isBlank()
                            ? p.getReference() : (p != null ? p.getSku() : null);
                    String barcode = p != null ? p.getBarcode() : null;
                    int pkgs = p != null && p.getPackagesPerBox() > 0 ? p.getPackagesPerBox() : 1;
                    int units = p != null && p.getUnitsPerPackage() > 0 ? p.getUnitsPerPackage() : 1;
                    BigDecimal price = (p != null && p.effectiveUnitPrice(qty) != null)
                            ? p.effectiveUnitPrice(qty) : BigDecimal.ZERO;
                    BigDecimal tax = p != null ? p.effectiveTaxRate() : BigDecimal.ZERO;
                    return new StockTransferLineDTO(
                            l.getId(),
                            p != null ? p.getId() : null,
                            p != null ? p.getSku() : null,
                            ref,
                            barcode,
                            p != null ? p.getName() : null,
                            qty,
                            l.getBatchNumber(),
                            pkgs,
                            units,
                            price,
                            tax,
                            l.getReceivedQuantity(),
                            l.getReturnedQuantity(),
                            l.getDiscrepancyReason()
                    );
                }).toList();
        Long buId = null;
        String buName = null;
        if (t.getLines() != null && !t.getLines().isEmpty()) {
            Set<String> bus = new HashSet<>();
            for (StockTransferLine l : t.getLines()) {
                if (l.getProduct() != null && l.getProduct().getBusinessUnit() != null) {
                    bus.add(l.getProduct().getBusinessUnit().getName());
                    if (buId == null) {
                        buId = l.getProduct().getBusinessUnit().getId();
                        buName = l.getProduct().getBusinessUnit().getName();
                    }
                }
            }
            if (bus.size() > 1) {
                buId = null;
                buName = "Misto";
            }
        }

        return new StockTransferDTO(
                t.getId(),
                t.getTransferNumber(),
                t.getTransferDate(),
                t.getCompany() != null ? t.getCompany().getId() : null,
                t.getOriginWarehouse().getId(),
                t.getOriginWarehouse().getName(),
                t.getDestinationWarehouse().getId(),
                t.getDestinationWarehouse().getName(),
                t.getStatus() != null ? t.getStatus().name() : null,
                t.getResponsible(),
                t.getVehicle(),
                t.getNotes(),
                t.getApprovedBy(),
                t.getApprovedAt(),
                t.getRejectionReason(),
                lineDTOs,
                t.getOrderId(),
                t.getOrderNumber(),
                t.getDriverName(),
                t.getVehiclePlate(),
                t.getVersion(),
                buId,
                buName,
                t.isRequiresReceipt(),
                t.getDispatchedAt(),
                t.getReceivedBy(),
                t.getReceivedAt(),
                t.getReceiptNotes()
        );
    }
}
