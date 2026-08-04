# 0002 — Modello dati multi-account

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 01 — Fondamenta
**Storia**: `0002` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore di piattaforma
> voglio che lo schema dell'app nasca già con l'isolamento fra account e le colonne di controllo al posto giusto
> così da non dover rincorrere dopo le tabelle che se ne sono dimenticate.

**Contesto.** L'isolamento fra account è l'invariante numero uno, e le tabelle che lo dimenticano si scoprono
tardi e male. Questa storia mette in piedi lo scheletro persistente dell'app — le due tabelle senza le quali
nessuna delle altre ha senso, `piano` e `abbonato` — insieme alla convenzione che tutte le successive dovranno
seguire. Non riempie ancora le tabelle di significato: quello è il lavoro dell'epica 02. Serve farlo adesso
perché la convenzione stabilita qui viene copiata da altre nove tabelle, e correggerla dopo costa nove
migrazioni invece di una.

## 2. Requisiti funzionali

1. **RF-1** — Esiste lo schema `app_abbonati` con le prime due tabelle, `piano` e `abbonato`, create da una
   migrazione versionata.
2. **RF-2** — Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7 generata dall'applicazione,
   `created_at`, `updated_at`, `created_by`, `updated_by` e `deleted_at`.
3. **RF-3** — Ogni interrogazione passa da un repository che applica il filtro per account; non esiste un percorso
   che legga senza filtro.
4. **RF-4** — La cancellazione è **logica** (`deleted_at`): la cancellazione fisica esiste solo per i diritti
   dell'interessato e per la chiusura dell'account.
5. **RF-5** — Non ci sono chiavi esterne verso altri schemi né interrogazioni che li attraversino: il riferimento
   all'anagrafica condivisa della suite, quando arriverà, sarà **logico**.

## 3. Requisiti tecnici

- **RT-1 — Persistenza (§8).** Migrazione `V1__piani_e_abbonati.sql` sullo schema `app_abbonati`, in SQL,
  applicata da Flyway e **non** all'avvio in produzione; un ruolo di database per servizio, con privilegi solo sul
  proprio schema.
- **RT-2 — Isolamento fra account (§1).** Ogni lettura e scrittura filtra per `tenant_id` preso dal token
  verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato. Se il filtro
  manca, il comportamento corretto è **negare**, non mostrare tutto.
- **RT-3 — Struttura del backend (§2).** Entità mai esposte: al bordo passano solo oggetti di trasferimento.
- **RT-4 — Dati personali (§10).** La tabella `abbonato` conterrà dati riferiti a persone: i campi si annotano
  `@PersonalData` e vanno dichiarati nel manifesto. In questa storia si crea la **struttura**; la compilazione
  del manifesto e il contratto di esportazione e cancellazione sono la storia `0009`, che è vincolante e non
  rimandabile oltre.
- **RT-5 — Registrazione eventi (§14).** Le operazioni di scrittura registrano `tenant_id`, `app_id`, `user_id` e
  correlazione; **nessun** nome o recapito finisce nel registro.
- **RT-6 — Prove (§11).** Integrazione con database effimero e migrazioni vere; **isolamento fra due account** su
  entrambe le tabelle, con tentativo di forzare l'account dall'esterno.

## 4. Criteri di accettazione

**CA-1 — Lo schema esiste ed è versionato**
- **Dato** un database vuoto
- **Quando** si applicano le migrazioni
- **Allora** esiste `app_abbonati` con `piano` e `abbonato`, ciascuna con tutte le colonne di controllo

**CA-2 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri piani e abbonati
- **Quando** un utente di `A` chiede l'elenco
- **Allora** vede solo i propri, anche se forza l'identificativo dell'account `B` nella richiesta

**CA-3 — Cancellazione logica**
- **Dato** un abbonato esistente · **Quando** lo si cancella · **Allora** la riga resta con `deleted_at`
  valorizzato e sparisce dalle letture ordinarie

**CA-4 — Nessun percorso senza filtro**
- **Dato** il codice del servizio · **Quando** si esegue il controllo strutturale della piattaforma
- **Allora** nessuna interrogazione aggira il filtro per account, e la compilazione fallirebbe se qualcuno lo
  facesse

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh backend`;
- [ ] prove di **unità** sulla generazione delle chiavi e di **integrazione** con migrazioni vere;
- [ ] prova di **isolamento fra account** su entrambe le tabelle;
- [ ] **prova end-to-end**: *nessun impatto* — non c'è superficie utente;
- [ ] **traduzioni**: nessun testo visibile;
- [ ] **manifesto dei dati**: struttura predisposta, compilazione nella storia `0009`;
- [ ] **registro delle decisioni** compilato: convenzione delle colonne e riferimento **logico** all'anagrafica
      condivisa;
- [ ] avvio locale e `dev migrate` funzionanti senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | serve il servizio in cui mettere lo schema |

## 7. Fuori ambito

- il significato di piano e abbonato, con i loro campi veri: storie `0006` e `0008`;
- tutte le altre tabelle (abbonamento, scadenza, mandato, sollecito, avviso, richiesta, istantanea): nascono con
  le storie che le usano, non tutte insieme adesso;
- il manifesto dei dati compilato: storia `0009`.

## 8. Punti aperti

**Nessuno.** Le convenzioni sono tutte già fissate dalla piattaforma; questa storia le applica, non le sceglie.
