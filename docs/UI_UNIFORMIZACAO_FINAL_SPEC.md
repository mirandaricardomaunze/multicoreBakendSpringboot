# SPEC — Uniformização final do design Swing

**Criado em:** 2026-08-15  
**Âmbito:** perfis, Plataforma, Fiscal, cores, idioma e formulários legados.

## 1. Objectivo

Eliminar as últimas divergências visuais detectadas após a profissionalização de navegação, RH,
Configurações, Stock e Comercial, sem alterar códigos persistidos, contratos HTTP ou regras.

## 2. Perfis e valores técnicos

- `ADMIN`, `MANAGER`, `EMPLOYEE` e `SUPERADMIN` permanecem códigos internos.
- Tabelas e selects apresentam Administrador, Gestor, Funcionário e Administrador da Plataforma.
- Filtros continuam a trabalhar sobre o código interno para não alterar comportamento.
- Mensagens não apresentam combinações técnicas como `MANAGER/ADMIN`.

## 3. Plataforma e Fiscal

- Empresas: Nova e Editar visíveis; estado/actualização em “Mais acções”.
- Assinaturas: Definir Plano e Registar Pagamento visíveis; histórico, suspensão e actualização
  agrupados.
- Utilizadores globais: Novo, Editar e Conceder Acesso visíveis; revogar, senha, estado e
  actualização agrupados.
- Fiscal/IVA: imprimir, exportar e validar agrupados em “Documentos”.

## 4. Formulários

- `UIHelper.createDialogForm` usa `FormField` como célula canónica para pares label/componente.
- Labels com `*` geram marcador obrigatório e mantêm o componente original.
- Styling e valores dos campos permanecem inalterados.
- `ModernFormDialog` continua responsável por scroll, limites e submissão.

## 5. Cores e idioma

- Painéis de negócio não criam `new Color(...)`; usam tokens de `UIHelper`/`Theme`.
- Componentes de desenho podem compor transparências internamente.
- Verbo canónico: “Actualizar”.
- Operação de produto: “Registar Produto”, não “Cadastrar”.

## 6. Definition of done

- Harness FU-01..12 verde.
- Nenhuma cor ad-hoc nos painéis de negócio.
- Nenhum “Atualizar”, “Cadastrar” ou `MANAGER/ADMIN` visível na árvore `gui`.
- Build limpo e suite completa verdes.
- Validação manual FU-20..23 registada sem substituir evidência automática.

