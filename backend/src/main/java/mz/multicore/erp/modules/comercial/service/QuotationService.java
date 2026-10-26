package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateQuotationRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.comercial.dto.UpdateQuotationRequest;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.inventory.repository.WarehouseRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Cotação ao cliente — a proposta de preço que antecede a encomenda.
 * Orquestrador modular delegando para serviços especializados. Ver docs/COTACAO_SPEC.md.
 */
@Service
public class QuotationService {

    private final QuotationLifecycleService lifecycleService;
    private final QuotationConversionService conversionService;
    private final QuotationQueryService queryService;

    public QuotationService(QuotationLifecycleService lifecycleService,
                            QuotationConversionService conversionService,
                            QuotationQueryService queryService) {
        this.lifecycleService = lifecycleService;
        this.conversionService = conversionService;
        this.queryService = queryService;
    }

    /** Construtor para retrocompatibilidade com suites de teste legadas. */
    public QuotationService(QuotationRepository quotationRepository,
                            ClientRepository clientRepository,
                            ProductRepository productRepository,
                            CompanyRepository companyRepository,
                            WarehouseRepository warehouseRepository,
                            WalkInClientProvider walkInClientProvider,
                            DocumentNumberService documentNumberService,
                            AuditLogService auditLogService,
                            ComercialService comercialService) {
        QuotationQueryService qService = new QuotationQueryService(quotationRepository);
        QuotationLifecycleService lService = new QuotationLifecycleService(
                quotationRepository, clientRepository, productRepository, companyRepository,
                warehouseRepository, walkInClientProvider, documentNumberService, auditLogService, qService);
        QuotationConversionService cService = new QuotationConversionService(
                quotationRepository, auditLogService, comercialService, qService, lService);
        this.queryService = qService;
        this.lifecycleService = lService;
        this.conversionService = cService;
    }

    @Transactional
    public QuotationDTO create(CreateQuotationRequest request) {
        return lifecycleService.create(request);
    }

    @Transactional
    public QuotationDTO update(Long id, UpdateQuotationRequest request) {
        return lifecycleService.update(id, request);
    }

    @Transactional
    public QuotationDTO send(Long id) {
        return lifecycleService.send(id);
    }

    @Transactional
    public QuotationDTO accept(Long id) {
        return lifecycleService.accept(id);
    }

    @Transactional
    public QuotationDTO reject(Long id, String reason) {
        return lifecycleService.reject(id, reason);
    }

    @Transactional
    public QuotationDTO cancel(Long id) {
        return lifecycleService.cancel(id);
    }

    @Transactional
    public QuotationDTO extendValidity(Long id, LocalDate newValidUntil) {
        return lifecycleService.extendValidity(id, newValidUntil);
    }

    @Transactional
    public OrderDTO convert(Long id) {
        return conversionService.convert(id);
    }

    @Transactional
    public InvoiceDTO convertToInvoice(Long id) {
        return conversionService.convertToInvoice(id);
    }

    @Transactional(readOnly = true)
    public List<QuotationDTO> findByCompany(Long companyId) {
        return queryService.findByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public List<QuotationDTO> findOpenByCompany(Long companyId) {
        return queryService.findOpenByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public QuotationDTO findById(Long id) {
        return queryService.findById(id);
    }
}
