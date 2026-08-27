package mz.multicore.erp.modules.purchases.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

/**
 * Recepção de mercadoria: o que a carrinha do fornecedor realmente descarregou.
 *
 * <p>Distingue o que <b>entra em stock</b> do que <b>chegou estragado</b>. Sem essa distinção,
 * recebia-se 97 de 100 e ficava-se sem saber se faltaram 3 ou se vieram 3 partidos — e sem nada
 * que sustentasse uma reclamação ao fornecedor. Ver docs/CONFERENCIA_CHEGADA_SPEC.md.
 */
public record ReceivePurchaseOrderRequest(
        @NotEmpty(message = "Indique pelo menos uma linha a receber.")
        @Valid List<ReceiveLine> lines
) {
    /**
     * Uma linha da conferência. O que se encomendou reparte-se em três:
     * <pre>
     *   encomendado 100 = 95 boas (entram em stock) + 2 danificadas + 3 em falta
     * </pre>
     *
     * @param quantity        quantidade <b>em boas condições</b>, a única que entra em stock
     * @param damagedQuantity chegou estragada ou foi recusada na descarga — não entra em stock,
     *                        mas fica registada para reclamar ao fornecedor
     * @param missingQuantity não chegou e o operador declara que não virá (fecho curto)
     * @param notes           descrição da ocorrência (ex.: "2 sacos rasgados na descarga")
     */
    public record ReceiveLine(
            @NotNull(message = "Linha em falta.") Long lineId,
            @NotNull @Positive(message = "Quantidade a receber tem de ser positiva.") BigDecimal quantity,

            @PositiveOrZero(message = "A quantidade danificada não pode ser negativa.")
            BigDecimal damagedQuantity,

            @PositiveOrZero(message = "A quantidade em falta não pode ser negativa.")
            BigDecimal missingQuantity,

            String notes
    ) {

        /** Retrocompatível: recepção sem conferência de divergências (o comportamento anterior). */
        public ReceiveLine(Long lineId, BigDecimal quantity) {
            this(lineId, quantity, null, null, null);
        }

        public BigDecimal safeDamaged() {
            return damagedQuantity == null ? BigDecimal.ZERO : damagedQuantity;
        }

        public BigDecimal safeMissing() {
            return missingQuantity == null ? BigDecimal.ZERO : missingQuantity;
        }

        /** Total que esta linha consome da encomenda: o que entrou mais o que se perdeu. */
        public BigDecimal totalAccountedFor() {
            return quantity.add(safeDamaged()).add(safeMissing());
        }

        public boolean hasDiscrepancy() {
            return safeDamaged().signum() > 0 || safeMissing().signum() > 0;
        }
    }
}
