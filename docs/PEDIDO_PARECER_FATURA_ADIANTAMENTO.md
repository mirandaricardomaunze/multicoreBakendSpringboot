# Pedido de parecer — Fatura de adiantamento (sinal de cliente)

**Para:** jurista e/ou contabilista certificado da empresa
**De:** Multicore ERP — responsável pelo produto
**Data:** 2026-11-10 · **Referência interna:** `docs/ADIANTAMENTOS_SPEC.md` (SA-C10)

## 1. Contexto

Clientes pagam parte do valor antes da entrega: o sinal de um bolo de aniversário, carne para uma festa, um
empreiteiro que adianta dinheiro antes de começar a obra. O Multicore passou a tratar estes valores com documentos
fiscais normais, sem documentos internos paralelos. Pedimos confirmação de que o circuito abaixo cumpre a
legislação moçambicana (Código do IVA e regras de facturação/SAF-T da AT) e, se não cumprir, o que mudar.

## 2. Circuito actual

Exemplo: sinal de **3 000 MT** (IVA 16% incluído) e fatura final de **10 000 MT** + IVA.

| Passo | Documento emitido | Efeito |
|---|---|---|
| 1. Recebe o sinal | **Fatura (FT)** do artigo "Adiantamento", serviço sem stock, com IVA à taxa normal: base 2 586,21 + IVA 413,79 = 3 000,00 | IVA liquidado no recebimento |
| 2. Pagamento no acto | **Recibo (RC)** de 3 000,00 | Entrada em caixa ou banco |
| 3. Entrega | **Fatura (FT)** final da encomenda: 10 000,00 + IVA 1 600,00 | IVA da venda |
| 4. Regularização do sinal | **Nota de crédito (NC)** sobre a fatura do passo 1, motivo "Regularização de adiantamento aplicado na fatura FT …" | Anula base e IVA do adiantamento |
| 5. Compensação | **Recibo de compensação** de 3 000,00 sobre a fatura final, forma "Compensação de adiantamento", sem movimento de caixa | O cliente fica a dever 8 600,00 |

Resultado: IVA total = IVA da fatura final; a caixa só registou o dinheiro uma vez; a conta de clientes (2101) fica
com o valor ainda em dívida.

## 3. Perguntas

1. **Documento no recebimento do sinal.** A emissão de uma **fatura** (FT) com IVA no momento do recebimento do
   adiantamento é o documento correcto em Moçambique? Ou deve ser **fatura-recibo**, ou outro documento?
2. **Exigibilidade do IVA.** Confirma que o IVA é exigível no recebimento do adiantamento (e não só na entrega)?
3. **Descrição na fatura do sinal.** Que texto deve constar? Proposta: "Adiantamento por conta da encomenda
   EC … — [descrição dos bens/serviços]". É obrigatório identificar os bens ou serviços futuros?
4. **Regularização.** Usar uma **nota de crédito** sobre a fatura do adiantamento, emitida na data da fatura final,
   é aceite? Ou a lei prefere que a **fatura final deduza o adiantamento** numa linha negativa, com referência à
   fatura do sinal?
5. **Recibo de compensação.** O recibo que compensa o adiantamento na fatura final, sem entrada de dinheiro, é um
   documento aceite? Como deve ser classificado no **SAF-T** (meio de pagamento)?
6. **Cliente desiste.** Se o cliente desiste e o sinal é devolvido: nota de crédito + devolução do dinheiro. Se o
   sinal **não** é devolvido (fica como penalização): que documento emitir e que IVA se aplica?
7. **Taxas reduzidas ou isenções.** Se o bem final tiver taxa diferente da normal (ou isenção), o adiantamento deve
   usar a taxa do bem final em vez da taxa normal?
8. **Consumidor final.** O sistema exige cliente identificado (nome e NUIT) para receber sinal. É obrigatório, ou
   aceita-se Consumidor Final abaixo de um certo valor?

## 4. O que precisamos de volta

- Resposta a cada pergunta, com a base legal.
- Se houver mudanças: a **redacção exacta** dos textos a imprimir nos documentos.
- Confirmação final, por escrito, para registarmos em `docs/ADIANTAMENTOS_SPEC.md` e fecharmos a decisão em aberto
  §7.2 de `docs/SECTORES_ACTIVIDADE_SPEC.md`.

Junto: exemplos impressos de cada documento do circuito (gerar no Multicore em ambiente de teste e anexar).

Contacto: ______________________ Prazo pretendido: ___/___/______
