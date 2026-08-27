package mz.multicore.erp.modules.comercial.model;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class OrderFulfillmentStatusHarnessTest {
 @Test void acceptsHappyPath() {
  assertDoesNotThrow(() -> OrderFulfillmentStatus.AWAITING_SEPARATION.requireTransitionTo(OrderFulfillmentStatus.IN_SEPARATION));
  assertDoesNotThrow(() -> OrderFulfillmentStatus.IN_SEPARATION.requireTransitionTo(OrderFulfillmentStatus.SEPARATED));
  assertDoesNotThrow(() -> OrderFulfillmentStatus.SEPARATED.requireTransitionTo(OrderFulfillmentStatus.INVOICED));
 }
 @Test void preventsSkippingAndDuplicateInvoice() {
  assertThrows(BusinessRuleException.class, () -> OrderFulfillmentStatus.AWAITING_SEPARATION.requireTransitionTo(OrderFulfillmentStatus.SEPARATED));
  assertThrows(BusinessRuleException.class, () -> OrderFulfillmentStatus.INVOICED.requireTransitionTo(OrderFulfillmentStatus.INVOICED));
 }
 @Test void preventsCancellingInvoice() {
  assertThrows(BusinessRuleException.class, () -> OrderFulfillmentStatus.INVOICED.requireTransitionTo(OrderFulfillmentStatus.CANCELLED));
 }
}
