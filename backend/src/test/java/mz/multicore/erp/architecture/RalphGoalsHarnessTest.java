package mz.multicore.erp.architecture;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Permanent harness for the Ralph loop goal queue (docs/RALPH_LOOP_SPEC.md, RL-01..RL-10). */
class RalphGoalsHarnessTest {
    private static final Set<String> STATUSES = Set.of("TODO", "IN_PROGRESS", "BLOCKED", "DONE");
    private static final Set<String> PRIORITIES = Set.of("CRITICAL", "HIGH", "MEDIUM", "LOW");
    private static final Pattern FILE_NAME = Pattern.compile("(GOAL-\\d{3})-[a-z0-9-]+\\.md");
    private static final Pattern CRITERION = Pattern.compile("^\\s*- \\[([ xX])] ([A-Z][A-Z0-9]*(?:-[A-Z0-9]+)*)\\b.*$");
    private static final Path ROOT = Files.isDirectory(Path.of("tasks"))
            ? Path.of("").toAbsolutePath().normalize()
            : Path.of("..").toAbsolutePath().normalize();
    private static final Path GOALS = ROOT.resolve(Path.of("tasks", "goals"));

    @Test
    void goalQueue_followsRalphLoopSpec() throws IOException {
        assertThat(GOALS.resolve("_TEMPLATE.md")).as("goal template").isRegularFile();
        assertThat(ROOT.resolve(Path.of("docs", "RALPH_LOOP_SPEC.md"))).isRegularFile();

        List<Goal> goals = loadGoals();
        Set<String> ids = new HashSet<>();
        Set<String> graphIds = graphNodeIds();

        for (Goal goal : goals) {
            String where = goal.file().getFileName().toString();
            // RL-01
            assertThat(goal.id()).as("id in %s", where).isEqualTo(goal.fileId());
            assertThat(ids.add(goal.id())).as("duplicate goal id %s", goal.id()).isTrue();
            assertThat(goal.title()).as("title in %s", where).isNotBlank();
            // RL-02
            assertThat(goal.status()).as("status in %s", where).isIn(STATUSES);
            assertThat(goal.priority()).as("priority in %s", where).isIn(PRIORITIES);
            // RL-03
            assertThat(goal.spec()).as("spec in %s", where).isNotBlank();
            assertThat(ROOT.resolve(goal.spec())).as("spec file of %s", goal.id()).isRegularFile();
            // RL-04
            if ("BLOCKED".equals(goal.status())) {
                assertThat(goal.blockedBy()).as("blockedBy for %s", goal.id()).isNotEmpty();
            } else {
                assertThat(goal.blockedBy()).as("non-blocked goal %s", goal.id()).isEmpty();
            }
            // RL-05
            assertThat(goal.criteria()).as("acceptance criteria in %s", where).isNotEmpty();
            // RL-06
            if ("DONE".equals(goal.status())) {
                assertThat(goal.criteria()).as("open criteria in DONE goal %s", goal.id())
                        .allMatch(Criterion::done);
                assertThat(goal.verify()).as("verify for DONE goal %s", goal.id()).isNotEmpty();
                goal.verify().forEach(test -> assertThat(evidenceExists(test))
                        .as("verification evidence %s for %s", test, goal.id()).isTrue());
            }
            // RL-08
            if (goal.graphNode() != null) {
                assertThat(graphIds).as("graphNode of %s", goal.id()).contains(goal.graphNode());
            }
            assertSpecTraceability(goal);
        }
        // RL-07
        assertThat(goals.stream().filter(g -> "IN_PROGRESS".equals(g.status())).count())
                .as("goals IN_PROGRESS").isLessThanOrEqualTo(1);
    }

    /**
     * RL-09 and RL-10: every acceptance criterion is traceable to the harness table, and the harness
     * points back to the goal's spec. A TODO goal may still lack its harness file, because the first
     * iteration is allowed to write it; any goal that has started must have it.
     */
    private static void assertSpecTraceability(Goal goal) throws IOException {
        assertThat(goal.harness()).as("harness in %s", goal.id()).isNotBlank();
        Path harness = ROOT.resolve(goal.harness());
        if (!Files.isRegularFile(harness)) {
            assertThat(goal.status()).as("harness file %s missing for started goal %s", goal.harness(), goal.id())
                    .isEqualTo("TODO");
            return;
        }
        String harnessText = Files.readString(harness, StandardCharsets.UTF_8);
        // RL-10
        String specName = Path.of(goal.spec()).getFileName().toString();
        assertThat(harnessText).as("harness %s must reference spec %s", goal.harness(), specName)
                .contains(specName);
        // RL-09
        List<String> missing = goal.criteria().stream()
                .map(Criterion::id)
                .filter(id -> !mentionsCriterion(harnessText, id))
                .toList();
        assertThat(missing).as("criteria of %s absent from harness %s", goal.id(), goal.harness()).isEmpty();
    }

    /** Matches "SA-06" exactly, and also grouped rows such as "SA-UI-05/06" for "SA-UI-06". */
    private static boolean mentionsCriterion(String text, String id) {
        if (Pattern.compile("(?<![A-Z0-9-])" + Pattern.quote(id) + "(?![A-Za-z0-9-])").matcher(text).find()) {
            return true;
        }
        Matcher numbered = Pattern.compile("^(.*-)(\\d+)$").matcher(id);
        if (!numbered.matches()) {
            return false;
        }
        String prefix = Pattern.quote(numbered.group(1));
        String number = numbered.group(2);
        return Pattern.compile("(?<![A-Z0-9-])" + prefix + "(?:\\d+/)*" + number + "(?:/\\d+)*(?![A-Za-z0-9-])")
                .matcher(text).find();
    }

    private static List<Goal> loadGoals() throws IOException {
        if (!Files.isDirectory(GOALS)) {
            return List.of();
        }
        List<Goal> goals = new ArrayList<>();
        try (Stream<Path> files = Files.list(GOALS)) {
            for (Path file : files.filter(p -> p.getFileName().toString().startsWith("GOAL-")).sorted().toList()) {
                Matcher name = FILE_NAME.matcher(file.getFileName().toString());
                assertThat(name.matches()).as("goal file name %s (GOAL-NNN-slug.md)", file.getFileName()).isTrue();
                goals.add(parse(file, name.group(1)));
            }
        }
        return goals;
    }

    @SuppressWarnings("unchecked")
    private static Goal parse(Path file, String fileId) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertThat(lines).as("frontmatter in %s", file.getFileName()).isNotEmpty();
        assertThat(lines.get(0).trim()).as("frontmatter start in %s", file.getFileName()).isEqualTo("---");
        int end = -1;
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).trim().equals("---")) {
                end = i;
                break;
            }
        }
        assertThat(end).as("frontmatter end in %s", file.getFileName()).isPositive();
        Map<String, Object> front = new Yaml().load(String.join("\n", lines.subList(1, end)));
        assertThat(front).as("frontmatter content in %s", file.getFileName()).isNotNull();

        List<Criterion> criteria = new ArrayList<>();
        for (String line : lines.subList(end + 1, lines.size())) {
            Matcher m = CRITERION.matcher(line);
            if (m.matches()) {
                criteria.add(new Criterion(m.group(2), !m.group(1).isBlank()));
            }
        }
        Object graphNode = front.get("graphNode");
        return new Goal(file, fileId,
                text(front.get("id")), text(front.get("title")), text(front.get("status")),
                text(front.get("priority")), text(front.get("spec")), text(front.get("harness")),
                strings(front.get("verify")), strings(front.get("blockedBy")),
                graphNode == null ? null : graphNode.toString(), criteria);
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Set<String> graphNodeIds() throws IOException {
        Path graph = ROOT.resolve(Path.of("tasks", "graph.yaml"));
        if (!Files.isRegularFile(graph)) {
            return Set.of();
        }
        try (Reader reader = Files.newBufferedReader(graph)) {
            Map<String, Object> root = new Yaml().load(reader);
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) root.get("nodes");
            return nodes.stream().map(n -> String.valueOf(n.get("id"))).collect(Collectors.toSet());
        }
    }

    private static boolean evidenceExists(String evidence) {
        if (evidence.contains("/") || evidence.contains("\\")) {
            return Files.exists(ROOT.resolve(evidence));
        }
        String fileName = evidence + ".java";
        for (String module : List.of("backend", "desktop", "contracts")) {
            Path tests = ROOT.resolve(Path.of(module, "src", "test", "java"));
            if (!Files.isDirectory(tests)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(tests)) {
                if (walk.anyMatch(p -> p.getFileName().toString().equals(fileName))) {
                    return true;
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot scan " + tests, e);
            }
        }
        return false;
    }

    private record Criterion(String id, boolean done) {
    }

    private record Goal(Path file, String fileId, String id, String title, String status, String priority,
                        String spec, String harness, List<String> verify, List<String> blockedBy, String graphNode,
                        List<Criterion> criteria) {
    }
}
