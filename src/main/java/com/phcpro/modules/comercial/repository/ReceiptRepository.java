package com.phcpro.modules.comercial.repository;

import com.phcpro.modules.comercial.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findByCompanyId(Long companyId);
}
