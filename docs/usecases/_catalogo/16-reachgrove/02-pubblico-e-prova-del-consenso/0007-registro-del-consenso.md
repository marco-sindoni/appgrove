# 0007 — Registro del consenso

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 02 — Pubblico e prova del consenso
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che un giorno potrebbe dover dimostrare a un'autorità che quell'invio era lecito
> voglio che ogni «sì» e ogni «no» delle persone iscritte resti scritto con la data, il canale, la base giuridica
> e il testo che hanno letto
> così da poter produrre la prova invece di doverla raccontare.

**Contesto.** È la storia che giustifica l'esistenza di questa app. La legge chiede un consenso preventivo,
libero, specifico, informato, **documentato** e revocabile
([application-description.md](../application-description.md) §2.3 punto 1), e in un accertamento la prova la deve
produrre chi ha inviato: la responsabilità non si scarica su chi ha fornito la lista (§2.3 punto 4). Il Garante
italiano ha sanzionato con 45.000 € una società proprio per campagne di posta elettronica senza consenso
dimostrabile. Un file modificabile può non bastare in un procedimento; una colonna che si può sovrascrivere non
vale niente. Da qui la forma di questa storia: un registro **ad accrescimento**, in cui si aggiunge e non si
corregge, e da cui si **deriva** lo stato di contattabilità che la storia 0006 mostra.

## 2. Requisiti funzionali

1. **RF-1** — Una registrazione di consenso porta: canale (posta elettronica, messaggi brevi, messaggistica),
   esito (`concesso` oppure `revocato`), **base giuridica**, momento, testo esatto letto e accettato, origine
   della prova, e — quando esiste — indirizzo di rete e momento della conferma.
2. **RF-2** — Le basi giuridiche ammesse sono un **elenco chiuso** di due valori: `consenso` e `soft spam`. Il
   `soft spam` è selezionabile **solo** sul canale della posta elettronica e obbliga a indicare a quale vendita si
   riferisce; sui canali dei messaggi brevi e della messaggistica non compare proprio nell'elenco.
3. **RF-3** — Le origini della prova ammesse sono un elenco chiuso: `modulo pubblico`, `doppia conferma`,
   `dichiarazione dell'operatore`, `importazione con prova allegata`, `proiezione da un'altra app della suite`.
4. **RF-4** — Una registrazione **non si modifica e non si cancella**: non esistono rotte di aggiornamento né di
   eliminazione. Una revoca è una registrazione nuova con esito `revocato`, che non tocca la precedente.
5. **RF-5** — Lo stato di contattabilità per canale è **derivato** dall'ultima registrazione per quel canale, con
   la soppressione (storia 0011) che ha comunque la precedenza. È l'unica regola, e la usano l'anagrafica, il
   controllo pre-volo e la spedizione.
6. **RF-6** — La scheda dell'iscritto mostra lo storico completo delle registrazioni in ordine di tempo, e per
   ciascuna il testo accettato **così com'era allora**, non la versione corrente del testo del modulo.
7. **RF-7** — Quando l'utente registra una `dichiarazione dell'operatore`, l'interfaccia gli chiede di scrivere da
   dove viene quel consenso e gli ricorda in una riga che sta facendo una dichiarazione con valore probatorio, di
   cui risponde lui.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `consent_record` filtra per `tenant_id`
  preso dal token verificato; un `tenant_id` fornito dall'esterno viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `GET|POST /api/campaigns/v1/subscribers/{id}/consents`. **Nessun** `PATCH`, **nessun** `DELETE`: sono assenti
  dalla definizione delle interfacce, non solo negati a runtime. Errori in `application/problem+json`; definizione
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `consent_record` sullo schema `app_campaigns`, con `tenant_id`, chiave
  primaria UUID versione 7 e colonne di controllo; indice che recupera l'ultima registrazione per (iscritto,
  canale). La cancellazione logica su questa tabella si usa **solo** per l'esercizio dei diritti dell'interessato,
  mai per correggere una registrazione. Il testo accettato si conserva **per copia**, non per riferimento al
  modulo: se il cliente cambia il testo del modulo domani, la prova di ieri non deve cambiare con lui.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Consensi» dentro la scheda dell'iscritto: stato attuale per
  canale, storico, modulo di registrazione con l'elenco chiuso delle basi giuridiche. Solo token del sistema di
  design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe dell'interfaccia in `en, it, fr, es, de`. Attenzione: il
  **testo accettato** dall'iscritto resta nella lingua in cui è stato accettato e **non si traduce mai** — è
  prova, non interfaccia. È la stessa regola della storia 0011 dell'app 04 LeadGrove.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo della metrica `messages_sent`: registrare un consenso non è
  inviare. Registrare richiede il ruolo `owner`, `admin` o `member`; con abbonamento `canceled` la sezione
  risponde `402`, ma l'esportazione dei dati resta accessibile in ogni caso.
- **RT-7 — Esposizione conversazionale (§12).** `registra_consenso` **non è esposto** alla chat, per scelta
  dichiarata: è una dichiarazione con valore probatorio e dev'essere un atto compiuto da una persona
  nell'interfaccia, dove vede esattamente cosa sta dichiarando (§7 della descrizione). Lo strumento di **lettura**
  `stato_iscritto` legge questa derivazione e cita la registrazione che la determina.
- **RT-8 — Dati personali (§10).** Voce `consent.record` del manifesto in italiano e inglese: finalità
  «dimostrare la liceità dell'invio», base giuridica «obbligo di dimostrabilità in capo al titolare». Campi
  annotati `@PersonalData`, tabella in `exportData` e `purgeData`. Nota di conformità da scrivere nel manifesto:
  **la prova sopravvive alla revoca** — serve a dimostrare che l'invio di ieri era lecito — e viene meno solo con
  la cancellazione dei dati dell'interessato.
- **RT-9 — Registrazione eventi (§14).** «Consenso registrato» e «consenso revocato» con `tenant_id`, `app_id`,
  `user_id`, identificativo dell'iscritto, canale, base giuridica e origine; **mai** il recapito, **mai** il testo
  accettato.

## 4. Criteri di accettazione

**CA-1 — La registrazione rende contattabile**
- **Dato** un iscritto `in quarantena` senza registrazioni
- **Quando** si registra «posta elettronica, esito concesso, base giuridica consenso, origine dichiarazione
  dell'operatore» con il testo accettato
- **Allora** lo stato del canale della posta elettronica diventa `attivo`, con il momento e la base giuridica, e la
  registrazione compare nello storico

**CA-2 — La revoca non cancella la prova**
- **Dato** un iscritto con un consenso registrato tre mesi fa
- **Quando** si registra la revoca
- **Allora** lo stato diventa `disiscritto` e la registrazione di tre mesi fa **è ancora presente**, integra, con
  il suo testo accettato

**CA-3 — Una registrazione non si riscrive**
- **Dato** una registrazione esistente
- **Quando** si tenta di aggiornarla o di eliminarla
- **Allora** il servizio risponde `405`, perché quelle operazioni non esistono nella definizione delle interfacce

**CA-4 — Il soft spam non esce dalla posta elettronica**
- **Dato** un iscritto con un numero di telefono
- **Quando** si tenta di registrare «messaggi brevi, base giuridica soft spam»
- **Allora** il servizio risponde `400` con un messaggio che spiega che quell'eccezione riguarda solo la posta
  elettronica, e nell'interfaccia quel valore non era nemmeno selezionabile

**CA-5 — La prova non segue il testo del modulo**
- **Dato** una registrazione raccolta con un certo testo, e il cliente che poi cambia il testo del proprio modulo
- **Quando** si riapre lo storico
- **Allora** la registrazione mostra ancora il testo di allora, parola per parola

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B` con propri iscritti
- **Quando** un utente di `A` chiede le registrazioni di un iscritto di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla derivazione dello stato dallo storico (compresi i casi di registrazioni multiple
      nello stesso istante e della precedenza della soppressione) e di **integrazione** sulla risorsa, con la
      verifica che aggiornamento ed eliminazione non esistano;
- [ ] prova di **isolamento fra account** sulle registrazioni di consenso;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-CAMPAIGNS]` (storia 0037) include la registrazione di un
      consenso e la sua revoca, perché è il passo che distingue questa app da un mandaposta; voce aggiunta al
      registro di copertura [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** in tutte e cinque le lingue, con i testi accettati esclusi dalla traduzione;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `consent_record`, con la nota sulla
      sopravvivenza della prova alla revoca, campi annotati e tabella in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato perché il registro è ad accrescimento, perché le basi
      giuridiche sono un elenco chiuso e perché il testo accettato si copia invece di riferirlo;
- [ ] contratto degli **strumenti conversazionali**: `registra_consenso` **non** esposto, con la motivazione
      scritta nel contratto stesso;
- [ ] controllo automatico di **accessibilità** verde sulla sezione dei consensi;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0006` | Una registrazione sta su un iscritto |
| Conferma della classificazione dei dati personali (§6 della descrizione) | La base giuridica della prova del consenso e il suo termine di conservazione sono materia da validare, non da decidere qui |

## 7. Fuori ambito

- la raccolta del consenso dal modulo pubblico e la doppia conferma: sono le storie 0009 e 0008, che usano questa
  struttura;
- l'importazione di prove allegate a un file: è la storia 0010;
- la soppressione: è la storia 0011 e ha una precedenza che questa storia rispetta ma non implementa;
- la **campagna di ri-richiesta del consenso**: deliberatamente non implementata, perché quel messaggio è a sua
  volta una comunicazione senza consenso e l'orientamento non è pacifico (§2.7 della descrizione).

## 8. Punti aperti

- **Termine di conservazione della prova.** Non ho trovato un termine di legge: la proposta del manifesto è
  10 anni dall'ultimo invio ([application-description.md](../application-description.md) §6). È una scelta del
  titolare, da dichiarare e motivare: chiude lo sviluppatore con la revisione legale.
- **Confine di responsabilità sulla dichiarazione dell'operatore.** L'app registra ciò che il cliente dichiara ma
  non lo può verificare. Dove finisce la nostra responsabilità di responsabile del trattamento va scritto nel
  contratto di trattamento, non solo nel manifesto. Chiude la revisione legale.
- **Consenso arrivato per proiezione dall'app 04 LeadGrove.** Il contratto degli eventi dell'anagrafica condivisa
  non esiste ancora nel repository (§11.5 della descrizione): finché non esiste, l'origine
  `proiezione da un'altra app della suite` resta dichiarata nell'elenco chiuso ma senza produttore, e un contatto
  che arrivasse senza la sua prova entra comunque in quarantena (storia 0010).
