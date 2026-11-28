package mz.multicore.erp.modules.onlineshop.controller;

import mz.multicore.erp.modules.onlineshop.dto.OnlineShopSettingsDTO;
import mz.multicore.erp.modules.onlineshop.dto.SaveOnlineShopSettingsRequest;
import mz.multicore.erp.modules.onlineshop.service.OnlineShopSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal endpoints behind Configurações › Integrações › Loja online (docs/LOJA_ONLINE_SPEC.md §3.5, LO-UI-01):
 * the ADMIN reads and saves the online shop settings of the active company. {@code ApiRolePolicy} refuses every
 * other role; the service keeps its own ADMIN guard and the LO-01 validation messages.
 */
@RestController
@RequestMapping("/api/online-shop/settings")
public class OnlineShopSettingsController {

    private final OnlineShopSettingsService settingsService;

    public OnlineShopSettingsController(OnlineShopSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public OnlineShopSettingsDTO get() {
        return settingsService.get();
    }

    @PutMapping
    public OnlineShopSettingsDTO save(@Valid @RequestBody SaveOnlineShopSettingsRequest request) {
        return settingsService.save(request);
    }
}
