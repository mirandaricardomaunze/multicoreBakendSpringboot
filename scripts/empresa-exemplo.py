"""Enche a empresa de exemplo e percorre um dia de trabalho, conferindo as contas em cada passo.

Serve para rever o sistema com dados a sério: nomes longos, valores altos e muitas linhas.

    python scripts/empresa-exemplo.py 18099

Escreve na base de dados e pode repetir-se. Usar só numa base de revisão, nunca em produção.
"""
import json
import sys
import time
import urllib.error
import urllib.request
from decimal import ROUND_HALF_UP, Decimal

BASE = f"http://127.0.0.1:{sys.argv[1] if len(sys.argv) > 1 else '18099'}"
COMPANY = 2
TALLY = {"ok": 0, "bad": 0}
SLOW = []
CENT = Decimal("0.01")

PRODUCT_NAMES = [
    "Óleo Alimentar Vegetal Refinado Girassol Premium Garrafa PET 5 Litros",
    "Arroz Agulha Longo Extra Selecionado Importado Saco de 25 Kilogramas",
    "Farinha de Milho Branca Peneirada Fina para Xima Saco 10 Kilogramas",
    "Detergente Líquido Concentrado Multiusos Aroma Limão Embalagem 20 Litros",
    "Cimento Portland Composto Resistência 42.5N Saco Reforçado 50 Kilogramas",
    "Papel A4 Branco 80g Resma de 500 Folhas Certificado Caixa com 5 Resmas",
]
CLIENT_NAMES = [
    "Sociedade Comercial e Industrial de Distribuição do Niassa e Cabo Delgado, Lda",
    "Empresa Moçambicana de Transportes, Logística e Serviços Portuários, S.A.",
    "Associação das Cooperativas Agrícolas e Pesqueiras da Zambézia e Nampula",
    "Hospital Privado Nossa Senhora da Conceição de Maputo e Matola, Lda",
]


def call(path, token=None, method="GET", body=None):
    request = urllib.request.Request(BASE + path, method=method)
    request.add_header("Content-Type", "application/json")
    if token:
        request.add_header("Authorization", "Bearer " + token)
        request.add_header("X-Company-Id", str(COMPANY))
        request.add_header("X-Client-Version", "1.0.0")
    data = json.dumps(body).encode() if body is not None else None
    started = time.time()
    try:
        with urllib.request.urlopen(request, data, timeout=60) as response:
            raw = response.read().decode("utf-8", "replace")
            result = response.status, (json.loads(raw) if raw.strip() else None)
    except urllib.error.HTTPError as error:
        raw = error.read().decode("utf-8", "replace")
        try:
            result = error.code, json.loads(raw)
        except ValueError:
            result = error.code, raw
    elapsed = time.time() - started
    if elapsed > 1:
        SLOW.append((round(elapsed, 2), method, path.split("?")[0]))
    return result


def check(description, expected, got):
    same = money(expected) == money(got) if is_number(expected) and is_number(got) else expected == got
    TALLY["ok" if same else "bad"] += 1
    print(f"  {'OK   ' if same else 'FALHA'} {description}" + ("" if same else f" (esperado {expected}, obtido {got})"))
    return same


def is_number(value):
    return isinstance(value, (int, float, Decimal)) and not isinstance(value, bool)


def money(value):
    return Decimal(str(value)).quantize(CENT, ROUND_HALF_UP)


def section(title):
    print(f"\n--- {title} ---")


def must(result, what):
    code, body = result
    if code not in (200, 201):
        raise SystemExit(f"PAROU em {what}: {code} {str(body)[:400]}")
    return body


def seed_products(token, warehouse, vat, category):
    existing = {p["sku"]: p for p in must(call(f"/api/comercial/products?companyId={COMPANY}", token), "artigos")}
    products = []
    for index in range(60):
        sku = f"EX-{index:04d}"
        product = existing.get(sku)
        if product is None:
            name = f"{PRODUCT_NAMES[index % len(PRODUCT_NAMES)]} #{index:02d}"
            price = 1250 + index * 37_500.75 if index % 10 == 9 else 85.5 + index * 42
            product = must(call("/api/comercial/products", token, "POST", {
                "sku": sku, "reference": f"REF-{index:04d}", "barcode": f"69{index:011d}", "name": name,
                "unitPrice": round(price, 2), "purchasePrice": round(price * 0.7, 2), "minStock": 10,
                "unitsPerBox": 1, "categoryId": category, "saleType": "UNIT", "stockTracked": True,
                "taxRateId": vat}), f"criar artigo {sku}")
            must(call("/api/inventory/adjustments", token, "POST", {
                "companyId": COMPANY, "productId": product["id"], "warehouseId": warehouse,
                "countedQuantity": 5000, "reason": "Stock inicial da empresa de exemplo"}), f"stock {sku}")
        products.append(product)
    return products


def seed_clients(token):
    existing = {c.get("taxId"): c for c in must(call(f"/api/comercial/clients?companyId={COMPANY}", token), "clientes")}
    clients = []
    for index in range(40):
        tax_id = f"4{index + 10000000:08d}"
        client = existing.get(tax_id) or must(call("/api/comercial/clients", token, "POST", {
            "name": f"{CLIENT_NAMES[index % len(CLIENT_NAMES)]} ({index:02d})", "taxId": tax_id,
            "email": f"contabilidade{index}@exemplo.co.mz", "address": "Avenida Julius Nyerere, 1234, Maputo",
            "paymentTermsDays": 30}), f"cliente {index}")
        clients.append(client)
    return clients


def stock(token, product_id, warehouse):
    for row in must(call(f"/api/inventory/stocks?companyId={COMPANY}", token), "stock"):
        if row["productId"] == product_id and row["warehouseId"] == warehouse:
            return Decimal(str(row["quantity"]))
    return Decimal("0")


def line(product, quantity, discount=0):
    return {"productId": product["id"], "quantity": quantity, "taxRate": 0.16, "discountPercentage": discount}


def find_invoice(token, invoice_id):
    rows = must(call(f"/api/comercial/invoices?companyId={COMPANY}", token), "faturas")
    return next(row for row in rows if row["id"] == invoice_id)


def invoice_cycle(token, client, products, warehouse, account):
    section("Fatura com desconto, recibo parcial, nota de crédito e nota de débito")
    product = products[3]
    before = stock(token, product["id"], warehouse)
    invoice = must(call("/api/comercial/invoices", token, "POST", {
        "clientId": client["id"], "companyId": COMPANY, "warehouseId": warehouse,
        "lines": [line(product, 10, 10), line(products[4], 2)]}), "fatura")
    unit = Decimal(str(product["unitPrice"]))
    net = unit * 10 * Decimal("0.9") + Decimal(str(products[4]["unitPrice"])) * 2
    check("total = líquido com IVA de 16%", money(net * Decimal("1.16")), invoice["totalAmount"])
    check("stock baixa 10", before - 10, stock(token, product["id"], warehouse))

    half = money(Decimal(str(invoice["totalAmount"])) / 2)
    must(call("/api/comercial/receipts", token, "POST", {
        "invoiceId": invoice["id"], "treasuryAccountId": account, "paymentMethod": "TRANSFER",
        "amountPaid": float(half)}), "recibo parcial")
    check("recibo parcial fica registado", half, find_invoice(token, invoice["id"])["amountPaid"])

    credit = must(call("/api/credit-notes", token, "POST", {
        "invoiceId": invoice["id"], "reason": "RETURN", "warehouseId": warehouse,
        "description": "Devolução parcial de exemplo",
        "lines": [{"invoiceLineId": invoice["lines"][0]["id"], "quantity": 2}]}), "nota de crédito")
    must(call(f"/api/credit-notes/{credit['id']}/approve", token, "POST", {}), "aprovar nota de crédito")
    check("devolução repõe 2 unidades", before - 8, stock(token, product["id"], warehouse))

    debit = must(call("/api/debit-notes", token, "POST", {
        "invoiceId": invoice["id"], "reason": "FREIGHT", "description": "Transporte de exemplo",
        "lines": [{"description": "Transporte até Nampula", "amount": 1000, "taxRate": 0.16}]}), "nota de débito")
    must(call(f"/api/debit-notes/{debit['id']}/approve", token, "POST", {}), "aprovar nota de débito")

    current = find_invoice(token, invoice["id"])
    credit_value = money(unit * 2 * Decimal("0.9") * Decimal("1.16"))
    check("ajuste das notas = débito menos crédito", money("1160") - credit_value, current["noteAdjustment"])
    payable = Decimal(str(current["totalAmount"])) + Decimal(str(current["noteAdjustment"]))
    remaining = money(payable - half)
    code, _ = call("/api/comercial/receipts", token, "POST", {
        "invoiceId": invoice["id"], "treasuryAccountId": account, "paymentMethod": "TRANSFER",
        "amountPaid": float(remaining + 1)})
    check("receber a mais é recusado", 400, code)
    must(call("/api/comercial/receipts", token, "POST", {
        "invoiceId": invoice["id"], "treasuryAccountId": account, "paymentMethod": "TRANSFER",
        "amountPaid": float(remaining)}), "recibo final")
    check("fatura fica paga", "PAID", find_invoice(token, invoice["id"])["status"])


def quotation_cycle(token, client, products, warehouse):
    section("Cotação, encomenda, guia e fatura")
    product = products[7]
    before = stock(token, product["id"], warehouse)
    quotation = must(call("/api/comercial/quotations", token, "POST", {
        "clientId": client["id"], "companyId": COMPANY, "warehouseId": warehouse, "validityDays": 30,
        "lines": [{"productId": product["id"], "quantity": 20, "discountPercentage": 5}]}), "cotação")
    check("cotação não mexe no stock", before, stock(token, product["id"], warehouse))
    must(call(f"/api/comercial/quotations/{quotation['id']}/send", token, "POST", {}), "enviar cotação")
    must(call(f"/api/comercial/quotations/{quotation['id']}/accept", token, "POST", {}), "aceitar cotação")
    order = must(call(f"/api/comercial/quotations/{quotation['id']}/convert", token, "POST", {}), "converter")
    print("  encomenda:", order.get("id"), order.get("status"))
    guide = must(call("/api/comercial/delivery-guides", token, "POST", {
        "orderId": order["id"], "responsible": "Ana Costa", "vehicle": "MMD-12-34", "notes": "Revisão"}), "guia")
    must(call(f"/api/comercial/delivery-guides/{guide['id']}/approve", token, "POST", {}), "aprovar guia")
    check("guia aprovada baixa o stock", before - 20, stock(token, product["id"], warehouse))
    invoice = must(call(f"/api/comercial/delivery-guides/{guide['id']}/bill", token, "POST", {}), "faturar guia")
    expected = Decimal(str(product["unitPrice"])) * 20 * Decimal("0.95") * Decimal("1.16")
    check("fatura respeita o desconto da cotação", money(expected), invoice["totalAmount"])
    check("faturar a guia não baixa o stock outra vez", before - 20, stock(token, product["id"], warehouse))


def pos_cycle(token, products, warehouse, account):
    section("Caixa do POS: abrir, vender e fechar")
    operator = "ana"
    active = call(f"/api/pos/sessions/active?operator={operator}&companyId={COMPANY}", token)[1]
    session = active or must(call("/api/pos/sessions/open", token, "POST", {
        "operator": operator, "openingBalance": 5000, "companyId": COMPANY}), "abrir caixa")
    total = Decimal("0")
    for index in range(20):
        code, sale = call("/api/pos/checkout", token, "POST", {
            "operator": operator, "companyId": COMPANY, "walkInName": "Cliente de Balcão", "warehouseId": warehouse,
            "treasuryAccountId": account,
            "lines": [{"productId": products[index]["id"], "quantity": 1, "discountPercentage": 0}]})
        if not check(f"venda {index + 1} aceite", 200, code):
            print("  recusada:", str(sale)[:300])
            continue
        total += money(Decimal(str(products[index]["unitPrice"])) * Decimal("1.16"))
    expected = Decimal(str(session["openingBalance"])) + total
    closed = must(call(f"/api/pos/sessions/{session['id']}/close", token, "POST", {
        "closingBalanceReal": float(expected), "depositAccountId": account, "notes": "Revisão"}), "fechar caixa")
    print("  vendido no POS:", total)
    check("saldo esperado = abertura + vendas", expected, closed["closingBalanceExpected"])
    check("caixa fecha sem diferença", 0, closed["difference"])


def accounting_cycle(token, client, products, warehouse):
    section("Contabilidade: uma venda entra equilibrada no balancete")
    check("empresa já tem plano de contas", True,
          len(must(call("/api/accounting/accounts", token), "plano de contas")) > 0)
    period = "from=2026-01-01&to=2026-12-31"
    before = must(call(f"/api/accounting/trial-balance?{period}", token), "balancete")
    invoice = must(call("/api/comercial/invoices", token, "POST", {
        "clientId": client["id"], "companyId": COMPANY, "warehouseId": warehouse,
        "lines": [line(products[5], 4)]}), "fatura contabilizada")
    after = must(call(f"/api/accounting/trial-balance?{period}", token), "balancete")
    check("débitos == créditos", after["totalDebit"], after["totalCredit"])
    check("balancete diz-se equilibrado", True, after["balanced"])
    grew = Decimal(str(after["totalDebit"])) - Decimal(str(before["totalDebit"]))
    check("a venda entra no balancete", True, grew >= Decimal(str(invoice["totalAmount"])))


def procurement_cycle(token, products, warehouse, account):
    section("Compras: a recepção cria a dívida, sem duplicar stock")
    product = products[1]
    period = "from=2026-01-01&to=2026-12-31"
    supplier = must(call(f"/api/purchases/suppliers?companyId={COMPANY}", token), "fornecedores")[0]
    stock_before = stock(token, product["id"], warehouse)
    debit_before = must(call(f"/api/accounting/trial-balance?{period}", token), "balancete")["totalDebit"]
    order = must(call("/api/purchases/orders", token, "POST", {
        "supplierId": supplier["id"], "warehouseId": warehouse, "companyId": COMPANY, "expectedDate": "2026-12-15",
        "lines": [{"productId": product["id"], "quantity": 20, "unitPrice": 100.0}]}), "encomenda")
    received = must(call(f"/api/purchases/orders/{order['id']}/receive-partial", token, "POST", {"lines": [{
        "lineId": order["lines"][0]["id"], "quantity": 15, "damagedQuantity": 1, "missingQuantity": 4}]}), "recepção")
    check("encomenda fica parcial por causa do danificado", "PARTIALLY_RECEIVED", received["status"])
    check("stock sobe só o aceite", 15, money(stock(token, product["id"], warehouse) - stock_before))
    payables = must(call(f"/api/purchases/payables?companyId={COMPANY}", token), "contas a pagar")
    payable = max(payables, key=lambda item: item["purchaseId"])
    expected = money(Decimal(str(order["totalAmount"])) * 15 / 20)
    check("dívida só do aceite, com IVA", expected, money(payable["outstanding"]))
    after = must(call(f"/api/accounting/trial-balance?{period}", token), "balancete")
    check("débitos == créditos", after["totalDebit"], after["totalCredit"])
    check("a compra entra no balancete", True,
          Decimal(str(after["totalDebit"])) - Decimal(str(debit_before)) >= expected)
    must(call(f"/api/purchases/{payable['purchaseId']}/pay?amount=1000&financeAccountId={account}&reference=R1",
              token, "POST"), "pagamento")
    left = next(item for item in must(call(f"/api/purchases/payables?companyId={COMPANY}", token), "contas a pagar")
                if item["purchaseId"] == payable["purchaseId"])
    check("pagamento reduz a dívida", expected - 1000, money(left["outstanding"]))


def volume(token, clients, products, warehouse):
    section("Volume: 200 faturas e 40 cotações")
    started = time.time()
    for index in range(200):
        must(call("/api/comercial/invoices", token, "POST", {
            "clientId": clients[index % len(clients)]["id"], "companyId": COMPANY, "warehouseId": warehouse,
            "lines": [line(products[(index + k) % len(products)], 1 + k) for k in range(3)]}), f"fatura {index}")
    print(f"  200 faturas em {time.time() - started:.1f}s")
    for index in range(40):
        must(call("/api/comercial/quotations", token, "POST", {
            "clientId": clients[index % len(clients)]["id"], "companyId": COMPANY, "warehouseId": warehouse,
            "lines": [{"productId": products[index]["id"], "quantity": 3, "discountPercentage": 0}]}), "cotação")
    for path in ("/api/comercial/invoices?companyId=2", "/api/comercial/invoices/page?companyId=2&page=0&size=50",
                 "/api/comercial/quotations?companyId=2", "/api/inventory/stocks?companyId=2",
                 "/api/comercial/invoices/outstanding?companyId=2"):
        started = time.time()
        code, _ = call(path, token)
        print(f"  {code} {time.time() - started:.2f}s {path}")


def main():
    token = must(call("/api/auth/login", None, "POST", {"username": "ana", "password": "password"}), "login")["token"]
    warehouse = must(call(f"/api/inventory/warehouses?companyId={COMPANY}", token), "armazéns")[0]["id"]
    account = must(call(f"/api/finance/accounts?companyId={COMPANY}", token), "contas")[0]["id"]
    rates = must(call("/api/fiscal/tax-rates", token), "taxas")
    vat = next(r["id"] for r in rates if r["code"] == "IVA16")
    category = must(call(f"/api/product-categories?companyId={COMPANY}", token), "categorias")[0]["id"]

    section("Dados da empresa de exemplo")
    products = seed_products(token, warehouse, vat, category)
    clients = seed_clients(token)
    print(f"  {len(products)} artigos, {len(clients)} clientes, armazém {warehouse}, conta {account}, IVA {vat}")
    invoice_cycle(token, clients[0], products, warehouse, account)
    quotation_cycle(token, clients[1], products, warehouse)
    pos_cycle(token, products, warehouse, account)
    accounting_cycle(token, clients[2], products, warehouse)
    procurement_cycle(token, products, warehouse, account)
    if "--volume" in sys.argv:
        volume(token, clients, products, warehouse)

    print(f"\nRESULTADO: {TALLY['ok']} OK, {TALLY['bad']} FALHA(S)")
    for elapsed, method, path in sorted(SLOW, reverse=True)[:8]:
        print(f"  LENTO {elapsed}s {method} {path}")
    sys.exit(1 if TALLY["bad"] else 0)


if __name__ == "__main__":
    main()
