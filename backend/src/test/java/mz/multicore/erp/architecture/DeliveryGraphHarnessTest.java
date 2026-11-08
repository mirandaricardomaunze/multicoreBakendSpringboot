package mz.multicore.erp.architecture;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Permanent harness for the canonical delivery graph in docs/DELIVERY_GRAPH_SPEC.md. */
class DeliveryGraphHarnessTest {
    private static final Set<String> STATUSES = Set.of("TODO", "BLOCKED", "IN_PROGRESS", "VERIFIED", "DONE");
    private static final Set<String> PRIORITIES = Set.of("CRITICAL", "HIGH", "MEDIUM", "LOW");
    private static final Set<String> OWNERS = Set.of("contracts", "backend", "desktop", "platform", "external");
    private static final Set<String> COMPLETE_STATUSES = Set.of("VERIFIED", "DONE");
    private static final Path ROOT = Files.isDirectory(Path.of("tasks"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();
    private static final Path GRAPH = ROOT.resolve(Path.of("tasks", "graph.yaml"));

    @Test
    void deliveryGraph_isValidAcyclicAndReportable() throws Exception {
        DeliveryGraph graph = loadGraph();

        assertThat(graph.schemaVersion()).isEqualTo(1);
        assertThat(graph.objective()).isNotBlank();
        assertThat(graph.nodes()).isNotEmpty();

        Map<String, DeliveryNode> byId = new LinkedHashMap<>();
        for (DeliveryNode node : graph.nodes()) {
            assertThat(node.id()).matches("[A-Z][A-Z0-9-]*");
            assertThat(byId.put(node.id(), node)).as("duplicate node %s", node.id()).isNull();
            assertThat(node.title()).isNotBlank();
            assertThat(node.owner()).isIn(OWNERS);
            assertThat(node.status()).isIn(STATUSES);
            assertThat(node.priority()).isIn(PRIORITIES);
            assertThat(node.doneWhen()).as("doneWhen for %s", node.id()).isNotEmpty();
            assertThat(node.dependsOn()).doesNotContain(node.id());
            if ("BLOCKED".equals(node.status())) {
                assertThat(node.blockedBy()).as("blockedBy for %s", node.id()).isNotEmpty();
            } else {
                assertThat(node.blockedBy()).as("non-blocked node %s", node.id()).isEmpty();
            }
            if (COMPLETE_STATUSES.contains(node.status())) {
                assertThat(node.verification()).as("verification for %s", node.id()).isNotEmpty();
            }
        }

        graph.nodes().forEach(node -> assertThat(node.dependsOn())
                .as("dependencies for %s", node.id())
                .allMatch(byId::containsKey));
        assertWorkflowState(graph, byId);
        assertVerificationEvidenceExists(graph);
        assertAcyclic(byId);
        writeReport(graph, byId);
    }

    private static DeliveryGraph loadGraph() throws IOException {
        assertThat(GRAPH).isRegularFile();
        try (Reader reader = Files.newBufferedReader(GRAPH)) {
            Map<String, Object> root = new Yaml().load(reader);
            int schemaVersion = ((Number) root.get("schemaVersion")).intValue();
            String objective = String.valueOf(root.get("objective"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawNodes = (List<Map<String, Object>>) root.get("nodes");
            List<DeliveryNode> nodes = rawNodes.stream().map(DeliveryGraphHarnessTest::toNode).toList();
            return new DeliveryGraph(schemaVersion, objective, nodes);
        }
    }

    private static DeliveryNode toNode(Map<String, Object> raw) {
        return new DeliveryNode(
                text(raw, "id"), text(raw, "title"), text(raw, "owner"), text(raw, "status"),
                text(raw, "priority"), strings(raw, "dependsOn"), strings(raw, "blockedBy"),
                strings(raw, "verification"), strings(raw, "doneWhen"));
    }

    private static String text(Map<String, Object> raw, String key) {
        Object value = raw.get(key);
        assertThat(value).as("required field %s", key).isInstanceOf(String.class);
        return String.valueOf(value);
    }

    private static List<String> strings(Map<String, Object> raw, String key) {
        Object value = raw.get(key);
        assertThat(value).as("required list %s", key).isInstanceOf(Collection.class);
        return ((Collection<?>) value).stream().map(String::valueOf).toList();
    }

    private static void assertAcyclic(Map<String, DeliveryNode> byId) {
        Set<String> visited = new HashSet<>();
        Set<String> active = new HashSet<>();
        Deque<String> path = new ArrayDeque<>();
        for (String id : byId.keySet()) {
            visit(id, byId, visited, active, path);
        }
    }

    private static void assertWorkflowState(DeliveryGraph graph, Map<String, DeliveryNode> byId) {
        List<DeliveryNode> active = graph.nodes().stream()
                .filter(node -> "IN_PROGRESS".equals(node.status())).toList();
        assertThat(active).as("only one graph node may be IN_PROGRESS").hasSizeLessThanOrEqualTo(1);
        active.forEach(node -> assertThat(node.dependsOn().stream().map(byId::get)
                .allMatch(dependency -> COMPLETE_STATUSES.contains(dependency.status())))
                .as("IN_PROGRESS node %s must have complete dependencies", node.id()).isTrue());
    }

    private static void assertVerificationEvidenceExists(DeliveryGraph graph) throws IOException {
        Set<String> testClasses;
        try (var paths = Files.walk(ROOT)) {
            testClasses = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("src" + java.io.File.separator + "test"))
                    .map(path -> path.getFileName().toString().replaceFirst("\\.java$", ""))
                    .collect(Collectors.toSet());
        }

        graph.nodes().stream()
                .filter(node -> COMPLETE_STATUSES.contains(node.status()))
                .forEach(node -> node.verification().forEach(evidence -> {
                    boolean exists = evidence.contains("/")
                            ? Files.exists(ROOT.resolve(evidence))
                            : testClasses.contains(evidence);
                    assertThat(exists)
                            .as("verification evidence '%s' for %s must exist", evidence, node.id())
                            .isTrue();
                }));
    }

    private static void visit(String id, Map<String, DeliveryNode> byId, Set<String> visited,
                              Set<String> active, Deque<String> path) {
        if (visited.contains(id)) {
            return;
        }
        assertThat(active.add(id)).as("dependency cycle %s -> %s", path, id).isTrue();
        path.addLast(id);
        for (String dependency : byId.get(id).dependsOn()) {
            visit(dependency, byId, visited, active, path);
        }
        path.removeLast();
        active.remove(id);
        visited.add(id);
    }

    private static void writeReport(DeliveryGraph graph, Map<String, DeliveryNode> byId) throws IOException {
        Map<String, Long> statusCounts = graph.nodes().stream().collect(Collectors.groupingBy(
                DeliveryNode::status, LinkedHashMap::new, Collectors.counting()));
        List<DeliveryNode> executable = graph.nodes().stream()
                .filter(node -> "TODO".equals(node.status()))
                .filter(node -> node.dependsOn().stream()
                        .map(byId::get).allMatch(dependency -> COMPLETE_STATUSES.contains(dependency.status())))
                .sorted((left, right) -> priorityRank(left.priority()) - priorityRank(right.priority()))
                .toList();
        List<DeliveryNode> blocked = graph.nodes().stream()
                .filter(node -> "BLOCKED".equals(node.status())).toList();

        List<String> report = new ArrayList<>();
        report.add("# Delivery Graph Report");
        report.add("");
        report.add("Objective: " + graph.objective());
        report.add("");
        report.add("## Status summary");
        report.add("");
        STATUSES.forEach(status -> report.add("- " + status + ": " + statusCounts.getOrDefault(status, 0L)));
        report.add("");
        report.add("## Executable now");
        report.add("");
        if (executable.isEmpty()) {
            report.add("- None");
        } else {
            executable.forEach(node -> report.add("- **" + node.id() + "** — " + node.title()));
        }
        report.add("");
        report.add("## Blocked");
        report.add("");
        blocked.forEach(node -> report.add("- **" + node.id() + "** — "
                + String.join("; ", node.blockedBy())));
        report.add("");
        report.add("## Critical open path");
        report.add("");
        graph.nodes().stream()
                .filter(node -> "CRITICAL".equals(node.priority()))
                .filter(node -> !COMPLETE_STATUSES.contains(node.status()))
                .forEach(node -> report.add("- **" + node.id() + "** (`" + node.status() + "`) — " + node.title()));

        Path output = ROOT.resolve(Path.of("backend", "target", "delivery-graph-report.md"));
        Files.createDirectories(output.getParent());
        Files.write(output, report);
    }

    private static int priorityRank(String priority) {
        return List.of("CRITICAL", "HIGH", "MEDIUM", "LOW").indexOf(priority);
    }

    private record DeliveryGraph(int schemaVersion, String objective, List<DeliveryNode> nodes) {
    }

    private record DeliveryNode(String id, String title, String owner, String status, String priority,
                                List<String> dependsOn, List<String> blockedBy,
                                List<String> verification, List<String> doneWhen) {
    }
}

