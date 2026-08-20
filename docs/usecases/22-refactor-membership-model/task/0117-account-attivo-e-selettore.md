# Piano di lavoro — UC 0117 · Account attivo e selettore

**Storia**: [0117](../story/0117-account-attivo-e-selettore.md) · **Aree toccate**: `infra` (funzione del token), `services/auth`, `services/core`, `frontend/apps/backoffice`
**Dimensione stimata**: media-grande — poco codice, molto rischio · **Prerequisiti**: [UC 0116](0116-identita-e-appartenenze.md)

## Passo 1 — Dove vive l'account attivo

**Migrazione**: colonna `active_membership_id` (annullabile) su `platform.identity`.

**Perché non un attributo presso il fornitore di identità**: il gruppo di utenti Cognito
([auth.tf](../../../../infra/modules/platform_shared/auth.tf)) non dichiara attributi personalizzati.
Aggiungerne uno per via dichiarativa può forzare la **ricreazione del gruppo**, cioè la perdita degli utenti:
rischio inaccettabile per una comodità. La funzione che costruisce il token interroga già la banca dati, e
una colonna in più non costa nulla. Scrivere questa ragione nel commento della colonna, altrimenti qualcuno
riproverà la via «più elegante».

## Passo 2 — La funzione che scegli l'account

Il cuore della storia, e va scritta **una volta** in una funzione pura, poi usata dai due fornitori:

Ingredienti: le appartenenze attive dell'identità, il valore di `active_membership_id`.
Esiti, in quest'ordine:

| Caso | Esito |
|---|---|
| nessuna appartenenza attiva | nessun claim → i servizi rifiutano (comportamento di oggi, da conservare) |
| una sola | quella, ignorando il valore conservato |
| più di una, valore conservato **valido** | quella |
| più di una, valore conservato assente o non più valido | nessun claim + esito tipizzato «scegli l'account» |

**Il valore conservato non è creduto**: si usa solo se corrisponde a un'appartenenza **attiva** trovata
adesso. È la riga che impedisce che una manomissione di quella colonna diventi un varco fra due aziende.

**File**: la funzione va in `services/core` (unico posto in cui la logica di appartenenza vive) ed è
**replicata** in [handler.py](../../../../infra/modules/platform_shared/lambda/pre_token_gen/handler.py),
perché quella gira dentro l'infrastruttura e non può chiamare il core. Due attuazioni della stessa regola
sono un debito: renderlo **visibile** con un commento incrociato in entrambe e con collaudi che usano la
stessa tabella di casi. Alternativa scartata: far chiamare il core dalla funzione del token — aggiunge una
dipendenza di rete sul percorso di accesso, cioè un guasto in più su un percorso che deve sempre funzionare.

## Passo 3 — Il cambio di account

**Interfaccia nuova**: `POST /api/platform/v1/me/active-account` con l'identificativo dell'account.
Verifica l'appartenenza attiva, scrive, registra la traccia di controllo, risponde. **Non** restituisce un
token: il rinnovo passa dal percorso di rinnovo esistente, così la costruzione del claim resta in un solo
posto.

**Rifiuto `404`** se l'appartenenza non esiste — non `403`, per non rivelare l'esistenza dell'account.

## Passo 4 — Il selettore nell'interfaccia

**Modifiche**: [Topbar.tsx](../../../../frontend/apps/backoffice/src/shell/Topbar.tsx) e
[ShellContext.tsx](../../../../frontend/apps/backoffice/src/registry/ShellContext.tsx).

- il contesto della shell riceve l'elenco delle appartenenze e l'account attivo;
- **con una sola appartenenza il selettore non viene reso affatto** — non reso disabilitato: è il principio
  già adottato per i menu dei non-owner (UC 0107);
- il cambio: chiamata, rinnovo del token, **ricarica completa** dell'applicazione. Non tentare di aggiornare
  lo stato in memoria: mezza applicazione con l'account nuovo e mezza col vecchio è il modo peggiore di
  sbagliare;
- il **nome dell'account attivo** diventa visibile in permanenza nell'intestazione, non solo dentro il
  selettore aperto: con più account è un elemento di sicurezza percepita.

**Rilevamento delle schede vecchie**: se il token in uso porta un account diverso da quello attivo
conservato, mostrare un avviso «l'account attivo è cambiato in un'altra scheda: ricarica». Non è un varco
(vedi storia §5) ma è confusione su chi si sta guardando, e va evitata.

## Passo 5 — Parità dei fornitori

**Modifiche**: [TokenService.java](../../../../services/auth/src/main/java/app/appgrove/auth/local/TokenService.java)
e [UserDirectory.java](../../../../services/auth/src/main/java/app/appgrove/auth/local/UserDirectory.java) —
lo stesso criterio del passo 2, gli stessi claim, gli stessi esiti d'errore. La parità è già dichiarata nel
commento di `handler.py`: qui va **provata**, con la stessa tabella di casi eseguita su entrambi.

## Passo 6 — Collaudi

- **Unità, i quattro casi del passo 2**, su entrambe le attuazioni. È il collaudo più importante della
  storia.
- **Sicurezza**: `active_membership_id` scritto a mano su un'appartenenza revocata (o di un altro account)
  → nessun token con quel claim. Da provare manomettendo la colonna, perché è l'unica prova che il valore
  non è creduto.
- **Integrazione**: cambio di account → nuovo claim dopo il rinnovo; il token precedente resta valido per
  il suo account fino alla scadenza (comportamento **atteso**: va scritto in un collaudo, così nessuno lo
  scopre in seguito credendolo un difetto).
- **Interfaccia**: con una sola appartenenza il selettore non è nel documento reso (non «è nascosto»).
- **Percorsi end-to-end** `J-ACCOUNT-SWITCH`: la stessa persona entra nel proprio account (tutti i menu),
  passa all'account dell'azienda (solo le applicazioni abilitate), torna. Da registrare in
  [copertura-e2e.yaml](../../../testing/copertura-e2e.yaml).

## Verifica finale

```bash
cd services && mvn -B test
cd .. && ./run-tests.sh backend frontend infra
```

## Trappole note

1. **Credere al valore conservato** è l'unico modo di trasformare questa storia in un varco fra aziende.
   L'appartenenza si riverifica sempre.
2. **Due attuazioni della stessa regola** (Java e Python): divergeranno, se non c'è un collaudo con la
   stessa tabella di casi su entrambe. Il commento incrociato serve a chi legge, il collaudo a chi sbaglia.
3. **Aggiornare lo stato in memoria** invece di ricaricare, al cambio di account: sembra più raffinato e
   produce schermate con dati di due account mescolati.
4. **Il caso «una sola appartenenza» deve restare a costo zero**: è quello di tutti gli utenti attuali. Un
   passaggio in più lì è una regressione per il cento per cento delle persone, a beneficio di una minoranza.
5. **Toccare il gruppo di utenti** per aggiungere un attributo: rischio di ricreazione. La colonna in banca
   dati esiste per evitarlo.
