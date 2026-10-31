package mz.multicore.erp.modules.quality.service;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.quality.dto.EquipmentCreateRequest;
import mz.multicore.erp.modules.quality.dto.EquipmentDTO;
import mz.multicore.erp.modules.quality.model.Equipment;
import mz.multicore.erp.modules.quality.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestão de catálogo, especificações e prazos de validade de equipamentos e EPIs.
 */
@Service
public class EquipmentCatalogService {

    private final EquipmentCatalogMutationService mutationService;
    private final EquipmentCatalogQueryService queryService;

    @Autowired
    public EquipmentCatalogService(EquipmentCatalogMutationService mutationService,
                                   EquipmentCatalogQueryService queryService) {
        this.mutationService = mutationService;
        this.queryService = queryService;
    }

    public EquipmentCatalogService(EquipmentRepository equipmentRepository, CompanyService companyService) {
        EquipmentCatalogMutationService mut = new EquipmentCatalogMutationService(equipmentRepository, companyService);
        this.mutationService = mut;
        this.queryService = new EquipmentCatalogQueryService(equipmentRepository, mut);
    }

    @Transactional
    public EquipmentDTO createEquipment(EquipmentCreateRequest req) {
        return mutationService.createEquipment(req);
    }

    @Transactional
    public EquipmentDTO updateEquipment(Long id, EquipmentCreateRequest req) {
        return mutationService.updateEquipment(id, req);
    }

    @Transactional(readOnly = true)
    public EquipmentDTO getEquipment(Long id, Long companyId) {
        return queryService.getEquipment(id, companyId);
    }

    @Transactional(readOnly = true)
    public List<EquipmentDTO> listActiveEquipments(Long companyId) {
        return queryService.listActiveEquipments(companyId);
    }

    @Transactional(readOnly = true)
    public PageResponse<EquipmentDTO> searchEquipments(Long companyId, String search, int page, int size) {
        return queryService.searchEquipments(companyId, search, page, size);
    }

    public EquipmentDTO mapToEquipmentDTO(Equipment e) {
        return mutationService.mapToEquipmentDTO(e);
    }
}
