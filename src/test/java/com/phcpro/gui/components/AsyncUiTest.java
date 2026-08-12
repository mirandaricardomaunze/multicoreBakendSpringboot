package com.phcpro.gui.components;

import com.phcpro.architecture.security.CurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncUiTest {

    @AfterEach
    void clearContext() throws Exception {
        CurrentUserContext.clear();
        SwingUtilities.invokeAndWait(CurrentUserContext::clear);
    }

    @Test
    void loadAsync_fetchForaDoEdt_callbackNoEdt() throws Exception {
        JPanel scope = new JPanel();
        AtomicBoolean fetchOnEdt = new AtomicBoolean(true);
        AtomicBoolean callbackOnEdt = new AtomicBoolean(false);
        CountDownLatch done = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentUser("ana", "ADMIN");
            CurrentUserContext.setCurrentCompanyId(7L);
            UIHelper.loadAsync(scope, () -> {
                fetchOnEdt.set(SwingUtilities.isEventDispatchThread());
                assertEquals(7L, CurrentUserContext.getCurrentCompanyId());
                return "ok";
            }, value -> {
                callbackOnEdt.set(SwingUtilities.isEventDispatchThread());
                done.countDown();
            });
        });

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertFalse(fetchOnEdt.get());
        assertTrue(callbackOnEdt.get());
        waitUntilLoaded(scope);
        assertEquals(Boolean.FALSE, scope.getClientProperty("loading"));
    }

    @Test
    void loadAsync_erro_entregaUmaVezELibertaLoading() throws Exception {
        JPanel scope = new JPanel();
        AtomicInteger errors = new AtomicInteger();
        CountDownLatch done = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> UIHelper.loadAsync(scope,
                () -> { throw new IllegalStateException("API indisponível"); },
                value -> { },
                error -> { errors.incrementAndGet(); done.countDown(); }));

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(1, errors.get());
        waitUntilLoaded(scope);
        assertEquals(Boolean.FALSE, scope.getClientProperty("loading"));
    }

    @Test
    void loadAsync_tenantMudou_ignoraRespostaAntiga() throws Exception {
        JPanel scope = new JPanel();
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);
        AtomicBoolean applied = new AtomicBoolean(false);

        SwingUtilities.invokeAndWait(() -> {
            CurrentUserContext.setCurrentCompanyId(1L);
            UIHelper.loadAsync(scope, () -> {
                fetchStarted.countDown();
                releaseFetch.await(5, TimeUnit.SECONDS);
                return "empresa 1";
            }, value -> applied.set(true));
        });
        assertTrue(fetchStarted.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> CurrentUserContext.setCurrentCompanyId(2L));
        releaseFetch.countDown();
        for (int i = 0; i < 50 && Boolean.TRUE.equals(scope.getClientProperty("loading")); i++) {
            Thread.sleep(20);
        }

        assertFalse(applied.get());
    }

    @Test
    void submitAsync_segundoClique_naoDuplica() throws Exception {
        JButton button = new JButton();
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();

        SwingUtilities.invokeAndWait(() -> {
            UIHelper.submitAsync(button, () -> { calls.incrementAndGet(); release.await(); return null; }, v -> { }, e -> { });
            assertNull(UIHelper.submitAsync(button, () -> { calls.incrementAndGet(); return null; }, v -> { }, e -> { }));
        });
        release.countDown();
        for (int i = 0; i < 50 && !button.isEnabled(); i++) Thread.sleep(20);

        assertEquals(1, calls.get());
        assertTrue(button.isEnabled());
    }

    private static void waitUntilLoaded(JPanel scope) throws InterruptedException {
        for (int i = 0; i < 50 && Boolean.TRUE.equals(scope.getClientProperty("loading")); i++) {
            Thread.sleep(20);
        }
    }
}
