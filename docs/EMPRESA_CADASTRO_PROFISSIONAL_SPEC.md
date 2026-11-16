# Registo profissional de empresas (superadmin)

Continua [DADOS_EMPRESA_DOCUMENTOS_SPEC.md](DADOS_EMPRESA_DOCUMENTOS_SPEC.md) (telefone e logótipo).
Harness: [EMPRESA_CADASTRO_PROFISSIONAL_HARNESS.md](EMPRESA_CADASTRO_PROFISSIONAL_HARNESS.md).

## Problema

- O formulário "Nova Empresa" só tinha nome, NUIT, email, endereço livre, telefone e logótipo.
  Faltavam a identidade legal (forma jurídica, matrícula, capital social, alvará, CAE), o regime
  de IVA, a morada estruturada e os contactos.
- O registo vinha em passos soltos: a empresa num ecrã, o administrador noutro, a assinatura noutro.
  Uma empresa podia ficar sem administrador e sem assinatura.
- Não havia forma de criar uma conta bancária fora do seeder de demonstração, por isso a factura de
  uma empresa nova saía sem dados para pagamento.
- O pedido de edição não validava o email nem o telefone, ao contrário do de criação.
- Erro encontrado durante este trabalho: desde `513a0ad` (2026-11-07), o "Consumidor Final" era criado
  numa transacção própria durante a transacção que cria a empresa. Essa transacção não via a empresa,
  e **todo o registo de empresa falhava**.

## Decisões

1. **Perfil profissional opcional em `Company`** (V113, colunas todas nulas): `trade_name`,
   `legal_form`, `registry_number`, `share_capital`, `activity_license`, `activity_code`, `vat_regime`,
   `tax_office`, `neighborhood`, `city`, `province`, `postal_code`, `secondary_phone`, `website`,
   `contact_name`, `contact_phone`. A coluna `address` continua a guardar a rua. Os enums
   `CompanyLegalForm`, `VatRegime` e `MozambiqueProvince` ficam em `contracts`.
2. **Um único contrato `CompanyProfile`**, usado na criação, na edição e na leitura
   (`PlatformCompanyDTO.profile`). Na edição, `profile == null` mantém o perfil gravado.
   Os pedidos antigos (5 e 4 argumentos) continuam válidos.
3. **Os dados bancários não vão para `Company`**: já existem nas contas de tesouraria do tipo BANK,
   que é o que `CompanyBankDetails` imprime na factura. O registo abre essa conta através de
   `TreasuryAccountProvisioningService`.
4. **Registo atómico** com `POST /api/platform/companies/onboard` (`PlatformCompanyOnboardingService`):
   empresa, administrador (perfil ADMIN), assinatura (sem plano indicado: avaliação de 30 dias) e conta
   bancária opcional, tudo numa transacção. O utilizador repetido e a senha fraca são recusados antes de
   gravar o que quer que seja. Os endpoints antigos de criação e edição mantêm-se.
5. **A senha da criação de utilizadores da plataforma passa a seguir `PasswordPolicy`**. Antes, esse
   caminho aceitava qualquer senha.
6. **O "Consumidor Final" de uma empresa nova é criado depois do commit**
   (`@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)`). Se falhar,
   `WalkInClientProvider.getOrCreate()` cria-o na primeira venda.
7. **Documentos**: o título passa a ser o nome comercial, com a razão social por baixo quando os dois
   diferem. Acrescenta-se uma linha legal (forma, matrícula, capital social, alvará, CAE e regime de
   IVA quando não é o normal), a morada composta (`Company.formattedAddress()`), o telefone alternativo
   e o website. O texto vem de `CompanyIdentityText`. Os três documentos A4 comerciais (factura,
   encomenda, cotação) passam a partilhar `CompanyDetailsBlock`, em vez de três cópias.
   `CompanyHeaderRenderer`, o talão térmico, o rodapé da factura e o aviso de cobrança usam a mesma
   morada.
8. **UI**: o diálogo passa a ser um assistente em passos (`CompanyWizardDialog` + `StepIndicator`):
   - **Registo**, em quatro passos: Identificação → Morada e contactos → Dados legais e banco →
     Administrador e plano. Avançar valida o passo actual. "Registar empresa" só aparece no último
     passo, onde se mostra um resumo e se sugere o nome de utilizador.
   - **Edição**: três passos que se podem abrir directamente, com "Guardar" sempre visível.
   - Se o logótipo falhar depois de a empresa estar gravada, o diálogo fecha na mesma e mostra um
     aviso. Tentar de novo duplicaria o NUIT.
   - A tabela mostra o nome comercial, a localidade e o telefone.

## Fora de âmbito

- Morada estruturada no SAF-T (`FiscalSalesXmlBuilder`): fica para quando houver o XSD oficial da AT-MZ.
- Perfil de actividade (sectores, V112) no assistente: é trabalho de outra frente, ainda por integrar.
- Gestão de várias contas bancárias pelo superadmin. Mais contas são abertas na tesouraria da empresa.
