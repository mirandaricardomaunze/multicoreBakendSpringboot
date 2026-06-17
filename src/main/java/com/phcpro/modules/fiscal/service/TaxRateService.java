package com.phcpro.modules.fiscal.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.fiscal.dto.CreateTaxRateRequest;
import com.phcpro.modules.fiscal.dto.TaxRateDTO;
import com.phcpro.modules.fiscal.model.TaxRate;
import com.phcpro.modules.fiscal.model.TaxType;
import com.phcpro.modules.fiscal.repository.TaxRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaxRateService {

    private final TaxRateRepository repository;
    private final CompanyRepository companyRepository;

    public TaxRateService(TaxRateRepository repository, CompanyRepository companyRepository) {
        this.repository = repository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public TaxRateDTO create(CreateTaxRateRequest request) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (repository.findByCodeAndCompaniesId(request.code(), companyId).isPresent()) {
            throw new BusinessRuleException("Já existe uma taxa com o código " + request.code() + ".");
        }
        TaxRate shared = repository.findByCode(request.code()).orElse(null);
        if (shared != null) {
            shared.getCompanies().add(companyRepository.getReferenceById(companyId));
            return toDTO(repository.save(shared));
        }
        TaxType type;
        try {
            type = TaxType.valueOf(request.type());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Tipo fiscal inválido: " + request.type());
        }
        TaxRate t = new TaxRate();
        t.setCode(request.code());
        t.setName(request.name());
        t.setType(type);
        t.setRate(request.rate());
        t.setLegalBasis(request.legalBasis());
        t.setActive(true);
        t.setCreatedBy("SYSTEM");
        t.getCompanies().add(companyRepository.getReferenceById(companyId));
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
}
