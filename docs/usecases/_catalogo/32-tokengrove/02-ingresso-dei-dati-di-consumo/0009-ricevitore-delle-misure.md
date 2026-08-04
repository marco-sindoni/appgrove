# 0009 — Ricevitore delle misure e chiave di invio

**Applicazione**: 32 — TokenGrove (`spesa_modelli`) · **Epica**: 02 — Ingresso dei dati di consumo
**Storia**: `0009` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore del prodotto di un cliente
> voglio poter mandare a TokenGrove una misura dopo ogni chiamata al modello, senza rischiare di rallentare o
> fermare il mio prodotto
> così da avere l'attribuzione per cliente e per funzionalità che il rendiconto del fornitore non mi può dare.

**Contesto.** È la storia che realizza la scelta architetturale del §3.1 del documento capofila: **riceviamo, non
stiamo in mezzo**. La conseguenza pratica è che il ricevitore deve essere progettato con una regola sopra tutte: se
noi siamo lenti o assenti, il cliente non se ne accorge. Il rendiconto del fornitore (storie `0006`-`0007`) dà il
totale ma si ferma a chiave e progetto, e il costo solo a giorno (§2.6, fonti 1-3); solo la misura in ingresso può
dire «questa chiamata era per il cliente Rossi, dalla funzione di riassunto».

## 2. Requisiti funzionali

1. **RF-1** — Esiste una **chiave di invio** per account, generabile e revocabile dall'interfaccia, distinta dalle
   credenziali dei fornitori e con il solo permesso di scrivere misure. Una chiave revocata smette di funzionare
   subito.
2. **RF-2** — Il ricevitore accetta misure **a lotti** (più misure in una sola chiamata) e risponde appena il lotto
   è preso in carico, senza aspettare che sia lavorato.
3. **RF-3** — Una risposta di errore del ricevitore **non deve mai** essere un motivo per cui il prodotto del
   cliente si ferma: la documentazione prescrive l'invio asincrono e a perdere, e il ricevitore non chiede mai un
   nuovo tentativo immediato.
4. **RF-4** — Un lotto in cui alcune misure sono valide e altre no viene accettato **parzialmente**: si registrano
   le valide e si restituisce l'elenco delle respinte con il motivo, per posizione nel lotto.
5. **RF-5** — Il ricevitore protegge sé stesso: dimensione massima del lotto, dimensione massima del corpo, e un
   limite di frequenza per account che risponde `429` con l'indicazione di quanto attendere.
6. **RF-6** — La sezione Fonti mostra lo stato dell'invio: quante misure ricevute nell'ultima ora, quante respinte
   e per quale motivo, e l'istante dell'ultima ricevuta.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'account si ricava **solo** dalla chiave di invio verificata; una
  misura che dichiarasse un `tenant_id` nel corpo viene respinta. Ogni scrittura porta il `tenant_id` della chiave.
  Prova di isolamento: una chiave dell'account `A` non può scrivere misure nell'account `B`.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `POST /api/spesa_modelli/v1/misure` che accetta un lotto e
  restituisce un riepilogo (accettate, respinte con motivo e posizione); corpo validato secondo lo schema della
  storia `0008`; errori in `problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Autenticazione della chiave di invio.** La chiave di invio **non** è un gettone di accesso di
  piattaforma: è una credenziale di servizio con un solo permesso. Va conservata come impronta, mai in chiaro, ed è
  visibile una sola volta alla generazione.
- **RT-4 — Persistenza (§8).** Migrazione sullo schema `app_spesa_modelli`: tabella `chiave_di_invio` con
  `tenant_id`, impronta, nome, stato, ultimo uso, colonne di controllo e cancellazione logica.
- **RT-5 — Varchi e quota (§6, §7).** Ogni misura accettata prenota una unità della metrica `misure_registrate`
  (natura a consumo); a quota esaurita il ricevitore risponde `429` con il messaggio che spiega il rimedio e **non
  registra nulla**. Un abbonamento non attivo produce `402`.
- **RT-6 — Modulo frontend (§3, §5).** Nella sezione «Fonti» compaiono la generazione della chiave, l'esempio di
  invio pronto da copiare e i conteggi di ricezione; solo token del sistema di design; tema chiaro e scuro.
- **RT-7 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `spesa_modelli` e sono
  presenti in `en, it, fr, es, de`.
- **RT-8 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura: mandare misure è un'operazione fra
  programmi, non una funzione che una persona comanda a voce. Lo stato della ricezione è compreso in `stato_fonti`.
- **RT-9 — Dati personali (§10).** Le etichette accettate sono già dichiarate nel manifesto (storia `0008`);
  nessuna categoria nuova. La chiave di invio è un segreto, non un dato personale.
- **RT-10 — Registrazione eventi (§14).** Eventi «lotto ricevuto con N accettate e M respinte», «misura respinta
  per motivo X», «limite di frequenza raggiunto» con `tenant_id`, `app_id`, `user_id` (assente per le chiamate a
  chiave di servizio) e identificativo di correlazione. **Mai** il corpo della misura respinta.

## 4. Criteri di accettazione

**CA-1 — Un lotto valido viene preso in carico**
- **Dato** una chiave di invio valida e un lotto di cinquanta misure ben formate
- **Quando** il lotto viene inviato
- **Allora** la risposta arriva subito con «50 accettate, 0 respinte», e le misure compaiono nella spesa entro il
  ritardo di lavorazione dichiarato

**CA-2 — Accettazione parziale**
- **Dato** un lotto di dieci misure di cui due portano un campo non previsto
- **Quando** il lotto viene inviato
- **Allora** otto sono registrate, due sono respinte con motivo e posizione, e il valore dei campi respinti non
  compare nei registri

**CA-3 — Chiave revocata**
- **Dato** una chiave di invio revocata
- **Quando** viene usata
- **Allora** riceve `401` e nessuna misura viene registrata

**CA-4 — Isolamento fra account**
- **Dato** una chiave di invio dell'account `A`
- **Quando** invia una misura che dichiara nel corpo l'account `B`
- **Allora** la misura è respinta, e in nessun caso finisce nell'account `B`

**CA-5 — Quota esaurita**
- **Dato** un account che ha raggiunto il tetto di `misure_registrate`
- **Quando** invia un lotto
- **Allora** riceve `429` con un messaggio che spiega come rimediare, e nulla viene registrato

**CA-6 — Il ricevitore non chiede al cliente di riprovare subito**
- **Dato** il ricevitore momentaneamente non disponibile
- **Quando** il cliente invia un lotto
- **Allora** la risposta di errore non contiene alcun invito a un nuovo tentativo immediato, e la documentazione
  prescrive di scartare o accodare localmente senza bloccare la chiamata dell'utente finale

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sull'accettazione parziale e sui limiti, e di **integrazione** sul ricevitore con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla chiave di invio;
- [ ] **prova end-to-end**: **coprire ora**, estendendo `[J-SPESA-MODELLI]` con il passo «genera la chiave, invia
      un lotto, vedi la spesa attribuita», e aggiornare il registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova rispetto alla storia `0008`, dichiarato esplicitamente;
- [ ] **registro delle decisioni** compilato, in particolare sull'accettazione parziale e sul divieto di chiedere
      un nuovo tentativo immediato;
- [ ] contratto degli **strumenti conversazionali**: nessuno introdotto, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0008` | Serve lo schema della misura da validare |
| Storia `0006` | La ricezione è una fonte di tipo «invio» e ne riusa la sezione e il modello |
| Storia `0004` | Serve la prenotazione della quota |

## 7. Fuori ambito

- la deduplica e gli arrivi in ritardo: sono della storia `0010`;
- il calcolo del costo della misura: è dell'epica 03;
- una libreria pronta per i linguaggi dei clienti: rimandata (vedi storia `0008`, §7).

## 8. Punti aperti

- **Se accettare anche il formato nativo di OpenTelemetry** oltre al nostro, per ricevere dati da chi ha già un
  raccoglitore. Sarebbe una porta d'ingresso in più a costo contenuto, ma lega il prodotto a uno standard che non è
  ancora stabile (§2.6, fonte 16). La proposta è **rimandare** e riprendere la decisione quando lo standard sarà
  dichiarato stabile; nel frattempo la mappatura documentata della storia `0008` permette comunque a chi vuole di
  convertire da sé.
