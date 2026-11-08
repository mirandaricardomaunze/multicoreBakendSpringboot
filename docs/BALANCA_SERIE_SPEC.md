# Spec — Balança real por porta série (SA-C05, F3)

**Data:** 2026-11-08 · **Estado:** feito (driver e configuração), por validar com a balança da loja piloto
**SPEC mestre:** `docs/SECTORES_ACTIVIDADE_SPEC.md` · **Prompt:** `docs/prompts/PROMPT_BALANCA_VENDA_PESO.md`

## Decisões

1. **Driver:** jSerialComm 2.11.2 no desktop. Uma ligação partilhada por posto (`ScaleConnection`) serve o POS
   e a bancada do talho. Sem porta configurada fica a simulação de antes.
2. **Tramas:** terminam em CR, LF ou ETX; o STX inicial é retirado (`ScaleFrameSplitter`). Reconhece peso estável,
   instável (`US`, `U`, `?`) e sobrecarga (`OL`, `OVER`, `HHHH`). Sem tramas durante 3 s, o peso passa a
   "desligada" e a ligação tenta reabrir a porta a cada 2 s.
3. **Modos:** contínuo; por pedido com `W` + CR; por pedido com ENQ (0x05), conforme o manual da balança.
4. **Configuração por posto:** cada balcão tem a sua porta. Fica no ficheiro local do posto
   (`%LOCALAPPDATA%\Multicore\desktop.properties`: `desktop.pos.scale-enabled`, `desktop.scale.port`,
   `desktop.scale.baud`, `desktop.scale.mode`), não no servidor.
5. **Captura:** só peso estável e positivo. No POS mantém-se o botão "Capturar"; na desmancha, "Ler balança"
   põe o peso no corte seleccionado ou, sem selecção, no peso da carcaça.

## Interface

Configurações › **Balança deste Posto**: activar, porta (com "Procurar portas"), velocidade, modo, "Gravar e
Ligar" e o peso em directo com o estado (estável, a estabilizar, sobrecarga, sem resposta, simulação).

## Testes

`ScaleDriverTest` (tramas, sobrecarga, comandos, definições por posto, simulação sem porta),
`SerialScaleReaderTest`, `SectorFoundationUiHarnessTest` SA-UI-03.

## Limites

- Etiquetas de balança com preço (EAN-13 de peso) já eram lidas; a impressão de etiquetas não faz parte desta fase.
- Protocolos binários proprietários não são suportados; as balanças comuns (CAS, Toledo, Dibal em modo ASCII) sim.
