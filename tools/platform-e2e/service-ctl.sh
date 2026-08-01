#!/usr/bin/env bash
# service-ctl.sh — governo del ciclo di vita dei servizi DELLA SUITE (UC 0090 §5, UC 0092).
#
# Perché esiste: il journey F-DEGRADE deve fermare DAVVERO un servizio (non simulare una
# risposta) e poi rimetterlo su. Serve quindi un modo per riavviare un servizio *identico* a
# come `run.sh` l'aveva avviato — stessa porta, stesse variabili d'ambiente, stesso registro.
#
# Come lo garantisce: la ricetta di avvio NON è duplicata qui. `run.sh` la scrive una volta
# in `.run/services.json`, dentro lo stesso ciclo in cui avvia i servizi, e questo script è
# l'unico esecutore — sia al primo avvio sia ai riavvii del journey. Un solo percorso di
# codice: se un giorno cambia una variabile d'ambiente, non c'è un secondo posto che resta
# indietro. Il descrittore deriva dalla scoperta automatica dei servizi (dev/lib/services.sh),
# quindi una nuova app entra nella suite senza toccare questo file.
#
# Uso (sempre col nome del MODULO, es. `core`, `auth`, `fatture`):
#   service-ctl.sh start   <svc>   # avvia e ATTENDE la readiness (idempotente: già su → nulla)
#   service-ctl.sh spawn   <svc>   # avvia senza attendere (avvio in parallelo di tutti, run.sh)
#   service-ctl.sh wait    <svc>   # attende la readiness di un servizio già avviato
#   service-ctl.sh stop    <svc>   # ferma e attende che la porta si liberi
#   service-ctl.sh restart <svc>
#   service-ctl.sh status  <svc>   # 0 = in ascolto, 1 = giù
#   service-ctl.sh stop-all        # ferma tutti i servizi del descrittore (pulizia di run.sh)
#
# Il processo avviato scrive su file (mai sullo standard output dello script): chi invoca
# questo comando in modo sincrono — l'helper TypeScript dei journey — non resta appeso al
# tubo di un processo che è progettato per non morire.
set -uo pipefail

TOOL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$TOOL_DIR/../.." && pwd)"
RUN_DIR="$TOOL_DIR/.run"
SERVICES_JSON="$RUN_DIR/services.json"

# Attesa massima (secondi) perché una porta si liberi o un servizio risponda pronto.
CTL_TIMEOUT="${CTL_TIMEOUT:-120}"

die() { printf 'service-ctl: %s\n' "$*" >&2; exit 1; }

[ -f "$SERVICES_JSON" ] || die "descrittore assente ($SERVICES_JSON): la suite non è avviata (run.sh)"

# svc_field <svc> <campo> — legge un campo scalare del descrittore.
svc_field() {
  node -e '
    const d = require(process.argv[1]);
    const s = d[process.argv[2]];
    if (!s) { console.error("servizio sconosciuto: " + process.argv[2]); process.exit(1) }
    const v = s[process.argv[3]];
    if (v === undefined) { console.error("campo assente: " + process.argv[3]); process.exit(1) }
    process.stdout.write(String(v));
  ' "$SERVICES_JSON" "$1" "$2"
}

# Una riga per servizio, terminatore incluso: senza il "\n" finale `read` scarta l'ultima riga e
# l'ultimo servizio non verrebbe mai fermato (difetto vero, trovato con un processo residuo).
svc_names() {
  node -e 'for (const k of Object.keys(require(process.argv[1]))) console.log(k)' "$SERVICES_JSON"
}

# porta_libera <porta> — 0 se NESSUNO è in ascolto (connessione rifiutata).
porta_libera() {
  ! (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

# attendi <condizione> <messaggio> [secondi] — polling a 0.25s; silenzioso se il messaggio è vuoto.
attendi() {
  local cond="$1" msg="$2" secs="${3:-$CTL_TIMEOUT}" i=0
  while [ "$i" -lt $((secs * 4)) ]; do
    if eval "$cond"; then return 0; fi
    sleep 0.25; i=$((i + 1))
  done
  [ -n "$msg" ] && printf 'service-ctl: %s (scaduto dopo %ss)\n' "$msg" "$secs" >&2
  return 1
}

# url_ready <svc> — URL di readiness: l'auth espone il JWKS, gli altri la health di Quarkus.
url_ready() {
  local port; port="$(svc_field "$1" port)" || return 1
  if [ "$(svc_field "$1" role)" = auth ]; then
    printf 'http://localhost:%s/api/auth/jwks\n' "$port"
  else
    printf 'http://localhost:%s/q/health/ready\n' "$port"
  fi
}

cmd_status() {
  local port; port="$(svc_field "$1" port)" || return 2
  porta_libera "$port" && return 1 || return 0
}

cmd_wait() {
  local svc="$1" port; port="$(svc_field "$svc" port)" || return 2
  attendi "curl -s -o /dev/null -w '%{http_code}' '$(url_ready "$svc")' | grep -q '^200$'" \
          "$svc non pronto su :$port (registro: $(svc_field "$svc" log))"
}

# cmd_spawn — avvia senza attendere la readiness: serve a run.sh, che accende tutti i
# servizi insieme e poi li attende (l'avvio sequenziale costerebbe la somma dei tempi di
# avvio invece del massimo). I journey usano `start`, che attende.
cmd_spawn() {
  local svc="$1" port log
  port="$(svc_field "$svc" port)" || return 2
  log="$(svc_field "$svc" log)" || return 2
  if ! porta_libera "$port"; then return 0; fi   # già su: idempotente

  local envs=()
  while IFS= read -r line; do [ -n "$line" ] && envs+=("$line"); done < <(
    node -e '
      const s = require(process.argv[1])[process.argv[2]];
      for (const [k, v] of Object.entries(s.env)) console.log(k + "=" + v);
    ' "$SERVICES_JSON" "$svc"
  )

  # stdin da /dev/null e uscite sul registro: il processo sopravvive allo script e non tiene
  # aperto alcun tubo verso il chiamante.
  env "${envs[@]}" QUARKUS_HTTP_PORT="$port" \
      java -Dquarkus.profile=dev -jar "$ROOT/services/$svc/target/quarkus-app/quarkus-run.jar" \
      >>"$log" 2>&1 </dev/null &
  printf '%s\n' "$!" > "$RUN_DIR/$svc.pid"
}

cmd_start() { cmd_spawn "$1" && cmd_wait "$1"; }

cmd_stop() {
  local svc="$1" port pid; port="$(svc_field "$svc" port)" || return 2
  pid="$(cat "$RUN_DIR/$svc.pid" 2>/dev/null || true)"
  # Ripiego su chi occupa la porta: il file del processo può mancare (servizio sopravvissuto a
  # un'esecuzione precedente e quindi non riavviato da questa, che lo trova già in ascolto).
  # Senza questo ripiego un residuo diventerebbe permanente.
  [ -n "$pid" ] || pid="$(lsof -ti "tcp:$port" -sTCP:LISTEN 2>/dev/null | tr '\n' ' ')"
  [ -n "$pid" ] && kill $pid 2>/dev/null
  # 15s per l'arresto ordinato, poi la maniera forte: un servizio che non muore bloccherebbe
  # il riavvio del journey e con esso l'intera batteria.
  if ! attendi "porta_libera $port" "" 15; then
    [ -n "$pid" ] && kill -9 $pid 2>/dev/null
    attendi "porta_libera $port" "impossibile liberare la porta :$port di $svc" 15 || return 1
  fi
  rm -f "$RUN_DIR/$svc.pid"
}

case "${1:-}" in
  start)   [ $# -eq 2 ] || die "uso: start <svc>";   cmd_start "$2" ;;
  spawn)   [ $# -eq 2 ] || die "uso: spawn <svc>";   cmd_spawn "$2" ;;
  wait)    [ $# -eq 2 ] || die "uso: wait <svc>";    cmd_wait "$2" ;;
  stop)    [ $# -eq 2 ] || die "uso: stop <svc>";    cmd_stop "$2" ;;
  restart) [ $# -eq 2 ] || die "uso: restart <svc>"; cmd_stop "$2" && cmd_start "$2" ;;
  status)  [ $# -eq 2 ] || die "uso: status <svc>";  cmd_status "$2" ;;
  stop-all)
    rc=0
    while IFS= read -r svc; do [ -n "$svc" ] && { cmd_stop "$svc" || rc=1; }; done < <(svc_names)
    exit "$rc" ;;
  *) die "comando sconosciuto: '${1:-}' (start | stop | restart | status | stop-all)" ;;
esac
