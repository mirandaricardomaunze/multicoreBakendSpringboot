package mz.multicore.erp.modules.comercial;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:comercial-contract;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComercialControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void authenticatedDesktopContractCanListAndCreateClients() throws Exception {
        JsonNode login = login();
        String token = login.get("token").asText();
        String companyId = login.get("companies").get(0).get("id").asText();

        mockMvc.perform(get("/api/comercial/clients")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(post("/api/comercial/clients")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Cliente API Desktop",
                                  "taxId":"400123457",
                                  "email":"desktop@example.co.mz",
                                  "address":"Maputo"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cliente API Desktop"));
    }

    @Test
    void authenticatedDesktopCanLoadPaginatedPOSCatalog() throws Exception {
        JsonNode login = login();
        String token = login.get("token").asText();
        String companyId = login.get("companies").get(0).get("id").asText();

        mockMvc.perform(get("/api/comercial/products/pos-catalog/page")
                        .param("page", "0").param("size", "36")
                        .param("query", "").param("availableOnly", "false")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(36))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    @Test
    void authenticatedDesktopCanFilterPaginatedPOSSalesByDate() throws Exception {
        JsonNode login = login();
        String token = login.get("token").asText();
        String companyId = login.get("companies").get(0).get("id").asText();

        mockMvc.perform(get("/api/comercial/pos-sales/page")
                        .param("companyId", companyId)
                        .param("page", "0")
                        .param("size", "20")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void authenticatedDesktopCanLoadPOSSalesSummaryWithVariation() throws Exception {
        JsonNode login = login();
        String token = login.get("token").asText();
        String companyId = login.get("companies").get(0).get("id").asText();

        mockMvc.perform(get("/api/comercial/pos-sales/summary")
                        .param("companyId", companyId)
                        .param("from", "2026-01-01")
                        .param("to", "2026-01-31")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber())
                .andExpect(jsonPath("$.totalAmount").isNumber())
                .andExpect(jsonPath("$.previousCount").isNumber())
                .andExpect(jsonPath("$.previousTotalAmount").isNumber());
    }

    @Test
    void authenticatedDesktopCanUpdateProduct() throws Exception {
        JsonNode login = login();
        String token = login.get("token").asText();
        String companyId = login.get("companies").get(0).get("id").asText();

        String productsJson = mockMvc.perform(get("/api/comercial/products")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", companyId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode products = objectMapper.readTree(productsJson);
        org.junit.jupiter.api.Assertions.assertFalse(products.isEmpty());
        JsonNode first = products.get(0);
        long id = first.get("id").asLong();
        String name = first.get("name").asText() + " Alterado";

        String updatePayload = """
                {
                    "sku": "%s",
                    "reference": %s,
                    "barcode": %s,
                    "name": "%s",
                    "unitPrice": 120.50,
                    "purchasePrice": 80.00,
                    "minStock": 5,
                    "unitsPerBox": 1,
                    "categoryId": %s,
                    "saleType": "UNIT",
                    "stockTracked": true,
                    "taxRateId": %s,
                    "description": "Atualizacao de teste",
                    "wholesalePrice": null,
                    "wholesaleMinQty": null,
                    "netUnitWeightKg": null,
                    "grossUnitWeightKg": null
                }
                """.formatted(
                        first.hasNonNull("sku") ? first.get("sku").asText() : "SKU-1",
                        first.hasNonNull("reference") ? "\"" + first.get("reference").asText() + "\"" : "null",
                        first.hasNonNull("barcode") ? "\"" + first.get("barcode").asText() + "\"" : "null",
                        name,
                        first.hasNonNull("categoryId") ? first.get("categoryId").asText() : "null",
                        first.hasNonNull("taxRateId") ? first.get("taxRateId").asText() : "null"
                );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/comercial/products/" + id)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Company-Id", companyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));
    }

    private JsonNode login() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ana","password":"password"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }
}
