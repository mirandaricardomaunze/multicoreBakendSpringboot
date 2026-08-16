# Actualizações do desktop — harness

Cenários de verificação da [ACTUALIZACOES_CLIENTE_SPEC.md](ACTUALIZACOES_CLIENTE_SPEC.md).
**AC-01..AC-16** automáticos; **AC-20..AC-24** executados ao vivo; **AC-50..AC-54** por fazer.

---

## Automáticos

### `SemanticVersionTest` — comparação de versões

| ID | Cenário | Esperado |
|---|---|---|
| AC-01 | `1.9.0` vs `1.10.0` | `1.9.0` é a mais antiga — **em texto seria o contrário** |
| AC-02 | Versões iguais | nenhuma é mais antiga |
| AC-03 | Maior/menor/correcção | ordem respeitada em cada posição |
| AC-04 | `1.2` vs `1.2.0` | iguais (partes em falta valem zero) |
| AC-05 | `1.2.0-SNAPSHOT` vs `1.2.0` | iguais (sufixo ignorado) |
| AC-06 | `null`, vazio, `"versão-de-teste"` | contam como a mais antiga possível |
| AC-07 | Mínima `0.0.0` | não bloqueia ninguém, nem `0.0.0-dev` |
| AC-08 | `" 1.2.0 "` | espaços não confundem |

### `ClientVersionInterceptorTest` — decisão no servidor

| ID | Cenário | Esperado |
|---|---|---|
| AC-10 | Cliente 1.4.0, mínima 1.2.0 | passa |
| AC-11 | Cliente exactamente na mínima | passa (o mínimo é inclusivo) |
| AC-12 | Cliente 1.1.9, mínima 1.2.0 | 426; a mensagem diz **a versão instalada, a mínima e o que fazer** |
| AC-13 | Sem cabeçalho, `require=false` | passa (curl, testes, integrações antigas) |
| AC-14 | Sem cabeçalho, `require=true` | 426 "sem versão declarada" |
| AC-15 | Mínima `0.0.0` | deixa passar tudo |
| AC-16 | Versão ilegível com mínima definida | 426 com corpo explicativo |

**Execução:** `mvn -o test -Dtest=SemanticVersionTest,ClientVersionInterceptorTest` → 15 testes,
0 falhas. Suite completa: **524 testes, 0 falhas** (2026-08-16).

---

## Executados ao vivo — 2026-08-16

Backend próprio no porto **8081** com `--app.client.min-version=1.5.0` (para não tocar no
backend de desenvolvimento no 8080).

| ID | Cenário | Resultado |
|---|---|---|
| AC-20 | `GET /api/version` sem autenticação | ✅ `{"serverVersion":"1.0.0","minClientVersion":"1.5.0"}` |
| AC-21 | Login com `X-Client-Version: 1.0.0` | ✅ **HTTP 426** |
| AC-22 | Login com `X-Client-Version: 1.5.0` | ✅ HTTP 200, sessão criada |
| AC-23 | Login **sem** o cabeçalho | ✅ HTTP 200 — não parte o que já existe |
| AC-24 | Corpo do 426 traz a mensagem | ✅ depois da correcção (ver abaixo) |

### Defeito encontrado a correr (não aparecia nos testes)

A primeira execução ao vivo devolveu o 426 **sem mensagem nenhuma**:

```json
{"timestamp":"...","status":426,"error":"Upgrade Required","path":"/api/auth/login"}
```

Causa: `response.sendError(código, mensagem)` — o Spring Boot **descarta o texto** a menos que
`server.error.include-message=always` esteja ligado. A mensagem cuidada nunca chegaria ao
operador, que é precisamente o objectivo da funcionalidade.

Não se ligou a propriedade global (exporia as mensagens internas de **todos** os erros para
resolver um caso). O interceptor passou a escrever o corpo JSON directamente, no mesmo formato
do `GlobalExceptionHandler`. Os testes foram ajustados para verificarem o **corpo**, não a
chamada ao `sendError` — que era o que os deixava passar com o defeito presente.

---

### `ClientVersionRegistryTest` — saber quem está em quê

| ID | Cenário | Esperado |
|---|---|---|
| AC-30 | Primeira vez que uma empresa aparece numa versão | grava empresa, versão, utilizador e as duas datas |
| AC-31 | 50 pedidos seguidos da mesma loja | **uma** gravação — não se escreve a cada pedido |
| AC-32 | Empresas e versões diferentes | linhas diferentes |
| AC-33 | Versão já conhecida | actualiza a última vez vista; **não** reescreve a primeira |
| AC-34 | Sem empresa ou sem versão | não grava nada |
| AC-35 | Base de dados a falhar | **não rebenta** o pedido |
| AC-36 | Depois de uma falha | volta a tentar no pedido seguinte |
| AC-37 | Lista de utilização | diz empresa, versão e último utilizador |

**Execução:** `mvn -o test -Dtest=SemanticVersionTest,ClientVersionInterceptorTest,ClientVersionRegistryTest`
→ 23 testes, 0 falhas (2026-08-16).

---

## Por fazer (precisam do desktop a correr e do instalador)

| ID | Cenário | Esperado |
|---|---|---|
| AC-50 | Desktop antigo contra servidor com mínima acima | mensagem "Actualize o programa", com a versão instalada |
| AC-51 | Desktop actual | funciona sem aviso |
| AC-52 | Servidor com versão mais recente que o desktop | rodapé mostra "Versão X disponível" e **não** bloqueia |
| AC-53 | `GET /api/platform/client-versions` com superadmin | lista das empresas e respectivas versões |
| AC-54 | O mesmo com utilizador normal | recusado |
| AC-55 | Instalador `jpackage` + Inno Setup por utilizador | **não implementado** — ver spec §6 |
| AC-56 | Actualização preserva `desktop.api.base-url` | **não implementado** |
