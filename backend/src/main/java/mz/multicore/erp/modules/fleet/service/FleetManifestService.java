package mz.multicore.erp.modules.fleet.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.service.OrderLookup;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.fleet.dto.CreateFleetManifestRequest;
import mz.multicore.erp.modules.fleet.dto.FleetManifestDTO;
import mz.multicore.erp.modules.fleet.model.FleetManifest;
import mz.multicore.erp.modules.fleet.repository.FleetManifestRepository;
import mz.multicore.erp.modules.fleet.repository.VehicleRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Orquestrador tenant-scoped de manifestos de carga e rotas de distribuição da frota. */
@Service
public class FleetManifestService {

    private final FleetManifestBuilder manifestBuilder;
    private final FleetManifestQueryService queryService;

    @Autowired
    public FleetManifestService(FleetManifestBuilder manifestBuilder,
                                FleetManifestQueryService queryService) {
        this.manifestBuilder = manifestBuilder;
        this.queryService = queryService;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public FleetManifestService(FleetManifestRepository manifestRepository,
                                VehicleRepository vehicleRepository,
                                OrderLookup orderLookup,
                                CompanyLookup companyLookup,
                                DocumentNumberService documentNumberService) {
        this(new FleetManifestBuilder(manifestRepository, vehicleRepository, orderLookup,
                        companyLookup, documentNumberService),
                new FleetManifestQueryService(manifestRepository));
    }

    @Transactional
    public FleetManifestDTO createManifest(Long companyId, CreateFleetManifestRequest request) {
        Long resolvedCompanyId = companyId != null ? companyId : CurrentUserContext.getCurrentCompanyId();
        return queryService.toDTO(manifestBuilder.createManifest(resolvedCompanyId, request));
    }

    @Transactional(readOnly = true)
    public List<FleetManifestDTO> listManifests(Long companyId) {
        Long resolvedCompanyId = companyId != null ? companyId : CurrentUserContext.getCurrentCompanyId();
        return queryService.listManifests(resolvedCompanyId);
    }

    @Transactional(readOnly = true)
    public FleetManifestDTO getManifest(Long companyId, Long id) {
        Long resolvedCompanyId = companyId != null ? companyId : CurrentUserContext.getCurrentCompanyId();
        return queryService.getManifest(resolvedCompanyId, id);
    }

    @Transactional
    public FleetManifestDTO updateStatus(Long companyId, Long id, String status) {
        Long resolvedCompanyId = companyId != null ? companyId : CurrentUserContext.getCurrentCompanyId();
        return queryService.updateStatus(resolvedCompanyId, id, status);
    }

    public FleetManifestDTO toDTO(FleetManifest m) {
        return queryService.toDTO(m);
    }
}
