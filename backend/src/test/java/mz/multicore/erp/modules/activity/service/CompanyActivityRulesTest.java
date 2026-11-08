package mz.multicore.erp.modules.activity.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.activity.model.ActivityCapability;
import mz.multicore.erp.modules.activity.model.ActivityProfile;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** SA-C01: pure activity profile rules ({@code docs/SECTORES_ACTIVIDADE_SPEC.md}). */
class CompanyActivityRulesTest {

    @Test
    void semPerfilGravadoValeComercioGeral() {
        assertThat(CompanyActivityService.effectiveProfiles(List.of())).containsExactly(ActivityProfile.GENERAL_RETAIL);
    }

    @Test
    void capacidadesSaoAUniaoDosPerfis() {
        Set<ActivityCapability> caps = CompanyActivityService.capabilitiesOf(
                List.of(ActivityProfile.BUILDING_MATERIALS, ActivityProfile.BLOCK_FACTORY));
        assertThat(caps).contains(ActivityCapability.PENDING_PICKUP, ActivityCapability.CUSTOMER_PROJECTS,
                ActivityCapability.BULK_STOCK, ActivityCapability.PRODUCTION, ActivityCapability.RETURNABLES);
    }

    @Test
    void sectoresPedidosTemAsCapacidadesCriticas() {
        assertThat(ActivityProfile.BUTCHER.capabilities())
                .contains(ActivityCapability.WEIGHED_SALES, ActivityCapability.PRODUCTION, ActivityCapability.PERISHABLES);
        assertThat(ActivityProfile.BAKERY.capabilities())
                .contains(ActivityCapability.PRODUCTION, ActivityCapability.CONSIGNMENT, ActivityCapability.MARKDOWNS);
        assertThat(ActivityProfile.BUILDING_MATERIALS.capabilities())
                .contains(ActivityCapability.MULTI_UNIT_SALES, ActivityCapability.PENDING_PICKUP, ActivityCapability.DELIVERIES);
    }

    @Test
    void todaACapacidadeEUsadaPorAlgumPerfil() {
        Set<ActivityCapability> used = CompanyActivityService.capabilitiesOf(Arrays.asList(ActivityProfile.values()));
        assertThat(used).containsExactlyInAnyOrderElementsOf(EnumSet.allOf(ActivityCapability.class));
    }

    @Test
    void textosEstaoPreenchidos() {
        for (ActivityProfile p : ActivityProfile.values()) {
            assertThat(p.label()).isNotBlank();
            assertThat(p.description()).isNotBlank();
            assertThat(p.capabilities()).isNotEmpty();
        }
        for (ActivityCapability c : ActivityCapability.values()) {
            assertThat(c.label()).isNotBlank();
            assertThat(c.description()).isNotBlank();
        }
    }

    @Test
    void parseAceitaMinusculasEIgnoraRepetidos() {
        assertThat(CompanyActivityService.parseProfiles(List.of("bakery", "BAKERY", " butcher ")))
                .containsExactlyInAnyOrder(ActivityProfile.BAKERY, ActivityProfile.BUTCHER);
    }

    @Test
    void parseRecusaVazioEDesconhecido() {
        assertThatThrownBy(() -> CompanyActivityService.parseProfiles(List.of()))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("pelo menos um");
        assertThatThrownBy(() -> CompanyActivityService.parseProfiles(List.of("  ")))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> CompanyActivityService.parseProfiles(List.of("CASINO")))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("CASINO");
    }
}
