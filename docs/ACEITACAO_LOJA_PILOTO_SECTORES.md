# Aceitação na loja piloto — balança, báscula e ecrãs sectoriais

**Data:** 2026-11-10 · **Para:** quem instala o Multicore na loja piloto e o responsável da loja
**Specs:** `BALANCA_SERIE_SPEC.md`, `BASCULA_CAMIAO_SPEC.md`, `SECTORES_ACTIVIDADE_SPEC.md`

O sistema foi testado com tramas simuladas. Este guia confirma, **com o equipamento real**, que a balança e a
báscula comunicam, que o peso registado é o peso verdadeiro e que os ecrãs se lêem bem no computador da loja.
Preencha a folha de registo no fim e guarde-a assinada.

## 0. Antes de começar

- [ ] Versão instalada (rodapé da aplicação): ____________
- [ ] Manual da balança e da báscula à mão (velocidade, modo contínuo ou por pedido, formato da trama).
- [ ] Pesos de referência aferidos: **1 kg, 5 kg e 10 kg** para a balança; para a báscula, um camião de tara conhecida
      ou o certificado de verificação metrológica mais recente.
- [ ] Utilizador com perfil de **gestor** (para configurar) e outro de **vendedor** (para usar).

## 1. Ligar a balança do balcão (POS e talho)

1. Ligue a balança por USB ou RS-232. No Windows, veja em *Gestor de Dispositivos › Portas (COM e LPT)* qual a porta (ex.: COM4).
2. No Multicore: **Configurações › Balança deste Posto**.
3. Marque "Usar balança neste posto", carregue em **Procurar portas**, escolha a porta, a velocidade e o modo
   indicados no manual. Carregue em **Gravar e Ligar**.
4. Ponha um peso no prato. Em baixo deve ver:
   - o peso em directo a mudar;
   - o estado **"Peso estável"** quando o prato pára;
   - a linha **"Tramas recebidas: N · última: …"**, com N a subir.

| Se vê… | Causa provável | O que fazer |
|---|---|---|
| "Sem resposta da balança" e N não sobe | Porta ou cabo errados, ou balança em modo por pedido | Confirme a porta; experimente o modo "Por pedido, comando W" ou "ENQ" |
| N sobe mas o peso fica a 0 | Velocidade errada (tramas ilegíveis) | Teste 9600, 4800 e 2400 |
| A trama mostra letras estranhas | Velocidade ou paridade diferentes | Ajuste a velocidade; confirme 8 bits, sem paridade, 1 stop no manual |
| "Sobrecarga" com pouco peso | A balança envia o aviso "OL" noutro formato | Fotografe a trama e envie à equipa |

**Registe a trama mostrada** (ex.: `<STX>00001.450kg<CR>`) na folha: é o que a equipa precisa se houver dúvidas.

## 2. Exactidão da balança

Para cada peso de referência, leia o valor no Multicore (Configurações › Balança deste Posto) e no visor da balança.

| Peso | Visor da balança | Multicore | Diferença aceite |
|---|---|---|---|
| 1 kg | | | 0 (o mesmo valor) |
| 5 kg | | | 0 |
| 10 kg | | | 0 |

O Multicore deve mostrar **exactamente** o que o visor mostra (o programa não arredonda nem corrige o peso).

## 3. Venda ao peso e desmancha

1. **POS:** abra uma venda, escolha um artigo ao peso, ponha 1,250 kg no prato e carregue em **Capturar**. A linha
   deve ficar com 1,250 kg. Com o prato a mexer, "Capturar" deve avisar que o peso não está estável.
2. **Talho › Nova Desmancha:** com a carcaça (ou um peso de teste) no prato e nenhum corte seleccionado, **Ler
   balança** preenche o peso da carcaça; seleccionando um corte, preenche o peso desse corte.

## 4. Báscula de camião

1. No computador da báscula, configure a porta como no passo 1 (o ecrã **Báscula** mostra o peso em directo no topo).
2. **1.ª Pesagem:** camião à entrada. Matrícula, sentido ("Saída de material" para venda), **Ler Báscula**, Registar.
3. **2.ª Pesagem:** camião à saída. **Ler Báscula**, Fechar Talão. O talão abre para imprimir.
4. Confirme:
   - [ ] o líquido é a diferença entre as duas pesagens;
   - [ ] a coluna **Leitura** diz "Balança" (se escrever um peso à mão, deve dizer "Manual" e aparecer no módulo Excepções);
   - [ ] uma 2.ª pesagem no sentido errado é recusada (camião de venda que sai mais leve);
   - [ ] o talão impresso tem matrícula, as duas pesagens, o líquido e as assinaturas.
5. Compare com a tara conhecida ou com o certificado: diferença dentro da classe da báscula.

## 5. Ecrãs no computador da loja

Abra cada módulo do perfil da loja (Configurações › Perfil de Actividade decide quais aparecem) e confirme, com a
janela **maximizada** e depois **a metade do ecrã**:

| Verificação | Resultado |
|---|---|
| Nada fica cortado à direita nem à esquerda; não há barra de deslocação horizontal | |
| Os botões do topo passam para uma segunda linha quando a janela é estreita | |
| Os indicadores do topo ficam em uma ou duas linhas, legíveis | |
| Em janela estreita, a lista e o detalhe ficam um por cima do outro | |
| Os textos e descrições não ficam cortados | |
| Tema claro e escuro: tudo legível | |

Módulos a percorrer, conforme o perfil: Preços, Levantamentos, Obras, Adiantamentos, Talho, Validades, Plano de
Produção, Agenda de Encomendas, Consignação e Rota, Vasilhame, Variantes, Báscula, Excepções.

Se algo estiver mal: **tire uma captura de ecrã** (tecla Windows + Shift + S), anote o módulo e a resolução do
ecrã (Definições › Ecrã) e envie à equipa.

## 6. Painel Executivo

Com pelo menos uma excepção de gravidade alta ou média nos últimos 7 dias (por exemplo uma pesagem manual), abra o
Painel Executivo: o rodapé deve mostrar "Excepções a verificar (7 dias): …". Sem excepções, o rodapé não mostra nada.

## Folha de registo

| Item | OK | Não OK | Observações / trama / foto |
|---|---|---|---|
| 1. Balança comunica (porta, velocidade, modo) | | | |
| 2. Exactidão 1 / 5 / 10 kg | | | |
| 3. Captura no POS e na desmancha | | | |
| 4. Báscula: duas pesagens, líquido, talão | | | |
| 4. Báscula: peso manual assinalado | | | |
| 5. Ecrãs maximizados e a metade do ecrã | | | |
| 6. Alerta no Painel Executivo | | | |

Balança (marca/modelo): ______________ Porta: ____ Velocidade: ____ Modo: __________

Báscula (marca/modelo): ______________ Porta: ____ Velocidade: ____ Modo: __________

Ecrã (resolução): __________

Testado por: ______________________ Data: ___/___/______ Assinatura: ______________

Responsável da loja: ______________ Data: ___/___/______ Assinatura: ______________
