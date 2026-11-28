package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.WarehouseService;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopDeliveryZoneDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopSettingsDTO;
import mz.multicore.erp.modules.onlineshop.dto.SaveOnlineShopSettingsRequest;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Online shop settings of the active company (docs/LOJA_ONLINE_SPEC.md §3.1, LO-01): only the ADMIN reads and saves
 * them; the public code is unique in the system; pickup stores are the company's sales warehouses. Every save is
 * audited. Activating the shop provisions the delivery fee service product of each zone (LO-08).
 */
@Service
public class OnlineShopSettingsService {

    private final OnlineShopSettingsRepository repository;
    private final WarehouseService warehouseService;
    private final CompanyService companyService;
    private final AuditLogService auditLogService;
    private final DeliveryFeeProductProvisioner deliveryFeeProducts;

    public OnlineShopSettingsService(OnlineShopSettingsRepository repository, WarehouseService warehouseService,
                                     CompanyService companyService, AuditLogService auditLogService,
                                     DeliveryFeeProductProvisioner deliveryFeeProducts) {
        this.repository = repository;
        this.warehouseService = warehouseService;
        this.companyService = companyService;
        this.auditLogService = auditLogService;
        this.deliveryFeeProducts = deliveryFeeProducts;
    }

    @Transactional(readOnly = true)
    public OnlineShopSettingsDTO get() {
        OnlineShopSettingsRules.requireAdmin(PermissionGuard.isAdmin());
        return repository.findByCompanyId(CurrentUserContext.requireCurrentCompanyId())
                .map(OnlineShopSettingsService::toDTO)
                .orElseGet(() -> new OnlineShopSettingsDTO(false, null, List.of(), List.of(), null,
                        OnlineShopSettings.DEFAULT_AUTO_CANCEL_DAYS));
    }

    @Transactional
    public OnlineShopSettingsDTO save(SaveOnlineShopSettingsRequest request) {
        OnlineShopSettingsRules.requireAdmin(PermissionGuard.isAdmin());
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        boolean enabled = request.enabled();
        String code = OnlineShopSettingsRules.publicCode(request.publicCode(), enabled);
        if (code != null && repository.existsByPublicCodeAndCompanyIdNot(code, companyId)) {
            throw new BusinessRuleException("O código público " + code + " já está a ser usado por outra loja.");
        }
        List<Long> pickups = OnlineShopSettingsRules.pickupWarehouses(request.pickupWarehouseIds(),
                salesWarehouseIds(companyId), enabled);
        var zones = OnlineShopSettingsRules.deliveryZones(request.deliveryZones());
        int days = OnlineShopSettingsRules.autoCancelDays(request.autoCancelDays());
        String phone = OnlineShopSettingsRules.contactPhone(request.contactPhone());

        OnlineShopSettings settings = repository.findByCompanyId(companyId).orElseGet(() -> {
            OnlineShopSettings created = new OnlineShopSettings();
            created.setCompany(companyService.getCurrentCompanyReference(companyId));
            created.setCreatedBy(CurrentUserContext.getUsername());
            return created;
        });
        settings.setEnabled(enabled);
        settings.setPublicCode(code);
        settings.getPickupWarehouseIds().clear();
        settings.getPickupWarehouseIds().addAll(pickups);
        settings.getDeliveryZones().clear();
        settings.getDeliveryZones().addAll(zones);
        settings.setAutoCancelDays(days);
        settings.setContactPhone(phone);
        OnlineShopSettings saved = repository.save(settings);
        if (enabled) {
            saved.getDeliveryZones().forEach(zone -> deliveryFeeProducts.forZone(saved.getCompany(), zone));
        }

        auditLogService.logCurrent("ONLINE_SHOP_SETTINGS", describe(saved));
        return toDTO(saved);
    }

    private Set<Long> salesWarehouseIds(Long companyId) {
        return warehouseService.getSalesWarehousesByCompany(companyId).stream()
                .map(Warehouse::getId).collect(Collectors.toSet());
    }

    private static String describe(OnlineShopSettings settings) {
        return "Loja online " + (settings.isEnabled() ? "activada" : "desactivada")
                + (settings.getPublicCode() != null ? " em /loja/" + settings.getPublicCode() : "")
                + ", " + settings.getPickupWarehouseIds().size() + " loja(s) de levantamento, "
                + settings.getDeliveryZones().size() + " zona(s) de entrega, prazo "
                + settings.getAutoCancelDays() + " dia(s)";
    }

    private static OnlineShopSettingsDTO toDTO(OnlineShopSettings settings) {
        return new OnlineShopSettingsDTO(settings.isEnabled(), settings.getPublicCode(),
                List.copyOf(settings.getPickupWarehouseIds()),
                settings.getDeliveryZones().stream()
                        .map(zone -> new OnlineShopDeliveryZoneDTO(zone.getName(), zone.getFee())).toList(),
                settings.getContactPhone(), settings.getAutoCancelDays());
    }
}
