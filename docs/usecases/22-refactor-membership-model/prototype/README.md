# Prototipi navigabili dell'epica 22 — documentazione per l'implementazione

**Epica**: [22 — Rifacimento del modello di appartenenza](../epic/E22-00-rifacimento-modello-appartenenza.md)
**A cosa servono**: mostrare la nuova navigazione vista dai quattro ruoli, e dare a chi implementa una
specifica illustrata invece di una descrizione a parole. **Ultimo aggiornamento**: 2026-08-19

> **Questi file sono specifica, non codice da riusare.** Nessun frammento va copiato nel prodotto: là
> valgono i componenti di `@appgrove/design-system` con le loro regole di accessibilità. Qui si guarda
> *che cosa* deve esserci e *per chi*.

## 0. Che cosa NON va implementato

**Leggere prima di tutto il resto.** Due elementi di questi prototipi sono **impalcatura**: esistono solo
per far leggere il prototipo, e nel prodotto non devono comparire.

| Elemento | Dov'è | Perché esiste | Nel prodotto |
|---|---|---|---|
| **Barra scura in cima** — «Prototipo · epica 22 · la stessa applicazione vista da: Owner / Admin / …» | in alto, sopra l'intestazione vera | passare da un ruolo all'altro **mantenendo la schermata**: è il modo più rapido per vedere la differenza | **non esiste.** Una persona è un ruolo, non li commuta |
| **Riquadro «Cosa cambia per …»** | pulsante in basso a destra | elenca a parole le differenze del ruolo e la matrice dei menu | **non esiste.** Le differenze fra ruoli si vivono, non si spiegano in un riquadro |
| **Note tratteggiate in corsivo** — «non reso nel prototipo: …» | dove il prototipo salta un pezzo del prodotto (per esempio i controlli di lingua, tema e notifiche nell'intestazione) | dire che quel pezzo **esiste** nel prodotto e qui non è disegnato, invece di far credere che sia stato eliminato | **non esiste** la nota; esiste il pezzo che la nota nomina |

Come si riconoscono, senza dover ricordare questa tabella:

1. **Lo dicono di sé.** Entrambi portano l'etichetta «impalcatura · non è prodotto», visibile in ogni
   schermata e quindi anche in ogni fotografia dello schermo che finisca in un documento o in un
   messaggio. È il presidio che conta, perché il fraintendimento nasce guardando un'immagine, non
   leggendo questo file.
2. **Portano un prefisso di classe che non esiste nel prodotto**: `x-proto-scaffold-`. Nel design system
   non c'è nulla che cominci così. Se una classe con quel prefisso comparisse in `frontend/`, sarebbe un
   errore di copia — e si trova con un solo comando:

   ```bash
   grep -rn "x-proto-scaffold" frontend/   # deve dare zero risultati
   ```
3. **Non appaiono nella tabella di mappatura** del §4, che elenca solo elementi con un file React di
   destinazione. Se un elemento non è in quella tabella, non ha un posto nel prodotto.
4. **Un controllo automatico lo sorveglia**: il passo (9) dell'area `tooling` di
   [run-tests.sh](../../../../run-tests.sh) fa fallire la suite se il prefisso compare in `frontend/`.
   Nessun altro collaudo se ne accorgerebbe, e un comando estraneo nell'interfaccia vera non rompe
   nulla — resta lì.

Tutto il resto — intestazione con selettore dell'account e inviti, menu laterale, schermate, comandi
disabilitati con spiegazione — **è** prodotto, e il §4 dice dove va.

## 1. Come si aprono

Doppio clic su uno dei cinque file. Nessuna compilazione, nessun server, nessuna dipendenza oltre ai
caratteri tipografici (che arrivano da rete; senza rete si vede lo stesso, con un carattere di sistema).

| File | Chi guarda | Che cosa mette in evidenza |
|---|---|---|
| [owner.html](owner.html) | **Owner** dell'account | menu completo, «Members» come elenco unico senza ruolo, riquadro dei posti col costo, riduzione in attesa, fatturazione con la scheda dei posti |
| [admin.html](admin.html) | **`admin`** sul Mini-CRM | menu ridotto, una sola applicazione, abilitazione di persone **già esistenti**, il confine (le persone nuove le invita l'owner), il **selettore di account** e un **invito ricevuto** |
| [editor.html](editor.html) | **`editor`** sul Mini-CRM | tutte le operazioni dell'applicazione, ma la schermata «Utenti» in sola lettura |
| [viewer.html](viewer.html) | **`viewer`** sul Mini-CRM | sola lettura ovunque, con i comandi **disabilitati e spiegati** invece che nascosti; ha il **selettore di account** pur essendo il ruolo con meno poteri |
| [platform-admin.html](platform-admin.html) | **Amministratore di piattaforma appgrove** | tariffe delle fasce dei posti per tutti gli account, anteprima dell'effetto, decorrenza dal ciclo successivo, storico immutabile |

I quattro prototipi per ruolo mostrano **lo stesso caso d'uso sulla stessa applicazione** (il Mini-CRM:
entro nel workspace, apro l'applicazione, lavoro sui contatti, guardo chi ha accesso). La barra in alto
passa da un ruolo all'altro **mantenendo la schermata**: è il modo più rapido per vedere la differenza.
In basso a destra, il pulsante «Cosa cambia per …» apre il riquadro che elenca le differenze del ruolo.

## 2. Lo stile non è inventato

Anche le **icone** sono quelle vere: stesso font (Material Symbols Rounded), stesso nome di classe del
prodotto (`material-symbols-rounded`, da `design-system/components/Icon.tsx`) e **stessi nomi di icona**,
copiati da `shell/Sidebar.tsx` e dai manifesti dei moduli — `space_dashboard`, `apps`, `account_circle`,
`credit_card`, `group`, `shield_person`, `support_agent`, `settings`, `shield`, `logout`, più `contacts`
per il Mini-CRM e `receipt_long` per Fatture. Non si scelgono: si copiano, altrimenti il prototipo mostra
un'interfaccia che non esiste. Nel prototipo il font arriva da rete: senza collegamento si leggono i nomi
delle icone al posto dei simboli, come già succede per i caratteri tipografici.

`assets/proto.css` **importa** la fonte unica dei token del design system —
`frontend/packages/design-system/src/tokens/tokens.css` — e dipinge solo con quelle variabili: non un
colore scritto a mano. Se i token evolvono, i prototipi seguono. Le classi rispecchiano i componenti
reali; la corrispondenza è nella tabella del §4.

## 3. Le schermate e i loro stati

| Schermata | Indirizzo nel prototipo | Stati mostrati | Stati da implementare comunque |
|---|---|---|---|
| Cruscotto | `#dashboard` | owner completo · collaboratore ridotto · nessuna applicazione | caricamento, errore |
| Members | `#members` | elenco unico · riquadro dei posti · riduzione in attesa | caricamento, errore, invito in corso |
| Catalogo | `#catalog` | attiva con accesso · attiva senza accesso · da installare · già richiesta | caricamento, errore, ricerca vuota |
| Applicazione — Contatti | `#crm-contatti` | comandi per ruolo | caricamento, elenco vuoto, errore |
| Applicazione — Utenti | `#crm-utenti` | gestibile · sola lettura | caricamento, errore |
| Billing | `#billing` | scheda dei posti col calcolo | caricamento, errore, importo non disponibile |
| I miei dati | `#privacy` | completa (owner) · ridotta (collaboratore) | esportazione in corso, errore |
| Selettore dell'account | **barra laterale, sotto il marchio** — ogni schermata | nome fisso con una appartenenza (owner, editor) · menu apribile con due (admin, viewer) · account attivo evidenziato | caricamento dell'elenco, cambio in corso, appartenenza revocata mentre il menu è aperto |
| Inviti ricevuti | **sezione del cruscotto**, in testa · più il contatore sulla voce «Dashboard» | **assente** senza inviti · un invito con accetta/rifiuta (admin) | caricamento, invito scaduto nel frattempo, errore di accettazione, più di un invito |
| Pagina di rifiuto | `#members` da un collaboratore | rifiuto con spiegazione | — |
| Listino dei posti | `platform-admin.html` | vigente · nuova versione · anteprima · fasce non valide · versione programmata | caricamento, sincronizzazione col fornitore in attesa |

Gli stati «da implementare comunque» **non** sono nei prototipi per non appesantirli, ma sono richiesti
dalle storie: chi implementa non li deve dimenticare.

## 4. Tabella di mappatura — dal prototipo al codice

Riga per riga: elemento → file reale da creare o modificare → che cosa cambia → quale chiamata di rete
serve e quale storia la introduce.

| Elemento del prototipo | File React reale | Che cosa cambia | Chiamata di rete | Storia |
|---|---|---|---|---|
| Menu laterale, elenco applicazioni | `frontend/apps/backoffice/src/shell/Sidebar.tsx` · `registry/registry.ts` | intersezione a **tre**: moduli ∩ diritti dell'account ∩ accessi della persona | `GET /api/platform/v1/me/app-access` | 0099 · 0107 |
| Menu laterale, voci Account/Billing/Members | `shell/Sidebar.tsx` | mostrate **solo** all'owner (`canManageMembers` da `owner∨admin` a `isOwner`) | nessuna (dal token) | 0107 |
| Scheda utente nel menu | `shell/Sidebar.tsx` | «Titolare dell'account» solo per l'owner; nessun ruolo per gli altri | nessuna | 0107 |
| Guardie delle rotte riservate | `routing/routes.tsx` · `routing/guards.tsx` | `requireAnyRole(['owner','admin'])` → `requireRole('owner')`; Account e Billing protette | — | 0107 |
| Ruolo passato ai moduli | `registry/types.ts` · `registry/ShellContext.tsx` | `roles: string[]` → `platformRole` + `appRole` | — | 0107 |
| Cruscotto del collaboratore | `pages/dashboard/DashboardPage.tsx` | via «gestisci il piano», cifre, scorciatoia di invito; ruolo per applicazione sulle schede | `me/app-access` | 0108 |
| «Members» elenco unico | `pages/members/MembersPage.tsx` | **una** tabella, colonna stato, colonna applicazioni, **nessun ruolo** | `GET /users` (con conteggio) + `GET /invitations` | 0100 |
| Riquadro dei posti | `pages/members/MembersPage.tsx` + nuovo componente | posti usati, composizione, importo, costo del prossimo | `GET /api/platform/v1/me/seats` | 0103 |
| Invito senza ruolo | `pages/members/MembersPage.tsx` · `api/hooks.ts` | il form perde il selettore di ruolo; mostra il costo prima di confermare | `POST /invitations` (senza ruolo) | 0100 · 0103 |
| Riduzione in attesa | `pages/members/MembersPage.tsx` | selezione multipla, avviso, annullamento, invito disabilitato | `POST/DELETE /me/seat-downgrade` | 0104 |
| Schermata «Utenti» dell'applicazione | **nuovo** `shell/AppUsersScreen.tsx`; montata dai manifesti dei moduli | componente **condiviso** da tutte le applicazioni; sostituisce `modules/crm/screens/MembersScreen.tsx` | `GET/POST/PATCH/DELETE /apps/{appId}/access` | 0111 |
| Comandi per ruolo nei contatti | `modules/crm/screens/ContactListScreen.tsx` | disabilitati con spiegazione secondo `appRole` | nessuna nuova | 0101 · 0111 |
| Involucro «disabilitato con spiegazione» | **nuovo** in `packages/design-system` | comando visibile, `aria-disabled`, descrizione collegata | — | 0101 |
| Catalogo in sola lettura | `pages/catalog/AppCatalogPage.tsx` | comando di acquisto sostituito, non disabilitato | `POST /app-install-requests` | 0109 |
| Scheda dei posti in fatturazione | **nuovo** `billing/SeatsCard.tsx` · `billing/SubscriptionsPanel.tsx` | scheda in testa; la voce di piattaforma **esclusa** dalla tabella delle applicazioni | `GET /me/subscriptions` + `me/seats` | 0106 |
| «I miei dati» ridotto | `pages/privacy/PrivacyPage.tsx` | `canManageAccountData` da `owner∨admin` a `isOwner`; nota sulla cancellazione | nessuna nuova | 0110 |
| **Selettore dell'account attivo** | `shell/Sidebar.tsx` (sotto il marchio) · `registry/ShellContext.tsx` | nome dell'account **sempre** visibile; elenco delle appartenenze; **non reso** con una sola appartenenza (non «disabilitato»); il cambio **ricarica** l'applicazione, non aggiorna lo stato in memoria | `GET /api/platform/v1/me/memberships` + `POST /me/active-account` + rinnovo del token | 0117 |
| **Avviso «account cambiato in un'altra scheda»** | `shell/ShellLayout.tsx` | confronto fra l'account del token in uso e l'account attivo conservato; invito a ricaricare | nessuna nuova (dal token + `me/memberships`) | 0117 |
| **Inviti ricevuti — sezione del cruscotto** | `pages/dashboard/DashboardPage.tsx` + **nuovo** `dashboard/PendingInvitesSection.tsx` | in testa al cruscotto, prima delle applicazioni; accettando nasce una nuova **appartenenza**, non una seconda identità | `GET /me/invitations` + `POST /me/invitations/{id}/accept\|reject` | 0118 · 0108 |
| **Contatore degli inviti sulla voce «Dashboard»** | `shell/Sidebar.tsx` | numero accanto alla voce quando ci sono inviti in attesa: da un'altra schermata resterebbero invisibili | `GET /me/invitations` (già caricata) | 0118 |
| **Nessuna etichetta di ruolo nell'intestazione** | `shell/Topbar.tsx` (nulla da aggiungere) | il ruolo è **per applicazione**: una sola etichetta globale sarebbe falsa appena una persona è abilitata a più di una applicazione. Si legge sulla scheda dell'applicazione nel cruscotto e in testa alle sue schermate | — | 0101 · 0107 |
| Listino dei posti (console) | **nuovo** `frontend/apps/admin/src/pages/SeatPricing.tsx` | vigente, nuova versione, anteprima, storico | `GET/POST /api/admin/v1/seat-pricing` | 0105 |

## 5. Matrice ruolo × elemento — la specifica

Tre valori, e sono **tre meccanismi diversi** da implementare:

- **visibile** — c'è e si usa;
- **sola lettura** — c'è, **disabilitato con la spiegazione** del ruolo mancante (mai nascosto: la
  funzione esiste, manca il ruolo);
- **assente** — non compare nella navigazione (ambito che non compete a quel ruolo).

### Voci di menu

| Voce | Owner | `admin` | `editor` | `viewer` |
|---|---|---|---|---|
| Dashboard | visibile | visibile (ridotto) | visibile (ridotto) | visibile (ridotto) |
| App catalog | visibile | visibile (sola lettura) | visibile (sola lettura) | visibile (sola lettura) |
| Account | visibile | **assente** | **assente** | **assente** |
| Billing | visibile | **assente** | **assente** | **assente** |
| Members | visibile | **assente** | **assente** | **assente** |
| I miei dati | visibile (completa) | visibile (**ridotta**) | visibile (**ridotta**) | visibile (**ridotta**) |
| Supporto | visibile | visibile | visibile | visibile |
| Applicazioni nel menu | tutte quelle dell'account | solo quelle abilitate | solo quelle abilitate | solo quelle abilitate |

### Comandi

| Comando | Owner | `admin` | `editor` | `viewer` |
|---|---|---|---|---|
| Invitare una persona nuova | visibile | **assente** | **assente** | **assente** |
| Ridurre i posti | visibile | **assente** | **assente** | **assente** |
| Installare un'applicazione | visibile | **assente** | **assente** | **assente** |
| Chiedere all'owner di installare | assente (installa) | visibile | visibile | visibile |
| Gestire il piano | visibile | **assente** | **assente** | **assente** |
| Creare · modificare · eliminare dati dell'applicazione | visibile | visibile | visibile | **sola lettura** |
| Abilitare persone all'applicazione, cambiarne i ruoli | visibile | visibile | **sola lettura** | **sola lettura** |
| Esportare tutto l'account, recedere, chiudere l'account | visibile | **assente** | **assente** | **assente** |
| Correggere il proprio nome, scaricare i propri dati | visibile | visibile | visibile | visibile |

### Selettore dell'account e inviti — non dipendono dal ruolo

| Elemento | Owner | `admin` | `editor` | `viewer` |
|---|---|---|---|---|
| Nome dell'account attivo | visibile | visibile | visibile | visibile |
| Selettore dell'account | **assente** (1 appartenenza) | visibile (2) | **assente** (1) | visibile (2) |
| Inviti ricevuti (nel cruscotto) | assente (nessuno) | visibile (1) | assente | assente |

Nemmeno l'etichetta del ruolo è in queste tabelle, e non per dimenticanza: **non esiste**
un'etichetta di ruolo globale. Il ruolo è per applicazione, quindi una sola etichetta nell'intestazione
sarebbe falsa appena una persona è abilitata a più di una applicazione — «Admin del Mini-CRM, Viewer delle
Note, Editor di Teams…» non è un'informazione, è un elenco. Il ruolo si legge dove è vero: sulla **scheda
dell'applicazione** nel cruscotto e in **testa alle schermate** di quell'applicazione.

Questa tabella si legge in modo diverso dalle altre due: **non è una matrice di permessi**. Il selettore
non compare o non compare in base al *ruolo*, ma in base al **numero di appartenenze** della persona — per
questo il `viewer`, che è il ruolo con meno poteri, ce l'ha, e l'`editor` no. Confondere le due cose in
implementazione produrrebbe un selettore legato al ruolo, cioè sbagliato.

L'ultima riga della tabella dei comandi non è una dimenticanza: i diritti della persona sui **propri** dati sono esenti da ogni
ruolo e da ogni varco (artt. 15, 16 e 20 del Regolamento europeo). Vale anche per un `viewer`.

**La tabella del §4 non elenca l'impalcatura**, e non è una dimenticanza: contiene solo elementi con un
file React di destinazione. Barra di commutazione e riquadro «Cosa cambia per …» non ci sono perché nel
prodotto non ci sono (§0).

Questa matrice vive anche in forma leggibile da un programma nella costante `MATRICE` di
[assets/proto-data.js](assets/proto-data.js): è la stessa specifica, e i prototipi ne discendono. Chi
scrive i collaudi può prenderla da là.

## 6. Che cosa è deliberatamente finto

- **Dati inventati**: l'account «Studio Marchetti», cinque persone, quattro contatti, un portafoglio di
  sei account nella console. Nessuna chiamata di rete, nessuna persistenza: ricaricando si riparte.
- **Interruttore didattico** nel prototipo owner: il comando «Riduci i posti» accende lo stato di
  riduzione in attesa per farlo vedere; nel prodotto quello stato nasce da una scelta di persone e ha
  una data di esecuzione vera.
- **Il cambio di account non è simulato**: scegliere l'altro account spiega cosa fa il prodotto (riscrive
  l'account attivo, rinnova il token, ricarica) e poi porta al prototipo che mostra quell'esperienza —
  `owner.html` per Marta, che nel suo studio è titolare. Simularlo davvero richiederebbe un secondo
  insieme di dati d'esempio, che non insegnerebbe nulla di più: ciò che serve sapere è **dov'è il
  comando**, **che cosa accade** e che l'appartenenza viene **riverificata** al rinnovo.
- **Un solo tema (chiaro)** e una sola lingua (italiano). Il prodotto ne ha due e cinque: le storie lo
  richiedono, i prototipi non lo mostrano.
- **Il numero dei posti è fisso** (cinque). Nella console, invece, il calcolo è **vero e a scaglioni**:
  cambiando una tariffa l'anteprima ricalcola con la regola di
  [UC 0102](../story/0102-listino-posti-a-fasce.md), ed è verificato che dia gli stessi numeri della sua
  tabella — 3 → 0,00 · 4 → 2,99 · 10 → 20,93 · 11 → 22,92 · 52 → 102,51 · 120 → 159,83 €.
- **Fasce: sono quattro, più la franchigia.** La richiesta iniziale parlava di «tre fasce», ma le
  tariffe date sono quattro (2,99 · 1,99 · 0,99 · 0,49) oltre ai tre posti gratuiti. Il prototipo
  mostra quello che i requisiti dicono.
- **Il menu è quello vero**, comprese le voci che questa epica **non** cambia: «Impostazioni» nel piede
  e il menu della persona con «Sicurezza» e «Esci». Sono nel prototipo di proposito — un prototipo che le
  omette invita a perderle in implementazione, e sono preferenze **della persona**, quindi restano visibili
  a ogni ruolo. Reso semplificato: nel prodotto i gruppi delle applicazioni si aprono e chiudono, qui sono
  sempre aperti.
- **Le icone arrivano da rete** (Material Symbols Rounded da Google Fonts) mentre nel prodotto vengono dal
  pacchetto `material-symbols`: stesso font, stessi nomi, provenienza diversa. Senza collegamento il
  prototipo mostra i nomi delle icone al posto dei simboli — resta leggibile, ma è brutto.

## 7. Tre cose che il prototipo insegna e che a parole si perdono

**La prima**: il listino a **scaglioni progressivi** si racconta con *due* numeri, non con uno. Il totale
cresce sempre (10 posti → 20,93 €, 11 → 22,92 €), mentre a scendere è il **costo del posto successivo**
(2,99 € fino al decimo, 1,99 € dall'undicesimo). L'interfaccia deve mostrarli entrambi: il totale senza i
due numeri sembra solo «più caro», e il costo marginale senza il totale non dice quanto si spende.

Il modello precedente — tariffa unica di fascia su tutti i posti a pagamento — dava un solo numero ma
faceva **scendere il totale** ai confini: undici posti costavano meno di dieci. Aritmeticamente corretto,
commercialmente indifendibile: un prezzo che cala quando cresci sembra un errore di conteggio, anche
quando è a favore del cliente. Nella console la tabella dei casi tipici include ora proprio i confini
(4, 10, 11, 52, 120), perché è lì che un difetto di calcolo si vedrebbe.

**La seconda**: la differenza fra `admin` e owner è **economica**, non gerarchica. L'`admin` amministra
le persone dentro un'applicazione ma non può farne entrare di nuove, perché quello costa all'account.
Nel prototipo `admin.html` questa differenza è l'unica cosa che distingue la sua schermata «Utenti» da
quella dell'owner, ed è il cuore del modello.

**La terza**: il selettore dell'account **non esiste** per chi appartiene a un solo account — che oggi
sono tutti. Aprendo `editor.html` e `admin.html` una accanto all'altra si vede la differenza, ed è la
ragione per cui la regola va scritta come «non renderlo» e non come «renderlo disabilitato»: un comando
che non serve a nulla è rumore per il cento per cento delle persone, a beneficio di una minoranza.
