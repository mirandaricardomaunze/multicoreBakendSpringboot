package mz.multicore.erp.desktop.update;

import mz.multicore.erp.architecture.version.SemanticVersion;

/**
 * Decides whether this workstation may update now, in the order of the spec (§4.2). Pure, so every refusal is
 * testable without a server or an installed program.
 */
public final class UpdateEligibility {

    private UpdateEligibility() {
    }

    /** What the caller knows about this workstation when the operator asks to update. */
    public record Workstation(boolean installed, String installedVersion, boolean serverRefusedVersion,
                              boolean tillSessionOpen, int suspendedSales) {
    }

    /** @return the refusal shown to the operator, or {@code null} when the update may proceed */
    public static String refusal(Workstation station, UpdateManifest manifest) {
        if (!station.installed()) {
            return "A actualização automática só funciona no programa instalado.";
        }
        if (!SemanticVersion.isOlderThan(station.installedVersion(), manifest.version())) {
            return "O instalador publicado (" + manifest.version() + ") não é mais recente do que este programa ("
                    + station.installedVersion() + ").";
        }
        // A workstation the server already refuses cannot sell: there is no till or sale to protect.
        if (station.serverRefusedVersion()) return null;
        if (station.tillSessionOpen()) {
            return "Feche a caixa antes de actualizar. O programa vai fechar durante a instalação.";
        }
        if (station.suspendedSales() > 0) {
            return station.suspendedSales() == 1
                    ? "Retome ou anule a venda suspensa antes de actualizar."
                    : "Retome ou anule as " + station.suspendedSales() + " vendas suspensas antes de actualizar.";
        }
        return null;
    }
}
