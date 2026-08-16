# Actualizações do desktop e compatibilidade com o servidor — especificação

**Criado em:** 2026-08-16
**Estado:** aperto de mão de versão implementado (AC-01..AC-16); instalador **por fazer**
**Origem:** pergunta do utilizador — *"como fazer actualizações depois de instalador
profissional"*.

---

## 1. O problema

Desde o Track B (cliente-fino) o mesmo codebase instala-se em **dois sítios com ritmos
diferentes**:

| Metade | Onde | Quem actualiza | Quando |
|---|---|---|---|
| Backend | VPS que a empresa controla | o dono do produto | quando quiser |
| Desktop | computador de cada loja | tem de chegar a cada loja | quando a loja deixar |

O desvio de versões não é uma hipótese, é uma **certeza**: o servidor actualiza hoje, a loja da
Beira só daqui a dois meses.

O sintoma disso não parece um problema de versões. Parece isto: o operador carrega em *Emitir
Factura*, aparece um erro sem explicação, e quem for ver o log do servidor encontra um **400**
por causa de um campo que o cliente antigo não manda — ou que manda e já não existe. Perde-se
uma tarde a procurar no sítio errado.

**Este documento resolve a parte do diagnóstico. O instalador é a §6 e ainda não existe.**

---

## 2. A regra: o cliente identifica-se sempre

O desktop manda `X-Client-Version` em **todos** os pedidos (`DesktopApiClient.request(...)`, um
só sítio). O servidor compara com `app.client.min-version` e decide:

| Situação | Resposta |
|---|---|
| versão ≥ mínima | passa |
| versão < mínima | **426 Upgrade Required** + mensagem que diz a versão instalada, a mínima e o que fazer |
| sem cabeçalho, `require-version=false` (default) | passa |
| sem cabeçalho, `require-version=true` | 426 |

**426 e não 400** de propósito: o 400 diz *"o teu pedido está mal feito"* e manda procurar no
sítio errado; o 426 diz exactamente qual é o problema.

### Dois defaults conservadores, deliberados

1. **`app.client.min-version = 0.0.0` — não bloqueia ninguém.** Uma política que tranca lojas
   fora do sistema não pode entrar ligada por acidente. Sobe-se quando for mesmo preciso, e cada
   subida obriga as lojas a actualizar **antes de poderem trabalhar** — avisar antes.
2. **Sem cabeçalho passa.** `curl`, testes e integrações antigas não mandam nada e não têm de
   partir por causa disto. Quem quiser apertar liga `app.client.require-version=true`.

### Comparar versões é onde os erros se escondem
`SemanticVersion` compara **número a número**. Em ordem alfabética `"1.10.0" < "1.9.0"` — e o
resultado seria bloquear um cliente **novo** como se fosse velho. Versão ausente ou ilegível
conta como a mais antiga possível: quem não se identifica não pode ser tratado como o mais
recente, senão contornar a política era só mandar lixo no cabeçalho.

---

## 3. De onde vem a versão

`app.version=@project.version@` no `application.properties`, substituído pelo Maven a partir do
`<version>` do `pom`. Nunca escrito à mão — um número à mão fica a mentir na primeira release.

`ClientVersion` lê-o **sem Spring** (utilitário estático), porque tem de funcionar nos dois
entrypoints: o desktop é um contexto não-web que só faz scan de `com.phcpro.desktop`/`gui` e não
veria um bean de `architecture`. Cai para o manifesto do jar e, em último caso, para
`0.0.0-dev`.

---

## 4. `GET /api/version` (público)

```json
{ "serverVersion": "1.0.0", "minClientVersion": "0.0.0" }
```

Público como o `/actuator/health`, por duas razões: o desktop precisa de o consultar **antes do
login** (é no arranque que faz sentido avisar que há versão nova, não a meio de uma venda), e um
cliente já bloqueado tem de conseguir perguntar qual é a versão boa. Por isso o interceptor
exclui `/api/version` — senão a única porta de diagnóstico fechava-se a si própria.

---

## 5. Disciplina de migrações (o que evita ter de subir a mínima)

Como os desktops antigos continuam a chamar o backend novo, as alterações têm de ser
**expandir agora, contrair depois**:

- campo novo num pedido → **opcional**, com construtor retrocompatível. Já é o padrão do
  projecto: ver `SaveClientRequest` e `CreateInvoiceRequest`, que ganharam campos sem partir quem
  chamava;
- coluna a abandonar → deixa-se de escrever numa versão, larga-se **duas** versões depois;
- endpoint a substituir → o antigo fica a responder até ninguém antigo ligar (é o que se fez com
  `/invoices` vs `/invoices/page`).

Subir `min-version` é o último recurso, não a ferramenta do dia-a-dia.

---

## 6. O instalador — por fazer

Nada disto está implementado. Fica o desenho decidido:

1. **`jpackage`** (vem no JDK 21) gera o *app-image* Windows a partir de um jar com
   `DesktopApplication` como main — hoje o `pom` fixa `mainClass` no **backend**, pelo que é
   preciso um perfil Maven próprio.
2. **Instalar em `%LOCALAPPDATA%`, não em `Program Files`.** Em Program Files cada actualização
   pede administrador — e na loja ninguém sabe a password. Este detalhe sozinho decide se as
   actualizações acontecem ou ficam por fazer durante um ano.
3. **A configuração tem de sobreviver à actualização.** `desktop.api.base-url` é o que aponta a
   loja ao servidor; se o instalador o reescrever, a loja fica a apontar para `localhost` e pára.
   Guardar fora da pasta da aplicação.
4. **Assinatura de código**, senão o SmartScreen do Windows apresenta o programa como não
   reconhecido.
5. **Actualização automática**: o desktop consulta `/api/version`, compara com a sua, e propõe
   descarregar/instalar ao fechar.

---

## 7. Limites conhecidos (v1)

- O desktop **bloqueia** quando é recusado, mas ainda **não avisa** quando existe versão nova
  estando dentro do mínimo (o endpoint já dá a informação; falta o aviso na barra de topo).
- Não há descarga nem instalação automática — só a mensagem a dizer que é preciso actualizar.
- A versão mínima é global; não há política por loja.
