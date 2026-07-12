# Validação SAF-T contra a XSD (certificação)

**Última actualização:** 2026-07-12
**Estado:** infraestrutura feita (validador + serviço + UI + teste). **Falta a XSD oficial da AT-MZ**
para certificar — sem ela a validação reporta "XSD não configurada".

## Objectivo

O `FiscalSalesExportService` já produz um XML **com a estrutura SAF-T**, mas não é **certificado**.
Esta iteração fecha o passo de **validação**: validar a exportação contra a **XSD oficial** da AT-MZ e
mostrar o resultado (válido / lista de erros). Assim que a XSD for fornecida, o export torna-se
verificável antes de submeter.

## Como funciona

1. **Configuração:** `fiscal.saft.xsd-path` aponta para o ficheiro `.xsd` oficial. Vazio = validação
   indisponível (falha segura, com mensagem a explicar).
2. **`SaftValidationService.validateSalesExport(companyId, from, to)`** — gera a exportação do período
   (via `FiscalSalesExportService`) e valida-a.
3. **`SaftXsdValidator.validate(xml, xsd)`** — W3C XML Schema; recolhe **todos** os erros (não pára no
   primeiro), endurecido contra XXE (sem DTD/schema externos). Lógica pura — testável com uma XSD de
   exemplo.
4. **UI (`FiscalPanel`):** botão **"Validar SAF-T"** ao lado de "Exportar SAF-T" — valida o período
   selecionado e mostra "válido" ou os erros (com dica se a XSD não estiver configurada).

## Peças

- `SaftValidationResult` (dto): `xsdConfigured`, `valid`, `errors`, `message`.
- `SaftXsdValidator` (util puro), `SaftValidationService` (`@Value fiscal.saft.xsd-path`).
- `SaftXsdValidatorTest` — valida um XML contra uma XSD de exemplo (caso válido e caso inválido).

## Limite honesto

- **Não é certificação oficial** enquanto a XSD da AT-MZ não for fornecida e o XML não passar sem
  erros. O mecanismo está pronto; falta o schema oficial (input externo). Passos seguintes prováveis:
  obter a XSD, correr a validação, e ajustar campos/ordem do export até "válido".
