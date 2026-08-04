# 0028 — Rapporto periodico

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 05 — Lettura, ricerca e rendicontazione
**Storia**: `0028` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0024`, `0013`, `0027`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come persona che deve rendere conto a qualcun altro — un cliente, un revisore, un'assicurazione, il proprio
> consiglio di amministrazione
> voglio un documento di periodo che racconti che cosa hanno fatto i miei agenti e come sono andate le approvazioni
> così da poter rispondere con un allegato invece che con una promessa.

**Contesto.** La cronologia (storia 0024) serve a chi indaga; l'esportazione (storia 0027) serve a chi elabora.
Questa storia serve a chi deve **mostrare**, e la differenza non è cosmetica: un elenco di diecimila righe non è
una risposta, è uno scaricabarile. Il rapporto di periodo è ciò che il cliente allega quando gli chiedono conto, ed
è anche — commercialmente — il momento in cui il prodotto si fa vedere: un documento con il logo del cliente, i
numeri del trimestre e l'esito della verifica dei sigilli è la cosa che fa rinnovare l'abbonamento.

**L'avvertenza che governa la storia.** Il rapporto **non attesta la conformità del cliente a nessuna norma**.
Attesta che cosa è successo, secondo quanto è stato dichiarato ad AuditGrove. La distinzione non è formale: la
maggior parte degli agenti di una micro-impresa non ricade negli obblighi del regolamento europeo
sull'intelligenza artificiale, e vendere «ti mette a norma» sarebbe falso (§2.3 della descrizione
dell'applicazione). La frase di avvertenza va **stampata nel documento**, non nascosta nelle condizioni d'uso.

## 2. Requisiti funzionali

1. **RF-1** — Si genera un rapporto per un periodo scelto, che contiene: intervallo, conteggi per sorgente, per
   strumento, per classe di effetto e per esito; le azioni che hanno richiesto un'approvazione e come sono andate
   (concesse, negate, scadute); gli **scostamenti** rilevati (storia 0023); i **sigilli** del periodo con l'esito
   della loro verifica.
2. **RF-2** — Il documento porta in prima pagina l'**avvertenza obbligatoria**: il rapporto descrive ciò che è
   stato dichiarato ad AuditGrove nel periodo, non attesta la conformità a nessuna norma, e non copre le azioni
   che nessuna sorgente ha dichiarato.
3. **RF-3** — Il rapporto dichiara la propria **completezza**: numero di buchi di sequenza rilevati (storia 0011),
   periodi in cui una sorgente è stata silenziosa, e azioni rifiutate per quota esaurita (storia 0004). Un
   rapporto che tace su ciò che manca sarebbe peggio di nessun rapporto.
4. **RF-4** — Il rapporto si genera su richiesta e si può **programmare** (mensile o trimestrale), con recapito
   per posta elettronica ai destinatari scelti.
5. **RF-5** — Il documento è prodotto in un formato stampabile e archiviabile, nella lingua scelta fra le cinque
   dell'interfaccia, e ogni rapporto generato è **una riga del registro**.
6. **RF-6** — Il rapporto rimanda al pacchetto di prova (storia 0015) per lo stesso periodo: chi legge il rapporto
   e vuole verificare invece che credere sa dove andare.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Tutti i conteggi si calcolano sui soli dati dell'account, con
  `tenant_id` preso dal token verificato; il rapporto programmato gira per account e non può leggere altro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/agentaudit/v1/reports` (generazione su
  richiesta), `GET /api/agentaudit/v1/reports` (elenco), `GET /api/agentaudit/v1/reports/{id}/download` e
  `PUT /api/agentaudit/v1/report-schedules` (programmazione); corpi validati; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V…__rapporti.sql` sullo schema `app_agentaudit`: tabelle `reports` e
  `report_schedules` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione
  logica. Il documento prodotto è **immutabile una volta generato**: rigenerare lo stesso periodo produce un nuovo
  rapporto, non sovrascrive il precedente — altrimenti un documento consegnato a terzi potrebbe cambiare sotto i
  piedi di chi l'ha ricevuto.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `rapporti` del modulo `agentaudit` con generazione, elenco e
  programmazione; solo token del sistema di design; funziona in tema chiaro e scuro. L'anteprima usa gli stessi
  token del documento, così che ciò che si vede sia ciò che si stampa.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili passano dallo spazio-nomi `agentaudit` e sono presenti
  in `en, it, fr, es, de`, **compreso il testo del documento e l'avvertenza obbligatoria** — che è la frase più
  importante del file e va tradotta con attenzione particolare, non con una traduzione automatica.
- **RT-6 — Varchi e quota (§6, §7).** La generazione **non consuma** quota in proporzione alle righe lette;
  consuma una unità della metrica `actions` per la riga di registro che la traccia. Con abbonamento in `past_due`
  la generazione resta accessibile; con `canceled` risponde `402`, ma l'esportazione dei propri dati resta
  accessibile in ogni caso.
- **RT-7 — Esposizione conversazionale (§12).** Questa storia non dichiara strumenti nuovi: `riepiloga_attivita`
  (storia 0034, lettura) restituisce gli stessi conteggi in forma minimizzata, ma **non** genera il documento —
  produrre un file da consegnare a terzi è un atto che una persona deve compiere.
- **RT-8 — Dati personali (§10).** Il rapporto contiene conteggi e, nella parte sulle approvazioni, **identificativi
  di chi ha deciso**: è un trattamento di dati personali e va dichiarato nel manifesto in italiano e inglese, con
  la tabella dei rapporti presente in `exportData` e `purgeData`. Il rapporto **non** contiene contenuti conservati
  né impronte: è un documento di sintesi, e più è sintetico meno espone.
- **RT-9 — Registrazione eventi (§14).** Generazione, recapito e scaricamento di un rapporto sono registrati con
  `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, senza dati personali nel registro tecnico.

## 4. Criteri di accettazione

**CA-1 — Il rapporto contiene le cinque parti**
- **Dato** un periodo con 4.000 azioni, 37 approvazioni richieste e 2 scostamenti
- **Quando** l'utente genera il rapporto
- **Allora** il documento riporta conteggi, esito delle approvazioni, i 2 scostamenti, i sigilli del periodo con
  l'esito della verifica e la dichiarazione di completezza

**CA-2 — L'avvertenza è stampata**
- **Dato** un rapporto qualsiasi
- **Quando** lo si apre
- **Allora** in prima pagina compare l'avvertenza che il documento non attesta la conformità a nessuna norma e
  non copre le azioni non dichiarate, nella lingua del rapporto

**CA-3 — La completezza è dichiarata anche quando è imbarazzante**
- **Dato** un periodo con 3 buchi di sequenza e 120 azioni rifiutate per quota esaurita
- **Quando** si genera il rapporto
- **Allora** entrambi i numeri compaiono nella dichiarazione di completezza, e non sono omessi né arrotondati

**CA-4 — Un rapporto consegnato non cambia**
- **Dato** un rapporto già generato per il primo trimestre
- **Quando** si rigenera lo stesso periodo
- **Allora** si ottiene un secondo documento distinto, con il proprio momento di generazione, e il primo resta
  scaricabile identico a com'era

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede il rapporto e prova a scaricare quello di `B`
- **Allora** i conteggi contengono solo dati di `A` e il tentativo di scaricamento risponde `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sui conteggi e sulla dichiarazione di completezza, e di **integrazione** sulla
      generazione e sulla programmazione, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su conteggi e scaricamento;
- [ ] **prova end-to-end**: **rimando** — il percorso `[J-AGENTAUDIT]` della storia 0037 comprende la generazione
      di un rapporto; voce `da-coprire` nel registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) con storia proprietaria `0037`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue (`en, it, fr, es, de`), compreso il testo del documento
      e in particolare l'avvertenza obbligatoria;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per la tabella dei rapporti, con i campi annotati e
      la tabella in `exportData` e `purgeData`;
- [ ] **registro delle decisioni** compilato, con le voci su: immutabilità del rapporto generato, dichiarazione di
      completezza obbligatoria, testo dell'avvertenza e perché c'è;
- [ ] contratto degli **strumenti conversazionali**: dichiarato che la generazione del documento **non** viene
      esposta a un assistente, con il motivo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0024` | I conteggi si calcolano sul livello di interrogazione della cronologia |
| storia `0013` | La parte sui sigilli del periodo esiste solo se i sigilli esistono |
| storia `0027` | Il rapporto riusa il meccanismo di produzione asincrona e di scadenza dei file |
| storia `0023` | Gli scostamenti riportati vengono da lì |
| storia `0011` | La dichiarazione di completezza si regge sui buchi rilevati lì |

## 7. Fuori ambito

- **la personalizzazione grafica del documento** (logo del cliente, colori propri): desiderabile e non
  indispensabile al primo passo; se si farà, sarà una storia a sé;
- **il confronto fra due periodi**: utile, non richiesto da nessuna fonte, rimandato;
- **qualunque affermazione di conformità**: esplicitamente esclusa, per la ragione al §1;
- **la firma del rapporto** da parte nostra come attestazione: il rapporto è un documento di sintesi, la prova sta
  nel pacchetto della storia 0015 — sovrapporre le due cose confonderebbe il cliente su che cosa può dimostrare.

## 8. Punti aperti

- **In quale formato si produce il documento.** Serve un formato stampabile e archiviabile; la produzione di quel
  formato dentro il servizio ha un costo di libreria e di manutenzione che va valutato. Chi chiude: sviluppatore,
  in sede di implementazione.
- **Il testo esatto dell'avvertenza** è materia di revisione legale, non di un agente che scrive documenti: la
  formulazione proposta qui è un punto di partenza, e va aggiunta ai punti da far rivedere prima del rilascio
  (§11, punto 6 della descrizione dell'applicazione).
- **Se il rapporto programmato debba partire anche quando il periodo è vuoto.** Un rapporto che dice «nessuna
  azione dichiarata in questo trimestre» è informazione utile — potrebbe significare che una sorgente si è rotta —
  ma è anche un messaggio in più. Propongo di inviarlo comunque, marcato.
