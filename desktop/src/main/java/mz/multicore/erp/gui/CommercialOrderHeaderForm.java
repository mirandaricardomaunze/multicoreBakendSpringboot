package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.model.OrderKind;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Constrói e gere o formulário de cabeçalho do editor de encomendas (cliente, modalidade, armazém, tipo e notas).
 */
final class CommercialOrderHeaderForm {

    private CommercialOrderHeaderForm() {}

    /** O que muda ao escolher cada via, dito antes de o operador escolher. */
    static String kindHint(OrderKind kind) {
        return kind.isThermal()
                ? "Reserva stock e imprime talão para o armazém separar. Não passa por aprovação."
                : "Documento A4 igual à fatura. Passa por aprovação antes de poder ser faturada.";
    }

    static ModernPanel createHeaderCard(ComercialPanel owner) {
        ModernPanel formCard = new ModernPanel(16);
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(12, 16, 10, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Coluna 0: Cliente
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 1.2;
        gbc.insets = new Insets(2, 6, 2, 6);
        JLabel clientLbl = new JLabel("Cliente:");
        clientLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(clientLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 6, 4, 6);
        owner.orderClientCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.orderClientCombo);
        formCard.add(owner.orderClientCombo, gbc);

        // Coluna 1: Nome do Comprador (Walk-in se 'Consumidor Final')
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        gbc.insets = new Insets(2, 6, 2, 6);
        JLabel walkInLbl = new JLabel("Comprador (se Consumidor Final):");
        walkInLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(walkInLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 6, 4, 6);
        owner.orderClientWalkInField = new JTextField();
        UIHelper.styleTextField(owner.orderClientWalkInField);
        owner.orderClientWalkInField.putClientProperty("JTextField.placeholderText", "Nome do comprador...");
        formCard.add(owner.orderClientWalkInField, gbc);

        // Coluna 2: Modalidade de Pagamento
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 1.0;
        gbc.insets = new Insets(2, 6, 2, 6);
        JLabel termsLbl = new JLabel("Modalidade de pagamento:");
        termsLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(termsLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 6, 4, 6);
        owner.orderPaymentTermsCombo = new JComboBox<>(new String[]{
                "Pronto Pagamento", "15 dias", "30 dias", "45 dias", "60 dias", "90 dias"
        });
        owner.orderPaymentTermsCombo.setEditable(true);
        UIHelper.styleComboBox(owner.orderPaymentTermsCombo);
        formCard.add(owner.orderPaymentTermsCombo, gbc);

        // Coluna 3: Armazém Origem
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.8;
        gbc.insets = new Insets(2, 6, 2, 6);
        JLabel warehouseLbl = new JLabel("Armazém:");
        warehouseLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(warehouseLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 6, 4, 6);
        owner.orderWarehouseCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.orderWarehouseCombo);
        formCard.add(owner.orderWarehouseCombo, gbc);

        // Coluna 4: Tipo de Encomenda
        gbc.gridx = 4; gbc.gridy = 0; gbc.weightx = 0.8;
        gbc.insets = new Insets(2, 6, 2, 6);
        JLabel kindLbl = new JLabel("Tipo de encomenda:");
        kindLbl.setForeground(UIHelper.TEXT_MUTED);
        formCard.add(kindLbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 6, 4, 6);
        owner.orderKindCombo = new JComboBox<>(OrderKind.values());
        owner.orderKindCombo.setRenderer(UIHelper.labelRenderer(OrderKind::label));
        UIHelper.styleComboBox(owner.orderKindCombo);
        owner.orderKindCombo.setSelectedItem(OrderKind.PICKING_REQUEST);
        formCard.add(owner.orderKindCombo, gbc);

        // Coluna 5: Armazém Destino (visível apenas para reposição interna)
        JLabel destLbl = new JLabel("Armazém de destino:");
        destLbl.setForeground(UIHelper.TEXT_MUTED);
        owner.orderDestinationCombo = new JComboBox<>();
        UIHelper.styleComboBox(owner.orderDestinationCombo);

        JPanel destPanel = new JPanel(new BorderLayout(0, 2));
        destPanel.setOpaque(false);
        destPanel.add(destLbl, BorderLayout.NORTH);
        destPanel.add(owner.orderDestinationCombo, BorderLayout.CENTER);

        gbc.gridx = 5; gbc.gridy = 0; gbc.gridheight = 2; gbc.weightx = 0.8;
        gbc.insets = new Insets(2, 6, 4, 6);
        formCard.add(destPanel, gbc);

        // Linha 2: Dica contextual da via selecionada + Alerta de crédito para clientes problemáticos
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 6; gbc.gridheight = 1; gbc.weightx = 1.0;
        gbc.insets = new Insets(3, 6, 2, 6);
        JPanel hintsPanel = new JPanel(new BorderLayout(12, 0));
        hintsPanel.setOpaque(false);

        JLabel kindHint = new JLabel();
        kindHint.setForeground(UIHelper.TEXT_MUTED);
        kindHint.setFont(kindHint.getFont().deriveFont(11f));
        kindHint.setIcon(UIHelper.semanticIcon("fas-info-circle", 12));
        hintsPanel.add(kindHint, BorderLayout.WEST);

        owner.orderClientCreditWarningLabel = new JLabel();
        owner.orderClientCreditWarningLabel.setFont(owner.orderClientCreditWarningLabel.getFont().deriveFont(Font.BOLD, 11f));
        owner.orderClientCreditWarningLabel.setForeground(UIHelper.REJECTED_RED);
        owner.orderClientCreditWarningLabel.setIcon(UIHelper.semanticIcon("fas-exclamation-triangle", 12));
        owner.orderClientCreditWarningLabel.setVisible(false);
        hintsPanel.add(owner.orderClientCreditWarningLabel, BorderLayout.CENTER);

        formCard.add(hintsPanel, gbc);

        Runnable syncKind = () -> {
            OrderKind kind = owner.selectedOrderKind();
            kindHint.setText(" " + kindHint(kind));
            boolean replenishment = kind.requiresDestinationWarehouse();
            destPanel.setVisible(replenishment);
            GridBagLayout layout = (GridBagLayout) formCard.getLayout();
            GridBagConstraints c = layout.getConstraints(destPanel);
            c.weightx = replenishment ? 0.8 : 0.0;
            layout.setConstraints(destPanel, c);
            formCard.revalidate();
            formCard.repaint();
        };
        owner.orderKindCombo.addActionListener(e -> syncKind.run());
        syncKind.run();

        // Linha 3: Nota no pé da página / Observações
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 6; gbc.gridheight = 1; gbc.weightx = 1.0;
        gbc.insets = new Insets(3, 6, 4, 6);
        JPanel notesPanel = new JPanel(new BorderLayout(0, 2));
        notesPanel.setOpaque(false);
        JLabel notesLbl = new JLabel("Nota no pé da página / Observações:");
        notesLbl.setForeground(UIHelper.TEXT_MUTED);
        owner.orderNotesField = new JTextField();
        UIHelper.styleTextField(owner.orderNotesField);
        owner.orderNotesField.setToolTipText("Instruções de entrega, condições bancárias ou nota institucional impressa no rodapé do documento.");
        notesPanel.add(notesLbl, BorderLayout.NORTH);
        notesPanel.add(owner.orderNotesField, BorderLayout.CENTER);
        formCard.add(notesPanel, gbc);

        // Instalar listeners de alteração para controlo de dirty state
        owner.orderClientCombo.addActionListener(e -> {
            owner.markOrderEditorDirty();
            CommercialOrderEditorActions.handleClientChanged(owner);
        });
        if (owner.orderPaymentTermsCombo != null) {
            owner.orderPaymentTermsCombo.addActionListener(e -> owner.markOrderEditorDirty());
            if (owner.orderPaymentTermsCombo.getEditor() != null
                    && owner.orderPaymentTermsCombo.getEditor().getEditorComponent() instanceof JTextField tf) {
                UIHelper.onTextChange(tf, owner::markOrderEditorDirty);
            }
        }
        owner.orderWarehouseCombo.addActionListener(e -> owner.markOrderEditorDirty());
        owner.orderDestinationCombo.addActionListener(e -> owner.markOrderEditorDirty());
        owner.orderKindCombo.addActionListener(e -> owner.markOrderEditorDirty());
        UIHelper.onTextChange(owner.orderClientWalkInField, owner::markOrderEditorDirty);
        UIHelper.onTextChange(owner.orderNotesField, owner::markOrderEditorDirty);

        return formCard;
    }
}
