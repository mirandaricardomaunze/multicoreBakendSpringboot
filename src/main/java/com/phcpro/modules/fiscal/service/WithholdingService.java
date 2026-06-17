package com.phcpro.modules.fiscal.service;

import com.phcpro.architecture.exception.BusinessRuleException;
import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.modules.company.repository.CompanyRepository;
import com.phcpro.modules.fiscal.dto.CreateWithholdingRequest;
import com.phcpro.modules.fiscal.dto.WithholdingRecordDTO;
import com.phcpro.modules.fiscal.model.WithholdingRecord;
import com.phcpro.modules.fiscal.repository.WithholdingRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class WithholdingService {

    private final WithholdingRecordRepository repository;
    private final CompanyRepository companyRepository;

    public WithholdingService(WithholdingRecordRepository repository, CompanyRepository companyRepository) {
        this.repository = repository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public WithholdingRecordDTO create(CreateWithholdingRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        var company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));

        WithholdingRecord r = new WithholdingRecord();
        r.setCompany(company);
        r.setRecordDate(request.recordDate());
        r.setBeneficiaryName(request.beneficiaryName());
        r.setBeneficiaryTaxId(request.beneficiaryTaxId());
        r.setServiceDescription(request.serviceDescription());
        r.setBaseAmount(request.baseAmount());
        r.setTaxRate(request.taxRate());
        r.setTaxCategory(request.taxCategory());

        BigDecimal withheld = request.baseAmount().multiply(request.taxRate())
                .setScale(2, RoundingMode.HALF_UP);
        r.setWithheldAmount(withheld);
        r.setNetPaid(request.baseAmount().subtract(withheld));
        r.setStatus("PENDING");
        r.setCreatedBy("SYSTEM");

        return toDTO(repository.save(r));
    }

    @Transactional
    public WithholdingRecordDTO markDelivered(Long id) {
        WithholdingRecord r = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Registo não encontrado."));
        CurrentUserContext.requireCompany(r.getCompany().getId());
        if ("DELIVERED".equals(r.getStatus())) {
            throw new BusinessRuleException("Já marcado como entregue.");
        }
        r.setStatus("DELIVERED");
        r.setDeliveredAt(LocalDate.now());
        return toDTO(repository.save(r));
    }

    @Transactional
    public void delete(Long id) {
        WithholdingRecord r = repository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("Registo não encontrado."));
        CurrentUserContext.requireCompany(r.getCompany().getId());
        if ("DELIVERED".equals(r.getStatus())) {
            throw new BusinessRuleException("Não é possível eliminar registos já entregues.");
        }
        repository.delete(r);
    }

    @Transactional(readOnly = true)
    public List<WithholdingRecordDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return repository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    private WithholdingRecordDTO toDTO(WithholdingRecord r) {
        return new WithholdingRecordDTO(
                r.getId(),
                r.getRecordDate(),
                r.getBeneficiaryName(),
                r.getBeneficiaryTaxId(),
                r.getServiceDescription(),
                r.getBaseAmount(),
                r.getTaxRate(),
                r.getTaxCategory(),
                r.getWithheldAmount(),
                r.getNetPaid(),
                r.getStatus(),
                r.getDeliveredAt()
        );
    }
}
