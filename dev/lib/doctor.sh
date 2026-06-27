# shellcheck shell=bash
# dev doctor — preflight read-only dell'ambiente locale (UC 0009).

cmd_doctor() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev doctor — preflight read-only dell'ambiente di sviluppo locale.
Verifica engine/Docker, plugin compose, mkcert + certificati, /etc/hosts, porte
e prerequisiti app. Stampa fix azionabili. NON modifica nulla. Exit ≠0 se ci sono blocchi.
EOF
    return 0
  fi

  local blocking=0

  step "Engine & Docker"
  if require_cmd docker; then ok "docker CLI ($(docker --version 2>/dev/null | awk '{gsub(/,/,"",$3); print $3}'))"
  else err "docker CLI assente → brew install docker"; blocking=1; fi
  if engine_up; then ok "Docker daemon raggiungibile"
  else err "Docker daemon non attivo → colima start  (o ./dev.sh setup)"; blocking=1; fi
  if docker compose version >/dev/null 2>&1; then ok "plugin docker compose"
  else err "plugin compose assente → brew install docker-compose"; blocking=1; fi

  # Coerenza VM Colima ↔ host: una VM x86_64 su Apple Silicon è instabile (con vmType=vz è proprio
  # impossibile → il guest non completa il boot, manca il guest agent, docker.sock non viene inoltrato).
  step "Colima VM (host arm64)"
  if [ "$(uname -m)" != "arm64" ]; then
    info "  host non arm64 → check non applicabile"
  elif ! require_cmd colima; then
    info "  colima non installato → check saltato"
  else
    local colima_yaml="$HOME/.colima/default/colima.yaml"
    if [ ! -f "$colima_yaml" ]; then
      info "  VM Colima non ancora creata (la crea 'colima start' / ./dev.sh setup)"
    else
      local vm_arch vm_type
      vm_arch="$(sed -n 's/^arch:[[:space:]]*//p' "$colima_yaml" | head -1 | tr -d '"[:space:]')"
      vm_type="$(sed -n 's/^vmType:[[:space:]]*//p' "$colima_yaml" | head -1 | tr -d '"[:space:]')"
      case "$vm_arch" in
        x86_64|amd64)
          err "VM Colima '$vm_arch' su host arm64 → instabile (vz non esegue x86_64; il guest non completa il boot)."
          info "  fix: colima delete -f && colima start --arch aarch64 --vm-type vz --cpu 4 --memory 6 --disk 60"
          blocking=1
          ;;
        aarch64|arm64|"")
          if [ "$vm_type" = "qemu" ]; then
            warn "VM Colima arm64 ma vmType=qemu → preferisci vz (più stabile/veloce):"
            info "  colima delete -f && colima start --arch aarch64 --vm-type vz"
          else
            ok "VM Colima nativa (${vm_arch:-aarch64}${vm_type:+/$vm_type})"
          fi
          ;;
        *)
          warn "VM Colima con arch non riconosciuta: '$vm_arch' (atteso aarch64 su Apple Silicon)"
          ;;
      esac
    fi
  fi

  step "TLS locale (mkcert)"
  if require_cmd mkcert; then
    ok "mkcert presente"
    local caroot; caroot="$(env -u JAVA_HOME mkcert -CAROOT 2>/dev/null || true)"
    if [ -n "$caroot" ] && [ -f "$caroot/rootCA.pem" ]; then ok "CA locale generata ($caroot)"
    else warn "CA locale non installata → mkcert -install  (o ./dev.sh setup)"; fi
    if certs_present; then ok "certificati in dev/certs/"
    else warn "certificati assenti → ./dev.sh setup  (genera dev/certs/local.appgrove.app*.pem)"; fi
  else
    err "mkcert assente → brew install mkcert nss"; blocking=1
  fi

  step "Domini /etc/hosts"
  local d miss=()
  for d in "${DOMAINS[@]}"; do host_mapped "$d" || miss+=("$d"); done
  if [ "${#miss[@]}" -eq 0 ]; then ok "tutti i domini → 127.0.0.1"
  else
    warn "mancano in /etc/hosts: ${miss[*]}"
    info "  fix: sudo sh -c 'for d in ${miss[*]}; do echo \"127.0.0.1   \$d\" >> /etc/hosts; done'"
  fi

  step "Porte stack"
  local p used=()
  for p in "${HOST_PORTS[@]}"; do port_in_use "$p" && used+=("$p"); done
  if [ "${#used[@]}" -eq 0 ]; then ok "porte libere (${HOST_PORTS[*]})"
  else info "  in uso: ${used[*]} (atteso se lo stack è già su; altrimenti libera la porta)"; fi

  step "Prerequisiti app (per Quarkus dev / Vite)"
  for p in "${APP_PREREQS[@]}"; do
    if require_cmd "$p"; then ok "$p"; else warn "$p assente (servirà per i processi-app)"; fi
  done

  echo
  if [ "$blocking" -eq 0 ]; then ok "doctor: nessun blocco. Prosegui con ./dev.sh setup (o ./dev.sh up)."; return 0
  else err "doctor: ci sono blocchi da risolvere (vedi sopra)."; return 1; fi
}
