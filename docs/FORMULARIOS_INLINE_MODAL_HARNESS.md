# Harness — Formulários inline → modal (CRM, Financeiro, Config)

> Cenários de validação manual da [spec](FORMULARIOS_INLINE_MODAL_SPEC.md). UI/Swing — verificação
> manual. Critério técnico: `mvn clean compile` + app arranca + `mvn test` sem regressões.

**Última actualização:** 2026-06-30

| ID    | Passos                                                                          | Esperado                                                                                   |
|-------|---------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|
| FI-01 | Abrir **CRM**                                                                   | Folhas de Obra a largura total; botões **Registar Folha de Obra** + **Faturar** no topo.   |
| FI-02 | CRM › **Registar Folha de Obra** (sem tickets abertos)                          | Aviso "Não existem tickets em aberto"; modal não abre.                                      |
| FI-03 | CRM › **Registar Folha de Obra** (com ticket aberto) → preencher → Gravar       | Modal premium (`fas-tools`); grava; ticket fica RESOLVIDO; lista actualiza.                 |
| FI-04 | CRM › Gravar com técnico/descrição vazios                                       | Erro de validação; **modal mantém-se aberto**.                                              |
| FI-05 | Abrir **Financeiro**                                                            | Histórico de fluxo a largura total; botão **Registar Recebimento** no topo.                 |
| FI-06 | Financeiro › **Registar Recebimento** → escolher fatura + conta → Receber       | Modal (`fas-money-bill-wave`); liquida; saldo e movimentos actualizam.                      |
| FI-07 | Financeiro sem faturas aprovadas / sem contas                                   | Aviso adequado; modal não abre.                                                            |
| FI-08 | Abrir **Configurações › Utilizadores**                                          | Lista a largura total; botões **Novo Utilizador**, **Alterar Perfil**, **Atualizar Lista**.|
| FI-09 | Config › **Novo Utilizador** → preencher → Registar                             | Modal (`fas-user-plus`); cria utilizador; lista actualiza.                                  |
| FI-10 | Config › **Novo Utilizador** com campo vazio                                    | Erro "Todos os campos são obrigatórios"; modal fica aberto.                                |
| FI-11 | Config › selecionar utilizador → **Alterar Perfil**                             | Modal (`fas-user-shield`) com combo pré-seleccionado no perfil actual; altera e actualiza. |
| FI-12 | Config › **Alterar Perfil** sem selecção                                        | Aviso "Selecione um utilizador"; modal não abre.                                           |

## Verificação técnica

```
mvn clean compile     # 0 erros
mvn clean test        # sem regressões
```
