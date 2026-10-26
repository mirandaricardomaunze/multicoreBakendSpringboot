package mz.multicore.erp.architecture.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:role-policy-coverage;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
@DisplayName("Harness de cobertura integral da política HTTP (RPC-01)")
class ApiRolePolicyEndpointCoverageHarnessTest {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    @Test
    @DisplayName("RPC-01: todo endpoint /api de Controller possui classificação explícita")
    void everyApiEndpointIsExplicitlyClassified() {
        List<String> missing = new ArrayList<>();

        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            if (!handler.getBeanType().isAnnotationPresent(RestController.class)) return;
            Set<String> paths = mapping.getPatternValues();
            Set<org.springframework.web.bind.annotation.RequestMethod> methods = mapping.getMethodsCondition().getMethods();
            for (String path : paths) {
                if (!path.startsWith("/api/")) continue;
                if (methods.isEmpty()) {
                    for (HttpMethod method : List.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
                            HttpMethod.PATCH, HttpMethod.DELETE)) {
                        collectIfMissing(missing, method.name(), path, mapping);
                    }
                } else {
                    methods.forEach(method -> collectIfMissing(missing, method.name(), path, mapping));
                }
            }
        });

        missing.sort(Comparator.naturalOrder());
        assertTrue(missing.isEmpty(), () -> "Endpoints sem classificação em ApiRolePolicy:\n"
                + String.join("\n", missing));
    }

    private void collectIfMissing(List<String> missing, String method, String path, RequestMappingInfo mapping) {
        if (!ApiRolePolicy.isExplicitlyClassified(method, path)) {
            missing.add(method + " " + path + " (" + mapping + ")");
        }
    }
}
