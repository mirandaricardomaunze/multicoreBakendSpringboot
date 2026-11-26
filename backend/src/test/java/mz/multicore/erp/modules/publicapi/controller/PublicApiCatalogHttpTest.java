package mz.multicore.erp.modules.publicapi.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.publicapi.dto.CreateApiKeyRequest;
import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;
import mz.multicore.erp.modules.publicapi.service.ApiKeyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Harness API-07 over HTTP (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.3.1): the catalogue routes answer JSON pages for the
 * key's company, unknown SKUs and warehouses → 404 with a stable {@code code}, and no cost field leaves the API.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:public-api-catalog;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicApiCatalogHttpTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ApiKeyService apiKeyService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    private String key;

    @BeforeEach
    void createKey() {
        long companyId = jdbc.queryForObject("select min(id) from companies", Long.class);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(companyId);
        try {
            key = apiKeyService.create(new CreateApiKeyRequest("API-07 Catalogo",
                    EnumSet.of(ApiKeyPermission.CATALOG_READ))).secret();
        } finally {
            CurrentUserContext.clear();
        }
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from api_keys where name like 'API-07%'");
    }

    @Test
    void api07ProductsAndStockAnswerJsonPagesWithoutCosts() throws Exception {
        String products = mockMvc.perform(get("/api/public/v1/products").param("size", "500")
                        .header("X-Api-Key", key))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode page = objectMapper.readTree(products);
        assertThat(page.get("items").isArray()).isTrue();
        assertThat(page.get("size").asInt()).isEqualTo(100);
        assertThat(products).doesNotContainIgnoringCase("purchasePrice").doesNotContainIgnoringCase("wholesale")
                .doesNotContainIgnoringCase("supplier");

        JsonNode stock = objectMapper.readTree(mockMvc.perform(get("/api/public/v1/stock").header("X-Api-Key", key))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(stock.get("items").isArray()).isTrue();
        assertThat(stock.get("page").asInt()).isZero();
    }

    @Test
    void api07UnknownSkuAndWarehouseAreNotFoundWithCode() throws Exception {
        JsonNode product = objectMapper.readTree(mockMvc.perform(get("/api/public/v1/products/NAO-EXISTE-API07")
                        .header("X-Api-Key", key))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString());
        assertThat(product.get("code").asText()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(product.get("message").asText()).isNotBlank();

        var warehouse = mockMvc.perform(get("/api/public/v1/stock").param("warehouse", "Nenhum-API07")
                        .header("X-Api-Key", key))
                .andExpect(status().isNotFound()).andReturn().getResponse();
        assertThat(warehouse.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(objectMapper.readTree(warehouse.getContentAsString()).get("code").asText())
                .isEqualTo("WAREHOUSE_NOT_FOUND");
    }
}
