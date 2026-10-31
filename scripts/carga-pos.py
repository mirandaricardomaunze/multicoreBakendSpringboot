"""Mede a venda POS em paralelo e confere que a conta bate no fim.

Cada posto abre a sua sessão e vende sem pausas, no pior caso: todos disputam o mesmo artigo e a
mesma série de facturas. Um posto real faz muito menos vendas por minuto do que este guião.

    python scripts/carga-pos.py 18099 5,10,20,40 30    # porta, postos por ronda, segundos

Escreve na base de dados. Usar só numa instância descartável, nunca contra produção.
"""
import http.client
import json
import statistics
import sys
import threading
import time
from decimal import Decimal

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 18099
ROUNDS = [int(n) for n in (sys.argv[2] if len(sys.argv) > 2 else "5,10,20,40").split(",")]
SECONDS = int(sys.argv[3]) if len(sys.argv) > 3 else 30
COMPANY, WAREHOUSE, PRODUCT = 1, 1, 1
OPENING_STOCK = 10_000_000
METHODS = ["CASH", "CARD", "BANK_TRANSFER", "MPESA", "EMOLA"]
MAX_P95_MS = 2000


class Client:
    def __init__(self, token=None):
        self.token = token
        self.connection = http.client.HTTPConnection("127.0.0.1", PORT, timeout=60)

    def call(self, path, method="GET", body=None):
        headers = {"Content-Type": "application/json", "X-Client-Version": "1.0.0"}
        if self.token:
            headers |= {"Authorization": "Bearer " + self.token, "X-Company-Id": str(COMPANY)}
        self.connection.request(method, path, json.dumps(body) if body is not None else None, headers)
        response = self.connection.getresponse()
        raw = response.read().decode("utf-8", "replace")
        try:
            return response.status, json.loads(raw) if raw.strip() else None
        except ValueError:
            return response.status, raw


def stock(client):
    rows = client.call(f"/api/inventory/stocks?companyId={COMPANY}")[1]
    return next(Decimal(str(r["quantity"])) for r in rows
                if r["productId"] == PRODUCT and r["warehouseId"] == WAREHOUSE)


def sale_body(operator, account, price, sequence):
    method = METHODS[sequence % len(METHODS)]
    payment = {"method": method, "amount": price}
    if method != "CASH":
        payment |= {"treasuryAccountId": account, "reference": f"{operator}-{sequence}"}
    return {"operator": operator, "companyId": COMPANY, "walkInName": "Cliente Carga",
            "warehouseId": WAREHOUSE, "payments": [payment],
            "lines": [{"productId": PRODUCT, "quantity": 1, "discountPercentage": 0}]}


def seller(token, operator, account, price, deadline, results, lock):
    client = Client(token)
    latencies, errors, sequence = [], {}, 0
    while time.time() < deadline:
        started = time.perf_counter()
        try:
            status, body = client.call("/api/pos/checkout", "POST", sale_body(operator, account, price, sequence))
        except (OSError, http.client.HTTPException) as error:
            status, body = 0, str(error)
            client = Client(token)
        latencies.append((time.perf_counter() - started) * 1000)
        sequence += 1
        if status != 200:
            key = f"{status}: {str(body)[:140]}"
            errors[key] = errors.get(key, 0) + 1
    with lock:
        results["latencies"] += latencies
        results["ok"] += len(latencies) - sum(errors.values())
        for key, count in errors.items():
            results["errors"][key] = results["errors"].get(key, 0) + count


def percentile(values, share):
    return sorted(values)[min(len(values) - 1, int(len(values) * share))]


def main():
    admin = Client()
    status, login = admin.call("/api/auth/login", "POST", {"username": "ana", "password": "password"})
    if status != 200:
        sys.exit(f"Login falhou: {login}")
    token = login["token"]
    admin = Client(token)

    code, body = admin.call("/api/inventory/adjustments", "POST",
                            {"companyId": COMPANY, "productId": PRODUCT, "warehouseId": WAREHOUSE,
                             "countedQuantity": OPENING_STOCK, "reason": "Preparar teste de carga"})
    if code != 200:
        sys.exit(f"Ajuste falhou: {body}")
    account = admin.call(f"/api/finance/accounts?companyId={COMPANY}")[1][0]["id"]
    products = admin.call(f"/api/comercial/products?companyId={COMPANY}")[1]
    product = next(p for p in products if p["id"] == PRODUCT)
    price = round(float(product["unitPrice"]) * (1 + float(product.get("taxRate") or 0)), 2)

    before = stock(admin)
    total_ok, healthy = 0, True
    print("postos  vendas  erros  vendas/s  p50 ms  p95 ms  p99 ms  máx ms")
    for round_number, posts in enumerate(ROUNDS):
        operators = [f"carga{round_number}x{i}" for i in range(posts)]
        for operator in operators:
            code, body = admin.call("/api/pos/sessions/open", "POST",
                                    {"operator": operator, "openingBalance": 0, "companyId": COMPANY})
            if code != 200:
                sys.exit(f"Sessão de {operator} falhou: {body}")
        results = {"latencies": [], "ok": 0, "errors": {}}
        lock = threading.Lock()
        deadline = time.time() + SECONDS
        threads = [threading.Thread(target=seller, args=(token, o, account, price, deadline, results, lock))
                   for o in operators]
        for thread in threads:
            thread.start()
        for thread in threads:
            thread.join()
        values = results["latencies"]
        failures = sum(results["errors"].values())
        p95 = percentile(values, 0.95)
        print(f"{posts:>6}  {results['ok']:>6}  {failures:>5}  {results['ok'] / SECONDS:>8.1f}  "
              f"{statistics.median(values):>6.0f}  {p95:>6.0f}  {percentile(values, 0.99):>6.0f}  {max(values):>6.0f}")
        for message, count in sorted(results["errors"].items(), key=lambda item: -item[1])[:4]:
            print(f"        {count}x {message}")
        total_ok += results["ok"]
        healthy = healthy and failures == 0 and p95 <= MAX_P95_MS

    after = stock(admin)
    print(f"\nStock: {before} -> {after}; vendas aceites: {total_ok}; diferença: {before - after}")
    if before - after != total_ok:
        healthy = False
        print("FALHA: o stock não bate com as vendas aceites")
    print("RESULTADO:", "OK" if healthy else "FALHOU")
    sys.exit(0 if healthy else 1)


if __name__ == "__main__":
    main()
