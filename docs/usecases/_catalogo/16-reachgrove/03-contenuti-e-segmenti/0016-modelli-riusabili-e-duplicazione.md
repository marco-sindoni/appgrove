# 0016 — Modelli riusabili e duplicazione

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 03 — Contenuti e segmenti
**Storia**: `0016` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0014`, `0015`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che manda da due a otto messaggi al mese
> voglio ripartire da qualcosa che ho già fatto invece che dal foglio bianco
> così da preparare la comunicazione del mese in dieci minuti anziché in un'ora.

**Contesto.** Chi manda con regolarità manda **lo stesso messaggio con parole diverse**: stessa intestazione, stesso
piè di pagina, stessa struttura. Senza il riuso, ogni mese si rifà tutto e ogni mese si sbaglia qualcosa di diverso.
La cosa da fare bene non è la copia in sé, è **cosa la copia non porta con sé**: il duplicato di una campagna
conclusa non deve ereditare né l'esito della verifica né i destinatari già serviti, altrimenti si costruisce
l'incidente più grave possibile in questa app — una campagna che riparte e riscrive a chi ha già ricevuto, magari a
chi nel frattempo si è disiscritto. Per questo la copia riparte **sempre** dallo stato `bozza`, in fondo alla
macchina a stati descritta al §4 della [descrizione dell'applicazione](../application-description.md).

## 2. Requisiti funzionali

1. **RF-1** — Un messaggio si salva come **modello riusabile** con un nome e una descrizione breve; i modelli
   dell'account si elencano, si cercano per nome e si eliminano (cancellazione logica).
2. **RF-2** — Da un modello si crea un messaggio nuovo: il contenuto viene copiato, il modello resta intatto e le
   modifiche successive al messaggio **non** tornano indietro sul modello.
3. **RF-3** — Una campagna esistente — in qualunque stato, compresa `conclusa` e `bloccata` — si duplica. La copia
   riparte dallo stato `bozza`.
4. **RF-4** — La copia **non eredita**: l'esito del controllo pre-volo, il momento programmato, l'elenco dei
   destinatari già serviti, gli invii, i loro eventi e i rapporti. Eredita il contenuto, il segmento scelto, il
   canale e il mittente, purché siano ancora validi.
5. **RF-5** — Se qualcosa dell'originale non è più valido — il segmento è stato cancellato, il dominio mittente non
   è più verificato, il canale non è più attivo — la copia si crea comunque, con quei riferimenti vuoti e un elenco
   di ciò che va ripristinato prima di poterla mandare in verifica.
6. **RF-6** — Il nome della copia è quello dell'originale con un suffisso riconoscibile, modificabile subito; la
   copia porta il riferimento alla campagna d'origine, così che nel rapporto si capisca da dove viene.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Modelli e campagne si copiano solo dentro lo stesso account: la
  duplicazione legge e scrive filtrando per `tenant_id` dal token verificato, e l'identificativo dell'originale che
  arrivasse dal corpo della richiesta viene risolto **dentro** l'account del chiamante o non è risolto affatto.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/campaigns/v1/templates`,
  `DELETE /api/campaigns/v1/templates/{id}`, `POST /api/campaigns/v1/campaigns/{id}/duplicate`,
  `POST /api/campaigns/v1/templates/{id}/instantiate`. L'operazione di duplicazione è **idempotente rispetto a una
  chiave di richiesta**, così che due clic non producano due copie. Errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** La tabella `message_template` nasce con la storia 0002; qui si aggiunge la
  migrazione `V<N>__campaign_copied_from.sql` con la colonna che tiene il riferimento alla campagna d'origine.
  Tutte le tabelle con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
  La copia **non** copia righe delle tabelle `delivery`, `delivery_event` e `automation_run`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Modelli» del modulo `campaigns` e azione «Duplica» nella scheda
  della campagna, con un riepilogo esplicito di **cosa non viene copiato** mostrato prima di confermare. Solo token
  del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, compreso il riepilogo di ciò che non si eredita,
  passano dallo spazio-nomi `campaigns` in `en, it, fr, es, de`. Nomi dei modelli e contenuti restano del cliente.
- **RT-6 — Varchi e quota (§6, §7).** Duplicare non consuma la metrica `messages_sent` (natura `flow`): la copia è
  una bozza e le bozze non spediscono. Valgono i varchi comuni; con abbonamento `canceled` la risorsa risponde
  `402`.
- **RT-7 — Esposizione conversazionale (§12).** `crea_bozza_di_campagna` (storia 0035) può partire da un modello
  indicandolo per nome: resta uno strumento di **scrittura con conferma umana**, e produce comunque una campagna in
  stato `bozza` che non può partire da sola.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. Va però scritto nel manifesto un punto che sembra un
  dettaglio e non lo è: **la copia non porta con sé i destinatari**, quindi non duplica dati di persone. La tabella
  `message_template` entra in `exportData` e `purgeData` perché contiene contenuti del cliente.
- **RT-9 — Registrazione eventi (§14).** «Modello creato», «campagna duplicata» con l'identificativo dell'origine e
  della copia, `tenant_id`, `app_id`, `user_id` e identificativo di correlazione; mai nomi né contenuti.

## 4. Criteri di accettazione

**CA-1 — La copia riparte da bozza**
- **Dato** una campagna in stato `conclusa`, con 900 invii e il pre-volo superato
- **Quando** l'utente la duplica
- **Allora** la copia è in stato `bozza`, senza esito di verifica, senza momento programmato e con zero invii

**CA-2 — I destinatari già serviti non si ereditano**
- **Dato** la stessa campagna conclusa
- **Quando** si guarda la copia
- **Allora** non esiste nessun invio collegato e nessun evento di recapito: l'elenco dei destinatari si ricalcolerà
  dal segmento al momento della spedizione

**CA-3 — Riferimenti non più validi**
- **Dato** una campagna il cui segmento è stato cancellato dopo l'invio
- **Quando** la si duplica
- **Allora** la copia si crea con il segmento vuoto e mostra l'elenco di ciò che va scelto prima di poterla mandare
  in verifica

**CA-4 — Il modello non cambia**
- **Dato** un messaggio creato da un modello
- **Quando** l'utente ne modifica il testo
- **Allora** il modello resta com'era, e gli altri messaggi creati da quel modello non cambiano

**CA-5 — Doppio clic, una copia sola**
- **Dato** una richiesta di duplicazione inviata due volte con la stessa chiave di richiesta
- **Quando** entrambe arrivano al servizio
- **Allora** esiste **una** copia sola, e la seconda risposta indica la stessa copia

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede di duplicare una campagna di `B`
- **Allora** riceve `404` e nessuna copia viene creata

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** su cosa la copia eredita e cosa no, e di **integrazione** sulla duplicazione, compresa
      l'idempotenza, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su duplicazione e modelli;
- [ ] **prova end-to-end**: nessun impatto — il percorso `[J-CAMPAIGNS]` (storia 0037) copre la catena consenso →
      campagna → invio → disiscrizione e non passa dalla duplicazione; la regola «la copia riparte da bozza» è
      coperta dalle prove d'integrazione;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `message_template`, con la nota che la copia non
      duplica destinatari;
- [ ] **registro delle decisioni** compilato, con annotato perché la copia non eredita lo stato di verifica;
- [ ] contratto degli **strumenti conversazionali**: partenza da modello dentro `crea_bozza_di_campagna`, con
      conferma umana;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0014` | Si copiano messaggi: prima devono esistere |
| Storia `0015` | La copia porta con sé i campi variabili e i loro valori di ripiego |
| Storia `0013` | La copia eredita il segmento scelto, se esiste ancora |

## 7. Fuori ambito

- una raccolta di modelli predefiniti forniti da noi: sarebbe contenuto editoriale da mantenere in cinque lingue,
  e non è lavoro di questa storia;
- la condivisione di modelli fra account diversi: fuori perimetro, sarebbe una superficie fra account che
  l'isolamento non prevede;
- il versionamento dei modelli con storico delle modifiche: rimandato, nessuno lo ha chiesto;
- la ripetizione di una campagna verso i soli destinatari che non hanno aperto: dipende dalla misurazione
  (storia 0029) ed è una funzione diversa dalla duplicazione.

## 8. Punti aperti

- **Se la copia debba poter ereditare il momento programmato spostato in avanti.** È comodo per le comunicazioni
  ricorrenti; è anche il modo più rapido per far partire una campagna senza riguardarla. Proposta: no, e chi manda
  ogni mese lo stesso messaggio usa un percorso automatico (epica 05). Chiude lo sviluppatore.
