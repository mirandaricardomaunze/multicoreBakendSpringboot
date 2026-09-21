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
| DE-04 | `TableExportPrintServiceTest` — listagem exportada do desktop | O texto do PDF traz nome, NUIT, morada, telefone e email da empresa (TE-01..TE-06) |

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
  Tel: +258 84 123 4567 / contacto@multicore.co.mz` **+ imagem embutida** (`/Image` presente).
- **DE-52 (recibo POS):** mesmo conjunto no cabeçalho térmico + logótipo embutido.
- **DE-54:** empresa sem logo/telefone (PT) → PDF gera na mesma (sem imagem, sem linha de telefone, sem crash).
- **DE-55:** logótipo com bytes inválidos → PDF gera na mesma, **sem exceção** (try/catch no renderer).
- **Verificação determinística:** `scratchpad/PdfVerify.java` (OpenPDF `PdfTextExtractor` + deteção de
  `/Image`) em vez de screenshot — o texto do PDF é a prova.
- Dados de teste (telefone/logo de PT e MZ) repostos a nulo no fim.

## Evidência (execução 2026-08-30, exportação de listagens)

- **A última folha sem cabeçalho fechou-se.** As listagens (*Exportar PDF* em Clientes, Faturas,
  Encomendas, Lotes & Validades e nos separadores do RH) eram desenhadas **pelo desktop**, com uma
  cópia própria do `TablePdfExporter`: saíam com título e tabela, e mais nada — sem nome, sem NUIT,
  sem morada. O desktop é um cliente fino e não tem a empresa; tem-na o servidor.
- A listagem passou a subir para `POST /api/print/table` (`TableExportPrintService`) e a descer em
  PDF pelo mesmo `CompanyHeaderRenderer` da factura e da guia. A cópia do desktop foi apagada e o
  OpenPDF ficou em `scope=test` no `desktop/pom.xml` — o cliente lê e imprime PDF, não o compõe.
- **DE-04:** `TableExportPrintServiceTest` extrai o texto do PDF e exige lá os cinco campos da
  empresa; cobre também linhas curtas/compridas, listagem vazia e os tectos de 20 000 linhas /
  40 colunas. `MultiModuleArchitectureHarnessTest.desktopDoesNotDrawPdfDocuments` impede o regresso.
- **Por confirmar na loja, e não é da IA:** a exportação nunca foi vista contra um backend a correr
  — o PDF de uma listagem real, com o logótipo da empresa, ainda não saiu no papel.

**Nota:** limpar um logótipo pelo endpoint (POST com corpo vazio) devolve 500 — a UI nunca envia vazio,
por isso fica fora de âmbito (substituir funciona; para limpar, actualizar via BD ou um futuro DELETE).
