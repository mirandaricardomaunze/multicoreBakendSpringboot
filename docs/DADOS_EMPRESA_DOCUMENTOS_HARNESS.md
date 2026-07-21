# Harness — Dados completos da empresa nos documentos

Ver [DADOS_EMPRESA_DOCUMENTOS_SPEC.md](DADOS_EMPRESA_DOCUMENTOS_SPEC.md).

Os serviços de impressão não têm testes automáticos (geram PDF; validação visual, como os restantes
do projeto). Cobertura = compilação + testes tocados verdes + verificação **visual ao vivo** de um PDF.

## Automáticos
| ID | Cenário | Esperado |
|----|---------|----------|
| DE-01 | `mvn -o compile` após entidade/migração/renderer/serviço | BUILD SUCCESS |
| DE-02 | Suite tocada (Platform/Comercial/POS) | verde |
| DE-03 | `V33` aplica em PostgreSQL real | Flyway success; `companies` tem `phone` + `logo` |

## Manuais (ao vivo)
| ID | Passos | Esperado |
|----|--------|----------|
| DE-50 | Superadmin: editar empresa com Telefone + carregar logótipo | 200; `hasLogo=true`, telefone gravado |
| DE-51 | Gerar PDF de **Fatura** dessa empresa | Cabeçalho mostra Logo + Nome + NUIT + Morada + Telefone + Email |
| DE-52 | Gerar **Recibo POS** | Cabeçalho térmico centra Logo + os mesmos dados |
| DE-53 | Gerar mais 2 tipos (ex.: Encomenda, Relatório de stock) | Mesmo cabeçalho completo (via renderer partilhado) |
| DE-54 | Empresa **sem** logótipo / sem telefone | Documento sai na mesma, sem imagem e sem linhas em branco (à prova de falha) |
| DE-55 | Logótipo com bytes inválidos | PDF gera sem imagem, sem exceção |

## Evidência (execução 2026-07-21, backend prod / PostgreSQL real)
- **DE-01/02:** `mvn -o compile` limpo; testes tocados (Platform/Comercial/POS + regressão) **50, 0 falhas**.
- **DE-03:** `V33` aplicada — `companies` tem `phone` + `logo`.
- **DE-50:** superadmin definiu telefone `+258 84 123 4567` + logótipo na empresa MZ → `hasLogo=true`.
- **DE-51 (fatura A4):** extração de texto do PDF confirma o cabeçalho completo —
  `Multicore Moçambique Lda / NUIT: 400123456 / Avenida 24 de Julho 1500, Maputo /
  Tel: +258 84 123 4567 / contacto@phcpro.co.mz` **+ imagem embutida** (`/Image` presente).
- **DE-52 (recibo POS):** mesmo conjunto no cabeçalho térmico + logótipo embutido.
- **DE-54:** empresa sem logo/telefone (PT) → PDF gera na mesma (sem imagem, sem linha de telefone, sem crash).
- **DE-55:** logótipo com bytes inválidos → PDF gera na mesma, **sem exceção** (try/catch no renderer).
- **Verificação determinística:** `scratchpad/PdfVerify.java` (OpenPDF `PdfTextExtractor` + deteção de
  `/Image`) em vez de screenshot — o texto do PDF é a prova.
- Dados de teste (telefone/logo de PT e MZ) repostos a nulo no fim.

**Nota:** limpar um logótipo pelo endpoint (POST com corpo vazio) devolve 500 — a UI nunca envia vazio,
por isso fica fora de âmbito (substituir funciona; para limpar, actualizar via BD ou um futuro DELETE).
