package mz.multicore.erp.gui.components;

import mz.multicore.erp.desktop.client.ApiClientException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness of the single error phrasing (docs/MENSAGENS_ERRO_SPEC.md). */
class UserErrorsHarnessTest {

    private static final Path GUI = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("ER-01: frase única \"Não foi possível {acção}.\" seguida do motivo legível")
    void er01_singlePhrasing() {
        String msg = UserErrors.message("carregar o extrato",
                new ApiClientException(503, "O servidor está em manutenção"));
        assertThat(msg).isEqualTo("Não foi possível carregar o extrato. O servidor está em manutenção.");
    }

    @Test
    @DisplayName("ER-02: erro técnico não chega ao operador; mostra o que fazer a seguir")
    void er02_technicalErrorsAreHidden() {
        assertThat(UserErrors.message("gravar", new NullPointerException("Cannot invoke \"x.y()\"")))
                .isEqualTo("Não foi possível gravar. " + UserErrors.TECHNICAL_HINT);
        assertThat(UserErrors.message("gravar", new RuntimeException("null")))
                .endsWith(UserErrors.TECHNICAL_HINT);
    }

    @Test
    @DisplayName("ER-03: usa a mensagem da causa quando o invólucro não tem texto; sem acção diz \"concluir a operação\"")
    void er03_causeAndDefaultAction() {
        assertThat(UserErrors.message(null, new RuntimeException(new IllegalArgumentException("Stock insuficiente!"))))
                .isEqualTo("Não foi possível concluir a operação. Stock insuficiente!");
    }

    @Test
    @DisplayName("ER-04: nenhum toast ou diálogo monta \"Erro ao …: \" + e.getMessage() à mão")
    void er04_noHandBuiltErrorText() {
        Pattern handBuilt = Pattern.compile("(?:ToastManager|ModernMessageDialog)\\.show\\([^;]*FeedbackType\\.ERROR,[^;]*"
                + "\"(?:Erro ao|Erro a|Falha ao|Falha a|Não foi possível) [^\"]*:\\s*\"\\s*\\+\\s*\\w+\\.getMessage\\(\\)");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(GUI)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    Matcher m = handBuilt.matcher(Files.readString(p));
                    while (m.find()) offenders.add(GUI.relativize(p).toString().replace('\\', '/'));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertThat(offenders).isEmpty();
    }
}
