package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.pos.dto.MobilePaymentProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class MobilePaymentPhoneValidator {

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

    public String generateTransactionId(MobilePaymentProvider provider) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rand = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "TX-" + provider.name() + "-" + dateStr + "-" + rand;
    }

    public String generateFinancialReference(MobilePaymentProvider provider) {
        String prefix = provider == MobilePaymentProvider.MPESA ? "MP" : "EM";
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd.HHmm"));
        String rand = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + dateStr + "." + rand;
    }
}
