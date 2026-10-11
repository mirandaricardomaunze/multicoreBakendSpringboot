package mz.multicore.erp.modules.recurring.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.recurring.dto.BatchBillingResultDTO;
import mz.multicore.erp.modules.recurring.dto.CreateRecurringContractRequest;
import mz.multicore.erp.modules.recurring.dto.RecurringContractDTO;
import mz.multicore.erp.modules.recurring.model.BillingFrequency;
import mz.multicore.erp.modules.recurring.model.ContractStatus;
import mz.multicore.erp.modules.recurring.model.RecurringBillingLog;
import mz.multicore.erp.modules.recurring.model.RecurringContract;
import mz.multicore.erp.modules.recurring.repository.RecurringBillingLogRepository;
import mz.multicore.erp.modules.recurring.repository.RecurringContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecurringContractService {

    private final RecurringContractRepository contractRepository;
    private final RecurringBillingLogRepository billingLogRepository;
    private final CompanyRepository companyRepository;
    private final ClientRepository clientRepository;

    public RecurringContractService(RecurringContractRepository contractRepository,
                                    RecurringBillingLogRepository billingLogRepository,
                                    CompanyRepository companyRepository,
                                    ClientRepository clientRepository) {
        this.contractRepository = contractRepository;
        this.billingLogRepository = billingLogRepository;
        this.companyRepository = companyRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<RecurringContractDTO> listContracts(Long companyId) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        return contractRepository.findByCompanyIdWithClient(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecurringContractDTO getContract(Long companyId, Long id) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        RecurringContract contract = contractRepository.findByIdAndCompanyIdWithClient(companyId, id)
                .orElseThrow(() -> new BusinessRuleException("Contrato não encontrado: " + id));
        return toDTO(contract);
    }

    @Transactional
    public RecurringContractDTO createContract(Long companyId, CreateRecurringContractRequest request) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        String number = request.contractNumber().trim().toUpperCase();

        if (contractRepository.existsByCompanyIdAndContractNumber(companyId, number)) {
            throw new BusinessRuleException("Já existe um contrato registado com o número '" + number + "'.");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada: " + companyId));

        Client client = clientRepository.findById(request.customerId())
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado: " + request.customerId()));

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

        LocalDate initialNextBilling = calculateInitialBillingDate(request.startDate(), request.billingDayOfMonth());
        contract.setNextBillingDate(initialNextBilling);

        RecurringContract saved = contractRepository.save(contract);
        return toDTO(saved);
    }

    @Transactional
    public RecurringContractDTO updateStatus(Long companyId, Long id, ContractStatus newStatus) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        if (newStatus == null) throw new BusinessRuleException("O novo estado do contrato é obrigatório.");

        RecurringContract contract = contractRepository.findByIdAndCompanyIdWithClient(companyId, id)
                .orElseThrow(() -> new BusinessRuleException("Contrato não encontrado: " + id));

        contract.setStatus(newStatus);
        return toDTO(contract);
    }

    @Transactional
    public BatchBillingResultDTO processBatchBilling(Long companyId, LocalDate referenceDate) {
        if (companyId == null) throw new BusinessRuleException("A empresa activa é obrigatória.");
        LocalDate effectiveRef = (referenceDate != null) ? referenceDate : LocalDate.now();

        List<RecurringContract> dueContracts = contractRepository.findDueContracts(
                companyId, ContractStatus.ACTIVE, effectiveRef);

        List<String> generatedInvoices = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        BigDecimal totalBilled = BigDecimal.ZERO;
        int processedCount = 0;

        for (RecurringContract contract : dueContracts) {
            try {
                processedCount++;
                String invoiceNumber = "FT-" + contract.getContractNumber() + "-" + effectiveRef.toString().replace("-", "");

                RecurringBillingLog log = new RecurringBillingLog();
                log.setContract(contract);
                log.setBillingDate(effectiveRef);
                log.setBilledAmount(contract.getFixedAmount());
                log.setInvoiceNumber(invoiceNumber);
                log.setStatus("SUCCESS");
                log.setNotes("Facturação periódica emitida para o contrato " + contract.getContractNumber());

                billingLogRepository.save(log);

                contract.setLastBilledDate(effectiveRef);

                LocalDate nextDate = computeNextBillingDate(
                        contract.getNextBillingDate(),
                        contract.getFrequency(),
                        contract.getBillingDayOfMonth()
                );

                if (contract.getEndDate() != null && nextDate.isAfter(contract.getEndDate())) {
                    if (contract.isAutoRenew()) {
                        contract.setNextBillingDate(nextDate);
                    } else {
                        contract.setStatus(ContractStatus.EXPIRED);
                        contract.setNextBillingDate(nextDate);
                    }
                } else {
                    contract.setNextBillingDate(nextDate);
                }

                contractRepository.save(contract);

                totalBilled = totalBilled.add(contract.getFixedAmount());
                generatedInvoices.add(invoiceNumber + " (" + contract.getClient().getName() + " - " + contract.getFixedAmount() + " MT)");
            } catch (Exception e) {
                errors.add("Erro no contrato " + contract.getContractNumber() + ": " + e.getMessage());
            }
        }

        return new BatchBillingResultDTO(
                effectiveRef,
                processedCount,
                generatedInvoices.size(),
                totalBilled,
                generatedInvoices,
                errors
        );
    }

    private LocalDate calculateInitialBillingDate(LocalDate startDate, int targetDay) {
        YearMonth ym = YearMonth.from(startDate);
        int day = Math.min(targetDay, ym.lengthOfMonth());
        LocalDate candidate = ym.atDay(day);

        if (candidate.isBefore(startDate)) {
            YearMonth nextYm = ym.plusMonths(1);
            int nextDay = Math.min(targetDay, nextYm.lengthOfMonth());
            return nextYm.atDay(nextDay);
        }
        return candidate;
    }

    private LocalDate computeNextBillingDate(LocalDate currentDate, BillingFrequency frequency, int targetDay) {
        int months = frequency != null ? frequency.monthsInterval() : 1;
        YearMonth nextYm = YearMonth.from(currentDate).plusMonths(months);
        int day = Math.min(targetDay, nextYm.lengthOfMonth());
        return nextYm.atDay(day);
    }

    private RecurringContractDTO toDTO(RecurringContract c) {
        return new RecurringContractDTO(
                c.getId(),
                c.getCompany().getId(),
                c.getContractNumber(),
                c.getClient().getId(),
                c.getClient().getName(),
                c.getClient().getTaxId(),
                c.getDescription(),
                c.getStatus(),
                c.getFrequency(),
                c.getBillingDayOfMonth(),
                c.getFixedAmount(),
                c.getStartDate(),
                c.getEndDate(),
                c.getNextBillingDate(),
                c.getLastBilledDate(),
                c.isAutoRenew()
        );
    }
}
