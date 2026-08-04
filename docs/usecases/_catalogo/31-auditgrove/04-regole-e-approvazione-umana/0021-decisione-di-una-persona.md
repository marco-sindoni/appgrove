# 0021 — Decisione di una persona

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 04 — Regole e approvazione umana
**Storia**: `0021` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0020`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona a cui è stato chiesto di approvare
> voglio vedere in modo comprensibile che cosa un agente sta per fare, e poter dire sì o no scrivendo perché
> così da assumermi consapevolmente una responsabilità, invece di premere un pulsante senza sapere su cosa.

**Contesto.** È la storia da cui dipende il valore commerciale dell'intera applicazione. La scheda di catalogo lo
dice senza giri di parole — *abbandona se i team registrano ma non usano le approvazioni* — e l'analisi lo conferma:
la sola registrazione ha poca disponibilità a pagare (§11 della descrizione dell'applicazione). L'approvazione è il
momento in cui il prodotto smette di essere un archivio e diventa un presidio.

Il problema di progetto è uno solo, e non è tecnico: **la persona che riceve la richiesta deve capire in dieci
secondi che cosa sta per succedere**. Se la scheda mostra un nome tecnico di strumento e un elenco di impronte,
chi decide approverà tutto per stanchezza — e un'approvazione data senza guardare è peggio di nessuna
approvazione, perché produce una firma su una responsabilità che nessuno si è assunto davvero.

C'è una tensione, e va vista in faccia: la scheda è più comprensibile se mostra i valori dei parametri, ma per
impostazione predefinita **quei valori non li abbiamo** (§6.3 della descrizione dell'applicazione). La scelta di
questa storia è di **non tradire la minimizzazione per la comodità**: si mostra la forma, l'effetto atteso e
l'impronta; i valori si vedono solo per gli strumenti su cui il cliente ha esplicitamente attivato la
conservazione dei contenuti (storia 0031).

## 2. Requisiti funzionali

1. **RF-1** — Esiste una schermata «Approvazioni» che elenca i nulla osta in attesa dell'account, ordinati per
   urgenza (scadenza più vicina prima), con strumento, sorgente, chi ha chiesto, classe di effetto e tempo
   rimanente.
2. **RF-2** — La scheda di una richiesta mostra: chi ha chiesto l'azione, quale agente, quale strumento, la
   **forma dei parametri** (nomi, tipi, lunghezze), l'effetto atteso dichiarato, e i **valori solo se conservati**
   per quello strumento — con l'indicazione esplicita quando non lo sono.
3. **RF-3** — Chi decide sceglie **approva** o **nega** e deve scrivere un **motivo**: il campo è obbligatorio in
   entrambi i casi, perché il motivo di un sì vale quanto quello di un no quando si guarderà indietro.
4. **RF-4** — La decisione richiede un ruolo adeguato; **chi ha solo la lettura vede la coda ma non decide**.
5. **RF-5** — La decisione è immediatamente visibile alla sorgente che sta aspettando (storia 0020), ed è una riga
   del registro con chi ha deciso, quando, con quale motivo.
6. **RF-6** — Chi deve approvare riceve un **avviso dentro l'applicazione** e un **avviso per posta elettronica**;
   la frequenza degli avvisi per posta è configurabile per non diventare rumore.
7. **RF-7** — Una richiesta già decisa non si decide una seconda volta: chi arriva dopo vede la decisione presa,
   da chi e quando.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La coda mostra esclusivamente i nulla osta dell'account ricavato dal
  token verificato; un identificativo di nulla osta di un altro account restituisce la stessa risposta di un
  identificativo inesistente. Ogni scrittura della decisione filtra per `tenant_id`.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/agentaudit/v1/clearances?status=pending`
  (coda paginata) e `POST /api/agentaudit/v1/clearances/{id}/decision` (decisione, con motivo obbligatorio);
  corpo validato; errori in `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__decisioni.sql` sullo schema `app_agentaudit`: colonne di
  decisione, decisore, momento e motivo sulla tabella dei nulla osta, oppure tabella `clearance_decisions`
  separata **in sola aggiunta** — è la forma da preferire, perché una decisione è una prova e non deve stare in
  una riga aggiornabile. `tenant_id`, chiave primaria UUID versione 7, colonne di controllo.
- **RT-4 — Modulo frontend (§3, §5).** Nuova sezione «Approvazioni» nel manifesto del modulo `agentaudit`, con
  indicatore del numero di richieste in attesa nella barra laterale; dati letti con il client generato; solo token
  del sistema di design; funziona in tema chiaro e scuro. La conferma di una decisione passa da una finestra
  modale che ripete l'effetto atteso: è l'ultimo punto in cui una persona può accorgersi di stare approvando la
  cosa sbagliata.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono presenti
  in `en, it, fr, es, de`, **compreso il testo dell'avviso per posta elettronica**. È il testo che porta una
  persona a interrompere quello che sta facendo: se è oscuro, la richiesta resta in coda e scade (storia 0022).
- **RT-6 — Varchi e quota (§6, §7).** La decisione **non consuma** la metrica `actions`: la richiesta ha già
  consumato la propria unità alla storia 0020, e far pagare anche l'approvazione metterebbe un prezzo sul
  comportamento virtuoso. La decisione richiede il ruolo adeguato (`403` altrimenti). Con abbonamento in
  `past_due` la funzione resta accessibile — sospendere le approvazioni per un pagamento in ritardo bloccherebbe
  il lavoro del cliente; con `canceled` risponde `402`.
- **RT-7 — Esposizione conversazionale (§12).** `elenca_approvazioni_in_attesa(sorgente?) → elenco di nulla osta
  pendenti` è marcato **lettura** ed è esposto. `nega_azione(id, motivo) → bozza di rifiuto` è marcato
  **scrittura** con **conferma umana obbligatoria**. **`approva_azione` non è esposto e non lo sarà**: se
  l'approvazione si può ottenere chiedendola a un assistente, la catena di responsabilità è finita e il prodotto
  ha smesso di funzionare (§7 della descrizione dell'applicazione). Il contratto vive dentro il servizio; il
  server conversazionale è di piattaforma e non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Introduce **due voci nuove** nel manifesto
  `docs/compliance/manifests/agentaudit.yaml`, in italiano e inglese: l'**identificativo di chi ha deciso**
  (finalità: dimostrare che una persona ha approvato; base giuridica: esecuzione del contratto) e il **motivo
  scritto**, che è **testo libero** e quindi un ingresso non presidiato — di rischio basso, perché lo scrive una
  persona consapevole in un campo breve, ma va dichiarato come tale. Entrambi i campi vanno annotati
  `@PersonalData` e la tabella delle decisioni va aggiunta a `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `richiesta presa in carico`, `decisione registrata` e `avviso
  di approvazione inviato` sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione,
  **senza il testo del motivo** — che è contenuto e non metadato.

## 4. Criteri di accettazione

**CA-1 — L'approvazione sblocca l'agente**
- **Dato** un nulla osta in attesa e una sorgente che sta interrogando periodicamente
- **Quando** una persona con ruolo adeguato approva scrivendo il motivo
- **Allora** la sorgente riceve `concesso` alla prima interrogazione successiva, e nel registro compare la riga
  della decisione con chi, quando e il motivo

**CA-2 — Il motivo è obbligatorio**
- **Dato** una richiesta in attesa
- **Quando** una persona tenta di approvarla senza scrivere il motivo
- **Allora** la decisione viene rifiutata con un errore di validazione, e la richiesta resta in attesa

**CA-3 — I valori non conservati non si inventano**
- **Dato** una richiesta relativa a uno strumento per cui la conservazione dei contenuti non è attiva
- **Quando** una persona apre la scheda della richiesta
- **Allora** vede la forma dei parametri e l'impronta, con l'indicazione esplicita che i valori non sono
  conservati, e non vede nessun valore

**CA-4 — Chi ha solo la lettura non decide**
- **Dato** un utente con il solo permesso di lettura
- **Quando** apre la coda e tenta di decidere una richiesta
- **Allora** vede la coda e le schede, ma la decisione riceve `403` e la richiesta resta in attesa

**CA-5 — Non si decide due volte**
- **Dato** una richiesta già approvata da una persona
- **Quando** una seconda persona tenta di negarla
- **Allora** l'operazione viene rifiutata, viene mostrata la decisione già presa con autore e momento, e nulla
  cambia

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con richieste in attesa
- **Quando** un utente di `A` apre la coda e tenta di decidere una richiesta di `B` usandone l'identificativo
- **Allora** vede solo le proprie richieste e la decisione sulla richiesta altrui riceve la stessa risposta di un
  identificativo inesistente

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sull'obbligatorietà del motivo e sulla non ripetibilità della decisione, e di
      **integrazione** sul percorso richiesta → attesa → decisione → sblocco, con database effimero e migrazioni
      vere;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulla decisione: chi legge non decide, chi
      decide non amministra le regole se non ne ha il ruolo;
- [ ] **prova end-to-end**: risposta «coprire ora» — è il passo centrale del percorso `[J-AGENTAUDIT]`: una
      sorgente chiede un nulla osta su uno strumento con regola `richiedi approvazione`, una persona apre
      «Approvazioni», approva con motivo, la sorgente riceve `concesso`; il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) viene aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), compreso l'avviso per posta
      elettronica;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con decisore e motivo scritto, campi annotati, e
      tabella presente in esportazione e cancellazione;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con **due voci obbligatorie**: il
      motivo obbligatorio anche sull'approvazione, e la scelta di non mostrare i valori non conservati;
- [ ] contratto degli **strumenti conversazionali**: `elenca_approvazioni_in_attesa` in lettura, `nega_azione` in
      scrittura con conferma, e il divieto permanente su `approva_azione` motivato;
- [ ] controllo automatico di **accessibilità** sulla schermata «Approvazioni»;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0020` | Senza nulla osta in attesa non c'è niente da decidere |
| storia `0003` | Serve il guscio del modulo frontend per appendere la sezione «Approvazioni» |
| Servizio di invio di posta elettronica della piattaforma | L'avviso per posta è ciò che porta la persona a guardare la coda; in locale è sempre simulato |

## 7. Fuori ambito

- **l'avviso su messaggistica di squadra (Slack, Teams)**: deliberatamente escluso dal perimetro iniziale.
  Introdurrebbe un fornitore esterno che tratterebbe dati per nostro conto e che riceverebbe **la parte più
  delicata dell'intera applicazione** — il testo di ciò che un agente sta per fare (§2.4 della descrizione
  dell'applicazione). È anche la funzione che il concorrente con listino pubblico mette in evidenza nel proprio
  piano a pagamento, quindi la mancanza si sentirà: è un punto aperto per lo sviluppatore, non una svista;
- la scadenza della richiesta non decisa: storia 0022;
- la verifica che l'agente abbia poi fatto ciò che gli era stato concesso: storia 0023;
- l'approvazione delegata o a più firme: fuori, vedi punti aperti.

## 8. Punti aperti

- ⚠️ **Avviso su messaggistica di squadra.** Un'approvazione urgente che arriva per posta elettronica alle 23 di
  sabato non viene vista. I concorrenti risolvono con Slack; noi non possiamo farlo senza aprire un fornitore
  esterno con accesso a materiale delicato. Chi chiude: sviluppatore (§2.4 e §11, punto 12 della descrizione
  dell'applicazione).
- **Chi deve essere avvisato.** Tutti quelli che hanno il ruolo, oppure una persona designata per classe di
  effetto? Avvisare tutti produce rumore e diffusione di responsabilità; designare qualcuno crea un punto singolo
  di guasto quando quella persona è in ferie. Chi chiude: sviluppatore.
- **Approvazione a più firme per le azioni gravissime.** Sopra una certa soglia potrebbe servire il consenso di
  due persone. È una funzione che il segmento micro probabilmente non userà — in un'azienda di tre persone la
  seconda firma è teatro — ma che il segmento piccolo chiederà. Proprietaria naturale: una storia futura
  dell'epica 04. Chi chiude: sviluppatore.
- **Il rischio di approvare per stanchezza.** È il rischio d'uso più serio di questa storia e non si risolve con
  una funzione: si attenua tenendo le richieste poche (regole ben scritte, storia 0019) e la scheda leggibile. Se
  i dati d'uso mostrassero approvazioni sistematicamente immediate, sarebbe un segnale da guardare, non da
  ignorare.
