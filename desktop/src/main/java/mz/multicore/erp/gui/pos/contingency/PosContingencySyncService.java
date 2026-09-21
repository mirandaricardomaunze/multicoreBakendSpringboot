package mz.multicore.erp.gui.pos.contingency;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import javax.swing.*;
import java.awt.Component;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Serviço em segundo plano que monitoriza e sincroniza automaticamente as vendas em contingência do POS.
 */
@org.springframework.stereotype.Component
@Profile("desktop")
public class PosContingencySyncService {

    private static final Logger log = LoggerFactory.getLogger(PosContingencySyncService.class);

    private final PosContingencyManager contingencyManager;
    private final POSApiClient posApiClient;
    private final ScheduledExecutorService scheduler;

    public PosContingencySyncService(PosContingencyManager contingencyManager, POSApiClient posApiClient) {
        this.contingencyManager = contingencyManager;
        this.posApiClient = posApiClient;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pos-contingency-sync");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    public void start() {
        // Tenta sincronizar a cada 45 segundos se houver pendências
        scheduler.scheduleWithFixedDelay(this::runAutoSync, 15, 45, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
    }

    private void runAutoSync() {
        if (contingencyManager.getPendingCount() == 0) return;
        try {
            int synced = contingencyManager.syncPendingSales(posApiClient);
            if (synced > 0) {
                SwingUtilities.invokeLater(() -> {
                    ToastManager.show(null, FeedbackType.SUCCESS,
                            "Contingência POS: " + synced + " venda(s) sincronizada(s) com o servidor.");
                });
            }
        } catch (Exception e) {
            log.debug("Verificação de sincronização periódica de contingência falhou: {}", e.getMessage());
        }
    }

    /**
     * Sincronização manual accionada pelo operador com indicador visual de progresso.
     */
    public void triggerManualSync(Component parent, Runnable onComplete) {
        if (contingencyManager.getPendingCount() == 0) {
            ToastManager.show(parent, FeedbackType.INFO, "Não existem vendas pendentes de contingência.");
            if (onComplete != null) onComplete.run();
            return;
        }

        UIHelper.runWithProgress(parent, "A sincronizar vendas em contingência…",
                () -> contingencyManager.syncPendingSales(posApiClient),
                synced -> {
                    if (synced > 0) {
                        ToastManager.show(parent, FeedbackType.SUCCESS,
                                "Sincronização concluída: " + synced + " venda(s) enviada(s) com sucesso.");
                    } else if (contingencyManager.getPendingCount() > 0) {
                        ToastManager.show(parent, FeedbackType.WARNING,
                                "Não foi possível contactar o servidor. A fila continua protegida localmente.");
                    }
                    if (onComplete != null) onComplete.run();
                },
                ex -> {
                    ToastManager.show(parent, FeedbackType.ERROR,
                            "Erro na sincronização de contingência: " + ex.getMessage());
                    if (onComplete != null) onComplete.run();
                }
        );
    }
}
