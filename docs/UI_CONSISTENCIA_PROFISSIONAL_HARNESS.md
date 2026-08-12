# HARNESS — Consistência profissional da UI Swing

Complementa [UI_CONSISTENCIA_PROFISSIONAL_SPEC.md](UI_CONSISTENCIA_PROFISSIONAL_SPEC.md).

## Fase A — componentes de formulário

| ID | Cenário automático | Esperado |
|---|---|---|
| UI-01 | `MoneyField` recebe `1 234,50` | `BigDecimal("1234.50")` |
| UI-02 | `MoneyField` recebe texto inválido | erro em português; campo marcado inválido |
| UI-03 | `QuantityField` recebe `1,250` | `BigDecimal("1.250")` |
| UI-04 | quantidade zero quando positiva é obrigatória | recusada inline |
| UI-05 | `DateField` recebe `2026-08-09` | `LocalDate.of(2026, 8, 9)` |
| UI-06 | data inválida | erro inline; foco permanece no campo |
| UI-07 | `FormField` obrigatório vazio | marcador e mensagem acessível presentes |
| UI-08 | campo read-only | não editável e client-property `readOnly=true` |

## Fase B — selects, botões e tabelas

| ID | Cenário automático | Esperado |
|---|---|---|
| UI-09 | renderer existente recebe `styleComboBox` | renderer funcional preservado/decorado |
| UI-10 | `humanStatus("PARTIALLY_PAID")` | `Parcialmente paga` |
| UI-11 | botão apenas com ícone | tooltip e nome acessível obrigatórios |
| UI-12 | renderer monetário | alinhado à direita e `1 234,50 MT` |
| UI-13 | renderer de quantidade | alinhado à direita e 3 casas |
| UI-14 | badge de estado desconhecido | texto humano/valor seguro e cor neutra |
| UI-15 | tabela entra em loading | modelo não aparenta dados actuais; loading visível |

## Fase C — assíncrono

| ID | Cenário automático | Esperado |
|---|---|---|
| UI-16 | fetch de `loadAsync` | executa fora do EDT |
| UI-17 | callback de sucesso/erro | executa no EDT |
| UI-18 | erro durante fetch | loading termina e erro é entregue uma vez |
| UI-19 | duplo clique em submissão activa | apenas uma execução |
| UI-20 | tenant muda antes da resposta | resposta antiga não é aplicada |

## Fase D — auditorias estáticas

| ID | Regra | Esperado |
|---|---|---|
| UI-21 | `new Color(...)` em painéis | apenas allowlist documentada; tendência até zero |
| UI-22 | `setBounds(...)` em painéis | zero |
| UI-23 | `JButton` funcional criado directamente | zero fora de componentes internos |
| UI-24 | chamadas `desktop.client` em listeners/EDT | todas envolvidas por helper assíncrono |
| UI-25 | painel acima de 1.000 linhas | backlog decrescente até zero |
| UI-26 | enum técnico em arrays de selects | zero nos fluxos migrados |

`UiPanelDecompositionTest` automatiza UI-25 para os seis painéis prioritários. Novos casos de uso
devem ficar nos componentes extraídos, sem mover regras de negócio para o desktop.

### Auditoria de dependências

Executar `mvn dependency:analyze`. Starters Spring Boot e drivers `runtime` não podem ser removidos
apenas por aparecerem como “unused”: o analisador atribui as classes usadas às dependências
transitivas, não ao starter. Nesta fase não foi encontrada biblioteca declarada removível sem quebrar
boot, persistência, segurança, migrations, OpenAPI ou testes. A redução aplicada é de acoplamento
entre painéis, componentes e casos de uso.

## Fase E — validação manual

| ID | Cenário | Evidência esperada |
|---|---|---|
| UI-50 | Formulário com obrigatório vazio | erro junto ao campo, sem modal genérico |
| UI-51 | Dinheiro com vírgula e ponto | ambos aceites e formatados igualmente |
| UI-52 | Select de produto/cliente volumoso | pesquisa por texto sem bloquear |
| UI-53 | Botão icon-only | tooltip; leitor/acessibilidade encontra nome |
| UI-54 | Tabela com dinheiro/quantidade/estado | alinhamento e badges consistentes |
| UI-55 | API lenta/indisponível | janela responde; loading e tentar novamente |
| UI-56 | Trocar empresa durante loading | dados da empresa anterior não aparecem |
| UI-57 | Formulário longo em 1366×768 | conteúdo e botões acessíveis por scroll |
| UI-58 | Escala Windows 100%/125%/150% | sem cortes, sobreposição ou texto truncado crítico |
| UI-59 | Tema claro e escuro | contraste e estados legíveis |
| UI-60 | Fatura/encomenda/compra | editor em painel completo; modal só em acção curta |
| UI-61 | Navegação por teclado | Tab previsível, Esc volta/cancela, Ctrl+S grava |
| UI-62 | POS | F2/F4/F6/F9/Delete continuam funcionais |

## Gate por fase

Cada fase exige testes focados, `mvn clean compile`, suite completa verde, auditoria do diff e
actualização de `tasks/current.md`. Os casos UI-50..62 só fecham com evidência visual em Windows.

## Evidência de execução — 2026-08-09

- Harness focado (`CanonicalFormFieldsTest`, `CanonicalControlsTest`, `AsyncUiTest`,
  `POSKeyboardShortcutTest` e `UiPanelDecompositionTest`): verde.
- `mvn clean test`: verde, 391 testes, 0 falhas, 0 erros e 0 ignorados.
- Painéis prioritários: Comercial 987, Stock 896, POS 972, Compras 761, RH 994 e
  Configuração 911 linhas.
- Auditoria estática: nenhum posicionamento absoluto em painéis de negócio; o `setBounds` restante
  pertence ao overlay calculado de estado vazio da tabela. Tamanhos preferidos restantes servem
  alvos de toque, diálogos, ícones, navegação ou alturas canónicas.
- UI-50..62 permanecem como validação manual obrigatória num Windows com escalas 100%, 125% e
  150%, temas claro/escuro, API lenta e periféricos POS. A suite automática não substitui essa
  evidência visual e física.
