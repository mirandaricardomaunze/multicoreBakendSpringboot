package mz.multicore.erp.modules.licensing.repository;

import mz.multicore.erp.modules.licensing.model.LicenseAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LicenseAcceptanceRepository extends JpaRepository<LicenseAcceptance, Long> {
    Optional<LicenseAcceptance> findByCompanyIdAndLicenseVersion(Long companyId, String licenseVersion);
}
