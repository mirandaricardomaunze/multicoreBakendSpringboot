package mz.multicore.erp.modules.fiscal.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.fiscal.dto.CreateTaxRateRequest;
import mz.multicore.erp.modules.fiscal.dto.TaxRateDTO;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.model.TaxType;
import mz.multicore.erp.modules.fiscal.repository.TaxRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaxRateService {

    private final TaxRateRepository repository;
    private final CompanyService companyService;

    public TaxRateService(TaxRateRepository repository, CompanyService companyService) {
        this.repository = repository;
        this.companyService = companyService;
    }

    @Transactional
    public TaxRateDTO create(CreateTaxRateRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (repository.findByCodeAndCompaniesId(request.code(), companyId).isPresent()) {
            throw new BusinessRuleException("Já existe uma taxa com o código " + request.code() + ".");
        }
        TaxRate shared = repository.findByCode(request.code()).orElse(null);
        if (shared != null) {
            shared.getCompanies().add(companyService.getCurrentCompanyReference(companyId));
            return toDTO(repository.save(shared));
        }
        TaxType type;
        try {
            type = TaxType.valueOf(request.type());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Tipo fiscal inválido: " + request.type());
        }
        requireLegalBasisWhenVatFree(type, request.rate(), request.legalBasis());
        TaxRate t = new TaxRate();
        t.setCode(request.code());
        t.setName(request.name());
        t.setType(type);
        t.setRate(request.rate());
        t.setLegalBasis(request.legalBasis());
        t.setActive(true);
        t.setCreatedBy("SYSTEM");
        t.getCompanies().add(companyService.getCurrentCompanyReference(companyId));
        return toDTO(repository.save(t));
    }

    @Transactional
    public TaxRateDTO update(Long id, CreateTaxRateRequest request) {
        TaxRate t = repository.findByIdAndCompaniesId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Taxa não encontrada."));
        TaxType type;
        try {
            type = TaxType.valueOf(request.type());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Tipo fiscal inválido: " + request.type());
        }
        requireLegalBasisWhenVatFree(type, request.rate(), request.legalBasis());
        t.setName(request.name());
        t.setType(type);
        t.setRate(request.rate());
        t.setLegalBasis(request.legalBasis());
        return toDTO(repository.save(t));
    }

    @Transactional
    public void deactivate(Long id) {
        TaxRate t = repository.findByIdAndCompaniesId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Taxa não encontrada."));
        t.setActive(false);
        repository.save(t);
    }

    @Transactional
    public void activate(Long id) {
        TaxRate t = repository.findByIdAndCompaniesId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Taxa não encontrada."));
        t.setActive(true);
        repository.save(t);
    }

    @Transactional(readOnly = true)
    public List<TaxRateDTO> getAll() {
        return repository.findDistinctByCompaniesIdOrderByTypeAscRateDesc(CurrentUserContext.getCurrentCompanyId())
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<TaxRateDTO> getActive() {
        return repository.findDistinctByCompaniesIdAndActiveTrueOrderByTypeAscRateDesc(
                CurrentUserContext.getCurrentCompanyId()).stream().map(this::toDTO).toList();
    }

    private TaxRateDTO toDTO(TaxRate t) {
        return new TaxRateDTO(t.getId(), t.getCode(), t.getName(), t.getType().name(), t.getRate(), t.getLegalBasis(), t.isActive());
    }

    /**
     * An invoice that does not charge VAT must state the legal provision that justifies it (Código do IVA),
     * and the printed reason comes from this field. Refuse a 0% VAT rate without one.
     */
    private static void requireLegalBasisWhenVatFree(TaxType type, java.math.BigDecimal rate, String legalBasis) {
        boolean vat = type == TaxType.IVA_STANDARD || type == TaxType.IVA_REDUCED
                || type == TaxType.IVA_ZERO || type == TaxType.IVA_EXEMPT;
        boolean vatFree = type == TaxType.IVA_ZERO || type == TaxType.IVA_EXEMPT || (rate != null && rate.signum() == 0);
        if (vat && vatFree && (legalBasis == null || legalBasis.isBlank())) {
            throw new BusinessRuleException("Indique a base legal da isenção (artigo, número e alínea do Código do IVA). "
                    + "É o motivo que a fatura imprime quando não cobra IVA.");
        }
    }
}
