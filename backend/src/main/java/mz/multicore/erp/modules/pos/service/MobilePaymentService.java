package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.pos.dto.*;
import mz.multicore.erp.modules.pos.model.MobilePaymentTransaction;
import mz.multicore.erp.modules.pos.repository.MobilePaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Serviço de integração e processamento de pagamentos móveis Push USSD (M-Pesa e e-Mola).
 */
@Service
public class MobilePaymentService {

    private final MobilePaymentRepository repository;

    public MobilePaymentService(MobilePaymentRepository repository) {
        this.repository = repository;
    }

    /**
     * Inicia a solicitação de débito via Push USSD ao telemóvel do cliente.
     */
    @Transactional
    public MobilePaymentResponse initiatePayment(InitiateMobilePaymentRequest request) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("O valor do pagamento móvel deve ser superior a zero.");
        }
        if (request.provider() == null) {
            throw new BusinessRuleException("O operador de pagamento móvel (M-Pesa ou e-Mola) é obrigatório.");
        }

        String normalizedPhone = normalizePhoneNumber(request.phoneNumber());
        validateProviderPhone(request.provider(), normalizedPhone);

        String txId = generateTransactionId(request.provider());

        MobilePaymentTransaction tx = new MobilePaymentTransaction();
        tx.setCompanyId(request.companyId() != null ? request.companyId() : 1L);
        tx.setTransactionId(txId);
        tx.setProvider(request.provider());
        tx.setPhoneNumber(normalizedPhone);
        tx.setAmount(request.amount());
        tx.setReference(request.reference());
        tx.setOperator(request.operator());

        // Lógica de simulação/sandbox inteligente:
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

    /**
     * Consulta o estado atual da transação (usado para polling pelo POS).
     */
    @Transactional
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
            } else if (elapsedSeconds >= 2) {
                // No modo sandbox/simulação integrada, após 2 segundos o pagamento é confirmado
                tx.setStatus(MobilePaymentStatus.SUCCESS);
                tx.setFinancialReference(generateFinancialReference(tx.getProvider()));
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

    /**
     * Permite forçar o estado de uma transação (usado para demonstração e testes).
     */
    @Transactional
    public MobilePaymentStatusResponse simulateComplete(String transactionId, Long companyId, boolean approve) {
        MobilePaymentTransaction tx = repository.findByTransactionId(transactionId)
                .orElseThrow(() -> new BusinessRuleException("Transacção não encontrada: " + transactionId));

        if (companyId != null && !companyId.equals(tx.getCompanyId())) {
            throw new BusinessRuleException("Acesso não autorizado.");
        }

        if (approve) {
            tx.setStatus(MobilePaymentStatus.SUCCESS);
            tx.setFinancialReference(generateFinancialReference(tx.getProvider()));
            tx.setMessage("Pagamento móvel aprovado com sucesso.");
        } else {
            tx.setStatus(MobilePaymentStatus.FAILED);
            tx.setMessage("Pagamento recusado pelo operador ou cliente.");
        }

        repository.save(tx);
        return new MobilePaymentStatusResponse(tx.getTransactionId(), tx.getStatus(), tx.getFinancialReference(), tx.getMessage());
    }

    /**
     * Normaliza qualquer formato de telefone Moçambicano para 9 dígitos ("84XXXXXXX").
     */
    public String normalizePhoneNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new BusinessRuleException("O número de telemóvel é obrigatório.");
        }
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("258") && digits.length() == 12) {
            digits = digits.substring(3);
        }
        if (digits.length() != 9) {
            throw new BusinessRuleException("O número de telemóvel deve conter 9 dígitos (ex.: 84 123 4567). Encontrado: " + raw);
        }
        return digits;
    }

    /**
     * Valida compatibilidade do prefixo móvel nacional com o operador escolhido.
     */
    public void validateProviderPhone(MobilePaymentProvider provider, String normalizedPhone) {
        String prefix = normalizedPhone.substring(0, 2);
        if (provider == MobilePaymentProvider.MPESA) {
            if (!prefix.equals("84") && !prefix.equals("85")) {
                throw new BusinessRuleException("Número incompatível com M-Pesa. Os números Vodacom devem iniciar por 84 ou 85.");
            }
        } else if (provider == MobilePaymentProvider.EMOLA) {
            if (!prefix.equals("86") && !prefix.equals("87")) {
                throw new BusinessRuleException("Número incompatível com e-Mola. Os números Movitel devem iniciar por 86 ou 87.");
            }
        }
    }

    private String generateTransactionId(MobilePaymentProvider provider) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rand = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TX-" + provider.name() + "-" + dateStr + "-" + rand;
    }

    private String generateFinancialReference(MobilePaymentProvider provider) {
        String prefix = provider == MobilePaymentProvider.MPESA ? "MP" : "EM";
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd.HHmm"));
        String rand = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + dateStr + "." + rand;
    }
}
