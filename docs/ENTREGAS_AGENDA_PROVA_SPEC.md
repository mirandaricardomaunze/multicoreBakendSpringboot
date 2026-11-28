# Spec — Agenda de entregas, prova de entrega e devolução na obra (SA-C08)

**Data:** 2026-11-28 · **Estado:** aprovada pelo responsável · **SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md`
**Harness:** `docs/ENTREGAS_AGENDA_PROVA_HARNESS.md` · **Goal:** GOAL-017

## 1. Problema

O estaleiro entrega material com guia de remessa (`GR`), mas ninguém sabe que entregas há amanhã nem em
que camião vão. Quando o cliente diz "não recebi", não há prova. E quando sobram sacos na obra e o camião
os traz de volta, o stock e a fatura ficam errados.

O frete por zona (Ctrl+Alt+F, serviço `FRT-`) já está feito e não muda.

## 2. Decisões do responsável (2026-11-28)

| Tema | Decisão |
|---|---|
| Agenda | Cada entrega tem **dia**, **período** (manhã/tarde) e **camião**. A agenda mostra as entregas de cada dia por camião |
| Prova de entrega | **Nome de quem recebeu obrigatório**; assinatura e foto **opcionais** |
| Devolução na obra | O motorista regista o que voltou; o **stock volta quando o camião chega ao armazém**; se já estava faturado, o sistema **propõe uma nota de crédito que o gestor aprova** |

## 3. Regras

### 3.1 Agenda

- A entrega pertence à **guia de remessa aprovada** (`DeliveryGuide`, estado `APPROVED`). Guias pendentes,
  rejeitadas ou canceladas não se agendam.
- Campos: `scheduledDate` (data, não pode ser anterior a hoje ao agendar), `scheduledPeriod`
  (`MORNING` "Manhã" / `AFTERNOON` "Tarde") e `vehicleId` (viatura activa da frota, da mesma empresa).
- Estado da entrega (`DeliveryStatus`, separado do estado da guia): `UNSCHEDULED` "Por agendar" →
  `SCHEDULED` "Agendada" → `DELIVERED` "Entregue" ou `NOT_DELIVERED` "Não entregue".
- Reagendar é possível enquanto não está `DELIVERED`; `NOT_DELIVERED` volta a `SCHEDULED` ao reagendar.
- "Não entregue" exige motivo (texto, até 255 caracteres).
- A agenda de um dia lista as entregas agendadas por camião e período, com cliente, morada, contacto e
  número da guia. Filtro por dia (por omissão, amanhã) e por camião.
- Agendar não mexe no stock (o stock já saiu na aprovação da guia).

### 3.2 Prova de entrega

- Marcar como `DELIVERED` exige `receiverName` (nome de quem recebeu, 2–120 caracteres, sem espaços só).
- Opcionais: `receiverDocument` (BI/NUIT de quem recebeu, até 40 caracteres), assinatura e foto.
- Assinatura e foto são imagens PNG/JPEG validadas pelo `ImagePayloadValidator` existente (tipo real e
  tamanho máximo), guardadas com a entrega, nunca no disco do servidor.
- Guarda-se quem registou e quando (`deliveredAt`, `deliveredBy`). A data/hora de entrega é a do registo.
- Depois de `DELIVERED`, a prova não se altera (só se acrescenta devolução, §3.3). Auditoria
  `DELIVERY_CONFIRM`.
- A prova aparece no detalhe da guia e no PDF da guia (nome, data/hora e, se existir, a assinatura).

### 3.3 Devolução na obra

- Só numa entrega `DELIVERED`, no mesmo registo da entrega ou depois, enquanto a guia não tiver uma
  devolução recebida no armazém.
- Por linha da guia: quantidade devolvida, `0 ≤ devolvida ≤ quantidade da linha`. Motivo obrigatório
  (texto). Pelo menos uma linha com quantidade > 0.
- A devolução nasce `PENDING_RECEIPT` "A caminho do armazém" e **não mexe no stock**.
- **Receber no armazém** (gestão: ADMIN/MANAGER, ou quem pode aprovar guias): passa a `RECEIVED` e regista
  movimento de stock `RETURN` de cada quantidade devolvida no **armazém da guia**, com o lote da linha.
  Auditoria `SITE_RETURN_RECEIVE`. Receber duas vezes é recusado.
- Cancelar uma devolução `PENDING_RECEIPT` (gestão, com motivo) não mexe no stock.
- **Guia já faturada** (`invoiceId` preenchido) quando a devolução é recebida: o sistema cria uma
  **nota de crédito em rascunho/pendente** sobre essa fatura, com as linhas e quantidades devolvidas ao
  preço da fatura, motivo "Devolução na obra — Guia {número}", para o gestor aprovar ou rejeitar pelo
  circuito normal das notas de crédito.
  - Essa nota fica marcada **`stockAlreadyReturned = true`**: ao ser aprovada **não volta a mexer no
    stock** (o stock já voltou na recepção). Sem esta marca, as notas comportam-se como hoje.
  - As regras existentes das notas mantêm-se (só sobre faturas válidas, guardas de linha, etc.). Se a nota
    não puder ser criada por uma dessas regras, a recepção do stock continua válida e o gestor recebe um
    aviso no sino para tratar a nota à mão.
- **Guia ainda não faturada** quando a devolução é recebida: ao faturar a guia, cada linha fatura
  **quantidade entregue − quantidade devolvida recebida**; linhas que ficam a zero não entram na fatura.

### 3.4 Permissões e empresa

- Agendar, reagendar e registar entrega/não entrega/devolução: os mesmos perfis que hoje vêem e trabalham
  as guias de remessa.
- Receber a devolução no armazém e cancelar devolução: ADMIN/MANAGER (ou quem aprova guias).
- Tudo sempre na empresa activa; guia, viatura ou entrega de outra empresa → recusado.
- Mensagens ao utilizador em PT-MZ, via `BusinessRuleException`.

### 3.5 Desktop

- Comercial › Guias de remessa: acções **Agendar entrega**, **Registar entrega** (nome obrigatório;
  assinatura desenhada com o rato/caneta num quadro de assinatura ou imagem escolhida; foto opcional),
  **Não entregue**, **Devolução na obra** e, para a gestão, **Receber devolução**.
- Novo separador **Agenda de Entregas**: escolher dia e camião, ver as entregas por período, estado com
  `StatusTone`, e abrir a guia.
- Padrões do projecto: `DialogChrome`, `ButtonStyles`, `UIHelper.icon`, `Typography`, validação no campo
  (`FieldValidationException`), sem emojis.

## 4. Fora de âmbito

- Aplicação de telemóvel para o motorista e GPS.
- Hora exacta de entrega (decidido: só manhã/tarde).
- Mudanças ao frete por zona e aos manifestos da frota.
- Devolução de material de faturas sem guia (continua pela nota de crédito normal).
