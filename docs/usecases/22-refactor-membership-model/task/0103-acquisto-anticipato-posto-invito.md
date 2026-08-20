# Piano di lavoro — UC 0103 · Acquisto anticipato del posto all'invito

**Storia**: [0103](../story/0103-acquisto-anticipato-posto-invito.md) · **Aree toccate**: `services/core`, `frontend/`
**Dimensione stimata**: grande (tocca il pagamento) · **Prerequisiti**: UC 0100, UC 0102

## Passo 1 — La voce di catalogo di piattaforma

**File nuovo di migrazione**: aggiunge a `platform.app` una colonna `kind` (`application` · `platform`), con
valore predefinito `application`, e inserisce la riga della voce dei posti. Aggiunge inoltre a
`platform.subscription` la colonna `quantity` (intera, predefinita 1): oggi non esiste perché gli
abbonamenti delle applicazioni sono singoli.

**Le esclusioni da fare, una per una** (è il debito della scelta strutturale, e ognuna vuole il suo collaudo):

| Dove | File | Che cosa |
|---|---|---|
| Diritti d'accesso | `EntitlementReadModel.java` | la voce di piattaforma non concede accesso ad alcuna applicazione |
| Catalogo del cliente | `MeCatalogResource.java`, `CatalogReadModel.java` | non compare fra le applicazioni |
| Console di amministrazione | `AdminResource.java` | compare, ma marcata come voce di piattaforma |
| Menu laterale | il registro dei moduli del frontend non la conosce | nessuna azione, ma va provato |
| Lettura «dove posso entrare» | `MeAppAccessResource.java` (UC 0099) | esclusa |

## Passo 2 — Il servizio dei posti

**File nuovi** in `services/core/src/main/java/app/appgrove/core/billing/seats/`:

- `SeatSubscriptionService.java` — il cuore: dato il numero di posti bersaglio, calcola il dovuto (UC 0102),
  crea o aggiorna l'abbonamento di piattaforma con la quantità, e delega l'addebito al fornitore di
  pagamento attraverso `PaymentProvider` (già astratto, con il simulatore locale — è la ragione per cui
  questa storia si può provare senza toccare il fornitore vero).
- `SeatQuoteResource.java` — `GET /api/platform/v1/me/seats` con: posti usati e loro composizione, fascia,
  dovuto attuale, **costo del prossimo posto** e nuovo totale (compreso il caso in cui scende). Tutti i
  numeri li calcola il servizio.

## Passo 3 — L'invito che passa dalla cassa

**Modifica**: `InvitationResource.java` — la creazione diventa una sequenza ordinata, tutta dentro una
transazione con il chiamante del fornitore di pagamento **fuori** da essa (non si tiene aperta una
transazione su una chiamata di rete):

1. rifiuto se esiste una riduzione in attesa (UC 0104);
2. verifiche già esistenti su indirizzo duplicato;
3. calcolo del nuovo dovuto;
4. se il nuovo posto è oltre la franchigia → addebito; se fallisce, **si esce senza creare nulla**;
5. creazione dell'invito e collegamento all'addebito;
6. invio dell'email (già oggi delegato al servizio di autenticazione);
7. se il passo 5 fallisce dopo il 4 → annullamento dell'addebito e avviso operativo di severità alta.

**Atomicità**: il conteggio dei posti e la creazione dell'invito vanno serializzati per account. La via più
semplice e già in uso nel progetto è il blocco pessimistico sulla riga dell'account
(`SELECT ... FOR UPDATE` su `platform.accounts`). Scriverlo nel commento: è il tipo di dettaglio che sembra
superfluo finché due clic simultanei non addebitano due volte.

## Passo 4 — Il riquadro dei posti nella schermata

**Modifica**: [MembersPage.tsx](../../../../frontend/apps/backoffice/src/pages/members/MembersPage.tsx) — il
riquadro in testa (posizione già predisposta da UC 0100): posti usati con composizione, importo attuale con
fascia, costo del prossimo posto, e la nota quando la tariffa scende.

**Modifica**: la finestra di invito mostra la stima **prima** della conferma; il pulsante resta disabilitato
finché la stima non è nota (mai invitare alla cieca) e in caso di errore di lettura l'invito è impedito.

**Modifica**: `hooks.ts` — nuova lettura dei posti, con invalidazione dopo ogni invito, revoca o rimozione.

Traduzioni nelle cinque lingue, comprese le frasi con i numeri (attenzione al plurale: «1 posto» / «8
posti»).

## Passo 5 — Collaudi

- `SeatSubscriptionServiceTest.java`: franchigia → nessun abbonamento; quarto posto → abbonamento con
  quantità 1; quinto → 2.
- Addebito rifiutato → nessun invito; creazione fallita dopo addebito → addebito annullato.
- Concorrenza: due inviti simultanei, un solo addebito del salto.
- Invito scaduto o revocato → posto liberato; nuovo invito nello stesso periodo senza secondo addebito.
- **Cinque collaudi di esclusione**, uno per ogni riga della tabella del passo 1.
- `frontend`: riquadro dei posti nei suoi stati; pulsante disabilitato senza stima.
- Percorso di piattaforma `J-SEATS` (scritto in UC 0113, ma il primo tratto si può già fare qui).

## Verifica finale

```bash
cd services && mvn -B -pl core -am test
cd ../frontend && npm run typecheck && npm test
cd .. && ./run-tests.sh backend frontend
```

## Trappole note

1. **La transazione e la chiamata di rete**: mai tenere aperta una transazione mentre si chiama il fornitore
   di pagamento. Sequenza corretta: prenota (transazione breve) → addebita (fuori) → conferma (transazione
   breve).
2. **Il simulatore locale del pagamento** permette di provare tutto senza il fornitore vero: usarlo, e
   ricordarsi che il comportamento reale sulla proporzione del periodo va verificato più tardi.
3. **Le esclusioni della voce di piattaforma** sono cinque punti in cui *non* fare qualcosa: senza collaudi
   dedicati, uno resterà scoperto e comparirà nel menu di un cliente.
