# Especificação de Validação de Dados e Expressões Regulares (Frontend & Backend)

## 1. Visão Geral e Objectivo
Esta especificação estabelece a fonte única de verdade (Single Source of Truth) para validação de dados, expressões regulares (regex) e filtragem reactiva de digitação no ERP Multicore em Moçambique, abrangendo tanto as fronteiras HTTP / serviços de negócio (Backend) quanto os formulários Swing interactivos (Frontend Desktop).

O objectivo é:
1. **Garantir a integridade estrita dos dados** fiscais, comerciais e operacionais (NUIT, Bilhete de Identidade, Telefones moçambicanos, Emails, Barcodes e SKUs).
2. **Prevenir erros no momento da digitação**, impedindo caracteres inválidos de entrarem nos campos (ex.: apenas algarismos em NUIT e telefone).
3. **Fornecer feedback visual inline imediato** em português de Moçambique, sem fechar diálogos modais nem fazer o operador perder dados digitados.
4. **Respeitar os padrões fiscais da Autoridade Tributária de Moçambique (AT)**, incluindo o algoritmo de dígito de controlo do NUIT (Módulo 11) e o NUIT canónico de consumidor final (`999999999`).

---

## 2. Padrões Canónicos no Módulo `contracts`

A classe `ValidationPatterns` em `contracts` (`mz.multicore.erp.architecture.validation.ValidationPatterns`) centraliza todas as expressões regulares e algoritmos de validação:

| Campo | Regex / Algoritmo | Descrição e Regras | Exemplo Válido | Exemplo Inválido |
| :--- | :--- | :--- | :--- | :--- |
| **Email** | `^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\.[A-Za-z]{2,})$` | Validação de sintaxe RFC 5322 simplificada com domínio e TLD mínimo de 2 caracteres. | `contacto@empresa.co.mz` | `invalido@`, `@dominio.com` |
| **NUIT** | `^\d{9}$` + Módulo 11 | Exatamente 9 dígitos numéricos. Verificação de dígito de controlo com pesos `[9, 8, 7, 6, 5, 4, 3, 2]`. Consumidor final `999999999` é aceite. | `100123458`, `999999999` | `12345`, `abcdefghi`, dígito inválido |
| **Telefone MZ** | `^(?:\+?258[\s-]?)?(8[2-7]\d{7}\|2[1-8]\d{6})$` | Operadoras móveis (82/83 Tmcel, 84/85 Vodacom, 86/87 Movitel) e redes fixas nacionais (21 a 28). Aceita prefixo internacional opcional `+258` ou `258`. | `841234567`, `+258 84 123 4567` | `811234567`, `12345` |
| **B.I. MZ** | `^\d{12}[A-Z]$` | Bilhete de Identidade moçambicano: 12 dígitos seguidos de 1 letra maiúscula de controlo. | `110100234567B` | `1101002345678`, `123A` |
| **Código de Barras** | `^[A-Za-z0-9\-_]{4,30}$` | EAN-13, EAN-8, UPC ou alfanumérico padrão de leitor óptico industrial (4 a 30 caracteres). | `6001234567890`, `PROD-001` | `ab`, caracteres especiais ilegais |
| **SKU / Código Artigo** | `^[A-Za-z0-9\-_]{2,30}$` | Identificador interno de produto limpo (2 a 30 caracteres). | `ART-00123`, `P100` | `?`, `*` |
| **Montante Monetário** | `amount != null && amount > 0` | Valida valores monetários estritamente positivos (`BigDecimal`), evitando números negativos ou nulos. | `150.00 MT` | `-10.00 MT`, `null` |
| **Percentagem** | `0 <= pct <= 100` | Margens de desconto, impostos (ex.: IVA 16%) e comissões. | `16.00%` | `120%`, `-5%` |

---

## 3. Filtragem Reactiva de Entrada no Desktop (`UIHelper`)

Para evitar que o utilizador insira caracteres espúrios (como letras em NUIT ou caracteres minúsculos onde a norma exige maiúsculas), foram implementados `DocumentFilter` reactivos em tempo real:

1. **`UIHelper.installDigitsOnlyFilter(JTextComponent comp, int maxLength)`**:
   - Intercepta inserções (`insertString`) e substituições (`replace`).
   - Rejeita qualquer carácter que não pertença a `0-9`.
   - Limita o comprimento máximo do campo (ex.: 9 para NUIT, 12 para telefone).
2. **`UIHelper.installUppercaseFilter(JTextComponent comp, int maxLength)`**:
   - Converte automaticamente letras minúsculas para maiúsculas no momento da digitação.
   - Ideal para B.I., SKU e códigos de série.

---

## 4. Validação Declarativa em Formulários (`FormField`)

O componente `FormField` (`mz.multicore.erp.gui.components.FormField`) providencia validações visuais encadeadas sem fechar modais nem interromper o fluxo de trabalho:

- **`validateRequired()`**: Assegura que o campo preenchido não está vazio quando `required == true`.
- **`validateMinLength(int minLength, String fieldName)`**: Verifica tamanho mínimo com mensagem amigável.
- **`validateEmail()`**: Valida o padrão canónico de email.
- **`validateNuit()`**: Valida os 9 dígitos e o algoritmo Módulo 11 da AT.
- **`validatePhone()`**: Valida o telefone nacional móvel ou fixo.
- **`validateRegex(Pattern pattern, String errorMessage)`**: Validação genérica com regex configurada e mensagem em português de Moçambique.

Ao falhar qualquer validação:
1. O campo de entrada é contornado visualmente a vermelho (`UIHelper.markFieldInvalid`).
2. A etiqueta de erro vermelha com ícone exibe o motivo específico logo abaixo do campo.
3. O foco do cursor é posicionado no campo com erro e o modal não fecha, protegendo o tempo do operador.

---

## 5. Validação no Backend

No backend:
1. **DTOs de Contrato**: Anotações `@NotBlank`, `@Pattern(regexp = ValidationPatterns.NUIT_REGEX)`, `@Email`, `@Min`, `@Max`, `@DecimalMin`.
2. **Serviços de Domínio**: `TaxIdValidator.validate(taxId)` usa directamente `ValidationPatterns.NUIT_PATTERN`, disparando `BusinessRuleException` em caso de violação com mensagem clara para a API.
