package mz.multicore.erp.modules.pricing.repository;

import mz.multicore.erp.modules.pricing.model.PriceListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PriceListItemRepository extends JpaRepository<PriceListItem, Long> {

    @Query("select i from PriceListItem i join fetch i.product p where i.priceList.id = :priceListId "
            + "order by p.name, i.minQuantity")
    List<PriceListItem> findByPriceListIdWithProduct(@Param("priceListId") Long priceListId);

    List<PriceListItem> findByPriceListIdAndProductId(Long priceListId, Long productId);

    Optional<PriceListItem> findByPriceListIdAndProductIdAndMinQuantity(Long priceListId, Long productId,
                                                                       BigDecimal minQuantity);

    long countByPriceListId(Long priceListId);
}
