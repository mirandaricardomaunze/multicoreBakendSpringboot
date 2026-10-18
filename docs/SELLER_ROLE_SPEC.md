# SPEC - Perfil de Vendedor

Estado: **obrigatório**  
Âmbito: autenticação, autorização por empresa e apresentação no desktop

## Objectivo

Formalizar o perfil operacional de vendas sem lhe conceder poderes de aprovação ou administração.
O código persistido e transportado pela sessão é `SELLER`; o texto apresentado ao utilizador é
**Vendedor**.

## Hierarquia

| Perfil | Designação | Operação comercial normal | Operação sensível | Gestão de utilizadores |
|---|---|---:|---:|---:|
| `EMPLOYEE` | Funcionário | Não | Não | Não |
| `SELLER` | Vendedor | Sim | Não | Não |
| `MANAGER` | Gestor | Sim | Sim | Não |
| `ADMIN` | Administrador | Sim | Sim | Sim |

`MANAGER` e `ADMIN` incluem a capacidade de vendedor. `SELLER` nunca inclui capacidade de gestor.
`SUPERADMIN` é um papel de plataforma separado e não participa nesta hierarquia de empresa.

## Regras

1. O papel efectivo é sempre o papel do acesso à empresa activa, não apenas o papel legado da conta.
2. Criar e actualizar encomendas de clientes exige `SELLER`, `MANAGER` ou `ADMIN`.
3. Facturar directamente, aprovar/rejeitar, cancelar documentos, efectuar devoluções sensíveis
   e gerir utilizadores continuam restritos ao guard específico já definido para a operação.
4. O backend é a autoridade. Ocultar acções no desktop é apenas ergonomia e nunca substitui o guard.
5. A gestão de utilizadores aceita apenas `EMPLOYEE`, `SELLER`, `MANAGER` e `ADMIN`.
6. Selects e tabelas mostram designações em português de Moçambique; os códigos internos não
   devem aparecer ao utilizador.

## POS

`SELLER`, `MANAGER` e `ADMIN` podem abrir sessão de caixa, efectuar checkout e fechar sessão
sem diferença. O vendedor só pode abrir e usar a sua própria sessão e só pode fechá-la quando
é o operador actual, incluindo depois de uma passagem de turno. O backend compara o operador
informado no pedido com a identidade autenticada; o nome enviado pelo cliente não concede acesso.

Fecho com diferença, sangria e devolução continuam a exigir `MANAGER` ou `ADMIN`. `EMPLOYEE`
e o código legado `CASHIER` não concedem acesso ao POS. A mudança de perfil de operadores
existentes deve ser feita por um administrador da empresa antes do próximo uso do POS.

## Critérios de aceite

- `SELLER` é aceite e normalizado na criação e alteração de utilizadores.
- `SELLER`, `MANAGER` e `ADMIN` passam a guarda de venda; `EMPLOYEE` é recusado.
- Um vendedor cria e actualiza encomendas, mas não executa operações de gestor ou administrador.
- O vendedor abre, vende e fecha sem diferença na sua própria sessão; outro perfil ou operador é recusado.
- O desktop apresenta `SELLER` como **Vendedor** e não oferece roles inexistentes no backend.
- `SellerRoleHarnessTest`, `DesktopSellerRoleHarnessTest`, `MultiModuleArchitectureHarnessTest` e
  `DesktopThinContextTest` passam.
