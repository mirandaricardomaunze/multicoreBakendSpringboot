package mz.multicore.erp.modules.onlineshop.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PortalSecurityHeaders;
import mz.multicore.erp.modules.inventory.service.WarehouseService;
import mz.multicore.erp.modules.onlineshop.dto.OnlineShopDeliveryZoneDTO;
import mz.multicore.erp.modules.onlineshop.dto.SaveOnlineShopSettingsRequest;
import mz.multicore.erp.modules.onlineshop.service.OnlineShopSettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Harness LO-11 (docs/LOJA_ONLINE_SPEC.md §3.2): the public page of the online shop at {@code /loja/{code}} follows the
 * technical rules of the manager portal — own files in {@code static/loja/}, no CDN, no inline script/style/handlers,
 * CSP with {@code 'self'} only, API text only through {@code textContent}, layout from 360 px — and keeps the cart in
 * {@code localStorage} with SKUs and quantities only (never prices, names or customer data). The checkout reads the
 * shop's pickup stores and delivery zones from {@code GET /api/shop/{code}}, which never shows internal data.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:online-shop-static;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnlineShopStaticAssetsHarnessTest {

    private static final Path SHOP = Path.of("src/main/resources/static/loja");
    private static final String[] TEXT_FILES = {"index.html", "loja.css", "loja.js", "icon.svg"};
    private static final String CODE = "lo11-montra";

    @Autowired private MockMvc mockMvc;
    @Autowired private OnlineShopSettingsService settingsService;
    @Autowired private WarehouseService warehouseService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("LO-11: index.html, CSS, JS e ícone próprios existem")
    void ownFilesExist() {
        for (String file : TEXT_FILES) {
            assertThat(SHOP.resolve(file)).as(file).isRegularFile();
        }
    }

    @Test
    @DisplayName("LO-11: nenhum ficheiro da loja aponta para CDN ou origem externa")
    void noExternalOrigins() throws IOException {
        Pattern external = Pattern.compile("(?i)(https?:)?//[a-z0-9.-]+\\.[a-z]{2,}");
        for (String file : TEXT_FILES) {
            String content = read(file);
            Matcher matcher = external.matcher(content);
            while (matcher.find()) {
                // O namespace do SVG é um identificador XML, não um pedido de rede.
                assertThat(matcher.group()).as(file).isEqualTo("http://www.w3.org");
            }
            assertThat(content).as(file).doesNotContainPattern("(?i)@import");
        }
    }

    @Test
    @DisplayName("LO-11: index.html sem script, estilo nem handlers inline")
    void noInlineScriptOrStyle() throws IOException {
        String html = read("index.html");

        Matcher scripts = Pattern.compile("(?is)<script\\b([^>]*)>(.*?)</script>").matcher(html);
        int count = 0;
        while (scripts.find()) {
            count++;
            assertThat(scripts.group(1)).as("script com src próprio").contains("src=\"/loja/loja.js\"");
            assertThat(scripts.group(2)).as("script sem corpo").isBlank();
        }
        assertThat(count).as("um só script, o loja.js").isEqualTo(1);
        assertThat(html).doesNotContainPattern("(?i)<style\\b");
        assertThat(html).doesNotContainPattern("(?i)\\sstyle\\s*=");
        assertThat(html).doesNotContainPattern("(?i)\\son[a-z]+\\s*=");
        assertThat(html).doesNotContainPattern("(?i)javascript:");
        assertThat(html).contains("href=\"/loja/loja.css\"");
        assertThat(html).contains("lang=\"pt-MZ\"");
        assertThat(html).contains("rel=\"icon\" href=\"/loja/icon.svg\"");
    }

    @Test
    @DisplayName("LO-11: /loja/{código} abre a página com CSP só 'self', DENY e no-referrer")
    void pageIsServedWithCsp() throws Exception {
        for (String path : new String[]{"/loja/" + CODE, "/loja/" + CODE + "/"}) {
            MockHttpServletResponse response = mockMvc.perform(get(path)).andReturn().getResponse();
            assertThat(response.getStatus()).as(path).isEqualTo(200);
            assertThat(response.getForwardedUrl()).as(path).isEqualTo("/loja/index.html");
            assertSecurityHeaders(path, response);
        }
        MockHttpServletResponse page = mockMvc.perform(get("/loja/index.html")).andReturn().getResponse();
        assertThat(page.getContentAsString(StandardCharsets.UTF_8)).contains("/loja/loja.js");
        for (String asset : new String[]{"/loja/loja.js", "/loja/loja.css", "/loja/icon.svg"}) {
            MockHttpServletResponse response = mockMvc.perform(get(asset)).andReturn().getResponse();
            assertThat(response.getStatus()).as(asset).isEqualTo(200);
            assertSecurityHeaders(asset, response);
        }
        // A CSP do portal não tem 'unsafe-inline' nem origens externas.
        assertThat(PortalSecurityHeaders.CONTENT_SECURITY_POLICY).doesNotContain("unsafe", "http", "*");
        // Fora de /loja (ex.: /lojax) o cabeçalho não muda.
        assertThat(mockMvc.perform(get("/lojax")).andReturn().getResponse().getHeader("Content-Security-Policy"))
                .isNull();
    }

    @Test
    @DisplayName("LO-11: texto da API só por textContent, nunca como HTML")
    void onlyTextContent() throws IOException {
        String js = read("loja.js");

        assertThat(js).contains("textContent");
        assertThat(js).doesNotContain("innerHTML", "outerHTML", "insertAdjacentHTML", "document.write",
                "eval(", "new Function", "setTimeout(\"", "setInterval(\"", "DOMParser", "createContextualFragment");
    }

    @Test
    @DisplayName("LO-11: carrinho no localStorage só com SKUs e quantidades; nada de cookies nem sessão")
    void cartKeepsOnlySkusAndQuantities() throws IOException {
        String js = read("loja.js");

        assertThat(js).doesNotContain("sessionStorage", "document.cookie", "indexedDB", "Authorization");
        // Uma só escrita no browser, e é a do carrinho.
        assertThat(count(js, "localStorage.setItem(")).isEqualTo(1);
        assertThat(js).contains("localStorage.setItem(cartKey(), JSON.stringify(cartLines()))");
        // As linhas guardadas e enviadas são só {sku, quantity}.
        assertThat(js).containsPattern(
                "function cartLines\\(\\) \\{[^}]*return \\{ sku: sku, quantity: cart\\[sku\\] \\};");
        // Ao ler o carrinho, tudo o que não é SKU + quantidade positiva é descartado.
        assertThat(js).containsPattern("function loadCart\\(\\) \\{[\\s\\S]*?typeof line\\.sku === \"string\"");
        // Os pedidos à API não levam credenciais.
        assertThat(js).contains("credentials: \"omit\"");
        assertThat(js).contains("\"/api/shop/\" + encodeURIComponent(code)");
    }

    @Test
    @DisplayName("LO-11: viewport móvel sem bloquear o zoom; layout cabe em 360 px")
    void mobileLayoutFrom360px() throws IOException {
        String html = read("index.html");
        assertThat(html).contains("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        assertThat(html).doesNotContainPattern("(?i)user-scalable\\s*=\\s*no|maximum-scale");

        String css = read("loja.css");
        String rules = css.replaceAll("@media[^{]*\\{", "{");
        Matcher widths = Pattern.compile("(?i)(?<![a-z-])(min-width|width|flex-basis)\\s*:\\s*(\\d+)px").matcher(rules);
        while (widths.find()) {
            assertThat(Integer.parseInt(widths.group(2))).as(widths.group()).isLessThanOrEqualTo(360);
        }
        Matcher flexBasis = Pattern.compile("flex\\s*:\\s*\\d+\\s+\\d+\\s+(\\d+)px").matcher(css);
        while (flexBasis.find()) {
            assertThat(Integer.parseInt(flexBasis.group(1))).as(flexBasis.group()).isLessThanOrEqualTo(360);
        }
        assertThat(css).doesNotContainPattern("(?i)grid-template-columns\\s*:[^;]*\\d+px");
        assertThat(css).containsPattern("body\\s*\\{[^}]*overflow-x\\s*:\\s*hidden");
        assertThat(css).containsPattern("\\.products\\s*\\{[^}]*repeat\\(auto-fill, minmax\\(min\\(100%, \\d+rem\\), 1fr\\)\\)");
        assertThat(css).containsPattern("overflow-wrap\\s*:\\s*anywhere");
    }

    @Test
    @DisplayName("LO-11: catálogo por categoria, pesquisa, Disponível/Esgotado e esgotado não vai ao carrinho")
    void catalogueSearchAndAvailability() throws IOException {
        String html = read("index.html");
        String js = read("loja.js");

        assertThat(html).containsPattern("<input[^>]*id=\"search\"[^>]*type=\"search\"");
        assertThat(html).containsPattern("<select[^>]*id=\"category\"");
        assertThat(js).contains("product.available ? \"Disponível\" : \"Esgotado\"");
        assertThat(js).contains("add.disabled = !product.available");
        // Nada de quantidades de stock: o produto só traz available.
        assertThat(js).doesNotContainPattern("(?i)product\\.(stock|quantity|cost|supplier)");
    }

    @Test
    @DisplayName("LO-11: checkout com honeypot escondido, modo e consulta por número + telemóvel")
    void checkoutFormAndHoneypot() throws IOException {
        String html = read("index.html");
        String css = read("loja.css");
        String js = read("loja.js");

        // Campo-armadilha: fora do ecrã, fora do Tab e sem preenchimento automático.
        assertThat(html).containsPattern(
                "<div class=\"hp\" aria-hidden=\"true\">\\s*<label>Website\\s*<input[^>]*name=\"website\"[^>]*"
                        + "tabindex=\"-1\"[^>]*autocomplete=\"off\"");
        assertThat(css).containsPattern("\\.hp\\s*\\{[^}]*left\\s*:\\s*-9999px");
        assertThat(js).contains("website: el(\"website\").value");
        for (String id : new String[]{"customer-name", "phone", "nuit", "fulfilment", "pickup", "zone", "address",
                "notes", "lookup-number", "lookup-phone"}) {
            assertThat(html).as(id).contains("id=\"" + id + "\"");
        }
        assertThat(js).contains("\"/orders/lookup\"");
        assertThat(js).contains("lines: cartLines()");
    }

    @Test
    @DisplayName("LO-11: valores em 1.460,00 MT como o DisplayNumbers, sem formatos do browser")
    void systemFormats() throws IOException {
        String js = read("loja.js");

        assertThat(js).contains("digits.replace(/\\B(?=(\\d{3})+(?!\\d))/g, \".\")");
        assertThat(js).contains("\",\" + parts[1] + \" MT\"");
        assertThat(js).contains("toFixed(3).replace(/0+$/, \"\")");
        assertThat(js).doesNotContain("toLocaleString", "toLocaleDateString", "toLocaleTimeString", "Intl.");
        assertThat(count(js, "toFixed(")).isEqualTo(2);
        for (String money : new String[]{"product.priceWithVat", "order.totalAmount", "zone.fee"}) {
            assertThat(js).as(money).contains("formatMoney(" + money + ")");
        }
        assertThat(js).contains("formatQty(line.quantity)");
    }

    @Test
    @DisplayName("LO-11: GET /api/shop/{código} dá nome, contacto, lojas de levantamento e zonas, sem ids internos da empresa")
    void storefrontForTheCheckout() throws Exception {
        Long pickup = openShop(true);

        MockHttpServletResponse response = mockMvc.perform(get("/api/shop/" + CODE)).andReturn().getResponse();

        assertThat(response.getStatus()).as(response.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(200);
        JsonNode body = objectMapper.readTree(response.getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.path("name").asText()).isNotBlank();
        assertThat(body.path("contactPhone").asText()).isEqualTo("841112233");
        assertThat(body.path("pickupStores")).hasSize(1);
        assertThat(body.path("pickupStores").get(0).path("id").asLong()).isEqualTo(pickup);
        assertThat(body.path("pickupStores").get(0).path("name").asText()).isNotBlank();
        assertThat(body.path("deliveryZones")).hasSize(1);
        assertThat(body.path("deliveryZones").get(0).path("name").asText()).isEqualTo("Baixa");
        assertThat(new BigDecimal(body.path("deliveryZones").get(0).path("fee").asText()))
                .isEqualByComparingTo("150");
        List<String> fields = iterable(body.fieldNames());
        assertThat(fields).containsExactlyInAnyOrder("name", "contactPhone", "pickupStores", "deliveryZones");

        openShop(false);
        MockHttpServletResponse closed = mockMvc.perform(get("/api/shop/" + CODE)).andReturn().getResponse();
        assertThat(closed.getStatus()).isEqualTo(400);
        assertThat(objectMapper.readTree(closed.getContentAsString(StandardCharsets.UTF_8)).path("message").asText())
                .isEqualTo("Loja indisponível.");
    }

    private void assertSecurityHeaders(String path, MockHttpServletResponse response) {
        assertThat(response.getHeader("Content-Security-Policy")).as(path)
                .isEqualTo(PortalSecurityHeaders.CONTENT_SECURITY_POLICY);
        assertThat(response.getHeader("X-Frame-Options")).as(path).isEqualTo("DENY");
        assertThat(response.getHeader("Referrer-Policy")).as(path).isEqualTo("no-referrer");
    }

    private Long openShop(boolean enabled) {
        long companyId = jdbc.queryForObject("select min(id) from companies", Long.class);
        CurrentUserContext.setCurrentUser("admin", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(companyId);
        try {
            Long pickup = warehouseService.getSalesWarehousesByCompany(companyId).get(0).getId();
            settingsService.save(new SaveOnlineShopSettingsRequest(enabled, CODE, List.of(pickup),
                    List.of(new OnlineShopDeliveryZoneDTO("Baixa", new BigDecimal("150"))), "841112233", null));
            return pickup;
        } finally {
            CurrentUserContext.clear();
        }
    }

    private static <T> List<T> iterable(Iterator<T> iterator) {
        List<T> list = new ArrayList<>();
        iterator.forEachRemaining(list::add);
        return list;
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + 1)) {
            count++;
        }
        return count;
    }

    private static String read(String file) throws IOException {
        return Files.readString(SHOP.resolve(file), StandardCharsets.UTF_8);
    }
}
