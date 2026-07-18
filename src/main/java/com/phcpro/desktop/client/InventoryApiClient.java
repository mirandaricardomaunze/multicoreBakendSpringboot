package com.phcpro.desktop.client;

import com.phcpro.modules.inventory.dto.ProductBatchDTO;
import com.phcpro.modules.inventory.dto.StockDTO;
import com.phcpro.modules.inventory.dto.WarehouseDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Cliente HTTP para o inventário ({@code /api/inventory}). Espelha o padrão do
 * {@link ComercialApiClient}. Cobre por agora as leituras usadas pelo dashboard; será estendido
 * quando o StockPanel migrar.
 */
@Component
@Profile("desktop")
public class InventoryApiClient {

    private final DesktopClientFactory clientFactory;

    public InventoryApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<StockDTO> getStocksByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/stocks?companyId=" + companyId, StockDTO.class);
    }

    public List<WarehouseDTO> getWarehousesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/warehouses?companyId=" + companyId, WarehouseDTO.class);
    }

    /**
     * Lotes vencidos ou a vencer nos próximos {@code daysAhead} dias. O endpoint recebe a data-limite
     * ({@code before}); traduzimos aqui o horizonte em dias para a data ISO que o servidor espera.
     */
    public List<ProductBatchDTO> findExpiringBatches(Long companyId, int daysAhead) {
        LocalDate before = LocalDate.now().plusDays(daysAhead);
        return clientFactory.authenticatedClient()
                .getList("/api/inventory/batches/expiring?companyId=" + companyId + "&before=" + before,
                        ProductBatchDTO.class);
    }
}
