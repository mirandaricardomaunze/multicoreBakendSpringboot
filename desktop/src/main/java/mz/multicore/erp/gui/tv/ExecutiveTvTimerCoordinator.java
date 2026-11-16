package mz.multicore.erp.gui.tv;

import javax.swing.Timer;
import java.time.LocalDateTime;

/**
 * Coordenador dos temporizadores de atualização do painel de TV (Relógio, Auto-refresh e Rodízio).
 */
public class ExecutiveTvTimerCoordinator {

    public static final int SLIDESHOW_DURATION_SECONDS = 15;
    public interface TimerListener {
        void onClockTick(LocalDateTime now);
        void onAutoRefresh();
        void onSlideAdvance();
        void onCountdownTick(int secondsRemaining);
    }

    private final TimerListener listener;
    private Timer clockTimer;
    private Timer autoRefreshTimer;
    private Timer slideshowTimer;

    private boolean isSlideshowActive = false;
    private int secondsToNextSlide = SLIDESHOW_DURATION_SECONDS;

    public ExecutiveTvTimerCoordinator(TimerListener listener) {
        this.listener = listener;
    }

    public void start() {
        clockTimer = new Timer(1000, e -> {
            if (listener != null) listener.onClockTick(LocalDateTime.now());
        });
        clockTimer.start();
        if (listener != null) listener.onClockTick(LocalDateTime.now());

        autoRefreshTimer = new Timer(45000, e -> {
            if (listener != null) listener.onAutoRefresh();
        });
        autoRefreshTimer.start();

        slideshowTimer = new Timer(1000, e -> {
            if (!isSlideshowActive) return;
            secondsToNextSlide--;
            if (secondsToNextSlide <= 0) {
                secondsToNextSlide = SLIDESHOW_DURATION_SECONDS;
                if (listener != null) listener.onSlideAdvance();
            }
            if (listener != null) listener.onCountdownTick(secondsToNextSlide);
        });
    }

    public void stop() {
        if (clockTimer != null) clockTimer.stop();
        if (autoRefreshTimer != null) autoRefreshTimer.stop();
        if (slideshowTimer != null) slideshowTimer.stop();
    }

    public boolean toggleSlideshow() {
        isSlideshowActive = !isSlideshowActive;
        if (isSlideshowActive) {
            secondsToNextSlide = SLIDESHOW_DURATION_SECONDS;
            slideshowTimer.start();
        } else {
            slideshowTimer.stop();
        }
        return isSlideshowActive;
    }

    public boolean isSlideshowActive() {
        return isSlideshowActive;
    }

    public int getSecondsToNextSlide() {
        return secondsToNextSlide;
    }
}
