package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.architecture.validation.TaxIdValidator;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.ClientCreditEligibilityDTO;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.SaveClientRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ComercialClientAndCreditService {

    private final ClientRepository clientRepository;
    private final CompanyLookup companyLookup;
    private final InvoiceRepository invoiceRepository;
    private final AuditLogService auditLogService;

    public ComercialClientAndCreditService(
            ClientRepository clientRepository,
            CompanyLookup companyLookup,
            InvoiceRepository invoiceRepository,
            AuditLogService auditLogService) {
        this.clientRepository = clientRepository;
        this.companyLookup = companyLookup;
        this.invoiceRepository = invoiceRepository;
        this.auditLogService = auditLogService;
    }

    public ClientDTO createClient(SaveClientRequest request) {
        String taxId = request.taxId();
        TaxIdValidator.validate(taxId);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (clientRepository.findByTaxIdAndCompaniesId(taxId, companyId).isPresent()) {
            throw new BusinessRuleException("Já existe um cliente registado com este NUIT/NIF.");
        }
        Client sharedClient = clientRepository.findByTaxId(taxId).orElse(null);
        if (sharedClient != null) {
            sharedClient.getCompanies().add(companyLookup.reference(companyId));
            sharedClient = clientRepository.save(sharedClient);
            return toDTO(sharedClient);
        }
        Client client = new Client();
        client.setName(request.name());
        client.setTaxId(taxId);
        client.setEmail(request.email());
        client.setAddress(request.address());
        client.setPaymentTermsDays(request.effectivePaymentTermsDays());
        client.setCreditLimit(request.creditLimit());
        client.setCreatedBy("SYSTEM");
        client.getCompanies().add(companyLookup.reference(companyId));
        return toDTO(clientRepository.save(client));
    }

    public ClientDTO updateClient(Long id, SaveClientRequest request) {
        Client client = clientRepository.findByIdAndCompaniesId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."));
        String taxId = request.taxId();
        TaxIdValidator.validate(taxId);
        if (!client.getTaxId().equals(taxId)) {
            clientRepository.findByTaxIdAndCompaniesId(taxId, CurrentUserContext.getCurrentCompanyId()).ifPresent(existing -> {
                throw new BusinessRuleException("Já existe outro cliente registado com este NUIT/NIF.");
            });
        }
        BigDecimal oldLimit = client.getCreditLimit();
        BigDecimal newLimit = request.creditLimit();
        boolean creditLimitChanged = (oldLimit == null && newLimit != null)
                || (oldLimit != null && (newLimit == null || oldLimit.compareTo(newLimit) != 0));

        client.setName(request.name());
        client.setTaxId(taxId);
        client.setEmail(request.email());
        client.setAddress(request.address());
        client.setPaymentTermsDays(request.effectivePaymentTermsDays());
        client.setCreditLimit(request.creditLimit());
        client = clientRepository.save(client);

        if (creditLimitChanged && auditLogService != null) {
            String oldVal = oldLimit != null ? oldLimit.toPlainString() : "0";
            String newVal = newLimit != null ? newLimit.toPlainString() : "0";
            auditLogService.logCurrent("CLIENT_CREDIT_LIMIT_CHANGE",
                    "Limite de crédito do cliente " + client.getName() + " (ID " + client.getId() + ") alterado de " + oldVal + " para " + newVal + " MZN");
        }
        return toDTO(client);
    }

    public ClientDTO toDTO(Client client) {
        return new ClientDTO(client.getId(), client.getName(), client.getTaxId(),
                client.getEmail(), client.getAddress(), client.effectivePaymentTermsDays(),
                client.getCreditLimit(), BigDecimal.ZERO, null, client.getVersion());
    }

    public void deleteClient(Long id) {
        PermissionGuard.requireManagerOrAdmin("eliminar cliente");
        Client client = clientRepository.findByIdAndCompaniesId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado."));
        try {
            clientRepository.delete(client);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessRuleException(
                    "Não é possível eliminar este cliente porque já tem documentos associados (faturas, encomendas, etc.).");
        }
    }

    public List<ClientDTO> getAllClients() {
        return clientRepository.findDistinctByCompaniesIdOrderByName(CurrentUserContext.getCurrentCompanyId())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ClientCreditEligibilityDTO getClientCreditEligibility(Long clientId) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        if (clientId == null) {
            return new ClientCreditEligibilityDTO(
                    null, "Consumidor Final", false, false,
                    "Consumidor Final (sem registo) está limitado estritamente a Pronto Pagamento.",
                    0, null, BigDecimal.ZERO, null, 0);
        }
        Client client = clientRepository.findByIdAndCompaniesId(clientId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado: " + clientId));
        return evaluateClientCreditEligibility(client, companyId);
    }

    public ClientCreditEligibilityDTO evaluateClientCreditEligibility(Client client, Long companyId) {
        if (client == null) {
            return new ClientCreditEligibilityDTO(
                    null, "Consumidor Final", false, false,
                    "Consumidor Final (sem registo) está limitado estritamente a Pronto Pagamento.",
                    0, null, BigDecimal.ZERO, null, 0);
        }
        if (client.hasCreditLimit() && client.getCreditLimit().signum() == 0) {
            return new ClientCreditEligibilityDTO(
                    client.getId(), client.getName(), false, true,
                    "Cliente sem autorização para crédito (limite zero definido na ficha).",
                    0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        LocalDate today = LocalDate.now();
        List<Invoice> openInvoices = invoiceRepository.findByCompanyId(companyId).stream()
                .filter(inv -> inv.getClient() != null && client.getId().equals(inv.getClient().getId()))
                .filter(inv -> inv.getStatus() != null && inv.getStatus().isCollectable())
                .filter(inv -> inv.outstandingAmount().signum() > 0)
                .toList();

        BigDecimal currentDebt = BigDecimal.ZERO;
        int maxDaysOverdue = 0;
        for (Invoice inv : openInvoices) {
            currentDebt = currentDebt.add(inv.outstandingAmount());
            int days = inv.daysOverdue(today);
            if (days > maxDaysOverdue) maxDaysOverdue = days;
        }

        BigDecimal available = client.creditAvailable(currentDebt);
        if (maxDaysOverdue > 30) {
            return new ClientCreditEligibilityDTO(
                    client.getId(), client.getName(), false, true,
                    String.format("Cliente com faturas vencidas em mora há %d dias (máx. permitido: 30 dias).", maxDaysOverdue),
                    0, client.getCreditLimit(), currentDebt, available, maxDaysOverdue);
        }
        if (client.hasCreditLimit() && available != null && available.signum() <= 0) {
            return new ClientCreditEligibilityDTO(
                    client.getId(), client.getName(), false, true,
                    String.format("Limite de crédito esgotado (Dívida: %s MT · Limite: %s MT).",
                            formatMoney(currentDebt), formatMoney(client.getCreditLimit())),
                    0, client.getCreditLimit(), currentDebt, available, maxDaysOverdue);
        }
        return new ClientCreditEligibilityDTO(
                client.getId(), client.getName(), true, false, null,
                client.effectivePaymentTermsDays(), client.getCreditLimit(), currentDebt, available, maxDaysOverdue);
    }

    public void validatePaymentTermsForClient(Client client, String paymentTerms, Long companyId) {
        if (isPromptPayment(paymentTerms)) return;
        if (client == null) {
            throw new BusinessRuleException("Consumidor Final (sem registo) está limitado estritamente a Pronto Pagamento.");
        }
        ClientCreditEligibilityDTO eligibility = evaluateClientCreditEligibility(client, companyId);
        if (!eligibility.creditAllowed() || eligibility.problematic()) {
            throw new BusinessRuleException(String.format(
                    "Venda a prazo não autorizada para o cliente %s. Restrição de crédito: %s. As encomendas para este cliente estão limitadas estritamente a Pronto Pagamento.",
                    client.getName(), eligibility.restrictionReason() != null ? eligibility.restrictionReason() : "Risco de crédito"));
        }
    }

    public static boolean isPromptPayment(String terms) {
        if (terms == null || terms.isBlank()) return true;
        String norm = terms.trim().toLowerCase(Locale.ROOT);
        return norm.contains("pronto") || norm.startsWith("0 dia") || norm.equals("0");
    }

    public LocalDate resolveDueDateFromTerms(String paymentTerms, LocalDate issueDate, Client client) {
        if (paymentTerms == null || paymentTerms.isBlank()) {
            return issueDate.plusDays(client != null ? client.effectivePaymentTermsDays() : 0);
        }
        if (isPromptPayment(paymentTerms)) return issueDate;
        int days = parseDaysFromTerms(paymentTerms, client != null ? client.effectivePaymentTermsDays() : 0);
        return issueDate.plusDays(days);
    }

    private static int parseDaysFromTerms(String terms, int defaultDays) {
        if (terms == null || terms.isBlank()) return defaultDays;
        Matcher m = Pattern.compile("(\\d+)").matcher(terms);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        return defaultDays;
    }

    private static String formatMoney(BigDecimal val) {
        return (val == null ? BigDecimal.ZERO : val).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
