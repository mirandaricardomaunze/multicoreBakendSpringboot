package mz.multicore.erp.modules.fleet.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.fleet.model.FleetManifest;
import mz.multicore.erp.modules.fleet.model.FuelLog;
import mz.multicore.erp.modules.fleet.model.Vehicle;
import mz.multicore.erp.modules.fleet.repository.FleetManifestRepository;
import mz.multicore.erp.modules.fleet.repository.FuelLogRepository;
import mz.multicore.erp.modules.fleet.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Porta de leitura da frota para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2): viaturas e, por
 * serem do agregado mais próximo, abastecimentos e manifestos de carga.
 */
@Service
@Transactional(readOnly = true)
public class VehicleLookup {

    private static final String MANIFEST_NOT_FOUND = "Manifesto de carga não encontrado.";

    private final VehicleRepository vehicleRepository;
    private final FuelLogRepository fuelLogRepository;
    private final FleetManifestRepository manifestRepository;

    public VehicleLookup(VehicleRepository vehicleRepository,
                         FuelLogRepository fuelLogRepository,
                         FleetManifestRepository manifestRepository) {
        this.vehicleRepository = vehicleRepository;
        this.fuelLogRepository = fuelLogRepository;
        this.manifestRepository = manifestRepository;
    }

    /** Viaturas da empresa, ordenadas pela matrícula. */
    public List<Vehicle> listForCompany(Long companyId) {
        if (companyId == null) {
            return List.of();
        }
        return vehicleRepository.findByCompanyIdOrderByLicensePlateAsc(companyId);
    }

    /** Abastecimentos da viatura, do mais recente para o mais antigo. */
    public List<FuelLog> listFuelLogsForVehicle(Long vehicleId) {
        if (vehicleId == null) {
            return List.of();
        }
        return fuelLogRepository.findByVehicleIdOrderByLogTimestampDesc(vehicleId);
    }

    /** Manifesto de carga da empresa com as paragens carregadas. */
    public FleetManifest requireManifestForCompany(Long manifestId, Long companyId) {
        if (manifestId == null || companyId == null) {
            throw new BusinessRuleException(MANIFEST_NOT_FOUND);
        }
        return manifestRepository.findByIdAndCompanyIdWithStops(manifestId, companyId)
                .orElseThrow(() -> new BusinessRuleException(MANIFEST_NOT_FOUND));
    }
}
