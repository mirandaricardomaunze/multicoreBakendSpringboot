# Guia de remessa, lacunas de gestão, contabilidade e compatibilidade de versões

28 commits acumulados desde a `main` — várias semanas de trabalho. Cada alteração de regra tem
**spec + harness** em `docs/` e migração retroactiva conservadora.

**Verificação:** `mvn -o clean test` → **533 testes, 0 falhas, 0 erros, 0 ignorados**.
**CI:** verde no último commit (`6669ba7`).

---

## 1. Lacunas de gestão (auditoria de 2026-08-09) — todas fechadas

| Lacuna | Migração | Cenários | Spec |
|---|---|---|---|
| Sem `dueDate`/aging — não se sabia o que estava **em atraso** | V35 | VA-01..25 | `VENCIMENTO_ANTIGUIDADE_SPEC.md` |
| Sem limite de crédito — nada travava o fiado | V36 | LC-01..32 | `LIMITE_CREDITO_SPEC.md` |
| Margem com o preço de compra **actual** | V37 | MC-01..06 | `MARGEM_CUSTO_HISTORICO_SPEC.md` |
| Zero paginação; dashboard lia a tabela inteira | — | PG-01..11 | `PAGINACAO_SPEC.md` |
| **Sem contabilidade** — a maior ausência | V38 | CT-01..46 | `CONTABILIDADE_SPEC.md` |

### Contabilidade (PGC-NIRF)
Plano de contas moçambicano por empresa, diário de partidas dobradas (série `LC`), razão com
saldo de abertura e balancete. Venda e recebimento lançam automaticamente.

Duas decisões que merecem revisão atenta:

- **Os lançamentos automáticos entram por eventos** (`SaleRegisteredEvent`,
  `PaymentReceivedEvent`), não por chamada directa: o módulo `comercial` não passou a conhecer o
  `accounting`. `@EventListener` síncrono, na mesma transacção — se o lançamento falhar, a venda
  não fica gravada pela metade.
- **A natureza do saldo é gravada na conta, não derivada da classe.** Clientes (2101) e
  Fornecedores (2201) são ambos classe 2 com naturezas opostas; IVA liquidado e dedutível idem.
  Um teste do balancete falhou exactamente por isto durante o desenvolvimento (CT-42).

Validado ao vivo: fatura de 2200 → `D Clientes / C Vendas` + `D CMVMC 1720 / C Mercadorias`
(custo = 430 × 4, da fotografia da linha); recibo → `D Caixa / C Clientes` sem tocar em Vendas;
balancete fecha (8080 = 8080).

---

## 2. Compatibilidade desktop ↔ backend (novo)

O desktop e o backend são o mesmo codebase mas actualizam em ritmos diferentes. Sem isto, o
desvio de versões aparece como um erro sem explicação a meio de uma factura.

- O desktop identifica-se em todos os pedidos (`X-Client-Version`).
- Versão abaixo da mínima → **426** com a versão instalada, a mínima e o que fazer.
- Aviso discreto no rodapé quando há versão nova (**não** bloqueia).
- Aba **"Versões dos Clientes"** na consola da plataforma: quem está em quê, e quantas versões
  diferentes estão em uso.

**Por omissão não bloqueia ninguém** (`min-version = 0.0.0`): uma política que tranca lojas fora
do sistema não pode entrar ligada por acidente. Spec: `ACTUALIZACOES_CLIENTE_SPEC.md`.

---

## 3. Correcções de dinheiro e fiscais

- **IVA: a taxa é do artigo, nunca a que o ecrã envia** (`a0f3f5c`) — o mesmo produto era
  tributado de forma diferente conforme a porta. Contaminava fatura, encomenda, guia, NC, a
  declaração mensal de IVA e o SAF-T.
- **IVA da compra vem da factura do fornecedor** (`7893ce7`), não de 16% cego.
- **"Isto conta como venda" passa a ter uma definição só** (`d244875`).
- **Recibo parcial deixava de apagar a dívida** — três furos de dinheiro fechados.
- **Fail-closed no contexto** (`50c684f`): sem contexto não há `ADMIN` nem empresa 1.

---

## 4. Guia de Remessa ao cliente
`DeliveryGuide` (série `GR`) gerada a partir da encomenda; stock SALE só na aprovação. Caminhos
separados: uma encomenda vira guia **ou** fatura, nunca as duas.

---

## 5. UI e POS
Componentes canónicos (`FormField`, `MoneyField`, `IntegerField`, `TablePager`), carregamento
assíncrono com propagação de contexto, documentos em painel completo, central de notificações,
paginação uniforme nas tabelas, catálogo do POS paginado e com estado de stock, e backups
automáticos. Cada um com a sua spec/harness em `docs/`.

---

## 6. Três defeitos apanhados a correr a aplicação (não pelos testes)

1. **A suite não era determinística.** `LoadingCursorTest` rebentava com `HeadlessException`
   conforme a **ordem das classes** — o surefire arranca `headless=true` e o AWT decide a
   headlessness uma só vez. Passou a ser declarado no `pom.xml`.
2. **No POS o stock saía antes de a venda estar autorizada** (LC-32). Extraído
   `deductStockForSale()` e movido para depois da trava de crédito.
3. **O 426 chegava sem mensagem.** O `sendError` do Spring descarta o texto; a mensagem cuidada
   nunca chegaria ao operador. Passou a escrever-se o corpo directamente — e os testes passaram a
   verificar o **corpo**, que era o que os deixava passar com o defeito presente.

---

## O que fica por fazer (declarado, não escondido)

- **Compras e salários ainda não lançam na contabilidade.** O plano já tem as contas; falta
  publicar os eventos. Ver `CONTABILIDADE_SPEC.md` §7.
- **Notas de crédito não geram estorno contabilístico.**
- **Filtros das tabelas continuam do lado do cliente** — numa listagem paginada filtram dentro da
  página.
- **Instalador e actualização automática não existem** — desenhados em
  `ACTUALIZACOES_CLIENTE_SPEC.md` §6, não implementados.
- **A maioria dos cenários manuais não foi executada.** Validados ao vivo: CT-50..55, VA-50/52,
  LC-50/52/53, AC-20..24. Os restantes continuam por fazer.
- Itens de mundo real por fazer: `docker compose up` numa VPS, restore verificado,
  impressora/leitor/gaveta, proteção de ramo.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
