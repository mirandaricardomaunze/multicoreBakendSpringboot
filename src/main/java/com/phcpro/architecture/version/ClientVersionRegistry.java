package com.phcpro.architecture.version;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda que versão do programa cada empresa está a usar.
 *
 * <p>Duas regras que este serviço não pode quebrar:
 *
 * <ol>
 *   <li><b>Não escrever a cada pedido.</b> Uma loja faz milhares de pedidos por dia e a versão
 *       dela muda uma vez por mês. Gravar sempre seria transformar informação de gestão num
 *       problema de desempenho. Só se grava quando passou {@link #THROTTLE} desde a última vez
 *       — o resto do tempo a decisão é tomada em memória.</li>
 *   <li><b>Nunca partir um pedido.</b> Isto é informação para quem gere, não parte da venda. Se
 *       a gravação falhar, engole-se o erro: mais vale ficar sem o dado do que impedir a loja de
 *       facturar por causa dele.</li>
 * </ol>
 */
@Service
public class ClientVersionRegistry {

    /** Intervalo mínimo entre gravações da mesma (empresa, versão). */
    private static final Duration THROTTLE = Duration.ofMinutes(15);

    private final ClientVersionSightingRepository repository;

    /** Última gravação por chave, para não ir à base de dados a cada pedido. */
    private final Map<String, LocalDateTime> lastPersisted = new ConcurrentHashMap<>();

    public ClientVersionRegistry(ClientVersionSightingRepository repository) {
        this.repository = repository;
    }

    /**
     * Regista que esta empresa foi vista nesta versão. Chamada por pedido HTTP autenticado —
     * daí a travagem e o silêncio em caso de falha.
     */
    public void record(Long companyId, String clientVersion, String username) {
        if (companyId == null || clientVersion == null || clientVersion.isBlank()) return;

        String version = clientVersion.trim();
        String key = companyId + "|" + version;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = lastPersisted.get(key);
        if (last != null && Duration.between(last, now).compareTo(THROTTLE) < 0) {
            return;
        }
        lastPersisted.put(key, now);

        try {
            persist(companyId, version, username, now);
        } catch (RuntimeException ex) {
            // Informação de gestão nunca pode fazer cair um pedido de negócio.
            lastPersisted.remove(key); // tenta outra vez no próximo pedido
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void persist(Long companyId, String version, String username, LocalDateTime now) {
        ClientVersionSighting sighting = repository.findByCompanyIdAndClientVersion(companyId, version)
                .orElseGet(() -> {
                    ClientVersionSighting created = new ClientVersionSighting();
                    created.setCompanyId(companyId);
                    created.setClientVersion(version);
                    created.setFirstSeenAt(now);
                    created.setCreatedBy(username == null ? "SYSTEM" : username);
                    return created;
                });
        sighting.setLastSeenAt(now);
        if (username != null && !username.isBlank()) {
            sighting.setLastUsername(username);
        }
        repository.save(sighting);
    }

    /**
     * Quem está em que versão, do visto mais recentemente para o mais antigo.
     *
     * @param companyNames nomes por id, para o DTO sair completo do servidor. Recebido de fora
     *                     porque este serviço vive em {@code architecture} e não deve passar a
     *                     conhecer o módulo das empresas só para escrever um nome.
     */
    @Transactional(readOnly = true)
    public List<ClientVersionUsageDTO> listUsage(Map<Long, String> companyNames) {
        Map<Long, String> names = companyNames == null ? Map.of() : companyNames;
        return repository.findAllByOrderByLastSeenAtDesc().stream()
                .map(s -> new ClientVersionUsageDTO(
                        s.getCompanyId(),
                        names.getOrDefault(s.getCompanyId(), "Empresa " + s.getCompanyId()),
                        s.getClientVersion(),
                        s.getLastUsername(),
                        s.getFirstSeenAt(),
                        s.getLastSeenAt()))
                .toList();
    }
}
