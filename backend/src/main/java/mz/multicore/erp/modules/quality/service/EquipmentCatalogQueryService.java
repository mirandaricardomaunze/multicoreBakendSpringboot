package mz.multicore.erp.modules.quality.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.quality.dto.EquipmentDTO;
import mz.multicore.erp.modules.quality.model.Equipment;
import mz.multicore.erp.modules.quality.repository.EquipmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EquipmentCatalogQueryService {

    private final EquipmentRepository equipmentRepository;
    private final EquipmentCatalogMutationService mutationService;

    public EquipmentCatalogQueryService(EquipmentRepository equipmentRepository,
                                        EquipmentCatalogMutationService mutationService) {
        this.equipmentRepository = equipmentRepository;
        this.mutationService = mutationService;
    }

    public EquipmentDTO getEquipment(Long id, Long companyId) {
        Long resolvedCompanyId = QualityEquipmentService.resolveCompanyId(companyId);
        return equipmentRepository.findByIdAndCompanyId(id, resolvedCompanyId)
                .map(mutationService::mapToEquipmentDTO)
                .orElseThrow(() -> new BusinessRuleException("Equipamento não encontrado."));
    }

    public List<EquipmentDTO> listActiveEquipments(Long companyId) {
        Long resolvedCompanyId = QualityEquipmentService.resolveCompanyId(companyId);
        return equipmentRepository.findByCompanyIdAndActiveTrueOrderByNameAsc(resolvedCompanyId)
                .stream()
                .map(mutationService::mapToEquipmentDTO)
                .collect(Collectors.toList());
    }

    public PageResponse<EquipmentDTO> searchEquipments(Long companyId, String search, int page, int size) {
        Long resolvedCompanyId = QualityEquipmentService.resolveCompanyId(companyId);
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 25 : Math.min(200, size);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("name").ascending());

        String cleanSearch = search != null && !search.isBlank() ? search.trim() : null;
        Page<Equipment> result = equipmentRepository.searchEquipments(resolvedCompanyId, cleanSearch, pageable);

        List<EquipmentDTO> dtoList = result.getContent().stream()
                .map(mutationService::mapToEquipmentDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(dtoList, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
