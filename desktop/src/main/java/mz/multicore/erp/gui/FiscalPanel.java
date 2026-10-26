package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.FiscalApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Painel principal de Gestão Fiscal e Tributária de Moçambique.
 * Orquestrador desacoplado das abas de IVA, taxas, retenções na fonte, salários e declarações.
 */
public class FiscalPanel extends JPanel {

    private final FiscalApiClient fiscalApiClient;
    private final FiscalIvaTab ivaTab;
    private final FiscalTaxRatesTab taxRatesTab;
    private final FiscalWithholdingsTab withholdingsTab;
    private final FiscalPayrollTab payrollTab;
    private final FiscalDeclarationsTab declarationsTab;

    public FiscalPanel(FiscalApiClient fiscalApiClient) {
        this.fiscalApiClient = fiscalApiClient;
        this.ivaTab = new FiscalIvaTab(this, fiscalApiClient);
        this.taxRatesTab = new FiscalTaxRatesTab(this, fiscalApiClient);
        this.withholdingsTab = new FiscalWithholdingsTab(this, fiscalApiClient);
        this.payrollTab = new FiscalPayrollTab(this, fiscalApiClient);
        this.declarationsTab = new FiscalDeclarationsTab(ivaTab);

        setLayout(new BorderLayout(0, 8));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(8, 12, 8, 12));

        add(UIHelper.createHeading("Área Fiscal — Moçambique"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Apuramento IVA", UIHelper.icon("fas-percent", 16, UIHelper.MODULE_FISCAL), ivaTab.buildTab());
        tabs.addTab("Taxas Fiscais", UIHelper.icon("fas-balance-scale", 16, UIHelper.ACCENT_BLUE), taxRatesTab.buildTab());
        tabs.addTab("Retenções na Fonte", UIHelper.icon("fas-hand-holding-usd", 16, UIHelper.APPROVED_GREEN), withholdingsTab.buildTab());
        tabs.addTab("IRPS & INSS Salarial", UIHelper.icon("fas-users-cog", 16, UIHelper.ACCENT), payrollTab.buildTab());
        tabs.addTab("Declarações", UIHelper.icon("fas-file-pdf", 16, UIHelper.REJECTED_RED), declarationsTab.buildTab());

        add(tabs, BorderLayout.CENTER);
    }

    public void onPanelSelected() {
        refreshAll();
    }

    public void refreshAll() {
        taxRatesTab.loadTaxRates();
        withholdingsTab.loadWithholdings();
        ivaTab.recomputeIva();
        payrollTab.loadPayrollFiscal();
    }

    void showLoadError(String area, Throwable error) {
        ToastManager.show(this, FeedbackType.ERROR,
                "Não foi possível carregar " + area + ": " + (error != null ? error.getMessage() : "erro desconhecido"));
    }

    void showActionError(Throwable error) {
        ToastManager.show(this, FeedbackType.ERROR,
                "Erro: " + (error != null ? error.getMessage() : "operação falhou"));
    }
}
