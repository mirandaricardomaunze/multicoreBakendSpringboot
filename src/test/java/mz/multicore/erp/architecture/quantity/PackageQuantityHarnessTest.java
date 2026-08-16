package mz.multicore.erp.architecture.quantity;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PackageQuantityHarnessTest {
    @Test void convertsBoxesAndLooseUnitsToTotal() {
        assertEquals(41, PackageQuantity.fromPackages(3, 5, 12).totalUnits());
    }
    @Test void decomposesTotalIntoBoxesAndLooseUnits() {
        PackageQuantity value = PackageQuantity.fromTotal(41, 12);
        assertEquals(3, value.boxes()); assertEquals(5, value.looseUnits());
        assertEquals("3 cx + 5 un", value.label());
    }
    @Test void rejectsLooseUnitsThatAlreadyMakeABox() {
        assertThrows(BusinessRuleException.class, () -> PackageQuantity.fromPackages(1, 12, 12));
    }
}
