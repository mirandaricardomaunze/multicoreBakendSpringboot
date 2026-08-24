package mz.multicore.erp.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RHC-54 do harness do RH: <b>o RH não conhece a contabilidade</b>.
 *
 * <p>A folha passou a chegar ao razão e ao balancete (§B5), e havia duas maneiras de lá chegar. A
 * fácil era o {@code HRService} chamar o serviço de lançamentos — e ficava a funcionar. Mas criava
 * a dependência que o comercial deliberadamente não tem: o RH passaria a ter de saber o que é uma
 * partida dobrada para pagar um salário, e mudar o plano de contas passaria a partir a folha.
 *
 * <p>A ligação faz-se por <b>evento</b> ({@code PayslipPaidEvent},
 * {@code PayrollLiabilityDeliveredEvent}), como o {@code SaleRegisteredEvent} do comercial. Esta
 * guarda existe porque a diferença entre as duas soluções não se vê a correr o programa: as duas
 * produzem o mesmo lançamento. Só se vê num import.
 */
class HrDoesNotKnowAccountingTest {

    private static final Path HR = Path.of("src", "main", "java", "mz", "multicore", "erp",
            "modules", "hr");

    @Test
    void hrModuleHasNoImportFromAccounting() throws IOException {
        List<String> offenders;
        try (Stream<Path> sources = Files.walk(HR)) {
            offenders = sources
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(HrDoesNotKnowAccountingTest::importsAccounting)
                    .map(Path::toString)
                    .toList();
        }

        assertThat(offenders)
                .as("ficheiros do módulo RH que importam a contabilidade — a ligação tem de ser por "
                        + "evento (PayslipPaidEvent), não por chamada directa")
                .isEmpty();
    }

    /** O contrário também: a contabilidade ouve eventos, não vai buscar recibos ao RH. */
    @Test
    void accountingPostingReadsEventsAndNotHrEntities() throws IOException {
        String posting = Files.readString(Path.of("src", "main", "java", "mz", "multicore", "erp",
                "modules", "accounting", "service", "AutomaticPostingService.java"));

        assertThat(posting)
                .as("o serviço de lançamentos automáticos não pode depender do modelo do RH")
                .doesNotContain("modules.hr.model")
                .doesNotContain("modules.hr.repository")
                .doesNotContain("modules.hr.service");
        assertThat(posting)
                .as("mas tem de ouvir os eventos da folha")
                .contains("PayslipPaidEvent")
                .contains("PayrollLiabilityDeliveredEvent");
    }

    private static boolean importsAccounting(Path source) {
        try {
            return Files.readString(source).contains("mz.multicore.erp.modules.accounting");
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível ler " + source, ex);
        }
    }
}
