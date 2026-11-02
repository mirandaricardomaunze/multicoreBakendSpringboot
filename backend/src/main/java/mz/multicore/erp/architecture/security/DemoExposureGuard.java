package mz.multicore.erp.architecture.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Os dados de demonstração trazem contas com senhas conhecidas, como admin/admin. Por isso o
 * servidor recusa arrancar com eles se estiver acessível a partir de outra máquina.
 */
@Component
public class DemoExposureGuard {

    private static final Set<String> LOOPBACK = Set.of("127.0.0.1", "localhost", "::1", "0:0:0:0:0:0:0:1");

    public DemoExposureGuard(@Value("${app.seed-demo-data:false}") boolean seedDemoData,
                             @Value("${server.address:}") String serverAddress) {
        if (seedDemoData && !LOOPBACK.contains(serverAddress.trim().toLowerCase())) {
            throw new IllegalStateException("Dados de demonstração com senhas conhecidas não podem ficar "
                    + "acessíveis na rede. Use server.address=127.0.0.1 ou app.seed-demo-data=false.");
        }
    }
}
