# 0088 — Log di implementazione · UC 0116 Identità della persona e appartenenze agli account

**Modalità**: fast (gate di workflow rinunciati all'invocazione; suite completa verde prima del commit)
**Branch**: `change/0088-use-case-0116-identita-e-appartenenze`
**Registro strutturato delle scelte**: [decisions.json](decisions.json) — 26 voci
**Verifica manuale**: [how-to-test.md](how-to-test.md)

## Cosa è stato fatto

Il vincolo «una persona appartiene a un solo account» non era una convenzione: erano **due indici unici
globali** (indirizzo di posta, identificativo di autenticazione) su `platform.users`, una tabella che sta
**dentro** l'account. È quel disallineamento a produrre il vincolo di troppo. Ora:

- **`platform.identity`** — l'identità di accesso della persona. Entità di **piattaforma**: nessun
  `tenant_id`, come `platform.app`. Qui vive l'unicità globale.
- **`platform.membership`** — l'appartenenza (account, identità) con ruolo e stato. Entità di **account**.
  Unicità su `(tenant_id, identity_id)` **limitata alle righe vive**: il vincolo che serve davvero — «non
  due volte nello stesso account» — è ora esplicito invece di essere l'effetto collaterale di uno più forte.

Lo **stato si sdoppia** e non si sposta: `identity.status` dice se la persona accede alla piattaforma (leva
del titolare: limitazione del trattamento, art. 18); `membership.status` se si presenta come persona di
**quell'** account (leva dell'owner). Chi emette il token pretende **entrambi** attivi.

`platform.users` **resta in piedi** come rete di ritorno, ma è **fredda**: nessuno la legge, nessuno la
scrive, e nemmeno il seme la popola. La rimozione fisica è di una migrazione successiva
([_BACKLOG.md](../../docs/_BACKLOG.md)).

## Il travaso, e come è provato

`V17__identity_membership.sql` crea le due tabelle, travasa ogni riga viva di `platform.users` (identità
**con lo stesso identificativo** — così `invitations.invited_by`, `accepted_user_id`, il bersaglio di una
limitazione e ogni altro riferimento memorizzato continuano a risolvere — più una appartenenza), rimuove i
due indici unici globali e **verifica i conteggi dentro la migrazione**, facendola fallire se non tornano.

`IdentityMigrationTest` riesegue il **SQL vero** del file su uno schema-sonda usa-e-getta: conteggi a
confronto riga per riga, e la guardia che fallisce a voce alta quando qualcuno si perde. Verifica anche gli
indici: i due globali su `platform.users` non ci sono più, quelli sull'identità ci sono, e l'unicità
dell'appartenenza è parziale sulle righe vive.

## Aree toccate

| Area | Cosa |
|---|---|
| `services/core` | `Identity`, `Membership` + repository e stati; `UserResource`/`UserDtos` su appartenenza + identità (contratto invariato); console admin, esportazione/cancellazione GDPR, limitazione art. 18, avviso di inattività, recapito dei biglietti, newsletter, inviti |
| `services/auth` | `PlatformWriter` (account + identità **se manca** + appartenenza), `UserDirectory` (identità ⋈ appartenenza, stato peggiore dei due), guardie dei percorsi d'ingresso |
| `infra` | funzione che compone il token (identità ⋈ appartenenza, parità dichiarata col fornitore locale) + permessi di scrittura delle Lambda sulle tabelle nuove |
| conformità | manifesto dati sdoppiato (`identity.*` + `membership.identity_id`), registro dei trattamenti rigenerato, rilevatore dei segnali privacy eseguito |
| `dev/seed` | identità + appartenenze; `platform.users` non più popolata |
| `tools/platform-e2e` | nove percorsi adeguati alle tabelle nuove |
| documenti | `docs/02 §14` (vincolo superato, da cosa e perché), `docs/01`, `docs/05`, registro di copertura, quattordici riferimenti stantii nelle specifiche |

## Classificazione privacy

**MINORE.** Nessuna finalità nuova, nessuna base giuridica nuova, nessun responsabile esterno nuovo, nessuna
categoria particolare: gli stessi dati cambiano tabella e **titolarità del posto** — l'identità è dato di
piattaforma, l'appartenenza è dato dell'account. Nessun aumento di versione di privacy policy o termini.
Manifesto aggiornato, registro dei trattamenti rigenerato (`npm run assemble`), `@PersonalData` annotate.

## Copertura end-to-end

Nessun percorso nuovo: storia di modello dati. La voce `0116` del registro passa da `non-implementato` a
**`senza-superficie`**; i percorsi arrivano con UC 0117 (selettore) e UC 0118 (ingressi). Il verde dei
percorsi esistenti, riscritti sulle tabelle nuove, è la prova che il travaso non ha rotto nulla di visibile.

## Collaudi aggiunti

- `IdentityMigrationTest` — travaso e guardia dei conteggi, indici prima/dopo.
- `AccountUserApiTest` — due appartenenze per la stessa identità; rifiuto della seconda nello stesso account
  (dal **vincolo**, non dall'interfaccia); uscita da un account che lascia intatta l'altra; sospensione che
  non attraversa il confine; unicità dell'indirizzo sull'identità.
- `MultiTenancyTest` — la stessa identità in due account non attraversa il confine, in lettura, nell'elenco
  e nell'esportazione.
- `PlatformGdprContractTest` — cancellare l'account A non cancella l'identità di chi appartiene anche a B;
  cancellato l'ultimo account, l'identità viene cancellata. Due prove, entrambe necessarie.
- `InviteAcceptTest` — chi è già registrato riceve un rifiuto **comprensibile** (409), non una violazione
  di indice, e il rifiuto non lascia appartenenze fantasma.
- `test_handler.py` — tabelle nuove, ordine deterministico dell'appartenenza più antica, identità senza
  appartenenze a chiusura.

## Cosa è rimasto fuori (e dove è scritto)

- **Account attivo e selettore** → UC 0117, che riceve anche il ripiego «appartenenza più antica» da
  sostituire **nelle due implementazioni insieme**.
- **Percorsi d'ingresso** (invitare chi esiste già, registrare chi è già membro altrove, messaggi non
  rivelatori, stato «in attesa di accettazione», riuso di un indirizzo) → UC 0118.
- **Limitazione su una persona senza appartenenze** → UC 0034.
- **Rimozione fisica di `platform.users`** → `docs/_BACKLOG.md`.
- **Ritiro del ruolo `admin`** (due soli ruoli) → UC 0098/0113. Il piano di UC 0098 è stato corretto: `V17`
  è occupata, `app_access` può riferire direttamente `platform.identity` senza migrazione doppia.

## Esito della suite

`./run-tests.sh` **completa, senza parametri** — **tutte le otto aree verdi** (contropartita obbligatoria
della modalità fast):

```
✓ backend  ✓ frontend  ✓ infra  ✓ compliance  ✓ tooling  ✓ smoke  ✓ platform  ✓ site
✓ TUTTE le suite eseguite sono verdi.
```

Dettagli che contano: **306** collaudi del core verdi (era 306 con 3 rossi durante il lavoro, poi rientrati),
**14** percorsi end-to-end di piattaforma verdi contro lo stack reale con browser vero e posta vera — è
questa la prova più stringente che il travaso non ha rotto nulla di visibile — e i **146** collaudi
frontend invariati (il contratto esposto non è cambiato).
