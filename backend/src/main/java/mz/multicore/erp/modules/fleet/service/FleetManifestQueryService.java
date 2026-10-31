package mz.multicore.erp.modules.fleet.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.fleet.dto.FleetManifestDTO;
import mz.multicore.erp.modules.fleet.dto.FleetManifestStopDTO;
import mz.multicore.erp.modules.fleet.model.FleetManifest;
import mz.multicore.erp.modules.fleet.repository.FleetManifestRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** Consultas tenant-scoped, actualização de estado e projecção DTO de manifestos de carga. */
@Component
public class FleetManifestQueryService {

    private final FleetManifestRepository manifestRepository;

    public FleetManifestQueryService(FleetManifestRepository manifestRepository) {
        this.manifestRepository = manifestRepository;
    }

    public List<FleetManifestDTO> listManifests(Long companyId) {
        return manifestRepository.findByCompanyIdOrderByManifestDateDesc(companyId).stream()
                .map(this::toDTO)
                .toList();
    }

    public FleetManifestDTO getManifest(Long companyId, Long id) {
        FleetManifest manifest = manifestRepository.findByIdAndCompanyIdWithStops(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Manifesto de carga não encontrado: " + id));
        return toDTO(manifest);
    }

    public FleetManifestDTO updateStatus(Long companyId, Long id, String status) {
        FleetManifest manifest = manifestRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Manifesto de carga não encontrado: " + id));
        manifest.setStatus(status);
        return toDTO(manifestRepository.save(manifest));
    }

    public FleetManifestDTO toDTO(FleetManifest m) {
        List<FleetManifestStopDTO> stops = m.getStops() != null
                ? m.getStops().stream().map(s -> new FleetManifestStopDTO(
                        s.getSequence(),
                        s.getDocumentType(),
                        s.getDocumentId(),
                        s.getDocumentNumber(),
                        s.getClient() != null ? s.getClient().getId() : null,
                        s.getClientName(),
                        s.getDeliveryAddress(),
                        s.getContactPhone(),
                        s.getBoxCount(),
                        s.getPackageCount(),
                        s.getTotalAmount(),
                        s.getAmountToCollect(),
                        s.getPaymentTerms()
                )).toList()
                : List.of();

        return new FleetManifestDTO(
                m.getId(),
                m.getManifestNumber(),
                m.getCompany() != null ? m.getCompany().getId() : null,
                m.getManifestDate(),
                m.getStatus(),
                m.getVehicle() != null ? m.getVehicle().getId() : null,
                m.getVehiclePlate(),
                m.getVehicleModel(),
                m.getDriverName(),
                m.getDriverPhone(),
                m.getRouteName(),
                m.getDepartureTime(),
                m.getNotes(),
                m.getTotalStops(),
                m.getTotalBoxes(),
                m.getTotalPackages(),
                m.getTotalAmount(),
                m.getTotalAmountToCollect(),
                stops
        );
    }
}
