# Dados completos da empresa em todos os documentos (Telefone + Logótipo)

## Objectivo

Todos os documentos imprimíveis devem apresentar o **conjunto completo de dados da empresa**:
**Logótipo · Nome · NUIT · Morada · Telefone · Email**. Hoje a `Company` só tem Nome/NUIT/Morada/Email
e não há Telefone nem Logótipo; os documentos mostram o que existe.

## Estado actual (auditoria)

- **Documentos A4** (Fatura, Encomenda, NC, ND, Guia, Recibo comercial, Relatórios de stock/inventário,
  Etiquetas, Mapa fiscal, Declaração de IVA, Fecho Z, Transferências) partilham
  `CompanyHeaderRenderer` → **uma alteração ao renderer cobre todos** (DRY).
- **Recibo POS** (`ReceiptPrintService`) tem cabeçalho térmico próprio (centrado, ~80 mm) — alterado à parte.
- `Company`: `name`, `taxId` (NUIT), `email`, `address`. **Faltam** `phone` e `logo`.
- Empresas são geridas pelo **superadmin** (`/api/platform/companies`); é aí que os novos campos entram.

## Âmbito

### 1. Dados (`Company` + migração `V33`)
- `Company.phone` (`String`, opcional, ≤40) — telefone/contacto.
- `Company.logo` (`byte[]`, `bytea`, opcional) — logótipo (imagem reduzida, espelha `Product.imageData`).
- `V33__company_contact_and_logo.sql`: `add column phone varchar(40)`, `add column logo bytea`.

### 2. Renderização (todos os documentos)
- `CompanyHeaderRenderer.build(company, título, número)`: acrescenta **Telefone** e **Email** ao bloco
  de dados e, quando há `logo`, desenha a **imagem** à esquerda do nome (escalada, altura fixa). Cobre
  automaticamente todos os documentos A4. Robusto a `logo` inválido (ignora, nunca rebenta o PDF).
- `ReceiptPrintService`: cabeçalho térmico mostra **logótipo centrado** (escalado à largura) + Nome +
  NUIT + Morada + **Telefone** + **Email**.

### 3. Entrada (superadmin)
- `CreateCompanyRequest` + `UpdateCompanyRequest`: `+ phone`.
- `PlatformCompanyService.createCompany/updateCompany`: gravam `phone` (auditado).
- **Logótipo por endpoint próprio** (binário, espelha a imagem de produto):
  `POST /api/platform/companies/{id}/logo` (`application/octet-stream`) →
  `PlatformCompanyService.updateCompanyLogo(id, bytes)` (SUPERADMIN + auditoria).
- `PlatformCompanyDTO`: `+ phone`, `+ boolean hasLogo` (a UI pré-preenche telefone e indica se há logo).
- **UI** `PlataformaPanel` (diálogo criar/editar empresa): campo **Telefone** + **seletor de logótipo**
  (reusa `UIHelper.readScaledImage`, pré-visualização). `PlatformApiClient` ganha `updateCompanyLogo`.

## Fora de âmbito
- Ecrã de edição no lado da loja (tenant) — a gestão continua no superadmin (decisão do utilizador).
- Formatos de imagem exóticos: aceita o que o `ImageIO`/`UIHelper.readScaledImage` já suporta (PNG/JPG).

## Notas de desenho
- **DRY:** o ganho está em `CompanyHeaderRenderer` — um sítio cobre ~13 documentos.
- **À prova de falha:** `logo` nulo/ inválido → documento sai na mesma (sem imagem). Campos vazios não
  imprimem linhas em branco.
- **Migração portável:** só corre em PostgreSQL (perfil prod); os testes usam H2 gerado das entidades.
