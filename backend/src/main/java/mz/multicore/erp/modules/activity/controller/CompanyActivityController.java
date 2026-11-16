package mz.multicore.erp.modules.activity.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.activity.dto.ActivityCatalogDTO;
import mz.multicore.erp.modules.activity.dto.CompanyActivityDTO;
import mz.multicore.erp.modules.activity.dto.UpdateCompanyActivityRequest;
import mz.multicore.erp.modules.activity.service.CompanyActivityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Company activity profile (SA-C01). Everyone may read it; only management may change it. */
@RestController
@RequestMapping("/api/activity")
public class CompanyActivityController {

    private final CompanyActivityService activityService;

    public CompanyActivityController(CompanyActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/catalog")
    public ResponseEntity<ActivityCatalogDTO> catalog() {
        return ResponseEntity.ok(activityService.catalog());
    }

    @GetMapping("/company")
    public ResponseEntity<CompanyActivityDTO> company(@RequestParam Long companyId) {
        return ResponseEntity.ok(activityService.getForCompany(companyId));
    }

    @PutMapping("/company")
    public ResponseEntity<CompanyActivityDTO> update(@RequestBody @Valid UpdateCompanyActivityRequest request) {
        return ResponseEntity.ok(activityService.update(request));
    }
}
