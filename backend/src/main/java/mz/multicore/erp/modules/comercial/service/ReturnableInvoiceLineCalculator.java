package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.ReturnableInvoiceLineDTO;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class ReturnableInvoiceLineCalculator {

    private final InvoiceRepository invoiceRepository;
    private final CreditNoteRepository creditNoteRepository;

    public ReturnableInvoiceLineCalculator(InvoiceRepository invoiceRepository,
                                           CreditNoteRepository creditNoteRepository) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository obrigatório");
        this.creditNoteRepository = Objects.requireNonNull(creditNoteRepository, "creditNoteRepository obrigatório");
    }

    public List<ReturnableInvoiceLineDTO> getReturnableLines(Long invoiceId) {
        if (invoiceId == null) throw new BusinessRuleException("ID da fatura obrigatório.");
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada: " + invoiceId));

        CurrentUserContext.requireCompany(invoice.getCompany().getId());

        List<ReturnableInvoiceLineDTO> result = new ArrayList<>();
        for (InvoiceLine line : invoice.getLines()) {
            Product p = line.getProduct();
            BigDecimal alreadyReturned = creditNoteRepository.sumNonVoidedReturnedByInvoiceLineId(line.getId());
            if (alreadyReturned == null) alreadyReturned = BigDecimal.ZERO;

            BigDecimal returnable = line.getQuantity().subtract(alreadyReturned);
            if (returnable.signum() < 0) returnable = BigDecimal.ZERO;

            BigDecimal boxes = (p != null && p.getUnitsPerBox() > 0)
                    ? returnable.divide(BigDecimal.valueOf(p.getUnitsPerBox()), 2, RoundingMode.HALF_UP)
                    : new BigDecimal("0.00");

            int packsPerBox = (p != null && p.getPackagesPerBox() > 0) ? p.getPackagesPerBox() : 1;
            int unitsPerPack = (p != null && p.getUnitsPerPackage() > 0) ? p.getUnitsPerPackage() : (p != null ? p.getUnitsPerBox() : 1);

            result.add(new ReturnableInvoiceLineDTO(
                    line.getId(), p != null ? p.getId() : null,
                    p != null && p.getSku() != null ? p.getSku() : "-",
                    p != null ? p.getName() : "Produto",
                    line.getUnitPrice(), line.getTaxRate(), line.getQuantity(),
                    alreadyReturned, returnable, boxes, packsPerBox, unitsPerPack,
                    line.getBatchNumber(), null
            ));
        }
        return result;
    }
}
