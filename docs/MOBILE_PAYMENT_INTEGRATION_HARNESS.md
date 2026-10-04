# HARNESS-MPI-001: Matriz de Testes para Pagamento Móvel (M-Pesa & e-Mola)

> **Documento Canónico de Testes e Validação**  
> **Referência Técnica:** `docs/MOBILE_PAYMENT_INTEGRATION_SPEC.md` (`SPEC-MPI-001`)  
> **Data:** 2026-09-25

---

## Matriz de Cobertura de Testes

| ID | Cenário | Entrada | Resultado Esperado | Camada |
| :--- | :--- | :--- | :--- | :--- |
| **MPI-01** | Validação e normalização de número Vodacom M-Pesa | `"+258 84 123 4567"` ou `"851234567"` | Normalizado para `"841234567"` / `"851234567"`; aceita operadora `MPESA`. | Backend / Core |
| **MPI-02** | Validação e normalização de número Movitel e-Mola | `"861234567"` ou `"+258 87 987 6543"` | Normalizado para `"861234567"` / `"879876543"`; aceita operadora `EMOLA`. | Backend / Core |
| **MPI-03** | Rejeição de número incompatível com a operadora selecionada | Provedor `MPESA` com número `"861234567"` (Movitel) | Lança `BusinessRuleException` indicando número inválido para M-Pesa. | Backend / Service |
| **MPI-04** | Iniciação de transação e geração de ID único | Pedido válido com 150 MT no M-Pesa | Retorna status `PENDING`, ID único gerado e persistido na BD. | Backend / Integration |
| **MPI-05** | Consulta de status e transição para `SUCCESS` | Transação em polling | Status passa a `SUCCESS`, gera referência financeira (ex.: `MP...`). | Backend / Service |
| **MPI-06** | Isolamento Multi-Tenant da transação | Consulta com `companyId` diferente | Retorna 404 ou não encontrado; dados não vazam entre empresas. | Backend / Security |
| **MPI-07** | Validador UI de telemóvel Moçambicano no Desktop | Digitação no campo de telemóvel | Identifica operadora em tempo real e ativa botão de envio. | Desktop / UI |
| **MPI-08** | Integração completa com o checkout do POS | Pagamento aprovado no `MobilePaymentModal` | Preenche referência no `PosPaymentRequest` e retorna sem erro. | Desktop / POS |
