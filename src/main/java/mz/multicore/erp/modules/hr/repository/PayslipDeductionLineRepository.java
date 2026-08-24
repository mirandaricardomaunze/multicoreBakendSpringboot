package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.PayslipDeductionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PayslipDeductionLineRepository extends JpaRepository<PayslipDeductionLine, Long> {

    /**
     * Quanto já foi descontado de um compromisso. <b>É daqui que sai o saldo em dívida</b> — não há
     * coluna de saldo, de propósito: uma anulação de recibo apaga as linhas e a dívida volta a
     * existir sozinha, sem ninguém ter de se lembrar de a repor.
     */
    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM PayslipDeductionLine l "
            + "WHERE l.deduction.id = :deductionId")
    BigDecimal sumApplied(@Param("deductionId") Long deductionId);

    List<PayslipDeductionLine> findByPayslipId(Long payslipId);

    void deleteByPayslipId(Long payslipId);
}
