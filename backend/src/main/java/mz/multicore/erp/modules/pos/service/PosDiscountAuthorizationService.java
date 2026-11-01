package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Autorizações de desconto dadas por PIN: valem uma venda, nesta empresa e até à percentagem aprovada. */
@Service
public class PosDiscountAuthorizationService {

    private static final Duration LIFETIME = Duration.ofMinutes(30);
    private final Map<String, Grant> grants = new ConcurrentHashMap<>();

    public String issue(Long companyId, String managerName, BigDecimal maxPercent) {
        grants.values().removeIf(grant -> grant.expiresAt().isBefore(Instant.now()));
        String token = UUID.randomUUID().toString();
        grants.put(token, new Grant(companyId, managerName, maxPercent, Instant.now().plus(LIFETIME)));
        return token;
    }

    /** Gasta a autorização e devolve quem a deu. */
    public String consume(String token, Long companyId, BigDecimal percent) {
        Grant grant = token == null ? null : grants.remove(token);
        if (grant == null || !grant.companyId().equals(companyId) || grant.expiresAt().isBefore(Instant.now())
                || grant.maxPercent().compareTo(percent) < 0) {
            throw new BusinessRuleException("Este desconto precisa de ser autorizado com o PIN de um gerente. "
                    + "Volte a aplicá-lo com F5.");
        }
        return grant.managerName();
    }

    private record Grant(Long companyId, String managerName, BigDecimal maxPercent, Instant expiresAt) {}
}
