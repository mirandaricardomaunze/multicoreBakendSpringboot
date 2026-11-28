# Spec — Actualização do programa com um clique

Status: **aprovada**
Effective date: 2026-11-28
Harness: [ACTUALIZACAO_UM_CLIQUE_HARNESS.md](ACTUALIZACAO_UM_CLIQUE_HARNESS.md)
Relacionadas: [ACTUALIZACOES_CLIENTE_SPEC.md](ACTUALIZACOES_CLIENTE_SPEC.md) (aviso, 426, instalador) ·
[DEPLOY_VPS_SPEC.md](DEPLOY_VPS_SPEC.md) (pasta `/downloads/`)

## 1. Problema

O rodapé já diz "Versão X disponível" e o 426 já oferece "Descarregar actualização", mas isso só abre
o navegador. Depois, alguém na loja tem de encontrar o ficheiro, fechar o ERP e correr o instalador, e
na prática isso não acontece. A actualização validada ao vivo em 2026-11-27 (AC-59) prova que o
instalador por cima preserva configuração, preferências e atalhos. Falta pô-lo a correr sem ajuda.

## 2. Decisões do responsável (2026-11-28)

| Tema | Decisão |
|---|---|
| Confiança | **SHA-256 até haver certificado.** Sem assinante configurado no build, instala se o SHA-256 bater com o manifesto publicado e o servidor for HTTPS (ou este computador). Com assinante configurado, exige **também** assinatura Authenticode válida desse assinante, sem excepções. |
| Quem | **Qualquer utilizador autenticado**, mas recusa se o operador tiver **sessão de caixa POS aberta** ou o posto tiver **vendas suspensas** nesta empresa. |
| Quando | **O operador escolhe:** "Reiniciar e instalar agora" (fecha o ERP, instala em silêncio e reabre) ou "Instalar ao fechar" (instala quando o operador sair do ERP). |

## 3. Publicação

- `scripts/build-windows-installer.ps1` gera, ao lado do `Multicore-instalador.exe`, o manifesto
  `Multicore-instalador.json`: `{"version":"1.0.4","sha256":"<64 hex>","size":<bytes>}`.
- `scripts/publicar-instalador.sh <exe> <json>` confere que o SHA-256 e o tamanho do manifesto batem
  com o `.exe` e publica os dois em `data/downloads/`, **o `.exe` primeiro e o manifesto por último**,
  cada um por renomeação atómica. Um posto nunca vê um manifesto novo a apontar para o `.exe` antigo.
- Endereços fixos: `<servidor>/downloads/Multicore-instalador.json` e `.exe`. O desktop nunca aceita
  outro endereço vindo do servidor (§6.5 da spec de actualizações).

## 4. Desktop

### 4.1 Onde se pede

- Rodapé: "Versão X disponível" passa a ser clicável ("Clique para actualizar").
- Login recusado com 426: o botão passa a "Actualizar agora". Aqui não há sessão nem venda possível,
  por isso não se verifica caixa nem vendas suspensas.
- Programa a correr a partir do jar (desenvolvimento, sem `jpackage.app-path`): recusa com
  "A actualização automática só funciona no programa instalado."

### 4.2 Verificações antes de descarregar (por esta ordem)

1. Programa instalado (§4.1).
2. Manifesto legível: versão, SHA-256 com 64 hexadecimais e tamanho entre 1 byte e 1 GB.
3. A versão do manifesto é **mais recente** que a instalada; senão: "O instalador publicado (X) não é
   mais recente do que este programa (Y)."
4. Fora do 426: sem sessão de caixa aberta do operador ("Feche a caixa antes de actualizar") e sem
   vendas suspensas desta empresa neste posto ("Retome ou anule as N vendas suspensas antes de actualizar").

### 4.3 Descarga e verificação

- Para `%LOCALAPPDATA%\MulticoreERP\updates\Multicore-<versão>.exe.part`, com barra de progresso e
  botão Cancelar. Pára e apaga se passar o tamanho do manifesto.
- Depois: tamanho igual ao do manifesto, SHA-256 igual ao do manifesto e, se houver assinante
  configurado (`desktop.update.signer` no build), assinatura Authenticode `Valid` com esse nome no
  assunto do certificado. Qualquer falha apaga o ficheiro e diz qual verificação falhou.
- Só depois de verificado passa a `Multicore-<versão>.exe`.

### 4.4 Instalação

- Um auxiliar PowerShell (janela oculta, `%LOCALAPPDATA%\MulticoreERP\updates\instalar.ps1`) espera que
  saiam todos os `Multicore.exe` da pasta instalada, **volta a conferir o SHA-256** (o ficheiro esteve
  em disco entretanto), corre o instalador com `/quiet` e, se foi "agora" e o instalador terminou com
  0, reabre o programa. Escreve o que fez em `updates\instalar.log`.
- "Agora": o ERP fecha pelo caminho normal da janela (guarda a janela, faz logout).
- "Ao fechar": o rodapé passa a "Versão X será instalada ao fechar" e deixa de ser clicável.
- O endereço do servidor e as preferências vivem fora da pasta instalada (AC-59), pelo que sobrevivem.

## 5. Fora de âmbito

- Instalar sem ninguém pedir. O operador decide sempre.
- Voltar à versão anterior pelo programa. Faz-se reinstalando o `.exe` antigo.
- Postos instalados com a 1.0.0 perdem o `desktop.properties` na primeira actualização (AC-59). Não há
  clientes nessa versão.
- Certificado de assinatura e canal de publicação: dependem de compra e do VPS.
