# 0008 — Contratto della misura di consumo

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 02 — Ingresso dei dati di consumo
**Storia**: `0008` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore del prodotto di un cliente
> voglio sapere esattamente che cosa devo mandare a TokenGrove, e soprattutto che cosa non devo mandargli
> così da poter strumentare le mie chiamate in mezz'ora sapendo che non sto consegnando i dati dei miei clienti.

**Contesto.** Questa storia scrive il **formato**, non il codice che lo riceve (che è della storia `0009`). Va
separata perché è la decisione che tiene insieme mezza applicazione: dalla forma della misura dipendono la
deduplica, l'attribuzione, il calcolo del costo e — soprattutto — la promessa di non vedere il contenuto. Ed è la
decisione più costosa da cambiare dopo, perché il formato sarà già dentro il codice dei clienti. Ci si allinea ai
nomi delle convenzioni OpenTelemetry per l'intelligenza artificiale generativa (`gen_ai.usage.input_tokens`,
`gen_ai.usage.output_tokens`) così che chi ha già uno strumento di osservabilità non debba scrivere due volte la
stessa cosa; ma **con una mappatura nostra in mezzo**, perché quelle convenzioni al luglio 2026 non sono ancora
dichiarate stabili e sono state spostate in un archivio dedicato (§2.6, fonte 16).

## 2. Requisiti funzionali

1. **RF-1** — È pubblicato lo schema della misura di consumo, versionato, con questi campi e nessun altro:
   identificativo della chiamata, istante, fornitore, chiave del modello, conteggi (ingresso, uscita, ingresso
   servito da cache, scrittura in cache), durata, esito, e un insieme di **etichette** chiave-valore.
2. **RF-2** — Lo schema **non prevede alcun campo per il contenuto** della richiesta o della risposta, né in
   chiaro, né troncato, né riassunto. Non è un'omissione: è scritto come divieto esplicito nella documentazione
   dello schema.
3. **RF-3** — Una misura che contenga un campo non previsto viene **respinta** con un errore che dice quale campo
   e perché; non viene accettata e ripulita, perché un'accettazione silenziosa insegnerebbe al cliente che può
   mandarci qualunque cosa.
4. **RF-4** — Le etichette hanno vincoli dichiarati: numero massimo, lunghezza massima della chiave e del valore,
   e l'avvertenza esplicita che non sono un posto per dati personali.
5. **RF-5** — È pubblicata la **mappatura** fra i nomi delle convenzioni OpenTelemetry e i campi del nostro
   schema, in un solo punto, così che un cambio dello standard si assorba lì.
6. **RF-6** — Lo schema è versionato: una misura dichiara la versione con cui è stata scritta, e il ricevitore
   accetta le versioni che sa leggere rifiutando le altre con un messaggio che dice quale versione usare.

## 3. Requisiti tecnici

- **RT-1 — Interfaccia di programmazione (§2).** Lo schema entra nella definizione OpenAPI del servizio, con
  esempi, ed è versionato con essa nello stesso commit. La documentazione per chi integra nasce da lì, non da un
  documento a parte che invecchierebbe.
- **RT-2 — Dati personali (§10).** È la storia che **stabilisce il presidio principale** dell'app in materia:
  nessun contenuto, mai. Le etichette restano l'unico ingresso di testo libero e vanno dichiarate nel manifesto in
  italiano e inglese come possibile veicolo di identificativi di utenti e clienti finali (voci
  `misura.etichetta_utente_finale` e `misura.etichetta_cliente`). Il campo si annota come dato personale.
- **RT-3 — Isolamento fra account (§1).** Lo schema **non contiene** il `tenant_id`: l'account si ricava dalla
  chiave di invio (storia `0009`). Una misura che dichiarasse un account nel corpo viene respinta come campo non
  previsto: è lo stesso principio dell'invariante, applicato al formato.
- **RT-4 — Esposizione conversazionale (§12).** Nessuno strumento: è un contratto di dati, non una funzione.
- **RT-5 — Registrazione eventi (§14).** Nessun evento nuovo. Va però stabilito qui che **il corpo di una misura
  respinta non finisce nei registri**: si registra il codice del motivo e il nome del campo offensivo, mai il suo
  valore. Un campo respinto perché sospetto di contenere testo delle richieste sarebbe altrimenti riversato nei
  registri proprio da chi lo sta rifiutando.

## 4. Criteri di accettazione

**CA-1 — Una misura ben formata è accettata**
- **Dato** una misura con tutti i campi previsti, tre etichette e la versione corrente dello schema
- **Quando** viene validata
- **Allora** passa, e i suoi conteggi sono interpretati secondo la mappatura pubblicata

**CA-2 — Il contenuto viene respinto, non ripulito**
- **Dato** una misura che porta un campo con il testo della richiesta
- **Quando** viene validata
- **Allora** è **respinta** con un errore che nomina il campo e spiega che TokenGrove non riceve contenuti; nulla
  viene registrato e il valore del campo non compare nei registri

**CA-3 — Etichette fuori limite**
- **Dato** una misura con più etichette del massimo consentito, o con un valore più lungo del massimo
- **Quando** viene validata
- **Allora** è respinta con un errore che dice quale vincolo è stato violato

**CA-4 — Versione dello schema non supportata**
- **Dato** una misura che dichiara una versione dello schema più recente di quella che il servizio sa leggere
- **Quando** viene validata
- **Allora** è respinta con un messaggio che indica le versioni accettate

**CA-5 — Mappatura dallo standard**
- **Dato** un record scritto con i nomi delle convenzioni OpenTelemetry
- **Quando** passa dalla mappatura pubblicata
- **Allora** produce una misura equivalente a quella scritta con i nostri nomi, con gli stessi conteggi

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione e sulla mappatura, con casi limite e casi ostili;
- [ ] prova di **isolamento fra account**: si verifica che un account dichiarato nel corpo venga ignorato/respinto;
- [ ] **prova end-to-end**: **nessun impatto** (nessuna superficie utente); il percorso è coperto dalla storia
      `0009` che usa questo schema;
- [ ] **traduzioni**: i messaggi di errore della validazione sono presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con le due voci delle etichette, campi annotati;
- [ ] **registro delle decisioni** compilato, in particolare sul rifiuto invece della ripulitura e
      sull'allineamento non vincolante alle convenzioni OpenTelemetry;
- [ ] documentazione per chi integra generata dalla definizione delle interfacce.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` | La forma della tabella `misura` e la forma del contratto devono corrispondere |
| Punto aperto P8 del documento capofila | Lo stato delle convenzioni OpenTelemetry va riverificato prima di scrivere la mappatura |

## 7. Fuori ambito

- il ricevitore che accetta le misure: è della storia `0009`;
- la libreria di invio per i linguaggi dei clienti: **rimandata** oltre il perimetro minimo, perché il formato è
  abbastanza semplice da poter essere prodotto a mano; se la prova sul mercato mostrasse che l'attrito è troppo, la
  libreria diventa una storia sua;
- la validazione dei valori delle etichette rispetto alle dimensioni dichiarate dall'account: è della storia
  `0019`.

## 8. Punti aperti

- **Il controllo che segnala le etichette con forma di indirizzo di posta** (punto P4 del documento capofila) va
  deciso qui, perché è nel contratto che va scritto se una tale etichetta è respinta, segnalata o accettata. La
  proposta è **accettarla segnalandola**, perché respingerla farebbe perdere al cliente misure che ha già pagato;
  ma è una decisione dello sviluppatore.
- **Se accettare un'impronta del contenuto** (una firma che non permette di risalire al testo) per riconoscere le
  chiamate ripetute. Sarebbe utile per la storia `0026`, ma è il primo passo su una china: la proposta è **no**, e
  la decisione è dello sviluppatore.
