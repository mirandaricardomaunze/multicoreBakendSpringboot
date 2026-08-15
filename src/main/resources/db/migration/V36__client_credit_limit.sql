-- Limite de crédito do cliente.
--
-- Porquê: com a V35 passou a saber-se o que está em atraso, mas nada impedia continuar a
-- vender fiado a quem já deve mais do que devia. O limite é a trava.
-- Ver docs/LIMITE_CREDITO_SPEC.md.
--
-- NULL = sem limite definido (crédito livre), que é o comportamento de toda a base anterior.
-- Zero é diferente de NULL e significa "não vende fiado a este cliente".
alter table clients add column if not exists credit_limit numeric(14, 2);
