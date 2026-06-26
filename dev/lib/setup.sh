# shellcheck shell=bash
# dev setup — bootstrap one-time idempotente dell'ambiente locale (UC 0009).

cmd_setup() {
  if [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    cat <<'EOF'
dev setup — bootstrap one-time, idempotente e auto-riparante.
Passi: engine (Colima) → CA mkcert → certificati → /etc/hosts → dev/.env → stack su.
Ri-eseguibile in sicurezza: non duplica, ripara solo ciò che manca.
Alcuni passi sono stub finché non atterrano i rispettivi UC (auth/seed/migrazioni/config).
EOF
    return 0
  fi

  step "1/8 Engine Docker"
  ensure_engine
  ok "engine attivo"

  step "2/8 CA locale mkcert (trust di sistema/Firefox/Java)"
  if require_cmd mkcert; then
    if mkcert -install >/dev/null 2>&1; then ok "CA mkcert installata"
    else warn "mkcert -install non completato (richiede sudo interattivo): eseguilo a mano se il browser non si fida"; fi
  else
    warn "mkcert assente → brew install mkcert nss, poi ri-esegui setup"
  fi

  step "3/8 Certificati TLS locali (dev/certs/)"
  if certs_present; then ok "certificati già presenti"
  elif require_cmd mkcert; then
    mkdir -p "$CERT_DIR"
    if gen_certs && certs_present; then ok "certificati generati per ${DOMAINS[*]}"
    else warn "generazione certificati fallita; riprova: env -u JAVA_HOME mkcert -cert-file $CERT_CRT -key-file $CERT_KEY ${DOMAINS[*]}"; fi
  else
    warn "mkcert assente: salto i certificati"
  fi

  step "4/8 Domini /etc/hosts → 127.0.0.1"
  local d miss=()
  for d in "${DOMAINS[@]}"; do host_mapped "$d" || miss+=("$d"); done
  if [ "${#miss[@]}" -eq 0 ]; then
    ok "/etc/hosts già configurato"
  elif [ -t 0 ]; then
    info "  aggiungo a /etc/hosts (richiede sudo): ${miss[*]}"
    { printf '\n# appgrove — stack dev locale (UC 0008/0009)\n'; for d in "${miss[@]}"; do printf '127.0.0.1   %s\n' "$d"; done; } | sudo tee -a /etc/hosts >/dev/null \
      && ok "/etc/hosts aggiornato" || warn "scrittura /etc/hosts non riuscita: aggiungi a mano le voci ${miss[*]}"
  else
    warn "ambiente non interattivo: aggiungi a mano a /etc/hosts:"
    for d in "${miss[@]}"; do info "  127.0.0.1   $d"; done
  fi

  step "5/8 Config locale (dev/.env)"
  ensure_env
  ok "dev/.env pronto"

  step "6/8 Chiavi JWT locali  [stub → UC 0010]"
  warn "provider auth locale non ancora implementato: chiavi JWT/JWKS e claim dal DB arriveranno con UC 0010."

  step "7/8 Avvio stack locale"
  ensure_env
  compose up -d

  step "8/8 Migrazioni + seed deterministico (UC 0011)"
  # In subshell: un eventuale errore (es. psql assente) non aborta il setup auto-riparante.
  if ( source "$DEV_DIR/lib/seed.sh"; cmd_seed ); then
    ok "schema migrato e seed caricato."
  else
    warn "migrazioni/seed non completati: rieseguibili con ./dev.sh seed (richiede psql + container engine)."
  fi
  echo
  ok "setup completato. Prossimi passi: ./dev.sh doctor per il preflight, ./dev.sh down per fermare."
}
