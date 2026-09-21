# Matriz de Testes & Harness de Conformidade: Envio de Extratos por Email
**Código:** HARNESS-EEC-001  
**Referência:** `docs/ENVIO_EMAIL_COBRANCA_SPEC.md` (SPEC-EEC-001)  
**Data:** 2026-09-17  

---

## 1. Objectivo
Validar a cadeia completa de envio de extratos de conta corrente por e-mail com anexo PDF, assegurando conformidade de dados, validação de e-mails, anexo de PDF gerado pelo backend e resiliência contra indisponibilidade de servidor SMTP.

---

## 2. Critérios de Avaliação Automatizada

| ID | Área | Descrição do Teste | Critério de Aceitação |
|---|---|---|---|
| **EEC-01** | Validação de Entrada | Rejeição de requisição com e-mail inválido ou vazio | Lança `BusinessRuleException` com mensagem clara |
| **EEC-02** | Geração do Anexo | Renderização do PDF oficial de extrato | Gera bytes PDF válidos com cabeçalho `%PDF-` |
| **EEC-03** | Envio com JavaMail | Despacho quando `JavaMailSender` está disponível | Monta `MimeMessage` com anexo e executa `send()` |
| **EEC-04** | Resiliência SMTP | Fallback seguro se SMTP não estiver configurado | Retorna `EmailDispatchResultDTO` com sucesso simulado e ID rastreável |
| **EEC-05** | API Client Desktop | Chamada HTTP tipada via `AccountStatementApiClient` | Submete requisição POST e recebe `EmailDispatchResultDTO` |
| **EEC-06** | Diálogo Desktop | Instanciação e validação de `SendStatementEmailDialog` | Inicializa campos em modo headless sem falhas |

---

## 3. Testes Automatizados
- Backend: `backend/src/test/java/mz/multicore/erp/modules/comercial/service/CustomerStatementMailHarnessTest.java`
- Desktop: `desktop/src/test/java/mz/multicore/erp/gui/SendStatementEmailDialogTest.java`
