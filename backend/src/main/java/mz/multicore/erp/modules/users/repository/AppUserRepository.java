package mz.multicore.erp.modules.users.repository;

import mz.multicore.erp.modules.users.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    List<AppUser> findDistinctByCompanyAccessesCompanyIdOrderByName(Long companyId);

    /** Atómico: entre dois logins com o mesmo código, só um actualiza a linha. */
    @Modifying
    @Query("update AppUser u set u.lastTotpStep = :step "
            + "where u.id = :id and (u.lastTotpStep is null or u.lastTotpStep < :step)")
    int claimTotpStep(@Param("id") Long id, @Param("step") long step);
}
