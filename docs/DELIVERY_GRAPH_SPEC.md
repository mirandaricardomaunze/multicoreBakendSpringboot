# Delivery Graph Specification

Status: **canonical**  
Effective date: 2026-11-08

## Purpose

The delivery graph is the structured execution plan for taking Multicore ERP from its current
state to a production pilot. It exposes dependencies, external blockers, verification evidence,
and the shortest executable path without changing the physical `contracts`, `backend`, and
`desktop` architecture.

The canonical data source is [`tasks/graph.yaml`](../tasks/graph.yaml). `tasks/current.md` remains
the concise human-readable operational summary.

## Node model

Every node contains:

- `id`: stable uppercase identifier;
- `title`: short outcome-oriented description;
- `owner`: architectural owner (`contracts`, `backend`, `desktop`, `platform`, or `external`);
- `status`: `TODO`, `BLOCKED`, `IN_PROGRESS`, `VERIFIED`, or `DONE`;
- `priority`: `CRITICAL`, `HIGH`, `MEDIUM`, or `LOW`;
- `dependsOn`: graph nodes that must be complete first;
- `blockedBy`: concrete external or decision blockers;
- `verification`: tests, harnesses, commands, or signed evidence;
- `doneWhen`: objective completion conditions.

`VERIFIED` means implementation evidence exists but a wider production milestone may still be
open. `DONE` means no work remains for that node. A blocked node must name at least one blocker.

## Graph rules

1. Node identifiers are unique and dependencies reference existing nodes.
2. The dependency graph is acyclic.
3. A node cannot depend on itself.
4. `BLOCKED` nodes declare `blockedBy`; other states do not.
5. `VERIFIED` and `DONE` nodes contain verification evidence.
6. Every node has at least one objective `doneWhen` condition.
7. At most one node may be `IN_PROGRESS`, and all of its dependencies must be complete.
8. Verification evidence for completed nodes must resolve to a repository test class or file.
9. The graph records delivery dependencies only. It never authorizes forbidden code dependencies.
10. Business-rule uncertainty remains subject to the stop-and-ask rules in `AGENTS.md`.

## Execution policy

Work selection follows this order:

1. unblocked nodes whose dependencies are `VERIFIED` or `DONE`;
2. `CRITICAL` before `HIGH`, `MEDIUM`, and `LOW`;
3. nodes that unlock the largest number of downstream nodes;
4. the smallest independently verifiable vertical slice.

Each vertical slice should cover contract, backend, desktop, and verification only when those
layers are genuinely required. A layer must not be added merely to make the graph symmetrical.

## Automated verification and report

`DeliveryGraphHarnessTest` validates the canonical graph during the backend test phase and writes:

```text
backend/target/delivery-graph-report.md
```

The report contains status totals, currently executable work, blocked nodes, and the critical
delivery path. It is generated output and must not become a competing source of truth.

Run the gate with:

```powershell
mvn test -pl backend "-Dtest=DeliveryGraphHarnessTest"
```

