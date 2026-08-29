# Harness — Conformidade legal (Moçambique)

Mede o progresso contra [CONFORMIDADE_LEGAL_MZ_SPEC.md](CONFORMIDADE_LEGAL_MZ_SPEC.md).
Legenda: ✅ feito · 🟡 parcial · ❌ em falta · 🔴 **defeito confirmado no código anterior** ·
⚖️ **depende de decisão jurídica humana — a IA não a pode marcar como feita**.

**Data:** 2026-08-25 · Migração **V59**.

> **Estado:** as guardas técnicas estão implementadas e cobertas por testes (18 no
> `OccupationalHealthServiceTest`, eram 5). A **homologação jurídica não está feita** — é a §4 deste
> documento e não é da IA. Nenhuma linha ⚖️ pode ser dada como validada por uma suite de testes.

---

## 1. Protecção de dados de saúde

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| CL-01 | Conta `EMPLOYEE` chama `GET /api/hr/occupational-health/employee/{outro}/summary` | Recusado nomeando que a aptidão é dado de saúde | 🔴→✅ **Antes desta sessão devolvia o estado de aptidão do colega.** `ensureManagerOrSelf`; teste `summaryOfAnotherEmployeeIsBlockedForNonManager` |
| CL-02 | Conta `EMPLOYEE` consulta **o seu próprio** resumo de aptidão | Devolve normalmente | ✅ teste `summaryOfSelfIsAllowedForEmployee` — a guarda não podia matar o self-service |
| CL-03 | Gestor consulta o histórico clínico de um trabalhador | Grava `OCCUPATIONAL_HEALTH_ACCESS` com quem, de quem e quantos registos | ✅ teste `readingClinicalHistoryIsAudited`. `history()` deixou de ser `readOnly` — senão o rasto era criado e nunca escrito |
| CL-04 | Registar exame com "Teste de HIV negativo" nas observações | Recusado em PT-MZ, sem gravar | ✅ teste `serologyInFreeTextIsRejected` (Lei n.º 19/2014 + Lei n.º 13/2023) |
| CL-05 | Registos **anteriores** que já contenham texto proibido | Não são apagados nem mascarados pelo sistema | 🟡 **assumido**: apagar dados existentes sem decisão humana é destruir prova. Verificação e limpeza são do RH da empresa — ver §4, linha H-07 |
| CL-06 | Restrição legítima ("sem levantar cargas acima de 20 kg") | Passa; a guarda não é um filtro de palavras genérico | ✅ teste `ordinaryRestrictionIsNotMistakenForProhibitedData` |
| CL-07 | Conta `EMPLOYEE` pede a lista de exames a caducar ou em falta | Recusado | ✅ `requireManagerOrAdmin` em `expiring()` e `missingExams()`; o sino não os mostra a outros perfis |
| CL-08 | Anexo digitalizado do exame na base de dados | Cifrado em repouso; ilegível em backup ou cópia da BD | 🔴→✅ **estava em claro — e, pior, era gravado e nunca mais saía.** `AttachmentCrypto` (AES-256-GCM, chave em `security.attachment-key`); 8 testes |
| CL-08b | Anexos gravados **antes** da cifra | Continuam a abrir, sem migração | ✅ prefixo `MCE1`: sem ele, o blob é devolvido tal e qual (`legacyPlainAttachmentsStillOpen`) |
| CL-08c | Anexo cifrado com a chave em falta ou errada | Recusado com a razão, nunca bytes ilegíveis | ✅ 3 testes, incluindo anexo adulterado na BD (GCM detecta) |
| CL-08d | Abrir o comprovativo | Só MANAGER/ADMIN e fica auditado | ✅ `OCCUPATIONAL_HEALTH_ATTACHMENT_ACCESS`; `employeeCannotOpenAnAttachment` |
| CL-09 | Direitos do titular (acesso, rectificação, oposição, eliminação) | Não implementados | ❌ ⚖️ não há regime em vigor que os defina em Moçambique. Rever quando a Lei de Protecção de Dados entrar em vigor |

## 2. Saúde ocupacional — conformidade laboral

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| CL-10 | Trabalhador no activo que **nunca** fez exame de aptidão | Aparece em lista própria e no sino, com dias desde a admissão | 🔴→✅ **antes não aparecia em lado nenhum**: sem exame não há validade a caducar. `missingExams()`; teste `missingExamsListsActiveEmployeesWithoutAnyExam` |
| CL-11 | Trabalhador `TERMINATED` sem exame | **Não** aparece na lista | ✅ mesmo teste — a obrigação é sobre quem está ao serviço |
| CL-12 | Trabalhador sem data de admissão registada | Aparece na lista, a dizer "Admissão por registar" em vez de um número inventado | ✅ `daysSinceHire` nulo, ordenado no fim |
| CL-13 | Periodicidade legal dos exames periódicos | Configurável, com base legal registada | ❌ ⚖️ **está no diploma ministerial conjunto (Trabalho + Saúde) que esta análise não identificou.** O sistema não inventa o prazo — a validade continua a vir do exame, que é quem a fixa |
| CL-14 | Prazo de conservação dos registos de saúde e expurgo | Configurável; expurgo como acto humano auditado | ❌ ⚖️ mesmo diploma. Não se constrói um expurgo irreversível sem saber ao fim de quanto tempo |

## 3. Prestadores e custo dos exames (V59)

| ID | Cenário | Resultado esperado | Estado |
|----|---------|--------------------|--------|
| CL-20 | Registar exame escolhendo prestador do cadastro de fornecedores | Fica ligado ao fornecedor; `providerName` mostra o cadastrado | ✅ teste `registerLinksProviderAndCost` |
| CL-21 | Prestador de **outra empresa** | Recusado | ✅ teste `providerFromAnotherCompanyIsRejected` — mesma regra de tenant do resto do sistema |
| CL-22 | Prestador inactivo no cadastro | Recusado nomeando o prestador | ✅ `resolveProvider` |
| CL-23 | Clínica não cadastrada | Texto livre continua a funcionar; registos anteriores à V59 não perdem o nome | ✅ `providerLabel()` — cadastrado manda sobre texto livre |
| CL-24 | Número de factura sem valor | Recusado | ✅ teste `invoiceWithoutCostIsRejected` — despesa sem montante não existe em relatório nenhum |
| CL-25 | Pagar o exame à clínica | Saída de tesouraria + `OCCUPATIONAL_HEALTH_EXAM_PAID` auditado | ✅ teste `payingExamLeavesTreasuryAndIsAudited` |
| CL-26 | Pagar o mesmo exame duas vezes | Recusado, nomeando a data do primeiro pagamento | ✅ teste `payingTheSameExamTwiceIsRejected` |
| CL-27 | Pagar exame sem custo registado | Recusado, dizendo o que falta | ✅ teste `payingExamWithoutCostIsRejected` |
| CL-28 | Custo da saúde ocupacional no ano, por prestador | Total, pago e por pagar; exames sem custo ficam de fora da contagem | ✅ teste `costReportAggregatesByProvider` |
| CL-29 | O custo do exame **nunca** vira desconto ao trabalhador | Não existe porta que o faça: o pagamento é saída de tesouraria e não toca na folha | ✅ por construção — `payExam` não cria `PayrollDeduction` nem altera recibos |
| CL-30 | Lançamento contabilístico do encargo com saúde ocupacional | Por fazer | ❌ **fronteira declarada**, a mesma dos adiantamentos e acertos finais do §B5: mapear a contas do PGC-NIRF é decisão do contabilista. O RH continua a não importar contabilidade (`HrDoesNotKnowAccountingTest`) |

---

## 4. Homologação jurídica — **por assinar**

> **H-01 a H-04 já têm o pedido escrito.** Ver
> [PEDIDO_MITESS_EXAMES_MEDICOS.md](PEDIDO_MITESS_EXAMES_MEDICOS.md) — minuta pronta a rever, pôr em
> papel timbrado e enviar ao MITESS/MTGAS, com a tabela de onde cada resposta entra no sistema.
> Nenhuma delas exige código novo.


Cada linha precisa de confirmação de um jurista ou advogado com prática laboral moçambicana. A
coluna *Onde entra no sistema* diz o que acontece depois de a resposta existir — em quase todos os
casos é preencher configuração, **não escrever código novo**.

| ID | Pergunta ao jurista | Onde entra no sistema | Resposta | Data / Assinatura |
|----|---------------------|-----------------------|----------|-------------------|
| H-01 | Qual o **diploma ministerial conjunto** (Trabalho + Saúde) que fixa as regras dos testes, exames médicos e **respectivos registos**? Está em vigor? | Base legal de tudo o que está em CL-13 e CL-14 | *Pesquisa de 2026-08-28: confirma-se que a Lei n.º 13/2023 **remete** para este diploma, mas ele não é identificável em fontes públicas — nem número, nem data, nem texto. Pedi-lo ao MITESS/MTGAS é passo de jurista, não de pesquisa.* | |
| H-02 | **Periodicidade** legal dos exames de aptidão — geral e agravada (funções de risco, menores, trabalho nocturno) | Campo novo em `hr_policy_configs` com `legal_basis` | | |
| H-03 | **Prazo de conservação** dos registos de saúde ocupacional após a cessação do contrato | Campo novo + relatório de registos fora de prazo | | |
| H-04 | O **exame de admissão** é obrigatório antes do início de funções, ou basta na admissão? | Torna CL-10 uma recusa em vez de um aviso, se for o caso | | |
| H-05 | Confirma que o **custo dos exames é integralmente do empregador**, sem excepções, e que nunca pode ser descontado na folha? | Confirma CL-29, hoje garantido por construção | | |
| H-06 | Que **artigos** da Lei n.º 13/2023 suportam: exames de aptidão, sigilo médico e proibição de teste de HIV/SIDA? | §2 e §3 da spec passam a citar artigo | | |
| H-07 | Como tratar registos históricos que contenham informação clínica que hoje é recusada (CL-05)? Apagar, mascarar, ou conservar? | Decide se é preciso um procedimento de limpeza | | |
| H-08 | ~~O anexo tem de estar cifrado em repouso?~~ **Feito** (CL-08). Resta confirmar o **período de custódia da chave** e quem lhe tem acesso | Procedimento de segredos de produção | | |
| H-09 | É exigível **informar por escrito** o trabalhador sobre os dados que a empresa guarda sobre si? | Se sim, documento do colaborador (§B8.8) — não é código novo | | |
| H-10 | Confirmação dos **valores legais do RH** já declarados como pendentes: escalões de IRPS, taxas de INSS, dias de férias por antiguidade, aviso prévio, multiplicadores de horas extra | `PayrollTaxConfig`, `HrPolicyConfig`, `OvertimeRateConfig` — todos já configuráveis com `legal_basis` | | |
| H-11 | Com a **Lei de Protecção de Dados** ainda por aprovar, que regime se aplica hoje à conservação e ao acesso a dados de trabalhadores? | Confirma ou corrige §2 da spec | | |
| H-12 | Os **modelos de documento** que o sistema imprime — contrato de trabalho, certificado de trabalho, acerto final — cumprem os requisitos de forma da Lei n.º 13/2023? | `EmploymentContractPrintService`, `TerminationPrintService` | | |

---

## 5. Declarações honestas

1. ~~Nada foi validado ao vivo pela UI.~~ **O ecrã de histórico foi** (2026-08-28), pelo
   `OccupationalHealthScreensDriver`, contra backend real e com dados que cobrem os dois caminhos
   de cada campo. Encontrou e fechou três defeitos que só existem ao pintar: a linha de totais
   saía cortada a meio, a coluna de dinheiro truncava, e o papel do utilizador lido fora da EDT
   dava "sem permissão" a um ADMIN verdadeiro. **Continuam por abrir** o formulário de registo, o
   fluxo de *Registar Pagamento* e os separadores *Sem exame* / *Custos do ano* — o driver está
   escrito e estende-se a eles com uma linha cada.
2. ~~A V59 não foi aplicada contra PostgreSQL real.~~ **Foi** — ver §6.
3. **A tabela de homologação (§4) está vazia.** Enquanto estiver, o sistema **não pode ser
   apresentado a um cliente como legalmente homologado** — só como preparado para o ser.
4. **Não foram citados números de artigo.** É deliberado e está explicado na §6 da spec. As linhas
   H-01 e H-06 existem para os preencher.
5. **O que este harness mede é código, não conformidade.** Um teste verde prova que a guarda existe
   e funciona; não prova que a guarda é a que a lei exige. Essa segunda parte é a §4.

---

## 6. Verificação

```
mvn -o test  → 878 testes, 0 falhas, 0 erros, 0 ignorados (2026-08-25; eram 865)
               18 no OccupationalHealthServiceTest, eram 5
```

**V59 validada contra PostgreSQL real (2026-08-25).** Cluster descartável na porta 55433, mesma
receita das V48–V58, sem tocar no PostgreSQL do utilizador (5432, confirmado a escutar no fim):

- **58 migrações aplicadas, schema em v59**, e a aplicação arrancou com `ddl-auto=validate` no
  perfil `prod` — cada mapeamento de entidade bate com o schema que o Flyway construiu, incluindo
  o `numeric(19,2)` do custo e a associação `provider_id`.
- Confirmado no schema real: as 4 colunas novas (`provider_id`, `cost`, `invoice_number`,
  `paid_at`), a **chave estrangeira `occupational_health_exams_provider_id_fkey → suppliers(id)`**
  e os 2 índices novos (`idx_occupational_health_provider`, `idx_occupational_health_unpaid`).
- Cluster destruído no fim.
