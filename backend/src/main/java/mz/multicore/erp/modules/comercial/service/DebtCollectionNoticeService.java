package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.DebtCollectionNoticeDTO;
import mz.multicore.erp.modules.comercial.dto.OverdueInvoiceItemDTO;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Emissão e consolidação de notificações formais de cobrança com discriminação de facturas em mora.
 */
@Service
public class DebtCollectionNoticeService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final CompanyService companyService;

    public DebtCollectionNoticeService(
            InvoiceRepository invoiceRepository,
            ClientRepository clientRepository,
            CompanyService companyService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.clientRepository = clientRepository;
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public DebtCollectionNoticeDTO getDebtCollectionNotice(Long clientId, LocalDate referenceDate) {
        LocalDate refDate = referenceDate != null ? referenceDate : LocalDate.now();
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        Client client = clientRepository.findByIdAndCompaniesId(clientId, companyId)
                .orElseThrow(() -> new BusinessRuleException("Cliente não encontrado na empresa actual: " + clientId));

        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new BusinessRuleException("Empresa não encontrada: " + companyId);
        }

        List<Invoice> overdueInvoices = invoiceRepository.findByCompanyId(companyId).stream()
                .filter(inv -> inv.getClient() != null && clientId.equals(inv.getClient().getId()))
                .filter(inv -> inv.getStatus().isCollectable())
                .filter(inv -> inv.outstandingAmount().signum() > 0)
                .filter(inv -> inv.daysOverdue(refDate) > 0)
                .sorted(Comparator.comparing(Invoice::effectiveDueDate))
                .toList();

        if (overdueInvoices.isEmpty()) {
            throw new BusinessRuleException(String.format("O cliente %s não possui facturas vencidas em mora.", client.getName()));
        }

        BigDecimal totalOverdue = BigDecimal.ZERO;
        int maxOverdue = 0;
        List<OverdueInvoiceItemDTO> items = new ArrayList<>();

        for (Invoice inv : overdueInvoices) {
            BigDecimal outstanding = inv.outstandingAmount();
            totalOverdue = totalOverdue.add(outstanding);
            int days = inv.daysOverdue(refDate);
            if (days > maxOverdue) {
                maxOverdue = days;
            }
            items.add(new OverdueInvoiceItemDTO(
                    inv.getId(),
                    inv.getInvoiceNumber(),
                    inv.issueDate(),
                    inv.effectiveDueDate(),
                    days,
                    inv.payableTotal(),
                    inv.getAmountPaid(),
                    outstanding
            ));
        }

        String noticeRef = String.format("NOT-COB-%s-%04d", refDate.format(DateTimeFormatter.ofPattern("yyyyMM")), client.getId());

        List<String> bankAccounts = List.of(
                "Millennium BIM: 1234567890 (Conta Corrente MZN) - NIB: 000100000123456789012",
                "BCI: 9876543210 (Conta Corrente MZN) - NIB: 000800000987654321098",
                "Standard Bank: 5544332211 (Conta Corrente MZN) - NIB: 000300000554433221144"
        );

        String customMessage = "Solicitamos a regularização das facturas em mora acima discriminadas no prazo impreterível de 5 (cinco) dias úteis "
                + "a contar da data da recepção desta notificação. Após confirmação da transferência, agradecemos o envio do respectivo comprovativo de pagamento.";

        return new DebtCollectionNoticeDTO(
                client.getId(),
                client.getName(),
                client.getTaxId(),
                client.getAddress(),
                client.getEmail(),
                refDate,
                noticeRef,
                totalOverdue,
                maxOverdue,
                items,
                bankAccounts,
                company.getName(),
                company.getTaxId(),
                company.getAddress(),
                company.getPhone(),
                company.getEmail(),
                customMessage
        );
    }
}
