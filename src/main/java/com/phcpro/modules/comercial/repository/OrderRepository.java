package com.phcpro.modules.comercial.repository;

import com.phcpro.modules.comercial.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCompanyId(Long companyId);
}
