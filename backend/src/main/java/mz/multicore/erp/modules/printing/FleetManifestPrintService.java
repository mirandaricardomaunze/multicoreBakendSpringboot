package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.fleet.model.FleetManifest;
import mz.multicore.erp.modules.fleet.repository.FleetManifestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FleetManifestPrintService {

    private final FleetManifestRepository manifestRepository;
    private final FleetManifestDocumentRenderer documentRenderer;

    @Autowired
    public FleetManifestPrintService(FleetManifestRepository manifestRepository,
                                     FleetManifestDocumentRenderer documentRenderer) {
        this.manifestRepository = manifestRepository;
        this.documentRenderer = documentRenderer;
    }

    public FleetManifestPrintService(FleetManifestRepository manifestRepository) {
        this(manifestRepository, new FleetManifestDocumentRenderer());
    }

    @Transactional(readOnly = true)
    public byte[] renderA4(Long manifestId) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        FleetManifest manifest = manifestRepository.findByIdAndCompanyIdWithStops(manifestId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Manifesto de carga não encontrado: " + manifestId));
        return documentRenderer.renderA4(manifest);
    }
}
