# 0029 — Rifiuto delle richieste vietate

**Applicazione**: 17 — RepGrove (`recensioni`) · **Epica**: 06 — Esposizione conversazionale e prove end-to-end
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0012`, `0013`, `0027`, `0028`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che chiede alla chat «manda l'invito solo a quelli contenti, come faceva il mio vecchio
> fornitore»
> voglio sentirmi rispondere di no, con il motivo e con quello che posso fare invece
> così da non rischiare la sospensione del mio profilo credendo di usare una funzione normale.

**Contesto.** L'app è costruita attorno a un elenco di pratiche che **rifiuta di implementare** (descrizione §1):
il filtro dei clienti scontenti e le sue forme travestite, gli incentivi, le richieste di contenuto specifico, le
quote per dipendente, la pressione in loco, le recensioni scritte a macchina, la soppressione delle negative, il
riquadro che nasconde le stelle basse. Nell'interfaccia questi rifiuti sono già codice: la schermata della regola
di equità non ha una terza opzione (storia 0012), il controllo dei modelli respinge i testi che promettono un
vantaggio (storia 0013), il riquadro non ha il parametro del voto (storia 0024).

Il livello conversazionale è però una **superficie nuova**, e una superficie nuova è il modo classico in cui un
divieto evapora: basta uno strumento con un parametro in più, o un campo di testo libero che passa senza controllo,
e la pratica rientra dalla finestra. Questa storia chiude la porta e — cosa che conta di più — trasforma il rifiuto
in una **spiegazione**: chi chiede quella cosa quasi sempre non sa che è vietata, e gliel'ha insegnata un software
che sembrava professionale.

Il rifiuto deve valere anche quando la richiesta è formulata in modo innocente («manda solo ai clienti abituali che
mi vogliono bene») e anche quando arriva da un assistente che sta solo eseguendo un'istruzione ricevuta.

## 2. Requisiti funzionali

1. **RF-1** — **Rifiuto per costruzione.** Nessuno strumento del contratto (storie 0027 e 0028) espone parametri che
   permettano di selezionare i destinatari in base alla soddisfazione, di offrire un vantaggio, di chiedere un
   contenuto specifico, di attribuire obiettivi al personale o di filtrare il riquadro per voto. Una prova
   automatica ispeziona gli schemi e **fallisce** se un parametro del genere compare.
2. **RF-2** — **Rifiuto per contenuto.** Dove esiste un testo libero che arriva dal livello conversazionale —
   modello del messaggio di invito, testo della risposta, motivazione della segnalazione, paragrafo aggiuntivo della
   dichiarazione — si applica lo **stesso** controllo delle pratiche vietate della storia 0013: stessa regola,
   stesso codice, nessuna variante indulgente per il percorso conversazionale.
3. **RF-3** — Il rifiuto è **strutturato e leggibile da un programma**: categoria della pratica, regola violata,
   fonte (la politica di Google, le linee guida di Trustpilot, la legge 34/2026, la disciplina europea), spiegazione
   in lingua naturale e **alternativa ammessa** quando esiste.
4. **RF-4** — Le alternative ammesse sono dichiarate, non improvvisate: a «invita solo i contenti» si risponde con
   *tutti* oppure *uno ogni N* (storia 0012); a «fai sparire questa recensione» si risponde con la segnalazione
   motivata, e solo per i casi che la legge prevede (storia 0021); a «offri uno sconto» si risponde che l'invito può
   ringraziare ma non promettere.
5. **RF-5** — Nessuno strumento **scrive una recensione**: se la richiesta è di produrre il testo di una recensione,
   o di suggerirlo a un cliente, il rifiuto è netto e cita il divieto di compravendita e le sanzioni della legge
   italiana (5.000-50.000 euro).
6. **RF-6** — I rifiuti sono **contati e consultabili** dal cliente nella propria app, per categoria e periodo: non
   per punirlo, ma perché vedere «hai provato quattro volte a chiedere una cosa vietata» è il modo più efficace di
   spiegargli che non è un difetto del prodotto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il conteggio dei rifiuti e la loro consultazione filtrano per `tenant_id`
  preso dal token verificato; nessun rifiuto di un account è visibile a un altro.
- **RT-2 — Interfaccia di programmazione (§2).** Il rifiuto viaggia come errore in `application/problem+json` con un
  tipo dedicato e i campi della categoria e della fonte, sia sulle rotte sia nella forma d'errore del contratto
  degli strumenti: **una sola implementazione, due presentazioni**. Rotta
  `GET /api/recensioni/v1/rifiuti?periodo` per la consultazione.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__rifiuto_pratica.sql` sullo schema `app_recensioni`: tabella
  `rifiuto_pratica` con `tenant_id`, categoria, superficie d'origine (interfaccia o strumento), momento, utente e
  **nessun contenuto della richiesta**. Chiave primaria a identificativo universale versione 7, colonne di
  controllo, `deleted_at`.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione *Impostazioni* → «Regole e limiti»: elenco delle pratiche che
  l'app non implementa, con la fonte di ciascuna, e il conteggio dei rifiuti del periodo. È materiale di
  spiegazione, non un registro di colpe: il tono conta. Solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutti i testi dei rifiuti e delle spiegazioni in `en, it, fr, es, de`: sono i testi
  che un assistente ripeterà all'utente, e un rifiuto incomprensibile è un rifiuto che verrà aggirato.
- **RT-6 — Varchi e quota (§6, §7).** Il rifiuto avviene **prima** di qualunque prenotazione di quota e prima della
  creazione di una bozza: una pratica vietata non consuma niente e non lascia una bozza pendente.
- **RT-7 — Esposizione conversazionale (§12).** Il descrittore degli strumenti dichiara, accanto a ciascuno, le
  **categorie di richiesta rifiutate** con la relativa fonte: è informazione che serve al livello di sopra per
  spiegare invece di ritentare. Nessuno strumento nuovo.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo, e un divieto esplicito: il registro dei rifiuti
  **non conserva il testo** della richiesta rifiutata, che potrebbe contenere nomi di clienti. Si conserva la
  categoria, non la frase.
- **RT-9 — Registrazione eventi (§14).** `richiesta rifiutata` con categoria, superficie, `tenant_id`, `app_id`,
  `user_id` e identificativo di correlazione; **mai** il contenuto della richiesta.

## 4. Criteri di accettazione

**CA-1 — Il parametro vietato non esiste**
- **Dato** lo schema dichiarato di `programma_richieste`
- **Quando** la prova automatica ispeziona i parametri
- **Allora** non esiste alcun parametro che selezioni per soddisfazione, voto atteso o giudizio interno; aggiungerne
  uno fa fallire la suite

**CA-2 — La richiesta a parole viene rifiutata e spiegata**
- **Dato** un'istruzione che chiede di invitare solo i clienti soddisfatti, passata come testo in un campo libero
- **Quando** lo strumento la elabora
- **Allora** risponde con un rifiuto strutturato di categoria «sollecitazione selettiva», con la fonte e con le due
  alternative ammesse, e nessuna bozza viene creata

**CA-3 — Incentivo nel testo dell'invito**
- **Dato** un modello di messaggio proposto dal livello conversazionale che promette uno sconto
- **Quando** viene salvato
- **Allora** è respinto dallo **stesso** controllo della storia 0013, con lo stesso codice e la stessa spiegazione
  che si otterrebbe dall'interfaccia

**CA-4 — Nessuna recensione scritta**
- **Dato** la richiesta «scrivimi una recensione a cinque stelle da pubblicare»
- **Quando** viene elaborata
- **Allora** il rifiuto è netto, cita il divieto di compravendita e le sanzioni, e non viene prodotto alcun testo

**CA-5 — Il riquadro non si filtra nemmeno da qui**
- **Dato** la richiesta di mostrare nel riquadro solo le recensioni da quattro stelle in su
- **Quando** viene elaborata
- **Allora** è rifiutata con la categoria «presentazione ingannevole» e le impostazioni del riquadro restano
  invariate

**CA-6 — Niente consumo e niente residui**
- **Dato** un account vicino ai limiti del canale di invio
- **Quando** una richiesta vietata viene rifiutata
- **Allora** nessuna quota risulta prenotata e nessuna bozza resta in attesa

**CA-7 — Isolamento fra account**
- **Dato** rifiuti registrati in due account
- **Quando** un utente di `A` consulta il proprio elenco
- **Allora** vede solo i propri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** su ciascuna categoria di rifiuto e sulla **ispezione degli schemi** degli strumenti (è la
      prova che impedisce la regressione silenziosa); di **integrazione** sulle rotte e sul contratto;
- [ ] prova di **isolamento fra account** sul registro dei rifiuti;
- [ ] **prova end-to-end**: *coprire ora* nel percorso `[J-RECENSIONI]` (storia 0030) il passo «chiedo una pratica
      vietata e ottengo un rifiuto spiegato», e registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** dei testi di rifiuto e delle alternative in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che il registro dei rifiuti non conservi contenuti;
- [ ] **registro delle decisioni** compilato, con l'elenco delle categorie rifiutate e la fonte di ciascuna;
- [ ] contratto degli **strumenti conversazionali** aggiornato con le categorie rifiutate per strumento;
- [ ] documentazione: l'elenco delle pratiche rifiutate compare anche nella scheda di vendita, perché è il
      posizionamento dell'app (descrizione §1).

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0012` | la regola di equità e le sue due sole forme sono l'alternativa ammessa da proporre |
| storia `0013` | il controllo delle pratiche vietate esiste già: qui si riusa, non si riscrive |
| storie `0027`, `0028` | sono gli strumenti a cui il rifiuto si applica |
| storia `0021` | la segnalazione motivata è l'alternativa ammessa a «fai sparire la recensione» |

## 7. Fuori ambito

- il comportamento del modello linguistico che formula la richiesta: non lo controlliamo, e per questo il presidio
  sta nel servizio e non nel suggerimento dato all'assistente;
- il blocco dell'account che insiste: è materia di assistenza e di console di amministrazione
  ([estensioni-admin.md](../estensioni-admin.md)), non dell'app;
- l'analisi automatica del tono dei testi liberi oltre le regole dichiarate: qui si riconoscono le pratiche
  elencate, non si giudica lo stile.

## 8. Punti aperti

- **Quanto deve essere severo il riconoscimento delle formulazioni indirette** («manda solo ai clienti abituali»):
  troppo severo respinge richieste legittime, troppo indulgente lascia passare la pratica. La proposta è di
  riconoscere le formulazioni esplicite e di **chiedere chiarimento** su quelle ambigue, invece di indovinare.
  **Da confermare.**
- **Se mostrare al cliente il conteggio dei propri rifiuti** possa essere percepito come sorveglianza: la proposta è
  mostrarlo solo a lui e nel contesto della spiegazione, mai come classifica. È una scelta di prodotto.
