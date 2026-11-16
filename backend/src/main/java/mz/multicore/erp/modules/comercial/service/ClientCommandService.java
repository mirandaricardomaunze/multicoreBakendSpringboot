package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comandos de escrita de clientes para os outros módulos (seeders): gravam por aqui, nunca pelo
 * {@link ClientRepository} (docs/MODULE_DECOUPLING_SPEC.md §2). Leitura: {@link ClientLookup}.
 */
@Service
@Transactional
public class ClientCommandService {

    private final ClientRepository clientRepository;

    public ClientCommandService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Client saveClient(Client client) {
        return clientRepository.save(client);
    }
}
