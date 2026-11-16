package mz.multicore.erp.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module decoupling (docs/MODULE_DECOUPLING_SPEC.md). Two levels of enforcement, both read from the
 * goal files so the loop cannot claim progress it has not made:
 * <ul>
 *   <li>a migration criterion is enforced as soon as it is ticked ({@code - [x] MD-C02});</li>
 *   <li>a whole wave is enforced as soon as its goal is DONE.</li>
 * </ul>
 * Progress is written to backend/target/module-decoupling-report.md.
 */
class ModuleDecouplingHarnessTest {
    private static final Path ROOT = CrossModuleRepositoryScanner.ROOT;
    private static final Pattern TICKED = Pattern.compile("^\\s*- \\[[xX]] (MD-[A-Z]\\d{2})\\b");
    private static final Pattern CONSUMER_MODULE = Pattern.compile("/erp/(?:modules/)?([a-z0-9]+)/");
    private static final Predicate<String> ANY = s -> true;

    private record Wave(String goalId, String scope, Predicate<String> target) {
    }

    /** A ticked criterion: links whose target AND consumer module match must be gone. */
    private record Criterion(String scope, Predicate<String> target, Predicate<String> consumer) {
    }

    private static final Set<String> PRODUCT = Set.of(
            "comercial.repository.ProductRepository", "comercial.repository.ProductCategoryRepository");

    private static final List<Wave> WAVES = List.of(
            new Wave("GOAL-001", "company.repository.*", t -> t.startsWith("company.")),
            new Wave("GOAL-002", "Product/ProductCategory", PRODUCT::contains),
            new Wave("GOAL-003", "comercial.repository.*", t -> t.startsWith("comercial.")),
            new Wave("GOAL-004", "inventory.repository.*", t -> t.startsWith("inventory.")),
            new Wave("GOAL-005", "todos os modulos", ANY));

    private static final Map<String, Criterion> CRITERIA = Map.ofEntries(
            // GOAL-001 company
            Map.entry("MD-C02", company("hr")),
            Map.entry("MD-C03", company("inventory", "comercial")),
            Map.entry("MD-C04", company("purchases", "platform", "fleet", "users")),
            Map.entry("MD-C05", company("manufacturing", "crm", "assets", "architecture", "support", "subscription",
                    "recurring", "promotions", "pos", "performance", "intelligence", "backup", "approvals")),
            Map.entry("MD-C06", company("weighbridge", "variants", "units", "returnables", "projects", "printing",
                    "pricing", "numbering", "monitoring", "fiscal", "expiry", "butchery", "bulkstock", "advances",
                    "activity", "accounting")),
            // GOAL-002 product
            Map.entry("MD-P02", product("inventory")),
            Map.entry("MD-P03", product("purchases", "crm", "architecture", "promotions", "pricing", "pos",
                    "manufacturing", "backup")),
            Map.entry("MD-P04", product("weighbridge", "variants", "units", "returnables", "expiry", "consignment",
                    "butchery", "bulkstock", "advances")),
            // GOAL-003 commercial documents
            Map.entry("MD-V01", repos("ClientRepository", "comercial.repository.ClientRepository")),
            Map.entry("MD-V02", new Criterion("InvoiceRepository em inventory/printing",
                    "comercial.repository.InvoiceRepository"::equals, Set.of("inventory", "printing")::contains)),
            Map.entry("MD-V03", repos("InvoiceRepository", "comercial.repository.InvoiceRepository")),
            Map.entry("MD-V04", repos("Order/Quotation/DeliveryGuide", "comercial.repository.OrderRepository",
                    "comercial.repository.QuotationRepository", "comercial.repository.DeliveryGuideRepository")),
            Map.entry("MD-V05", repos("Receipt/CreditNote/DebitNote", "comercial.repository.ReceiptRepository",
                    "comercial.repository.CreditNoteRepository", "comercial.repository.DebitNoteRepository")),
            // GOAL-004 inventory
            Map.entry("MD-I01", repos("WarehouseRepository", "inventory.repository.WarehouseRepository")),
            Map.entry("MD-I03", repos("StockRepository", "inventory.repository.StockRepository")),
            Map.entry("MD-I04", repos("Waste/Transfer/Batch/Movement", "inventory.repository.StockWasteRepository",
                    "inventory.repository.StockTransferRepository", "inventory.repository.ProductBatchRepository",
                    "inventory.repository.StockMovementRepository")),
            // GOAL-005 remaining owners
            Map.entry("MD-R01", owner("users")),
            Map.entry("MD-R02", owner("hr")),
            Map.entry("MD-R03", owner("purchases")),
            Map.entry("MD-R04", owner("pos")),
            Map.entry("MD-R05", owner("financeira")),
            Map.entry("MD-R06", owner("fleet", "fiscal", "approvals", "manufacturing", "assets", "recurring",
                    "performance", "audit", "quality", "crm")));

    @Test
    void tickedCriteriaAndDoneWavesHaveNoCrossModuleRepositoryAccess() throws IOException {
        Set<String> links = CrossModuleRepositoryScanner.links();
        StringBuilder report = new StringBuilder("# Module decoupling progress\n\n")
                .append("| Goal | Scope | Status | Remaining links |\n|---|---|---|---|\n");

        for (Wave wave : WAVES) {
            Optional<Path> goalFile = goalFile(wave.goalId());
            List<String> lines = goalFile.isPresent()
                    ? Files.readAllLines(goalFile.get(), StandardCharsets.UTF_8) : List.of();
            String status = lines.stream().filter(l -> l.startsWith("status:"))
                    .map(l -> l.substring("status:".length()).trim()).findFirst().orElse("absent");
            List<String> remaining = matching(links, wave.target(), ANY);
            report.append("| ").append(wave.goalId()).append(" | ").append(wave.scope()).append(" | ")
                    .append(status).append(" | ").append(remaining.size()).append(" |\n");

            for (String id : ticked(lines)) {
                Criterion criterion = CRITERIA.get(id);
                if (criterion == null) {
                    continue; // creation criteria (Lookups) are proven by their own unit tests
                }
                assertThat(matching(links, criterion.target(), criterion.consumer()))
                        .as("%s is ticked in %s but %s is still read across modules (use the owner's Lookup/Service)",
                                id, wave.goalId(), criterion.scope())
                        .isEmpty();
            }
            if ("DONE".equals(status)) {
                assertThat(remaining)
                        .as("%s is DONE but %s is still read from other modules (use the owner's Lookup/Service)",
                                wave.goalId(), wave.scope())
                        .isEmpty();
            }
        }
        Path out = ROOT.resolve(Path.of("backend", "target", "module-decoupling-report.md"));
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString(), StandardCharsets.UTF_8);
    }

    private static Criterion company(String... consumers) {
        return new Criterion("company.repository.* em " + String.join(", ", consumers),
                t -> t.startsWith("company."), Set.of(consumers)::contains);
    }

    private static Criterion product(String... consumers) {
        return new Criterion("Product/ProductCategory em " + String.join(", ", consumers),
                PRODUCT::contains, Set.of(consumers)::contains);
    }

    private static Criterion repos(String scope, String... targets) {
        return new Criterion(scope, Set.of(targets)::contains, ANY);
    }

    private static Criterion owner(String... owners) {
        Set<String> prefixes = Stream.of(owners).map(o -> o + ".").collect(Collectors.toSet());
        return new Criterion(String.join(", ", owners) + ".repository.*",
                t -> prefixes.stream().anyMatch(t::startsWith), ANY);
    }

    private static List<String> matching(Set<String> links, Predicate<String> target, Predicate<String> consumer) {
        return links.stream()
                .filter(l -> target.test(CrossModuleRepositoryScanner.target(l)))
                .filter(l -> consumer.test(consumerModule(l)))
                .toList();
    }

    private static String consumerModule(String link) {
        Matcher m = CONSUMER_MODULE.matcher(link.substring(0, link.indexOf(" -> ")));
        return m.find() ? m.group(1) : "";
    }

    private static List<String> ticked(List<String> lines) {
        return lines.stream().map(TICKED::matcher).filter(Matcher::find).map(m -> m.group(1)).toList();
    }

    private static Optional<Path> goalFile(String goalId) throws IOException {
        Path goals = ROOT.resolve(Path.of("tasks", "goals"));
        if (!Files.isDirectory(goals)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(goals)) {
            return files.filter(p -> p.getFileName().toString().startsWith(goalId + "-")).findFirst();
        }
    }
}
