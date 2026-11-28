package mz.multicore.erp.modules.publicapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import mz.multicore.erp.architecture.exception.PublicApiError;
import mz.multicore.erp.architecture.security.PublicApiRoutes;
import mz.multicore.erp.modules.publicapi.dto.CreatePublicOrderRequest;
import mz.multicore.erp.modules.publicapi.dto.PublicOrderDTO;
import mz.multicore.erp.modules.publicapi.dto.PublicProductDTO;
import mz.multicore.erp.modules.publicapi.dto.PublicStockDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI 3 document of the public API group (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.2.2, API-11).
 *
 * <p>Built here instead of by springdoc so it exists in prod, where {@code springdoc.api-docs.enabled=false} keeps the
 * internal documentation off. It lists only {@code /api/public/v1/**} (never {@code /docs} itself); the body schemas
 * come from the public DTO records, so a field change in a DTO reaches the document. {@code PublicApiHttpIntegrationTest}
 * checks that the documented routes are exactly the ones the server maps.
 */
@Service
public class PublicApiDocumentationService {

    private static final String SECURITY_SCHEME = "ApiKeyAuth";
    private static final String V1 = PublicApiRoutes.PREFIX + "/v1";
    private static final String JSON = "application/json";
    private static final String ERROR = PublicApiError.class.getSimpleName();

    private final String document;

    public PublicApiDocumentationService() {
        try {
            this.document = Json.mapper().writeValueAsString(build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível gerar a documentação da API pública.", e);
        }
    }

    /** The OpenAPI 3 document as JSON (built once: it only changes with the code). */
    public String openApiJson() {
        return document;
    }

    private static OpenAPI build() {
        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER).name(PublicApiRoutes.API_KEY_HEADER)
                        .description("Chave de API criada pelo administrador (mk_live_...)."));
        for (Class<?> type : List.of(PublicProductDTO.class, PublicStockDTO.class, PublicOrderDTO.class,
                CreatePublicOrderRequest.class, PublicApiError.class)) {
            ModelConverters.getInstance().readAll(type).forEach(components::addSchemas);
        }
        components.addSchemas("PublicProductPage", page(PublicProductDTO.class));
        components.addSchemas("PublicStockPage", page(PublicStockDTO.class));

        Paths paths = new Paths()
                .addPathItem(V1 + "/products", new PathItem().get(operation("listProducts",
                        "Artigos activos da empresa com preço de venda com IVA (permissão CATALOG_READ).",
                        List.of(query("updatedSince", "Só artigos criados ou alterados desde este instante (ISO 8601)."),
                                pageParam(), sizeParam()),
                        ok("PublicProductPage"))))
                .addPathItem(V1 + "/products/{sku}", new PathItem().get(operation("getProduct",
                        "Um artigo pelo SKU (permissão CATALOG_READ). SKU inexistente: 404 PRODUCT_NOT_FOUND.",
                        List.of(pathParam("sku", "SKU do artigo.")),
                        ok(PublicProductDTO.class.getSimpleName()))))
                .addPathItem(V1 + "/stock", new PathItem().get(operation("listStock",
                        "Quantidade disponível (stock menos reservas) por artigo e armazém de venda (permissão CATALOG_READ).",
                        List.of(query("warehouse", "Código do armazém de venda (opcional)."), pageParam(), sizeParam()),
                        ok("PublicStockPage"))))
                .addPathItem(V1 + "/orders", new PathItem().post(operation("createOrder",
                        "Cria uma encomenda de cliente (permissão ORDERS_WRITE). 201 criada; 200 quando a mesma "
                                + "externalReference é repetida com o mesmo conteúdo; 409 com conteúdo diferente.",
                        List.of(),
                        orderCreation(ok(PublicOrderDTO.class.getSimpleName())))
                        .requestBody(new RequestBody().required(true).content(json(
                                CreatePublicOrderRequest.class.getSimpleName())))))
                .addPathItem(V1 + "/orders/{number}", new PathItem().get(operation("getOrder",
                        "Estado actual de uma encomenda pelo número, ex.: ENC-2026/00001 (permissão ORDERS_READ).",
                        List.of(pathParam("number", "Número da encomenda (a barra vai tal como está).")),
                        ok(PublicOrderDTO.class.getSimpleName()))));

        return new OpenAPI()
                .info(new Info().title("Multicore ERP — API pública").version("v1")
                        .description("Catálogo, stock disponível e encomendas da empresa da chave. Limite de 60 "
                                + "pedidos por minuto por chave (429 RATE_LIMIT_EXCEEDED com Retry-After)."))
                .components(components)
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .paths(paths);
    }

    private static Operation operation(String id, String summary, List<Parameter> parameters, ApiResponses responses) {
        return new Operation().operationId(id).summary(summary).parameters(parameters.isEmpty() ? null : parameters)
                .responses(responses);
    }

    private static ApiResponses ok(String schema) {
        ApiResponses responses = new ApiResponses().addApiResponse("200", response("Pedido aceite.", schema));
        Map.of("400", "Pedido inválido (VALIDATION_ERROR ou regra de negócio).",
                        "401", "Chave em falta, inválida ou revogada.",
                        "403", "A chave não tem a permissão desta rota (API_PERMISSION_DENIED).",
                        "404", "Recurso inexistente na empresa da chave.",
                        "429", "Limite de pedidos por minuto atingido (RATE_LIMIT_EXCEEDED).",
                        "500", "Erro interno (INTERNAL_ERROR).")
                .forEach((status, description) -> responses.addApiResponse(status, response(description, ERROR)));
        return responses;
    }

    private static ApiResponses orderCreation(ApiResponses responses) {
        return responses.addApiResponse("201", response("Encomenda criada.", PublicOrderDTO.class.getSimpleName()))
                .addApiResponse("409", response("Referência externa já usada com outro conteúdo.", ERROR))
                .addApiResponse("422", response("Stock disponível insuficiente (INSUFFICIENT_STOCK).", ERROR));
    }

    private static ApiResponse response(String description, String schema) {
        return new ApiResponse().description(description).content(json(schema));
    }

    private static Content json(String schema) {
        return new Content().addMediaType(JSON, new MediaType().schema(ref(schema)));
    }

    private static Schema<?> ref(String schema) {
        return new Schema<>().$ref("#/components/schemas/" + schema);
    }

    private static Schema<?> page(Class<?> item) {
        return new ObjectSchema()
                .addProperty("items", new ArraySchema().items(ref(item.getSimpleName())))
                .addProperty("page", new IntegerSchema().description("Página, a partir de 0."))
                .addProperty("size", new IntegerSchema().description("Tamanho da página (máx. 100)."))
                .addProperty("totalItems", new IntegerSchema().format("int64"))
                .addProperty("totalPages", new IntegerSchema());
    }

    private static Parameter query(String name, String description) {
        return new Parameter().in("query").name(name).required(false).description(description)
                .schema(new StringSchema());
    }

    private static Parameter pageParam() {
        return new Parameter().in("query").name("page").required(false).description("Página, a partir de 0.")
                .schema(new IntegerSchema()._default(0));
    }

    private static Parameter sizeParam() {
        return new Parameter().in("query").name("size").required(false)
                .description("Itens por página: 50 por omissão, máximo 100.").schema(new IntegerSchema()._default(50));
    }

    private static Parameter pathParam(String name, String description) {
        return new Parameter().in("path").name(name).required(true).description(description)
                .schema(new StringSchema());
    }
}
