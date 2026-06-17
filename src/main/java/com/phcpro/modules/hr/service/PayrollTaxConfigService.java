package com.phcpro.modules.hr.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.hr.dto.PayrollTaxConfigDTO;
import com.phcpro.modules.hr.dto.SavePayrollTaxConfigRequest;
import com.phcpro.modules.hr.model.PayrollIrpsBracket;
import com.phcpro.modules.hr.model.PayrollTaxConfig;
import com.phcpro.modules.hr.repository.PayrollTaxConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PayrollTaxConfigService {
    private final PayrollTaxConfigRepository repository;
    private final CompanyRepository companyRepository;

    public PayrollTaxConfigService(PayrollTaxConfigRepository repository, CompanyRepository companyRepository) {
        this.repository = repository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<PayrollTaxConfigDTO> list() {
        return repository.findAllByCompanyId(CurrentUserContext.getCurrentCompanyId()).stream().map(this::toDTO).toList();
    }

    @Transactional
    public PayrollTaxConfigDTO create(SavePayrollTaxConfigRequest request) {
        ensureAdmin();
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new BusinessRuleException("A data final da configuração fiscal é inválida.");
        }
        PayrollTaxConfig config = new PayrollTaxConfig();
        config.setCompany(companyRepository.findById(CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa ativa não encontrada.")));
        config.setName(request.name().trim());
        config.setEffectiveFrom(request.effectiveFrom());
        config.setEffectiveTo(request.effectiveTo());
        config.setEmployeeInssRate(request.employeeInssRate());
        config.setEmployerInssRate(request.employerInssRate());
        config.setLegalBasis(request.legalBasis());
        config.setActive(true);
        request.brackets().forEach(b -> {
            if (b.upperBound() != null && b.upperBound().compareTo(b.lowerBound()) < 0) {
                throw new BusinessRuleException("Existe um escalão IRPS com limite superior inválido.");
            }
            PayrollIrpsBracket bracket = new PayrollIrpsBracket();
            bracket.setConfig(config);
            bracket.setLowerBound(b.lowerBound());
            bracket.setUpperBound(b.upperBound());
            bracket.setRate(b.rate());
            bracket.setFixedDeduction(b.fixedDeduction());
            bracket.setDependentDeduction(b.dependentDeduction());
            config.getBrackets().add(bracket);
        });
        return toDTO(repository.save(config));
    }

    private PayrollTaxConfigDTO toDTO(PayrollTaxConfig config) {
        return new PayrollTaxConfigDTO(
                config.getId(), config.getName(), config.getEffectiveFrom(), config.getEffectiveTo(),
                config.getEmployeeInssRate(), config.getEmployerInssRate(), config.getLegalBasis(), config.isActive(),
                config.getBrackets().stream().map(b -> new PayrollTaxConfigDTO.BracketDTO(
                        b.getLowerBound(), b.getUpperBound(), b.getRate(), b.getFixedDeduction(), b.getDependentDeduction()
                )).toList()
        );
    }

    private void ensureAdmin() {
        if (!"ADMIN".equalsIgnoreCase(CurrentUserContext.getRole())) {
            throw new BusinessRuleException("Apenas administradores podem alterar configurações fiscais salariais.");
        }
    }
}
