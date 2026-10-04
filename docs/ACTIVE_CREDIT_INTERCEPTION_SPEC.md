# SPEC-ACI-001: Intercepção Activa de Crédito e Risco na Faturação e POS

## 1. Princípio e Enquadramento

A concessão de crédito comercial a clientes ("vendas a prazo" na faturação ou "fiado" no ponto de venda) é um dos maiores vetores de risco operacional e liquidez de qualquer empresa.
Para evitar a emissão inadvertida de faturas ou finalização de vendas de balcão para clientes insolventes, em mora prolongada ou com limite esgotado:
- O ERP Multicore deve validar proactivamente as condições de crédito no momento da preparação do documento, antes de consumir números fiscais ou registar saídas de caixa.
- Clientes avulsos (não cadastrados / walk-in) nunca podem beneficiar de crédito.
- Clientes com limite de crédito estritamente igual a 0,00 MT estão configurados para pronto pagamento exclusivo e devem ser imediatamente advertidos/bloqueados.
- Caso o limite ou mora seja ultrapassado, o sistema deve apresentar mensagens claras em português de Moçambique com os valores envolvidos (limite, dívida actual, disponível e valor da nova venda).

## 2. Comportamento e Apresentação Canónica

1. **Ponto de Venda (POS — `POSPanel`):**
   - Ao assinalar a opção `"Venda a crédito (fiado)"`:
     - Se o cliente for avulso/não registado: a finalização é interrompida com o aviso `"Vendas a crédito (fiado) exigem a seleção de um cliente cadastrado."`
     - Se o cliente tiver `creditLimit == 0,00 MT`: a venda é interrompida com o aviso `"O cliente '{Nome}' não tem autorização para vendas a crédito (limite de 0,00 MT)."`
     - O erro de crédito do backend é interceptado e exibido com destaque e foco, sem corromper o carrinho.

2. **Faturação Comercial (`ComercialPanel` / `CommercialInvoicesView`):**
   - Na emissão de fatura a partir do editor (`saveInvoiceFromEditor`):
     - Verifica o limite de crédito do cliente seleccionado.
     - Se o limite for zero (pronto pagamento), emite alerta claro de que o cliente não tem crédito atribuído.
     - Validação através do motor canónico `CustomerCreditValidator`.

3. **Garantia de Semântica e Formatação:**
   - Valores monetários sempre formatados com 2 casas decimais e sufixo `" MT"`.
   - Proibição absoluta de emojis ou caracteres especiais Unicode.
   - Respeito pelo isolamento das camadas (`contracts`, `desktop`, `backend`).

## 3. Harness e Verificação

- `ActiveCreditInterceptionHarnessTest` verifica:
  1. Rejeição de venda a crédito para cliente nulo / avulso.
  2. Bloqueio imediato para cliente com limite de crédito 0,00 MT.
  3. Comportamento da validação em `POSPanel` e `ComercialPanel`.
  4. Respeito às convenções de texto e ausência de emojis.
