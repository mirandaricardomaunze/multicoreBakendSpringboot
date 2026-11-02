package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.modules.pos.dto.InitiateMobilePaymentRequest;
import mz.multicore.erp.modules.pos.dto.MobilePaymentAvailabilityDTO;
import mz.multicore.erp.modules.pos.dto.MobilePaymentProvider;
import mz.multicore.erp.modules.pos.dto.MobilePaymentResponse;
import mz.multicore.erp.modules.pos.dto.MobilePaymentStatusResponse;
import mz.multicore.erp.modules.pos.repository.MobilePaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de integração e processamento de pagamentos móveis Push USSD (M-Pesa e e-Mola).
 */
@Service
public class MobilePaymentService {

    private final MobilePaymentTransactionProcessor transactionProcessor;
    private final MobilePaymentPhoneValidator phoneValidator;
    private final MobilePaymentSimulationPolicy simulationPolicy;

    @Autowired
    public MobilePaymentService(MobilePaymentTransactionProcessor transactionProcessor,
                                MobilePaymentPhoneValidator phoneValidator,
                                MobilePaymentSimulationPolicy simulationPolicy) {
        this.transactionProcessor = transactionProcessor;
        this.phoneValidator = phoneValidator;
        this.simulationPolicy = simulationPolicy;
    }

    public MobilePaymentService(MobilePaymentRepository repository, boolean simulationEnabled) {
        MobilePaymentPhoneValidator validator = new MobilePaymentPhoneValidator();
        this.phoneValidator = validator;
        this.simulationPolicy = new MobilePaymentSimulationPolicy(simulationEnabled);
        this.transactionProcessor = new MobilePaymentTransactionProcessor(repository, validator, simulationPolicy);
    }

    public MobilePaymentAvailabilityDTO availability() {
        return new MobilePaymentAvailabilityDTO(simulationPolicy.isEnabled());
    }

    @Transactional
    public MobilePaymentResponse initiatePayment(InitiateMobilePaymentRequest request) {
        return transactionProcessor.initiatePayment(request);
    }

    @Transactional
    public MobilePaymentStatusResponse checkStatus(String transactionId, Long companyId) {
        return transactionProcessor.checkStatus(transactionId, companyId);
    }

    @Transactional
    public MobilePaymentStatusResponse simulateComplete(String transactionId, Long companyId, boolean approve) {
        return transactionProcessor.simulateComplete(transactionId, companyId, approve);
    }

    public String normalizePhoneNumber(String raw) {
        return phoneValidator.normalizePhoneNumber(raw);
    }

    public void validateProviderPhone(MobilePaymentProvider provider, String normalizedPhone) {
        phoneValidator.validateProviderPhone(provider, normalizedPhone);
    }
}
