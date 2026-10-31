package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.dto.ReturnableInvoiceLineDTO;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardRequest;
import mz.multicore.erp.modules.comercial.dto.SalesReturnWizardResultDTO;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Serviço de orquestração do assistente de devoluções no backoffice com reintegração FEFO.
 */
@Service
public class SalesReturnService {

    private final ReturnableInvoiceLineCalculator lineCalculator;
    private final SalesReturnWizardProcessor wizardProcessor;

    @Autowired
    public SalesReturnService(ReturnableInvoiceLineCalculator lineCalculator,
                              SalesReturnWizardProcessor wizardProcessor) {
        this.lineCalculator = Objects.requireNonNull(lineCalculator, "lineCalculator obrigatório");
        this.wizardProcessor = Objects.requireNonNull(wizardProcessor, "wizardProcessor obrigatório");
    }

    public SalesReturnService(InvoiceRepository invoiceRepository,
                              CreditNoteRepository creditNoteRepository,
                              CreditNoteService creditNoteService) {
        ReturnableInvoiceLineCalculator calc = new ReturnableInvoiceLineCalculator(invoiceRepository, creditNoteRepository);
        this.lineCalculator = calc;
        this.wizardProcessor = new SalesReturnWizardProcessor(invoiceRepository, creditNoteService, calc);
    }

    @Transactional(readOnly = true)
    public List<ReturnableInvoiceLineDTO> getReturnableLines(Long invoiceId) {
        return lineCalculator.getReturnableLines(invoiceId);
    }

    @Transactional
    public SalesReturnWizardResultDTO processReturn(SalesReturnWizardRequest request) {
        return wizardProcessor.processReturn(request);
    }
}
