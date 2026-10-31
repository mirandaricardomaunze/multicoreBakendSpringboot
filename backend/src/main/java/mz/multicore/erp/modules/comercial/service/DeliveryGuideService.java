package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateDeliveryGuideRequest;
import mz.multicore.erp.modules.comercial.dto.CreateStandaloneDeliveryGuideRequest;
import mz.multicore.erp.modules.comercial.dto.DeliveryGuideDTO;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orquestrador de Guias de Remessa ao Cliente (Fase 111).
 * Decomposto em colaboradores especializados para conformidade estrita com SOLID/SRP.
 * Ver docs/DELIVERY_GUIDE_SERVICE_DECOMPOSITION_SPEC.md.
 */
@Service
public class DeliveryGuideService {

    private final DeliveryGuideCreationService creationService;
    private final DeliveryGuideLifecycleService lifecycleService;
    private final DeliveryGuideQueryService queryService;

    @Autowired
    public DeliveryGuideService(DeliveryGuideCreationService creationService,
                                DeliveryGuideLifecycleService lifecycleService,
                                DeliveryGuideQueryService queryService) {
        this.creationService = creationService;
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
    }

    /**
     * Construtor retrocompatível preservado para compatibilidade integral com testes legados.
     */
    public DeliveryGuideService(DeliveryGuideRepository guideRepository,
                                OrderRepository orderRepository,
                                ClientRepository clientRepository,
                                ProductRepository productRepository,
                                InventoryService inventoryService,
                                DocumentNumberService documentNumberService,
                                AuditLogService auditLogService) {
        this(new DeliveryGuideCreationService(guideRepository, orderRepository, clientRepository, productRepository,
                        inventoryService, documentNumberService, auditLogService),
                new DeliveryGuideLifecycleService(guideRepository, orderRepository, inventoryService, auditLogService,
                        new DeliveryGuideQueryService(guideRepository)),
                new DeliveryGuideQueryService(guideRepository));
    }

    @Transactional
    public DeliveryGuideDTO createStandalone(CreateStandaloneDeliveryGuideRequest request) {
        return creationService.createStandalone(request);
    }

    @Transactional
    public DeliveryGuideDTO createFromOrder(CreateDeliveryGuideRequest request) {
        return creationService.createFromOrder(request);
    }

    @Transactional
    public DeliveryGuideDTO approve(Long id) {
        return lifecycleService.approve(id);
    }

    @Transactional
    public DeliveryGuideDTO reject(Long id, String rejectionReason) {
        return lifecycleService.reject(id, rejectionReason);
    }

    @Transactional
    public DeliveryGuideDTO cancel(Long id) {
        return lifecycleService.cancel(id);
    }

    @Transactional(readOnly = true)
    public List<DeliveryGuideDTO> findByCompany(Long companyId) {
        return queryService.findByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public DeliveryGuideDTO findById(Long id) {
        return queryService.findById(id);
    }

    @Transactional(readOnly = true)
    public DeliveryGuide loadForPrint(Long id) {
        return queryService.loadForPrint(id);
    }
}
