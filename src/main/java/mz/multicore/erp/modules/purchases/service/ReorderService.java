package mz.multicore.erp.modules.purchases.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.inventory.model.Stock;
import mz.multicore.erp.modules.inventory.repository.StockRepository;
import mz.multicore.erp.modules.purchases.dto.ReorderSuggestionDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reposição automática de stock: sugere que produtos encomendar ao fornecedor, a partir do
 * <b>stock mínimo</b> definido em cada produto. Leitura pura (não cria encomendas) — o operador
 * confirma e cria a encomenda com o fornecedor à escolha.
 *
 * Regra: para produtos com controlo de stock e {@code minStock > 0} cujo stock total da empresa
 * (soma de todos os armazéns) está <b>abaixo do mínimo</b>, sugere a quantidade que falta para
 * repor o mínimo, <b>arredondada para cima a caixas inteiras</b> (a loja compra ao grosso).
 */
@Service
public class ReorderService {

    private final StockRepository stockRepository;
    private final ProductRepository productRepository;

    public ReorderService(StockRepository stockRepository, ProductRepository productRepository) {
        this.stockRepository = stockRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestionDTO> suggestions(Long companyId) {
        CurrentUserContext.requireCompany(companyId);

        // Stock total por produto (soma de todos os armazéns da empresa).
        Map<Long, BigDecimal> stockByProduct = new HashMap<>();
        for (Stock s : stockRepository.findByWarehouseCompanyId(companyId)) {
            Long pid = s.getProduct().getId();
            BigDecimal qty = s.getQuantity() == null ? BigDecimal.ZERO : s.getQuantity();
            stockByProduct.merge(pid, qty, BigDecimal::add);
        }

        List<ReorderSuggestionDTO> out = new java.util.ArrayList<>();
        for (Product p : productRepository.findDistinctByCompaniesIdOrderByName(companyId)) {
            if (!p.isStockTracked()) continue;
            BigDecimal min = p.getMinStock();
            if (min == null || min.signum() <= 0) continue; // sem ponto de reposição definido

            BigDecimal current = stockByProduct.getOrDefault(p.getId(), BigDecimal.ZERO);
            if (current.compareTo(min) >= 0) continue; // stock suficiente

            int upb = p.getUnitsPerBox() <= 0 ? 1 : p.getUnitsPerBox();
            BigDecimal deficit = min.subtract(current);
            // Arredonda para cima a caixas inteiras.
            BigDecimal boxes = deficit.divide(BigDecimal.valueOf(upb), 0, RoundingMode.CEILING);
            BigDecimal suggestedUnits = boxes.multiply(BigDecimal.valueOf(upb));

            out.add(new ReorderSuggestionDTO(
                    p.getId(), p.getSku(), p.getName(),
                    current, min, upb, boxes, suggestedUnits));
        }

        // Mais urgentes primeiro: menor cobertura (current/min) ao topo.
        out.sort(Comparator.comparing((ReorderSuggestionDTO d) ->
                d.currentStock().divide(d.minStock(), 4, RoundingMode.HALF_UP)));
        return out;
    }
}
