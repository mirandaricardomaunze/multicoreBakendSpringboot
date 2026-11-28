package mz.multicore.erp.desktop.update;

import mz.multicore.erp.desktop.update.UpdateEligibility.Workstation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** AU-02: order of the refusals (spec §4.2). */
class UpdateEligibilityTest {

    private static final UpdateManifest NEWER = new UpdateManifest("1.0.10", UpdateManifestTest.SHA, 100);

    @Test
    void notInstalled_refusedFirst() {
        assertThat(UpdateEligibility.refusal(new Workstation(false, "1.0.0", false, true, 3), NEWER))
                .contains("programa instalado");
    }

    @Test
    void publishedNotNewer_refused_comparingNumbersNotText() {
        assertThat(UpdateEligibility.refusal(new Workstation(true, "1.0.10", false, false, 0), NEWER))
                .contains("não é mais recente");
        // As text "1.0.9" > "1.0.10"; as versions it is older, so the update goes ahead.
        assertThat(UpdateEligibility.refusal(new Workstation(true, "1.0.9", false, false, 0), NEWER)).isNull();
    }

    @Test
    void openTill_refusedBeforeSuspendedSales() {
        assertThat(UpdateEligibility.refusal(new Workstation(true, "1.0.0", false, true, 2), NEWER))
                .contains("Feche a caixa");
    }

    @Test
    void suspendedSales_refusedWithCount() {
        assertThat(UpdateEligibility.refusal(new Workstation(true, "1.0.0", false, false, 2), NEWER))
                .contains("as 2 vendas suspensas");
        assertThat(UpdateEligibility.refusal(new Workstation(true, "1.0.0", false, false, 1), NEWER))
                .contains("a venda suspensa");
    }

    @Test
    void serverRefusedVersion_skipsTillAndSuspendedSales_butNotTheOtherChecks() {
        assertThat(UpdateEligibility.refusal(new Workstation(true, "1.0.0", true, true, 5), NEWER)).isNull();
        assertThat(UpdateEligibility.refusal(new Workstation(false, "1.0.0", true, false, 0), NEWER)).isNotNull();
        assertThat(UpdateEligibility.refusal(new Workstation(true, "1.0.10", true, false, 0), NEWER)).isNotNull();
    }
}
