package mz.multicore.erp.modules.recurring.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.recurring.dto.CreateRecurringContractRequest;
import mz.multicore.erp.modules.recurring.dto.RecurringContractDTO;
import mz.multicore.erp.modules.recurring.model.BillingFrequency;
import mz.multicore.erp.modules.recurring.model.ContractStatus;
import mz.multicore.erp.modules.recurring.model.RecurringContract;
import mz.multicore.erp.modules.recurring.repository.RecurringContractRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Component
public class RecurringContractLifecycleService {

    private final RecurringContractRepository contractRepository;
    private final CompanyLookup companyLookup;
    private final ClientLookup clientLookup;

    public RecurringContractLifecycleService(
            RecurringContractRepository contractRepository,
            CompanyLookup companyLookup,
            ClientLookup clientLookup
    ) {
        this.contractRepository = contractRepository;
        this.companyLookup = companyLookup;
        this.clientLookup = clientLookup;
    }

    public List<RecurringContractDTO> listContracts(Long companyId) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        return contractRepository.findByCompanyIdWithClient(companyId).stream().map(this::toDTO).toList();
    }

    public RecurringContractDTO getContract(Long companyId, Long id) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        RecurringContract contract = contractRepository.findByIdAndCompanyIdWithClient(companyId, id)
                .orElseThrow(() -> new BusinessRuleException("Contrato não encontrado: " + id));
        return toDTO(contract);
    }

    public RecurringContractDTO createContract(Long companyId, CreateRecurringContractRequest request) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        String number = request.contractNumber().trim().toUpperCase();

        if (contractRepository.existsByCompanyIdAndContractNumber(companyId, number)) {
            throw new BusinessRuleException("Já existe um contrato registado com o número '" + number + "'.");
        }

        Company company = companyLookup.require(companyId);
        Client client = clientLookup.require(request.customerId());

        if (request.startDate() == null) {
            throw new BusinessRuleException("A data de início do contrato é obrigatória.");
        }
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("A data de término não pode ser anterior à data de início.");
        }

        RecurringContract contract = new RecurringContract();
        contract.setCompany(company);
        contract.setClient(client);
        contract.setContractNumber(number);
        contract.setDescription(request.description().trim());
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setFrequency(request.frequency() != null ? request.frequency() : BillingFrequency.MONTHLY);
        contract.setBillingDayOfMonth(request.billingDayOfMonth());
        contract.setFixedAmount(request.fixedAmount());
        contract.setStartDate(request.startDate());
        contract.setEndDate(request.endDate());
        contract.setAutoRenew(request.autoRenew());
        contract.setNextBillingDate(calculateInitialBillingDate(request.startDate(), request.billingDayOfMonth()));

        return toDTO(contractRepository.save(contract));
    }

    public RecurringContractDTO updateStatus(Long companyId, Long id, ContractStatus newStatus) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        if (newStatus == null) throw new BusinessRuleException("O novo estado do contrato é obrigatório.");

        RecurringContract contract = contractRepository.findByIdAndCompanyIdWithClient(companyId, id)
                .orElseThrow(() -> new BusinessRuleException("Contrato não encontrado: " + id));
        contract.setStatus(newStatus);
        return toDTO(contract);
    }

    private LocalDate calculateInitialBillingDate(LocalDate startDate, int targetDay) {
        YearMonth ym = YearMonth.from(startDate);
        int day = Math.min(targetDay, ym.lengthOfMonth());
        LocalDate candidate = ym.atDay(day);
        if (candidate.isBefore(startDate)) {
            YearMonth nextYm = ym.plusMonths(1);
            return nextYm.atDay(Math.min(targetDay, nextYm.lengthOfMonth()));
        }
        return candidate;
    }

    public RecurringContractDTO toDTO(RecurringContract c) {
        return new RecurringContractDTO(
                c.getId(), c.getCompany().getId(), c.getContractNumber(),
                c.getClient().getId(), c.getClient().getName(), c.getClient().getTaxId(),
                c.getDescription(), c.getStatus(), c.getFrequency(),
                c.getBillingDayOfMonth(), c.getFixedAmount(),
                c.getStartDate(), c.getEndDate(), c.getNextBillingDate(),
                c.getLastBilledDate(), c.isAutoRenew()
        );
    }
}
