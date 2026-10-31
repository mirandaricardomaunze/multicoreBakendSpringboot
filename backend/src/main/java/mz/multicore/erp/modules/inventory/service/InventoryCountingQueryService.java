package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.inventory.dto.InventoryItemDTO;
import mz.multicore.erp.modules.inventory.dto.InventorySessionDTO;
import mz.multicore.erp.modules.inventory.model.InventoryPhysicalSession;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.InventoryPhysicalSessionRepository;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Consultas tenant-scoped, stock esperado e projecção DTO de inventário físico. */
@Component
public class InventoryCountingQueryService {

    private final InventoryPhysicalSessionRepository sessionRepository;
    private final StockRepository stockRepository;

    public InventoryCountingQueryService(InventoryPhysicalSessionRepository sessionRepository,
                                         StockRepository stockRepository) {
        this.sessionRepository = sessionRepository;
        this.stockRepository = stockRepository;
    }

    public InventoryPhysicalSession getSessionEntity(Long id, Long companyId) {
        return sessionRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Sessão de inventário não encontrada."));
    }

    public InventorySessionDTO getSessionById(Long sessionId, Long companyId) {
        return toDTO(getSessionEntity(sessionId, companyId), false);
    }

    public List<InventorySessionDTO> listSessions(Long companyId) {
        return sessionRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(s -> toDTO(s, true))
                .toList();
    }

    public BigDecimal calculateTotalStock(Long productId) {
        List<Stock> stocks = stockRepository.findByProductId(productId);
        BigDecimal total = BigDecimal.ZERO;
        for (Stock s : stocks) {
            if (s.getQuantity() != null) {
                total = total.add(s.getQuantity());
            }
        }
        return total;
    }

    public InventorySessionDTO toDTO(InventoryPhysicalSession session, boolean summaryOnly) {
        List<InventoryItemDTO> itemDTOs = summaryOnly ? List.of() : session.getItems().stream()
                .map(item -> new InventoryItemDTO(
                        item.getId(), item.getProduct().getId(), item.getProductCode(),
                        item.getProductName(), item.getBarcode(), item.getExpectedQuantity(),
                        item.getCountedQuantity(), item.getDifference(), item.getUnitCost(),
                        item.getFinancialImpact(), item.getNotes()
                ))
                .toList();

        return new InventorySessionDTO(
                session.getId(), session.getInventoryNumber(), session.getDescription(),
                session.getStatus(), session.isBlindCounting(), session.getStartDate(),
                session.getEndDate(), session.getCompanyId(), session.getTotalItems(),
                session.getItemsCounted(), session.getTotalSurplusValue(),
                session.getTotalDeficitValue(), session.getNetFinancialImpact(),
                itemDTOs, session.getNotes()
        );
    }
}
