# 0010 — Consenso e disiscrizione

**Applicazione**: 05 — ChatGrove (`chat_commerce`) · **Epica**: 02 — Canale di messaggistica e conformità degli invii
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un negozio
> voglio sapere a chi posso mandare offerte e a chi no, e che chi mi dice di smettere smetta di ricevere subito
> così da non farmi sospendere il numero e da non infastidire i miei clienti.

**Contesto.** Le condizioni del canale richiedono che il destinatario abbia dato il proprio consenso agli
invii promozionali, e chi manda messaggi sgraditi vede il proprio punteggio di qualità scendere fino alla
sospensione dei modelli. In Europa si aggiunge la disciplina delle comunicazioni indesiderate. Il negozio non
conosce nessuna di queste regole: l'app deve applicarle al posto suo, e soprattutto deve **conservare la
prova** di come il consenso è stato raccolto — la prova serve dopo, quando qualcuno contesta.

## 2. Requisiti funzionali

1. **RF-1** — Ogni contatto porta uno stato di consenso agli invii promozionali: `non_richiesto`, `dato`,
   `revocato`, con **origine** (ha scritto per primo, modulo, importazione, richiesta esplicita in chat) e
   **data**.
2. **RF-2** — Il consenso si può registrare dalla scheda del contatto indicandone l'origine; l'app non lo
   presume mai da sola.
3. **RF-3** — Un messaggio in arrivo che contiene una parola di rifiuto riconosciuta (in una lista per lingua,
   modificabile dal negozio) porta il contatto a `revocato` **immediatamente**.
4. **RF-4** — Il contatto `revocato` **non** riceve modelli di categoria marketing: il tentativo è bloccato dal
   servizio, non solo nascosto dall'interfaccia. Restano possibili i messaggi di servizio e i modelli di
   utilità legati a un ordine in corso.
5. **RF-5** — La revoca si può registrare anche a mano dalla scheda del contatto, e si può ripristinare solo
   registrando un consenso nuovo con la sua origine.
6. **RF-6** — Lo storico dei cambi di stato del consenso è conservato e leggibile: chi, quando, da cosa a cosa,
   con quale origine.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il consenso è per (account, contatto): ogni lettura e scrittura
  filtra per `tenant_id` preso dal token verificato. La revoca in un account **non** si propaga a un altro: la
  stessa persona può aver dato il consenso a un negozio e non a un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte
  `PUT /api/chat_commerce/v1/contacts/{id}/consent` e `GET .../consent/history`; corpo validato (l'origine è
  obbligatoria quando si registra un consenso); errori in `application/problem+json`; definizione OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V5__consenso.sql`: colonne di consenso su `contact` e tabella
  `contact_consent_event` con `tenant_id`, chiave primaria UUID versione 7 e colonne di controllo. Lo storico
  del consenso è **prova**: non si cancella logicamente quando il contatto viene modificato.
- **RT-4 — Dati personali (§10).** Voci nuove nel manifesto in italiano e inglese per `contact.consent`
  (stato, origine, data) e per lo storico: sono dati personali a tutti gli effetti, la cui base giuridica è
  l'obbligo di dimostrare la liceità dell'invio. Tabella `contact_consent_event` aggiunta a `exportData` e
  `purgeData`. **Attenzione**: la cancellazione dell'interessato cancella anche la prova del suo consenso — è
  corretto e va scritto nel manifesto, non aggirato.
- **RT-5 — Modulo frontend (§3, §4, §5).** Lo stato del consenso è visibile nella scheda del contatto e come
  segno nell'elenco; le parole di rifiuto si gestiscono nelle Impostazioni; tutte le stringhe in
  `en, it, fr, es, de`.
- **RT-6 — Registrazione eventi (§14).** `consenso registrato`, `consenso revocato`, `invio bloccato per
  consenso mancante`, con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** il
  numero di telefono e senza il testo del messaggio di rifiuto.

## 4. Criteri di accettazione

**CA-1 — Revoca automatica**
- **Dato** un contatto con consenso `dato` e la parola «STOP» nella lista delle parole di rifiuto
- **Quando** quel contatto invia un messaggio contenente «STOP»
- **Allora** il suo consenso passa a `revocato` con origine «richiesta in chat» e la data del messaggio

**CA-2 — Blocco dell'invio promozionale**
- **Dato** un contatto con consenso `revocato`
- **Quando** si tenta di inviargli un modello di categoria marketing
- **Allora** la risposta è `409` con la spiegazione, **nulla parte** e la quota non viene consumata

**CA-3 — Il servizio resta possibile**
- **Dato** lo stesso contatto `revocato`, con un ordine in corso
- **Quando** si invia il modello di utilità «il tuo ordine è pronto»
- **Allora** il messaggio parte: la revoca riguarda gli invii promozionali, non le comunicazioni sull'ordine

**CA-4 — Il consenso non si presume**
- **Dato** un contatto che ha semplicemente scritto al negozio
- **Quando** si legge la sua scheda
- **Allora** il consenso risulta `non_richiesto`, non `dato`

**CA-5 — Isolamento fra account**
- **Dato** la stessa persona come contatto in due account `A` e `B`, con consenso dato solo ad `A`
- **Quando** `B` tenta un invio promozionale
- **Allora** è bloccato in `B` e resta possibile in `A`

**CA-6 — Storico consultabile**
- **Dato** un contatto che ha dato e poi revocato il consenso
- **Quando** si apre lo storico · **Allora** si vedono i due eventi con data, origine e autore

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend e compliance; l'intera suite prima del commit);
- [ ] prove di **unità** sul riconoscimento delle parole di rifiuto e di **integrazione** sul blocco degli
      invii;
- [ ] prova di **isolamento fra account** sul consenso e sul suo storico;
- [ ] **prova end-to-end**: *rimando* alla storia `0029`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue; le **parole di rifiuto** predefinite sono fornite
      almeno per le lingue dell'interfaccia e restano modificabili dal negozio;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con le voci del consenso, campi annotati
      `@PersonalData`, tabella dello storico in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con la scelta di non presumere mai il consenso e la distinzione
      fra invii promozionali e comunicazioni di servizio;
- [ ] contratto degli **strumenti conversazionali**: gli strumenti di invio verificano il consenso **prima**
      di produrre la bozza, così che l'assistente non proponga un invio vietato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0002` | Il consenso è una proprietà del contatto |
| `0009` | Serve la categoria del modello per sapere che cosa bloccare |

## 7. Fuori ambito

- la raccolta del consenso tramite un modulo pubblico sul sito del negozio: sarebbe una superficie esposta
  verso l'esterno, con le sue difese; è una storia futura;
- l'informativa che il negozio deve dare ai propri clienti: è un testo, non una funzione, e appartiene alla
  documentazione dell'app.

## 8. Punti aperti

- **Se il consenso vada richiesto attivamente in chat** al primo contatto (con un messaggio automatico) è una
  scelta di prodotto con effetti sull'esperienza del cliente finale: la propongo, non la decido.
- **Quale base giuridica** valga per gli invii promozionali nei diversi mercati (consenso oppure legittimo
  interesse verso clienti esistenti) cambia per paese: rientra nella revisione legale, non in questa storia.
