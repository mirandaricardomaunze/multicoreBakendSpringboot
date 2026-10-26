# Especificação de Decomposição Modular — RecordDetailsDialog (Fase 85)

## 1. Contexto e Diagnóstico

A classe `RecordDetailsDialog.java` no pacote `gui.components` acumulava 616 linhas, misturando num único ficheiro:
1. Orquestração do inspector modal e resolução de temas (`show`, `showInternal`, `resolveHeaderTheme`).
2. Descoberta de entidades em tabela e extração multi-domínio de propriedades para Artigos, Clientes, Encomendas e linhas genéricas (`extractProperties`, `findProduct`, `findClient`, `findOrder`, ~160 linhas).
3. Construção de tabelas secundárias de itens de documento e grelha de auditoria/sessão (`buildDocumentLinesTable`, `buildAuditTable`).
4. Ferramentas de exportação e partilha: resumo executivo textual (`buildExecutiveSummary`), serialização JSON (`buildJson`), cópia para área de transferência (`copyToClipboard`), gravação em ficheiro (`exportToFile`) e aba de visualização (`buildSharePreviewTab`).

## 2. Princípios Arquiteturais e Restrições Não Negociáveis

1. **Separação de Responsabilidades (SOLID / SRP):**
   - `RecordDetailsDialog`: Orquestrador e fachada pública com `PropertyItem`, `buildPropertySheet`, `show`, `valueText` ($\le 180$ linhas).
   - `RecordDetailsPropertyExtractor`: Extração semântica de propriedades, cálculo de margem e resolução de tema visual ($\le 140$ linhas).
   - `RecordDetailsExporter`: Resumo executivo, exportação TXT/JSON e aba de partilha ($\le 130$ linhas).
   - `RecordDetailsTabBuilder`: Tabela de linhas de documento e tabela de auditoria de sessão ($\le 90$ linhas).
2. **Preservação Estrita de Contratos:**
   - O record `RecordDetailsDialog.PropertyItem`, o método `buildPropertySheet(List<PropertyItem>)`, `show(JTable)`, `findProduct`, `findClient`, `findOrder`, `valueText(Object)` e `isHidden(JTable, int)` mantêm-se exatamente no mesmo ficheiro/assinatura, garantindo zero quebras no ecossistema de telas do Multicore ERP.
3. **Internacionalização e Conformidade:**
   - Português de Moçambique (pt-MZ).
   - Zero `JOptionPane`.
