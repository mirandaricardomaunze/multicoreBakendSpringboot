package mz.multicore.erp.architecture.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * PG-11 — a página do portal web do gestor (PORTAL_GESTOR_SPEC §3.5): ficheiros próprios em
 * {@code static/portal/}, sem CDN nem script/estilo inline (a CSP só permite {@code 'self'}), token em
 * {@code sessionStorage} (nunca {@code localStorage} nem cookie), texto da API só por {@code textContent},
 * e {@code manifest.webmanifest} com ícone para instalar no ecrã inicial.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:portal-static;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortalStaticAssetsHarnessTest {

    private static final Path PORTAL = Path.of("src/main/resources/static/portal");
    private static final String[] TEXT_FILES = {"index.html", "portal.css", "portal.js", "manifest.webmanifest", "icon.svg"};

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("PG-11: index.html, CSS, JS, manifest e ícone próprios existem")
    void pg11OwnFilesExist() {
        for (String file : TEXT_FILES) {
            assertThat(PORTAL.resolve(file)).as(file).isRegularFile();
        }
    }

    @Test
    @DisplayName("PG-11: nenhum ficheiro do portal aponta para CDN ou origem externa")
    void pg11NoExternalOrigins() throws IOException {
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
    @DisplayName("PG-11: index.html sem script, estilo nem handlers inline")
    void pg11NoInlineScriptOrStyle() throws IOException {
        String html = read("index.html");

        Matcher scripts = Pattern.compile("(?is)<script\\b([^>]*)>(.*?)</script>").matcher(html);
        int count = 0;
        while (scripts.find()) {
            count++;
            assertThat(scripts.group(1)).as("script com src próprio").contains("src=\"/portal/portal.js\"");
            assertThat(scripts.group(2)).as("script sem corpo").isBlank();
        }
        assertThat(count).as("um só script, o portal.js").isEqualTo(1);
        assertThat(html).doesNotContainPattern("(?i)<style\\b");
        assertThat(html).doesNotContainPattern("(?i)\\sstyle\\s*=");
        assertThat(html).doesNotContainPattern("(?i)\\son[a-z]+\\s*=");
        assertThat(html).doesNotContainPattern("(?i)javascript:");
        assertThat(html).contains("href=\"/portal/portal.css\"");
    }

    @Test
    @DisplayName("PG-11: token só em sessionStorage, nunca localStorage nem cookie")
    void pg11TokenOnlyInSessionStorage() throws IOException {
        String js = read("portal.js");

        assertThat(js).contains("sessionStorage");
        assertThat(js).doesNotContain("localStorage", "document.cookie", "indexedDB");
    }

    @Test
    @DisplayName("PG-11: texto da API só por textContent, nunca como HTML")
    void pg11OnlyTextContent() throws IOException {
        String js = read("portal.js");

        assertThat(js).contains("textContent");
        assertThat(js).doesNotContain("innerHTML", "outerHTML", "insertAdjacentHTML", "document.write",
                "eval(", "new Function", "setTimeout(\"", "setInterval(\"", "DOMParser", "createContextualFragment");
    }

    @Test
    @DisplayName("PG-11: manifest instalável com ícone existente, ligado no index.html")
    void pg11ManifestAndIcon() throws IOException {
        JsonNode manifest = new ObjectMapper().readTree(read("manifest.webmanifest"));

        assertThat(manifest.path("name").asText()).isNotBlank();
        assertThat(manifest.path("short_name").asText()).isNotBlank();
        assertThat(manifest.path("start_url").asText()).isEqualTo("/portal/");
        assertThat(manifest.path("scope").asText()).isEqualTo("/portal/");
        assertThat(manifest.path("display").asText()).isEqualTo("standalone");
        assertThat(manifest.path("lang").asText()).isEqualTo("pt-MZ");
        assertThat(manifest.path("icons").isArray() && manifest.path("icons").size() > 0).isTrue();
        for (JsonNode icon : manifest.path("icons")) {
            String src = icon.path("src").asText();
            assertThat(src).startsWith("/portal/");
            assertThat(PORTAL.resolve(src.substring("/portal/".length()))).as(src).isRegularFile();
        }

        String html = read("index.html");
        assertThat(html).contains("rel=\"manifest\" href=\"/portal/manifest.webmanifest\"");
        assertThat(html).contains("rel=\"icon\"");
        assertThat(html).contains("lang=\"pt-MZ\"");
    }

    @Test
    @DisplayName("PG-11: /portal e /portal/ abrem a página; ficheiros servidos pelo backend")
    void pg11PageIsServed() throws Exception {
        for (String path : new String[]{"/portal", "/portal/", "/portal/index.html"}) {
            MockHttpServletResponse response = mockMvc.perform(get(path)).andReturn().getResponse();
            String body = response.getForwardedUrl() != null
                    ? mockMvc.perform(get(response.getForwardedUrl())).andReturn().getResponse()
                            .getContentAsString(StandardCharsets.UTF_8)
                    : response.getContentAsString(StandardCharsets.UTF_8);

            assertThat(response.getStatus()).as(path).isEqualTo(200);
            assertThat(body).as(path).contains("/portal/portal.js");
        }
        for (String asset : new String[]{"/portal/portal.js", "/portal/portal.css", "/portal/manifest.webmanifest",
                "/portal/icon.svg"}) {
            assertThat(mockMvc.perform(get(asset)).andReturn().getResponse().getStatus()).as(asset).isEqualTo(200);
        }
    }

    private static String read(String file) throws IOException {
        return Files.readString(PORTAL.resolve(file), StandardCharsets.UTF_8);
    }
}
