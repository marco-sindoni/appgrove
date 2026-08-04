# 0027 — Collegamento del calendario esterno

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 06 — Sincronizzazione con i calendari esterni
**Storia**: `0027` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come operatore
> voglio collegare il mio calendario personale una volta e poterlo staccare quando voglio
> così da non dover controllare due agende e da non trovarmi appuntamenti presi mentre sono dal dentista.

**Contesto.** È la funzione che tutti i concorrenti mettono nei piani a pagamento e tolgono dal gratuito (§2.2
della descrizione): è ciò per cui si paga. Ma è anche la parte più fragile dell'applicazione, perché dipende da
due fornitori esterni con regole proprie, quote di chiamata e credenziali da custodire. Questa storia consegna il
solo **collegamento**, con il consenso e la revoca; leggere e scrivere sono le storie `0028` e `0029`, e tenerle
separate serve proprio a poter rilasciare il collegamento senza aver ancora deciso tutto il resto.

## 2. Requisiti funzionali

1. **RF-1** — L'operatore collega il proprio calendario di Google o di Microsoft dalle impostazioni della propria
   risorsa, autorizzando l'accesso.
2. **RF-2** — Il collegamento chiede **solo i permessi necessari** e li dichiara a schermo prima
   dell'autorizzazione, in parole comprensibili.
3. **RF-3** — L'operatore sceglie **quale** calendario collegare, quando ne ha più d'uno, e la direzione: solo
   lettura, solo scrittura, o entrambe.
4. **RF-4** — Il collegamento si revoca in qualsiasi momento, da parte dell'operatore e da parte
   dell'amministratore dell'account; la revoca cancella le credenziali conservate.
5. **RF-5** — Lo stato del collegamento è sempre visibile: attivo, in errore con il motivo, scaduto e da
   riautorizzare.
6. **RF-6** — Se il collegamento si rompe, l'app **continua a funzionare**: la sincronizzazione è un di più, non
   il cuore.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il collegamento appartiene a una risorsa di un `tenant_id` preso dal
  token verificato; nessun account può vedere o usare il collegamento di un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `POST|DELETE /api/prenotazioni/v1/risorse/{id}/calendario` e la rotta di ritorno dell'autorizzazione; errori in
  `problem+json` con codici stabili per «autorizzazione negata» e «collegamento scaduto»; OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V18__calendari.sql`: tabella `collegamento_calendario` con
  `tenant_id`, UUID versione 7, colonne di controllo, cancellazione logica, fornitore, identificativo del
  calendario, direzione, stato e **credenziali cifrate** — mai in chiaro, mai nei registri, mai nelle risposte
  delle interfacce.
- **RT-4 — Nessuna chiamata sincrona sul percorso caldo (§6).** Nessuna operazione dell'app deve dipendere da una
  risposta immediata del fornitore esterno: se il fornitore è lento o irraggiungibile, l'agenda funziona lo
  stesso.
- **RT-5 — Modulo frontend (§3, §5).** Impostazione dalla scheda della risorsa, con stato leggibile e un solo
  pulsante per staccare; solo token del sistema di design; tema chiaro e scuro.
- **RT-6 — Cinque lingue (§4).** Interfaccia, spiegazione dei permessi richiesti e messaggi di errore in
  `en, it, fr, es, de`.
- **RT-7 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese: l'identificativo dell'account
  esterno dell'operatore e lo stato del collegamento, con base giuridica «consenso dell'operatore»; campi
  annotati `@PersonalData`; tabella in `exportData` e `purgeData`. **I fornitori dei calendari sono fornitori
  esterni che trattano dati per nostro conto** e vanno elencati fra i fornitori e nell'informativa.
- **RT-8 — Registrazione eventi (§14).** `calendario collegato`, `calendario revocato`, `autorizzazione scaduta`
  con `tenant_id`, `app_id`, `user_id`, fornitore e correlazione — **mai le credenziali né l'identificativo
  dell'account esterno**.
- **RT-9 — Prove (§11).** Il fornitore esterno è **simulato** nelle prove: nessuna prova automatica parla con un
  servizio reale.

## 4. Criteri di accettazione

**CA-1 — Collegamento**
- **Dato** un operatore con una risorsa · **Quando** collega il proprio calendario e autorizza · **Allora** lo
  stato risulta attivo e il calendario scelto è quello indicato

**CA-2 — Permessi dichiarati**
- **Dato** il flusso di autorizzazione · **Quando** l'operatore lo avvia · **Allora** vede prima, in parole
  comprensibili, cosa l'app potrà leggere e cosa potrà scrivere

**CA-3 — Revoca**
- **Dato** un collegamento attivo · **Quando** lo si revoca · **Allora** le credenziali conservate spariscono e
  nessuna sincronizzazione successiva avviene

**CA-4 — Rottura senza danni**
- **Dato** un collegamento con autorizzazione scaduta · **Quando** si usa l'agenda · **Allora** tutto funziona,
  con un avviso non bloccante che invita a riautorizzare

**CA-5 — Segreti protetti**
- **Dato** un collegamento attivo · **Quando** si esaminano registri, risposte delle interfacce ed esportazioni
- **Allora** nessuna credenziale compare

**CA-6 — Isolamento fra account**
- **Dato** due account · **Quando** uno prova a leggere o usare il collegamento dell'altro · **Allora** la
  richiesta è rifiutata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend` e `compliance`);
- [ ] prove di **unità** sulla gestione dello stato e di **integrazione** con fornitore simulato;
- [ ] prova di **isolamento fra account** su credenziali e collegamenti;
- [ ] **prova end-to-end**: *rimando* — il fornitore è simulato e il flusso di autorizzazione non si guida in una
      prova automatica; motivo e storia proprietaria dichiarati in
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese, con i due fornitori dichiarati;
- [ ] **registro delle decisioni** compilato: permessi minimi richiesti, cifratura delle credenziali,
      degradazione senza danni;
- [ ] avvio locale invariato, con fornitore simulato;
- [ ] documentazione aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | il collegamento si appende a una risorsa |

## 7. Fuori ambito

- scrivere gli appuntamenti sul calendario: storia `0028`;
- leggere gli impegni personali: storia `0029`;
- il calendario di Apple: raggiungibile attraverso l'abbonamento in sola lettura della storia `0030`.

## 8. Punti aperti

**Custodia delle credenziali.** Le credenziali di accesso a un calendario altrui sono fra i dati più delicati che
l'applicazione conserverà. Dove vivano esattamente — cifrate nel database con una chiave gestita dalla
piattaforma, oppure in un servizio di segreti — è una decisione di piattaforma più che di questa app, e va presa
prima di scrivere la migrazione.
