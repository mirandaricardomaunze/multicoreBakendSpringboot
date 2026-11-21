package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateOpeningReceivableRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceDocumentKind;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Cria o documento "Saldo Inicial" (série SI) de uma factura em aberto trazida de outro ERP
 * (IMPORTACAO_DADOS_SPEC §5.2). Fica em {@code invoices} como {@link InvoiceDocumentKind#OPENING_BALANCE}, emitido e
 * por receber, para se cobrar e liquidar como uma factura; não tem linhas nem IVA, não publica evento de venda e por
 * isso não gera lançamento contabilístico. Numeração própria por empresa ({@code SI-aaaa/n}); o número original fica
 * guardado e, com o cliente, é a chave: a mesma dívida nunca entra duas vezes.
 */
@Service
public class OpeningReceivableCommandService {

    private final InvoiceRepository invoiceRepository;
    private final ClientLookup clientLookup;
    private final CompanyLookup companyLookup;
    private final DocumentNumberService documentNumberService;

    public OpeningReceivableCommandService(InvoiceRepository invoiceRepository, ClientLookup clientLookup,
                                           CompanyLookup companyLookup, DocumentNumberService documentNumberService) {
        this.invoiceRepository = invoiceRepository;
        this.clientLookup = clientLookup;
        this.companyLookup = companyLookup;
        this.documentNumberService = documentNumberService;
    }

    /** Grava o Saldo Inicial na empresa activa e devolve o número atribuído ({@code SI-aaaa/n}). */
    @Transactional
    public String create(CreateOpeningReceivableRequest request) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        Client client = clientLookup.requireForCompany(request.clientId(), companyId);
        if (invoiceRepository.existsByCompanyIdAndClientIdAndOriginalNumber(
                companyId, client.getId(), request.originalNumber())) {
            throw new BusinessRuleException("A dívida «" + request.originalNumber() + "» do cliente "
                    + client.getName() + " já foi importada.");
        }
        Invoice document = new Invoice();
        document.setDocumentKind(InvoiceDocumentKind.OPENING_BALANCE);
        document.setCompany(companyLookup.require(companyId));
        document.setClient(client);
        document.setOriginalNumber(request.originalNumber());
        document.setDocumentDate(request.documentDate());
        document.assignDueDate(request.documentDate(), request.dueDate());
        document.setDescription(request.description());
        document.setTotalBeforeTax(request.amount());
        document.setTaxAmount(BigDecimal.ZERO);
        document.setTotalAmount(request.amount());
        document.setStatus(InvoiceStatus.APPROVED);
        document.setInvoiceNumber(documentNumberService.next(DocumentSeries.OPENING_RECEIVABLE));
        return invoiceRepository.save(document).getInvoiceNumber();
    }
}
