# SPEC — Hierarquia visual de RH e Configurações

**Criado em:** 2026-08-15  
**Camada:** cliente desktop Swing  
**Âmbito:** `HRPanel`, `ConfigPanel` e componentes visuais reutilizáveis.

## 1. Objectivo

Reduzir a sensação de barras carregadas sem remover operações, alterar permissões ou mudar regras de
salários, utilizadores e backups. As acções frequentes e contextuais permanecem visíveis; operações
auxiliares relacionadas são agrupadas num menu textual previsível.

## 2. Hierarquia obrigatória

- Cada cabeçalho deve ter uma acção principal inequívoca.
- Uma barra operacional deve procurar ficar até três controlos visíveis; excepções exigem acções
  críticas opostas, como Aprovar/Rejeitar, que não podem ficar escondidas.
- Exportar, imprimir, actualizar e alterações administrativas secundárias podem ser agrupadas.
- Eliminar, rejeitar, pagar e executar processamento em lote permanecem explícitos.
- Menus usam texto “Mais acções” ou um substantivo claro, ícone, tooltip, nome acessível e teclado.
- O menu não pode conter mais de cinco entradas nem menus aninhados.
- Altura dos menus de acção segue `UIHelper.FORM_CONTROL_HEIGHT` e o estilo `ModernButton`.

## 3. Recursos Humanos

- Colaboradores: Novo Colaborador e Editar visíveis; Exportar PDF e Alterar Estado em “Mais acções”.
- Recibos: Gerar Recibo, Marcar Pago e Processar Mês visíveis; Imprimir PDF e Exportar Lista em
  “Documentos”.
- Aprovar/Rejeitar férias e Eliminar falta continuam visíveis por serem decisões críticas.

## 4. Configurações

- Utilizadores: Novo Utilizador e Editar visíveis; Alterar Perfil e Actualizar Lista em “Mais acções”.
- Backups: as três formas de criação ficam sob “Criar backup”; Verificar e Actualizar Arquivo
  permanecem junto da lista de ficheiros.

## 5. Acessibilidade e estados

- `Enter`/`Espaço` abre o menu; setas e `Enter` são geridos pelo `JPopupMenu` nativo.
- O botão apresenta nome acessível e tooltip.
- Cada entrada tem ícone, texto em português de Moçambique e alvo mínimo canónico.
- Acções continuam a usar os mesmos listeners e protecções assíncronas existentes.

## 6. Fora de âmbito

- Alterar tabs, contratos HTTP, permissões ou fluxos de negócio.
- Esconder acções destrutivas/críticas.
- Reestruturar formulários e tabelas já cobertos pela spec geral de consistência.

## 7. Definition of done

- `ActionMenuButton` canónico criado e testado.
- Barras indicadas cumprem a hierarquia desta spec.
- Harness automático verde e validação manual pendente claramente registada.
- `mvn clean compile` e testes focados verdes.

