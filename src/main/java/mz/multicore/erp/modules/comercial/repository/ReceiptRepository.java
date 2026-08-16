package mz.multicore.erp.modules.comercial.repository;

import mz.multicore.erp.modules.comercial.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    List<Receipt> findByCompanyId(Long companyId);
}
