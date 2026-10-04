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
    @Test void convertsTwoBoxesWithoutResidualLooseUnit() {
        PackageQuantity value = PackageQuantity.fromPackages(2, 0, 12);
        assertEquals(24, value.totalUnits());
        assertEquals("2 cx", value.label());
    }
    @Test void convertsTotalBackToTwoBoxesAndLooseUnits() {
        PackageQuantity value = PackageQuantity.fromTotal(29, 12);
        assertEquals(2, value.boxes());
        assertEquals(5, value.looseUnits());
    }

    @Test void packagingComposition_calculatesUnitsPerBox() {
        PackagingComposition composition = PackagingComposition.of(12, 6);
        assertEquals(72, composition.unitsPerBox());
    }

    @Test void packagingComposition_rejectsInvalidAndOverflowingFactors() {
        assertThrows(BusinessRuleException.class, () -> PackagingComposition.of(0, 6));
        assertThrows(BusinessRuleException.class, () -> PackagingComposition.of(12, 0));
        assertThrows(BusinessRuleException.class,
                () -> PackagingComposition.of(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test void packagingQuantity_convertsAndDecomposesAllLevels() {
        PackagingComposition composition = PackagingComposition.of(12, 6);
        PackagingQuantity entered = PackagingQuantity.fromPackaging(2, 3, 4, composition);
        assertEquals(166, entered.totalUnits());

        PackagingQuantity decomposed = PackagingQuantity.fromTotal(166, composition);
        assertEquals(2, decomposed.boxes());
        assertEquals(3, decomposed.packages());
        assertEquals(4, decomposed.looseUnits());
        assertEquals("2 cx + 3 emb + 4 un", decomposed.label());
    }
}
