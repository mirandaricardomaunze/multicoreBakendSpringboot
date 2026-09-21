# Spec — Modal de impressão antes de cada documento

> Nenhum documento do ERP sai para o papel sem passar por um modal de pré-visualização com escolha
> de **impressora**, **cópias**, **intervalo de páginas**, **posição no papel** e **ajuste**.

**Última actualização:** 2026-08-30
**Harness:** [IMPRESSAO_MODAL_HARNESS.md](IMPRESSAO_MODAL_HARNESS.md) — 25 testes em
`PrintModalHarnessTest` (19), `PrintPreviewDialogPaintTest` (3) e `PdfPrinterOrientationTest` (3)

## Problema

Todos os 28 pontos de impressão do desktop faziam a mesma coisa:

```java
pdf -> PdfFileSaver.saveAndOpen(pdf, "fatura-" + numero)
```

O PDF era gravado em `~/multicore-pdfs` e entregue ao leitor do sistema operativo. Daí resultava:

- **O operador não escolhia impressora.** Saía na que estivesse por defeito no Windows. Numa loja
  com laser no escritório e térmica no balcão, a etiqueta ia parar ao escritório.
- **Não havia cópias.** Duas vias de uma guia de remessa = imprimir duas vezes à mão, no leitor.
- **Não havia posição.** Um mapa fiscal largo saía cortado em retrato e ninguém percebia porquê
  antes de a folha sair.
- **Não havia pré-visualização dentro do ERP.** Via-se o documento no Acrobat/Edge — se estivesse
  instalado. Sem leitor de PDF, `Desktop.open` falhava em silêncio (`catch (IOException ignored)`)
  e o operador ficava a olhar para um ecrã onde, aparentemente, não tinha acontecido nada.

Imprimir é irreversível: consome papel, consome a etiqueta e, num recibo, consome um número de
série. A decisão tem de vir **antes** do acto, não depois de a folha sair.

## Decisões

### A — Um único caminho para o papel

`PrintPreviewDialog.show(owner, pdf, baseName)` substitui **todas** as chamadas a
`PdfFileSaver.saveAndOpen` nos ecrãs. O `PdfFileSaver` continua a existir, mas passa a ser detalhe
interno do modal (botões *Guardar PDF* e *Abrir no leitor*) — nenhum painel o chama directamente.
O harness falha se voltar a aparecer em `mz.multicore.erp.gui` fora de `components`.

O modal é sempre o mesmo, para o documento que for. Não há “imprimir rápido” num sítio e modal
noutro: a única maneira de o operador confiar no que vê é a janela ser sempre a mesma.

### B — Seis classes, uma responsabilidade cada (SRP)

Todas em `desktop/.../gui/components/`:

| Classe                | Responsabilidade                                                                  |
|-----------------------|-----------------------------------------------------------------------------------|
| `PrintOptions`        | Valor imutável + **validação** do intervalo de páginas e limites de cópias         |
| `PrintOptionsStore`   | Lembra as escolhas por **família de documento** nas `Preferences`                  |
| `PdfPreviewDocument`  | Abre o PDF **para ser visto**: nº de páginas, forma de cada uma, imagem em cache   |
| `PaperLayout`         | Geometria: onde fica a folha, se a página roda, onde assenta dentro da folha       |
| `PdfPrinter`          | Envia para a fila de impressão com as opções escolhidas                            |
| `PrintPreviewDialog`  | **Só composição**: monta a janela e liga os cinco acima                            |

A divisão não é decorativa: cinco das seis testam-se sem abrir janela nenhuma, e é por isso que o
harness consegue cobrir intervalos, rotação e geometria sem depender de um ecrã.

### C — O que o modal oferece

**Esquerda — pré-visualização.** A folha desenhada a branco sobre o fundo da aplicação, com sombra,
a página por cima. Navegação página a página, zoom (0,6× a 3×) e *ajustar à janela*. O render corre
**fora do EDT**, numa única thread — o `PDDocument` do PDFBox não é seguro entre threads — e um
resultado que já não interessa (o operador mudou de página entretanto) é descartado por um bilhete.

**Direita — opções**, em três secções:

- **Impressora** → destino, da lista de impressoras instaladas no posto.
- **Trabalho** → nº de cópias (1–99), páginas (*Todas* / *Página actual* / *Personalizado*) e o
  intervalo (`1,3-5`).
- **Posição no papel** → orientação (*Automática* / *Retrato* / *Paisagem*), ajuste (*Ajustar à
  página* / *Tamanho real*) e escala de cinzentos.

**Rodapé** → resumo em tempo real (`3 páginas · 2 cópias · Paisagem`) e as acções
`Cancelar` · `Abrir no leitor` · `Guardar PDF` · **`Imprimir`**.

Design: `ModernPanel` para os cartões, `buildPremiumHeader` no topo, `SectionHeader` + `FormField`
nas opções, `UIHelper.icon(...)` em todos os botões, cores só dos *slots* do tema. Nunca emojis.

### D — A posição vira a folha do documento, não força A4

`PaperLayout.paperAspect` em **Retrato** e **Paisagem** roda a folha *do próprio documento* em vez
de a substituir por A4. Um recibo térmico de 80 mm continua estreito em retrato — como sai da
impressora. Forçar A4 mostraria ao operador uma folha que ele nunca vai ter em mãos.

Quando a folha muda de lado, a página é desenhada rodada 90° na pré-visualização, e no `PdfPrinter`
cada página leva o seu próprio `PageFormat` dentro de um `Book` — é assim que a posição escolhida
chega à fila de impressão página a página, em vez de um único formato para o documento inteiro.

**O `PdfPrinter` roda mesmo a página antes de a enviar** (`turnPages`, `/Rotate += 90`). Sem isso,
escolher *Paisagem* mandava o documento em pé para uma folha deitada: o `PDFPrintable` **não roda
nada** — encolhe a página para caber e sairia um documento pequeno ao meio da folha, com margens
enormes, **diferente do que a pré-visualização mostra**. Um modal que mostra uma coisa e imprime
outra é pior do que não ter modal nenhum. A decisão de virar é a mesma nos dois sítios —
`PaperLayout.rotates` — precisamente para que não possam divergir.

Em **Automática**, cada página escolhe retrato ou paisagem consoante a sua própria forma, e nada é
rodado.

### E — As escolhas são lembradas por família

`PrintOptionsStore.familyOf("guia-remessa-2026/14")` → `guia-remessa`. As etiquetas saem sempre na
térmica e as facturas na laser; o operador não devia ter de o repetir a cada venda. Guarda-se a
impressora, as cópias, a posição, o ajuste e a cor — **nunca o intervalo de páginas**, que é do
documento concreto e não da família.

À prova de falha, como o `NotificationReadStore`: sem `Preferences` disponíveis, funciona em
memória e nunca impede a impressão.

### F — Caminhos de degradação declarados

| Situação                                | O que acontece                                                              |
|-----------------------------------------|------------------------------------------------------------------------------|
| Nenhuma impressora instalada            | Combo desactivada, **Imprimir** desactivado com explicação; *Guardar PDF* funciona |
| PDF ilegível para a pré-visualização    | Aviso + abre no leitor do sistema (comportamento antigo) — o documento nunca se perde |
| Impressora escolhida desapareceu        | Mensagem a pedir para escolher outra; nada é enviado                          |
| Intervalo inválido                      | Campo marcado a vermelho + mensagem que diz quantas páginas o documento tem    |
| Falha no envio para a fila              | Erro com a mensagem do spool; o modal fica aberto                             |

### G — Dependência nova: PDFBox 3.0.6 (só no desktop)

O OpenPDF **gera** PDF mas não o desenha nem o imprime. Para pré-visualizar e enviar para a fila é
preciso um render: `org.apache.pdfbox:pdfbox:3.0.6` no `desktop/pom.xml` (`PDFRenderer`,
`PDFPrintable`, `Book`). O backend não é tocado — continua a gerar com OpenPDF.

A fronteira do [MULTI_MODULE_ARCHITECTURE_SPEC](MULTI_MODULE_ARCHITECTURE_SPEC.md) mantém-se: o
PDFBox é uma biblioteca de cliente e vive no artefacto que se instala nas máquinas.

## Onde se aplica — os 28 pontos de impressão

| Módulo         | Ecrã / acção                                                     | `baseName`                    |
|----------------|-------------------------------------------------------------------|-------------------------------|
| **Comercial**  | Imprimir factura                                                  | `fatura-…`                    |
|                | Guia de remessa da factura                                        | `guia-remessa-…`              |
|                | Exportar tabela de facturas                                       | `faturas-export`              |
|                | Exportar tabela de encomendas                                     | `encomendas-export`           |
|                | Nota de crédito · Nota de débito                                  | `nota-credito-…` / `nota-debito-…` |
|                | Guias de remessa (painel próprio)                                 | `guia-remessa-…`              |
|                | Encomenda (detalhes) · Encomenda A4 (fulfillment)                 | `encomenda-…`                 |
|                | Cotação                                                           | `cotacao-…`                   |
|                | Guia de separação · Reimpressão de separação                      | `separacao-…` / `reimpressao-separacao-…` |
| **POS**        | Recibo da venda · Recibo do histórico                             | `recibo-…`                    |
|                | Fecho de caixa (Z)                                                | `fecho-caixa-Z-…`             |
| **Stock**      | Inventário · Etiquetas de produto                                 | `inventario-stock-…` / `etiquetas` |
|                | Folha de contagem · Lotes & validades                             | `folha-contagem-…` / `lotes-validades` |
|                | Guia de transferência                                             | `transferencia-…`             |
| **RH**         | Recibo de salário · Exportações de tabela                         | `recibo-salario-…` / `…-export` |
|                | Contrato de trabalho                                              | `contrato-…`                  |
|                | Acerto final · Certificado de trabalho                            | `acerto-final-…` / `certificado-…` |
| **Fiscal**     | Declaração de IVA · Mapa fiscal salarial                          | `declaracao-iva-…` / `mapa-fiscal-salarial-…` |
| **CRM**        | Folha de obra                                                     | `folha-obra-…`                |

O `baseName` dá o nome ao trabalho na fila de impressão, ao ficheiro guardado e à família de opções
lembradas — os três derivam do mesmo sítio, de propósito.

## Não-objetivos

- **Não altera nenhum PDF gerado.** O conteúdo, o layout e os serviços do backend ficam iguais.
- **Não imprime em silêncio.** Não há caminho que envie para o papel sem o modal, nem em POS.
- **Não gere filas nem estado de impressora** (tinta, papel, trabalhos pendentes).
- **Não guarda histórico de impressões.** Quem já tinha auditoria — a reimpressão de guias de
  separação — continua a tê-la, pelo backend, exactamente como antes.
- **Não substitui o diálogo do sistema operativo** para casos exóticos (duplex, bandejas, marcas
  de água): quem precisar disso guarda o PDF e usa o leitor.
