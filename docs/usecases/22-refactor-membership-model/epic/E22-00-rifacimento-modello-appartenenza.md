# Epica 22 — Rifacimento del modello di appartenenza (membership)

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 analisi scritta (da implementare)
**Sotto-epiche**: [E22.5 Identità e appartenenze](E22-05-identita-e-appartenenze.md) *(si esegue per prima)* · [E22.1 Fondamenta](E22-01-fondamenta-modello-centralizzato.md) · [E22.2 Posti a pagamento](E22-02-posti-a-pagamento.md) · [E22.3 Esperienza per ruolo](E22-03-esperienza-per-ruolo.md) · [E22.4 Dentro le applicazioni](E22-04-app-e-industrializzazione.md)
**Storie**: UC 0098 – 0120 (ventitré) · **Prototipi**: [prototype/](../prototype/README.md)
**Origine**: change `0087` — requisiti dettati dallo sviluppatore; storie `0119` e `0120` aggiunte il 22 agosto 2026 (vedi §9)
**Ultimo aggiornamento**: 2026-08-22

---

## 1. Il problema di oggi

Il modello di partenza nasceva da una semplificazione dichiarata in UC 0013: **l'appartenenza era
"ripiegata" sull'utente**. Una persona apparteneva a un solo account e portava **un unico ruolo**
(`owner`, `admin`, `member`) valido su tutto: nessuna tabella delle appartenenze, il ruolo come colonna
di `platform.users`, e la Lambda che costruisce il token lo copiava nel claim `roles`
([handler.py](../../../../infra/modules/platform_shared/lambda/pre_token_gen/handler.py)).

> **Stato del rifacimento.** La prima metà di questa premessa è già caduta: la change `0088`
> ([UC 0116](../story/0116-identita-e-appartenenze.md)) ha separato l'identità della persona
> (`platform.identity`, di piattaforma) dall'appartenenza (`platform.membership`, di account), e una
> persona può ora appartenere a più account. Resta in piedi il secondo difetto — **un unico ruolo valido
> su tutto** — che è il tema di UC 0098/0099 e delle storie che seguono.

Da questa scelta discendono tre difetti che oggi si vedono:

1. **Il ruolo è nel posto sbagliato.** Chi amministra la contabilità non deve per questo poter
   amministrare il gestionale dei clienti. Con un ruolo unico per persona, invece, `admin` significa
   «amministratore di tutto».
2. **L'accesso alle applicazioni non esiste come concetto.** Un utente dell'account vede *tutte* le
   applicazioni a cui l'account ha diritto. L'unico argine è stato costruito **dentro una singola
   app**: il Mini-CRM si è fatto la propria tabella dei posti
   ([Seat.java](../../../../services/crm/src/main/java/app/appgrove/crm/Seat.java)) e il proprio varco
   ([SeatAccess.java](../../../../services/crm/src/main/java/app/appgrove/crm/SeatAccess.java)). Ogni
   applicazione nuova dovrebbe rifare quel lavoro, in modo un po' diverso ogni volta. È il classico difetto che si
   moltiplica.
3. **Gli utenti non hanno un prezzo.** Il valore che il cliente estrae dalla piattaforma cresce col
   numero di persone che la usano, ma il listino non lo vede: si paga per applicazione, mai per
   persona.

## 2. Il modello bersaglio

Tre idee, e tutto il resto ne discende.

**Prima idea — le persone stanno in un elenco unico.** L'account ha un solo elenco di utenti, quello
della sezione «Members». Quell'elenco **non mostra ruoli**, perché a quel livello il ruolo non esiste:
esistono le persone, il loro stato e il posto che occupano.

**Seconda idea — il ruolo appartiene alla coppia persona × applicazione.** «Marta è `admin` sul
Mini-CRM e `viewer` sulle fatture» è la frase che il modello deve poter rappresentare. Nasce quindi
l'entità dell'**accesso a un'applicazione**, che porta il ruolo. Chi non ha accesso a un'applicazione
non la vede nemmeno nel menu.

**Terza idea — il posto è la cosa che si compra.** Il posto è di **piattaforma**, non di applicazione:
si paga una volta per persona, e quella persona può poi essere abilitata su quante applicazioni si
vuole senza costi aggiuntivi. È il rovescio esatto del modello per-app che l'epica 14 aveva
immaginato (§4).

### Entità e loro relazioni, in parole

| Concetto | Che cos'è | Chi lo governa |
|---|---|---|
| **Account** | Il cliente: la radice di tutto, con il suo unico owner | Nasce all'iscrizione |
| **Utente di piattaforma** | Una persona dentro un account, con uno stato e un posto | Solo l'owner ne crea di nuovi (invito) |
| **Ruolo di piattaforma** | Soltanto due valori: `owner` (uno) e `member` (tutti gli altri) | Definito alla nascita |
| **Accesso a un'applicazione** | Il fatto che una persona possa usare una certa applicazione, **con un ruolo** | Owner sempre; `admin` di quell'applicazione limitatamente ad essa |
| **Ruolo sull'applicazione** | `viewer` · `editor` · `admin` | Owner e `admin` dell'applicazione |
| **Posto** | L'unità che si acquista, una per persona, di piattaforma | Solo l'owner (ha effetto economico) |

### I tre ruoli sull'applicazione

- **`viewer`** — vede tutti i dati dell'applicazione e **nessuna** operazione dispositiva. Dove
  l'interfaccia crea, modifica o elimina, il `viewer` può soltanto leggere.
- **`editor`** — tutto quello che vede il `viewer`, più **ogni** operazione prevista
  dall'applicazione.
- **`admin`** — tutto quello che fa l'`editor`, più l'abilitazione all'applicazione di utenti di
  piattaforma **già esistenti e attivi** e il cambio dei loro ruoli. **Non** può invitare persone
  nuove: quello costa, e costa all'account.

L'**owner** può fare tutto su tutte le applicazioni, compreso cambiare qualunque ruolo.

### Il modello economico dei posti

Franchigia di **tre posti gratuiti, owner compreso**. Oltre la franchigia il listino è a **scaglioni
progressivi**: ogni posto paga la tariffa della fascia in cui cade *quel posto*, non la tariffa
dell'ultima fascia raggiunta.

| Scaglione | Tariffa mensile per posto | Posti che vi cadono |
|---|---|---|
| 1 – 3 | gratuiti | i primi tre, owner compreso |
| 4 – 10 | 2,99 € | sette posti |
| 11 – 50 | 1,99 € | quaranta posti |
| 51 – 100 | 0,99 € | cinquanta posti |
| oltre 100 | 0,49 € | tutti i successivi |

Esempi svolti: **8 posti** → 5 × 2,99 = **14,95 €**; **12 posti** → 7 × 2,99 + 2 × 1,99 = **24,91 €**;
**52 posti** → 7 × 2,99 + 40 × 1,99 + 2 × 0,99 = **102,51 €**; **120 posti** → 7 × 2,99 + 40 × 1,99 +
50 × 0,99 + 20 × 0,49 = **159,83 €**.

Proprietà che rende il modello spiegabile: **il totale cresce sempre**, e a scendere è il **costo del
posto successivo**. Ai confini di fascia il cliente scopre che la persona in più costa meno della
precedente — che è quello che uno sconto sul volume deve sembrare.

Regole di tempo: **si paga in anticipo**, all'invio dell'invito; la permanenza minima di un posto è
**un mese**. Ridurre i posti non è immediato: si **indicano le persone da cessare** e l'account entra
in **riduzione in attesa** fino alla scadenza del periodo. Durante l'attesa **nessun posto nuovo** può
essere aggiunto, e l'attesa si può **annullare**. Le persone indicate **restano operative** fino allo
scadere, perché il loro posto è pagato.

## 3. Da dove viene l'ispirazione, e dove ci discostiamo

Lo sviluppatore ha indicato GitHub come riferimento. Le somiglianze sono volute: due soli ruoli a
livello di organizzazione (`owner` e `member`) e ruoli distinti per ogni risorsa (là i repository, qui
le applicazioni); aggiunta di persone immediata e a pagamento; rimozioni che valgono dal periodo
successivo.

Due differenze deliberate:

1. **Chi è indicato per la cessazione continua a lavorare** fino a scadenza. GitHub revoca l'accesso
   subito e continua a fatturare: noi consideriamo che un posto pagato debba restare usabile. Chi vuole
   escludere qualcuno immediatamente gli **toglie l'accesso alle applicazioni** — operazione gratuita,
   immediata e concettualmente distinta dal ridurre i posti.
2. **Blocco delle aggiunte durante l'attesa.** GitHub permette di aggiungere mentre una riduzione è in
   corso. Il requisito qui è più severo, e ha una ragione pratica: impedisce di fabbricare situazioni
   in cui il conto del periodo diventa indecidibile.

## 4. Che cosa questa epica rovescia

L'**epica 14 — modello utenti multi-app** (storie [0072](../../14-modello-utenti-multiapp/0072-distinzione-b2c-b2b-livello-app.md),
[0073](../../14-modello-utenti-multiapp/0073-invito-utenti-per-app-posti-quota.md),
[0074](../../14-modello-utenti-multiapp/0074-directory-cross-app-ui-membri.md)) descriveva
l'impianto opposto: appartenenza e posti **per applicazione**, ognuna col proprio listino, e registrava
esplicitamente la gestione utenti **centralizzata di piattaforma** come *«opzione scartata
dall'utente»*, perché avrebbe richiesto «un listino posti centrale indipendente dalle app, non
desiderato».

Lo sviluppatore ha cambiato direzione, e l'ha fatto con cognizione: proprio quel listino centrale è
oggi il requisito. Le tre storie dell'epica 14 vanno quindi **marcate come superate da questa epica** e
togliersi dall'onda 2 — **non cancellate**: la memoria di una decisione ribaltata vale più della
pulizia dell'archivio.

Che cosa dell'epica 14 **sopravvive**, tradotto:

- il *fatto* che l'accesso sia per applicazione (era per-app anche là) — resta, ed è la seconda idea;
- l'idea di **elenco unico delle persone** dell'account, che là era una «comodità di lettura» chiamata
  directory e qui diventa **il** modello;
- la distinzione `App.user_model` fra applicazione a utente singolo e multi-utente: **resta utile**,
  ma cambia significato (§7).

## 5. Le cinque sotto-epiche e le ventitré storie

| Sotto-epica | Storie | Che cosa consegna |
|---|---|---|
| [E22.5 — Identità e appartenenze](E22-05-identita-e-appartenenze.md) *(prima)* | 0116, 0117, 0118 | Una persona, più appartenenze; account attivo e selettore; inviti e registrazione con identità esistente |
| [E22.1 — Fondamenta del modello centralizzato](E22-01-fondamenta-modello-centralizzato.md) | 0098, 0099, 0100, 0101 | I dati, l'autorizzazione, l'elenco unico, il contratto dei tre ruoli |
| [E22.2 — Posti a pagamento](E22-02-posti-a-pagamento.md) | 0102, 0103, 0104, 0105, 0106 | Listino a fasce, acquisto anticipato, riduzione in attesa, governo del listino, trasparenza |
| [E22.3 — Esperienza del backoffice per ruolo](E22-03-esperienza-per-ruolo.md) | 0107, 0108, 0109, 0110, **0119** | Menu e rotte, cruscotto, catalogo con richiesta all'owner, «I miei dati» ridotto, **responsività del backoffice** |
| [E22.4 — Dentro le applicazioni e industrializzazione](E22-04-app-e-industrializzazione.md) | 0111, 0112, 0114, 0115, 0113, **0120** | Gestione utenti nell'app, copilota della skill, ritiro della categoria B2C/B2B, ambito dei dati, migrazione e collaudo, **guida di collaudo manuale unica** |

**Ordine di esecuzione** (topologico sulle dipendenze reali):

```
0116 → 0117 → 0118 → 0098 → 0099 → 0101 → 0100 → 0102 → 0103 → 0104 → 0119 → 0105 → 0106
                          → 0107 → 0108 → 0109 → 0110 → 0111 → 0112 → 0114 → 0115 → 0113 → 0120
```

**Le prime tre aprono l'epica pur essendo state scritte per ultime** (sotto-epica
[E22.5](E22-05-identita-e-appartenenze.md), emersa dalla rilettura dei dati): sciolgono il vincolo «una
persona appartiene a un solo account», che oggi è imposto da indici unici globali su una tabella interna
all'account. Vanno prima di **0098** perché quella storia crea la tabella degli accessi con un riferimento
alla persona: se la forma dell'identità cambiasse dopo, quella tabella, la sua migrazione e la migrazione
finale (0113) si rifarebbero.

Le due prima della chiusura sono le storie aggiunte dopo la prima revisione: **0114** ritira la
categoria B2C/B2B delle applicazioni (che questo modello rende falsa) e **0115** mette al suo posto
l'**ambito dei dati**, la distinzione che invece ha conseguenze vere. Stanno dopo 0112 perché è lì che il
generatore di applicazioni viene rifatto, e prima di 0113 perché la migrazione finale deve trovare il
modello già assestato.

**Le due aggiunte dopo il collaudo del lotto `0095`–`0099`** (22 agosto 2026, §9) stanno agli estremi
opposti dell'ordine. **0119** (responsività del backoffice) va **subito**, prima di 0105: presidia le
tabelle che le storie successive continuano ad allargare, e scoprirlo dopo altre undici storie
significherebbe rifarne un pezzo. **0120** (guida di collaudo manuale unica) va **ultima**, dopo 0113:
raccoglie le guide di tutta l'epica e le riscrive per percorsi, quindi non può esistere prima che l'epica
esista per intero. Come per E22.5, il numero è l'identità, non l'ordine.

Il criterio è duplice e semplice: **nulla si può mostrare per ruolo prima che il ruolo esista** nei
dati e nel token (da qui 0098 e 0099 in testa), e **nulla si può vendere prima che il posto esista**
come oggetto contabile (da qui i posti prima dell'esperienza d'uso).

## 6. Rischi, e come li teniamo a bada

| Rischio | Perché fa male | Come lo affrontiamo |
|---|---|---|
| **Ruoli nel token che invecchiano** | Se i ruoli per applicazione finissero nel token, un cambio di ruolo avrebbe effetto solo al rinnovo del token: l'utente resterebbe `admin` per minuti dopo essere stato retrocesso | UC 0099: nel token restano solo il ruolo di piattaforma e l'identità; il ruolo per applicazione si legge dal core, con la stessa proiezione locale già usata per i diritti d'accesso |
| **Perdita di accesso durante la migrazione** | Un cliente che dopo il rilascio non vede più le sue applicazioni è un incidente grave | UC 0113: migrazione che concede a **tutti** gli utenti esistenti l'accesso a **tutte** le applicazioni dell'account, con ruolo dedotto dal ruolo attuale; nessuno perde nulla il giorno del rilascio |
| **Doppio conteggio dei posti** | Il Mini-CRM conta posti suoi; se restano entrambi, i numeri divergono e il cliente paga due volte lo stesso concetto | UC 0111: i posti locali del Mini-CRM vengono **ritirati** e sostituiti dall'accesso di piattaforma; il suo limite di quota cambia significato o sparisce |
| **Un cliente si autoblocca** | Un owner che si toglie l'accesso, o l'ultimo owner rimosso, lascia l'account senza governo | UC 0098: l'owner non è rimovibile né retrocedibile e ha accesso implicito a ogni applicazione; già oggi esiste l'argine dell'«ultimo owner» |
| **Prezzi in due posti** | Le tariffe governate dalla console e il listino come codice possono divergere silenziosamente | UC 0105: il listino dei posti è **versionato in banca dati** e la console crea *nuove versioni*, mai modifiche in luogo; il file resta il valore iniziale |
| **Ruoli che ogni applicazione interpreta a modo suo** | Un `viewer` che in un'app può esportare e in un'altra no è un modello che non si può spiegare | UC 0101 fissa il contratto; UC 0112 lo fa applicare da chi genera l'app nuova |

## 7. Confini dell'epica

**Dentro**: modello dati dell'accesso e del ruolo per applicazione; autorizzazione lato servizi;
elenco unico degli utenti; posti come oggetto acquistabile con il loro listino, il loro acquisto
anticipato e la loro riduzione in attesa; console di piattaforma per le tariffe; visibilità del
backoffice per ruolo; gestione utenti dentro le applicazioni; copilota della skill che genera le
applicazioni; migrazione degli account esistenti e collaudo end-to-end per ruolo.

**Fuori**:

- **Più di un owner per account** (e quindi il trasferimento della proprietà): il requisito dice
  esplicitamente «per il momento un solo owner». Il modello dati non deve però renderlo impossibile.
- **Gruppi o squadre di utenti** (l'equivalente dei *team* di GitHub): utile in futuro, non ora.
- **Ruoli personalizzati** oltre i tre previsti.
- **Persone esterne all'account** invitate su una singola applicazione (i *collaboratori esterni* di
  GitHub).
- **Un terzo ambito dei dati** («personali ma condivisibili a scelta», la forma delle applicazioni di
  appunti e documenti): due valori bastano al bisogno noto — UC 0115 §Punti aperti.
- **Ripartizione dei costi fra applicazioni**: sapere quanto «costa» il Mini-CRM in posti non ha
  senso in questo modello, perché il posto è di piattaforma. Se un giorno servirà, sarà un'analisi
  di ricavi, non di prezzi.

## 8. Documenti da aggiornare quando si implementa

Non si toccano ora (questa change è analisi), ma le storie li nominano una per una:

| Documento | Perché cambia |
|---|---|
| [docs/02-auth-sicurezza.md](../../../02-auth-sicurezza.md) | I claim del token cambiano: `roles` diventa il solo ruolo di piattaforma |
| [docs/09-pagamenti.md](../../../09-pagamenti.md) | Nasce un abbonamento **di piattaforma** accanto a quelli per applicazione |
| [docs/13-compliance-privacy.md](../../../13-compliance-privacy.md) | Chi vede i dati di chi cambia: il ruolo per applicazione diventa parte della base d'accesso |
| [UC 0013](../../04-platform-core/0013-account-utenti-inviti-api.md) | L'appartenenza non è più «ripiegata» sull'utente |
| [UC 0059](../../06-frontend/0059-gestione-membri-inviti.md) | La schermata «Members» perde la colonna del ruolo e guadagna i posti |
| [UC 0016](../../05-auth/0016-pre-token-gen-jwt.md) · UC 0010 | La funzione che costruisce il token e il suo gemello locale |
| [UC 0027](../../07-payments/0027-applicazione-entitlement-quota.md) | I posti sono una quota di piattaforma, non di applicazione |
| [UC 0046](../../10-skills-tooling/0046-skill-new-application.md) | La skill acquisisce il copilota dei ruoli |
| [UC 0054](../../11-apps/0054-app2-b2b-via-new-application.md) | I posti locali del Mini-CRM vengono ritirati; nota sull'etichetta «b2b» nel titolo (UC 0114) |
| [docs/01-architettura.md](../../../01-architettura.md) · [UC 0051](../../11-apps/0051-app1-backend.md) · [UC 0052](../../11-apps/0052-app1-modulo-frontend.md) | La categoria B2C/B2B delle applicazioni si ritira (UC 0114) |

## 9. Il collaudo di questa epica: visivo sospeso, unico a chiusura

**Decisione dello sviluppatore, 22 agosto 2026**, presa dopo aver collaudato a mano le cinque storie del
lotto `0095`–`0099`.

### Che cosa è stato deciso

1. **Il collaudo visivo delle singole storie è sospeso** fino alla fine dell'epica. Allo sviluppatore non
   si chiede più di guardare schermate a ogni storia.
2. **Nasce una storia dedicata al collaudo manuale di chiusura** — [UC 0120](../story/0120-guida-collaudo-manuale-epica.md),
   da eseguire **dopo `0113`** — che dalle guide delle singole change ricava **una** guida di collaudo
   manuale, per percorsi coerenti e di soli passi visivi.
3. **Nasce una storia per la responsività del backoffice** — [UC 0119](../story/0119-responsivita-backoffice.md),
   da eseguire **subito**, prima delle storie che aggiungono ancora colonne e comandi alle stesse
   tabelle. È il difetto che quel collaudo a mano ha effettivamente trovato, e la sua correzione non
   aspetta la fine dell'epica: la tabella delle persone ha già sette colonne e ogni storia che arriva le
   aggiunge qualcosa.

### Perché

In un'epica di **rifacimento**, collaudare a vista una storia per volta significa **giudicare la forma di
stati intermedi che nessun utente vedrà mai**. La schermata «Membri» guardata dopo il lotto `0095`–`0099`
è per costruzione un mezzo passaggio: `0100` ha tolto il ruolo dall'elenco, `0111` costruirà il posto dove
il ruolo si governa, `0107` deciderà che cosa si vede per ruolo. Il collaudo manuale serve a giudicare
**forma, coerenza e sequenza** — e questo ha senso solo su un insieme completo. Prima è un giudizio su un
cantiere: costa attenzione a ogni storia e produce osservazioni che la storia successiva rende obsolete.

### Che cosa continua a valere — la sospensione non è uno sconto

- le storie **continuano a scrivere** `how-to-test.md` e a **eseguirne i passi non visivi**: costa solo
  agli agenti, intercetta i difetti veri (il lotto `0095`–`0099` ne ha trovati eseguendo le guide, non
  rileggendole) ed è il **materiale grezzo** della guida unica di `0120`;
- la **passata di fine lotto** di `go-fast` resta: riesegue le guide del lotto contro lo stato finale di
  `main`;
- il presidio contro i **regressi** resta quello che già c'è, e non è il collaudo a vista: **suite
  automatica completa verde a ogni commit**, percorsi Playwright di livello 2, suite di piattaforma;
- le **fermate di escalation** restano attive: prezzi, direzione di prodotto, dati personali ambigui,
  effetti irreversibili. La sospensione riguarda il *guardare le schermate*, non il *chiedere*.

### Quando decade

**A epica chiusa**, quando la guida unica di [UC 0120](../story/0120-guida-collaudo-manuale-epica.md)
viene **eseguita**. È `0120` a dichiarare la decadenza in questo documento: da allora il collaudo visivo
torna alla sua forma normale, storia per storia.

### Che cosa **non** cambia

La decisione vale per **questa epica**. L'ipotesi di farne una regola generale per tutte le epiche è
stata esaminata e **scartata**: `CLAUDE.md` e le skill non vengono toccati.

## 10. Punti aperti / decisioni differite

- **Secondo owner e passaggio di proprietà**: fuori scope per volontà dello sviluppatore. Da riprendere
  quando un cliente chiederà di non essere l'unico responsabile dell'account. Il modello dati previsto
  da UC 0098 non lo preclude.
- ~~Sorte di `App.user_model`~~ — **deciso** dopo la prima revisione: la categoria B2C/B2B si **ritira**
  (UC 0114) perché il nuovo modello la rende falsa e il suo nome era ambiguo; al suo posto nasce
  l'**ambito dei dati** (UC 0115) — dichiarato qui con la sua guardia, fatto rispettare da un filtro che si
  costruisce con la prima applicazione che ne ha bisogno — che ha conseguenze verificabili nel codice invece di essere
  un'etichetta. L'uso di «B2C/B2B» in senso **giuridico** (titolare verso i consumatori, responsabile
  verso i clienti-azienda) resta intatto: sono due significati diversi sotto lo stesso nome, ed è proprio
  l'ambiguità che il ritiro elimina.
- **Posti e periodi di prova**: se un posto acquistato durante un periodo di prova gratuito di
  un'applicazione si paghi comunque (la risposta proposta è sì: il posto è di piattaforma e non
  c'entra con le prove delle singole applicazioni). Proprietario: UC 0103.
- **Rimborsi e cambi a metà periodo** di un posto: nessun rimborso previsto, coerente con la
  permanenza minima mensile. Da confermare con chi gestisce la fatturazione. Proprietario: UC 0106.
- **Tariffe e valuta oltre l'euro**: il listino nasce in euro; il comportamento in altre valute segue
  quello delle applicazioni. Proprietario: UC 0102.
- **Notifica alle persone cessate**: se avvertire per email chi è stato indicato per la cessazione, e
  quando. Proprietario: UC 0104.
