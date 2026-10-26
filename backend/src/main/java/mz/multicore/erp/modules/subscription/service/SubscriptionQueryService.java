package mz.multicore.erp.modules.subscription.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.subscription.dto.SubscriptionDTO;
import mz.multicore.erp.modules.subscription.model.PaymentMethod;
import mz.multicore.erp.modules.subscription.model.PlanType;
import mz.multicore.erp.modules.subscription.model.Subscription;
import mz.multicore.erp.modules.subscription.model.SubscriptionStatus;
import mz.multicore.erp.modules.subscription.repository.SubscriptionPaymentRepository;
import mz.multicore.erp.modules.subscription.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Consultas analíticas e visões globais de assinaturas da plataforma.
 */
@Service
public class SubscriptionQueryService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final CompanyRepository companyRepository;

    public SubscriptionQueryService(SubscriptionRepository subscriptionRepository,
                                    SubscriptionPaymentRepository paymentRepository,
                                    CompanyRepository companyRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionDTO> listOverview() {
        PermissionGuard.requireSuperAdmin("listar assinaturas");
        return companyRepository.findAll().stream()
                .sorted(Comparator.comparing(Company::getName, String.CASE_INSENSITIVE_ORDER))
                .map(c -> toDto(c, subscriptionRepository.findByCompanyId(c.getId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionDTO> findByCompany(Long companyId) {
        PermissionGuard.requireSuperAdmin("consultar a assinatura");
        Company company = requireCompany(companyId);
        return Optional.of(toDto(company, subscriptionRepository.findByCompanyId(companyId).orElse(null)));
    }

    public SubscriptionDTO toDto(Company company, Subscription sub) {
        if (sub == null) {
            return new SubscriptionDTO(company.getId(), company.getName(), company.isActive(),
                    false, null, "—", null, "Sem assinatura", null, null, null,
                    paymentRepository.countByCompanyId(company.getId()));
        }
        SubscriptionStatus effective = sub.effectiveStatus();
        return new SubscriptionDTO(company.getId(), company.getName(), company.isActive(), true,
                sub.getPlan().name(), sub.getPlan().label(), effective.name(), effective.label(),
                sub.getStartDate(), sub.getValidUntil(), sub.getMonthlyPrice(),
                paymentRepository.countByCompanyId(company.getId()));
    }

    public Company requireCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessRuleException("Empresa não encontrada."));
    }

    public List<String> planOptions() {
        return Arrays.stream(PlanType.values()).map(Enum::name).toList();
    }

    public List<String> methodOptions() {
        return Arrays.stream(PaymentMethod.values()).map(Enum::name).toList();
    }
}
