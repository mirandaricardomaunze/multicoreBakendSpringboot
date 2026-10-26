# SPEC — Arranque Enxuto do Perfil Vendedor

## Problema

O menu e a navegação já respeitavam a matriz de permissões, mas o `MainFrame` continuava a construir
painéis de gestão proibidos ao perfil `SELLER`. Isso aumentava o custo de arranque e memória e deixava
componentes não autorizados vivos no cliente, embora a segurança efectiva permanecesse no backend.

## Decisão

Quando a sessão activa é `SELLER`, o desktop constrói apenas:

- `SellerDashboardPanel`;
- `POSPanel`;
- `ComercialPanel`, na variante reduzida de vendedor;
- `ClientesPanel`;
- `CRMPanel`;
- `NotificationsPanel`.

Painéis de stock, compras, finanças, RH, desempenho, fiscalidade, contabilidade, activos, produção,
frotas, avenças, aprovações, configuração, auditoria forense e assistência de gestão não são
instanciados nem adicionados ao `CardLayout`.

Gestor e administrador mantêm a composição completa. A política HTTP do backend continua a ser a
fronteira de segurança; esta decisão é optimização, redução da superfície visual e defesa em profundidade.

## Critérios de aceitação

1. Os módulos autorizados do vendedor continuam navegáveis.
2. Nenhum painel de gestão é construído para `SELLER`.
3. O `CardLayout` aceita apenas componentes não nulos.
4. Callbacks que referenciem painéis opcionais são null-safe.
5. `SellerLeanStartupHarnessTest`, `SellerExperienceHarnessTest` e `DesktopThinContextTest` passam.
