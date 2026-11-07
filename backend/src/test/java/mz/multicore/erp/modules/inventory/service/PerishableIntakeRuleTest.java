package mz.multicore.erp.modules.inventory.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.inventory.model.StockMovementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;

import static mz.multicore.erp.modules.inventory.service.StockMovementRegistrationService.requireExpiryForPerishableIntake;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Produto perecível exige validade na compra e na entrada manual; os restantes não. */
class PerishableIntakeRuleTest {

    @ParameterizedTest
    @EnumSource(value = StockMovementType.class, names = {"PURCHASE", "ENTRY"})
    @DisplayName("Perecível sem validade numa compra ou entrada manual é recusado")
    void perishableIntakeWithoutExpiry_rejected(StockMovementType type) {
        assertThatThrownBy(() -> requireExpiryForPerishableIntake(product(true), type, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Iogurte")
                .hasMessageContaining("validade");
    }

    @ParameterizedTest
    @EnumSource(value = StockMovementType.class, names = {"PURCHASE", "ENTRY"})
    @DisplayName("Não perecível (loiça, mobília) entra sem validade")
    void nonPerishableIntakeWithoutExpiry_allowed(StockMovementType type) {
        assertThatCode(() -> requireExpiryForPerishableIntake(product(false), type, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Perecível com validade entra normalmente")
    void perishableWithExpiry_allowed() {
        assertThatCode(() -> requireExpiryForPerishableIntake(
                product(true), StockMovementType.PURCHASE, LocalDate.now().plusMonths(6)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = StockMovementType.class, names = {"RETURN", "REVERSAL", "TRANSFER", "ADJUSTMENT"})
    @DisplayName("Devoluções, estornos, transferências e acertos não exigem validade")
    void restockingMovements_notAffected(StockMovementType type) {
        assertThatCode(() -> requireExpiryForPerishableIntake(product(true), type, null))
                .doesNotThrowAnyException();
    }

    private static Product product(boolean perishable) {
        Product product = new Product();
        product.setName(perishable ? "Iogurte" : "Cadeira");
        product.setTracksExpiry(perishable);
        return product;
    }
}
