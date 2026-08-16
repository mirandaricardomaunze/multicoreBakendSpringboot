# Spec — Recursos Humanos & Processamento Salarial

Este documento define o escopo mínimo para o módulo de **RH/Folha (`mz.multicore.erp.modules.hr`)**
ser considerado **completo e profissional** para uma empresa em Moçambique. Complementa
[BUSINESS_FLOWS.md](BUSINESS_FLOWS.md), [SECURITY_AND_AUDIT.md](SECURITY_AND_AUDIT.md),
[DATABASE.md](DATABASE.md) e [TESTING_STRATEGY.md](TESTING_STRATEGY.md).

> Estado-fonte: ver o que já existe vs. o que falta na matriz do
> [HR_PAYROLL_HARNESS.md](HR_PAYROLL_HARNESS.md). Esta spec é o alvo; o harness mede o progresso.

## Objectivo

Gerir o ciclo de vida do colaborador e produzir folha de pagamento **conforme à lei laboral e fiscal
moçambicana** (IRPS, INSS, 13.º mês, subsídio de férias), com auditoria, numeração fidedigna e
ligação à tesouraria — sem quebrar as regras de tenant, permissão e fronteira de camadas do projecto.

## Perfis alvo

- **Colaborador**: consulta o seu recibo, submete pedido de férias e justifica faltas (self-service).
- **Gestor de RH (MANAGER)**: gere colaboradores, processa folha, aprova férias/despesas, lança faltas.
- **Administrador (ADMIN)**: configura o motor fiscal (escalões IRPS, taxas INSS), séries e permissões.
- **Financeiro**: confere a saída de tesouraria do líquido da folha e dos reembolsos de despesa.

## Capacidades obrigatórias

### 1. Cadastro de colaborador
- Nome, nº interno (único por empresa), email (único), telefone, departamento, função.
- NUIT (`taxId`) e nº INSS para a folha fiscal.
- Nº de dependentes (afecta dedução IRPS).
- Salário base, data de admissão, fim de contrato (não anterior à admissão).
- Estado laboral `ACTIVE / SUSPENDED / TERMINATED`; só `ACTIVE` entra na folha.
- **Toda criação/edição/mudança de estado é auditada.**

### 2. Motor fiscal (IRPS + INSS) — Moçambique
- Escalões IRPS progressivos configuráveis (limite inferior/superior, taxa, parcela a abater,
  dedução por dependente), com vigência por data.
- INSS do trabalhador e da entidade patronal por taxa configurável.
- Cálculo determinístico em `BigDecimal`, arredondamento `HALF_UP` a 2 casas.
- Falha explícita (`BusinessRuleException` em PT) se não houver config fiscal vigente para o período.
- A configuração fiscal é **gerível e consultável por API/UI** (não só seeder).

### 3. Recibo de vencimento (payslip)
- Um recibo por colaborador / ano / mês (anti-duplicação).
- Componentes: salário base, subsídios, horas extra, base tributável, IRPS, INSS trabalhador,
  INSS patronal, outras deduções, **descontos por faltas não remuneradas**, líquido.
- **Numeração gapless** via `DocumentNumberService` (série `REC`), nunca timestamp.
- Ciclo: `DRAFT → APPROVED → PAID`; `CANCELLED` só antes de pago.
- **Marcar pago gera saída de tesouraria** pelo líquido (espelho do reembolso de despesa).
- PDF imprimível com cabeçalho da empresa, identificação do colaborador e quebra fiscal.
- Toda emissão, pagamento e cancelamento é auditado.

### 4. Processamento mensal em lote
- Gera recibos `DRAFT` para todos os colaboradores `ACTIVE` sem recibo no período.
- Idempotente: reexecutar não duplica nem sobrepõe recibos já existentes.
- Aplica automaticamente descontos de faltas não remuneradas do período.

### 5. Obrigações legais periódicas (MZ)
- **13.º mês / subsídio de Natal**: apuramento proporcional ao tempo de serviço no ano.
- **Subsídio de férias**: calculado ao gozar férias aprovadas.
- **Mapa fiscal mensal** (INSS + IRPS): por colaborador e totais, **exposto por API e imprimível**
  para entrega às autoridades.

### 6. Férias
- Direito anual configurável (por defeito 22 dias úteis/ano) com **saldo = direito − gozado**.
- Pedido bloqueado se exceder o saldo disponível.
- Fluxo `PENDING → APPROVED/REJECTED`; o decisor vem do `CurrentUserContext`, **nunca do corpo do
  pedido**, e exige permissão MANAGER/ADMIN.
- Decisão auditada; rejeição exige motivo.

### 7. Faltas
- Tipo, intervalo de datas (fim ≥ início), total de dias, motivo, flag de documento de suporte.
- Distinção **remunerada vs. não remunerada**; a não remunerada desconta no recibo do período.
- Lançar/eliminar falta exige permissão e é auditado.

### 8. Despesas / reembolsos
- Submissão → Engine de Aprovações → na aprovação, reembolso automático à tesouraria. *(já existe)*
- Rejeição com motivo. Estados e decisões auditados.

### 9. Segurança, tenant e fronteiras
- Tudo scoped por empresa activa (`CurrentUserContext`).
- Operações de gestão exigem MANAGER/ADMIN; self-service restrito ao próprio colaborador.
- Controller só fala HTTP + DTO; Service detém a lógica e `@Transactional`; nunca devolver `@Entity`.
- `BusinessRuleException` para toda a regra de negócio, mensagem em PT-MZ.

## Não-objectivos (fora do escopo desta iteração)
- Ponto biométrico / integração com relógio de ponto.
- Cálculo de horas extra a partir de marcações (entram como valor manual).
- Integração directa com portais da AT/INSS (gera-se o mapa; submissão é manual).

## Critério de "pronto"
Esta spec considera-se satisfeita quando **todos os cenários `RH-01..RH-NN` do harness** estiverem
verdes (manual e automatizado) e `mvn test` cobrir os Services fiscais e de folha com a profundidade
descrita em [HR_PAYROLL_HARNESS.md](HR_PAYROLL_HARNESS.md).
