package mz.multicore.erp.architecture.validation;

import jakarta.validation.Valid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness transversal de validação dos inputs HTTP")
class BackendInputValidationCoverageHarnessTest {

    @Test
    @DisplayName("BIV-01: todo RequestBody estruturado usa DTO tipado e @Valid")
    void everyStructuredRequestBodyIsTypedAndValidated() throws Exception {
        List<String> violations = new ArrayList<>();
        Path classes = Path.of("target", "classes", "mz", "multicore", "erp");

        try (var paths = Files.walk(classes)) {
            paths.filter(path -> path.toString().endsWith("Controller.class"))
                    .filter(path -> !path.getFileName().toString().contains("$"))
                    .forEach(path -> inspect(path, classes, violations));
        }

        assertTrue(violations.isEmpty(), () -> "Corpos HTTP sem validação canónica:\n"
                + String.join("\n", violations));
    }

    private void inspect(Path classFile, Path classesRoot, List<String> violations) {
        String relative = classesRoot.relativize(classFile).toString();
        String className = "mz.multicore.erp." + relative
                .substring(0, relative.length() - ".class".length())
                .replace('/', '.').replace('\\', '.');
        try {
            Class<?> type = Class.forName(className);
            if (!type.isAnnotationPresent(RestController.class)) return;
            for (Method method : type.getDeclaredMethods()) {
                for (Parameter parameter : method.getParameters()) {
                    if (!parameter.isAnnotationPresent(RequestBody.class)) continue;
                    if (Map.class.isAssignableFrom(parameter.getType())) {
                        violations.add(type.getSimpleName() + "." + method.getName() + ": Map não permitido");
                    } else if (parameter.getType() != byte[].class
                            && !parameter.isAnnotationPresent(Valid.class)) {
                        violations.add(type.getSimpleName() + "." + method.getName() + ": falta @Valid");
                    }
                }
            }
        } catch (ReflectiveOperationException ex) {
            violations.add(className + ": não foi possível inspeccionar: " + ex.getMessage());
        }
    }
}
