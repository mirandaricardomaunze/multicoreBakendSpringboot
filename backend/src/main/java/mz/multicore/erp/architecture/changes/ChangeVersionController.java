package mz.multicore.erp.architecture.changes;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Change counter of the active company (X-Company-Id), polled by desktops every few seconds. */
@RestController
@RequestMapping("/api/changes")
public class ChangeVersionController {

    private final ChangeVersionRegistry registry;

    public ChangeVersionController(ChangeVersionRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public ResponseEntity<ChangeVersionDTO> current() {
        return ResponseEntity.ok(registry.snapshot(CurrentUserContext.getCurrentCompanyId()));
    }
}
