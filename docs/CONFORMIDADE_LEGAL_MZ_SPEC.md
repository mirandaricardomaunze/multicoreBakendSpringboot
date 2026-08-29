# Spec — Conformidade legal (Moçambique): regras laborais e protecção de dados

> O que a lei moçambicana impõe ao **software**, não ao patrão. Cobre o que o Multicore guarda,
> mostra e recusa sobre pessoas — com foco no que o §B8.9 (saúde ocupacional) trouxe: dados de
> saúde de trabalhadores identificados. Progresso medido em
> [CONFORMIDADE_LEGAL_MZ_HARNESS.md](CONFORMIDADE_LEGAL_MZ_HARNESS.md).

**Data:** 2026-08-25 · **Estado: guardas implementadas; homologação por jurista em falta.**

> ⚠️ **Este documento não é um parecer jurídico e não substitui um.** Foi escrito por uma IA a
> partir de fontes públicas consultadas a 2026-08-25 (listadas em §7). O que aqui está verificado é
> **qual é o diploma, sobre que matéria e se está em vigor**. O que aqui **não** está verificado
> são **números de artigo** — e não estão de propósito: citar um artigo errado num sistema que uma
> empresa usa para se defender numa inspecção é pior do que não citar nenhum. O preenchimento dos
> artigos e a assinatura de cada linha são do jurista, na tabela de homologação do harness.

---

## 1. Problema

O ERP passou a guardar **dados de saúde de pessoas identificadas** (§B8.9, migração V58) sem que
ninguém tivesse verificado sob que regime é que os pode guardar. Três coisas estavam erradas ao
mesmo tempo, e nenhuma delas se via a correr o programa:

1. **O resumo de aptidão não tinha guarda nenhuma.** `summary(employeeId)` verificava que o
   trabalhador existia na empresa e devolvia o estado de aptidão a **qualquer conta autenticada**.
   O detalhe clínico estava protegido por `requireManagerOrAdmin`; o resumo — que diz "Inapto" —
   não estava. Trocar o número no endereço lia a aptidão de um colega.
2. **O campo de observações era um campo de diagnóstico à espera de acontecer.** `notes` e
   `restrictions` são texto livre de 1000 caracteres. A lei fecha exactamente esta porta: o médico
   não pode comunicar ao empregador nada além da **capacidade ou incapacidade para o trabalho**, e
   é expressamente proibido apurar o estado de HIV/SIDA do trabalhador ou do candidato. Nada no
   código impedia que o resultado serológico fosse ali escrito — e uma vez escrito, entra no
   backup, na exportação e no ecrã de quem passar por ali.
3. **Ler dados de saúde não deixava rasto.** Registar um exame era auditado; **consultar** o
   histórico clínico de uma pessoa não era. Num sistema com dados sensíveis, quem consultou o quê é
   metade do controlo — a outra metade é quem pode consultar.

E, ao lado destas, a lacuna que a inspecção do trabalho encontra primeiro: **quem nunca fez exame
nunca aparecia em aviso nenhum**. A lista de alertas parte da validade, e quem não tem exame não
tem validade a caducar. O sistema avisava sobre os cumpridores e calava-se sobre os outros — o
mesmo defeito das obrigações sem prazo do §B5.

---

## 2. Quadro legal verificado

| Diploma | Matéria | Estado (2026-08-25) | O que impõe ao software |
|---|---|---|---|
| **Lei n.º 13/2023, de 25 de Agosto** — Lei do Trabalho | Relação laboral; higiene, saúde e segurança no trabalho | **Em vigor desde 21-02-2024** (*vacatio legis* de 180 dias). Revogou a Lei n.º 23/2007 | Exames de aptidão na admissão e periódicos; **o médico só comunica ao empregador a capacidade ou incapacidade para o trabalho**; proibição de apurar o estado de HIV/SIDA |
| **Diploma ministerial conjunto** (Trabalho + Saúde) sobre testes, exames médicos e **os seus registos** | Periodicidade, forma e conservação dos registos de exames | **Remetido pela Lei n.º 13/2023 — não identificado por esta análise** | É aqui que estão a periodicidade e o prazo de conservação. **Sem ele, o sistema não inventa nenhum dos dois** |
| **Lei n.º 19/2014** — protecção da pessoa, trabalhador e candidato a emprego vivendo com HIV e SIDA | Não-discriminação; testes no local de trabalho | Em vigor | Proibição de testes no local de trabalho salvo a pedido do próprio; proibição de discriminação em direitos, formação, promoção e carreira |
| **Constituição da República** (direito à reserva da vida privada e uso da informática) + **Lei n.º 3/2017** — Transacções Electrónicas | Protecção de dados pessoais | Em vigor, **fragmentado** | O regime geral aplicável hoje. Não define categorias especiais de dados nem direitos do titular ao nível de um regime autónomo |
| **Proposta de Lei de Protecção de Dados Pessoais** (INTIC; consulta pública desde 09-2025) | Regime autónomo; cria a **ANPD** | **Aprovada em Conselho de Ministros em Março de 2026; aguarda votação final. NÃO está em vigor** | É o alvo de desenho, não uma obrigação actual. Finalidade, minimização, acesso, rasto, conservação e direitos do titular |

**A consequência prática desta tabela é a regra central deste documento:**

> **Moçambique não tem, à data, lei autónoma de protecção de dados pessoais em vigor.** O sistema é
> desenhado contra os princípios da proposta de lei e do regime fragmentado actual — porque é o que
> se pode fazer hoje sem esperar — mas **nada aqui pode ser apresentado como "conforme à lei de
> protecção de dados"**, porque essa lei ainda não existe. Quando entrar em vigor, este documento e
> o harness voltam à mesa.

---

## 3. Saúde ocupacional — o que o sistema passou a impor

### 3.1 O empregador só sabe se a pessoa está apta

O registo do exame aceita **três resultados fechados** (`FIT`, `FIT_WITH_RESTRICTIONS`, `UNFIT`) e
**restrições de função**. Não tem campo de diagnóstico, e o texto livre que tem passou a ser
verificado: qualquer referência a estado serológico (HIV, SIDA, VIH, seropositividade, serologia,
carga viral, CD4) é **recusada com a razão em PT-MZ**, em vez de ser gravada.

A lista é **curta de propósito**. Não tenta detectar "diagnóstico" — nenhuma expressão regular sabe
fazer isso — apura só o caso que a lei nomeia. Um falso positivo custa reformular a frase; um falso
negativo custa dados que não podiam ter sido escritos.
[`OccupationalHealthService.PROHIBITED_HEALTH_DATA`](../backend/src/main/java/mz/multicore/erp/modules/hr/service/OccupationalHealthService.java)

### 3.2 Quem pode ver

| Dado | Quem vê | Onde |
|---|---|---|
| Histórico clínico ocupacional (detalhe, anexo, restrições) | Só **MANAGER/ADMIN** | `history()` |
| Resumo de aptidão (apto/inapto + validade) | **MANAGER/ADMIN e o próprio trabalhador** | `summary()` |
| Alertas de validade e de exame em falta | Só **MANAGER/ADMIN** | `expiring()`, `missingExams()`, sino |

A regra do resumo é a mesma do `ensureCanActFor` do `HRService` (§B7.2), aplicada à **leitura**: um
gestor vê por qualquer trabalhador; toda a gente vê por si e por mais ninguém.

### 3.3 Ler deixa rasto

Consultar o histórico clínico de alguém grava `OCCUPATIONAL_HEALTH_ACCESS` com quem consultou, de
quem, e quantos registos. Por isso `history()` **não é `@Transactional(readOnly = true)`**: numa
transacção só de leitura o registo de acesso seria criado e nunca escrito — um rasto que parece
existir e não existe é pior do que nenhum.

### 3.4 O comprovativo: cifrado em repouso e legível por quem pode

O anexo digitalizado do exame era **gravado e nunca mais saía** — não havia endpoint, serviço nem
cliente que o lesse. Ficava em claro na base de dados e nos backups sem servir para nada: exposto e
inútil ao mesmo tempo. Cifrá-lo sem o tornar legível seria proteger um documento morto, pelo que as
duas coisas foram feitas juntas.

- **Cifra AES-256-GCM** em repouso (`AttachmentCrypto`), com chave em `security.attachment-key`
  (32 bytes, Base64). GCM autentica além de cifrar: um anexo adulterado na base de dados é detectado
  em vez de descodificado, e o IV é aleatório por anexo.
- **Instalável numa loja a funcionar.** O que é cifrado leva o prefixo `MCE1`; blobs sem esse prefixo
  são devolvidos tal e qual, pelo que **os anexos gravados antes desta alteração continuam a abrir**,
  sem migração e sem janela em que ninguém lê nada. Sem chave configurada, o comportamento é o de
  antes — falhar o arranque por falta de chave transformaria segurança em paragem de serviço.
- **Mas nunca falha em silêncio.** Cifrado sem chave, ou com a chave errada, é **recusado com a
  razão**. Devolver bytes ilegíveis a fingir que são o ficheiro é como um anexo se perde sem ninguém
  dar por isso.
- **Abrir é acto registado.** `GET /api/hr/occupational-health/exam/{id}/attachment` exige
  MANAGER/ADMIN e grava `OCCUPATIONAL_HEALTH_ATTACHMENT_ACCESS` — mesmo regime do histórico, e pela
  mesma razão: é um documento clínico de uma pessoa com nome.

> **A chave não vive no código nem no repositório.** Vai por variável de ambiente, como as
> credenciais da base de dados (`application-prod.properties`). Perder a chave é perder os anexos
> cifrados com ela — entra no mesmo procedimento de custódia dos segredos de produção.

### 3.5 Quem paga o exame

O custo do exame de aptidão é **encargo do empregador**. O sistema regista-o no exame (`cost`,
`invoice_number`), paga-o à clínica por **saída de tesouraria** — a mesma porta do recibo e da
retenção — e **não tem nenhuma porta que o transforme em desconto ao trabalhador**: o pagamento não
gera `PayrollDeduction` nem toca na folha. Ver §4.

---

## 4. Prestadores e custo dos exames (V59)

**Uma clínica é um fornecedor.** O prestador do exame aponta para o cadastro de fornecedores
(`suppliers`) em vez de ser texto livre, porque é lá que a factura da clínica vive — com NUIT,
contacto e histórico de pagamentos. Criar um segundo cadastro só para o RH duplicava tudo isso e
partia a pergunta "quanto pagámos à Clínica X" em duas respostas diferentes.

- O texto livre `clinic` **fica**, para clínicas ainda não cadastradas e para os registos anteriores
  à V59, que não têm fornecedor nenhum a que apontar. `providerName` mostra o cadastrado quando
  existe e o texto livre quando não.
- O prestador tem de ser **da empresa activa e estar activo** no cadastro — a mesma verificação de
  tenant do resto do sistema.
- **Um número de factura sem valor é recusado.** Uma despesa registada sem montante é uma despesa
  que nunca chega a existir em relatório nenhum.
- **Pagar duas vezes é recusado**, nomeando a data do primeiro pagamento — espelho exacto do
  `markPayslipPaid` e da entrega de retenções (§B5).
- O relatório de custos agrega por prestador, com **total e por pagar**, no intervalo pedido.

**O que isto fecha:** o encargo com saúde ocupacional não existia em número nenhum. Saía da
tesouraria misturado com tudo o resto, e a pergunta "quanto nos custou a saúde ocupacional este
ano" não tinha resposta — nem a pergunta "o que devemos às clínicas".

**O que isto deliberadamente não faz:** não lança na contabilidade. É a mesma fronteira declarada no
§B5 para adiantamentos e acertos finais — mapear este encargo a contas do PGC-NIRF é decisão do
contabilista, não da IA, e o RH não importa contabilidade (`HrDoesNotKnowAccountingTest`).

---

## 5. O que continua por fazer — e porquê

| Lacuna | Porque não foi fechada | Quem a fecha |
|---|---|---|
| **Periodicidade dos exames** | Está no diploma ministerial conjunto que esta análise não conseguiu identificar. Escrever "12 meses" seria inventar um prazo legal | Jurista identifica o diploma → entra no `HrPolicyConfig` com `legal_basis`, como o IRPS e as horas extra |
| **Prazo de conservação e eliminação dos dados de saúde** | Mesmo motivo. E eliminar registos é irreversível: não se constrói um expurgo sem saber ao fim de quanto tempo | Jurista → depois, campo configurável + relatório de registos fora de prazo. **Eliminar continua a ser acto humano** |
| **Direitos do titular** (acesso, rectificação, oposição) | Não existe regime em vigor que os defina em Moçambique. Implementar hoje seria adivinhar o desenho da lei | Rever quando a Lei de Protecção de Dados entrar em vigor |
| **Consentimento / informação ao trabalhador** | O sistema não regista que o trabalhador foi informado de que dados a empresa guarda sobre si | Jurista decide se é exigível hoje; se sim, é um documento do colaborador (§B8.8), não código novo |
| **Valores legais do RH** (IRPS, INSS, férias, aviso prévio, horas extra) | Já eram configuráveis com `legal_basis` e **já estavam declarados como por confirmar** desde o §B2.2/§B6 | Contabilista + jurista, na tabela de homologação |

---

## 6. Divergências assumidas

**Não foram citados números de artigo.** A alternativa era copiá-los de resumos de terceiros sem os
confirmar no Boletim da República. Num documento cuja função é servir de defesa perante a inspecção,
uma citação errada é pior do que uma lacuna assumida — e a lacuna tem dono na tabela de homologação.

**A guarda de dados proibidos actua no `register`, não na leitura.** Registos anteriores à V59 que já
contenham texto proibido **não são apagados nem mascarados**: apagar dados existentes sem decisão
humana é destruir prova, e mascarar dá a ilusão de que o problema foi tratado. O harness tem um
cenário (CL-05) para essa verificação, que é manual e é do RH da empresa.

**O prestador é um `Supplier` e não uma entidade nova.** O módulo RH passa a importar
`modules.purchases`. É acoplamento novo, e assumido: `Supplier` é dado mestre partilhado, como
`Company` — ao contrário da contabilidade, que é a jusante e que o RH continua a **não** conhecer.

---

## 7. Fontes consultadas (2026-08-25)

- Ministério do Trabalho (MITESS/MTGAS) — [Nova Lei do Trabalho já está em vigor](https://www.mtgas.gov.mz/blog/nova-lei-do-trabalho-j%C3%A1-est%C3%A1-em-vigor)
- Tribunal Supremo — [Lei n.º 13/2023, de 25 de Agosto (texto integral)](https://ts.gov.mz/wp-content/uploads/2024/05/Lei-No-13-2023-de-25-de-Agosto-NOVA-LEI-DO-TRABALHO-BR_165_I_SEI_RIE_2Ao__230831_091658.pdf)
- Assembleia da República — [Lei do Trabalho (PDF)](https://www.parlamento.mz/wp-content/uploads/2023/05/Lei-do-Trabalho.pdf)
- RSM Moçambique — [Esclarecimento sobre a realização de testes e exames médicos nas relações jurídicas de trabalho](https://www.rsm.global/mozambique/pt-pt/news/esclarecimento-sobre-realizacao-de-testes-e-exames-medicos-nas-relacoes-juridicas-de-trabalho)
- LexLink — [Lei n.º 19/2014](https://www.lexlink.eu/conteudo/mocambique/ia-serie/166421/lei-no-192014/20525/por-tema)
- INTIC — [Consulta pública da Proposta de Lei de Protecção de Dados](https://intic.gov.mz/consulta-publica-da-proposta-de-lei-de-proteccao-de-dados/) · [Actual quadro legal e regulamentar](https://intic.gov.mz/actual-quadro-legal-e-regulamentar-garante-a-proteccao-de-dados-pessoais-em-mocambique/)
- PLMJ — [Proposta de Lei de Protecção de Dados Pessoais (nota informativa)](https://www.plmj.com/xms/files/NI_Poposta_de_Lei_de_Protecao_de_Dados_Pessoais_Mocambique.pdf)
- WageIndicator/MeuSalário Moçambique — [Saúde e segurança no trabalho](https://meusalario.org/mocambique/lei-de-trabalho/seguranca-e-saude-ocupacional)

Ver também: [SECURITY_AND_AUDIT.md](SECURITY_AND_AUDIT.md) · [RH_COMPLETO_SPEC.md](RH_COMPLETO_SPEC.md)
