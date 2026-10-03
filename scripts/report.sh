#!/usr/bin/env bash
# Envía los resultados de la ejecución al panel de pipelines de QA Playground.
# Funciona igual en GitHub Actions, Jenkins, Azure Pipelines o en tu máquina (detecta el motor solo).
#
#   export QALAB_REPORT_TOKEN=qlp_...        # el token que te dio el panel al crear el proyecto
#   export QALAB_USER=descuadre              # (opcional) usuario de prueba con el que corriste
#   ./scripts/report.sh
set -uo pipefail

URL="${QALAB_REPORT_URL:-https://qa.morewater.dev/api/pipelines/ingest}"
TOKEN="${QALAB_REPORT_TOKEN:-}"
if [ -z "$TOKEN" ]; then echo "report.sh: QALAB_REPORT_TOKEN no está definido; no se envían resultados."; exit 0; fi

shopt -s nullglob
xmls=(target/surefire-reports/TEST-*.xml)
if [ ${#xmls[@]} -eq 0 ]; then echo "report.sh: no hay reportes en target/surefire-reports/ (¿corrió mvn test?)"; exit 0; fi

# --- detectar el motor de CI ---
if [ -n "${GITHUB_ACTIONS:-}" ]; then
  engine=github; branch="${GITHUB_REF_NAME:-}"; commit="${GITHUB_SHA:-}"; actor="${GITHUB_ACTOR:-}"; trigger="${GITHUB_EVENT_NAME:-}"
  ext="${GITHUB_RUN_ID:-}"; build_url="${GITHUB_SERVER_URL:-}/${GITHUB_REPOSITORY:-}/actions/runs/${GITHUB_RUN_ID:-}"
elif [ -n "${JENKINS_URL:-}" ]; then
  engine=jenkins; branch="${BRANCH_NAME:-${GIT_BRANCH:-}}"; commit="${GIT_COMMIT:-}"; actor="${BUILD_USER:-jenkins}"; trigger="${BUILD_CAUSE:-build}"
  ext="${JOB_NAME:-job}-${BUILD_NUMBER:-0}"; build_url="${BUILD_URL:-}"
elif [ -n "${TF_BUILD:-}" ]; then
  engine=azure; branch="${BUILD_SOURCEBRANCHNAME:-}"; commit="${BUILD_SOURCEVERSION:-}"; actor="${BUILD_REQUESTEDFOR:-}"; trigger="${BUILD_REASON:-}"
  ext="${BUILD_BUILDID:-}"; build_url="${SYSTEM_COLLECTIONURI:-}${SYSTEM_TEAMPROJECT:-}/_build/results?buildId=${BUILD_BUILDID:-}"
else
  engine=local; branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo)"; commit="$(git rev-parse HEAD 2>/dev/null || echo)"
  actor="${USER:-local}"; trigger=manual; ext=""; build_url=""
fi
target="${QALAB_USER:-estandar}"
env_json="{\"browser\":\"chrome\",\"qalab_url\":\"${QALAB_URL:-https://qalab.morewater.dev}\",\"language\":\"java\",\"selenium\":\"4.27\"}"

args=(-sS -w '\n%{http_code}' -X POST "$URL" -H "Authorization: Bearer $TOKEN"
  -F "engine=$engine" -F "branch=$branch" -F "commit=$commit" -F "actor=$actor" -F "trigger=$trigger"
  -F "target=$target" -F "external_id=$ext" -F "build_url=$build_url" -F "env=$env_json")
for f in "${xmls[@]}"; do args+=(-F "junit=@$f;type=application/xml"); done
n=0
for f in target/screenshots/*.png; do
  [ "$n" -ge 20 ] && break
  size=$(wc -c < "$f"); [ "$size" -gt 2000000 ] && continue
  args+=(-F "files=@$f;type=image/png"); n=$((n+1))
done

resp="$(curl "${args[@]}")"; code="${resp##*$'\n'}"; body="${resp%$'\n'*}"
if [ "$code" = "201" ]; then echo "report.sh: resultados enviados ($engine, usuario $target): $body"
else echo "report.sh: el panel respondió $code: $body" >&2; fi
exit 0   # reportar nunca debe romper tu pipeline
