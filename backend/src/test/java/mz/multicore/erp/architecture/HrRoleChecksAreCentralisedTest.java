package mz.multicore.erp.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>A verificação de perfil do RH tem uma implementação, não treze.</b>
 *
 * <p>Até esta guarda existir, treze serviços do módulo RH tinham cada um a sua cópia privada de
 * {@code !"ADMIN".equalsIgnoreCase(role) && !"MANAGER".equalsIgnoreCase(role)}, enquanto o projecto
 * já tinha um {@code PermissionGuard.requireManagerOrAdmin} canónico. A mecânica era idêntica em
 * todas; só a mensagem mudava.
 *
 * <p>O perigo de treze cópias de uma regra de autorização não é a repetição — é a divergência.
 * Bastava a décima quarta ser escrita com {@code equals} em vez de {@code equalsIgnoreCase}, ou
 * esquecer o {@code ADMIN}, para abrir uma porta que <b>não se vê a correr o programa</b>: os
 * testes do serviço passam, o ecrã comporta-se na perfeição, e a única prova está num operador de
 * comparação. É o mesmo argumento do {@code HrDoesNotKnowAccountingTest} — a diferença entre as
 * duas soluções só se vê no código-fonte, por isso é no código-fonte que se mede.
 */
class HrRoleChecksAreCentralisedTest {

    private static final Path HR = Path.of("src", "main", "java", "mz", "multicore", "erp",
            "modules", "hr");

    /** A comparação de papéis à mão, em qualquer das formas em que apareceu. */
    private static final Pattern HAND_ROLLED = Pattern.compile(
            "\"(ADMIN|MANAGER)\"\\s*\\.\\s*equals(IgnoreCase)?\\s*\\(\\s*role");

    @Test
    void noHrSourceComparesRolesByHand() throws IOException {
        List<String> offenders;
        try (Stream<Path> sources = Files.walk(HR)) {
            offenders = sources
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(HrRoleChecksAreCentralisedTest::comparesRolesByHand)
                    .map(Path::toString)
                    .toList();
        }

        assertThat(offenders)
                .as("ficheiros do RH que comparam o perfil à mão — a verificação faz-se por "
                        + "PermissionGuard.requireManagerOrAdmin(operacao) ou pelo HrAccessGuard")
                .isEmpty();
    }

    /** E o guard do RH não pode voltar a ter regra própria: delega no canónico. */
    @Test
    void hrAccessGuardDelegatesToTheCanonicalPermissionGuard() throws IOException {
        String guard = Files.readString(HR.resolve(Path.of("service", "HrAccessGuard.java")));

        assertThat(guard)
                .as("o HrAccessGuard tem de encaminhar para o PermissionGuard, não reimplementá-lo")
                .contains("PermissionGuard.requireManagerOrAdmin")
                .contains("PermissionGuard.isManagerOrAdmin");
        assertThat(HAND_ROLLED.matcher(withoutComments(guard)).find())
                .as("nem o próprio guard pode comparar papéis à mão")
                .isFalse();
    }

    private static boolean comparesRolesByHand(Path source) {
        try {
            return HAND_ROLLED.matcher(withoutComments(Files.readString(source))).find();
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível ler " + source, ex);
        }
    }

    /**
     * Um comentário não é código. Esta guarda tem de deixar documentar o padrão antigo — é
     * precisamente onde se explica porque é que ele saiu.
     */
    private static String withoutComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }
}
