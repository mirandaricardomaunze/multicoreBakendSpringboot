# Spec — Inspetor de detalhes como modal profissional

> Converte o diálogo de detalhes de linha (`UIHelper.showRowDetailsDialog`, aberto por **duplo-clique**
> numa tabela) para o modal canónico [ModernFormDialog](../src/main/java/mz/multicore/erp/gui/components/ModernFormDialog.java).
> **Só apresentação.**

**Última actualização:** 2026-06-30

## Problema

O inspector de detalhes era um `JDialog` montado à mão:

- Cabeçalho simples (só um `JLabel`), sem badge/ícone/subtítulo como os restantes modais.
- Tamanho fixo `600×480`, centrado no ecrã — **não contido** na janela principal (podia sair fora,
  ao contrário dos `ModernFormDialog`).
- Cores **hardcoded** (`new Color(75,85,99)`, `BG_CARD`) nos campos de valor — destoava do tema e
  dos novos campos profissionais.
- Botão "Fechar" solto, sem o rodapé fixo com divisória dos outros modais.

Resultado: inconsistente e amador face aos formulários já migrados para `ModernFormDialog`.

## Decisões

- **Reutilizar o `ModernFormDialog`** (DRY): cabeçalho premium (badge + ícone `fas-info-circle` +
  subtítulo), contenção na janela principal, scroll responsivo e rodapé fixo — tudo herdado.
- **Novo modo só-leitura** no `ModernFormDialog`: `asReadOnly("Fechar")` remove "Gravar" e
  transforma "Cancelar" no único botão **Fechar** (`fas-check`). Não há acção de gravação.
- **Campos de valor pelos estilizadores centrais** (`styleTextField`/`styleTextArea`) — cores do
  tema, borda arredondada, realce de foco e **copiáveis** (só-leitura, não editáveis). Etiqueta da
  coluna a acento.
- **Valores longos** (>50 caracteres ou com quebras) numa `JTextArea` com *wrap* dentro de scroll.
- **Colunas escondidas** (ID com largura 0) continuam saltadas.

## Não-objetivos

- Não permitir edição no inspector (continua só-leitura).
- Não alterar o gatilho (duplo-clique) nem o `styleTable`.
- Não introduzir nova biblioteca.
