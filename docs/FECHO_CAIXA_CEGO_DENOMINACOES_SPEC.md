# Especificação Técnica: Fecho de Caixa Cego (Blind Drop) com Contagem de Notas/Moedas e Apuramento de Quebras/Sobras

**Código:** `SPEC-POS-BLIND-CLOSE-001`  
**Domínio:** POS & Controlo de Caixa  
**Data:** 2026-09-27  
**Estado:** Proposta Canónica  

---

## 1. Objectivo de Negócio

No retalho moderno e na operação de frentes de loja em Moçambique, o fecho de caixa deve obedecer ao princípio estrito de **Fecho Cego (Blind Drop / Blind Close)**:
1. O operador de caixa/gaveta **não deve ver** o valor esperado pelo sistema (saldo teórico) antes de realizar a conferência física do dinheiro.
2. O sistema deve fornecer uma ferramenta prática, rápida e intuitiva para contagem por **denominações oficiais do Metical (MZN)**:
   - **Cédulas / Notas:** 1000 MT, 500 MT, 200 MT, 100 MT, 50 MT, 20 MT.
   - **Moedas Metálicas:** 10 MT, 5 MT, 2 MT, 1 MT, 0.50 MT.
3. À medida que o operador digita as quantidades ou usa botões de incremento rápido (+1, +5, +10), o sistema calcula o subtotal por cédula/moeda e o valor acumulado total em numerário.
4. O operador também pode optar pela introdução rápida de um montante global direto.
5. Se houver divergência (quebra ou sobra):
   - O sistema exige/solicita uma justificação ou nota explicativa para auditoria.
   - O fecho com diferença continua a exigir permissão gerencial (`PermissionGuard.requireManagerOrAdmin("fechar caixa com diferença")`).
6. A discriminação das cédulas e a justificação ficam arquivadas na sessão (`till_sessions`) e saem impressas no **Relatório Z (A4)** e no comprovativo de fecho.

---

## 2. Denominações Oficiais Suportadas (MZN)

| Tipo | Denominação | Representação Numérica |
| :--- | :--- | :--- |
| Cédula | 1000 MT | `1000.00` |
| Cédula | 500 MT | `500.00` |
| Cédula | 200 MT | `200.00` |
| Cédula | 100 MT | `100.00` |
| Cédula | 50 MT | `50.00` |
| Cédula | 20 MT | `20.00` |
| Moeda | 10 MT | `10.00` |
| Moeda | 5 MT | `5.00` |
| Moeda | 2 MT | `2.00` |
| Moeda | 1 MT | `1.00` |
| Moeda | 50 Centavos | `0.50` |

---

## 3. Modelo de Dados & Persistência

### 3.1 Migração Flyway (`V73__pos_blind_close_denominations_and_notes.sql`)
```sql
ALTER TABLE till_sessions ADD COLUMN IF NOT EXISTS closing_notes VARCHAR(500);
ALTER TABLE till_sessions ADD COLUMN IF NOT EXISTS cash_breakdown_json TEXT;
```

### 3.2 Entidade `TillSession`
- `closingNotes` (`String`, máx 500 caracteres): Justificação ou observações do operador/gerente no fecho.
- `cashBreakdownJson` (`String` TEXT): Estrutura JSON com a contagem de cada denominação, ex.:
  ```json
  [
    {"denomination": 1000, "count": 12, "subtotal": 12000.00},
    {"denomination": 500, "count": 8, "subtotal": 4000.00},
    {"denomination": 200, "count": 5, "subtotal": 1000.00}
  ]
  ```

---

## 4. Contratos de API (`contracts`)

### 4.1 Record `CloseSessionRequest`
```java
public record CloseSessionRequest(
        @NotNull(message = "Saldo real de fecho é obrigatório.")
        @PositiveOrZero(message = "Saldo real não pode ser negativo.") BigDecimal closingBalanceReal,
        Long depositAccountId,
        String notes,
        String cashBreakdownJson
) {
    // Construtor retrocompatível
    public CloseSessionRequest(BigDecimal closingBalanceReal, Long depositAccountId) {
        this(closingBalanceReal, depositAccountId, null, null);
    }
}
```

### 4.2 Record `PosZReportDTO`
Adicionados `closingNotes` e `cashBreakdownJson` com preservação estrita de construtores sobrecarregados retrocompatíveis.

---

## 5. Interface Gráfica Desktop (`PosBlindCloseDialog`)

1. **Header Card:** Identificação da sessão, nome do operador e saldo inicial de abertura.
2. **Grelha de Denominações:**
   - Separadores/secções bem demarcados: "Notas" e "Moedas".
   - Cada linha contém: Denominação destacada, botões de incremento rápido (+1, +5), campo de texto de quantidade (inteiro) e subtotal calculado.
3. **Card de Totais Contados:**
   - Destaque em `UIHelper.ACCENT_BLUE` exibindo a contagem física total calculada em tempo real.
   - Alternador rápido para "Introdução Direta de Valor Único" para operadores que já contaram fora do sistema.
4. **Campo de Justificação / Observações:**
   - Visível para anotações do fecho; torna-se enfático com alerta quando houver quebra ou sobra.
5. **Painel Pós-Fecho:**
   - Revelação dos números oficiais da gaveta:
     - Esperado pelo sistema
     - Contado pelo operador
     - Divergência: `0.00 MT` (Verde), `Falta / Quebra` (Vermelho) ou `Sobra` (Azul).
   - Botão direto "Imprimir Relatório Z (A4)".
