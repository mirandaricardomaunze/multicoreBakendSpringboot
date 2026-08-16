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

## 7. Aviso discreto (o que faz as lojas actualizarem)

O desktop pergunta a versão ao servidor no arranque e, se houver uma mais recente, mostra no
**rodapé**: *"Versão 1.5.0 disponível"*.

Não é um diálogo de propósito. Um diálogo a meio de uma venda é fechado sem ler; um aviso
permanente no rodapé é visto no fecho do dia — que é quando dá jeito actualizar. **É este aviso
que faz as lojas actualizarem, não o bloqueio.**

À prova de falha: se o servidor não responder, não há aviso e a loja trabalha na mesma. Um
problema a verificar a versão nunca pode impedir alguém de vender.

---

## 8. Saber quem está em quê (antes de decidir bloquear)

Sem isto, subir a versão mínima é decidir às cegas — podem ficar bloqueadas lojas que nem se
sabia que existiam nessa versão.

`ClientVersionRegistry` guarda **uma linha por (empresa, versão)**: primeira vez vista, última
vez vista e o último utilizador (para se saber a quem telefonar).
`GET /api/platform/client-versions` (superadmin) mostra a lista, e a consola da plataforma tem a
aba **"Versões dos Clientes"**.

O nome da empresa é resolvido **no servidor** (o controller compõe o registo com o
`PlatformCompanyService`), para o desktop não andar a cruzar ids com outra listagem só para
escrever um nome numa tabela — foi esse o incómodo que já existiu no painel de Compras com o
armazém. Uma empresa entretanto apagada aparece como `Empresa <id>`, e não com a célula vazia:
quem lê tem de perceber que aquilo existiu.

O rodapé da aba diz **quantas versões diferentes estão em uso** — é essa a informação que decide:
com uma só, subir a mínima é seguro; com várias, alguém vai ficar de fora.

Duas regras que este registo não pode quebrar:

1. **Não escrever a cada pedido.** Uma loja faz milhares de pedidos por dia e muda de versão uma
   vez por mês. Só grava de 15 em 15 minutos por chave; o resto decide-se em memória.
2. **Nunca partir um pedido.** É informação de gestão, não parte da venda: se a gravação falhar,
   engole-se o erro (e tenta-se no pedido seguinte).

Regista-se no `postHandle`, **depois** da autenticação: no `preHandle` qualquer um poderia encher
a tabela com versões inventadas.

---

## 9. Política recomendada para o travão

| Fase | Versão mínima |
|---|---|
| Hoje (sem instalador nem actualização automática) | **0.0.0** — não bloquear ninguém |
| Com instalador e aviso a funcionar | 0.0.0 na mesma; o aviso trata de 95% dos casos |
| Alteração que faça a versão antiga **errar em dinheiro ou imposto** | subir, depois de olhar para a lista de §8 e avisar as lojas |

O critério **não** é "são incompatíveis?". É: *"o que a versão antiga faz está errado em dinheiro
ou em imposto?"*

- Falta-lhe uma funcionalidade → **não bloquear**. O que faz está certo; a loja continua a vender.
- Calcularia **IVA errado**, totais errados ou contabilidade errada → **bloquear**. Uma factura
  errada já foi entregue ao cliente e comunicada à AT; não se corrige depois.

Bloquear uma loja é pará-la de vender. O prejuízo é dela, a culpa é de quem fez o software — por
isso a barra é alta.

---

## 10. Limites conhecidos (v1)

- Não há descarga nem instalação automática — o aviso diz que há versão nova, mas actualizar
  ainda é manual.
- A versão mínima é global; não há política por loja nem período de tolerância automático.
- A lista de §8 mostra as versões **vistas**, não as instaladas: uma loja que não abra o programa
  há um mês aparece com a versão de há um mês. A coluna "Último acesso" existe precisamente para
  isso ser visível.
- Não há alerta activo (email/notificação) quando aparece uma versão antiga — é preciso ir ver.
