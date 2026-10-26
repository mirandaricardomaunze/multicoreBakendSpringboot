package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine;
import mz.multicore.erp.modules.pos.dto.*;

import javax.swing.*;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coordenador assíncrono de requisições Push USSD, timers de contagem decrescente e polling de transações.
 */
public final class MobilePaymentWorkflowCoordinator {

    public interface Listener {
        void onWaitingStarted(String message); void onCountdownTick(int remainingSeconds);
        void onSuccess(String financialRef); void onFailed(String message);
        void onTimeout(); void onError(String error); void onCancelled();
    }

    private final POSApiClient apiClient;
    private final Listener listener;
    private Timer countdownTimer;
    private Timer pollTimer;
    private String activeTransactionId;
    private int remainingSeconds = 60;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public MobilePaymentWorkflowCoordinator(POSApiClient apiClient, Listener listener) {
        this.apiClient = apiClient;
        this.listener = listener;
    }

    public void startPayment(MobilePaymentProvider provider, String phone, BigDecimal amount) {
        if (isProcessing.get()) return;
        isProcessing.set(true);
        Long companyId = CurrentUserContext.findCurrentCompanyId() != null ? CurrentUserContext.findCurrentCompanyId() : 1L;
        String operator = CurrentUserContext.getUsername();
        remainingSeconds = 60;
        listener.onWaitingStarted("A enviar pedido USSD ao telemóvel...");

        new SwingWorker<MobilePaymentResponse, Void>() {
            @Override
            protected MobilePaymentResponse doInBackground() throws Exception {
                return apiClient.initiateMobilePayment(new InitiateMobilePaymentRequest(
                        provider, phone, amount, "POS-" + System.currentTimeMillis(), companyId, operator));
            }
            @Override
            protected void done() {
                try {
                    activeTransactionId = get().transactionId(); startTimers();
                } catch (Exception ex) {
                    isProcessing.set(false);
                    PosAudioFeedbackEngine.getInstance().playAsync(PosAudioFeedbackEngine.SoundEvent.ERROR);
                    listener.onError(ex.getMessage());
                }
            }
        }.execute();
    }

    private void startTimers() {
        countdownTimer = new Timer(1000, e -> {
            remainingSeconds--; listener.onCountdownTick(remainingSeconds);
            if (remainingSeconds <= 0) {
                stopTimers(); isProcessing.set(false);
                PosAudioFeedbackEngine.getInstance().playAsync(PosAudioFeedbackEngine.SoundEvent.ERROR);
                listener.onTimeout();
            }
        });
        countdownTimer.start();
        pollTimer = new Timer(2000, e -> pollStatus());
        pollTimer.start();
    }

    private void pollStatus() {
        if (activeTransactionId == null) return;
        Long companyId = CurrentUserContext.findCurrentCompanyId() != null ? CurrentUserContext.findCurrentCompanyId() : 1L;

        new SwingWorker<MobilePaymentStatusResponse, Void>() {
            @Override
            protected MobilePaymentStatusResponse doInBackground() {
                try { return apiClient.getMobilePaymentStatus(activeTransactionId, companyId); } catch (Exception ex) { return null; }
            }
            @Override
            protected void done() {
                try {
                    MobilePaymentStatusResponse st = get();
                    if (st != null && st.status() == MobilePaymentStatus.SUCCESS) {
                        stopTimers();
                        PosAudioFeedbackEngine.getInstance().playAsync(PosAudioFeedbackEngine.SoundEvent.SUCCESS);
                        listener.onSuccess(st.financialReference());
                    } else if (st != null && (st.status() == MobilePaymentStatus.FAILED || st.status() == MobilePaymentStatus.CANCELLED)) {
                        stopTimers(); isProcessing.set(false);
                        PosAudioFeedbackEngine.getInstance().playAsync(PosAudioFeedbackEngine.SoundEvent.ERROR);
                        listener.onFailed(st.message());
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void forceSimulateComplete(boolean approve) {
        if (activeTransactionId == null) return;
        Long companyId = CurrentUserContext.findCurrentCompanyId() != null ? CurrentUserContext.findCurrentCompanyId() : 1L;

        new SwingWorker<MobilePaymentStatusResponse, Void>() {
            @Override
            protected MobilePaymentStatusResponse doInBackground() throws Exception {
                return apiClient.simulateCompleteMobilePayment(activeTransactionId, companyId, approve);
            }
            @Override
            protected void done() {
                try {
                    MobilePaymentStatusResponse st = get();
                    if (st != null && st.status() == MobilePaymentStatus.SUCCESS) {
                        stopTimers();
                        PosAudioFeedbackEngine.getInstance().playAsync(PosAudioFeedbackEngine.SoundEvent.SUCCESS);
                        listener.onSuccess(st.financialReference());
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void cancelActiveTransaction() {
        stopTimers(); isProcessing.set(false); listener.onCancelled();
    }

    public void stopTimers() {
        if (countdownTimer != null) countdownTimer.stop();
        if (pollTimer != null) pollTimer.stop();
    }
}
