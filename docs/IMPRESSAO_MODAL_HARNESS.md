# Harness — Modal de impressão

> Cenários de verificação da [spec](IMPRESSAO_MODAL_SPEC.md). A parte automatizável são **26
> testes** em três classes; o resto é Swing e verifica-se com a aplicação a correr contra um
> backend real.
>
> - `gui/PrintModalHarnessTest.java` (20) — o modal é o único caminho, e as regras sem janela.
> - `gui/components/PrintPreviewDialogPaintTest.java` (3) — a janela monta e a folha pinta.
> - `gui/components/PdfPrinterOrientationTest.java` (3) — o que se vê e o que sai concordam.
>
> Há ainda um driver manual, `gui/components/PrintPreviewSnapshotDriver` (não corre na suite), que
> fotografa o modal em retrato e em paisagem para inspecção visual.

**Última actualização:** 2026-08-30

## 1. Automatizado — 26 testes

| ID    | Regra protegida                                                                | Teste                                                     |
|-------|---------------------------------------------------------------------------------|-----------------------------------------------------------|
| IM-01 | Nenhum ecrã grava o PDF directamente: `PdfFileSaver` não aparece em `gui` fora de `components` | `nenhumEcraGravaOPdfDirectamenteSemPassarPeloModal`       |
| IM-02 | Os 18 ficheiros que imprimem chamam `PrintPreviewDialog.show(` — ou delegam no `TableExportAction`, que o chama | `todosOsEcrasQueImprimemAbremOModal`                      |
| IM-03 | O modal oferece impressora, cópias, páginas, orientação, ajuste e cor           | `oModalOfereceImpressoraCopiasPosicaoEPreVisualizacao`     |
| IM-04 | Cópias sempre entre 1 e 99, seja qual for o valor recebido                      | `asCopiasFicamSempreDentroDosLimites`                     |
| IM-05 | Intervalo vazio = documento inteiro                                             | `intervaloVazioImprimeODocumentoInteiro`                  |
| IM-06 | `1,3-5` → páginas 1, 3, 4, 5; repetições e espaços tolerados                    | `intervaloAceitaPaginasSoltasEGamas`                      |
| IM-07 | Página inexistente, texto, gama invertida e intervalo vazio são recusados **com a razão** | `intervaloRecusaOQueNaoExisteEExplicaPorque`      |
| IM-08 | O resumo do rodapé diz o que vai sair no papel                                  | `oResumoDizOQueVaiSairNoPapel`                            |
| IM-09 | Retrato/paisagem **viram a folha do documento**; um recibo de 80 mm continua estreito | `aPosicaoViraAFolhaDoProprioDocumentoEmVezDeAForcarA4` |
| IM-10 | A página só roda quando a folha muda de lado                                    | `aPaginaSoRodaQuandoAFolhaMudaDeLado`                     |
| IM-11 | A folha fica centrada; ajustada à página a página **cabe** na folha; tamanho real ocupa-a toda | `aFolhaFicaCentradaEAPaginaNuncaSaiDaFolha`      |
| IM-12 | O zoom aumenta a folha                                                          | `aFolhaCresceComOZoom`                                    |
| IM-13 | A pré-visualização lê as páginas, conhece a forma de cada uma e usa cache       | `aPreVisualizacaoLeAsPaginasEConheceAFormaDeCadaUma`      |
| IM-14 | PDF vazio ou ilegível falha com mensagem, não em silêncio                       | `umPdfIlegivelNaoFicaEmSilencio`                          |
| IM-15 | Listar impressoras nunca rebenta, mesmo sem nenhuma instalada                   | `aListaDeImpressorasNuncaRebenta`                         |
| IM-16 | Imprimir sem documento é recusado                                               | `imprimirSemDocumentoERecusado`                           |
| IM-17 | As escolhas são lembradas por família (`guia-remessa-2026/14` → `guia-remessa`) | `asEscolhasSaoLembradasPorFamiliaDeDocumento`             |
| IM-18 | Sem `Preferences`, a impressão continua a funcionar com os defeitos             | `semPreferenciasDisponiveisAImpressaoContinuaAFuncionar`  |
| IM-19 | O intervalo de páginas **não** é herdado de outro documento                     | `oIntervaloDePaginasNaoEHerdadoDeOutroDocumento`          |
| IM-20 | O modal monta: os códigos de ícone existem e o esqueleto compõe-se              | `oModalMontaComTodosOsIconesEControlos`                   |
| IM-21 | A folha é pintada como folha — branca, contornada, com proporção A4             | `aFolhaEDesenhadaComoUmA4RecortadoContraOFundo`           |
| IM-22 | O mesmo em tema claro, onde o cartão por baixo também é branco                  | `aFolhaContinuaBrancaERecortadaNoTemaClaro`               |
| IM-23 | *Paisagem* **roda** uma página em pé antes de a enviar para a fila              | `paisagemViraUmaPaginaEmPeAntesDeAEnviar`                 |
| IM-24 | *Retrato* não mexe no que já está em pé; *Automática* nunca mexe                | `retratoNaoMexeNumaPaginaQueJaEstaEmPeEAutomaticoNuncaMexe` |
| IM-25 | Impressor e pré-visualização decidem virar **com a mesma regra**                | `oImpressorEAPreVisualizacaoDecidemVirarComAMesmaRegra`   |
| IM-26 | A exportação de listagens abre o modal **num só sítio** (`TableExportAction`)   | `aExportacaoDeListagensAbreOModalNumSoSitio`              |

## 2. Manual — aplicação a correr

Requer backend a correr e pelo menos uma impressora instalada (um *Microsoft Print to PDF* serve).

| ID    | Passos                                                                | Esperado                                                                                          |
|-------|-----------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| MI-01 | Comercial › seleccionar factura › **Imprimir**                        | Abre o modal com cabeçalho `Imprimir documento`, a factura desenhada na folha e o nº de páginas.   |
| MI-02 | No modal, mudar **Orientação** para *Paisagem*                        | A folha vira; a página aparece rodada 90°, centrada, sem sair da folha.                            |
| MI-03 | Mudar **Ajuste** para *Tamanho real*                                  | A página passa a ocupar a folha de bordo a bordo (sem a margem do ajuste).                          |
| MI-04 | Documento com várias páginas › setas ◀ ▶                              | Muda de página; o rótulo acompanha (`Página 2 de 3`); a folha redesenha sem congelar a janela.      |
| MI-05 | Zoom **+** três vezes                                                 | A folha cresce, aparecem barras de deslocamento, o texto continua nítido (redesenha à medida nova). |
| MI-06 | **Páginas** = *Personalizado*, escrever `1,3-5` num documento de 2 págs | Erro a dizer que o documento tem 2 páginas; campo a vermelho; **nada é enviado**.                   |
| MI-07 | **Nº de cópias** = `0` ou `abc` › **Imprimir**                        | Erro inline; o modal fica aberto.                                                                   |
| MI-08 | Escolher impressora, 2 cópias › **Imprimir**                          | Saem 2 cópias na impressora escolhida; o modal fecha com confirmação a nomear a impressora.         |
| MI-09 | Reabrir a impressão de **outra** factura                              | A impressora e as cópias vêm preenchidas da vez anterior; o intervalo volta a *Todas as páginas*.   |
| MI-10 | Imprimir **etiquetas** (Stock) e escolher a térmica; depois factura   | A factura volta a propor a laser — a memória é por família, não global.                             |
| MI-11 | **Guardar PDF**                                                       | Mensagem com o caminho em `~/multicore-pdfs`; o ficheiro existe; o leitor **não** abre.             |
| MI-12 | **Abrir no leitor**                                                   | Grava e abre no leitor de PDF do sistema (comportamento antigo, agora opcional).                    |
| MI-13 | **Cancelar** / `Esc` / fechar na cruz                                 | Fecha sem imprimir e sem gravar nada.                                                               |
| MI-14 | `Ctrl+P` dentro do modal                                              | Imprime, como o botão.                                                                              |
| MI-15 | POS › concluir venda › *Sim* a imprimir recibo                        | Abre o mesmo modal, com o recibo térmico em folha estreita — **não** em A4.                          |
| MI-16 | Desligar todas as impressoras (Windows › Impressoras) e imprimir      | Combo desactivada, **Imprimir** desactivado com explicação; *Guardar PDF* continua a funcionar.      |
| MI-17 | Tema claro › repetir MI-01                                            | A folha continua branca com texto escuro; as opções seguem o tema claro.                            |
| MI-18 | Redimensionar a janela principal para o mínimo e imprimir             | O modal continua contido na janela principal (`containWithinMain`), com deslocamento onde precisa.   |

## 3. Verificação técnica

```
mvn -o -pl desktop test -Dtest=PrintModalHarnessTest,PrintPreviewDialogPaintTest,PdfPrinterOrientationTest
mvn -o -pl desktop test                                 # suite do desktop
mvn -o test                                             # suite completa, sem regressões
```

## 4. Por fechar

- **A direcção da rotação em paisagem** está provada no modelo (IM-23/IM-25: a mesma regra decide
  nos dois lados) e vista na fotografia do driver, mas **não foi confirmada contra papel a sair**.
  Se numa impressora concreta a folha sair virada ao contrário, o sítio a corrigir é
  `PdfPrinter.turnPages` — e o harness apanha a divergência com a pré-visualização.
- **MI-08 e MI-15 exigem impressora física** para valer como verificação real. Com *Microsoft Print
  to PDF* prova-se que o trabalho chega à fila com as opções certas, mas **não** que o papel sai
  bem numa térmica de 80 mm — isso fica por confirmar na loja.
- **Duplex, bandejas e marcas de água** não estão no modal por decisão da spec (§Não-objetivos).
  Se a loja precisar, é uma iteração nova — não um remendo neste diálogo.
