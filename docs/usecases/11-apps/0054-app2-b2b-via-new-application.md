# UC 0054 — App #2 (B2B multi-user, es. mini-CRM) via `new-application` (valida skill + inviti/seat)

**Area**: 11-apps · **Fase**: 4 · **Stato**: 🟢 deciso (esempio: mini-CRM)
**Dipendenze**: UC [0046](../10-skills-tooling/0046-skill-new-application.md) (skill new-application)
**Fonte decisioni**: #01 (multi-user), #02 G (inviti), #09 (seat=stock), #04/#05
**Ultimo aggiornamento**: 2026-06-21
**Aree collegate**: [01-architettura](../../01-architettura.md), [02-auth-sicurezza](../../02-auth-sicurezza.md), [09-pagamenti](../../09-pagamenti.md), [05-persistenza-dati](../../05-persistenza-dati.md)

## 1. Obiettivo / Scope
Creare la **seconda app** (B2B **multi-user**, es. mini-CRM) **interamente via `new-application`**, validando la skill end-to-end
e i percorsi multi-utente (inviti, ruoli, seat come quota **stock**).
**Incluso**: app multi-user generata dalla skill (BE+FE+modulo+CI+manifest+pricing+landing draft+seed+test); pricing **per
numero utenti** (seat = metrica **stock**); inviti/ruoli owner/admin/member intra-tenant; downgrade **gated** sui seat.
**Escluso**: l'implementazione della skill (UC 0046), i pagamenti runtime (UC 0024-0039), l'admin (UC 0021).

## 2. Attori & ruoli
- **Owner/admin** (tenant B2B): invitano membri, gestiscono seat/tier.
- **Member**: usa l'app secondo ruolo.
- **`new-application`**: genera l'app; conferma la skill funzionante.

## 3. Precondizioni
- Skill `new-application` (UC 0046); inviti/ruoli del core (UC 0013); enforcement quota (UC 0027) per i seat.

## 4. Flusso principale
1. `/new-application "mini-CRM"` con `user_model=multi-user` → scaffold completo (UC 0046).
2. **Co-pilota pricing**: tier **per numero utenti** (seat); il co-pilota chiede natura metrica → **stock** (#09 E23).
3. **Inviti/ruoli**: l'app riusa il flusso inviti del core (owner/admin invita email→token single-use→member nel tenant invitante) (#02 14, #01 8).
4. **Seat come stock**: il numero di utenti è un tetto sullo stato corrente; superarlo → gate quota (UC 0027).
5. **Downgrade gated** (#09 E23): se i seat correnti eccedono il tier target → blocco + remediation ("rientra nel limite"); poi downgrade a fine periodo.

## 5. Flussi alternativi / edge / errori
- **Invito oltre il seat**: bloccato dal gate quota stock (429/remediation) finché non si fa upgrade.
- **Downgrade con troppi utenti**: gated finché non si rientra (#09 E23/24).
- **Isolamento intra-tenant**: i membri vedono i dati del **proprio** tenant (filtro `tenant_id`); authz intra-tenant via ruoli/`created_by` (#05 10).
- **Validazione skill**: eventuali lacune emerse → si correggono in `new-application` (UC 0046).

## 6. Schermate & stati
UI mini-CRM (es. contatti/pipeline) + gestione membri/inviti (owner/admin); banner seat/quota; stati loading/empty/error.
Coerenti con shell + design system.

## 7. Dati toccati
Schema `app_<app_id>` (es. contatti = **dati personali di terzi** immessi dal tenant). **Postura uniforme** #13 A2: appgrove
titolare anche per i contenuti dei consumatori; per i tenant B2B titolari, appgrove **responsabile** (DPA incorporato). Manifesto
dati per-app (categorie/finalità/base/retention) generato da `new-application`. `@PersonalData` sui campi contatto.

## 8. Permessi & gate
- **Invarianti**: `tenant_id` dal JWT; filtro row-level; ruolo DB per-servizio; logging strutturato — ereditati dallo scaffold.
- **Ruoli intra-tenant** owner/admin/member (`@RolesAllowed`); **seat = stock** con downgrade gated (#09 E23). Diritti GDPR esenti (#09 F31).

## 9. Requisiti di test
- **Integration + security/multi-tenancy** (#10 D): isolamento cross-tenant; matrice ruoli; anti-override `tenant_id`.
- **E2E** (#10 F): flusso B2B (invito→accept→uso come member), seat/quota stock, downgrade gated.
- **Compliance**: export/purge dell'app coprono i dati dei contatti; manifesto allineato.

## 10. Riferimenti & Definition of Done
- **Decisioni**: #01 4/8, #02 14, #09 E23/E24/F31, #05 10, #13 A2.
- **DoD**:
  1. App #2 B2B multi-user generata **interamente** da `new-application` (skill validata).
  2. Inviti/ruoli + seat come quota stock + downgrade gated funzionanti.
  3. Suite security/multi-tenancy + E2E B2B + compliance verdi.
  4. Eventuali lacune della skill corrette in UC 0046.

## Punti aperti / decisioni differite

- **Primo consumatore `stock` reale (seats) della SPI quota** _(tracciato dalla change `0023-use-case-0027-…`)_. UC 0027
  ha completato il **contratto** quota flow/stock in `commons` (`QuotaNature`, `EntitlementService.natureOf/capFor`, SPI che
  gestisce semanticamente entrambe) ma l'**enforcement live è solo `flow`** (app `fatture`); la natura `stock` è coperta
  solo da test a livello commons/SPI, **senza app/metrica fittizia**. **Questo UC è il primo consumatore stock reale**: la
  metrica "seats" (posti B2B) è `stock` (tetto sul livello istantaneo, niente reset) con **downgrade gated** a monte
  (UC 0026). Deve **riusare** il contratto già pronto — `quota.checkAndReserve("seats")` prima di consumare un posto — senza
  re-introdurre logica di tetto. Il cap arriva dall'entitlement (`app_tier.limits` con `type: stock`) via lo stesso
  read-model di UC 0027.
- **Niente chiamata sincrona app→core**: l'enforcement dell'app #2 **non deve perpetuare** la chiamata HTTP sincrona
  `app → core` introdotta da UC 0027 — va costruito sulla **proiezione locale** event-driven industrializzata da UC 0046
  (vedi [_INDEX.md](../_INDEX.md) Eccezioni #5 e [_BACKLOG.md](../../_BACKLOG.md)).
  ✅ **Risolto a monte** dalla change `0041-use-case-0046-…`: la proiezione locale è il percorso **predefinito** dello
  scaffold, quindi l'app #2 la eredita senza fare nulla. Attenzione a **non regredire**: se durante UC 0054 vien voglia
  di iniettare la lettura sincrona (`@SafetyNet`) nel codice di dominio per "semplificare", è un passo indietro —
  quel qualificatore esiste apposta per rendere esplicito chi chiede una chiamata di rete sul percorso caldo.
  Nota per i **posti (seat)**, metrica a giacenza: la proiezione riporta il tetto, ma il **conteggio** dei posti
  occupati resta un dato dell'app; non tentare di leggerlo dalla proiezione.

### Rimandi aperti dopo la change `0042-use-case-0054-…` (App #2 implementata)

Lavoro **deliberatamente lasciato fuori** da questa change, con dove è tracciato:

- **Generalizzare i «posti» e il canale d'uso nello scaffold** _(owner: [UC 0046](../10-skills-tooling/0046-skill-new-application.md))_.
  Nell'app #2 la gestione posti (`Seat`, `SeatResource`, `SeatAccess`, `SeatUsagePublisher`) e la pubblicazione dell'uso
  a giacenza sono scritte **a mano nel servizio crm**, non nei modelli-sorgente: era giusto così (UC 0054 non deve
  anticipare la skill, e solo le app multi-utente a giacenza ne hanno bisogno). Ma se nascerà una **seconda** app
  multi-utente a giacenza, questo va industrializzato nella skill `new-application` (una variante `--user-model multi`
  che genera posti + varco posto + publisher). Finché c'è **una sola** app così, resta a mano. Il contratto del canale
  (`commons/UsageEvents`) e il consumatore in core (`AppUsageConsumer`/`AppUsageStore`) sono invece **già generici**.
- **Consumo quota in tempo reale nel pannello di fatturazione** _(owner: [UC 0028](../07-payments/0028-portale-cliente-self-service.md))_.
  Questa change costruisce la **sorgente** (l'uso a giacenza materializzato in `platform.app_usage_stock`); mostrarla nel
  portale cliente («4 posti su 10») resta di UC 0028. Il gate del downgrade la consuma già; il pannello no.
- **Ruolo applicativo del posto**: oggi un posto è un semplice **permesso d'accesso** (presenza/assenza); non porta un
  ruolo specifico dell'app (i permessi vengono dal ruolo di account owner/admin/member nel token). Se un'app volesse
  ruoli **interni** all'app diversi da quelli di account, è un'estensione futura, non di questo UC.
- **Presidio del testo libero (note contatto/interazione)** _(owner: compliance, [_BACKLOG](../../_BACKLOG.md) / UC 0033)_.
  I campi nota sono dichiarati nel manifesto come possibile ingresso non presidiato per categorie particolari (art. 9):
  l'app **non** fa rilevazione né blocco di contenuto. Un eventuale presidio (avviso, classificazione) è tema compliance
  trasversale, non di questa app.
