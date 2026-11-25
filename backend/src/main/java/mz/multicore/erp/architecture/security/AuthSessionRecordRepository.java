package mz.multicore.erp.architecture.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuthSessionRecordRepository extends JpaRepository<AuthSessionRecord, String> {

    @Query("select s.tokenHash from AuthSessionRecord s where s.username = :username order by s.issuedAt asc")
    List<String> findHashesOldestFirst(@Param("username") String username);

    @Modifying
    @Query("delete from AuthSessionRecord s where s.username = :username")
    int deleteByUsername(@Param("username") String username);

    @Modifying
    @Query("update AuthSessionRecord s set s.expiresAt = :expiresAt where s.tokenHash = :tokenHash")
    int updateExpiresAt(@Param("tokenHash") String tokenHash, @Param("expiresAt") Instant expiresAt);

    @Modifying
    @Query("delete from AuthSessionRecord s where s.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
