package com.phcpro.architecture.version;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Versão do servidor e política de versões do cliente.
 *
 * <p>Público de propósito (como o {@code /actuator/health}): o desktop tem de o poder consultar
 * <b>antes</b> do login — é no arranque que faz sentido avisar que há versão nova, não depois de
 * o operador já estar a meio de uma venda.
 */
@RestController
@RequestMapping("/api/version")
public class VersionController {

    private final String serverVersion;
    private final String minClientVersion;

    public VersionController(
            @Value("${app.version:0.0.0}") String serverVersion,
            @Value("${app.client.min-version:0.0.0}") String minClientVersion) {
        this.serverVersion = serverVersion;
        this.minClientVersion = minClientVersion;
    }

    @GetMapping
    public ResponseEntity<ServerVersionDTO> getVersion() {
        return ResponseEntity.ok(new ServerVersionDTO(serverVersion, minClientVersion));
    }
}
