# 0020 — Collegamento in sola lettura al fornitore

**Applicazione**: 19 — SubGrove (`abbonati`) · **Epica**: 04 — Incassi e solleciti
**Storia**: `0020` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore · ⚠️ **fermata di escalation**
**Dipende da**: `0018`, `0019`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che incassa già con il proprio conto presso un fornitore di pagamento
> voglio che SubGrove legga da sola gli esiti, senza che io scarichi un file ogni settimana
> così da avere l'elenco degli scoperti sempre aggiornato invece che aggiornato al mio ultimo giro di pazienza.

**Contesto e avvertenza.** ⚠️ **Questa storia è una fermata di escalation dello sviluppatore e non va implementata
senza un suo sì esplicito.** Non perché sia tecnicamente difficile, ma per due ragioni di sostanza. La prima: il
collegamento introduce un **responsabile esterno del trattamento** — un fornitore che tratta dati per nostro
conto — e va nell'informativa e nell'elenco dei fornitori. La seconda: è la storia in cui la tentazione di
attraversare il confine del §5.2 della descrizione diventa concreta, perché una volta collegati «basterebbe una
riga» per **disporre** un addebito. Disporre un addebito è avviare un pagamento, cioè un servizio regolato: è
esattamente ciò che non facciamo.

Il perimetro è quindi stretto e va difeso nel codice, non nelle intenzioni: **sola lettura, e nient'altro**.

## 2. Requisiti funzionali

1. **RF-1** — Il cliente collega il **proprio** conto presso il **proprio** fornitore, autorizzando l'accesso con
   il meccanismo che il fornitore prevede; le credenziali sono sue, il conto è suo, i soldi sono suoi.
2. **RF-2** — Il collegamento chiede e usa **soltanto** permessi di lettura degli esiti delle operazioni. Se il
   fornitore non consente di limitare i permessi alla sola lettura, il collegamento **non si fa**: il messaggio a
   schermo lo dice e propone l'importazione da file.
3. **RF-3** — SubGrove **non** dispone pagamenti, **non** crea richieste di addebito, **non** modifica nulla
   presso il fornitore: non esiste alcun percorso nel codice che lo permetta.
4. **RF-4** — Gli esiti letti si applicano con lo **stesso** meccanismo dell'importazione (storia `0019`):
   stesso abbinamento, stessi scarti, stessa idempotenza. Il collegamento è una sorgente in più, non una logica
   in più.
5. **RF-5** — La schermata dice sempre: quando è stata l'ultima lettura, quante operazioni ha portato, e se il
   collegamento è in errore, con il rimedio.
6. **RF-6** — Il collegamento si può revocare in qualunque momento, dall'app; la revoca cancella le credenziali
   custodite e l'app torna alla registrazione manuale senza perdere nulla di quanto già letto.
7. **RF-7** — Prima di collegare, l'app mostra un riquadro che dichiara: quali dati verranno letti, chi è il
   fornitore, che **appgrove non incasserà nulla** e che il denaro continua ad andare dove andava prima.

## 3. Requisiti tecnici

- **RT-1 — Nessun movimento di denaro (§5.2 della descrizione).** Il modulo che parla col fornitore espone
  **solo** operazioni di lettura; qualunque operazione di scrittura verso il fornitore è assente dal codice, e
  una prova lo verifica. È un presidio strutturale, non una convenzione.
- **RT-2 — Isolamento fra account (§1).** Le credenziali del collegamento sono per account e non si leggono mai
  fuori dal contesto del token verificato; gli esiti letti si applicano solo a scadenze dello stesso account.
- **RT-3 — Segreti.** Le credenziali del collegamento vivono nel deposito dei segreti della piattaforma, mai in
  tabella e mai nei registri; in locale il fornitore è **sempre simulato**, come già vale per il fornitore di
  pagamento della piattaforma.
- **RT-4 — Interfaccia di programmazione (§2).** Rotte `POST /api/abbonati/v1/collegamenti`,
  `DELETE /api/abbonati/v1/collegamenti/{id}`, `GET .../stato`; errori in `problem+json` con codice stabile per
  «permessi troppo ampi»; OpenAPI aggiornata.
- **RT-5 — Persistenza (§8).** Migrazione `V15__collegamento_fornitore.sql`: tabella `collegamento_fornitore`
  con `tenant_id`, colonne di controllo, fornitore, stato, ultima lettura, **riferimento** al segreto — mai il
  segreto.
- **RT-6 — Modulo frontend (§3, §5).** Riquadro nelle impostazioni dell'app con il testo di RF-7, lo stato e la
  revoca; solo token del sistema di design.
- **RT-7 — Cinque lingue (§4).** Testo del riquadro, stati ed errori in `en, it, fr, es, de`.
- **RT-8 — Esposizione conversazionale (§12).** Nessuno strumento: collegare o revocare un accesso a un conto non
  si fa da una chat.
- **RT-9 — Dati personali (§10).** ⚠️ **Il fornitore diventa un responsabile esterno del trattamento**: va
  dichiarato nel manifesto, nell'informativa e nell'elenco dei fornitori. Gli esiti letti possono contenere il
  nome del pagatore: si conserva solo ciò che serve all'abbinamento, come nella storia `0019`.
- **RT-10 — Registrazione eventi (§14).** `collegamento creato`, `lettura eseguita (operazioni)`,
  `collegamento in errore (causa)`, `collegamento revocato`, con `tenant_id`, `app_id`, `user_id` e
  correlazione, **senza** credenziali né nomi.

## 4. Criteri di accettazione

**CA-1 — Collegamento con soli permessi di lettura**
- **Dato** un cliente con un conto presso un fornitore supportato
- **Quando** collega il conto dopo aver letto il riquadro informativo
- **Allora** il collegamento è attivo, i permessi richiesti sono di sola lettura, e la schermata dice quando
  avverrà la prima lettura

**CA-2 — Permessi troppo ampi**
- **Dato** un fornitore che concede solo permessi comprensivi della scrittura
- **Quando** si prova a collegarlo
- **Allora** il collegamento è rifiutato con spiegazione, e l'app propone l'importazione da file

**CA-3 — Esiti applicati come un'importazione**
- **Dato** un collegamento attivo e dieci operazioni nuove presso il fornitore
- **Quando** gira la lettura
- **Allora** le scadenze abbinate risultano incassate, quelle non abbinate finiscono negli scarti, e nulla è
  applicato due volte

**CA-4 — Nessuna scrittura possibile**
- **Dato** il codice del modulo di collegamento
- **Quando** si esegue la prova strutturale
- **Allora** non esiste alcuna chiamata di scrittura verso il fornitore

**CA-5 — Revoca pulita**
- **Dato** un collegamento attivo · **Quando** il cliente lo revoca
- **Allora** le credenziali sono cancellate, gli esiti già applicati restano, e l'app torna alla registrazione
  manuale

## 5. Definizione di fatto

- [ ] **consenso esplicito dello sviluppatore** all'implementazione di questa storia — è una fermata di
      escalation e senza quel consenso la storia non parte;
- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sull'assenza di operazioni di scrittura e di **integrazione** con fornitore simulato;
- [ ] prova di **isolamento fra account** su credenziali ed esiti;
- [ ] **prova end-to-end**: *rimando* — richiede un fornitore simulato; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml), storia proprietaria `0033`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato **e** fornitore aggiunto all'elenco dei responsabili esterni e
      all'informativa;
- [ ] **registro delle decisioni** compilato: perimetro di sola lettura, presidio strutturale, fornitore come
      responsabile esterno;
- [ ] nessun percorso, nemmeno di prova, che disponga un pagamento.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0019` | riusa abbinamento, scarti e idempotenza: qui non si reinventa nulla |
| **decisione dello sviluppatore** | fermata di escalation: responsabile esterno nuovo ed effetto verso l'esterno |
| revisione legale | l'informativa e l'elenco dei fornitori vanno aggiornati |

## 7. Fuori ambito

- **disporre** addebiti, creare richieste di pagamento, muovere denaro: fuori dal perimetro dell'app, per
  scelta e per legge (§5.2 della descrizione);
- il collegamento come **unica** via di incasso: il nucleo resta la registrazione manuale;
- il pagamento dell'abbonato «dentro» una pagina di appgrove: mai.

## 8. Punti aperti

**Quali fornitori supportare, e se supportarne affatto.** Ogni fornitore è un lavoro di integrazione, un contratto
di trattamento dati e una manutenzione permanente. Con due fornitori si è già dentro un impegno serio.
**Proposta**: nessuno nel primo giro, e questa storia resta scritta ma non implementata finché non ci saranno
clienti veri che chiedono lo **stesso** fornitore. È coerente con la scelta delle app 06 e 07, che con il denaro
non si sono collegate affatto. Chiude: lo sviluppatore.

**Se un giorno si volesse incassare davvero.** Non sarebbe un'estensione di questa storia: sarebbe un altro
prodotto, con licenza o con un prestatore autorizzato che si assuma la responsabilità, un contratto diverso e
un'analisi legale che non spetta a un agente. Va aperta come decisione di prodotto, non come storia tecnica.
