package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.WarehouseService;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopDeliveryZoneDTO;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopSettingsDTO;
import mz.multicore.erp.modules.onlineshop.dto.SaveOnlineShopSettingsRequest;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopDeliveryZone;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness LO-01 (docs/LOJA_ONLINE_SPEC.md §3.1): only the ADMIN activates the online shop; the public code is
 * lower-case letters, digits and hyphens and unique in the system; pickup stores are sales warehouses of the company;
 * delivery zones have a name and a fee >= 0; the automatic cancellation period defaults to 7 days.
 */
class OnlineShopSettingsTest {

    private static final Long COMPANY_ID = 1L;

    private final OnlineShopSettingsRepository repository = mock(OnlineShopSettingsRepository.class);
    private final WarehouseService warehouseService = mock(WarehouseService.class);
    private final CompanyService companyService = mock(CompanyService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final DeliveryFeeProductProvisioner deliveryFeeProducts = mock(DeliveryFeeProductProvisioner.class);
    private final AtomicReference<OnlineShopSettings> stored = new AtomicReference<>();

    private final OnlineShopSettingsService service = new OnlineShopSettingsService(repository, warehouseService,
            companyService, auditLogService, deliveryFeeProducts);

    @BeforeEach
    void setUp() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        Company company = new Company();
        company.setId(COMPANY_ID);
        when(companyService.getCurrentCompanyReference(COMPANY_ID)).thenReturn(company);
        when(warehouseService.getSalesWarehousesByCompany(COMPANY_ID))
                .thenReturn(List.of(warehouse(10L), warehouse(11L)));
        when(repository.findByCompanyId(COMPANY_ID)).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(repository.save(any(OnlineShopSettings.class))).thenAnswer(invocation -> {
            OnlineShopSettings settings = invocation.getArgument(0);
            stored.set(settings);
            return settings;
        });
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void lo01_semDefinicoesALojaEstaDesactivadaComPrazoDeSeteDias() {
        OnlineShopSettingsDTO settings = service.get();

        assertFalse(settings.enabled());
        assertNull(settings.publicCode());
        assertTrue(settings.pickupWarehouseIds().isEmpty());
        assertTrue(settings.deliveryZones().isEmpty());
        assertEquals(7, settings.autoCancelDays());
    }

    @Test
    void lo01_activaALojaComCodigoLojasZonasEPrazoPorOmissao() {
        OnlineShopSettingsDTO saved = service.save(new SaveOnlineShopSettingsRequest(true, "  Mercearia-Baixa ",
                List.of(10L, 11L), List.of(zone(" Polana ", "150.00"), zone("Matola", "0")), " 84 123 4567 ", null));

        assertTrue(saved.enabled());
        assertEquals("mercearia-baixa", saved.publicCode());
        assertEquals(List.of(10L, 11L), saved.pickupWarehouseIds());
        assertEquals(List.of(zone("Polana", "150.00"), zone("Matola", "0.00")), saved.deliveryZones());
        assertEquals("84 123 4567", saved.contactPhone());
        assertEquals(7, saved.autoCancelDays());
        assertEquals(COMPANY_ID, stored.get().getCompany().getId());
        assertEquals(saved, service.get());
        verify(auditLogService).logCurrent(eq("ONLINE_SHOP_SETTINGS"), anyString());
    }

    @Test
    void lo01_prazoDefinidoPeloAdministrador() {
        OnlineShopSettingsDTO saved = service.save(request("loja-1", List.of(10L), List.of(), 3));

        assertEquals(3, saved.autoCancelDays());
        assertMessage("O prazo de cancelamento automático tem de ser de pelo menos 1 dia.",
                () -> service.save(request("loja-1", List.of(10L), List.of(), 0)));
    }

    @Test
    void lo01_codigoPublicoObrigatorioEValido() {
        String invalid = "O código público só pode ter letras minúsculas, números e hífen (ex.: mercearia-baixa).";
        assertMessage("Indique o código público da loja.",
                () -> service.save(request("  ", List.of(10L), List.of(), null)));
        assertMessage("Indique o código público da loja.",
                () -> service.save(request(null, List.of(10L), List.of(), null)));
        assertMessage(invalid, () -> service.save(request("mercearia baixa", List.of(10L), List.of(), null)));
        assertMessage(invalid, () -> service.save(request("loja_1", List.of(10L), List.of(), null)));
        assertMessage(invalid, () -> service.save(request("-loja", List.of(10L), List.of(), null)));
        assertMessage(invalid, () -> service.save(request("loja-", List.of(10L), List.of(), null)));
        assertMessage(invalid, () -> service.save(request("café", List.of(10L), List.of(), null)));
        assertMessage(invalid, () -> service.save(request("a".repeat(51), List.of(10L), List.of(), null)));
        verify(repository, never()).save(any());
    }

    @Test
    void lo01_codigoPublicoUnicoNoSistema() {
        when(repository.existsByPublicCodeAndCompanyIdNot("mercearia-baixa", COMPANY_ID)).thenReturn(true);

        assertMessage("O código público mercearia-baixa já está a ser usado por outra loja.",
                () -> service.save(request("mercearia-baixa", List.of(10L), List.of(), null)));
        verify(repository, never()).save(any());
    }

    @Test
    void lo01_lojasDeLevantamentoSaoArmazensDeVendaDaEmpresa() {
        assertMessage("Escolha pelo menos uma loja de levantamento.",
                () -> service.save(request("loja-1", List.of(), List.of(), null)));
        assertMessage("Escolha pelo menos uma loja de levantamento.",
                () -> service.save(request("loja-1", null, List.of(), null)));
        assertMessage("A loja de levantamento escolhida não é um armazém de venda desta empresa.",
                () -> service.save(request("loja-1", List.of(10L, 99L), List.of(), null)));
        verify(repository, never()).save(any());
    }

    @Test
    void lo01_zonasComNomeUnicoETaxaNaoNegativa() {
        assertMessage("Indique o nome da zona de entrega.",
                () -> service.save(request("loja-1", List.of(10L), List.of(zone(" ", "10")), null)));
        assertMessage("A taxa de entrega da zona Polana não pode ser negativa.",
                () -> service.save(request("loja-1", List.of(10L), List.of(zone("Polana", "-1")), null)));
        assertMessage("Indique a taxa de entrega da zona Polana.",
                () -> service.save(request("loja-1", List.of(10L), List.of(zone("Polana", null)), null)));
        assertMessage("A zona de entrega polana está repetida.",
                () -> service.save(request("loja-1", List.of(10L),
                        List.of(zone("Polana", "10"), zone("polana ", "20")), null)));
        verify(repository, never()).save(any());
    }

    @Test
    void lo01_desactivarMantemAsDefinicoes() {
        service.save(request("loja-1", List.of(10L), List.of(zone("Polana", "10")), null));

        OnlineShopSettingsDTO disabled = service.save(new SaveOnlineShopSettingsRequest(false, "loja-1",
                List.of(10L), List.of(zone("Polana", "10")), null, null));

        assertFalse(disabled.enabled());
        assertEquals("loja-1", disabled.publicCode());
        assertFalse(service.get().enabled());
    }

    @Test
    void lo01_soOAdministradorGereALoja() {
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");

        assertMessage("Só o administrador gere a loja online.", service::get);
        assertMessage("Só o administrador gere a loja online.",
                () -> service.save(request("loja-1", List.of(10L), List.of(), null)));
        verify(repository, never()).save(any());
    }

    // ─── LO-08: artigo da taxa de entrega criado na activação ─────────────────

    @Test
    void lo08_activarCriaOArtigoDaTaxaDeEntregaDeCadaZona() {
        service.save(request("loja-1", List.of(10L), List.of(zone("Polana", "150"), zone("Matola", "0")), null));

        ArgumentCaptor<OnlineShopDeliveryZone> zones = ArgumentCaptor.forClass(OnlineShopDeliveryZone.class);
        verify(deliveryFeeProducts, times(2)).forZone(eq(stored.get().getCompany()), zones.capture());
        assertEquals(List.of("Polana", "Matola"),
                zones.getAllValues().stream().map(OnlineShopDeliveryZone::getName).toList());
    }

    @Test
    void lo08_lojaDesactivadaNaoCriaArtigos() {
        service.save(new SaveOnlineShopSettingsRequest(false, "loja-1", List.of(10L), List.of(zone("Polana", "10")),
                null, null));

        verify(deliveryFeeProducts, never()).forZone(any(), any());
    }

    private static SaveOnlineShopSettingsRequest request(String code, List<Long> pickups,
                                                         List<OnlineShopDeliveryZoneDTO> zones, Integer days) {
        return new SaveOnlineShopSettingsRequest(true, code, pickups, zones, null, days);
    }

    private static OnlineShopDeliveryZoneDTO zone(String name, String fee) {
        return new OnlineShopDeliveryZoneDTO(name, fee == null ? null : new BigDecimal(fee));
    }

    private static Warehouse warehouse(Long id) {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        return warehouse;
    }

    private static void assertMessage(String expected, Executable executable) {
        assertEquals(expected, assertThrows(BusinessRuleException.class, executable).getMessage());
    }
}
