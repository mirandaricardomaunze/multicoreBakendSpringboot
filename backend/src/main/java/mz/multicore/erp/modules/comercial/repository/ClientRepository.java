package mz.multicore.erp.modules.comercial.repository;

import mz.multicore.erp.modules.comercial.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByTaxId(String taxId);
    Optional<Client> findByTaxIdAndCompaniesId(String taxId, Long companyId);
    Optional<Client> findByIdAndCompaniesId(Long id, Long companyId);
    List<Client> findDistinctByCompaniesIdOrderByName(Long companyId);
}
