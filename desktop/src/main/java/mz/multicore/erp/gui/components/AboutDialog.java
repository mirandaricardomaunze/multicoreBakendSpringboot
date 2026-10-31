package mz.multicore.erp.gui.components;

import java.awt.Component;

/**
 * Diálogo executivo canónico "Sobre o Multicore ERP" com identificação detalhada
 * do autor e arquiteto do sistema: Miranda Ricardo Maunze.
 */
public final class AboutDialog {

    private AboutDialog() {}

    /**
     * Apresenta o diálogo de Sobre o Sistema centrado na janela pai informada.
     */
    public static void show(Component parent) {
        ExecutiveDetailDialog dialog = ExecutiveDetailDialog.create(parent, "Sobre o Multicore ERP");

        dialog.setTitle("Multicore Enterprise ERP");
        dialog.setSubtitle("Sistema Integrado de Gestão Empresarial • Moçambique");
        dialog.setHeaderIcon("fas-layer-group", 36, UIHelper.ACCENT_BLUE);
        dialog.setStatusBadge("AUTOR & ARQUITETO: MIRANDA RICARDO MAUNZE", ExecutiveDetailDialog.StatusSeverity.SUCCESS);

        dialog.addKpi("Autor & Concepção", "Miranda Ricardo Maunze", "Liderança Técnica & Arquitetura", UIHelper.ACCENT_BLUE, "fas-user-tie");
        dialog.addKpi("Versão do Sistema", "v1.0.0 Enterprise", "Build de Produção Estável", UIHelper.APPROVED_GREEN, "fas-code-branch");
        dialog.addKpi("Mercado & Jurisdição", "Moçambique", "Conformidade Fiscal AT / IVA", UIHelper.PENDING_YELLOW, "fas-globe-africa");
        dialog.addKpi("Engenharia", "Hexagonal / Reactor", "Java 21 • Spring Boot 3 • Swing", UIHelper.MODULE_ACCOUNTING, "fas-cubes");

        dialog.addTab("Autoria & Concepção", "fas-user-shield", UIHelper.ACCENT_BLUE, AboutDialogTabBuilder.buildAuthorTab());
        dialog.addTab("Módulos Integrados", "fas-th-large", UIHelper.APPROVED_GREEN, AboutDialogTabBuilder.buildModulesTab());
        dialog.addTab("Arquitetura & Engenharia", "fas-microchip", UIHelper.MODULE_ACCOUNTING, AboutDialogTabBuilder.buildArchitectureTab());
        dialog.addTab("Licenciamento & Direitos", "fas-certificate", UIHelper.PENDING_YELLOW, AboutDialogTabBuilder.buildLicenseTab());

        dialog.showDialog();
    }
}
