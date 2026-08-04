# 0006 — Anagrafica dei debitori

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 02 — Portafoglio crediti
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come addetta all'amministrazione di una piccola impresa
> voglio tenere l'elenco di chi mi deve dei soldi, con i recapiti giusti e la persona a cui rivolgermi
> così da non dover cercare l'indirizzo di posta elettronica dentro una vecchia fattura ogni volta che scrivo.

**Contesto.** Oggi i recapiti dei clienti stanno in tre posti: il gestionale, la rubrica del telefono e la memoria di
chi lavora. Il sollecito parte all'indirizzo sbagliato, oppure alla persona sbagliata, e il credito invecchia perché
nessuno se n'è accorto. Questa storia è la prima con una schermata vera. Il perimetro dei campi non è libero: il
vademecum del Garante sul recupero crediti ammette **solo** dati identificativi, codice fiscale o partita IVA, recapiti,
importo del debito e condizioni di pagamento ([documento capofila](../application-description.md) §2.3, punto 4). Ciò
che la norma non ammette, l'app non deve nemmeno offrirlo.

## 2. Requisiti funzionali

1. **RF-1** — L'utente crea un debitore indicando denominazione, forma (impresa o persona fisica), identificativo
   fiscale, recapito di posta elettronica, telefono, nome del referente e lingua preferita.
2. **RF-2** — Denominazione e almeno un recapito sono obbligatori; senza un recapito valido il debitore si può salvare
   ma è marcato «non sollecitabile».
3. **RF-3** — L'elenco dei debitori si cerca per denominazione e per identificativo fiscale, con paginazione.
4. **RF-4** — L'utente modifica e cancella logicamente un debitore; la cancellazione è impedita finché esistono crediti
   aperti collegati, con un messaggio che dice quanti sono.
5. **RF-5** — La scheda del debitore mostra il totale dovuto e il numero di crediti aperti, calcolati dal servizio.
6. **RF-6** — Accanto al campo note compare l'avvertenza di non inserirvi dati delicati, in tutte e cinque le lingue.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura dell'entità `Debitore` filtra per `tenant_id` preso
  dal token verificato; un `tenant_id` che arrivasse dal corpo della richiesta o dai parametri viene ignorato. Prova di
  isolamento fra due account sulla risorsa.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET`, `POST`, `PATCH`, `DELETE /api/crediti/v1/debitori` (e
  `/{id}`); corpo validato con vincoli dichiarativi; errori in `application/problem+json`; paginazione a pagina e
  dimensione con totale; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** La tabella esiste dalla storia `0002`; qui si aggiunge l'indice di ricerca su
  (`tenant_id`, denominazione) e il vincolo che impedisce la cancellazione con crediti aperti.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Debitori* del modulo `crediti`: elenco con ricerca e paginazione,
  scheda di dettaglio, modulo di inserimento con React Hook Form e validazione dichiarativa. Dati letti con il client
  generato; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `crediti` e sono presenti in
  `en, it, fr, es, de`. Attenzione: la **lingua preferita del debitore** è un dato del debitore e non ha nulla a che
  vedere con la lingua dell'interfaccia — sono due cose diverse e vanno chiamate diversamente anche nei testi.
- **RT-6 — Varchi e quota (§6, §7).** L'anagrafica **non** consuma la metrica `crediti_monitorati`: si contano i
  crediti, non i debitori. Restano tutti gli altri varchi: `402` senza abilitazione, `403` a ruolo insufficiente.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: la ricerca del debitore entra nel
  contratto con la storia `0028`, dove gli strumenti di lettura si dichiarano insieme.
- **RT-8 — Dati personali (§10).** Le voci del manifesto per i campi del debitore esistono dalla storia `0002`: qui si
  verifica che siano **complete e vere** rispetto a ciò che l'interfaccia raccoglie davvero, e si annota nel manifesto
  che il campo note è a testo libero.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «debitore creato», «debitore modificato», «cancellazione respinta
  per crediti aperti» sono registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza
  nomi né recapiti**.

## 4. Criteri di accettazione

**CA-1 — Creazione**
- **Dato** un utente con ruolo `member` di un account abilitato
- **Quando** crea un debitore con denominazione e indirizzo di posta elettronica
- **Allora** il debitore compare nell'elenco e la sua scheda mostra zero crediti aperti

**CA-2 — Debitore senza recapito**
- **Dato** un debitore salvato senza posta elettronica né telefono
- **Quando** si apre la sua scheda
- **Allora** è marcato «non sollecitabile» con la spiegazione di che cosa manca

**CA-3 — Cancellazione impedita**
- **Dato** un debitore con 3 crediti aperti
- **Quando** l'utente tenta di cancellarlo
- **Allora** riceve un errore `409` in `problem+json` che dice «3 crediti aperti collegati» e nulla viene cancellato

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri debitori
- **Quando** un utente di `A` chiede l'elenco dei debitori
- **Allora** vede solo i propri, anche se forza l'identificativo dell'altro account nella richiesta

**CA-5 — Ruolo insufficiente**
- **Dato** un utente in sola lettura (per esempio il commercialista)
- **Quando** tenta di creare un debitore
- **Allora** riceve `403` e l'interfaccia non gli mostra affatto il pulsante di creazione

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sulla validazione dei recapiti e di **integrazione** sulla risorsa `debitori`;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** sulla risorsa introdotta;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che possiede il percorso `[J-CREDITI]` — motivo: il percorso
      ha senso solo quando esiste il flusso completo fino al sollecito;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** verificato voce per voce contro i campi realmente raccolti;
- [ ] **registro delle decisioni** compilato, in particolare sul perimetro dei campi ammessi e sul perché non ce ne sono
      altri;
- [ ] contratto degli **strumenti conversazionali**: nessuna aggiunta, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | Serve la tabella `debitore` |
| storia `0003` | Serve il guscio dove innestare la sezione |

## 7. Fuori ambito

- L'importazione dei debitori da file: arriva insieme ai crediti nella storia `0008`, perché nella pratica il file è
  uno solo.
- L'unione di debitori duplicati: rimandata, non è il problema del primo giorno; se emergerà, sarà una storia propria.
- Il punteggio di rischio mostrato nella scheda: storia `0025`.

## 8. Punti aperti

Nessuno.
