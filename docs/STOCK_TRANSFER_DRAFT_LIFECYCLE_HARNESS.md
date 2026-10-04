# Harness — Transferências editáveis

## Backend

- Criação devolve `DRAFT` e não consome lote nem grava movimento.
- Actualização de `DRAFT` substitui cabeçalho/linhas e preserva número, empresa e data.
- Versão desactualizada e actualização fora de `DRAFT` são recusadas.
- Submissão válida produz `PENDING_APPROVAL` sem movimento de stock.
- Aprovação de `DRAFT` e rejeição de `DRAFT` são recusadas.
- Apenas aprovação de `PENDING_APPROVAL` movimenta origem/destino por FEFO.
- Rejeição e cancelamento não movimentam stock; estados terminais não podem ser cancelados.
- Tenant, armazéns distintos, motorista, matrícula, linhas e disponibilidade continuam validados.

## Arquitectura e contratos

- Executar `MultiModuleArchitectureHarnessTest` por alteração em `contracts`.
- Confirmar que controller delega ao service e desktop usa apenas o cliente HTTP.
- Confirmar compatibilidade dos construtores de `StockTransferDTO`.

## Desktop

- Executar `DesktopThinContextTest`.
- Confirmar lista/editor no mesmo separador, sem modal de edição.
- Confirmar campos editáveis apenas em `DRAFT`, aviso de descarte e `Ctrl+S`.
- Confirmar pesquisa de produto dentro da célula e edição directa de `Qtd`, `Emb.` e `Cx.`.
- Confirmar recálculo imediato das colunas derivadas sem formulário externo à grelha.
- Confirmar que os dados gerais são editados numa grelha de uma linha e que o editor não contém
  `createDialogForm` nem `ModernFormDialog`.
- Confirmar pesquisa, filtros, tabela e paginação/acções dentro do card.
- Confirmar os controlos adjacentes com altura visual uniforme.

## Build

- Executar os testes dirigidos da transferência e os dois harnesses.
- Executar `mvn clean compile`.
- Reiniciar backend/desktop e verificar `/actuator/health` antes de abrir a GUI.
