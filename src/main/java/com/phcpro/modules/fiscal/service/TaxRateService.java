package com.phcpro.modules.fiscal.service;

import com.phcpro.architecture.exception.BusinessRuleException;
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

    public TaxRateService(TaxRateRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public TaxRateDTO create(CreateTaxRateRequest request) {
        if (repository.findByCode(request.code()).isPresent()) {
            throw new BusinessRuleException("Já existe uma taxa com o código " + request.code() + ".");
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
        return toDTO(repository.save(t));
    }

    @Transactional
    public TaxRateDTO update(Long id, CreateTaxRateRequest request) {
        TaxRate t = repository.findById(id)
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
        TaxRate t = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Taxa não encontrada."));
        t.setActive(false);
        repository.save(t);
    }

    @Transactional
    public void activate(Long id) {
        TaxRate t = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Taxa não encontrada."));
        t.setActive(true);
        repository.save(t);
    }

    @Transactional(readOnly = true)
    public List<TaxRateDTO> getAll() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<TaxRateDTO> getActive() {
        return repository.findByActiveTrueOrderByTypeAscRateDesc().stream().map(this::toDTO).toList();
    }

    private TaxRateDTO toDTO(TaxRate t) {
        return new TaxRateDTO(t.getId(), t.getCode(), t.getName(), t.getType().name(), t.getRate(), t.getLegalBasis(), t.isActive());
    }
}
