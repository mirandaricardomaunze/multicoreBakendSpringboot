#!/usr/bin/env bash
# Validação do servidor antes de receber dados reais. Corre no VPS, na pasta do projecto.
# Uso:  ./scripts/validar-producao.sh https://erp.dominio <commit-aprovado>
# Sai 0 só se tudo passar. Faz duas tentativas de login falhadas de propósito (admin e superadmin).
set -u
BASE="${1:?indique o endereço público, por exemplo https://erp.dominio}"
EXPECTED="${2:?indique o commit aprovado}"
BACKUPS="${BACKUPS:-data/backups}"
fail=0

ok()    { echo "  OK    $1"; }
falha() { echo "  FALHA $1"; fail=1; }
same()  { if [ "$2" = "$3" ]; then ok "$1"; else falha "$1 (obtido '$2', esperado '$3')"; fi; }
http()  { curl -s -o /dev/null -m 15 -w "%{http_code}" "$@"; }
envval() { grep -E "^$1=" .env | tail -1 | cut -d= -f2-; }

for tool in git docker curl ss stat find; do
  command -v "$tool" >/dev/null || falha "falta a ferramenta $tool: o resultado não é fiável"
done

echo "== Versão instalada =="
same "commit da pasta" "$(git rev-parse HEAD 2>/dev/null)" "$EXPECTED"
if [ -n "$(git status --porcelain 2>/dev/null)" ]; then falha "a pasta tem alterações locais"; else ok "pasta sem alterações locais"; fi
container=$(docker compose ps -q backend 2>/dev/null)
if [ -z "$container" ]; then
  falha "backend não está a correr"
else
  same "commit da imagem em execução" \
    "$(docker inspect --format '{{ index .Config.Labels "org.opencontainers.image.revision" }}' "$container")" "$EXPECTED"
  same "perfil activo" "$(docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$container" \
    | grep '^SPRING_PROFILES_ACTIVE=' | cut -d= -f2)" "prod"
fi
latest=$(ls backend/src/main/resources/db/migration | sed -E 's/^V([0-9]+)__.*/\1/' | sort -n | tail -1)
applied=$(docker compose exec -T db sh -c \
  'psql -tA -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select max(version::int) from flyway_schema_history where success"' 2>/dev/null | tr -d '[:space:]')
same "última migração aplicada" "$applied" "$latest"

echo "== Segredos =="
if [ ! -f .env ]; then
  falha ".env não existe"
else
  same "permissões do .env" "$(stat -c '%a' .env)" "600"
  if grep -qiE 'trocar|changeme' .env; then falha ".env ainda tem valores de exemplo"; else ok ".env sem valores de exemplo"; fi
  [ -n "$(envval ATTACHMENT_KEY)" ] && ok "ATTACHMENT_KEY definida" || falha "ATTACHMENT_KEY vazia"
  case "$(envval APP_SEED_DEMO_DATA)" in true|TRUE) falha "dados de demonstração ligados";; *) ok "dados de demonstração desligados";; esac
fi

echo "== Portas =="
if listening=$(ss -ltnH 2>/dev/null); then
  exposed=$(echo "$listening" | awk '{print $4}' | grep -vE '^(127\.|\[::1\])' | grep -E ':(5432|8080)$')
  if [ -z "$exposed" ]; then ok "PostgreSQL e backend não estão abertos ao exterior"; else falha "portas abertas: $exposed"; fi
else
  falha "não foi possível ler as portas abertas"
fi

echo "== HTTPS e API =="
case "$BASE" in https://*) ok "endereço em HTTPS";; *) falha "endereço sem HTTPS";; esac
same "HTTP redirecciona" "$(http "http://${BASE#*://}/api/version")" "308"
same "versão responde" "$(http "$BASE/api/version")" "200"
same "API recusa sem sessão" "$(http "$BASE/api/platform/companies")" "401"
same "actuator fechado ao exterior" "$(http "$BASE/actuator/health")" "404"
if curl -sI -m 15 "$BASE/api/version" | grep -qi '^strict-transport-security:'; then ok "HSTS activo"; else falha "HSTS em falta"; fi
same "instalador do desktop publicado" "$(http -I "$BASE/downloads/Multicore-instalador.exe")" "200"

echo "== Senhas de demonstração =="
for account in admin superadmin; do
  same "login $account/$account recusado" "$(http -X POST -H 'Content-Type: application/json' \
    -d "{\"username\":\"$account\",\"password\":\"$account\"}" "$BASE/api/auth/login")" "400"
done

echo "== Backups =="
if [ ! -d "$BACKUPS" ]; then
  falha "pasta $BACKUPS não existe"
else
  plain=$(find "$BACKUPS" -maxdepth 1 -type f \( -name '*.dump' -o -name '*.json' \) -mmin +120)
  if [ -z "$plain" ]; then ok "nenhum backup em claro"; else falha "backups em claro: $plain"; fi
  recent=$(find "$BACKUPS" -maxdepth 1 -type f -name '*.dump.enc' -mtime -2 | head -1)
  if [ -n "$recent" ]; then ok "backup cifrado com menos de dois dias"; else falha "sem backup cifrado recente"; fi
  case "$(stat -c '%a' "$BACKUPS")" in 7[05]0|700) ok "pasta de backups fechada a outros";; *) falha "pasta de backups aberta a outros";; esac
fi

if [ "$fail" = "0" ]; then echo "== PRODUCAO OK =="; else echo "== PRODUCAO COM FALHAS =="; fi
exit $fail
