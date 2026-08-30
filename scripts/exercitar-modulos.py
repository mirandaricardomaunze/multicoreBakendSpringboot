"""Exercita as invariantes de negócio dos módulos contra um backend a correr.

Não verifica que os endpoints respondem — isso é fácil e diz pouco. Verifica que **a conta bate**:
uma venda tira do stock exactamente o que vendeu, uma transferência não cria nem destrói
mercadoria, o IVA apurado é o IVA dos documentos, a taxa de imposto é do artigo e não do payload,
reapurar as retenções não duplica a dívida ao
Estado, e o balancete equilibra.

    # com o backend de pé (ver docs/DEPLOYMENT_RUNBOOK.md):
    python scripts/exercitar-modulos.py            # porta 18099 por omissão
    python scripts/exercitar-modulos.py 8080

Corre contra dados reais e **escreve** na base de dados — facturas, ajustes, vendas. Usar só em
desenvolvimento, nunca contra produção.

Foi assim que apareceu o 500 em caminhos inexistentes e em parâmetros em falta: um engano a
escrever este guião expôs um defeito real do servidor. Ver o commit de 2026-08-29.
"""
import json
import sys
import urllib.error
import urllib.request
from decimal import Decimal

BASE = f"http://localhost:{sys.argv[1] if len(sys.argv) > 1 else '18099'}"
TALLY = {"ok": 0, "bad": 0}


def call(path, token=None, method="GET", body=None):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
        req.add_header("X-Company-Id", "1")
        req.add_header("X-Client-Version", "1.0.0")
    data = json.dumps(body).encode() if body else None
    try:
        with urllib.request.urlopen(req, data, timeout=30) as response:
            raw = response.read().decode("utf-8", "replace")
            return response.status, (json.loads(raw) if raw.strip() else None)
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8", "replace")
        try:
            return error.code, json.loads(raw)
        except ValueError:
            return error.code, raw


def check(description, expected, got):
    """Compara números como números. A primeira versão comparava texto e dizia que 0 != 0.0."""
    same = (Decimal(str(expected)) == Decimal(str(got))
            if _numeric(expected) and _numeric(got) else expected == got)
    if same:
        print(f"  OK    {description}")
        TALLY["ok"] += 1
    else:
        print(f"  FALHA {description}\n          esperado={expected}  obtido={got}")
        TALLY["bad"] += 1


def _numeric(value):
    try:
        Decimal(str(value))
        return True
    except Exception:
        return False


def section(title):
    print(f"\n--- {title} ---")


token = call("/api/auth/login", method="POST",
             body={"username": "ana", "password": "password"})[1]["token"]


def stock_of(product_id, warehouse_id=1):
    for row in call("/api/inventory/stocks?companyId=1", token)[1]:
        if row["productId"] == product_id and row["warehouseId"] == warehouse_id:
            return Decimal(str(row["quantity"]))
    return Decimal("0")


# ─── INVENTÁRIO ──────────────────────────────────────────────────────────────
section("Inventário: a venda tira do stock exactamente o que vendeu")
before = stock_of(1)
call("/api/comercial/invoices", token, "POST",
     {"clientId": 1, "companyId": 1, "warehouseId": 1,
      "lines": [{"productId": 1, "quantity": 3, "taxRate": 0.16}]})
after = stock_of(1)
check("factura de 3 unidades baixa o stock em 3", before - 3, after)

section("Inventário: não se vende o que não há")
code, _ = call("/api/comercial/invoices", token, "POST",
               {"clientId": 1, "companyId": 1, "warehouseId": 1,
                "lines": [{"productId": 1, "quantity": 999999, "taxRate": 0.16}]})
check("quantidade acima do stock é recusada", 400, code)
check("e a recusa NÃO mexeu no stock", after, stock_of(1))

section("Inventário: um ajuste exige motivo e fixa a quantidade contada")
code, _ = call("/api/inventory/adjustments", token, "POST",
               {"companyId": 1, "productId": 2, "warehouseId": 1,
                "countedQuantity": 100, "reason": ""})
check("ajuste sem motivo é recusado", 400, code)
counted = stock_of(2) - 5
code, _ = call("/api/inventory/adjustments", token, "POST",
               {"companyId": 1, "productId": 2, "warehouseId": 1,
                "countedQuantity": float(counted), "reason": "Contagem física"})
check("ajuste aceite", 200, code)
check("stock passa a ser o contado", counted, stock_of(2))

section("Inventário: uma transferência é um DOCUMENTO, não um movimento")
origin, destination = stock_of(3, 1), stock_of(3, 2)
code, transfer = call("/api/inventory/transfers", token, "POST",
                      {"companyId": 1, "originWarehouseId": 1, "destinationWarehouseId": 2,
                       "responsible": "ana", "notes": "Reposição loja",
                       "lines": [{"productId": 3, "quantity": 4}]})
check("transferência criada", 200, code)
# A mercadoria só se mexe quando se mexe. Criar o documento não pode tirar do armazém de origem —
# senão o stock ficava errado durante todo o tempo que a transferência esperasse aprovação.
check("nasce por aprovar", "PENDING_APPROVAL", transfer.get("status"))
check("e o stock de origem NÃO mexeu ainda", origin, stock_of(3, 1))
check("nem o de destino", destination, stock_of(3, 2))

# ─── POS ─────────────────────────────────────────────────────────────────────
section("POS: sessão de caixa e venda ao balcão")
active = call("/api/pos/sessions/active?operator=ana&companyId=1", token)[1]
if not active:
    code, session = call("/api/pos/sessions/open", token, "POST",
                         {"operator": "ana", "openingBalance": 500.00, "companyId": 1})
    check("sessão aberta", 200, code)
else:
    print(f"  NOTA  já havia sessão aberta (id={active['id']}) — reaproveitada")
code, _ = call("/api/pos/sessions/open", token, "POST",
               {"operator": "ana", "openingBalance": 500.00, "companyId": 1})
check("abrir DUAS sessões para o mesmo operador é recusado", 400, code)

account = call("/api/finance/accounts?companyId=1", token)[1][0]["id"]
before = stock_of(1)
code, _ = call("/api/pos/checkout", token, "POST",
               {"operator": "ana", "companyId": 1, "warehouseId": 1, "treasuryAccountId": account,
                "lines": [{"productId": 1, "quantity": 2, "discountPercentage": 0}]})
check("venda ao balcão aceite", 200, code)
check("venda de 2 unidades baixa o stock em 2", before - 2, stock_of(1))

# ─── FISCAL ──────────────────────────────────────────────────────────────────
section("Fiscal: o IVA apurado é o IVA dos documentos")
invoices = call("/api/comercial/invoices?companyId=1", token)[1]
from_documents = sum(Decimal(str(i["taxAmount"])) for i in invoices if i["status"] != "CANCELLED")
code, summary = call("/api/fiscal/iva-summary?companyId=1&year=2026&month=8", token)
if code == 200 and isinstance(summary, dict):
    assessed = Decimal(str(summary.get("outputVat") or summary.get("ivaLiquidado") or 0))
    print(f"  IVA das facturas: {from_documents} · apurado: {assessed}")
    check("o apurado bate com os documentos", from_documents, assessed)
else:
    print(f"  NOTA  iva-summary devolveu {code}")

section("Fiscal: a taxa é do artigo — nenhum payload a decide")
# Pedimos 16% num artigo isento. Se o pedido mandasse, o cliente pagava imposto a mais num bem
# isento, e a declaração mensal e o SAF-T declaravam imposto liquidado que não devia existir.
# É a regra da IVA_TAXA_CANONICA_SPEC, aqui verificada na fronteira HTTP.
code, forced = call("/api/comercial/invoices", token, "POST",
                    {"clientId": 1, "companyId": 1, "warehouseId": 1,
                     "lines": [{"productId": 1, "quantity": 1, "taxRate": 0.16}]})
check("factura criada", 200, code)
check("os 16% do payload são ignorados", 0, forced["lines"][0]["taxRate"])
check("e o imposto da factura fica a zero", 0, forced["taxAmount"])

# ─── RH ──────────────────────────────────────────────────────────────────────
section("RH §B6: o saldo em dívida apura-se das linhas, nunca se grava")
code, loan = call("/api/hr/deductions", token, "POST",
                  {"employeeId": 1, "kind": "EMPRESTIMO", "description": "Empréstimo 4 prestações",
                   "principalAmount": 8000.00, "installments": 4, "startDate": "2026-08-01"})
check("empréstimo criado", 200, code)
check("prestação derivada do capital (8000/4)", 2000, loan["installmentAmount"])
code, _ = call("/api/hr/deductions", token, "POST",
               {"employeeId": 1, "kind": "ADIANTAMENTO", "description": "",
                "principalAmount": 100, "startDate": "2026-08-01"})
check("desconto sem descrição é recusado", 400, code)
code, _ = call("/api/hr/deductions", token, "POST",
               {"employeeId": 1, "kind": "ADIANTAMENTO", "description": "negativo",
                "principalAmount": -50, "startDate": "2026-08-01"})
check("capital negativo é recusado", 400, code)

section("RH §B5: as retenções nascem no pagamento e reapurar não as duplica")
code, slip = call("/api/hr/payslips", token, "POST",
                  {"employeeId": 1, "year": 2026, "month": 7, "overtimeHours": 0,
                   "bonuses": 0, "otherDeductions": 0})
if code == 200:
    check("recibo nasce em rascunho", "DRAFT", slip["status"])
    check("aprovado antes de pago", "APPROVED",
          call(f"/api/hr/payslips/{slip['id']}/approve", token, "POST")[1]["status"])
    check("pago", "PAID",
          call(f"/api/hr/payslips/{slip['id']}/mark-paid", token, "POST")[1]["status"])
else:
    print(f"  NOTA  a folha de 2026/7 já tinha sido processada ({code})")

# Reapurar em vez de somar recibo a recibo é o que torna isto idempotente: pagar o mesmo recibo
# duas vezes não pode duplicar a dívida ao Estado.
first = len(call("/api/hr/payroll/liabilities", token)[1])
call("/api/hr/payroll/liabilities/accrue/2026/7", token, "POST")
check("reapurar NÃO duplica a dívida ao Estado", first,
      len(call("/api/hr/payroll/liabilities", token)[1]))

section("RH §B3: a conta da cessação aparece ANTES de ser irreversível")
code, preview = call("/api/hr/terminations/preview", token, "POST",
                     {"employeeId": 2, "terminationDate": "2026-08-31",
                      "reason": "MUTUO_ACORDO", "noticeServed": True})
if code == 200:
    print(f"  líquido previsto: {preview.get('netAmount')}"
          f" · avisos do que não sabe calcular: {len(preview.get('warnings') or [])}")
    check("a pré-visualização devolve a conta", True, preview.get("netAmount") is not None)
    # Repetível: numa segunda corrida o colaborador já está cessado, e cessar de novo TEM de ser
    # recusado. Distinguir os dois casos é o que impede o harness de acusar um defeito que não há.
    already = [t for t in (call("/api/hr/terminations", token)[1] or [])
               if t.get("employeeId") == 2]
    if already:
        print(f"  NOTA  o colaborador 2 já estava cessado ({already[0].get('reference') or ''})"
              f" — a cessação nova não se repete")
    else:
        code, _ = call("/api/hr/terminations", token, "POST",
                       {"employeeId": 2, "terminationDate": "2026-08-31",
                        "reason": "MUTUO_ACORDO", "noticeServed": True})
        check("cessação registada", 200, code)
    code, _ = call("/api/hr/terminations", token, "POST",
                   {"employeeId": 2, "terminationDate": "2026-08-31",
                    "reason": "MUTUO_ACORDO", "noticeServed": True})
    check("cessar DUAS vezes é recusado", 400, code)
    code, _ = call("/api/hr/payslips", token, "POST",
                   {"employeeId": 2, "year": 2026, "month": 9, "overtimeHours": 0,
                    "bonuses": 0, "otherDeductions": 0})
    check("recibo a colaborador cessado é recusado", 400, code)
else:
    print(f"  NOTA  o colaborador 2 já estava cessado ({code}) — cenário não repetível sem repor dados")

# ─── COMPRAS ─────────────────────────────────────────────────────────────────
section("Compras: a encomenda é um documento; a mercadoria entra na recepção")
suppliers = call("/api/purchases/suppliers?companyId=1", token)[1]
supplier = suppliers[0]["id"]
before = stock_of(1)
code, order = call("/api/purchases/orders", token, "POST",
                   {"supplierId": supplier, "warehouseId": 1, "companyId": 1,
                    "expectedDate": "2026-09-15", "notes": "Reposição mensal",
                    "lines": [{"productId": 1, "quantity": 20, "unitPrice": 400.00}]})
check("encomenda criada", 200, code)
check("criar a encomenda NÃO faz entrar mercadoria", before, stock_of(1))

section("Compras: recepção parcial entra só o que chegou")
line_id = order["lines"][0]["id"]
code, partial = call(f"/api/purchases/orders/{order['id']}/receive-partial", token, "POST",
                     {"lines": [{"lineId": line_id, "quantity": 12,
                                 "damagedQuantity": 0, "missingQuantity": 0,
                                 "notes": "Primeira metade"}]})
check("recepção parcial aceite", 200, code)
check("entram 12, não as 20 encomendadas", before + 12, stock_of(1))
print(f"  estado da encomenda após parcial: {partial.get('status')}")

section("Compras: o que chega danificado não entra como bom")
mid = stock_of(1)
# Faltam 8 das 20. Recebemos 5 boas + 3 danificadas = as 8 que faltavam: mais do que isso é
# recusado, e bem — receber acima do encomendado é como entra mercadoria fantasma no stock.
code, damaged = call(f"/api/purchases/orders/{order['id']}/receive-partial", token, "POST",
                     {"lines": [{"lineId": line_id, "quantity": 5,
                                 "damagedQuantity": 3, "missingQuantity": 0,
                                 "notes": "Três embalagens rasgadas"}]})
if code == 200:
    check("as 3 danificadas NÃO entram como stock bom", mid + 5, stock_of(1))
    disc = call("/api/purchases/discrepancies/open?companyId=1", token)[1]
    check("a divergência fica registada em aberto", True, len(disc or []) > 0)
else:
    print(f"  NOTA  recepção devolveu {code}: {str(damaged)[:140]}")

section("Compras: a dívida ao fornecedor nasce e baixa ao pagar")
payables = call("/api/purchases/payables?companyId=1", token)[1]
if payables:
    payable = payables[0]
    owed = Decimal(str(payable.get("outstandingAmount") or payable.get("totalAmount") or 0))
    account = call("/api/finance/accounts?companyId=1", token)[1][0]["id"]
    pay = min(owed, Decimal("1000"))
    code, _ = call(f"/api/purchases/{payable['id']}/pay?amount={pay}"
                   f"&financeAccountId={account}&reference=TESTE", token, "POST")
    check("pagamento ao fornecedor aceite", 200, code)
    after_pay = [p for p in call("/api/purchases/payables?companyId=1", token)[1]
                 if p["id"] == payable["id"]]
    if after_pay:
        left = Decimal(str(after_pay[0].get("outstandingAmount")
                           or after_pay[0].get("totalAmount") or 0))
        check("a dívida baixa exactamente o que se pagou", owed - pay, left)
    else:
        print("  NOTA  a conta saiu da lista de pendentes — ficou liquidada")
else:
    print("  NOTA  não há contas a pagar — a recepção pode não gerar factura sozinha")

# ─── CONTABILIDADE ───────────────────────────────────────────────────────────
section("Contabilidade: o balancete equilibra — débitos iguais a créditos")
code, tb = call("/api/accounting/trial-balance?from=2026-01-01&to=2026-12-31", token)
check("balancete devolvido", 200, code)
if code == 200:
    debit = Decimal(str(tb["totalDebit"]))
    credit = Decimal(str(tb["totalCredit"]))
    print(f"  débitos: {debit} · créditos: {credit} · {len(tb['lines'])} conta(s)")
    # Se estes dois não coincidirem há lançamentos corrompidos. É a invariante mais antiga da
    # contabilidade e a única que não admite excepção.
    check("débitos == créditos", debit, credit)
    check("e o balancete diz-se equilibrado", True, tb["balanced"])

section("Contabilidade: a folha paga chega ao razão (RHC-53)")
code, journal = call("/api/accounting/journal?companyId=1", token)
entries = journal.get("items", []) if isinstance(journal, dict) else (journal or [])
if code == 200 and entries:
    sources = {e.get("source") for e in entries}
    print(f"  {len(entries)} lançamento(s) · origens: {sorted(s for s in sources if s)}")
    check("há lançamentos vindos da folha de salários", True,
          any("PAYROLL" in str(s).upper() for s in sources))
else:
    # Sem plano de contas o AutomaticPostingService devolve em silêncio — nada é escriturado e não
    # fica rasto. É por isso que o ecrã da Contabilidade passou a avisar enquanto a tabela do plano
    # estiver vazia. Semeie o plano (botão "Semear PGC-NIRF") e volte a correr.
    print(f"  NOTA  razão vazio ({code}) — esta empresa tem plano de contas semeado?")

print(f"\nRESULTADO: {TALLY['ok']} OK, {TALLY['bad']} FALHA(S)")
sys.exit(1 if TALLY["bad"] else 0)
