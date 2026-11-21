# Spec — Modelos de documento por empresa

Status: **aprovada**
Effective date: 2026-11-19
Harness: [MODELOS_DOCUMENTO_HARNESS.md](MODELOS_DOCUMENTO_HARNESS.md)
Goal: [GOAL-015](../tasks/goals/GOAL-015-modelos-de-documento.md)

## 1. Problema

As empresas querem que a factura tenha a sua cara: logótipo no sítio certo, a cor da marca, os dados
bancários para transferência e uma nota própria. Hoje já se escolhem as colunas das linhas e um
comentário de rodapé (`DocumentColumnConfig`) e o logótipo da empresa; o resto está fixo no código.

## 2. Decisão do responsável (2026-11-19)

**Opções fixas por empresa**, sem editor visual livre. Os elementos fiscais obrigatórios nunca podem
ser escondidos.

## 3. Regras

### 3.1 Opções (estendem `DocumentColumnConfig`, por empresa e tipo de documento)

- **Cor de destaque**: hexadecimal `#RRGGBB`; recusada se o contraste com branco for inferior a
  4,5:1 (o texto em cima da cor tem de se ler). Omissão: a cor actual do `PdfTheme`.
- **Posição do logótipo**: `LEFT` (omissão), `CENTER` ou `RIGHT`; sem logótipo, nada aparece.
- **Colunas visíveis das linhas**: as que já existem.
- **Rodapé**: o comentário que já existe (até 255 caracteres).
- **Notas livres**: texto até 500 caracteres impresso antes do rodapé (ex.: condições de venda).
- **Dados bancários**: até 3 contas (banco, titular, NIB/IBAN), impressas num bloco próprio; podem
  vir das contas de tesouraria bancárias da empresa.
- **Mostrar QR code** de consulta quando existir (sim/não).

### 3.2 Elementos que nunca se escondem

Em facturas, facturas-recibo, notas de crédito, notas de débito, recibos e vendas POS aparecem
**sempre**, qualquer que seja o modelo:

- nome, NUIT e morada do emitente; nome e NUIT do adquirente (ou "Consumidor Final");
- tipo, série e número do documento; data de emissão (e vencimento quando existir);
- total sem IVA, **IVA por taxa** e total com IVA, mesmo com a coluna de IVA das linhas escondida;
- motivo de isenção quando o IVA é zero;
- a assinatura/hash do documento e a frase de certificação quando existirem;
- a indicação de 2.ª via/cópia.

### 3.3 Aplicação

- Todos os PDFs A4 dos tipos acima usam o modelo da empresa; os talões térmicos usam só logótipo,
  notas e rodapé (não usam cor nem dados bancários).
- O modelo aplica-se a documentos **impressos a partir de agora**, incluindo 2.ª vias de documentos
  antigos (o conteúdo fiscal não muda, só o aspecto).
- Alterar o modelo: **ADMIN**; auditoria `DOCUMENT_TEMPLATE_CHANGE`.

### 3.4 Desktop

- Configurações › Documentos (o ecrã de colunas que já existe): cor com amostra, posição do
  logótipo, notas, contas bancárias, QR, e **pré-visualização** de uma factura de exemplo gerada pelo
  backend com o modelo antes de gravar.

## 4. Fora de âmbito

- Editor visual livre, tipos de letra à escolha, vários modelos por tipo de documento.
- Modelos para relatórios internos (inventário, mapas de RH, etc.).
