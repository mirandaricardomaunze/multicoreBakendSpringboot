package mz.multicore.erp.modules.licensing.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import mz.multicore.erp.modules.licensing.dto.AcceptLicenseRequest;
import mz.multicore.erp.modules.licensing.dto.LicenseAcceptanceDTO;
import mz.multicore.erp.modules.licensing.dto.LicenseTermsDTO;
import mz.multicore.erp.modules.licensing.service.LicenseAcceptanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/license")
public class LicenseAcceptanceController {

    private final LicenseAcceptanceService service;

    public LicenseAcceptanceController(LicenseAcceptanceService service) {
        this.service = service;
    }

    @GetMapping("/current")
    public LicenseTermsDTO current() {
        return service.currentTerms();
    }

    @PostMapping("/accept")
    public LicenseAcceptanceDTO accept(@Valid @RequestBody AcceptLicenseRequest request,
                                       HttpServletRequest httpRequest) {
        return service.accept(request, clientIp(httpRequest), httpRequest.getHeader("X-Client-Version"),
                httpRequest.getHeader("User-Agent"));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr()
                : forwarded.split(",", 2)[0].trim();
    }
}
