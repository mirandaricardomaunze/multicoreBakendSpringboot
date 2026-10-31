package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteRequest;
import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.dto.ReturnableInvoiceLineDTO;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardLineRequest;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardRequest;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardResultDTO;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class SalesReturnWizardProcessor {

    private final InvoiceRepository invoiceRepository;
    private final CreditNoteService creditNoteService;
    private final ReturnableInvoiceLineCalculator lineCalculator;

    public SalesReturnWizardProcessor(InvoiceRepository invoiceRepository,
                                      CreditNoteService creditNoteService,
                                      ReturnableInvoiceLineCalculator lineCalculator) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository obrigatório");
        this.creditNoteService = Objects.requireNonNull(creditNoteService, "creditNoteService obrigatório");
        this.lineCalculator = Objects.requireNonNull(lineCalculator, "lineCalculator obrigatório");
    }

    public SalesReturnWizardResultDTO processReturn(SalesReturnWizardRequest request) {
        if (request == null) throw new BusinessRuleException("Pedido de devolução nulo.");
        if (request.invoiceId() == null) throw new BusinessRuleException("ID da fatura obrigatório.");
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new BusinessRuleException("Selecione pelo menos uma linha para devolução.");
        }

        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada: " + request.invoiceId()));

        CurrentUserContext.requireCompany(invoice.getCompany().getId());

        List<ReturnableInvoiceLineDTO> availableLines = lineCalculator.getReturnableLines(invoice.getId());
        Map<Long, ReturnableInvoiceLineDTO> lineMap = new HashMap<>();
        for (ReturnableInvoiceLineDTO l : availableLines) {
            lineMap.put(l.invoiceLineId(), l);
        }

        List<CreateCreditNoteLineRequest> creditLines = new ArrayList<>();
        int restockedCount = 0;

        for (SalesReturnWizardLineRequest item : request.lines()) {
            if (item.quantity() == null || item.quantity().signum() <= 0) continue;

            ReturnableInvoiceLineDTO available = lineMap.get(item.invoiceLineId());
            if (available == null) {
                throw new BusinessRuleException("Linha da fatura não pertence a esta fatura: " + item.invoiceLineId());
            }

            if (item.quantity().compareTo(available.returnableQuantity()) > 0) {
                throw new BusinessRuleException(String.format(
                        "Quantidade a devolver (%.2f) excede o saldo devolvível (%.2f) do produto %s.",
                        item.quantity(), available.returnableQuantity(), available.productName()));
            }

            creditLines.add(new CreateCreditNoteLineRequest(item.invoiceLineId(), item.quantity()));
            if (item.restock()) {
                restockedCount++;
            }
        }

        if (creditLines.isEmpty()) {
            throw new BusinessRuleException("Nenhuma quantidade positiva indicada para devolução.");
        }

        String reason = (request.reason() != null && !request.reason().isBlank()) ? request.reason() : "RETURN";
        Long targetWarehouseId = request.warehouseId() != null
                ? request.warehouseId()
                : (invoice.getWarehouse() != null ? invoice.getWarehouse().getId() : null);

        CreateCreditNoteRequest createRequest = new CreateCreditNoteRequest(
                invoice.getId(), reason, targetWarehouseId,
                request.description() != null ? request.description() : "Devolução assistida no backoffice",
                creditLines
        );

        CreditNoteDTO finalNote = creditNoteService.create(createRequest);
        if (request.autoApprove()) {
            try {
                finalNote = creditNoteService.approve(finalNote.id());
            } catch (Exception ignored) {}
        }

        return new SalesReturnWizardResultDTO(
                finalNote.id(), finalNote.noteNumber(), invoice.getId(), invoice.getInvoiceNumber(),
                finalNote.status(), finalNote.totalAmount(), restockedCount, LocalDateTime.now(),
                "Nota de Crédito " + finalNote.noteNumber() + " processada com sucesso."
        );
    }
}
