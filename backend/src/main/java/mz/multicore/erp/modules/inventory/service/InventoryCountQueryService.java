package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.inventory.dto.InventoryCountDTO;
import mz.multicore.erp.modules.inventory.dto.InventoryCountLineDTO;
import mz.multicore.erp.modules.inventory.model.InventoryCount;
import mz.multicore.erp.modules.inventory.model.InventoryCountStatus;
import mz.multicore.erp.modules.inventory.repository.InventoryCountRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryCountQueryService {

    private final InventoryCountRepository inventoryCountRepository;

    public InventoryCountQueryService(InventoryCountRepository inventoryCountRepository) {
        this.inventoryCountRepository = inventoryCountRepository;
    }

    public List<InventoryCountDTO> listSessions(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return inventoryCountRepository.findByCompany_IdOrderByCreatedAtDesc(companyId)
                .stream().map(this::toSummary).toList();
    }

    public InventoryCountDTO getSession(Long sessionId) {
        return toDetail(loadForCompany(sessionId));
    }

    public InventoryCount loadForCompany(Long sessionId) {
        InventoryCount count = inventoryCountRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessRuleException("Contagem não encontrada."));
        requireCurrentCompany(count);
        return count;
    }

    public InventoryCount loadForCompanyForUpdate(Long sessionId) {
        InventoryCount count = inventoryCountRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessRuleException("Contagem não encontrada."));
        requireCurrentCompany(count);
        return count;
    }

    public void requireCurrentCompany(InventoryCount count) {
        Long companyId = count.getCompany() != null ? count.getCompany().getId() : null;
        CurrentUserContext.requireCompany(companyId);
    }

    public void requireDraft(InventoryCount count) {
        if (count.getStatus() != InventoryCountStatus.DRAFT) {
            throw new BusinessRuleException("Esta contagem já foi aplicada ou cancelada.");
        }
    }

    public InventoryCountDTO toDetail(InventoryCount count) {
        List<InventoryCountLineDTO> lines = count.getLines().stream()
                .map(l -> new InventoryCountLineDTO(l.getId(), l.getProduct().getId(),
                        l.getProduct().getSku(), l.getProduct().getName(),
                        l.getCountedQuantity(), l.getSystemQuantity()))
                .toList();
        return build(count, lines);
    }

    public InventoryCountDTO toSummary(InventoryCount count) {
        return build(count, List.of());
    }

    private InventoryCountDTO build(InventoryCount count, List<InventoryCountLineDTO> lines) {
        int total = count.getLines().size();
        int counted = (int) count.getLines().stream().filter(l -> l.getCountedQuantity() != null).count();
        return new InventoryCountDTO(
                count.getId(), count.getWarehouse().getId(), count.getWarehouse().getName(),
                count.getStatus().name(), count.getNote(), count.getCreatedBy(),
                count.getCreatedAt(), count.getAppliedAt(), total, counted, lines);
    }
}
