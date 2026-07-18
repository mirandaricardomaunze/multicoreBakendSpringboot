#!/usr/bin/env bash
# Verificação pós-deploy do backend Multicore.
# Uso:  ./scripts/deploy-smoke.sh [BASE_URL]      (default: http://localhost:8080)
# Sai 0 se tudo OK; !=0 se algo falhar. Não precisa de credenciais.
set -u
BASE="${1:-http://localhost:8080}"
fail=0

check() { # descricao  codigo_obtido  codigo_esperado
  if [ "$2" = "$3" ]; then echo "  OK    $1 ($2)"; else echo "  FALHA $1 (obtido $2, esperado $3)"; fail=1; fi
}

echo "== Smoke test -> $BASE =="

# 1) Health do Actuator = UP
body=$(curl -fsS "$BASE/actuator/health" 2>/dev/null || echo "")
if echo "$body" | grep -q '"status":"UP"'; then echo "  OK    /actuator/health UP"; else echo "  FALHA /actuator/health ($body)"; fail=1; fi

# 2) Endpoint protegido sem token -> 401 (Spring Security a recusar)
code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/platform/companies")
check "/api/platform/companies sem token" "$code" "401"

# 3) Login público responde (400 com credencial errada = handler alcançado, não 401/500)
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST -H "Content-Type: application/json" \
        -d '{"username":"_","password":"_"}' "$BASE/api/auth/login")
check "/api/auth/login público responde" "$code" "400"

if [ "$fail" = "0" ]; then echo "== SMOKE OK =="; else echo "== SMOKE FALHOU =="; fi
exit $fail
