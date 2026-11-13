package mz.multicore.erp.modules.units.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.units.dto.CreateUnitOfMeasureRequest;
import mz.multicore.erp.modules.units.dto.UnitOfMeasureDTO;
import mz.multicore.erp.modules.units.model.UnitDimension;
import mz.multicore.erp.modules.units.model.UnitOfMeasure;
import mz.multicore.erp.modules.units.repository.UnitOfMeasureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Unit catalogue visible to a company: system units plus its own package units (SA-C02). */
@Service
public class UnitOfMeasureService {

    private static final Pattern CODE = Pattern.compile("[A-Z0-9_]{1,20}");

    private final UnitOfMeasureRepository unitRepository;
    private final CompanyLookup companyLookup;
    private final AuditLogService auditLogService;

    public UnitOfMeasureService(UnitOfMeasureRepository unitRepository, CompanyLookup companyLookup,
                                AuditLogService auditLogService) {
        this.unitRepository = unitRepository;
        this.companyLookup = companyLookup;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<UnitOfMeasureDTO> listVisible(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return unitRepository.findVisible(companyId).stream().map(UnitOfMeasureService::toDto).toList();
    }

    @Transactional
    public UnitOfMeasureDTO createCompanyPackage(CreateUnitOfMeasureRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        String code = normalizeCode(request.code());
        if (unitRepository.findByCodeAndCompanyIsNull(code).isPresent()
                || unitRepository.findByCodeAndCompanyId(code, request.companyId()).isPresent()) {
            throw new BusinessRuleException("Já existe uma unidade com o código " + code + ".");
        }
        Company company = companyLookup.require(request.companyId());
        UnitOfMeasure unit = new UnitOfMeasure();
        unit.setCompany(company);
        unit.setCode(code);
        unit.setName(request.name().trim());
        unit.setSymbol(request.symbol() == null || request.symbol().isBlank()
                ? code.toLowerCase(Locale.ROOT) : request.symbol().trim());
        unit.setDimension(UnitDimension.PACKAGE);
        unit.setDimensionFactor(null);
        unit.setDecimals(request.decimals());
        unit.setSystemUnit(false);
        unit.setActive(true);
        UnitOfMeasure saved = unitRepository.save(unit);
        auditLogService.logCurrent("UNIT_OF_MEASURE_CREATE", "Unidade criada: " + code + " (" + saved.getName() + ")");
        return toDto(saved);
    }

    /** System or company unit with this code; never a unit owned by another company. */
    @Transactional(readOnly = true)
    public UnitOfMeasure requireVisible(String code, Long companyId) {
        String normalized = normalizeCode(code);
        return unitRepository.findByCodeAndCompanyId(normalized, companyId)
                .or(() -> unitRepository.findByCodeAndCompanyIsNull(normalized))
                .filter(UnitOfMeasure::isActive)
                .orElseThrow(() -> new BusinessRuleException("Unidade de medida desconhecida: " + normalized));
    }

    static String normalizeCode(String raw) {
        if (raw == null || raw.isBlank()) throw new BusinessRuleException("O código da unidade é obrigatório.");
        String code = raw.trim().toUpperCase(Locale.ROOT);
        if (!CODE.matcher(code).matches()) {
            throw new BusinessRuleException("O código da unidade só pode ter letras, números e _ (até 20).");
        }
        return code;
    }

    public static UnitOfMeasureDTO toDto(UnitOfMeasure unit) {
        return new UnitOfMeasureDTO(unit.getId(), unit.getCode(), unit.getName(), unit.getSymbol(),
                unit.getDimension().name(), unit.getDimension().label(), unit.getDimensionFactor(),
                unit.getDecimals(), unit.isSystemUnit());
    }
}
