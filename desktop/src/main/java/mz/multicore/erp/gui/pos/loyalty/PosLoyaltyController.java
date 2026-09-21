package mz.multicore.erp.gui.pos.loyalty;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.LoyaltyEngine;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Controller desacoplado para gestão do Cartão de Fidelidade do Cliente e resgate de pontos no POS.
 */
public class PosLoyaltyController {

    private final ComercialApiClient comercialApiClient;
    private ClientDTO selectedLoyaltyClient;
    private BigDecimal pointsToRedeem = BigDecimal.ZERO;
    private BigDecimal loyaltyDiscountMzn = BigDecimal.ZERO;

    public PosLoyaltyController(ComercialApiClient comercialApiClient) {
        this.comercialApiClient = comercialApiClient;
    }

    public Optional<ClientDTO> getSelectedLoyaltyClient() {
        return Optional.ofNullable(selectedLoyaltyClient);
    }

    public void setSelectedLoyaltyClient(ClientDTO client) {
        this.selectedLoyaltyClient = client;
        if (client == null) {
            this.pointsToRedeem = BigDecimal.ZERO;
            this.loyaltyDiscountMzn = BigDecimal.ZERO;
        }
    }

    public BigDecimal getPointsToRedeem() {
        return pointsToRedeem;
    }

    public BigDecimal getLoyaltyDiscountMzn() {
        return loyaltyDiscountMzn;
    }

    public void resetRedemption() {
        this.pointsToRedeem = BigDecimal.ZERO;
        this.loyaltyDiscountMzn = BigDecimal.ZERO;
    }

    /**
     * Abre o diálogo modal de pesquisa rápida de Cartão de Fidelidade (Atalho F7 ou Leitura Scanner).
     */
    public void openLoyaltyCardDialog(Component parent, Runnable onClientSelectedCallback) {
        JTextField searchField = new JTextField();
        UIHelper.styleTextField(searchField);
        searchField.setToolTipText("Digite ou bipe o Cartão (ex: LOY-1001), NUIT ou número de telefone");

        JLabel infoLabel = new JLabel("Pressione Enter ou clique em Seleccionar");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(UIHelper.TEXT_MUTED);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setOpaque(false);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setOpaque(false);
        inputPanel.add(new JLabel("Código / NUIT / Telemóvel:"), BorderLayout.NORTH);
        inputPanel.add(searchField, BorderLayout.CENTER);

        content.add(inputPanel, BorderLayout.NORTH);
        content.add(infoLabel, BorderLayout.CENTER);

        Window parentWindow = SwingUtilities.getWindowAncestor(parent);
        ModernFormDialog dialog = new ModernFormDialog(
                parentWindow,
                "Cartão de Fidelidade do Cliente",
                "fas-star",
                "Pesquisar por Código do Cartão, NUIT ou Telemóvel (Atalho F7)",
                content
        );

        dialog.setConfirmButton("Seleccionar Cliente", "fas-check");
        dialog.setOnSave(() -> {
            String query = searchField.getText().trim();
            if (query.isEmpty()) {
                throw new IllegalArgumentException("Por favor, introduza o código do cartão, NUIT ou telefone.");
            }

            try {
                List<ClientDTO> clients = comercialApiClient != null ? comercialApiClient.getClients() : List.of();
                Optional<ClientDTO> match = clients.stream()
                        .filter(c -> (c.taxId() != null && c.taxId().equalsIgnoreCase(query))
                                || (c.code() != null && c.code().equalsIgnoreCase(query))
                                || (c.name() != null && c.name().toLowerCase().contains(query.toLowerCase())))
                        .findFirst();

                if (match.isPresent()) {
                    setSelectedLoyaltyClient(match.get());
                    BigDecimal points = match.get().loyaltyPoints() != null ? match.get().loyaltyPoints() : BigDecimal.ZERO;
                    BigDecimal valueMzn = LoyaltyEngine.pointsToValueMzn(points);

                    JOptionPane.showMessageDialog(parentWindow,
                            String.format("Cliente Fidelizado Encontrado!\n\nNome: %s\nPontos Acumulados: %s pts (%.2f MT)",
                                    match.get().name(), points.toPlainString(), valueMzn),
                            "Cartão de Fidelidade", JOptionPane.INFORMATION_MESSAGE);

                    if (onClientSelectedCallback != null) {
                        onClientSelectedCallback.run();
                    }
                } else {
                    throw new IllegalArgumentException("Nenhum cliente fidelizado encontrado para: " + query);
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception ex) {
                throw new IllegalStateException("Erro ao consultar clientes: " + ex.getMessage(), ex);
            }
        });

        dialog.showDialog();
    }

    /**
     * Prepara o resgate de pontos de fidelidade para abater no total da venda.
     */
    public LoyaltyEngine.RedemptionResult calculateRedemption(BigDecimal saleTotal, BigDecimal pointsRequested) {
        if (selectedLoyaltyClient == null) {
            return new LoyaltyEngine.RedemptionResult(BigDecimal.ZERO, BigDecimal.ZERO, saleTotal, BigDecimal.ZERO);
        }

        BigDecimal available = selectedLoyaltyClient.loyaltyPoints() != null ? selectedLoyaltyClient.loyaltyPoints() : BigDecimal.ZERO;
        LoyaltyEngine.RedemptionResult result = LoyaltyEngine.applyRedemption(available, saleTotal, pointsRequested);

        this.pointsToRedeem = result.pointsRedeemed();
        this.loyaltyDiscountMzn = result.discountMzn();

        return result;
    }
}
