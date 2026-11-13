package mz.multicore.erp.modules.activity.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.activity.dto.ActivityCatalogDTO;
import mz.multicore.erp.modules.activity.dto.ActivityOptionDTO;
import mz.multicore.erp.modules.activity.dto.CompanyActivityDTO;
import mz.multicore.erp.modules.activity.dto.UpdateCompanyActivityRequest;
import mz.multicore.erp.modules.activity.model.ActivityCapability;
import mz.multicore.erp.modules.activity.model.ActivityProfile;
import mz.multicore.erp.modules.activity.model.CompanyActivityProfile;
import mz.multicore.erp.modules.activity.repository.CompanyActivityProfileRepository;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Company activity profiles and the capabilities they enable (SA-C01).
 * A company without stored profiles is treated as "Comércio geral", the system's previous behaviour.
 */
@Service
public class CompanyActivityService {

    static final ActivityProfile DEFAULT_PROFILE = ActivityProfile.GENERAL_RETAIL;

    private final CompanyActivityProfileRepository profileRepository;
    private final CompanyLookup companyLookup;
    private final AuditLogService auditLogService;

    public CompanyActivityService(CompanyActivityProfileRepository profileRepository,
                                  CompanyLookup companyLookup,
                                  AuditLogService auditLogService) {
        this.profileRepository = profileRepository;
        this.companyLookup = companyLookup;
        this.auditLogService = auditLogService;
    }

    public ActivityCatalogDTO catalog() {
        List<ActivityOptionDTO> profiles = Arrays.stream(ActivityProfile.values())
                .map(p -> new ActivityOptionDTO(p.name(), p.label(), p.description(),
                        p.capabilities().stream().map(Enum::name).sorted().toList()))
                .toList();
        List<ActivityOptionDTO> capabilities = Arrays.stream(ActivityCapability.values())
                .map(c -> new ActivityOptionDTO(c.name(), c.label(), c.description(), List.of()))
                .toList();
        return new ActivityCatalogDTO(profiles, capabilities);
    }

    @Transactional(readOnly = true)
    public CompanyActivityDTO getForCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return toDto(companyId, storedProfiles(companyId));
    }

    @Transactional(readOnly = true)
    public boolean hasCapability(Long companyId, ActivityCapability capability) {
        return capabilitiesOf(effectiveProfiles(storedProfiles(companyId))).contains(capability);
    }

    @Transactional
    public CompanyActivityDTO update(UpdateCompanyActivityRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        Set<ActivityProfile> requested = parseProfiles(request.profiles());
        Company company = companyLookup.require(request.companyId());

        List<ActivityProfile> before = storedProfiles(company.getId());
        profileRepository.deleteByCompanyId(company.getId());
        for (ActivityProfile profile : requested) {
            CompanyActivityProfile row = new CompanyActivityProfile();
            row.setCompany(company);
            row.setProfile(profile);
            profileRepository.save(row);
        }
        List<ActivityProfile> after = new ArrayList<>(requested);
        auditLogService.logCurrent("COMPANY_ACTIVITY_PROFILES_UPDATE",
                "Perfis de actividade: " + labels(before) + " -> " + labels(after));
        return toDto(company.getId(), after);
    }

    static Set<ActivityProfile> parseProfiles(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            throw new BusinessRuleException("Escolha pelo menos um perfil de actividade.");
        }
        Set<ActivityProfile> parsed = EnumSet.noneOf(ActivityProfile.class);
        for (String code : codes) {
            if (code == null || code.isBlank()) continue;
            try {
                parsed.add(ActivityProfile.valueOf(code.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                throw new BusinessRuleException("Perfil de actividade desconhecido: " + code);
            }
        }
        if (parsed.isEmpty()) {
            throw new BusinessRuleException("Escolha pelo menos um perfil de actividade.");
        }
        return parsed;
    }

    static List<ActivityProfile> effectiveProfiles(List<ActivityProfile> stored) {
        return stored.isEmpty() ? List.of(DEFAULT_PROFILE) : stored;
    }

    static Set<ActivityCapability> capabilitiesOf(Collection<ActivityProfile> profiles) {
        Set<ActivityCapability> all = EnumSet.noneOf(ActivityCapability.class);
        profiles.forEach(p -> all.addAll(p.capabilities()));
        return all;
    }

    private List<ActivityProfile> storedProfiles(Long companyId) {
        if (companyId == null) return List.of();
        return profileRepository.findByCompanyIdOrderById(companyId).stream()
                .map(CompanyActivityProfile::getProfile).toList();
    }

    private static CompanyActivityDTO toDto(Long companyId, List<ActivityProfile> stored) {
        List<ActivityProfile> effective = effectiveProfiles(stored);
        return new CompanyActivityDTO(companyId, !stored.isEmpty(),
                effective.stream().map(Enum::name).toList(),
                capabilitiesOf(effective).stream().map(Enum::name).toList());
    }

    private static String labels(List<ActivityProfile> profiles) {
        if (profiles.isEmpty()) return "(nenhum)";
        return profiles.stream().map(ActivityProfile::label).collect(Collectors.joining(", "));
    }
}
