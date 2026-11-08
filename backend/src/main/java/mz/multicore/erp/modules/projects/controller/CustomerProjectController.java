package mz.multicore.erp.modules.projects.controller;

import jakarta.validation.Valid;
import mz.multicore.erp.modules.projects.dto.CustomerProjectDTO;
import mz.multicore.erp.modules.projects.dto.ProjectStatementDTO;
import mz.multicore.erp.modules.projects.dto.SaveCustomerProjectRequest;
import mz.multicore.erp.modules.projects.service.CustomerProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Client projects (SA-C09). Sellers create and link; only management unlinks (see {@code ApiRolePolicy}). */
@RestController
@RequestMapping("/api/projects")
public class CustomerProjectController {

    private final CustomerProjectService projectService;

    public CustomerProjectController(CustomerProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<List<CustomerProjectDTO>> list(@RequestParam Long companyId, @RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(projectService.list(companyId, clientId));
    }

    @PutMapping
    public ResponseEntity<CustomerProjectDTO> save(@RequestBody @Valid SaveCustomerProjectRequest request) {
        return ResponseEntity.ok(projectService.save(request));
    }

    @GetMapping("/{id}/statement")
    public ResponseEntity<ProjectStatementDTO> statement(@RequestParam Long companyId, @PathVariable Long id) {
        return ResponseEntity.ok(projectService.statement(companyId, id));
    }

    @PutMapping("/{id}/orders/{orderId}")
    public ResponseEntity<CustomerProjectDTO> attach(@RequestParam Long companyId, @PathVariable Long id, @PathVariable Long orderId) {
        return ResponseEntity.ok(projectService.attachOrder(companyId, id, orderId));
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<Void> detach(@RequestParam Long companyId, @PathVariable Long orderId) {
        projectService.detachOrder(companyId, orderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<CustomerProjectDTO> ofOrder(@RequestParam Long companyId, @PathVariable Long orderId) {
        return projectService.projectOfOrder(companyId, orderId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
