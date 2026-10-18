# Matriz de perfis e permissões

## Objectivo

Definir uma política única de autorização para `EMPLOYEE`, `SELLER`, `MANAGER` e `ADMIN`.
O backend é a autoridade; esconder opções no desktop serve apenas para ergonomia e nunca substitui
a validação HTTP e as guardas de negócio.

## Hierarquia

`EMPLOYEE < SELLER < MANAGER < ADMIN`

- `EMPLOYEE` — acesso mínimo de auto-serviço, sem POS, encomendas, finanças ou gestão.
- `SELLER` — execução comercial e POS, sem aprovação, configuração ou áreas financeiras.
- `MANAGER` — gestão operacional, aprovações e áreas financeiras, sem administração de utilizadores
  ou cópias de segurança.
- `ADMIN` — administração integral da empresa. `SUPERADMIN` permanece um papel separado da plataforma.

## Matriz canónica

| Área/operação | EMPLOYEE | SELLER | MANAGER | ADMIN |
|---|---:|---:|---:|---:|
| Licença/subscrição da empresa | Consulta | Consulta | Consulta | Total |
| Saúde ocupacional própria | Consulta | Consulta | Consulta | Total |
| Painel operacional | Não | Consulta | Total | Total |
| Clientes | Não | Criar/consultar/editar | Total | Total |
| Produtos/categorias | Não | Consulta | Total | Total |
| POS e caixa próprio | Não | Operar sessão própria | Total | Total |
| Devoluções, sangrias e acções sensíveis de caixa | Não | Não | Total | Total |
| Cotações | Não | Operar | Total | Total |
| Converter cotação directamente em factura | Não | Não | Total | Total |
| Encomendas | Não | Criar/consultar/editar | Total | Total |
| Aprovar, rejeitar, cancelar ou facturar encomenda | Não | Não | Total | Total |
| Facturas | Não | Consulta/impressão | Total | Total |
| CRM e assistência | Não | Operar | Total | Total |
| Promoções | Não | Consulta | Total | Total |
| Stock geral | Não | Consulta | Total | Total |
| Compras, transferências, contagens e quebras | Não | Não | Total | Total |
| Finanças, contabilidade, fiscal e relatórios | Não | Não | Total | Total |
| RH e salários | Apenas resumo pessoal | Não | Total | Total |
| Utilizadores e perfis | Não | Não | Não | Total |
| Configuração e backup | Não | Não | Não | Total |

## Regras técnicas

1. `ApiRolePolicy` bloqueia cada área antes do Controller e adopta *deny by default* para endpoints
   novos ou não classificados.
2. Services preservam as guardas de operações sensíveis. No POS, um vendedor só abre, usa e fecha
   a sua própria sessão; gestor ou administrador pode intervir.
3. `SignedInUser.canNavigate` limita os módulos desenhados no desktop, mas não concede permissões.
4. Alterar esta matriz exige actualizar a política, os harnesses backend/desktop e esta especificação.

## Critérios do harness

- EMPLOYEE não acede clientes, POS, encomendas, finanças, RH geral nem endpoints desconhecidos.
- SELLER acede ao catálogo, clientes, POS, cotações, encomendas, CRM e impressão comercial.
- SELLER não aprova, factura, gere promoções, devoluções, finanças, utilizadores ou backup.
- MANAGER gere a operação, mas não administra utilizadores nem backup.
- ADMIN mantém acesso integral no tenant.
- A barra lateral e a navegação programática do desktop respeitam a mesma divisão funcional.
