# Ecrãs que estavam fora das regras comuns

Estado: **feito** · 2026-11-17 · Harness: `desktop/.../components/OffRuleScreensHarnessTest.java`

Encontrados na galeria visual (`docs/GALERIA_VISUAL_SPEC.md`).

| ID    | Ecrã | Antes | Agora |
|-------|------|-------|-------|
| FR-01 | Configuração › Aparência | Três botões cheios (ciano, azul-escuro) que ciclavam estados escondidos: "Tema: Claro (Clique p/ Alto Contraste)" | Dois seletores segmentados com todas as opções à vista: `Claro \| Escuro \| Alto contraste` e `Compacto \| Padrão \| Confortável`; "Diagnóstico e Saúde" é botão de contorno. |
| FR-02 | Componente | — | `SegmentedControl`: opções exclusivas lado a lado, a activa preenchida a acento; serve qualquer escolha curta e visível. |
| FR-03 | Financeiro › Contas e Fluxo de Caixa | Listagens sem "Actualizar" | "Actualizar" no cabeçalho, como nos restantes ecrãs. RH (Colaboradores, Recibos, Faltas, Ponto, Férias) e CRM › Folhas de Obra ficam sem ele: já têm 3 botões, o limite do `HrCrmUiErgonomicsHarnessTest`. Pendente: decidir se o "Actualizar" entra no lugar de outro botão ou passa para o menu "Mais". |
| FR-04 | Todas as listas nos temas escuro e alto contraste | Seta sobre um quadrado cinzento cheio | Seta sobre a cor do campo, igual ao tema claro. |
| FR-05 | Fecho Cego de Caixa | "(Blind Drop)" no título; "Cancelar / Voltar" vermelho feito à mão | Título só em português; o "Cancelar" comum dos diálogos. |
