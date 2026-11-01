package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * O push USSD não está ligado a nenhum operador: só existe simulação, que confirma pagamentos sem
 * dinheiro real. Por isso fica desligada por omissão. Ver docs/POS_PRE_PILOTO_SPEC.md.
 */
@Component
public class MobilePaymentSimulationPolicy {

    private final boolean enabled;

    public MobilePaymentSimulationPolicy(
            @Value("${pos.mobile-payment.simulation-enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    void requireEnabled() {
        if (!enabled) {
            throw new BusinessRuleException("O envio automático para o telemóvel não está disponível. "
                    + "Confirme o pagamento no comprovativo do cliente e registe a referência.");
        }
    }
}
