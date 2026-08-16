package com.phcpro.architecture.version;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientVersionSightingRepository extends JpaRepository<ClientVersionSighting, Long> {

    Optional<ClientVersionSighting> findByCompanyIdAndClientVersion(Long companyId, String clientVersion);

    /** Tudo o que se viu, do mais recente para o mais antigo — a lista que se olha antes de decidir. */
    List<ClientVersionSighting> findAllByOrderByLastSeenAtDesc();
}
