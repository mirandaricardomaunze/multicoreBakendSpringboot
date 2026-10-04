# SPEC-NCL-001: Validação Canónica de NUIT Moçambicano e Controlo de Limite de Crédito

## 1. Enquadramento e Princípio de Integridade

O Número Único de Identificação Tributária (NUIT) de Moçambique é a chave fiscal obrigatória para pessoas singulares e colectivas perante a Autoridade Tributária (AT). O ERP deve assegurar:
1. **Validação Estrutural e Algorítmica de NUIT:**
   - Exatamente 9 dígitos numéricos.
   - Rejeição de sequências falsas ou triviais (`111111111`, `000000000`).
   - Verificação algorítmica por Módulo 11 sobre os primeiros 8 dígitos.
   - Suporte controlado a consumidor final genérico (`999999999`).
2. **Controlo Proativo de Limite de Crédito:**
   - Todo cliente com prazo de pagamento > 0 dias (vendas a crédito) está sujeito a verificação de limite de crédito.
   - Se o cliente possuir limite definido e a dívida actual somada ao novo documento ultrapassar esse limite, o sistema bloqueia a submissão a crédito.
   - Se o cliente tiver facturas em atraso há mais de 30 dias, o sistema exige regularização ou autorização expressa.

## 2. Componentes e Validações

1. **`NuitValidator.java`:**
   - Método `isValid(String nuit)`: valida comprimento, padrão numérico, diversidade e dígito de controlo Módulo 11.
   - Mensagens claras em português moçambicano indicando o erro (ex.: "NUIT deve ter 9 dígitos numéricos válidos").
2. **`CustomerCreditValidator.java`:**
   - Método `validateCreditAvailability(BigDecimal currentDebt, BigDecimal creditLimit, BigDecimal newAmount, boolean hasOverdue)`:
     - Devolve diagnóstico de crédito (`APPROVED`, `EXCEEDED_LIMIT`, `OVERDUE_BLOCK`).
3. **Integração nas Telas:**
   - `ClientesPanel.java`: validação em tempo real e na gravação de novo cliente ou edição.
   - `CommercialOrdersView.java` / `ComercialPanel.java`: verificação ao selecionar cliente e submeter encomenda a prazo.

## 3. Harness e Verificação

- `NuitAndCreditLimitValidationHarnessTest` verifica:
  1. Aceitação de NUITs válidos e rejeição de inválidos (tamanho incorreto, letras, sequências repetidas, dígito de controlo errado).
  2. Cálculo de disponibilidade de crédito e bloqueio por mora.
  3. Integração no cadastro de clientes.
