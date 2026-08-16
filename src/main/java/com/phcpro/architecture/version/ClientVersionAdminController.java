package com.phcpro.architecture.version;

import com.phcpro.architecture.security.PermissionGuard;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Que versão está cada empresa a usar — a lista que se consulta <b>antes</b> de subir a versão
 * mínima. Subir sem olhar para aqui é decidir às cegas quem fica bloqueado.
 *
 * <p>Do dono da plataforma (superadmin): a decisão de compatibilidade é de quem opera o
 * servidor, não de cada empresa.
 */
@RestController
@RequestMapping("/api/platform/client-versions")
public class ClientVersionAdminController {

    private final ClientVersionRegistry registry;
    private final com.phcpro.modules.platform.service.PlatformCompanyService platformCompanyService;

    public ClientVersionAdminController(
            ClientVersionRegistry registry,
            com.phcpro.modules.platform.service.PlatformCompanyService platformCompanyService) {
        this.registry = registry;
        this.platformCompanyService = platformCompanyService;
    }

    /**
     * O nome da empresa é resolvido aqui, compondo os dois serviços — o registo de versões vive
     * em {@code architecture} e não deve passar a conhecer o módulo das empresas.
     */
    @GetMapping
    public ResponseEntity<List<ClientVersionUsageDTO>> listUsage() {
        PermissionGuard.requireSuperAdmin("consultar as versões dos clientes");
        java.util.Map<Long, String> names = platformCompanyService.listCompanies().stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.phcpro.modules.platform.dto.PlatformCompanyDTO::id,
                        com.phcpro.modules.platform.dto.PlatformCompanyDTO::name,
                        (a, b) -> a));
        return ResponseEntity.ok(registry.listUsage(names));
    }
}
