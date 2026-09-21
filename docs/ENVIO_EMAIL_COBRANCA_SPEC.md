# Especificação Canónica: Envio Directo de Extratos e Cobranças por Email
**Código:** SPEC-EEC-001  
**Módulos:** `contracts`, `backend`, `desktop`  
**Data:** 2026-09-17  
**Estado:** APROVADO / EM IMPLEMENTAÇÃO  

---

## 1. Objectivo e Justificação
No ecossistema de gestão e faturação em Moçambique, a cobrança pró-ativa e a reconciliação de contas correntes exigem a comunicação direta de extratos financeiros oficiais aos clientes sem necessidade de exportar manualmente para o ambiente de trabalho e redigir e-mails externos.

O **Envio Directo de Extratos por Email (SMTP com PDF Anexado)** permite:
1. Enviar com 1 clique o extrato de conta corrente em PDF oficial A4 diretamente para o e-mail cadastrado do cliente.
2. Permitir ao operador ajustar ou personalizar o e-mail de destino, assunto e nota de acompanhamento amigável.
3. Gerar o documento probatório em PDF no backend via `CustomerStatementPrintService` e anexá-lo de forma segura e atómica (`MimeMessageHelper`).
4. Operar em modo de resiliência (se o servidor SMTP não estiver configurado no ambiente de desenvolvimento/teste, regista a operação com sucesso simulado e log de auditoria, garantindo que o ERP nunca bloqueia).

---

## 2. Contratos REST (`contracts`)

```java
public record SendStatementEmailRequest(
    Long clientId,
    String recipientEmail,
    String subject,
    String messageNote,
    LocalDate startDate,
    LocalDate endDate
) {}

public record EmailDispatchResultDTO(
    boolean success,
    String message,
    String recipientEmail,
    String messageId
) {}
```

---

## 3. Arquitetura do Backend (`backend`)
- **Dependência:** `spring-boot-starter-mail` em `backend/pom.xml`.
- **Serviço:** `mz.multicore.erp.modules.comercial.service.CustomerStatementMailService`
  - Injeta `CustomerStatementPrintService` e `CustomerStatementService`.
  - Injeção opcional de `JavaMailSender` (com fallback graceful para simulação).
  - Validação de endereço de e-mail (RFC 5322 regex).
  - Anexo de PDF gerado em memória (`ByteArrayResource`) com cabeçalho corporativo e extrato completo.
- **Endpoint:** `POST /api/comercial/statements/customer/email`

---

## 4. Interface Desktop (`desktop`)
- **Cliente HTTP:** `AccountStatementApiClient.sendStatementEmail(SendStatementEmailRequest request)`
- **UI:** Diálogo `SendStatementEmailDialog` acionado por botão *"Enviar por Email"* em `CustomerStatementPanel`:
  - Campo de e-mail pré-preenchido com o e-mail do cliente selecionado.
  - Assunto sugerido: `Extrato de Conta Corrente - {Nome do Cliente}`.
  - Caixa de texto para nota de cobrança amigável.
  - Indicador de envio assíncrono (`SwingWorker`) e feedback via `ToastManager`.
