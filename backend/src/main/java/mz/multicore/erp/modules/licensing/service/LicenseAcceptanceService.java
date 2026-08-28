package mz.multicore.erp.modules.licensing.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.licensing.dto.AcceptLicenseRequest;
import mz.multicore.erp.modules.licensing.dto.LicenseAcceptanceDTO;
import mz.multicore.erp.modules.licensing.dto.LicenseTermsDTO;
import mz.multicore.erp.modules.licensing.model.LicenseAcceptance;
import mz.multicore.erp.modules.licensing.repository.LicenseAcceptanceRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class LicenseAcceptanceService {

    public static final String CURRENT_VERSION = "1.0";
    public static final String TITLE = "Contrato de Licença do Utilizador Final — Multicore";

    private final LicenseAcceptanceRepository repository;
    private final AuditLogService auditLogService;
    private final byte[] currentBytes;
    private final String currentContent;
    private final String currentSha256;

    public LicenseAcceptanceService(LicenseAcceptanceRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
        try {
            currentBytes = new ClassPathResource("legal/EULA-1.0.txt").getInputStream().readAllBytes();
            currentContent = new String(currentBytes, StandardCharsets.UTF_8);
            currentSha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(currentBytes));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível carregar o contrato de licença.", exception);
        }
    }

    @Transactional(readOnly = true)
    public LicenseTermsDTO currentTerms() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return repository.findByCompanyIdAndLicenseVersion(companyId, CURRENT_VERSION)
                .filter(acceptance -> currentSha256.equals(acceptance.getLicenseSha256()))
                .map(acceptance -> new LicenseTermsDTO(CURRENT_VERSION, TITLE, currentContent, currentSha256,
                        true, acceptance.getAcceptedBy(), acceptance.getAcceptedAt().toString()))
                .orElseGet(() -> new LicenseTermsDTO(CURRENT_VERSION, TITLE, currentContent, currentSha256,
                        false, null, null));
    }

    @Transactional
    public LicenseAcceptanceDTO accept(AcceptLicenseRequest request, String ipAddress,
                                        String clientVersion, String userAgent) {
        PermissionGuard.requireManagerOrAdmin("aceitar o contrato em nome da empresa");
        if (!request.authorizedRepresentative()) {
            throw new BusinessRuleException("Confirme que possui poderes para representar a empresa.");
        }
        if (!CURRENT_VERSION.equals(request.version()) || !currentSha256.equalsIgnoreCase(request.sha256())) {
            throw new BusinessRuleException("O contrato foi actualizado. Leia novamente a versão vigente.");
        }

        Long companyId = CurrentUserContext.getCurrentCompanyId();
        LicenseAcceptance acceptance = repository
                .findByCompanyIdAndLicenseVersion(companyId, CURRENT_VERSION)
                .orElseGet(LicenseAcceptance::new);
        if (acceptance.getId() == null) {
            acceptance.setCompanyId(companyId);
            acceptance.setLicenseVersion(CURRENT_VERSION);
            acceptance.setLicenseSha256(currentSha256);
            acceptance.setAcceptedBy(CurrentUserContext.getUsername());
            acceptance.setAcceptedAt(Instant.now());
            acceptance.setDeclaration(request.declaration().trim());
            acceptance.setIpAddress(trim(ipAddress, 64));
            acceptance.setClientVersion(trim(clientVersion, 40));
            acceptance.setUserAgent(trim(userAgent, 300));
            acceptance = repository.save(acceptance);
            auditLogService.logCurrent("LICENSE_ACCEPTED",
                    "Contrato " + CURRENT_VERSION + " aceite; SHA-256=" + currentSha256);
        }
        return toDTO(acceptance);
    }

    private LicenseAcceptanceDTO toDTO(LicenseAcceptance acceptance) {
        return new LicenseAcceptanceDTO(acceptance.getId(), acceptance.getCompanyId(),
                acceptance.getLicenseVersion(), acceptance.getLicenseSha256(), acceptance.getAcceptedBy(),
                acceptance.getAcceptedAt().toString(), acceptance.getClientVersion());
    }

    private String trim(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }
}
