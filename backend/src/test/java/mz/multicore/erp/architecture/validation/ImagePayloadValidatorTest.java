package mz.multicore.erp.architecture.validation;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImagePayloadValidatorTest {

    @Test
    void acceptsEmptyPngAndJpegPayloads() {
        assertDoesNotThrow(() -> ImagePayloadValidator.validate(null, 100));
        assertDoesNotThrow(() -> ImagePayloadValidator.validate(new byte[0], 100));
        assertDoesNotThrow(() -> ImagePayloadValidator.validate(
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, 100));
        assertDoesNotThrow(() -> ImagePayloadValidator.validate(
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, 100));
    }

    @Test
    void rejectsOversizedAndUnknownPayloads() {
        assertThrows(BusinessRuleException.class,
                () -> ImagePayloadValidator.validate(new byte[101], 100));
        assertThrows(BusinessRuleException.class,
                () -> ImagePayloadValidator.validate(new byte[]{1, 2, 3, 4}, 100));
    }
}
