# Especificação Funcional & Técnica: Manifesto de Carga & Gestão de Rotas de Distribuição

## 1. Visão Geral e Enquadramento de Negócio

No ecossistema logístico e comercial de Moçambique (distribuição grossista, retalho alimentar, medicamentos e materiais de construção), após a conferência e separação de mercadorias no armazém, múltiplos pedidos ou Guias de Remessa precisam de ser consolidados numa viatura específica para entrega itinerante por rota.

O **Manifesto de Carga e Gestão de Rotas de Distribuição (Fleet Manifest & Dispatch Routes)** resolve o desafio da expedição e conferência no camião/carrinha:
1. Agrupa múltiplas Guias de Remessa ou Encomendas num único documento de expedição oficial.
2. Associa a viatura (`matricula`, marca/modelo) e motorista responsável (`Fleet/Viatura`).
3. Consolida e calcula volumes globais:
   - Total de Clientes / Pontos de Parada.
   - Total agregado de Caixas (`Cx.`) e Embalagens avulsas (`Emb.`), respeitando a regra de embalagens multinível de 2 casas decimais (`0.00`).
   - Valor Total de Mercadoria Transportada (MT).
   - Valor Total a Cobrar no Destino (para clientes com modalidade Pronto Pagamento / Dinheiro à Entrega).
4. Emite o documento executivo A4 **Manifesto de Carga & Rota de Distribuição** com grelha de entregas, contactos, horários e espaços formais para assinatura/carimbo do cliente recebedor e motorista.

---

## 2. Invariantes e Regras de Negócio

1. **Rastreabilidade de Documentos de Origem:**
   - O manifesto agrupa documentos existentes no sistema: prioritariamente **Guias de Remessa Aprovadas** (`APPROVED`) ou **Encomendas Separadas** (`SEPARATED`).
   - Cada ponto de parada (*stop*) referencia o cliente, endereço/contacto, número de documento e discriminativo de caixas/embalagens.

2. **Cálculo Físico de Caixas e Embalagens:**
   - As quantidades de caixas (`boxes`) e embalagens avulsas (`packages`) são calculadas somando as linhas dos documentos incluídos.
   - Produtos sem caixa definida no cadastro assumem `0.00` caixas.

3. **Valores Financeiros & Condições de Pagamento:**
   - Se o cliente tiver modalidade a crédito / fatura em conta-corrente, o manifesto exibe `"Conta Corrente / Faturado"`.
   - Se o documento tiver cobrança pendente (pronto pagamento ou contra-entrega), o manifesto destaca `"A Cobrar: {Valor} MT"`.

4. **Ciclo de Vida do Manifesto:**
   - `DRAFT`: Rascunho / em planeamento de carga.
   - `IN_TRANSIT` (Em Trânsito): Viatura carregada e despachada para a rota.
   - `COMPLETED` (Concluído): Rota finalizada e comprovativos devolvidos.
   - `CANCELLED` (Cancelado): Carga cancelada antes do despacho.

5. **Design e Interface:**
   - Segue estritamente `docs/UI_DESIGN_SYSTEM.md` e `AGENTS.md`.
   - Sem emojis Unicode (apenas ícones vetoriais FontAwesome).
   - Integração direta em `DeliveryGuidesPanel` e `FleetPanel`.
   - Modal de criação com seleção ergonómica de viatura, motorista e rota.

---

## 3. Modelo de Dados e Contratos (Módulo `contracts`)

### DTOs em `mz.multicore.erp.modules.fleet.dto`:

- `FleetManifestStopDTO`:
  - `sequence` (int - ordem de entrega 1, 2, 3...)
  - `documentType` (String - "GUIA_REMESSA", "ENCOMENDA")
  - `documentId` (Long)
  - `documentNumber` (String)
  - `clientId` (Long)
  - `clientName` (String)
  - `deliveryAddress` (String)
  - `contactPhone` (String)
  - `boxCount` (BigDecimal)
  - `packageCount` (BigDecimal)
  - `totalAmount` (BigDecimal)
  - `amountToCollect` (BigDecimal)
  - `paymentTerms` (String)

- `FleetManifestDTO`:
  - `id` (Long)
  - `manifestNumber` (String - ex: `MC-2026/0001`)
  - `companyId` (Long)
  - `manifestDate` (LocalDateTime)
  - `status` (String - `DRAFT`, `IN_TRANSIT`, `COMPLETED`, `CANCELLED`)
  - `vehicleId` (Long)
  - `vehiclePlate` (String)
  - `vehicleModel` (String)
  - `driverName` (String)
  - `driverPhone` (String)
  - `routeName` (String)
  - `departureTime` (String)
  - `notes` (String)
  - `totalStops` (int)
  - `totalBoxes` (BigDecimal)
  - `totalPackages` (BigDecimal)
  - `totalAmount` (BigDecimal)
  - `totalAmountToCollect` (BigDecimal)
  - `stops` (List<FleetManifestStopDTO>)

- `CreateFleetManifestRequest`:
  - `vehicleId` (Long)
  - `driverName` (String)
  - `driverPhone` (String)
  - `routeName` (String)
  - `departureTime` (String)
  - `notes` (String)
  - `deliveryGuideIds` (List<Long>)

---

## 4. Arquitectura de Impressão (Módulo `printing`)

- Classe: `FleetManifestPrintService`
- Layout A4:
  - Cabeçalho executivo corporativo da empresa.
  - Metadados de Transporte: Viatura, Condutor, Rota, Data/Hora de Saída.
  - Cartões de resumo KPI de Carga: `Nº Paradas`, `Total Caixas`, `Total Embalagens`, `Valor Total`, `Total a Cobrar`.
  - Tabela com linhas zebradas limpas contendo os pontos de paragem e colunas de conferência:
    - `Ordem | Cliente & Contacto | Destino | Guia/Doc | Volumes (Cx / Emb) | Condição | Hora | Assinatura Recebedor`
  - Bloco de rodapé com assinaturas do Conferente de Armazém e do Motorista.

---

## 5. Estratégia de Testes e Harness

- `FleetManifestServiceTest` (backend): valida criação de manifesto, agregação de caixas, soma de valores e estados.
- `FleetManifestPrintServiceTest` (backend): valida renderização do PDF do manifesto (cabeçalhos, paradas, totais).
- `FleetManifestUiHarnessTest` (desktop): valida formatação de colunas, mapeamento de viatura e criação de DTOs.
- `MultiModuleArchitectureHarnessTest`: garante independência de fronteiras.
- `DesktopThinContextTest`: valida instanciação sem falhas no Spring Desktop.
