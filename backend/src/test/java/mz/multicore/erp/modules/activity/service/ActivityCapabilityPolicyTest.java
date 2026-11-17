package mz.multicore.erp.modules.activity.service;

import mz.multicore.erp.modules.activity.model.ActivityCapability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SA-C01: que pedidos iniciam trabalho de uma capacidade")
class ActivityCapabilityPolicyTest {

    @Test
    @DisplayName("Iniciar trabalho sectorial exige a capacidade")
    void startingWorkIsGated() {
        assertThat(ActivityCapabilityPolicy.required("POST", "/api/advances")).contains(ActivityCapability.ADVANCE_PAYMENTS);
        assertThat(ActivityCapabilityPolicy.required("post", "/api/weighbridge/tickets/")).contains(ActivityCapability.TRUCK_SCALE);
        assertThat(ActivityCapabilityPolicy.required("PUT", "/api/custom-orders/42")).contains(ActivityCapability.CUSTOM_ORDERS);
        assertThat(ActivityCapabilityPolicy.required("PUT", "/api/projects/7/orders/9")).contains(ActivityCapability.CUSTOMER_PROJECTS);
        assertThat(ActivityCapabilityPolicy.required("POST", "/api/variants/families/3/values")).contains(ActivityCapability.VARIANTS);
        assertThat(ActivityCapabilityPolicy.required("PUT", "/api/units/products")).contains(ActivityCapability.MULTI_UNIT_SALES);
    }

    @Test
    @DisplayName("Concluir, liquidar ou desfazer o que já existe nunca é bloqueado")
    void windingDownIsNeverGated() {
        assertThat(ActivityCapabilityPolicy.required("POST", "/api/advances/5/refund")).isEmpty();
        assertThat(ActivityCapabilityPolicy.required("POST", "/api/advances/5/apply")).isEmpty();
        assertThat(ActivityCapabilityPolicy.required("POST", "/api/pickups/slips")).isEmpty();
        assertThat(ActivityCapabilityPolicy.required("POST", "/api/consignments/2/settle")).isEmpty();
        assertThat(ActivityCapabilityPolicy.required("POST", "/api/weighbridge/tickets/8/second")).isEmpty();
        assertThat(ActivityCapabilityPolicy.required("POST", "/api/butchery/breakdowns/4/cancel")).isEmpty();
        assertThat(ActivityCapabilityPolicy.required("POST", "/api/production-plans/1/close")).isEmpty();
        assertThat(ActivityCapabilityPolicy.required("POST", "/api/bulk-stock/measurements/6/approve")).isEmpty();
        assertThat(ActivityCapabilityPolicy.required("POST", "/api/expiry/write-offs")).isEmpty();
        assertThat(ActivityCapabilityPolicy.required("DELETE", "/api/pricing/lists/3")).isEmpty();
        // The base unit is general product data, not a sector feature.
        assertThat(ActivityCapabilityPolicy.required("PUT", "/api/units/products/base")).isEmpty();
    }

    @Test
    @DisplayName("Leituras nunca são bloqueadas")
    void readsAreNeverGated() {
        assertThat(ActivityCapabilityPolicy.required("GET", "/api/advances")).isEmpty();
        assertThat(ActivityCapabilityPolicy.required("GET", "/api/weighbridge/tickets")).isEmpty();
        assertThat(ActivityCapabilityPolicy.required(null, "/api/advances")).isEmpty();
    }

    @Test
    @DisplayName("A recusa diz o que falta e onde se activa")
    void deniedMessageNamesCapabilityAndPlace() {
        assertThat(ActivityCapabilityPolicy.deniedMessage(ActivityCapability.TRUCK_SCALE))
                .contains("Báscula de camião", "Configurações › Perfil de Actividade");
    }
}
