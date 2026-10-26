package mz.multicore.erp.architecture.validation;

import mz.multicore.erp.architecture.exception.BusinessRuleException;

public final class ImagePayloadValidator {

    private ImagePayloadValidator() {}

    public static void validate(byte[] data, int maxBytes) {
        if (data == null || data.length == 0) {
            return;
        }
        if (data.length > maxBytes) {
            throw new BusinessRuleException("A imagem excede o tamanho máximo permitido.");
        }
        if (!isPng(data) && !isJpeg(data)) {
            throw new BusinessRuleException("A imagem deve estar no formato PNG ou JPEG.");
        }
    }

    private static boolean isPng(byte[] data) {
        return data.length >= 8
                && (data[0] & 0xFF) == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47
                && data[4] == 0x0D && data[5] == 0x0A && data[6] == 0x1A && data[7] == 0x0A;
    }

    private static boolean isJpeg(byte[] data) {
        return data.length >= 3
                && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF;
    }
}
