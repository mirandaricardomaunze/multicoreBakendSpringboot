package mz.multicore.erp.desktop.client;

import mz.multicore.erp.desktop.config.DesktopApiConfig;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.modules.licensing.dto.AcceptLicenseRequest;
import mz.multicore.erp.modules.licensing.dto.LicenseAcceptanceDTO;
import mz.multicore.erp.modules.licensing.dto.LicenseTermsDTO;

public class LicenseApiClient {

    public static final String REPRESENTATION_DECLARATION =
            "Li o contrato e declaro possuir poderes para o aceitar em nome da empresa.";

    private final DesktopApiClient api;

    public LicenseApiClient(DesktopApiConfig config, DesktopSession session) {
        this.api = new DesktopApiClient(config, session);
    }

    public LicenseTermsDTO currentTerms() {
        return api.get("/api/license/current", LicenseTermsDTO.class);
    }

    public LicenseAcceptanceDTO accept(LicenseTermsDTO terms) {
        return api.post("/api/license/accept",
                new AcceptLicenseRequest(terms.version(), terms.sha256(), true, REPRESENTATION_DECLARATION),
                LicenseAcceptanceDTO.class);
    }
}
