# Contas partilhadas, credenciais e segredos — Especificação e harness

**Última actualização:** 2026-11-01

## Objectivo

Fechar as falhas da revisão de segurança antes de produção: tomada de contas entre empresas,
credenciais previsíveis, anexos clínicos em claro, sessões sem limite e contagem de falhas de login.

## Decisões

### Contas entre empresas

- Senha, PIN, nome e estado valem em todas as empresas da conta. Só os altera quem administra todas
  essas empresas, ou o administrador da plataforma.
- Desactivar uma conta partilhada retira apenas o acesso à empresa activa e termina as sessões.
- Reactivar uma conta partilhada é feito na plataforma.
- Conceder acesso a uma empresa exige ser administrador dessa empresa.

### Credenciais e exposição

- O código não tem senha de superadmin por omissão: sem `SUPERADMIN_PASSWORD`, a conta não é criada.
- Os dados de demonstração estão desligados por omissão no código. `application.properties` liga-os
  no ambiente de desenvolvimento, que escuta só em `127.0.0.1`.
- Com dados de demonstração, o servidor recusa arrancar num endereço acessível pela rede.
- `SERVER_ADDRESS` altera o endereço de desenvolvimento. O perfil `prod` usa `0.0.0.0`, porque recebe
  os pedidos do Caddy, e o Compose não publica a porta do backend.
- O atalho "Superadmin" saiu do login de demonstração.

### Anexos clínicos

- O perfil `prod` exige `ATTACHMENT_KEY`: sem chave, o servidor não arranca.
- O Compose transmite `ATTACHMENT_KEY` e `SUPERADMIN_PASSWORD` ao backend e recusa arrancar sem a chave.
- No arranque, os anexos ainda em claro são cifrados, um por transacção.

### Sessões e login

- As sessões expiradas são removidas de 10 em 10 minutos e em cada login.
- Cada utilizador tem até 10 sessões; ao exceder, cai a mais antiga.
- Um login válido limpa só o contador da própria conta. O contador do IP expira sozinho.

### Desconto e preço no POS

- O servidor decide. Um caixa vende a preço de tabela, com promoção, com o combinado numa cotação, ou
  até 10% de desconto. Acima disso é precisa uma autorização de gerente.
- `POST /api/pos/discount-authorization` troca o PIN por uma autorização com validade de 30 minutos.
  Vale uma venda, na empresa, até à percentagem aprovada, e fica na auditoria com o nome do gerente.
- Um caixa não vende abaixo do preço de tabela. Gerente e administrador sim.

### Senha

- Uma senha definida por outra pessoa, ao criar ou repor a conta, é trocada no primeiro acesso.
- A troca exige a senha actual e termina as sessões. Até lá, a sessão só serve para a trocar.
- A plataforma passa a respeitar a política de senhas.

### Dupla autenticação

- Códigos TOTP de 6 dígitos, compatíveis com Google Authenticator e semelhantes.
- Obrigatória em produção para administradores e plataforma (`security.two-factor.required`).
- A chave é mostrada no primeiro login. Até confirmar um código, a sessão fica restrita.
- O segredo é guardado cifrado com `ATTACHMENT_KEY`.
- Telemóvel perdido: a plataforma repõe, e o titular configura de novo.

### Segredos de produção

- O servidor não arranca com `DB_PASSWORD` ou `SUPERADMIN_PASSWORD` curtas ou de exemplo, nem com
  uma `ATTACHMENT_KEY` de exemplo.

### Dependências

- Spring Boot 3.5.16, com Tomcat, Jackson, PostgreSQL, Log4j e Commons Lang fixados em versões
  corrigidas.
- `scripts/check-vulnerabilities.py` consulta a base OSV, e o job `dependency-vulnerabilities`
  reprova o build com qualquer vulnerabilidade fora de `scripts/vulnerability-exceptions.txt`.
- As duas excepções do Spring MVC só têm correcção na série 7 e tocam em funções que o sistema não
  usa: Server-Sent Events e XsltView.

### Backups

- A cifra de backups antigos só apaga o original após autenticar o `.enc` e comparar SHA-256 do
  conteúdo decifrado com o original, por streaming. Conteúdo diferente ou falha de leitura preserva
  ambos os ficheiros e regista erro; uma cópia cifrada existente nunca é sobrescrita por este processo.
- Com chave, cada backup físico é cifrado com AES-256-GCM, por blocos, para `.dump.enc`.
- Antes de apagar a versão em claro, o sistema confirma com `pg_restore --list` que o dump é legível e
  que a cópia cifrada abre. O restauro decifra para um ficheiro temporário, apagado no fim.
- O restauro completo continua a ser provado na CI, em PostgreSQL.

## Harness

| Id | Cenário | Espera-se |
|----|---------|-----------|
| SC-01 | Repor senha de conta partilhada | Recusado, senha intacta |
| SC-02 | Administrador de todas as empresas da conta | Permitido, sessões terminadas |
| SC-03 | Desactivar conta partilhada | Sai só da empresa activa |
| SC-04 | Conta exclusiva da empresa | Comportamento normal |
| SC-05 | Conceder acesso a outra empresa | Recusado |
| SC-06 | Demonstração fora de loopback | Arranque recusado |
| SC-07 | Valores por omissão | Sem senha nem demonstração no código |
| SC-08 | Produção sem chave | Arranque recusado |
| SC-09 | Sessões expiradas | Removidas sem uso do token |
| SC-10 | Muitos logins | Máximo de 10 sessões por utilizador |
| SC-11 | Falhas intercaladas com sucessos | IP bloqueado |
| SC-12 | Anexos antigos | Cifrados uma só vez |
| SC-13 | Desconto até 10% | Permitido ao caixa |
| SC-14 | Desconto acima de 10% sem autorização | Recusado pela API |
| SC-15 | Reutilizar autorização ou exceder a percentagem | Recusado |
| SC-16 | Promoção activa ou gerente | Permitido |
| SC-17 | Caixa envia preço abaixo da tabela | Recusado |
| SC-18 | Trocar senha | Exige a actual e termina as sessões |
| SC-19 | Sessão restrita na API | Recusada |
| SC-20 | Código TOTP | Vector da RFC 6238; expira |
| SC-21 | Âmbito da dupla autenticação | Administradores e plataforma |
| SC-22 | Segredos fracos em produção | Arranque recusado |
| SC-23 | Backup cifrado | Abre igual e sem conteúdo em claro |
| SC-24 | Backup alterado, cortado ou com outra chave | Recusado |
| SC-25 | Código 2FA aceite, usado de novo | Recusado, também com pedidos em simultâneo |
| SC-26 | Código usado na configuração, repetido no login | Recusado |
| SC-27 | Conta com senha de demonstração em produção | Senha substituída, sessões terminadas |
| SC-28 | Superadmin com senha de demonstração, sem `SUPERADMIN_PASSWORD` | Arranque recusado |
| SC-29 | Senha antiga em texto simples | Passa a hash no arranque |
| SC-30 | Falha ao cifrar um backup | A cópia em claro fica; nada se perde |
| SC-31 | Backup antigo em claro (`.dump`, `.json`) | Cifrado com a data original; o claro sai depois de verificado |
| SC-32 | Backup lógico novo | Gravado cifrado e verificável |

Testes: `SharedAccountSecurityHarnessTest`, `SessionAndLoginLimitsHarnessTest`,
`MandatorySecurityHarnessTest`, `PreProductionSecurityHarnessTest` e `BackupAtRestHarnessTest`.

## Verificação do servidor

`scripts/validar-producao.sh` corre no VPS e confirma o que os testes não alcançam: commit
instalado, última migração, segredos, portas, HTTPS, senhas de demonstração e backups.

## Limites

- Não há recuperação de senha pelo próprio titular: uma conta partilhada depende da plataforma.
- Superadmin sem telemóvel e sem outro superadmin: a dupla autenticação só se repõe na base de dados.
- A venda em contingência com desconto acima de 10% fica para revisão se a autorização expirar.
- Um desconto retomado de uma venda suspensa precisa de novo PIN.
- `ATTACHMENT_KEY` protege anexos, segredos TOTP e backups: perdê-la impede restaurar os backups.
- As sessões vivem em memória: reiniciar o backend termina-as e várias instâncias não as partilham.
- Cifrar um backup antigo não apaga cópias já feitas fora do servidor, e o disco pode reter blocos
  do ficheiro apagado: essas cópias têm de ser destruídas ou substituídas pelo operador.
- Backups em claro têm uma hora de espera antes de serem cifrados, para não apanhar um ficheiro em escrita.
- Perder `ATTACHMENT_KEY` é perder os anexos cifrados.
- Só se detectam as senhas de demonstração conhecidas (`admin`, `password`, `superadmin`): outra
  senha fraca antiga só muda quando o titular a trocar.
- Depois da substituição, a conta só volta a entrar com uma senha reposta pelo administrador.
