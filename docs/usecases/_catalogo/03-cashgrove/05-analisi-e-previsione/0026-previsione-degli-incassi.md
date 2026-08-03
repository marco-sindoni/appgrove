# 0026 — Previsione degli incassi

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 05 — Analisi e previsione
**Storia**: `0026` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0024`, `0025`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che deve decidere se pagare il fornitore adesso o fra due settimane
> voglio una stima di quanto denaro rientrerà nelle prossime settimane
> così da prendere la decisione con un numero davanti invece che a sensazione.

**Contesto.** È la parte «cash flow» del nome dell'app, e la ragione per cui la scheda di catalogo la classifica come
finanza e non come amministrazione. La previsione non serve a essere esatta — non lo sarà mai — serve a evitare la
sorpresa. Per questo la regola di questa storia è una sola e vale più di tutte le altre: **le ipotesi si dichiarano**.
Una previsione che non dice da dove viene è un numero inventato con l'aria di essere un dato.

## 2. Requisiti funzionali

1. **RF-1** — L'app stima, per le prossime settimane fino a un orizzonte scelto, quanto denaro rientrerà, distribuito
   per settimana.
2. **RF-2** — La stima si basa su: crediti non ancora scaduti alla loro scadenza, corretta per il ritardo medio storico
   del debitore (o dell'account, se il debitore non ha storico); crediti già scaduti, corretti per la probabilità di
   incasso desunta dalla fascia di rischio; promesse di pagamento attive alla data promessa.
3. **RF-3** — Ogni settimana della previsione mostra la parte «quasi certa» (promesse e crediti non scaduti di debitori
   affidabili) distinta da quella «incerta».
4. **RF-4** — Le **ipotesi usate** sono sempre visibili accanto alla previsione, in parole comprensibili, con la
   possibilità di aprirne il dettaglio.
5. **RF-5** — L'app conserva una fotografia settimanale della previsione, così da poterla confrontare con quello che è
   davvero successo.
6. **RF-6** — Se lo storico non basta a stimare i ritardi, la previsione si limita alle scadenze nominali e lo
   **dichiara**, invece di applicare correzioni inventate.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni calcolo filtra per `tenant_id` preso dal token verificato; nessun
  parametro di stima è ricavato da altri account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/crediti/v1/previsione` (con orizzonte) e
  `GET /api/crediti/v1/previsione/storico`; errori in `application/problem+json`; definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `previsione_incassi` sullo schema `app_crediti` (istante,
  orizzonte, fasce temporali, importi, ipotesi usate) con `tenant_id`, chiave UUID versione 7, colonne di controllo e
  cancellazione logica. La fotografia settimanale la scrive la lavorazione programmata.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro della previsione nella *Panoramica*, con le settimane, la distinzione
  fra parte quasi certa e incerta, e il pannello delle ipotesi; solo token del sistema di design; tema chiaro e scuro.
  Il grafico ha una alternativa testuale.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, comprese le formulazioni delle ipotesi, passano dallo
  spazio-nomi `crediti` e sono presenti in `en, it, fr, es, de`. Le ipotesi sono frasi, non formule: vanno scritte in
  modo che le capisca chi non fa questo mestiere.
- **RT-6 — Varchi e quota (§6, §7).** Non consuma quota; accessibile anche in sola lettura.
- **RT-7 — Esposizione conversazionale (§12).** `previsione_incassi(orizzonte_giorni) → importi attesi per settimana,
  con le ipotesi usate` è dichiarato qui come strumento di **lettura**, raccolto nel contratto della storia `0028`. Il
  risultato **deve** contenere le ipotesi: uno strumento che restituisce solo il numero fa dire a un assistente una
  cosa che l'app non ha detto.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: la previsione è aggregata. La tabella è comunque
  aggiunta a `exportData` come dato del cliente.
- **RT-9 — Registrazione eventi (§14).** L'evento «fotografia della previsione scritta» è registrato con `tenant_id`,
  `app_id`, «sistema» e identificativo di correlazione, senza importi.

## 4. Criteri di accettazione

**CA-1 — Previsione di base**
- **Dato** tre crediti non scaduti con scadenze nelle prossime tre settimane e un debitore senza storico di ritardo
- **Quando** si chiede la previsione a 30 giorni
- **Allora** ogni importo compare nella settimana della propria scadenza e le ipotesi dichiarano «scadenze nominali,
  nessuna correzione: storico insufficiente»

**CA-2 — Correzione per ritardo storico**
- **Dato** un debitore che paga sistematicamente 20 giorni dopo la scadenza
- **Quando** si chiede la previsione
- **Allora** il suo credito compare circa tre settimane dopo la scadenza nominale, e le ipotesi dicono «corretto di 20
  giorni sul ritardo medio del debitore»

**CA-3 — Promesse**
- **Dato** una promessa di pagamento attiva per la settimana prossima · **Quando** si chiede la previsione · **Allora**
  l'importo compare in quella settimana, nella parte «quasi certa»

**CA-4 — Le ipotesi ci sono sempre**
- **Dato** una qualsiasi previsione · **Quando** la si legge, dall'interfaccia o dallo strumento conversazionale ·
  **Allora** le ipotesi sono presenti e leggibili, senza doverle chiedere

**CA-5 — Confronto con il reale**
- **Dato** una previsione di quattro settimane fa · **Quando** si apre lo storico · **Allora** si vede la previsione di
  allora accanto agli incassi effettivamente avvenuti

**CA-6 — Isolamento fra account**
- **Dato** due account con storici diversi · **Quando** ciascuno chiede la previsione · **Allora** i parametri di
  correzione sono quelli del proprio account

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend e frontend);
- [ ] prove di **unità** su ciascuna regola di correzione e sul caso «storico insufficiente», di **integrazione** sulla
      fotografia settimanale;
- [ ] prova di **isolamento fra account** sui parametri di stima;
- [ ] **prova end-to-end**: *nessun impatto* — la previsione è una lettura derivata; le prove di integrazione bastano;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con le ipotesi scritte in parole comuni;
- [ ] **manifesto dei dati** aggiornato con `previsione_incassi`, presente in esportazione;
- [ ] **registro delle decisioni** compilato, in particolare sul modello di stima scelto e sul perché non è più
      complesso;
- [ ] contratto degli **strumenti conversazionali**: `previsione_incassi` dichiarato come lettura, con ipotesi
      obbligatorie nel risultato;
- [ ] **accessibilità**: grafico con alternativa testuale;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0024` | I ritardi medi sono il parametro di correzione principale |
| storia `0025` | Le fasce di rischio danno la probabilità di incasso dei crediti già scaduti |

## 7. Fuori ambito

- La previsione **delle uscite** e quindi il saldo di cassa complessivo: richiederebbe i debiti verso fornitori, che
  l'app non conosce. È il confine fra questa app e un programma di tesoreria, e va tenuto.
- Modelli statistici elaborati: la previsione deve essere spiegabile in tre frasi, altrimenti nessuno se ne fida.
- La lettura dei movimenti bancari per verificare la previsione: punto aperto n. 10 del documento capofila §11.

## 8. Punti aperti

**Le probabilità di incasso associate alle fasce di rischio** sono una scelta di modello che nessuna fonte consultata
fonda su dati del segmento. Vanno confermate dallo sviluppatore e, soprattutto, **riviste con i dati reali** dopo
qualche mese di esercizio: la fotografia settimanale (RF-5) esiste proprio per rendere possibile quella revisione.
