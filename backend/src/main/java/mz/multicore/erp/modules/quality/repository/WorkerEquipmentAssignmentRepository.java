package mz.multicore.erp.modules.quality.repository;

import mz.multicore.erp.modules.quality.dto.AssignmentStatus;
import mz.multicore.erp.modules.quality.model.WorkerEquipmentAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkerEquipmentAssignmentRepository extends JpaRepository<WorkerEquipmentAssignment, Long> {

    Optional<WorkerEquipmentAssignment> findByIdAndCompanyId(Long id, Long companyId);

    @Query("SELECT a FROM WorkerEquipmentAssignment a " +
            "JOIN FETCH a.equipment eq " +
            "JOIN FETCH a.employee emp " +
            "JOIN FETCH a.company c " +
            "WHERE a.id = :id AND a.company.id = :companyId")
    Optional<WorkerEquipmentAssignment> findByIdWithDetailsAndCompanyId(
            @Param("id") Long id,
            @Param("companyId") Long companyId
    );

    @Query("SELECT a FROM WorkerEquipmentAssignment a " +
            "JOIN FETCH a.equipment eq " +
            "JOIN FETCH a.employee emp " +
            "WHERE a.company.id = :companyId AND " +
            "(:status IS NULL OR a.status = :status) AND " +
            "(:employeeId IS NULL OR emp.id = :employeeId) AND " +
            "(:search IS NULL OR LOWER(emp.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            " LOWER(eq.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(eq.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<WorkerEquipmentAssignment> searchAssignments(
            @Param("companyId") Long companyId,
            @Param("status") AssignmentStatus status,
            @Param("employeeId") Long employeeId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT a FROM WorkerEquipmentAssignment a " +
            "JOIN FETCH a.equipment eq " +
            "JOIN FETCH a.employee emp " +
            "WHERE a.company.id = :companyId AND a.status IN ('ACTIVE', 'EXPIRING_SOON', 'EXPIRED') " +
            "AND a.expiryDate < :today " +
            "ORDER BY emp.name ASC, a.expiryDate ASC")
    List<WorkerEquipmentAssignment> findExpiredActiveAssignments(
            @Param("companyId") Long companyId,
            @Param("today") LocalDate today
    );

    @Query("SELECT a FROM WorkerEquipmentAssignment a " +
            "JOIN FETCH a.equipment eq " +
            "JOIN FETCH a.employee emp " +
            "WHERE a.company.id = :companyId AND a.status IN ('ACTIVE', 'EXPIRING_SOON') " +
            "AND a.expiryDate >= :today AND a.expiryDate <= :untilDate " +
            "ORDER BY a.expiryDate ASC")
    List<WorkerEquipmentAssignment> findExpiringSoonAssignments(
            @Param("companyId") Long companyId,
            @Param("today") LocalDate today,
            @Param("untilDate") LocalDate untilDate
    );

    long countByCompanyIdAndStatus(Long companyId, AssignmentStatus status);

    @Query("SELECT COUNT(a) FROM WorkerEquipmentAssignment a WHERE a.company.id = :companyId AND a.status IN ('ACTIVE', 'EXPIRING_SOON', 'EXPIRED')")
    long countActiveAssignments(@Param("companyId") Long companyId);

    @Query("SELECT COUNT(DISTINCT a.employee.id) FROM WorkerEquipmentAssignment a " +
            "WHERE a.company.id = :companyId AND a.status IN ('ACTIVE', 'EXPIRING_SOON', 'EXPIRED') AND a.expiryDate < :today")
    long countDistinctEmployeesWithExpiredEquipment(@Param("companyId") Long companyId, @Param("today") LocalDate today);
}
