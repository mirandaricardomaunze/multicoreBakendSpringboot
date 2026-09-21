package mz.multicore.erp.gui.pos.contingency;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.pos.dto.PosContingencyStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Gestor da fila local de contingência para vendas offline/resilientes do POS.
 * Persiste atomicamente em disco local garantindo tolerância a falhas.
 */
@Component
@Profile("desktop")
public class PosContingencyManager {

    private static final Logger log = LoggerFactory.getLogger(PosContingencyManager.class);
    private static final DateTimeFormatter REF_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path queueFile;
    private final ObjectMapper mapper;
    private final List<PosContingencySale> queue = new CopyOnWriteArrayList<>();
    private final List<Consumer<Integer>> listeners = new CopyOnWriteArrayList<>();

    public PosContingencyManager() {
        this(resolveDefaultQueuePath());
    }

    public PosContingencyManager(Path queueFile) {
        this.queueFile = queueFile;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        loadFromFile();
    }

    private static Path resolveDefaultQueuePath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path dir;
        if (localAppData != null && !localAppData.isBlank()) {
            dir = Paths.get(localAppData, "MulticoreERP", "data");
        } else {
            dir = Paths.get(System.getProperty("user.home"), ".multicore", "data");
        }
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            log.warn("Falha ao criar directório de dados de contingência: {}", e.getMessage());
        }
        return dir.resolve("pos_contingency_queue.json");
    }

    public synchronized void loadFromFile() {
        queue.clear();
        if (!Files.exists(queueFile)) return;
        try {
            byte[] bytes = Files.readAllBytes(queueFile);
            if (bytes.length > 0) {
                List<PosContingencySale> list = mapper.readValue(bytes, new TypeReference<>() {});
                if (list != null) {
                    queue.addAll(list);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao ler fila local de contingência: {}", e.getMessage(), e);
        }
        notifyListeners();
    }

    public synchronized void saveToFile() {
        try {
            if (queueFile.getParent() != null) {
                Files.createDirectories(queueFile.getParent());
            }
            Path tmp = queueFile.resolveSibling(queueFile.getFileName() + ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), queue);
            Files.move(tmp, queueFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            log.error("Erro ao gravar fila local de contingência: {}", e.getMessage(), e);
        }
        notifyListeners();
    }

    /**
     * Identifica se a falha é de conectividade ou indisponibilidade de servidor (candidata a contingência).
     */
    public boolean isConnectivityError(Throwable t) {
        if (t == null) return false;
        Throwable curr = t;
        while (curr != null) {
            if (curr instanceof ConnectException
                    || curr instanceof SocketTimeoutException
                    || curr instanceof UnknownHostException
                    || curr.getClass().getName().contains("HttpTimeoutException")
                    || curr.getClass().getName().contains("ResourceAccessException")) {
                return true;
            }
            String msg = curr.getMessage() != null ? curr.getMessage().toLowerCase() : "";
            if (msg.contains("connection refused")
                    || msg.contains("connect timed out")
                    || msg.contains("read timed out")
                    || msg.contains("failed to connect")
                    || msg.contains("network is unreachable")
                    || msg.contains("503")
                    || msg.contains("502")
                    || msg.contains("504")) {
                return true;
            }
            curr = curr.getCause();
        }
        return false;
    }

    /**
     * Regista uma venda na fila local de contingência e emite o talão provisório de imediato.
     */
    public PosContingencySale enqueue(POSCheckoutRequest request, BigDecimal cartTotal) {
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String ref = "CONT-" + LocalDateTime.now().format(REF_FORMAT) + "-" + suffix;

        POSCheckoutRequest requestWithRef = new POSCheckoutRequest(
                request.operator(),
                request.companyId(),
                request.clientId(),
                request.walkInName(),
                request.warehouseId(),
                request.treasuryAccountId(),
                request.lines(),
                request.payments(),
                ref
        );

        PosContingencySale sale = new PosContingencySale(
                ref,
                LocalDateTime.now(),
                requestWithRef,
                cartTotal,
                PosContingencyStatus.PENDING_SYNC,
                null,
                null
        );

        queue.add(sale);
        saveToFile();

        // Emissão do talão provisório de caixa
        PosThermalReceiptPrinter.printSilent(sale);
        log.info("Venda gravada em contingência: {} (Total: {} MT)", ref, cartTotal);
        return sale;
    }

    /**
     * Auxiliar que confirma com o operador e executa a transição para contingência se for falha de rede.
     */
    public boolean handleContingencyCheckout(java.awt.Component parent, POSCheckoutRequest request,
                                            BigDecimal cartTotal, Throwable ex, Runnable onEnqueued) {
        if (!isConnectivityError(ex)) return false;
        int opt = javax.swing.JOptionPane.showConfirmDialog(parent,
                "Falha de ligação com o servidor:\n" + ex.getMessage() + "\n\n"
                        + "Deseja emitir o talão em Regime de Contingência e gravar na fila local?",
                "Modo de Contingência — POS", javax.swing.JOptionPane.YES_NO_OPTION, javax.swing.JOptionPane.WARNING_MESSAGE);
        if (opt == javax.swing.JOptionPane.YES_OPTION) {
            PosContingencySale sale = enqueue(request, cartTotal);
            mz.multicore.erp.gui.components.ToastManager.show(parent,
                    mz.multicore.erp.gui.components.FeedbackType.WARNING,
                    "Venda em contingência registada (" + sale.contingencyReference() + "). O atendimento pode continuar.");
            if (onEnqueued != null) onEnqueued.run();
            return true;
        }
        return false;
    }

    /**
     * Sincroniza todas as vendas pendentes com o backend.
     * Devolve o total de vendas sincronizadas com sucesso nesta tentativa.
     */
    public synchronized int syncPendingSales(POSApiClient posApiClient) {
        int syncedCount = 0;
        boolean hadChanges = false;

        for (int i = 0; i < queue.size(); i++) {
            PosContingencySale sale = queue.get(i);
            if (sale.status() != PosContingencyStatus.PENDING_SYNC) {
                continue;
            }

            try {
                InvoiceDTO invoice = posApiClient.checkout(sale.request());
                PosContingencySale updated = sale.withStatus(
                        PosContingencyStatus.SYNCED,
                        invoice != null ? invoice.invoiceNumber() : "SYNCED",
                        null
                );
                queue.set(i, updated);
                syncedCount++;
                hadChanges = true;
                log.info("Venda em contingência sincronizada com sucesso: {} -> {}",
                        sale.contingencyReference(), updated.syncedInvoiceNumber());
            } catch (Exception ex) {
                if (isConnectivityError(ex)) {
                    log.warn("Servidor ainda indisponível ao tentar sincronizar contingência: {}", ex.getMessage());
                    break; // Interrompe até à próxima tentativa
                } else {
                    log.error("Erro semântico na sincronização da venda {}: {}", sale.contingencyReference(), ex.getMessage());
                    PosContingencySale updated = sale.withStatus(
                            PosContingencyStatus.REVISION_NEEDED,
                            null,
                            ex.getMessage()
                    );
                    queue.set(i, updated);
                    hadChanges = true;
                }
            }
        }

        if (hadChanges) {
            saveToFile();
        }
        return syncedCount;
    }

    public List<PosContingencySale> getAllSales() {
        return Collections.unmodifiableList(new ArrayList<>(queue));
    }

    public int getPendingCount() {
        int count = 0;
        for (PosContingencySale s : queue) {
            if (s.status() == PosContingencyStatus.PENDING_SYNC) {
                count++;
            }
        }
        return count;
    }

    public BigDecimal getPendingTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (PosContingencySale s : queue) {
            if (s.status() == PosContingencyStatus.PENDING_SYNC && s.cartTotal() != null) {
                total = total.add(s.cartTotal());
            }
        }
        return total;
    }

    public void addListener(Consumer<Integer> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<Integer> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        int pending = getPendingCount();
        for (Consumer<Integer> l : listeners) {
            try {
                l.accept(pending);
            } catch (Exception ignored) {}
        }
    }
}
