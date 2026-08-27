package mz.multicore.erp.modules.comercial.repository;

import mz.multicore.erp.modules.comercial.model.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    List<Quotation> findByCompanyIdOrderByQuotationDateDesc(Long companyId);

    /**
     * Carrega a cotação com as linhas, garantindo o escopo da empresa. É o <b>único</b> carregador
     * usado pelas acções do serviço — quem não tem acesso à empresa não encontra o documento, em
     * vez de o encontrar e ser recusado depois.
     */
    @Query("select distinct q from Quotation q left join fetch q.lines "
            + "where q.id = :id and q.company.id = :companyId")
    Optional<Quotation> findByIdWithLinesAndCompanyId(@Param("id") Long id,
                                                       @Param("companyId") Long companyId);
}
