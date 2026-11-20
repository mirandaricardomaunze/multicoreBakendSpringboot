# Spec — Actualização automática dos ecrãs

Status: **aprovada**
Effective date: 2026-11-20
Harness: [ACTUALIZACAO_AUTOMATICA_HARNESS.md](ACTUALIZACAO_AUTOMATICA_HARNESS.md)
Relacionadas: [BOTAO_ACTUALIZAR_UNIFICADO_SPEC.md](BOTAO_ACTUALIZAR_UNIFICADO_SPEC.md) ·
[COMMERCIAL_MULTIUSER_REFRESH_SPEC.md](COMMERCIAL_MULTIUSER_REFRESH_SPEC.md)

## 1. Problema

Quando outro posto vende, factura, recebe ou mexe no stock, os outros só vêem a mudança depois de
carregar em **Actualizar**. Numa loja com vários caixas e um escritório, isso dá decisões tomadas
sobre números velhos.

## 2. Decisões do responsável (2026-11-20)

| Tema | Decisão |
|---|---|
| Mecanismo | **Verificação leve a cada 5 s**: o servidor guarda um contador de alterações por empresa; o desktop pergunta se mudou e só recarrega quando mudou. Sem ligações abertas nem infra nova. |
| Alcance | **Só o ecrã visível** (e o contador do sino). Os outros recarregam quando forem abertos. |
| Interrupção | **Esperar e preservar**: com diálogo aberto, a escrever ou a editar uma célula, espera; ao recarregar mantém página e linha seleccionada. |
| Botão | O botão **Actualizar continua** em todos os ecrãs. |

## 3. Servidor

- `ChangeVersionRegistry`: contador por empresa, em memória, que **sobe** a cada escrita com sucesso,
  e um `epoch` (identificador da instância do servidor) que muda quando o servidor reinicia.
- `ChangeTrackingInterceptor` (depois do `SecurityInterceptor`): pedido `POST`, `PUT`, `PATCH` ou
  `DELETE` em `/api/**`, com empresa activa, sem excepção e resposta **2xx** → sobe o contador dessa
  empresa. `GET`, respostas de erro, `/api/auth/**` e `/api/changes/**` não contam.
- `GET /api/changes` → `ChangeVersionDTO(epoch, version)` da empresa activa (`X-Company-Id`).
  Qualquer perfil autenticado da empresa pode consultar; nunca revela contadores de outra empresa.
- Escritas feitas fora de pedidos HTTP (tarefas agendadas) não sobem o contador; o botão
  Actualizar continua a cobri-las.

## 4. Desktop

### 4.1 Verificação

- `AutoRefreshCoordinator` arranca com a janela principal e pára no `dispose`. Corre numa thread
  de fundo (nunca na EDT) e pergunta `/api/changes` a cada **5 s**.
- Há alteração quando o `version` sobe **ou** o `epoch` muda (servidor reiniciado). A primeira
  leitura, e a primeira depois de trocar de empresa, só memoriza (não recarrega).
- Falha de rede ou servidor em baixo: ignora em silêncio e tenta na próxima volta; nunca mostra
  erro nem bloqueia o ecrã.
- Super-admin sem empresa activa: não verifica.

### 4.2 O que se recarrega

- `UIHelper.createRefreshButton(acção)` regista o botão num `RefreshRegistry` (referência fraca).
- Numa alteração, executa a acção de **cada botão Actualizar que está à vista e activo** na janela
  principal — exactamente o que o clique faria — e actualiza o contador do sino.
- Ecrãs escondidos não recarregam. Ecrãs sem botão Actualizar não mudam.

### 4.3 Não interromper

A actualização fica **pendente** (e volta a ser tentada a cada volta) enquanto:

- há um diálogo modal à vista;
- o utilizador carregou numa tecla há menos de **3 s**;
- uma tabela à vista está a editar uma célula;
- a janela está minimizada;
- já há um carregamento em curso na janela principal.

### 4.4 Preservar o estado

- Antes de recarregar, para cada tabela à vista: guarda a **chave da linha seleccionada** (valor da
  coluna "Nº", "Número", "Documento", "Código", "SKU" ou "ID"; sem nenhuma, a 1.ª coluna) e a
  **página** da paginação de cliente.
- Quando os dados novos chegam (mudança de dados ou de modelo da tabela, até 15 s), repõe a página
  (limitada às páginas que existem) e volta a seleccionar a linha com a mesma chave; se a linha já
  não existe, fica sem selecção.
- Filtros e pesquisa continuam, porque a acção é a mesma do botão.

## 5. Fora de âmbito

- Push em tempo real (SSE/WebSocket) e actualização de ecrãs escondidos.
- Contador partilhado entre várias instâncias do servidor (hoje há uma; com várias, o contador passa
  para a base de dados).
- Avisar mudanças feitas por tarefas agendadas.
