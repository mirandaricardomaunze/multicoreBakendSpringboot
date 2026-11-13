package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.fleet.model.FleetManifest;
import mz.multicore.erp.modules.fleet.service.VehicleLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FleetManifestPrintService {

    private final VehicleLookup vehicleLookup;
    private final FleetManifestDocumentRenderer documentRenderer;

    @Autowired
    public FleetManifestPrintService(VehicleLookup vehicleLookup,
                                     FleetManifestDocumentRenderer documentRenderer) {
        this.vehicleLookup = vehicleLookup;
        this.documentRenderer = documentRenderer;
    }

    public FleetManifestPrintService(VehicleLookup vehicleLookup) {
        this(vehicleLookup, new FleetManifestDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] renderA4(Long manifestId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        FleetManifest manifest = vehicleLookup.requireManifestForCompany(manifestId, companyId);
        return documentRenderer.renderA4(manifest);
    }
}
