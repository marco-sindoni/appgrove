# Change 0087 — Epica 22: rifacimento del modello di appartenenza (membership)

**Tipo**: change documentale (analisi) + un ritocco mirato al rilevatore del registro di copertura
**Modalità**: autopilot (i tre presidi restano dello sviluppatore)
**Origine**: richiesta diretta dello sviluppatore — nuova area `docs/usecases/22-refactor-membership-model/`
**Ultimo aggiornamento**: 2026-08-19

## 1. Obiettivo

Produrre l'**analisi completa** — epiche, storie e piani di lavoro — del rifacimento della gestione degli
utenti e delle loro appartenenze sulla piattaforma appgrove, secondo il modello **centralizzato** dettato
dallo sviluppatore: un solo elenco di utenti di piattaforma, e il **ruolo che appartiene alla coppia
utente × applicazione**, non alla persona in quanto tale.

Accanto ai documenti, la change produce **cinque prototipi navigabili** (§7) che mostrano la nuova
navigazione vista dai quattro ruoli e la console di piattaforma per il listino dei posti.

**Questa change non implementa nulla del prodotto.** Scrive documenti e prototipi. L'unica riga di
codice di produzione toccata è l'estensione del rilevatore descritta al §8, resa necessaria dalla
struttura in sottocartelle richiesta.

## 2. Il modello bersaglio, in breve

1. **Utenti centralizzati.** Tutti gli utenti dell'account sono visibili nella sezione «Members», che
   **non mostra più alcun ruolo**: il ruolo non è della persona, è del suo accesso a una applicazione.
2. **Un solo owner.** Chi ha creato l'account è `owner` per definizione; per ora se ne prevede uno solo.
3. **Accesso per applicazione con ruolo.** Ogni applicazione concede accesso a uno o più utenti con un
   ruolo fra `viewer` (sola lettura), `editor` (tutte le operazioni dell'applicazione) e `admin` (come
   editor, più l'abilitazione di altri utenti già esistenti e attivi e il cambio dei loro ruoli). Lo
   stesso utente può essere `admin` sull'applicazione 1 e `viewer` sull'applicazione 2. L'owner può
   cambiare i ruoli di tutti.
4. **I posti si pagano, a scaglioni progressivi.** Franchigia di **3 posti gratuiti**, **owner incluso**;
   oltre, **ogni posto paga la tariffa della fascia in cui cade quel posto**: 4–10 → 2,99 €/mese/posto,
   11–50 → 1,99, 51–100 → 0,99, oltre 100 → 0,49. Con 52 posti: 7 × 2,99 + 40 × 1,99 + 2 × 0,99 =
   102,51 €/mese.

   *Requisito rivisto in corsa dallo sviluppatore durante la rilettura dell'analisi.* La prima versione
   applicava la tariffa della fascia raggiunta a **tutti** i posti a pagamento, con tariffe 0,49 · 0,42 ·
   0,37 · 0,30. È stata scartata perché faceva **scendere il totale** ai confini di fascia, cosa
   indifendibile davanti a un cliente anche quando è a suo favore. Con la progressività il totale è
   sempre crescente e a scendere è il costo del posto successivo.
5. **Pagamento anticipato all'invito**, permanenza minima di un mese. Ridurre i posti significa
   **indicare quali utenti cessare** ed entrare in **riduzione in attesa** fino alla scadenza del
   periodo: durante l'attesa **nessun utente nuovo** può essere aggiunto, e l'attesa è **annullabile**.
   Gli utenti indicati **restano attivi fino allo scadere** (il posto è pagato).
6. **Solo l'owner invita** (l'operazione ha effetto economico), e solo lui vede i menu **Account**,
   **Billing** e **Members**. Il collaboratore vede in «I miei dati» la **forma ridotta** (soltanto i
   propri diritti: rettifica del nome, esportazione dei propri dati, informativa e contatto).
7. **Il collaboratore vede solo le applicazioni a cui è abilitato**, un cruscotto **senza azioni
   dispositive**, e il catalogo in **sola lettura** con la richiesta **«chiedi all'owner di installare»**
   (che recapita una email all'owner).
8. **Il listino dei posti è governato dall'amministratore di piattaforma**: può cambiare le tariffe delle
   fasce per tutti gli account, con effetto dal ciclo di fatturazione successivo.

Riferimento di ispirazione richiesto dallo sviluppatore: il modello di GitHub (ruoli di organizzazione
`owner`/`member` più ruoli per-repository; aggiunte immediate, rimozioni che valgono dal ciclo
successivo). Divergenza deliberata: da noi chi è indicato per la cessazione **non** perde subito
l'accesso, perché il posto è già pagato.

## 3. Che cosa questa analisi rovescia

L'epica **14 — modello utenti multi-app** (storie `0072`, `0073`, `0074`) registra la gestione utenti
**centralizzata** come «opzione **scartata** dall'utente», in favore di posti e listini **per-app**. La
richiesta odierna reintroduce proprio quel modello. Le tre storie vanno quindi **marcate come superate
dall'epica 22** — non cancellate, perché la memoria della decisione precedente ha valore — e tolte
dall'onda 2. L'operazione avviene **dopo** l'approvazione dell'analisi, insieme all'aggiornamento di
`EPICS-WAVE-2.md`.

## 4. Struttura prodotta

```
docs/usecases/22-refactor-membership-model/
├── README.md                      indice dell'area: epiche, storie, ordine di esecuzione, stato
├── epic/                          6 file: l'epica madre + le 5 sotto-epiche
├── story/                         21 storie numerate 0098–0118, formato drill-down standard
├── task/                          21 piani di lavoro, uno per storia
└── prototype/                     5 prototipi navigabili + la loro documentazione (§7)
```

Le **storie sono use case a pieno titolo** (numerazione assoluta continua: `0098`–`0118`), così entrano
nell'indice del catalogo, nell'onda 2 e nel registro di copertura come tutte le altre.

## 5. Le ventuno storie

Erano sedici alla prima stesura; `0114` e `0115` sono state aggiunte dopo la prima revisione (riverifica
della categorizzazione B2C/B2B) e `0116`–`0118` durante la rilettura, quando l'esame del modello dei dati ha
mostrato che il vincolo «una persona appartiene a un solo account» è imposto per costruzione e blocca il
primo cliente che invita un collaboratore già registrato.

**E22.5 — Identità e appartenenze** *(si esegue per prima)*
| UC | Titolo |
|---|---|
| 0116 | Identità della persona e appartenenze agli account |
| 0117 | Account attivo nella sessione, selettore e parità dei fornitori di identità |
| 0118 | Inviti e registrazione quando l'identità esiste già |

**E22.1 — Fondamenta del modello centralizzato**
| UC | Titolo |
|---|---|
| 0098 | Modello dati dell'accesso per applicazione (`platform.app_access`) e ruolo di piattaforma |
| 0099 | Autorizzazione per applicazione: token, proiezione del ruolo verso le app, varco riusabile |
| 0100 | Sezione «Members» come elenco unico di utenti, senza ruolo, con invito riservato all'owner |
| 0101 | Semantica dei tre ruoli (viewer/editor/admin) come contratto di piattaforma |

**E22.2 — Posti a pagamento**
| UC | Titolo |
|---|---|
| 0102 | Listino dei posti a fasce: modello versionato e calcolo del dovuto |
| 0103 | Acquisto anticipato del posto all'invito (abbonamento di piattaforma) |
| 0104 | Riduzione dei posti in attesa: scelta degli utenti, blocco delle aggiunte, annullamento, scadenza |
| 0105 | Governo del listino dei posti dalla console di piattaforma (effetto dal ciclo successivo) |
| 0106 | I posti nella sezione «Billing»: righe, storico, prossimo rinnovo |

**E22.3 — Esperienza del backoffice per ruolo**
| UC | Titolo |
|---|---|
| 0107 | Menu, rotte e visibilità per ruolo (solo le app abilitate; Account/Billing/Members all'owner) |
| 0108 | Cruscotto del collaboratore, senza azioni dispositive |
| 0109 | Catalogo in sola lettura + richiesta «chiedi all'owner di installare» |
| 0110 | «I miei dati» in forma ridotta per il collaboratore |

**E22.4 — Dentro le applicazioni e industrializzazione**
| UC | Titolo |
|---|---|
| 0111 | Schermata «Gestione utenti» dentro ogni applicazione |
| 0112 | Copilota dei ruoli nella skill `new-application` + parità dei modelli di scaffolding |
| 0113 | Migrazione degli account esistenti + copertura end-to-end per ruolo |

Ordine di esecuzione proposto: 0098 → 0099 → 0101 → 0100 → 0102 → 0103 → 0104 → 0105 → 0106 →
0107 → 0108 → 0109 → 0110 → 0111 → 0112 → 0113.

## 6. Contenuto di ogni documento

- **Epiche**: obiettivo, modello concettuale, decisioni fissate e loro motivo, confini, rischi, storie
  contenute con le loro dipendenze.
- **Storie**: formato del catalogo (`docs/usecases/_TEMPLATE.md`) — obiettivo, attori, precondizioni,
  flusso principale, casi limite ed errori, schermate e stati, dati toccati (con la parte sui dati
  personali), permessi e varchi, requisiti di test compresi i percorsi end-to-end, riferimenti e
  condizioni di completamento, punti aperti.
- **Piani di lavoro (task)**: passi ordinati e dimensionati, con **percorsi di file reali** ricavati
  dall'esplorazione del codice esistente, dipendenze fra i passi, verifiche da eseguire, trappole note.

## 7. Prototipi navigabili (artefatti HTML)

Richiesta esplicita dello sviluppatore: **cinque** prototipi navigabili che **evolvono l'interfaccia
attuale senza cambiarne lo stile**, pensati per essere letti da chi implementerà.

### 7.1 Impianto tecnico

- **Un file HTML per prototipo**, apribile con un doppio clic, senza compilazione né dipendenze da
  rete oltre ai caratteri tipografici; navigazione con poco JavaScript scritto a mano.
- **Nessun cambiamento di stile, garantito per costruzione**: ogni prototipo importa il file dei
  token del design system reale — `frontend/packages/design-system/src/tokens/tokens.css` — e dipinge
  solo con quelle variabili. Non un colore scritto a mano. Se i token evolvono, i prototipi seguono.
- **Fedeltà ai componenti esistenti**: schede, tabelle, etichette di stato, intestazioni di pagina e
  menu laterale riproducono i componenti veri (`Card`, `Table`, `Badge`, `Button`, `PageHeader` del
  pacchetto `@appgrove/design-system`), così ogni elemento del prototipo ha un corrispondente già
  scritto nel codice.
- **Scelta motivata**: *non* si estendono i prototipi esistenti `docs/frontend-design/v1/*.dc.html`.
  Sono monoliti da centoventimila caratteri che dipendono da un runtime proprietario di terze parti
  (`support.js`), sono in inglese e precedono metà delle schermate oggi esistenti: evolverli
  costerebbe più che ricostruire fedelmente le sole schermate in gioco.

### 7.2 I cinque prototipi

Tutti e quattro i prototipi per ruolo mostrano **lo stesso caso d'uso sulla stessa applicazione** — il
**Mini-CRM**, che è l'applicazione multi-utente del catalogo e ha già dati da leggere e modificare
(contatti e trattative) più una schermata di gestione degli utenti. Il caso d'uso è: *entro nel
workspace, apro il Mini-CRM, lavoro sui contatti, e guardo chi ha accesso all'applicazione*.

| # | File | Chi guarda | Che cosa mette in evidenza |
|---|---|---|---|
| 1 | `owner.html` | Owner dell'account | Vede tutto: menu completo (Account, Billing, Members), la sezione **Members** di piattaforma come elenco unico **senza ruolo**, l'invito con l'effetto economico (posti usati, costo del posto in più, riduzione in attesa), e nel Mini-CRM la **gestione utenti** con ogni ruolo assegnabile |
| 2 | `admin.html` | `admin` sul Mini-CRM | Menu ridotto (nessun Account/Billing/Members, «I miei dati» in forma ridotta), solo le applicazioni abilitate; **dentro** il Mini-CRM può abilitare utenti già esistenti e cambiare i loro ruoli, ma **non** può invitare gente nuova sulla piattaforma: la richiesta rimanda all'owner |
| 3 | `editor.html` | `editor` sul Mini-CRM | Tutte le operazioni dell'applicazione (crea, modifica, elimina contatti) ma la gestione utenti è in **sola lettura**: vede chi ha accesso, non lo cambia |
| 4 | `viewer.html` | `viewer` sul Mini-CRM | Sola lettura ovunque: i comandi di creazione, modifica ed eliminazione sono **assenti o disabilitati con spiegazione**; l'elenco di chi ha accesso è leggibile |
| 5 | `platform-admin.html` | Amministratore di piattaforma appgrove | Console di piattaforma: **impostazione delle tariffe delle fasce dei posti** per tutti gli account, con anteprima dell'effetto, data di decorrenza dal ciclo successivo e storico delle versioni del listino |

**Nota di aritmetica sulle fasce.** Lo sviluppatore le ha chiamate «tre fasce», ma i prezzi dati sono
**quattro** tariffe più la franchigia: 1–3 gratuiti, 4–10 → 2,99, 11–50 → 1,99, 51–100 → 0,99, oltre
100 → 0,49. Il prototipo espone la franchigia e le **quattro** tariffe, e ne calcola il dovuto **a
scaglioni**: il calcolo della console è quello vero, verificato contro la tabella di UC 0102.

Ogni prototipo mostra, nella stessa pagina, **il confronto fra i ruoli**: un riquadro «cosa cambia per
questo ruolo» che elenca ciò che è nascosto, ciò che è in sola lettura e il perché — l'informazione che
serve a chi implementa e a chi collauda.

### 7.3 Documentazione a corredo

`prototype/README.md`, pensato per l'implementazione e non per la vetrina:

1. **Mappa delle schermate** e degli stati (caricamento, vuoto, errore, successo) di ognuna;
2. **Tabella di mappatura**, riga per riga: elemento del prototipo → **file React reale** da creare o
   modificare (percorso vero, es. `frontend/apps/backoffice/src/pages/members/MembersPage.tsx`) → che
   cosa cambia → **quale chiamata di rete** serve e quale storia la introduce;
3. **Matrice ruolo × elemento**: per ogni voce di menu, schermata e comando, se è *visibile*,
   *disabilitata* o *assente* per owner, admin, editor, viewer — la specifica che chi implementa
   traduce in guardie di rotta e in condizioni di visibilità, e chi collauda traduce in casi di prova;
4. **Come aprirli** e quali scelte sono deliberatamente finte (dati d'esempio, nessuna chiamata reale).

I prototipi sono **specifica illustrata**, non codice da riusare: nessun frammento va copiato nel
prodotto, perché lì valgono i componenti del design system e le loro regole di accessibilità.

## 8. L'unico intervento sul codice

`tools/e2e-coverage/lib.mjs` enumera gli use case leggendo **soltanto il primo livello** di ciascuna area
(`docs/usecases/<area>/NNNN-*.md`). Con le storie in `story/` diventerebbero **invisibili** al presidio che
pretende la classificazione di ogni use case: un buco silenzioso. La funzione `listCatalogUseCases` va
quindi estesa a scandire **un livello di sottocartelle**, con il suo test, e le sedici storie nuove vanno
classificate nel registro come esenzione `non-implementato` (la loro superficie non esiste ancora).

## 9. Requisiti di test

- La change è documentale: nessun test applicativo di prodotto.
- L'estensione del rilevatore va coperta da un test in `tools/e2e-coverage/test/` e l'area **`tooling`**
  di `./run-tests.sh` deve restare verde (comprende il controllo del registro e la parità dello
  scaffolding).
- Copertura end-to-end (UC 0093/0094): **nessun impatto** su percorsi esistenti; le sedici storie
  dichiarano nel proprio drill-down i percorsi che serviranno, e entrano nel registro come
  `non-implementato` finché la superficie non esiste.

## 10. Condizioni di completamento

1. Le 5 epiche, le 16 storie e i 16 piani di lavoro sono scritti, in italiano, senza sigle non spiegate.
2. I 5 prototipi navigabili si aprono e si navigano, dipingono solo con i token reali, e
   `prototype/README.md` porta mappa, tabella di mappatura ai file React e matrice ruolo × elemento.
3. `docs/usecases/README.md` registra l'area 22 con le sue storie; `docs/usecases/22-…/README.md` è
   l'indice interno.
4. Il registro di copertura classifica le storie nuove e il rilevatore le vede davvero.
5. `./run-tests.sh tooling` verde (più `compliance` per prudenza).
6. Il registro delle decisioni `decisions.json` è completo e coerente con il log.
7. **L'analisi si ferma per la revisione dello sviluppatore**: `EPICS-WAVE-2.md` e il superamento
   dell'epica 14 si aggiornano **solo dopo** il suo esplicito «l'analisi è ok».

## 11. Fuori scope

- Qualunque implementazione del modello (codice di prodotto, migrazioni di banca dati, interfacce).
- Il collegamento col fornitore di pagamento per i prezzi dei posti (lo analizza 0103/0105, lo esegue chi
  implementa).
- La revisione dei documenti di decisione trasversali (`docs/02`, `docs/09`, `docs/13`): le storie
  annotano gli aggiornamenti necessari, che avvengono quando si implementa.
