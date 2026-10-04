package mz.multicore.erp.modules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Homologação repetível dos editores pré-emissão pela mesma fronteira HTTP usada pelo desktop.
 * A base H2 é exclusiva desta classe, portanto não cria documentos nos dados operacionais.
 */
@SpringBootTest(properties =
        "spring.datasource.url=jdbc:h2:mem:editable-document-editors;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EditableDocumentEditorsHttpIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private record Context(String token, String companyId, long warehouseId, long productId) {}

    @Test
    void quotationCanBeUpdatedByVersionAndLocksAfterSend() throws Exception {
        Context context = context();
        JsonNode created = json(postJson("/api/comercial/quotations", context, """
                {"walkInName":"Homologação Cotação","companyId":%s,"warehouseId":%d,
                 "validityDays":30,"paymentTerms":"Pronto pagamento","deliveryDays":2,
                 "notes":"Criação E2E",
                 "lines":[{"productId":%d,"quantity":2,"discountPercentage":0}]}
                """.formatted(context.companyId(), context.warehouseId(), context.productId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT")));

        long id = created.get("id").asLong();
        long initialVersion = created.get("version").asLong();
        String number = created.get("quotationNumber").asText();
        String update = quotationUpdate(context, initialVersion, 3, "Actualização E2E");

        JsonNode updated = json(putJson("/api/comercial/quotations/" + id, context, update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quotationNumber").value(number))
                .andExpect(jsonPath("$.lines[0].quantity").value(3)));
        assertThat(updated.get("version").asLong()).isGreaterThan(initialVersion);
        assertThat(getJson("/api/comercial/quotations/" + id, context)
                .withArray("lines").get(0).get("quantity").decimalValue())
                .isEqualByComparingTo("3");
        assertThat(updated.get("totalAmount").decimalValue()).isPositive();

        putJson("/api/comercial/quotations/" + id, context, update)
                .andExpect(status().isBadRequest());

        JsonNode sent = json(postJson("/api/comercial/quotations/" + id + "/send", context, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT")));
        putJson("/api/comercial/quotations/" + id, context,
                quotationUpdate(context, sent.get("version").asLong(), 4, "Tentativa bloqueada"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void purchaseOrderCanBeUpdatedBeforeReceiptAndLocksAfterPartialReceipt() throws Exception {
        Context context = context();
        long supplierId = supplierId(context);
        JsonNode created = json(postJson("/api/purchases/orders", context, """
                {"supplierId":%d,"warehouseId":%d,"companyId":%s,"notes":"Criação E2E",
                 "lines":[{"productId":%d,"quantity":4,"unitPrice":10.00}]}
                """.formatted(supplierId, context.warehouseId(), context.companyId(), context.productId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED")));

        long id = created.get("id").asLong();
        long initialVersion = created.get("version").asLong();
        String number = created.get("orderNumber").asText();
        String update = purchaseOrderUpdate(context, supplierId, initialVersion, 5, "Actualização E2E");

        JsonNode updated = json(putJson("/api/purchases/orders/" + id, context, update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(number))
                .andExpect(jsonPath("$.lines[0].quantity").value(5)));
        assertThat(updated.get("version").asLong()).isGreaterThan(initialVersion);
        JsonNode reopened = findById(getJson(
                "/api/purchases/orders?companyId=" + context.companyId(), context), id);
        assertThat(reopened.withArray("lines").get(0).get("quantity").decimalValue())
                .isEqualByComparingTo("5");

        putJson("/api/purchases/orders/" + id, context, update)
                .andExpect(status().isBadRequest());

        long lineId = updated.get("lines").get(0).get("id").asLong();
        JsonNode received = json(postJson("/api/purchases/orders/" + id + "/receive-partial", context,
                "{\"lines\":[{\"lineId\":" + lineId + ",\"quantity\":1}]}" )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PARTIALLY_RECEIVED")));

        putJson("/api/purchases/orders/" + id, context,
                purchaseOrderUpdate(context, supplierId, received.get("version").asLong(), 6,
                        "Tentativa bloqueada"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stockTransferCanBeUpdatedAsDraftAndLocksAfterSubmission() throws Exception {
        Context context = context();
        long destinationId = destinationWarehouseId(context);
        postJson("/api/inventory/adjustments", context, """
                {"companyId":%s,"productId":%d,"warehouseId":%d,
                 "countedQuantity":20,"reason":"Preparação do harness de transferência"}
                """.formatted(context.companyId(), context.productId(), context.warehouseId()))
                .andExpect(status().isOk());

        JsonNode created = json(postJson("/api/inventory/transfers", context,
                transferCreate(context, destinationId, 2, "Criação E2E"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT")));
        long id = created.get("id").asLong();
        long version = created.get("version").asLong();
        String number = created.get("transferNumber").asText();

        String update = transferUpdate(context, destinationId, version, 3, "Actualização E2E");
        JsonNode updated = json(putJson("/api/inventory/transfers/" + id, context, update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferNumber").value(number))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.lines[0].quantity").value(3)));
        assertThat(updated.get("version").asLong()).isGreaterThan(version);
        assertThat(getJson("/api/inventory/transfers/" + id, context)
                .withArray("lines").get(0).get("quantity").decimalValue())
                .isEqualByComparingTo("3");

        putJson("/api/inventory/transfers/" + id, context, update)
                .andExpect(status().isBadRequest());

        JsonNode submitted = json(postJson("/api/inventory/transfers/" + id + "/submit", context, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL")));
        putJson("/api/inventory/transfers/" + id, context,
                transferUpdate(context, destinationId, submitted.get("version").asLong(), 4,
                        "Tentativa bloqueada"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerOrderCanBeUpdatedReopenedAndLocksAfterCancellation() throws Exception {
        Context context = context();
        postJson("/api/inventory/adjustments", context, """
                {"companyId":%s,"productId":%d,"warehouseId":%d,
                 "countedQuantity":30,"reason":"Preparação do harness da encomenda"}
                """.formatted(context.companyId(), context.productId(), context.warehouseId()))
                .andExpect(status().isOk());

        JsonNode created = json(postJson("/api/comercial/orders", context,
                orderCreate(context, 2, "Criação E2E"))
                .andExpect(status().isOk()));
        long id = created.get("id").asLong();
        long version = created.get("version").asLong();
        String number = created.get("orderNumber").asText();

        JsonNode updated = json(putJson("/api/comercial/orders/" + id, context,
                orderUpdate(context, version, 6, "Actualização E2E"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(number))
                .andExpect(jsonPath("$.lines[0].quantity").value(6)));
        assertThat(updated.get("version").asLong()).isGreaterThan(version);
        assertThat(getJson("/api/comercial/orders/" + id, context)
                .withArray("lines").get(0).get("quantity").decimalValue())
                .isEqualByComparingTo("6");

        putJson("/api/comercial/orders/" + id, context,
                orderUpdate(context, version, 7, "Versão antiga"))
                .andExpect(status().isBadRequest());

        postJson("/api/comercial/orders/" + id + "/cancel", context,
                "{\"reason\":\"Fim da homologação automatizada\"}")
                .andExpect(status().isNoContent());
        putJson("/api/comercial/orders/" + id, context,
                orderUpdate(context, updated.get("version").asLong(), 8, "Tentativa bloqueada"))
                .andExpect(status().isBadRequest());
    }

    private Context context() throws Exception {
        JsonNode login = json(mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ana\",\"password\":\"password\"}"))
                .andExpect(status().isOk()));
        String token = login.get("token").asText();
        for (JsonNode company : login.get("companies")) {
            String companyId = company.get("id").asText();
            Context partial = new Context(token, companyId, 0, 0);
            JsonNode warehouses = getJson("/api/inventory/warehouses/all?companyId=" + companyId, partial);
            JsonNode products = getJson("/api/comercial/products", partial);
            if (!warehouses.isEmpty() && !products.isEmpty()) {
                return new Context(token, companyId,
                        warehouses.get(0).get("id").asLong(), products.get(0).get("id").asLong());
            }
        }
        throw new IllegalStateException("A seed não tem empresa com armazém e produto para homologação.");
    }

    private long supplierId(Context context) throws Exception {
        JsonNode suppliers = getJson("/api/purchases/suppliers?companyId=" + context.companyId(), context);
        if (!suppliers.isEmpty()) return suppliers.get(0).get("id").asLong();
        JsonNode created = json(postJson("/api/purchases/suppliers", context, """
                {"name":"Fornecedor Homologação","taxId":"400987654","email":"qa@example.co.mz",
                 "address":"Maputo","phone":"840000000","contactPerson":"Equipa QA","companyId":%s}
                """.formatted(context.companyId())).andExpect(status().isOk()));
        return created.get("id").asLong();
    }

    private long destinationWarehouseId(Context context) throws Exception {
        JsonNode warehouses = getJson("/api/inventory/warehouses/all?companyId=" + context.companyId(), context);
        for (JsonNode warehouse : warehouses) {
            long id = warehouse.get("id").asLong();
            if (id != context.warehouseId()) return id;
        }
        JsonNode created = json(postJson("/api/inventory/warehouses", context, """
                {"name":"Destino Harness Transferência","warehouseNumber":"E2E-TRF-01",
                 "capacity":1000,"location":"Maputo","companyId":%s,"type":"STORE",
                 "allowsSales":false,"manager":"Equipa QA","phone":"840000000"}
                """.formatted(context.companyId())).andExpect(status().isOk()));
        return created.get("id").asLong();
    }

    private String quotationUpdate(Context context, long version, int quantity, String notes) {
        return """
                {"version":%d,"walkInName":"Homologação Cotação","warehouseId":%d,
                 "validityDays":30,"paymentTerms":"Pronto pagamento","deliveryDays":2,
                 "notes":"%s","lines":[{"productId":%d,"quantity":%d,"discountPercentage":5}]}
                """.formatted(version, context.warehouseId(), notes, context.productId(), quantity);
    }

    private String purchaseOrderUpdate(Context context, long supplierId, long version,
                                       int quantity, String notes) {
        return """
                {"version":%d,"supplierId":%d,"warehouseId":%d,"notes":"%s",
                 "lines":[{"productId":%d,"quantity":%d,"unitPrice":12.00}]}
                """.formatted(version, supplierId, context.warehouseId(), notes, context.productId(), quantity);
    }

    private String transferCreate(Context context, long destinationId, int quantity, String notes) {
        return """
                {"companyId":%s,"originWarehouseId":%d,"destinationWarehouseId":%d,
                 "responsible":"Equipa QA","vehicle":"Camioneta QA","notes":"%s",
                 "driverName":"Motorista QA","vehiclePlate":"E2E-01-MC",
                 "lines":[{"productId":%d,"quantity":%d}]}
                """.formatted(context.companyId(), context.warehouseId(), destinationId, notes,
                context.productId(), quantity);
    }

    private String orderCreate(Context context, int quantity, String walkInName) {
        return """
                {"clientId":null,"walkInName":"%s","companyId":%s,"warehouseId":%d,
                 "kind":"PICKING_REQUEST",
                 "lines":[{"productId":%d,"quantity":%d,"taxRate":0,"discountPercentage":0}]}
                """.formatted(walkInName, context.companyId(), context.warehouseId(),
                context.productId(), quantity);
    }

    private String orderUpdate(Context context, long version, int quantity, String walkInName) {
        return """
                {"version":%d,"clientId":null,"walkInName":"%s","warehouseId":%d,
                 "destinationWarehouseId":null,
                 "lines":[{"productId":%d,"quantity":%d,"taxRate":0,"discountPercentage":5}]}
                """.formatted(version, walkInName, context.warehouseId(), context.productId(), quantity);
    }

    private String transferUpdate(Context context, long destinationId, long version,
                                  int quantity, String notes) {
        return """
                {"version":%d,"originWarehouseId":%d,"destinationWarehouseId":%d,
                 "responsible":"Equipa QA","vehicle":"Camioneta QA","notes":"%s",
                 "driverName":"Motorista QA","vehiclePlate":"E2E-01-MC",
                 "lines":[{"productId":%d,"quantity":%d}]}
                """.formatted(version, context.warehouseId(), destinationId, notes,
                context.productId(), quantity);
    }

    private JsonNode getJson(String path, Context context) throws Exception {
        return json(mockMvc.perform(get(path)
                .header("Authorization", "Bearer " + context.token())
                .header("X-Company-Id", context.companyId()))
                .andExpect(status().isOk()));
    }

    private JsonNode findById(JsonNode values, long id) {
        for (JsonNode value : values) {
            if (value.path("id").asLong() == id) return value;
        }
        throw new AssertionError("Documento " + id + " não foi encontrado depois da gravação.");
    }

    private ResultActions postJson(String path, Context context, String body) throws Exception {
        var request = post(path)
                .header("Authorization", "Bearer " + context.token())
                .header("X-Company-Id", context.companyId());
        if (!body.isBlank()) request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mockMvc.perform(request);
    }

    private ResultActions putJson(String path, Context context, String body) throws Exception {
        return mockMvc.perform(put(path)
                .header("Authorization", "Bearer " + context.token())
                .header("X-Company-Id", context.companyId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}
