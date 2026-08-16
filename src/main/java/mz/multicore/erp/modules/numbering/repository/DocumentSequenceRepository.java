package mz.multicore.erp.modules.numbering.repository;

import mz.multicore.erp.modules.numbering.model.DocumentSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, Long> {

    /**
     * Lê o contador da empresa/série/ano com bloqueio de escrita pessimista, garantindo que
     * duas transações concorrentes não obtêm o mesmo número. Numeração é <b>por empresa</b>.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from DocumentSequence s where s.companyId = :companyId and s.series = :series and s.year = :year")
    Optional<DocumentSequence> lockByCompanyAndSeriesAndYear(@Param("companyId") Long companyId,
                                                             @Param("series") String series,
                                                             @Param("year") int year);
}
