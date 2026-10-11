# SPEC — Controlo de impressão e reimpressão de documentos sensíveis

Estado: **aprovado para implementação**  
Decisão: 2026-10-11

## Objectivo

Permitir recuperar documentos perdidos ou danificados sem criar cópias anónimas. A primeira via é
normal; qualquer via posterior exige autorização de outro gerente ou administrador, motivo e
auditoria.

## Documentos abrangidos

- Fatura A4 e térmica.
- Recibo.
- Nota de crédito e nota de débito.
- Guia de remessa e guia de transporte.
- Guia de transferência e folha de separação.
- Encomenda e folha de separação de encomenda.
- Recibo de salário.
- Contrato de trabalho.
- Certificado e acerto de cessação.
- Vale de compras e fecho Z.

Cotações, relatórios analíticos, etiquetas e exportações de tabelas não exigem dupla autorização,
porque não comprovam por si só saída de valor, stock ou vínculo.

## Regras

1. A primeira impressão regista data, operador e terminal.
2. A partir da segunda via, o operador informa utilizador e senha de outro `MANAGER` ou `ADMIN`,
   motivo e terminal.
3. O operador actual não pode autorizar a própria reimpressão.
4. O autorizador deve pertencer à empresa activa.
5. Credenciais nunca são persistidas nem incluídas na auditoria.
6. A reimpressão mantém número, valores e estado do documento.
7. O PDF reimpresso recebe marca visível `REIMPRESSÃO — n.ª VIA` em todas as páginas.
8. Falha ao gerar o PDF não incrementa a contagem.
9. Cada evento guarda operador, autorizador, motivo, terminal e instante na auditoria; o controlo
   operacional mantém contagem e dados da última impressão.
10. A lista de faturas mostra `Impressões`, usando `—` quando nunca impressa e
    `N × dd/MM/yyyy HH:mm` quando já emitida.

## Persistência

Uma tabela central `document_print_controls` identifica unicamente
`(company_id, document_type, document_id)` e evita acrescentar os mesmos campos a todas as
entidades de negócio.

## API

- `GET /api/print/status/{type}/{id}` — consulta o estado.
- `GET /api/print/status/{type}?ids=...` — consulta em lote para listas.
- Os endpoints existentes geram apenas a primeira via.
- `POST /api/print/{type}/{id}/reprint` recebe `ReprintAuthorizationRequest` e devolve o PDF
  marcado como reimpressão.

## Testes obrigatórios

- Primeira via incrementa uma vez.
- Segunda chamada ao endpoint normal é recusada.
- Reimpressão exige outro utilizador gerente/administrador da empresa.
- Senha errada, mesmo utilizador, motivo vazio e utilizador de outro tenant são recusados.
- PDF reimpresso contém a marca e a contagem só aumenta após geração bem-sucedida.
- Isolamento por empresa e consulta em lote.
- `MultiModuleArchitectureHarnessTest` e `DesktopThinContextTest`.
