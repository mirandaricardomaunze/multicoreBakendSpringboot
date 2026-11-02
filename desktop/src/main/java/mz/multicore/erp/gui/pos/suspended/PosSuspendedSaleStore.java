package mz.multicore.erp.gui.pos.suspended;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Vendas em espera deste posto, gravadas atomicamente para sobreviverem a um reinício. */
public class PosSuspendedSaleStore {

    private static final Logger log = LoggerFactory.getLogger(PosSuspendedSaleStore.class);

    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final List<PosSuspendedSale> sales = new ArrayList<>();

    public PosSuspendedSaleStore() {
        this(defaultFile());
    }

    public PosSuspendedSaleStore(Path file) {
        this.file = file;
        load();
    }

    private static Path defaultFile() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path dir = localAppData != null && !localAppData.isBlank()
                ? Paths.get(localAppData, "MulticoreERP", "data")
                : Paths.get(System.getProperty("user.home"), ".multicore", "data");
        return dir.resolve("pos_suspended_sales.json");
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            List<PosSuspendedSale> stored = mapper.readValue(file.toFile(), new TypeReference<>() {});
            if (stored != null) sales.addAll(stored);
        } catch (IOException e) {
            // Põe o ficheiro de lado: a próxima gravação não pode apagar vendas que ainda se recuperam.
            log.error("Ficheiro de vendas em espera ilegível: {}", e.getMessage(), e);
            try {
                Files.move(file, file.resolveSibling(file.getFileName() + ".corrupt"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveError) {
                log.error("Não foi possível isolar o ficheiro de vendas em espera: {}", moveError.getMessage());
            }
        }
    }

    public synchronized void add(PosSuspendedSale sale) {
        sales.add(sale);
        try {
            save();
        } catch (UncheckedIOException e) {
            sales.remove(sale);
            throw e;
        }
    }

    public synchronized void remove(String id) {
        if (sales.removeIf(sale -> sale.id().equals(id))) save();
    }

    public synchronized List<PosSuspendedSale> forCompany(Long companyId) {
        return sales.stream().filter(sale -> Objects.equals(sale.companyId(), companyId)).toList();
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), sales);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível gravar a venda em espera neste posto.", e);
        }
    }
}
