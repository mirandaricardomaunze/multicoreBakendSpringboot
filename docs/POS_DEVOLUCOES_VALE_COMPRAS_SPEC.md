# Especificação Técnica: Devoluções e Trocas no POS com Emissão de Vale de Compras (Store Credit)

**Código:** `SPEC-POS-VOUCHER-001`  
**Domínio:** POS & Comercial  
**Data:** 2026-09-27  
**Estado:** Proposta Canónica  

---

## 1. Objectivo de Negócio

No retalho e frentes de loja em Moçambique, a gestão de devoluções e trocas de produtos exige flexibilidade financeira e retenção de valor para a empresa:
1. **Identificação da Venda de Origem:** O operador pode selecionar a venda no histórico do POS ou pesquisar diretamente pelo número da fatura (`invoiceNumber`).
2. **Devolução Parcial ou Total de Linhas:** O operador seleciona as quantidades específicas de cada artigo a devolver, com reposição automática de stock no armazém indicado.
3. **Método de Compensação Flexível:**
   - **Reembolso em Dinheiro (`CASH`):** Movimento de saída na gaveta de numerário (`REFUND`) validando o saldo disponível.
   - **Reembolso Eletrónico / Bancário (`CARD`, `BANK_TRANSFER`, `MPESA`, `EMOLA`):** Estorno via conta de tesouraria configurada.
   - **Crédito em Conta Corrente (`CREDIT`):** Para clientes cadastrados na base de dados.
   - **Emissão de Vale de Compras (`STORE_CREDIT`):** Emissão de um vale alfanumérico ao portador ou nominal, com validade (padrão 90 dias) e saldo em Meticais (MT), permitindo que o cliente utilize o crédito em compras futuras sem perda de liquidez imediata para o comerciante.
4. **Troca Imediata:** Ao emitir a devolução, o operador pode transitar imediatamente para o ecrã de venda para registar os artigos de substituição.
5. **Utilização do Vale no Checkout:** No momento do pagamento de qualquer venda no POS, o cliente pode apresentar o código do vale. O sistema valida a autenticidade, vigência e saldo disponível, abatendo o valor da venda e atualizando o saldo remanescente.
6. **Comprovativo / Talão Térmico:** Impressão em formato 80mm (ou A4) com dados do vale, valor, código de barras legível por scanner, validade e termos de uso.

---

## 2. Ciclo de Vida do Vale de Compras (`StoreVoucher`)

```
                  ┌──────────────────────┐
                  │   Devolução no POS   │
                  │ (Método STORE_CREDIT)│
                  └──────────┬───────────┘
                             │
                             ▼
                  ┌──────────────────────┐
                  │    ACTIVE (Ativo)    │
                  │ saldo == saldo inicial│
                  └──────────┬───────────┘
                             │
             ┌───────────────┴───────────────┐
             │ Abate parcial                 │ Abate total
             ▼                               ▼
  ┌──────────────────────┐        ┌──────────────────────┐
  │   PARTIALLY_USED     │        │    FULLY_REDEEMED    │
  │ saldo > 0 e < inicial│        │      saldo == 0      │
  └──────────┬───────────┘        └──────────────────────┘
             │                               ▲
             └─────── Abate remanescente ────┘
                             │
                Se expirar (data > expiresAt)
                             ▼
                  ┌──────────────────────┐
                  │       EXPIRED        │
                  └──────────────────────┘
```

---

## 3. Modelo de Dados & Persistência

### 3.1 Migração Flyway (`V74__store_vouchers.sql`)
```sql
CREATE TABLE IF NOT EXISTS store_vouchers (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    initial_amount NUMERIC(14, 2) NOT NULL,
    remaining_amount NUMERIC(14, 2) NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    client_id BIGINT REFERENCES clients(id),
    client_name VARCHAR(150),
    credit_note_id BIGINT REFERENCES credit_notes(id),
    issued_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at DATE NOT NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'ACTIVE',
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_vouchers_code ON store_vouchers(code);
CREATE INDEX IF NOT EXISTS idx_store_vouchers_company ON store_vouchers(company_id);
CREATE INDEX IF NOT EXISTS idx_store_vouchers_client ON store_vouchers(client_id);
CREATE INDEX IF NOT EXISTS idx_store_vouchers_status ON store_vouchers(status);
```

### 3.2 Entidade `StoreVoucher`
- `id`: Chave primária.
- `code`: Código alfanumérico seguro único (ex.: `VALE-7A8B-9C0D`).
- `initialAmount`: Valor original emitido pela nota de crédito.
- `remainingAmount`: Saldo remanescente utilizável.
- `company`: Empresa emitente (isolamento multi-tenant).
- `client`: Cliente associado (opcional se for cliente avulso).
- `clientName`: Nome do beneficiário gravado no documento.
- `creditNote`: Nota de crédito de origem da devolução.
- `issuedAt`: Data e hora de emissão.
- `expiresAt`: Data de expiração (padrão 90 dias após a emissão).
- `status`: `StoreVoucherStatus` (`ACTIVE`, `PARTIALLY_USED`, `FULLY_REDEEMED`, `EXPIRED`, `CANCELLED`).
- `createdBy`: Operador ou utilizador que processou a devolução.

---

## 4. Contratos de API (`contracts`)

### 4.1 Record `StoreVoucherDTO`
```java
public record StoreVoucherDTO(
        Long id,
        String code,
        BigDecimal initialAmount,
        BigDecimal remainingAmount,
        Long companyId,
        Long clientId,
        String clientName,
        Long creditNoteId,
        String creditNoteNumber,
        LocalDateTime issuedAt,
        LocalDate expiresAt,
        String status,
        String createdBy
) {
    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status) || "PARTIALLY_USED".equalsIgnoreCase(status);
    }
}
```

### 4.2 Record `POSReturnResultDTO`
```java
public record POSReturnResultDTO(
        CreditNoteDTO creditNote,
        StoreVoucherDTO voucher,
        String message
) {
    public POSReturnResultDTO(CreditNoteDTO creditNote, StoreVoucherDTO voucher) {
        this(creditNote, voucher, null);
    }
    public POSReturnResultDTO(CreditNoteDTO creditNote) {
        this(creditNote, null, null);
    }
}
```

---

## 5. Regras de Negócio no Backend (`POSService`)

1. **Validação da Permissão:** Exige `PermissionGuard.requireManagerOrAdmin("registar devolução no POS")`.
2. **Método `STORE_CREDIT`:**
   - Criação da `CreditNote` do tipo `RETURN` com linhas e quantidades selecionadas.
   - Aprovação imediata da nota de crédito para repor stock no armazém de destino.
   - Geração de código de vale único e imprevisível (`VALE-XXXX-XXXX`).
   - Persistência do `StoreVoucher` com validade de 90 dias.
   - Registro em log de auditoria com código do vale e valor emitido.
3. **Resgate do Vale no Checkout:**
   - Validação da existência do vale na mesma empresa.
   - Bloqueio se o estado não for `ACTIVE` ou `PARTIALLY_USED`.
   - Bloqueio se a data atual for posterior a `expiresAt` (atualiza estado para `EXPIRED`).
   - Bloqueio se `remainingAmount < amount`.
   - Dedução atómica do saldo remanescente. Se saldo chegar a 0, transita para `FULLY_REDEEMED`.
   - Registo em `PaymentEntry` com método `STORE_CREDIT` e referência igual ao código do vale.
   - Auditoria registada em `auditLogService`.

---

## 6. Interface Gráfica Desktop

1. **`PosReturnDialog`:**
   - Suporte a seleção de venda na tabela ou busca pelo número da fatura.
   - Opção "VALE DE COMPRAS (Store Credit)" no dropdown de métodos de reembolso.
   - Ao concluir devolução com vale, apresenta diálogo visual com código destacado, saldo, data de validade e opções para imprimir comprovativo térmico ou copiar código.
   - Confirmação de troca imediata para retornar ao catálogo do POS com o carrinho limpo para nova venda.
2. **`PosPaymentDialog`:**
   - Novo método selecionável "Vale de Compras" (`fas-ticket-alt`).
   - Campo para inserção ou leitura por código de barras do código do vale.
   - Botão "Verificar Vale" que valida em tempo real via API, exibindo o saldo disponível e validade.
   - Permite pagamento integral com o vale ou dedução parcial combinada com outros meios (Numerário, Cartão, M-Pesa).
