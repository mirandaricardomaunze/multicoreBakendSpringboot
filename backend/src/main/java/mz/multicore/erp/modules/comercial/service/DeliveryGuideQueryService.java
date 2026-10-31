package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.quantity.LogisticsLoadCalculator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.DeliveryGuideDTO;
import mz.multicore.erp.modules.comercial.dto.DeliveryGuideLineDTO;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideLine;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Service
public class DeliveryGuideQueryService {
    private final DeliveryGuideRepository guideRepository;

    public DeliveryGuideQueryService(DeliveryGuideRepository guideRepository) {
        this.guideRepository = guideRepository;
    }

    @Transactional(readOnly = true)
    public List<DeliveryGuideDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return guideRepository.findByCompanyId(companyId).stream().map(DeliveryGuideQueryService::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public DeliveryGuideDTO findById(Long id) {
        return toDTO(load(id));
    }

    @Transactional(readOnly = true)
    public DeliveryGuide loadForPrint(Long id) {
        return load(id);
    }

    public DeliveryGuide load(Long id) {
        return guideRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Guia de remessa não encontrada."));
    }

    public static String clientLabel(DeliveryGuide guide) {
        if (guide.getWalkInName() != null && !guide.getWalkInName().isBlank()) {
            return guide.getWalkInName();
        }
        return guide.getClient() != null ? guide.getClient().getName() : "";
    }

    public static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    public static DeliveryGuideDTO toDTO(DeliveryGuide g) {
        var logistics = LogisticsLoadCalculator.calculate(g.getLines().stream()
                .map(line -> new LogisticsLoadCalculator.Input(
                        line.getQuantity(), line.getProduct().getGrossUnitWeightKg())).toList());
        List<DeliveryGuideLineDTO> lineDTOs = IntStream.range(0, g.getLines().size())
                .mapToObj(index -> {
                    DeliveryGuideLine l = g.getLines().get(index);
                    var share = logistics.get(index);
                    return new DeliveryGuideLineDTO(
                            l.getId(),
                            l.getProduct().getId(),
                            l.getProduct().getSku(),
                            l.getProduct().getName(),
                            l.getQuantity(),
                            l.getUnitPrice(),
                            l.getLineTotal(),
                            l.getBatchNumber(),
                            Math.max(1, l.getProduct().getUnitsPerBox()),
                            l.getProduct().getNetUnitWeightKg(), l.getProduct().getGrossUnitWeightKg(),
                            share.lineWeightKg(), share.quantityPercentage(), share.weightPercentage());
                }).toList();
        Long buId = null;
        String buName = null;
        if (g.getLines() != null && !g.getLines().isEmpty()) {
            Set<String> bus = new HashSet<>();
            for (DeliveryGuideLine l : g.getLines()) {
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

        return new DeliveryGuideDTO(
                g.getId(),
                g.getGuideNumber(),
                g.getGuideDate(),
                g.getCompany() != null ? g.getCompany().getId() : null,
                g.getOrderId(),
                g.getOrderNumber(),
                g.getClient() != null ? g.getClient().getId() : null,
                clientLabel(g),
                g.getWarehouse() != null ? g.getWarehouse().getId() : null,
                g.getWarehouse() != null ? g.getWarehouse().getName() : null,
                g.getStatus() != null ? g.getStatus().name() : null,
                g.getTotalAmount() != null ? g.getTotalAmount() : BigDecimal.ZERO,
                g.getResponsible(),
                g.getVehicle(),
                g.getNotes(),
                g.getApprovedBy(),
                g.getApprovedAt(),
                g.getRejectionReason(),
                lineDTOs,
                buId,
                buName
        );
    }
}
