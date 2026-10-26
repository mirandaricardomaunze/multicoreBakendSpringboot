package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;

import javax.swing.JComboBox;

/**
 * Gestão de elegibilidade, restrições e políticas de crédito do cliente no editor de encomendas.
 */
final class CommercialOrderCreditPolicy {

    private CommercialOrderCreditPolicy() {}

    static void handleClientChanged(ComercialPanel owner) {
        if (owner.orderClientCombo == null) return;
        int clientIndex = owner.orderClientCombo.getSelectedIndex();
        if (clientIndex <= 0 || owner.clientsList == null || clientIndex - 1 >= owner.clientsList.size()) {
            if (owner.orderClientWalkInField != null) owner.orderClientWalkInField.setEnabled(true);
            if (owner.orderPaymentTermsCombo != null) {
                owner.orderPaymentTermsCombo.setSelectedItem("Pronto Pagamento");
                owner.orderPaymentTermsCombo.setEnabled(false);
            }
            if (owner.orderClientCreditWarningLabel != null) owner.orderClientCreditWarningLabel.setVisible(false);
            return;
        }

        if (owner.orderClientWalkInField != null) {
            owner.orderClientWalkInField.setEnabled(false);
            owner.orderClientWalkInField.setText("");
        }

        ClientDTO client = owner.clientsList.get(clientIndex - 1);

        if (client.creditLimit() != null && client.creditLimit().signum() == 0) {
            if (owner.orderPaymentTermsCombo != null) {
                owner.orderPaymentTermsCombo.setSelectedItem("Pronto Pagamento");
                owner.orderPaymentTermsCombo.setEnabled(false);
            }
            if (owner.orderClientCreditWarningLabel != null) {
                owner.orderClientCreditWarningLabel.setText(" Cliente sem autorização para crédito (limite zero) — encomenda limitada a Pronto Pagamento.");
                owner.orderClientCreditWarningLabel.setVisible(true);
            }
            return;
        }

        UIHelper.loadAsync(owner, () -> owner.comercialApiClient.getClientCreditEligibility(client.id()), eligibility -> {
            if (!eligibility.creditAllowed() || eligibility.problematic()) {
                if (owner.orderPaymentTermsCombo != null) {
                    owner.orderPaymentTermsCombo.setSelectedItem("Pronto Pagamento");
                    owner.orderPaymentTermsCombo.setEnabled(false);
                }
                if (owner.orderClientCreditWarningLabel != null) {
                    String reason = eligibility.restrictionReason() != null ? eligibility.restrictionReason() : "Risco de crédito";
                    owner.orderClientCreditWarningLabel.setText(" Restrição de Crédito: " + reason + " — Encomenda limitada a Pronto Pagamento.");
                    owner.orderClientCreditWarningLabel.setVisible(true);
                }
            } else {
                if (owner.orderPaymentTermsCombo != null) {
                    owner.orderPaymentTermsCombo.setEnabled(true);
                    if (owner.editingOrder == null) {
                        if (eligibility.defaultPaymentTermsDays() > 0) {
                            String opt = eligibility.defaultPaymentTermsDays() + " dias";
                            ensurePaymentTermsOption(owner.orderPaymentTermsCombo, opt);
                            owner.orderPaymentTermsCombo.setSelectedItem(opt);
                        } else {
                            owner.orderPaymentTermsCombo.setSelectedItem("Pronto Pagamento");
                        }
                    }
                }
                if (owner.orderClientCreditWarningLabel != null) owner.orderClientCreditWarningLabel.setVisible(false);
            }
        }, error -> {
            if (client.creditLimit() != null && client.creditLimit().signum() == 0) {
                if (owner.orderPaymentTermsCombo != null) {
                    owner.orderPaymentTermsCombo.setSelectedItem("Pronto Pagamento");
                    owner.orderPaymentTermsCombo.setEnabled(false);
                }
                if (owner.orderClientCreditWarningLabel != null) {
                    owner.orderClientCreditWarningLabel.setText(" Cliente sem crédito — limitado a Pronto Pagamento.");
                    owner.orderClientCreditWarningLabel.setVisible(true);
                }
            }
        });
    }

    static void ensurePaymentTermsOption(JComboBox<String> combo, String option) {
        if (combo == null || option == null || option.isBlank()) return;
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (option.equalsIgnoreCase(combo.getItemAt(i))) return;
        }
        combo.addItem(option);
    }

    static int clientIndex(ComercialPanel owner, Long clientId) {
        if (clientId == null) return 0;
        for (int index = 0; index < owner.clientsList.size(); index++) {
            if (clientId.equals(owner.clientsList.get(index).id())) return index + 1;
        }
        return 0;
    }
}
