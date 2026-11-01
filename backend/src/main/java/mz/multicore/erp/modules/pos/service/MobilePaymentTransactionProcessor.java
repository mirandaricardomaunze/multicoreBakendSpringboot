package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.pos.dto.InitiateMobilePaymentRequest;
import mz.multicore.erp.modules.pos.dto.MobilePaymentResponse;
import mz.multicore.erp.modules.pos.dto.MobilePaymentStatus;
import mz.multicore.erp.modules.pos.dto.MobilePaymentStatusResponse;
import mz.multicore.erp.modules.pos.model.MobilePaymentTransaction;
import mz.multicore.erp.modules.pos.repository.MobilePaymentRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Component
public class MobilePaymentTransactionProcessor {

    private final MobilePaymentRepository repository;
    private final MobilePaymentPhoneValidator phoneValidator;
    private final MobilePaymentSimulationPolicy simulationPolicy;

    public MobilePaymentTransactionProcessor(MobilePaymentRepository repository,
                                             MobilePaymentPhoneValidator phoneValidator,
                                             MobilePaymentSimulationPolicy simulationPolicy) {
        this.repository = repository;
        this.phoneValidator = phoneValidator;
        this.simulationPolicy = simulationPolicy;
    }

    public MobilePaymentResponse initiatePayment(InitiateMobilePaymentRequest request) {
        simulationPolicy.requireEnabled();
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("O valor do pagamento móvel deve ser superior a zero.");
        }
        if (request.provider() == null) {
            throw new BusinessRuleException("O operador de pagamento móvel (M-Pesa ou e-Mola) é obrigatório.");
        }

        String normalizedPhone = phoneValidator.normalizePhoneNumber(request.phoneNumber());
        phoneValidator.validateProviderPhone(request.provider(), normalizedPhone);

        String txId = phoneValidator.generateTransactionId(request.provider());

        MobilePaymentTransaction tx = new MobilePaymentTransaction();
        tx.setCompanyId(request.companyId() != null ? request.companyId() : 1L);
        tx.setTransactionId(txId);
        tx.setProvider(request.provider());
        tx.setPhoneNumber(normalizedPhone);
        tx.setAmount(request.amount());
        tx.setReference(request.reference());
        tx.setOperator(request.operator());

        if (normalizedPhone.endsWith("000")) {
            tx.setStatus(MobilePaymentStatus.FAILED);
            tx.setMessage("Saldo insuficiente ou PIN incorreto no telemóvel do cliente.");
        } else if (normalizedPhone.endsWith("999")) {
            tx.setStatus(MobilePaymentStatus.CANCELLED);
            tx.setMessage("Transacção cancelada pelo cliente.");
        } else {
            tx.setStatus(MobilePaymentStatus.PENDING);
            tx.setMessage("Solicitação USSD enviada ao cliente. Aguardando PIN...");
        }

        MobilePaymentTransaction saved = repository.save(tx);

        return new MobilePaymentResponse(
                saved.getTransactionId(),
                saved.getProvider(),
                saved.getPhoneNumber(),
                saved.getAmount(),
                saved.getReference(),
                saved.getFinancialReference(),
                saved.getStatus(),
                saved.getMessage(),
                Instant.now()
        );
    }

    public MobilePaymentStatusResponse checkStatus(String transactionId, Long companyId) {
        MobilePaymentTransaction tx = repository.findByTransactionId(transactionId)
                .orElseThrow(() -> new BusinessRuleException("Transacção de pagamento móvel não encontrada: " + transactionId));

        if (companyId != null && !companyId.equals(tx.getCompanyId())) {
            throw new BusinessRuleException("Acesso não autorizado à transacção de outra empresa.");
        }

        if (tx.getStatus() == MobilePaymentStatus.PENDING) {
            LocalDateTime createdAt = tx.getCreatedAt() != null ? tx.getCreatedAt() : LocalDateTime.now();
            long elapsedSeconds = Duration.between(createdAt, LocalDateTime.now()).getSeconds();

            if (elapsedSeconds > 60) {
                tx.setStatus(MobilePaymentStatus.EXPIRED);
                tx.setMessage("Tempo limite de 60 segundos excedido sem resposta do cliente.");
                repository.save(tx);
            } else if (simulationPolicy.isEnabled() && elapsedSeconds >= 2) {
                tx.setStatus(MobilePaymentStatus.SUCCESS);
                tx.setFinancialReference(phoneValidator.generateFinancialReference(tx.getProvider()));
                tx.setMessage("Pagamento móvel confirmado com sucesso.");
                repository.save(tx);
            }
        }

        return new MobilePaymentStatusResponse(
                tx.getTransactionId(),
                tx.getStatus(),
                tx.getFinancialReference(),
                tx.getMessage()
        );
    }

    public MobilePaymentStatusResponse simulateComplete(String transactionId, Long companyId, boolean approve) {
        simulationPolicy.requireEnabled();
        MobilePaymentTransaction tx = repository.findByTransactionId(transactionId)
                .orElseThrow(() -> new BusinessRuleException("Transacção não encontrada: " + transactionId));

        if (companyId != null && !companyId.equals(tx.getCompanyId())) {
            throw new BusinessRuleException("Acesso não autorizado.");
        }

        if (approve) {
            tx.setStatus(MobilePaymentStatus.SUCCESS);
            tx.setFinancialReference(phoneValidator.generateFinancialReference(tx.getProvider()));
            tx.setMessage("Pagamento móvel aprovado com sucesso.");
        } else {
            tx.setStatus(MobilePaymentStatus.FAILED);
            tx.setMessage("Pagamento recusado pelo operador ou cliente.");
        }

        repository.save(tx);
        return new MobilePaymentStatusResponse(tx.getTransactionId(), tx.getStatus(), tx.getFinancialReference(), tx.getMessage());
    }
}
