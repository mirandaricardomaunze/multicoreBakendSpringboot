# Verificação de conteúdo dos backups — 2026-11-01

- Antes de apagar o original, a cifra dos backups antigos e dos novos dumps físicos autentica a
  cópia cifrada e compara SHA-256 do conteúdo decifrado com o original, sem cópia decifrada em disco.
- Backups antigos divergentes ou com cópia cifrada inválida são preservados e geram erro no log.
  A cópia cifrada já existente não é sobrescrita pelo processo de manutenção.
- 23 testes dirigidos passaram, incluindo regressões de conteúdo igual, divergente e corrompido.
- `mvn compile` passou nos três módulos. `mvn clean compile` foi tentado, mas o JAR do backend
  estava bloqueado pelo processo em execução.
- `scripts/verify-backup-restore.ps1` passou os 14 testes do guião com PostgreSQL 18 isolado,
  incluindo paridade de schema e backup/restauro com dados, sequências, integridade e login.
- Evidência local: `data/restore-validation/31113a09087b42a3b57aa226a71863a1`.
  A instância temporária foi encerrada no fim.
- Não foi feito deploy nem reiniciado o backend activo. O VPS e a versão instalada em produção
  continuam por validar; não foi identificado um destino de produção acessível neste ambiente.
