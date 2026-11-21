-- Contas da diferença cambial (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.4, FX-11).
--
-- As empresas novas recebem-nas do plano semeado (PgcNirfChart). Aqui só as empresas que JÁ têm plano:
-- acrescenta-se cada conta em falta, sem tocar nas que existem (mesmo código = mantém-se a do contabilista).
-- Repetível: o NOT EXISTS impede duplicados.
INSERT INTO accounts (code, name, company_id, account_class, nature, postable, active, parent_code,
                      created_at, created_by)
SELECT s.code, s.name, c.id, s.account_class, s.nature, s.postable, TRUE, s.parent_code,
       CURRENT_TIMESTAMP, 'flyway'
FROM companies c
CROSS JOIN (VALUES
    ('66',   'Custos financeiros',                  'CUSTOS_E_PERDAS',    'DEVEDORA', FALSE, '6'),
    ('6601', 'Diferenças de câmbio desfavoráveis',  'CUSTOS_E_PERDAS',    'DEVEDORA', TRUE,  '660'),
    ('76',   'Proveitos financeiros',               'PROVEITOS_E_GANHOS', 'CREDORA',  FALSE, '7'),
    ('7601', 'Diferenças de câmbio favoráveis',     'PROVEITOS_E_GANHOS', 'CREDORA',  TRUE,  '760')
) AS s (code, name, account_class, nature, postable, parent_code)
WHERE EXISTS (SELECT 1 FROM accounts a WHERE a.company_id = c.id)
  AND NOT EXISTS (SELECT 1 FROM accounts a WHERE a.company_id = c.id AND a.code = s.code);
