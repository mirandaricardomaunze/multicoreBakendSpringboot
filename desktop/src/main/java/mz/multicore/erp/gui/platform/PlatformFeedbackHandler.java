package mz.multicore.erp.gui.platform;

import mz.multicore.erp.gui.components.FeedbackType;

/**
 * Contrato de feedback visual para as sub-views da consola de plataforma.
 */
public interface PlatformFeedbackHandler {
    void showError(String area, Throwable error, Runnable retry);
    void showNotice(FeedbackType type, String title, String message);
    void showSuccess(String message);
}
