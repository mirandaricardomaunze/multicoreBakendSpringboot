-- Centro de custo no lançamento manual e na despesa; linhas antigas (docs/CENTROS_CUSTO_ORCAMENTO_SPEC.md §3.2, CC-06).
--
-- A despesa do colaborador guarda o centro escolhido (vazio = centro por omissão), que segue para o lançamento do
-- reembolso. As partidas já gravadas em contas das classes 6 e 7 sem centro passam para o centro por omissão da
-- empresa do lançamento, para o resultado por centro bater com a demonstração de resultados desde o primeiro dia.
ALTER TABLE expense_claims ADD COLUMN IF NOT EXISTS cost_center_id BIGINT REFERENCES cost_centers(id);

CREATE INDEX IF NOT EXISTS idx_expense_claims_cost_center ON expense_claims (cost_center_id);

UPDATE journal_lines
SET cost_center_id = (
    SELECT cc.id
    FROM journal_entries je
    JOIN cost_centers cc ON cc.company_id = je.company_id AND cc.default_center = TRUE
    WHERE je.id = journal_lines.entry_id
)
WHERE cost_center_id IS NULL
  AND EXISTS (
      SELECT 1 FROM accounts a
      WHERE a.id = journal_lines.account_id
        AND SUBSTRING(a.code FROM 1 FOR 1) IN ('6', '7')
  );
