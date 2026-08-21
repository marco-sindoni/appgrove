# UC 0108 — Cruscotto del collaboratore, senza azioni dispositive

**Area**: 22-refactor-membership-model · **Fase**: evo · **Stato**: 🟢 scritto (da implementare)
**Epica**: [E22.3 Esperienza per ruolo](../epic/E22-03-esperienza-per-ruolo.md)
**Dipendenze**: UC 0107 (visibilità per ruolo), UC 0097 (cruscotto operativo del workspace)
**Piano di lavoro**: [task/0108](../task/0108-cruscotto-collaboratore.md)
**Prototipi**: [admin](../prototype/admin.html) · [editor](../prototype/editor.html) · [viewer](../prototype/viewer.html)
**Ultimo aggiornamento**: 2026-08-21

## 1. Obiettivo / Scope

Dare al collaboratore un cruscotto **utile e onesto**: le applicazioni a cui è abilitato e come entrarci,
senza nulla che appartenga al governo dell'account.

**Incluso**: quali riquadri del cruscotto restano e quali spariscono per un collaboratore; il testo di
benvenuto; il caso «nessuna applicazione»; le scorciatoie; l'**invito ad aprire un proprio account** per chi
collabora soltanto negli account di altri (§4.5).

**Escluso**: il menu → UC 0107; il catalogo → UC 0109.

## 2. Attori & ruoli

- **Collaboratore**: vede il cruscotto ridotto, con qualunque ruolo sulle applicazioni.
- **Owner**: vede il cruscotto completo come oggi, senza cambiamenti.

## 3. Precondizioni

- Esiste il cruscotto operativo (UC 0097) con i suoi riquadri: applicazioni attive, spesa, scadenze,
  attività, scorciatoie.
- La shell conosce gli accessi della persona (UC 0099, UC 0107).

## 4. Flusso principale

1. Il collaboratore entra e vede il cruscotto.
2. Restano: il **saluto**, l'elenco delle **applicazioni a cui è abilitato** — con il ruolo che ha su
   ognuna, dichiarato senza giri di parole («puoi consultare», «puoi modificare», «puoi gestire gli
   utenti») — e il pulsante per **entrare** in ognuna.
3. Sparisce tutto ciò che è dispositivo o di governo:
   - il comando «**gestisci il piano**» sulla scheda di ogni applicazione (porta alla fatturazione);
   - la **spesa mensile** dell'account e ogni cifra economica;
   - le **scadenze di pagamento** e gli avvisi di fatturazione;
   - la scorciatoia «**invita una persona**»;
   - le scorciatoie a fatturazione e catalogo restano solo dove hanno senso (il catalogo sì, in sola
     lettura; la fatturazione no).
4. Sulla scheda di ogni applicazione resta la **barra di consumo** della quota, se l'applicazione la
   dichiara: è informazione operativa e riguarda il lavoro, non il denaro. Nessun invito all'aumento di
   piano, che sarebbe una leva dell'owner.
5. **Se la persona non è titolare di nessun account**, in testa al cruscotto — sotto gli eventuali inviti
   ricevuti e sopra le applicazioni — compare un **invito ad aprire un account proprio**. Dettaglio in §4.5.

### 4.5 Invito ad aprire un proprio account (richiesta dello sviluppatore, 2026-08-21)

Dopo UC 0116 esiste una figura che prima non poteva esistere: la persona **nata da un invito**, che sta sulla
piattaforma solo perché qualcun altro l'ha chiamata a collaborare e **non ha un account proprio**. Oggi non le
viene detto da nessuna parte che potrebbe averne uno — e per lei l'ostacolo non è il prezzo (i primi tre posti
sono gratuiti, il suo compreso: §2 dell'epica), è che **non sa che si può**.

**La condizione è sull'insieme delle appartenenze, non sul ruolo.** L'invito si mostra quando **nessuna**
appartenenza viva della persona porta il ruolo di piattaforma `owner`. Non è la stessa cosa di «sono un
collaboratore in questo account»: chi ha un proprio account e collabora anche altrove **non deve vederlo mai**,
nemmeno mentre lavora nell'account di un altro. Confondere le due condizioni produrrebbe un invito mostrato a
chi ha già ciò che gli si propone — l'errore più fastidioso di questa classe di messaggi.

**Che cosa dice.** Il messaggio deve togliere il timore di perdere quello che si ha, che è la sola vera
obiezione: aprire un account **non** significa lasciare gli account di altri. Contenuto richiesto, non copy
definitivo (la resa finale è del prototipo e dei testi nelle cinque lingue):

- *titolo*: che si può avere un proprio account appgrove;
- *corpo*: che oggi collabora negli account di altri, dove le applicazioni le **sceglie** chi l'ha invitata;
  che aprendone uno proprio decide lei; che **non perde nulla** — resta dove è, e passa da un account all'altro
  col selettore (UC 0117); che i **primi tre posti sono gratuiti**, il suo compreso;

  Attenzione a una trappola trovata rendendo il prototipo: **il rimando al selettore non vale per tutti**. Chi
  ha una sola appartenenza non ha il selettore (UC 0117: con una appartenenza non viene reso), quindi indicarlo
  come se fosse sullo schermo manda a cercare un comando che non c'è. Il testo va declinato nei due casi — chi
  ha già più account lo trova adesso, chi ne ha uno solo lo vedrà comparire — ed è la ragione per cui questo è
  un requisito e non una frase da tradurre e basta.
- *comandi*: aprire l'account (porta al percorso di registrazione **già esistente**) e rinviare.

**Quanto vive l'invito, e come si rinvia** (deciso dallo sviluppatore, 2026-08-21). Due orologi, uno dentro
l'altro:

| Orologio | Durata | Che cosa fa |
|---|---|---|
| **Finestra di vita** | **un anno dall'iscrizione** della persona | oltre l'anno l'invito **non compare più**, e il meccanismo del rinvio si spegne con lui |
| **Rinvio** | **una settimana** per ogni «Non ora» | entro l'anno, chi rinvia non lo rivede per sette giorni; poi ricompare |

La ragione della forma è che un invito commerciale che ricompare a ogni accesso si smette di leggere e diventa
rumore su una schermata di lavoro; ma uno che sparisce per sempre al primo rinvio spreca l'unica occasione di
dirlo a chi non sapeva che si poteva. Un anno è la finestra in cui la proposta è ancora nuova per chi la riceve.

L'**iscrizione** da cui si contano i dodici mesi è la nascita dell'**identità** della persona
(`platform.identity.created_at`), non l'ingresso nell'account in cui sta lavorando: la proposta riguarda la
persona, e non si azzera perché qualcuno l'ha invitata da un'altra parte.

**Dove vive il rinvio: sul server** (deciso dallo sviluppatore, 2026-08-21, dopo la segnalazione sul cookie). Il
rinvio è una **data sull'identità** — `own_account_invite_snoozed_until` — e **non** un cookie. Tre conseguenze:

1. **Nessuna archiviazione sul terminale**, quindi la questione ePrivacy non si pone: l'inventario dei cookie
   resta a soli tecnici essenziali e la premessa su cui il prodotto non ha il banner di consenso
   ([docs/13 §F](../../../13-compliance-privacy.md), decisione 27) non viene toccata. Era la ragione della
   segnalazione, ed è chiusa;
2. il rinvio **segue la persona su tutti i suoi dispositivi**, che è anche il comportamento che uno si aspetta —
   un cookie l'avrebbe fatto valere su un browser solo;
3. il dato è **minimo**: una data, e per un anno. Va nel manifesto dei dati con la sua finalità (proposta
   commerciale) e cade da sé alla scadenza della finestra.

**L'invito si spegne da sé appena la persona apre un proprio account** (richiesto esplicitamente, 2026-08-21).
Non serve nessun meccanismo in più, e questo è la prova che la condizione di §4.5 è quella giusta: appena nasce
l'appartenenza con ruolo `owner`, «nessuna appartenenza porta il ruolo owner» diventa falsa e l'invito sparisce —
senza toccare il rinvio, senza una regola dedicata, senza un caso particolare. Una condizione che avesse
guardato «sono collaboratore in questo account» avrebbe invece continuato a mostrarlo.

Attenzione a un solo punto in implementazione: se la risposta viene tenuta in memoria o in una copia locale, la
**creazione dell'account deve invalidarla**. Nel percorso normale il problema non si presenta — aprire un account
cambia l'account attivo e rinnova la sessione, quindi il calcolo si rifà — ma è la cosa da verificare con un
collaudo, non da dare per scontata.

### Testi approvati, nelle cinque lingue

Tradotti dall'italiano su indicazione dello sviluppatore (2026-08-21). Registro linguistico allineato a quello
già in uso nel prodotto: **tu** in italiano e spagnolo, **vous** in francese, **Sie** in tedesco.

Le chiavi sono **due corpi interi**, non un corpo con un frammento da innestare: la frase sul selettore cade in
posizioni diverse a seconda della lingua (in tedesco il verbo va in fondo), e comporre le frasi per pezzi si
rompe in traduzione. L'unica interpolazione è `{{accounts}}`, l'elenco degli account ospitanti — da comporre con
`Intl.ListFormat` nella lingua attiva, perché la congiunzione va localizzata («e», «and», «und», «et», «y») e
non si scrive a mano.

| Chiave | Quando |
|---|---|
| `dashboard.ownAccount.title` | sempre |
| `dashboard.ownAccount.bodyMultiAccount` | la persona ha **più** appartenenze (il selettore c'è già) |
| `dashboard.ownAccount.bodySingleAccount` | la persona ha **una** appartenenza (il selettore non è reso) |
| `dashboard.ownAccount.cta` · `dashboard.ownAccount.dismiss` | sempre |

**Italiano** (sorgente di questa traduzione)

- *title*: Puoi avere anche il tuo account appgrove
- *bodyMultiAccount*: Oggi collabori in {{accounts}}, dove le applicazioni le sceglie chi ti ha invitato.
  Aprendo un account tuo decidi tu quali attivare — e non perdi nulla: resti dove sei, e passi da un account
  all’altro dal selettore qui a sinistra. I primi tre posti sono gratuiti, il tuo compreso.
- *bodySingleAccount*: Oggi collabori in {{accounts}}, dove le applicazioni le sceglie chi ti ha invitato.
  Aprendo un account tuo decidi tu quali attivare — e non perdi nulla: resti dove sei, e appena avrai due
  account comparirà qui a sinistra il selettore per passare dall’uno all’altro. I primi tre posti sono
  gratuiti, il tuo compreso.
- *cta*: Apri il mio account · *dismiss*: Non ora

**Inglese**

- *title*: You can have your own appgrove account
- *bodyMultiAccount*: Today you work in {{accounts}}, where the person who invited you chooses the apps. Open
  an account of your own and you decide which ones to turn on — and you lose nothing: you stay where you are,
  and you move between accounts from the selector on the left. The first three seats are free, yours included.
- *bodySingleAccount*: Today you work in {{accounts}}, where the person who invited you chooses the apps. Open
  an account of your own and you decide which ones to turn on — and you lose nothing: you stay where you are,
  and as soon as you have two accounts a selector will appear on the left to move between them. The first
  three seats are free, yours included.
- *cta*: Open my account · *dismiss*: Not now

**Francese**

- *title*: Vous pouvez avoir votre propre compte appgrove
- *bodyMultiAccount*: Aujourd’hui vous travaillez dans {{accounts}}, où les applications sont choisies par la
  personne qui vous a invité. En ouvrant votre propre compte, c’est vous qui décidez lesquelles activer — et
  vous ne perdez rien : vous restez où vous êtes et passez d’un compte à l’autre depuis le sélecteur à gauche.
  Les trois premières places sont gratuites, la vôtre comprise.
- *bodySingleAccount*: Aujourd’hui vous travaillez dans {{accounts}}, où les applications sont choisies par la
  personne qui vous a invité. En ouvrant votre propre compte, c’est vous qui décidez lesquelles activer — et
  vous ne perdez rien : vous restez où vous êtes et, dès que vous aurez deux comptes, un sélecteur apparaîtra
  à gauche pour passer de l’un à l’autre. Les trois premières places sont gratuites, la vôtre comprise.
- *cta*: Ouvrir mon compte · *dismiss*: Plus tard

**Spagnolo**

- *title*: También puedes tener tu propia cuenta de appgrove
- *bodyMultiAccount*: Hoy trabajas en {{accounts}}, donde las aplicaciones las elige quien te ha invitado. Si
  abres una cuenta propia, decides tú cuáles activar — y no pierdes nada: sigues donde estás y pasas de una
  cuenta a otra desde el selector de la izquierda. Las tres primeras plazas son gratuitas, la tuya incluida.
- *bodySingleAccount*: Hoy trabajas en {{accounts}}, donde las aplicaciones las elige quien te ha invitado. Si
  abres una cuenta propia, decides tú cuáles activar — y no pierdes nada: sigues donde estás y, en cuanto
  tengas dos cuentas, aparecerá a la izquierda el selector para pasar de una a otra. Las tres primeras plazas
  son gratuitas, la tuya incluida.
- *cta*: Abrir mi cuenta · *dismiss*: Ahora no

**Tedesco**

- *title*: Sie können auch Ihr eigenes appgrove-Konto haben
- *bodyMultiAccount*: Heute arbeiten Sie in {{accounts}}, wo die Person, die Sie eingeladen hat, die Apps
  auswählt. Mit einem eigenen Konto entscheiden Sie selbst, welche Sie aktivieren — und Sie verlieren nichts:
  Sie bleiben, wo Sie sind, und wechseln über die Auswahl links zwischen den Konten. Die ersten drei Plätze
  sind kostenlos, Ihr eigener eingeschlossen.
- *bodySingleAccount*: Heute arbeiten Sie in {{accounts}}, wo die Person, die Sie eingeladen hat, die Apps
  auswählt. Mit einem eigenen Konto entscheiden Sie selbst, welche Sie aktivieren — und Sie verlieren nichts:
  Sie bleiben, wo Sie sind, und sobald Sie zwei Konten haben, erscheint links die Auswahl, um zwischen ihnen
  zu wechseln. Die ersten drei Plätze sind kostenlos, Ihr eigener eingeschlossen.
- *cta*: Mein Konto eröffnen · *dismiss*: Jetzt nicht

Il termine dei **posti** (`seat` · `place` · `plaza` · `Platz`) è quello del listino di
[UC 0102](0102-listino-posti-a-fasce.md): quando quella storia porterà i propri testi, va usato lo stesso
vocabolario e non uno nuovo.

**Non dice niente a chi ospita.** L'avviso vive nella sessione della persona: l'owner dell'account ospitante
non ha modo di sapere che le è stato mostrato, né che l'ha chiusa o accettata. È la stessa riservatezza per cui
l'elenco «Members» non rivela che una persona ha un account proprio (UC 0116 §5, UC 0118).

**Aprire un account non crea una seconda identità.** Il percorso di registrazione è quello che esiste: nasce
una nuova **appartenenza** con ruolo `owner` sull'identità che la persona ha già, e le appartenenze negli
account di altri restano intatte. Questo vincolo è di UC 0116 e qui si eredita, non si reinventa.

## 5. Flussi alternativi / edge / errori

- **Edge — nessuna applicazione abilitata**: il cruscotto lo dice con chiarezza e senza colpevolizzare
  («il titolare dell'account non ti ha ancora abilitato a nessuna applicazione»), e offre due vie: il
  catalogo, dove può chiedere l'installazione di qualcosa (UC 0109), e il supporto.
- **Edge — applicazione abilitata ma disattivata dalla piattaforma**: la scheda resta visibile con lo
  stato, come già avviene, senza suggerire azioni che il collaboratore non può compiere.
- **Edge — quota esaurita**: il collaboratore lo vede (è informazione di lavoro) ma non riceve l'invito ad
  aumentare il piano: al suo posto, il suggerimento di avvisare il titolare dell'account.
- **Errore — letture non disponibili**: stato di errore con possibilità di riprovare, come già oggi.

## 6. Schermate & stati

Il cruscotto del collaboratore ha **un solo blocco** invece di quattro: le sue applicazioni. Il saluto
resta. Sotto le schede, una riga di scorciatoie ridotta.

Ogni scheda di applicazione mostra: nome e icona, il **ruolo** della persona su quella applicazione,
l'eventuale barra di consumo, il pulsante «Apri». Niente stato dell'abbonamento, niente prezzo, niente
rinnovo.

In testa, prima delle schede, due elementi condizionali e indipendenti: gli **inviti ricevuti** (UC 0118) e
l'**invito ad aprire un proprio account** (§4.5). Se ci sono entrambi, gli inviti vengono **primi**: hanno una
scadenza e chiedono una risposta a una persona in carne e ossa, mentre l'invito ad aprire un account non
scade. Reso in [prototype/editor.html](../prototype/editor.html) e
[prototype/viewer.html](../prototype/viewer.html) — i due ruoli del prototipo che non sono titolari di alcun
account; in `admin.html` **non** compare, perché quella persona è titolare di «Rinaldi Design», ed è la
dimostrazione visiva che la condizione non dipende dal ruolo.

Stati: caricamento, pronto, nessuna applicazione, errore. Per l'invito di §4.5: mostrato, rinviato (assente
dopo il comando di rinvio), **mai mostrato** per chi ha un proprio account. Da non mostrare durante il
caricamento delle appartenenze: comparire e poi sparire sarebbe peggio che comparire tardi.

## 7. Dati toccati

Nessuno nuovo. La pagina consuma la stessa lettura di UC 0099 (applicazioni con ruolo) invece della lettura
dei diritti dell'account, che per un collaboratore non è pertinente. Attenzione a **non chiedere** le
letture economiche quando chi guarda non è l'owner: sarebbero rifiutate e produrrebbero errori inutili in
console.

**Vincolo tecnico verificato per §4.5 — la lettura di oggi non basta.** L'unica lettura che elenca le
appartenenze della persona è `GET /api/platform/v1/me/memberships` (UC 0117), e nel contratto attuale ogni voce
porta **solo** identificativo e nome dell'account:
[MembershipDtos.java](../../../../services/core/src/main/java/app/appgrove/core/platform/MembershipDtos.java)
→ `record MembershipRef(String accountId, String accountName)`. **Il ruolo non c'è**, quindi allo stato attuale
l'interfaccia non può sapere se la persona è titolare da qualche parte, e la condizione di §4.5 non è
calcolabile. Va aggiunto il ruolo dell'appartenenza a quella lettura — sono dati della persona che chiede, non
di terzi, quindi non c'è nulla da minimizzare. Due avvertenze per chi implementa:

- il dato serve alla **logica**, non alla resa: il selettore dell'account continua a non mostrare etichette di
  ruolo (UC 0107), e aggiungere il campo non autorizza a stamparlo;
- se UC 0107 avesse già bisogno dello stesso campo per altre ragioni, si aggiunge **una volta**: da coordinare
  fra le due storie invece di estendere due volte lo stesso contratto.

## 8. Permessi & gate

- La pagina è accessibile a tutti gli autenticati; **il contenuto** dipende dal ruolo di piattaforma.
- Le letture economiche sono chiamate **solo** se chi guarda è l'owner.
- Nessun comando dispositivo raggiungibile, nemmeno disabilitato: qui si tratta di ambito, non di
  permesso su una singola operazione (regola di UC 0101).

## 9. Requisiti di test

- **Componente**: per un collaboratore il cruscotto non contiene «gestisci il piano», né cifre, né la
  scorciatoia di invito; per l'owner tutto resta come prima (prova di non-regressione).
- **Componente**: nessuna chiamata alle letture economiche quando chi guarda non è l'owner (verifica sulle
  chiamate simulate).
- **Componente**: caso «nessuna applicazione» con i suoi due rimandi.
- **Componente (§4.5)**: l'invito ad aprire un proprio account compare se **nessuna** appartenenza porta il
  ruolo `owner`; **non** compare se almeno una lo porta — compreso il caso insidioso «titolare altrove, qui
  collaboratore», che è la prova che distingue la condizione giusta da quella sbagliata; non compare dopo il
  rinvio; non compare mentre le appartenenze sono in caricamento.
- **Percorso end-to-end di livello 2** su `frontend/apps/backoffice/e2e/dashboard.spec.ts` (esistente, da
  estendere).

## 10. Riferimenti & Definition of Done

- **Riferimenti**: [UC 0097](../../21-catalogo-app-backoffice/0097-dashboard-operativa.md),
  [DashboardPage.tsx](../../../../frontend/apps/backoffice/src/pages/dashboard/DashboardPage.tsx) — dove
  oggi `canManage` è calcolato su `owner` **oppure** `admin` e va rifatto sul solo ruolo di piattaforma.
- **Definition of Done**:
  1. il cruscotto del collaboratore mostra solo le sue applicazioni, con il suo ruolo;
  2. nessuna leva dispositiva né cifra economica;
  3. il caso «nessuna applicazione» è accogliente e offre vie d'uscita;
  4. il cruscotto dell'owner non cambia;
  5. l'invito ad aprire un proprio account (§4.5) compare **solo** a chi non è titolare da nessuna parte, è
     chiudibile, e non trapela a chi ospita;
  6. `run-tests.sh frontend` verde più il percorso aggiornato.

## Punti aperti / decisioni differite

- ~~**Per quanto tempo vale il rinvio**~~ · ~~**copy nelle cinque lingue**~~ — **chiusi** dallo sviluppatore il
  2026-08-21: finestra di un anno dall'iscrizione, rinvio di una settimana per ogni «Non ora», testi tradotti
  dall'italiano. Tutto in §4.5.

- ~~**Dove si conserva il rinvio: cookie o preferenza sul server**~~ — **chiuso** dallo sviluppatore il
  2026-08-21: **sul server**, data `own_account_invite_snoozed_until` sull'identità. Nessun cookie, nessun
  effetto sull'assenza del banner di consenso. In §4.5.

- **Nessun punto aperto residuo su §4.5.** Restano da fare all'implementazione, ma sono lavoro e non decisioni:
  la voce nel manifesto dei dati per la nuova colonna, e il collaudo che l'invito si spenga davvero quando nasce
  l'appartenenza `owner`.

- **Sezione «inviti in attesa» in testa al cruscotto** (da [UC 0118](0118-inviti-e-registrazione-con-identita-esistente.md)):
  chi appartiene o può appartenere a più account trova qui gli inviti ricevuti, con accetta e rifiuta, prima
  delle applicazioni; la voce «Dashboard» del menu porta il numero. È lavoro di UC 0118, ma **atterra su
  questa schermata**: da coordinare quando si implementano. Reso in
  [prototype/admin.html](../prototype/admin.html). Proprietario: UC 0118.

- **Riquadro delle attività recenti**: se un collaboratore debba vedere le attività dell'account o solo le
  proprie. Proposta: **solo le proprie**, per minimizzazione. Da rifinire quando il riquadro esisterà
  davvero con dati reali. Proprietario: UC 0097.
- **Avviso al titolare quando una quota è esaurita**: utile, e sarebbe il gemello della richiesta di
  installazione (UC 0109). Rimandato a dopo il primo uso reale. Annotato in
  [docs/_BACKLOG.md](../../../_BACKLOG.md).
