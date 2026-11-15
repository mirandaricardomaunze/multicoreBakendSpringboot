# Teclado e leitores de ecrã nos diálogos

Estado: **feito** · 2026-11-15 · Harness: `desktop/.../components/KeyboardAccessHarnessTest.java`

## Problema

Nenhum botão tinha tecla Alt+letra: num diálogo com cinco botões, quem trabalha ao teclado tinha de
andar com Tab. Botões só com ícone feitos fora da fábrica `createIconButton` eram anunciados pelo
leitor de ecrã apenas como "botão".

## Regras

| ID    | Regra |
|-------|-------|
| TA-01 | Ao abrir, cada botão com texto de um diálogo ganha Alt+letra, sem repetir letras no mesmo diálogo: a 1.ª letra de uma palavra quando livre, senão a próxima letra livre do texto. |
| TA-02 | Uma tecla escolhida pelo próprio ecrã mantém-se e não é reutilizada. |
| TA-03 | Letras acentuadas contam pela letra base (É → E). |
| TA-04 | Um botão sem texto e sem nome acessível usa o tooltip como nome acessível. |
| TA-05 | Aplicado a todos os diálogos pelo `DialogKeys` (o mesmo que já põe Enter e Esc), sem código nos ecrãs. |

O FlatLaf só sublinha as letras enquanto se carrega em Alt, por isso o aspecto dos diálogos não muda.
