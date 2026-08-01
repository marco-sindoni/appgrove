# Come verificare a mano la change 0071 (disabilita applicazione — leva admin reversibile)

Questa change completa la **leva reversibile** che mette in pausa un'app per tutti gli account: motivazione
dell'azione, registro consultabile delle disabilitazioni, idempotenza, copy che distingue la pausa dalla
dismissione definitiva, e — lato utente — l'avviso sulla pagina degli abbonamenti quando l'app è sospesa.

Cosa serve: `./app-start.sh` (stack locale completo). Tempo indicativo: **25–35 minuti**.

Utenti del seed (password `Password1!`):

- **`admin@appgrove.test`** → console di amministrazione, <https://admin.local.appgrove.app>
- **`owner@acme.test`** → backoffice, <https://app.local.appgrove.app>

Aprire i due ruoli in **due finestre separate** (o una in incognito): buona parte della verifica consiste nel
guardare cosa succede all'utente mentre l'amministratore agisce.

> **Nota sulla lingua.** L'interfaccia parte nella lingua del browser. I testi qui sotto sono citati in italiano;
> se l'interfaccia è in inglese, cambiare lingua dal selettore in alto a destra oppure leggere l'equivalente
> ("Disabilitata" = "Disabled", "Registro disabilitazioni" = "Disable register", "Sospesa" = "Suspended").

---

## Blocco A — La sezione App della console di amministrazione

| # | Azione | Risultato atteso |
|---|---|---|
| A1 | Entrare come `admin@appgrove.test` su <https://admin.local.appgrove.app> e aprire **App** dal menu laterale | Tabella delle app del catalogo. La colonna **Stato** mostra ora un'etichetta **tradotta** — "Attiva" / "Disabilitata" — e non più il valore grezzo del database (`active`/`inactive`). Nello stack locale `Fatture` è **Attiva** e `Mini-CRM` è **Disabilitata** (scelta di prodotto, change 0042). |
| A2 | Guardare **sotto** la tabella | C'è una nuova sezione **"Registro disabilitazioni"** con il sottotitolo "Ogni disabilitazione e riabilitazione lascia una traccia: chi, quando, quale app e perché". Al primo avvio è vuota e lo dice: **"Nessuna disabilitazione registrata."** (non il generico "Ancora niente qui"). |
| A3 | Sulla riga **Fatture** premere **Disabilita** | Si apre il dialogo di conferma. Deve contenere **tutte e quattro** le informazioni: (1) "l'app diventa indisponibile a tutti gli account"; (2) "l'operazione è reversibile e i dati restano intatti: niente viene cancellato e gli abbonamenti restano validi"; (3) "l'effetto pieno si vede alla successiva lettura dei diritti di accesso…"; (4) **"Non è la dismissione definitiva dell'app — quella rimuove servizio, dati e infrastruttura, è un'altra operazione ed è irreversibile."** |
| A4 | Nello stesso dialogo, guardare il campo in fondo | C'è **"Motivazione (facoltativa)"**, un'area di testo con segnaposto "Es. manutenzione straordinaria" e sotto l'avvertenza "Finisce nel registro qui sotto. Non scrivere dati personali." |
| A5 | Premere **Annulla** (o il tasto Esc) | Il dialogo si chiude, **niente** cambia: `Fatture` resta Attiva e il registro resta vuoto. |
| A6 | Riaprire il dialogo, scrivere `manutenzione straordinaria del 1 agosto` e premere **Disabilita** | Il badge della riga `Fatture` diventa **Disabilitata** (rosso). Nel registro compare **una** riga: app `Fatture`, azione **Disabilitata**, operatore (identificativo opaco del token dell'amministratore), data e ora locali, motivazione `manutenzione straordinaria del 1 agosto`. |
| A7 | Guardare la scheda **Panoramica** (Overview) | Il contatore delle app disabilitate è salito di uno. |
| A8 | Tornare su **App**, premere **Riabilita** su `Fatture`, **senza** scrivere motivazione, e confermare | Il badge torna **Attiva**. Nel registro compare una **seconda** riga in **cima** (le più recenti prima): azione **Riabilitata**, stessa app, motivazione `—`. Il dialogo di riabilitazione **non** mostra l'avvertenza sulla dismissione definitiva (non serve: si sta riaccendendo). |
| A9 | Ricaricare la pagina | Il registro è ancora lì con le due righe: è persistito sul database, non è uno stato di sessione. |

---

## Blocco B — Cosa vede l'utente finale

Serve un abbonamento vivo su un'app che poi verrà sospesa. Il percorso più breve usa il **Mini-CRM**, spento di
default in locale.

| # | Azione | Risultato atteso |
|---|---|---|
| B1 | Console amministrazione → **App** → **Riabilita** su `Mini-CRM` (motivazione: `apertura per collaudo`) | Badge `Mini-CRM` → **Attiva**; terza riga nel registro. |
| B2 | Nell'altra finestra, entrare come `owner@acme.test` nel backoffice e andare su **Billing** | La card **Mini-CRM** è acquistabile. |
| B3 | Acquistare il piano a pagamento del Mini-CRM (pagamento finto locale) | Compare "Attivazione in corso…" e poi la conferma; nella barra laterale, sotto "Le tue app", compare **Mini-CRM**. |
| B4 | Nel pannello **"I tuoi abbonamenti"** guardare la card Mini-CRM | Un solo badge di fase (Attivo/In prova). **Nessun** avviso di sospensione. |
| B5 | Tornare nella finestra dell'amministratore → **App** → **Disabilita** `Mini-CRM`, motivazione `incidente di sicurezza` | Badge → Disabilitata; quarta riga nel registro con quella motivazione. |
| B6 | Tornare alla finestra dell'utente e **ricaricare** la pagina Billing | ① Nella barra laterale **Mini-CRM è sparito** dalle app dell'utente. ② Nel pannello "I tuoi abbonamenti" la card Mini-CRM c'è **ancora** (per disegno: mostra anche gli abbonamenti senza accesso) ma ora porta il badge **"Sospesa"** accanto alla fase, e dentro la card un riquadro giallo: **"App sospesa dalla piattaforma"** + "Questa app è temporaneamente indisponibile per tutti gli account. Il tuo abbonamento resta valido e i tuoi dati sono intatti: tornerà accessibile appena la sospensione sarà rimossa." — **questo è il punto della change**: prima la pagina diceva solo "Attivo" e contraddiceva la barra laterale. |
| B7 | Provare a raggiungere il modulo a mano, digitando l'indirizzo della sezione Mini-CRM | L'accesso è negato (guardia di rotta): non è l'authorizer di frontiera a bloccare, è l'applicazione della regola dentro il servizio — comportamento voluto e documentato nello use case. |
| B8 | Come amministratore, **Riabilita** `Mini-CRM`; poi ricaricare la finestra dell'utente | Mini-CRM torna nella barra laterale, l'avviso e il badge "Sospesa" spariscono, e **i dati di prima ci sono ancora** (aprire il modulo e verificare i contatti creati, se ne erano stati creati). |
| B9 | *(pulizia, facoltativa)* Rimettere `Mini-CRM` a Disabilitata come in partenza | Registro con una riga in più; lo stack riallinea comunque il catalogo al listino al prossimo riavvio. |

---

## Blocco C — Controlli non visivi

### C1. Righe di database

Aprire una console SQL sul Postgres di sviluppo:

```bash
docker exec -i "$(docker ps --format '{{.Names}}' | grep -m1 postgres)" \
  psql -U appgrove -d appgrove -c \
  "select app.slug, a.from_status, a.to_status, a.actor, a.reason, a.executed_at
     from platform.app_status_audit a join platform.app app on app.id = a.app_id
    order by a.executed_at desc"
```

Atteso: **una riga per ogni transizione fatta nei blocchi A e B**, nell'ordine inverso di esecuzione, con
`from_status`/`to_status` coerenti, l'identificativo dell'operatore e la motivazione dove è stata scritta
(`NULL` dove non lo è stata). **Nessuna** riga per il dialogo annullato di A5.

Verificare anche che lo stato dell'app sia davvero cambiato e chi l'ha toccata:

```bash
docker exec -i "$(docker ps --format '{{.Names}}' | grep -m1 postgres)" \
  psql -U appgrove -d appgrove -c "select slug, status, updated_by from platform.app order by slug"
```

E, soprattutto, che **nessun dato sia stato toccato**: i contatti del Mini-CRM e le fatture create prima della
disabilitazione devono essere ancora tutti lì (`select count(*) from app_crm.contact;`).

### C2. Chiamate all'interfaccia di programmazione

Serve un token di amministratore: il modo più semplice è copiarlo dagli strumenti di sviluppo del browser
(intestazione `Authorization` di una qualsiasi chiamata `/api/platform/v1/admin/...` fatta dalla console).

```bash
TOKEN='<incolla qui il token>'
APP=$(curl -sk -H "Authorization: Bearer $TOKEN" \
  https://api.local.appgrove.app/api/platform/v1/admin/apps | python3 -c \
  "import sys,json;print([a['id'] for a in json.load(sys.stdin) if a['slug']=='fatture'][0])")
```

| # | Chiamata | Risultato atteso |
|---|---|---|
| C2a | `curl -ski -X PATCH -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"status":"inactive","reason":"prova"}' https://api.local.appgrove.app/api/platform/v1/admin/apps/$APP` | **200**, corpo con `"status":"inactive"`. Nel registro (query C1) **una** riga in più. |
| C2b | **Ripetere identica** la chiamata C2a | **200** di nuovo (nessun errore: l'operazione è idempotente) e nel registro **nessuna riga in più** — è il punto delicato: un'azione a vuoto non deve lasciare una prova falsa. |
| C2c | Stessa chiamata con `-d '{"status":"dismessa"}'` | **400** con corpo `application/problem+json` (`"status":400`, un `title` leggibile). Lo stato dell'app **non** cambia. |
| C2d | Stessa chiamata con un identificativo inventato al posto di `$APP` | **404** *problem+json*. |
| C2e | Motivazione lunghissima: `-d "{\"status\":\"active\",\"reason\":\"$(python3 -c 'print("x"*600)')\"}"` | **400** (validazione della lunghezza massima, 512). |
| C2f | Rifare C2a **col token di `owner@acme.test`** (preso dal backoffice) | **403**. Poi `curl -ski -H "Authorization: Bearer $TOKEN_OWNER" https://api.local.appgrove.app/api/platform/v1/admin/apps/audit` → **403**: un ruolo di account non può nemmeno leggere il registro. Lo stato dell'app resta invariato. |

Al termine riportare `fatture` allo stato `active` (C2a con `"status":"active"`, oppure dalla console).

### C3. Log del servizio

```bash
grep 'admin.app.status-changed' dev/.run/core.log | tail -5
```

Attese **due** righe per ogni transizione:

- una del servizio: `admin.app.status-changed app_id=… from=active to=inactive actor=…`;
- una del canale di audit (`app.audit`): `admin.app.status-changed outcome=SUCCESS actor=… app_id=… from_status=… status=…`.

**Controllo importante**: nella riga di audit la **motivazione non deve comparire**. È voluto — il testo libero
resta nel database e non finisce nell'archivio a 12 mesi, che ammette soli identificativi opachi.

### C4. Posta (Mailpit, <http://localhost:8025>)

Atteso: **nessuna email**. Disabilitare un'app **non** avvisa gli utenti impattati: è una scelta di prodotto
esplicitamente rimandata (punti aperti dello use case 0076). Se dovesse arrivare un'email, è un difetto.

---

## Blocco D — Distinguere la pausa dalla dismissione definitiva

| # | Azione | Risultato atteso |
|---|---|---|
| D1 | Rileggere il dialogo di conferma della disabilitazione | Deve essere **impossibile** confonderlo con la dismissione definitiva: lo dice a parole, e l'unica cosa che l'azione può fare è cambiare uno stato. |
| D2 | Dopo una disabilitazione, controllare che infrastruttura e codice siano intatti: il servizio dell'app è ancora in esecuzione (`./dev.sh services`), lo schema di database dell'app esiste ancora, il modulo frontend è ancora compilato | Nulla è stato rimosso. La disabilitazione tocca **una sola colonna**: `platform.app.status`. |
