# Precisão da interface — Especificação e harness

**Última actualização:** 2026-11-02

## Objectivo

A revisão de 94 ecrãs com a empresa de exemplo mostrou uma interface consistente, mas pouco precisa:
valores cortados, rodapés errados e dados internos à vista. Estas regras valem para todo o sistema
porque vivem nos componentes partilhados.

## Decisões

- Em tabelas, valores, datas e estados nunca são cortados. O texto livre encolhe primeiro; só depois
  aparece barra horizontal.
- Nos cartões, a letra reduz até o valor caber. Um texto que continue sem caber ganha dica com o texto
  completo.
- A paginação regista-se antes de trocar o ordenador, para não nascer duplicada.
- Lotes `AUTO-` e `LEGACY` aparecem como "—". A validade 31/12/9999 significa "Sem validade".
- Na Tesouraria, os movimentos dizem Entrada e Saída.
- Balancete e Razão carregam ao abrir a Contabilidade.
- A acção de criar ou registar é verde. Azul fica para as outras acções principais.
- O plano de contas já é automático, por isso o botão passa a "Repor Contas em Falta", secundário.
- O registo de auditoria mostra a acção em português.
- Os cartões usam o fundo do tema. A cor da métrica fica na faixa e no ícone; vermelho, âmbar e laranja
  ficam neutros com valor zero.
- O nome do módulo aparece só na barra superior.
- Linhas de tabela só ganham cor quando pedem atenção: o estado normal fica na etiqueta.
- A coluna "Negócio" esconde-se quando nenhuma linha tem unidade de negócio.
- Grafia nos textos do desktop: Fatura, Acções, Reconciliação Bancária. Moeda: MT.

## Cenários

| ID | Cenário | Resultado |
|----|---------|-----------|
| UP-01 | Código de ícone no código | Existe na biblioteca |
| UP-02 | Botão Novo, Registar, Emitir, Criar, Adicionar ou Gerar | Verde |
| UP-03 | Tabela sem ordenador prévio | Uma só paginação |
| UP-04 | Total de nove dígitos e data com hora | Largura mínima mostra tudo |
| UP-05 | Nome de cliente muito longo | Pode encolher |
| UP-06 | Valor maior do que o cartão | Letra reduz até caber |
| UP-07 | Lote gerado pelo sistema | "—" |
| UP-08 | Validade 31/12/9999 | Sem validade |
| UP-09 | Código de auditoria | Texto em português |
| UP-10 | Cartão em qualquer cor | Fundo do tema, faixa da métrica |
| UP-11 | Alerta com valor zero | Faixa neutra |
| UP-12 | Alerta com valor | Faixa de alerta |
| UP-13 | Coluna "Negócio" sem unidades | Escondida; volta se houver uma |

Teste: `UiPrecisionHarnessTest`.

## Limites

- Alguns estados continuam em maiúsculas.
- O filtro "Negócio" continua visível, mesmo com a coluna escondida.
- O Ranking continua a mostrar o nome de utilizador, e Aprovações o número interno: vêm do backend.
- A tradução de auditoria usa palavras conhecidas; códigos raros podem ficar parcialmente em inglês.
