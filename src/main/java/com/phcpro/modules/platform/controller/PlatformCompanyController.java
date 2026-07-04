package com.phcpro.modules.platform.controller;

import com.phcpro.modules.platform.dto.CreateCompanyRequest;
import com.phcpro.modules.platform.dto.PlatformCompanyDTO;
import com.phcpro.modules.platform.dto.UpdateCompanyRequest;
import com.phcpro.modules.platform.service.PlatformCompanyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Endpoints de plataforma para gerir empresas. Protegidos pelo caminho /api/platform/** (superadmin). */
@RestController
@RequestMapping("/api/platform/companies")
public class PlatformCompanyController {

    private final PlatformCompanyService platformCompanyService;

    public PlatformCompanyController(PlatformCompanyService platformCompanyService) {
        this.platformCompanyService = platformCompanyService;
    }

    @GetMapping
    public List<PlatformCompanyDTO> list() {
        return platformCompanyService.listCompanies();
    }

    @PostMapping
    public PlatformCompanyDTO create(@Valid @RequestBody CreateCompanyRequest request) {
        return platformCompanyService.createCompany(request);
    }

    @PutMapping("/{id}")
    public PlatformCompanyDTO update(@PathVariable Long id, @Valid @RequestBody UpdateCompanyRequest request) {
        return platformCompanyService.updateCompany(id, request);
    }

    @PatchMapping("/{id}/active")
    public PlatformCompanyDTO setActive(@PathVariable Long id, @RequestBody SetActiveRequest request) {
        return platformCompanyService.setCompanyActive(id, request.active());
    }

    public record SetActiveRequest(boolean active) {}
}
