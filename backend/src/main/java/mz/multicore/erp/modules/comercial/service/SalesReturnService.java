package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.comercial.model.*;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Serviço de orquestração do assistente de devoluções no backoffice com reintegração FEFO.
 * Ver {@code docs/DEVOLUCOES_BACKOFFICE_FEFO_SPEC.md}.
 */
@Service
public class SalesReturnService {

    private final InvoiceRepository invoiceRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final CreditNoteService creditNoteService;

    public SalesReturnService(InvoiceRepository invoiceRepository,
                              CreditNoteRepository creditNoteRepository,
                              CreditNoteService creditNoteService) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository obrigatório");
        this.creditNoteRepository = Objects.requireNonNull(creditNoteRepository, "creditNoteRepository obrigatório");
        this.creditNoteService = Objects.requireNonNull(creditNoteService, "creditNoteService obrigatório");
    }

    @Transactional(readOnly = true)
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

            BigDecimal boxes;
            if (p != null && p.getUnitsPerBox() > 0) {
                boxes = returnable.divide(BigDecimal.valueOf(p.getUnitsPerBox()), 2, RoundingMode.HALF_UP);
            } else {
                boxes = new BigDecimal("0.00");
            }

            int packsPerBox = (p != null && p.getPackagesPerBox() > 0) ? p.getPackagesPerBox() : 1;
            int unitsPerPack = (p != null && p.getUnitsPerPackage() > 0) ? p.getUnitsPerPackage() : (p != null ? p.getUnitsPerBox() : 1);

            result.add(new ReturnableInvoiceLineDTO(
                    line.getId(),
                    p != null ? p.getId() : null,
                    p != null && p.getSku() != null ? p.getSku() : "-",
                    p != null ? p.getName() : "Produto",
                    line.getUnitPrice(),
                    line.getTaxRate(),
                    line.getQuantity(),
                    alreadyReturned,
                    returnable,
                    boxes,
                    packsPerBox,
                    unitsPerPack,
                    line.getBatchNumber(),
                    null
            ));
        }
        return result;
    }

    @Transactional
    public SalesReturnWizardResultDTO processReturn(SalesReturnWizardRequest request) {
        if (request == null) throw new BusinessRuleException("Pedido de devolução nulo.");
        if (request.invoiceId() == null) throw new BusinessRuleException("ID da fatura obrigatório.");
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new BusinessRuleException("Selecione pelo menos uma linha para devolução.");
        }

        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada: " + request.invoiceId()));

        CurrentUserContext.requireCompany(invoice.getCompany().getId());

        List<ReturnableInvoiceLineDTO> availableLines = getReturnableLines(invoice.getId());
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
                invoice.getId(),
                reason,
                targetWarehouseId,
                request.description() != null ? request.description() : "Devolução assistida no backoffice",
                creditLines
        );

        CreditNoteDTO created = creditNoteService.create(createRequest);
        CreditNoteDTO finalNote = created;

        if (request.autoApprove()) {
            try {
                finalNote = creditNoteService.approve(created.id());
            } catch (Exception ex) {
                // Se a auto-aprovação falhar (ex.: permissão de operador), mantém criada em PENDING_APPROVAL
            }
        }

        return new SalesReturnWizardResultDTO(
                finalNote.id(),
                finalNote.noteNumber(),
                invoice.getId(),
                invoice.getInvoiceNumber(),
                finalNote.status(),
                finalNote.totalAmount(),
                restockedCount,
                java.time.LocalDateTime.now(),
                "Nota de Crédito " + finalNote.noteNumber() + " processada com sucesso."
        );
    }
}
