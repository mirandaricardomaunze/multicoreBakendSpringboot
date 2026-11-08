# Code Comment Language Specification

Status: **canonical**  
Effective date: 2026-11-08

## Purpose

The codebase uses professional English for all source-code comments and Javadocs. This keeps the
technical documentation consistent across `contracts`, `backend`, and `desktop`, while preserving
Portuguese from Mozambique for user-facing communication.

## Rules

1. Javadocs, line comments, block comments, test comments, SQL comments, XML comments, and script
   comments that explain code must be written in professional English.
2. Comments explain intent, constraints, trade-offs, non-obvious business rules, or safety
   requirements. They must not narrate straightforward code.
3. Official names, legal expressions, and exact user-interface text may remain in Portuguese when
   quoted as domain data. The surrounding explanation remains in English.
4. User-facing labels, validation messages, errors, reports, and document content remain in
   Portuguese from Mozambique. They are functional data, not source-code comments.
5. Existing Portuguese comments are translated when they preserve useful knowledge. Redundant,
   obsolete, or misleading comments are removed instead of translated.
6. A comment-only migration must not change executable code, public contracts, persistence
   mappings, or business behaviour.

## Professional standard

A professional comment is concise, grammatically complete where appropriate, and specific about
why the implementation exists. It avoids conversational phrasing, personal attribution, issue
history, temporary speculation, and unsupported claims.

```java
// Keep the original invoice number because credit notes must remain traceable to the fiscal source.
```

The following comment is redundant and should be removed:

```java
// Saves the invoice.
invoiceRepository.save(invoice);
```

## Migration and verification

The legacy migration is performed domain by domain so that fiscal, accounting, stock, payroll,
security, and audit explanations are not damaged by blind word substitution. Each batch must:

1. change comments only;
2. review the diff for accidental modifications to strings or executable code;
3. compile the affected module, followed by `mvn clean compile` when practical; and
4. run domain tests when a comment review exposes and corrects a misleading description of
   behaviour.

The migration is complete when no Portuguese prose remains in source-code comments, except quoted
official terms or user-facing text explicitly identified as such.
