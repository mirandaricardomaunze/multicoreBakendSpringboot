package mz.multicore.erp.modules.fiscal.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.fiscal.dto.CreateWithholdingRequest;
import mz.multicore.erp.modules.fiscal.dto.WithholdingRecordDTO;
import mz.multicore.erp.modules.fiscal.model.WithholdingRecord;
import mz.multicore.erp.modules.fiscal.repository.WithholdingRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class WithholdingService {

    private final WithholdingRecordRepository repository;
    private final CompanyService companyService;

    public WithholdingService(WithholdingRecordRepository repository, CompanyService companyService) {
        this.repository = repository;
        this.companyService = companyService;
    }

    @Transactional
    public WithholdingRecordDTO create(CreateWithholdingRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        var company = companyService.getCurrentCompanyReference(request.companyId());

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
