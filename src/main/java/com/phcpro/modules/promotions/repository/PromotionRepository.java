package com.phcpro.modules.promotions.repository;

import com.phcpro.modules.promotions.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("SELECT p FROM Promotion p LEFT JOIN FETCH p.product LEFT JOIN FETCH p.category " +
            "WHERE p.company.id = :companyId ORDER BY p.active DESC, p.endDate DESC")
    List<Promotion> findByCompany(@Param("companyId") Long companyId);

    /** Promoções activas e dentro da janela de validade na data indicada (para aplicar na venda). */
    @Query("SELECT p FROM Promotion p LEFT JOIN FETCH p.product LEFT JOIN FETCH p.category " +
            "WHERE p.company.id = :companyId AND p.active = true " +
            "AND p.startDate <= :date AND p.endDate >= :date")
    List<Promotion> findActiveByCompany(@Param("companyId") Long companyId,
                                        @Param("date") LocalDate date);
}
