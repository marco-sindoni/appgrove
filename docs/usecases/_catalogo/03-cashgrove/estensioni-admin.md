# CashGrove — estensioni della console di amministrazione

**Applicazione**: 03 — CashGrove (`crediti`) · **Stato del documento**: 🟡 bozza d'autore
**Ultimo aggiornamento**: 2026-08-03
**Documento capofila**: [application-description.md](application-description.md)

## 1. Esito

**B — Servono le estensioni descritte qui sotto.**

CashGrove è l'unica app fin qui del suo gruppo che **manda messaggi verso persone che non sono nostri utenti**,
attraverso fornitori configurati dal cliente. Quando un cliente scrive «i miei solleciti non partono più», la scheda
comune dell'account non basta a rispondere: la causa può essere un mittente scaduto, un fornitore che respinge, una coda
ferma o una sospensione che nessuno ricorda di aver messo. Servono quindi **viste diagnostiche su metadati** e una
deroga temporanea al tetto dei crediti monitorati per il caricamento iniziale. Non serve nulla che dia accesso ai
contenuti: chi amministra la piattaforma non entra nell'account del cliente e non legge i suoi solleciti.

## 2. Parametri di configurazione per account

| Parametro | Che cosa governa | Valore predefinito | Chi può cambiarlo | Perché non è nell'app |
|---|---|---|---|---|
| `invii_sospesi` | Interruttore che ferma **tutti** gli invii dell'account | spento | amministratore di piattaforma | È una misura di contenimento: si usa quando un account sta producendo segnalazioni di abuso o quando un fornitore ci minaccia il blocco. Metterlo nelle mani del cliente non avrebbe senso: lui ha già la sospensione per credito e per debitore |
| `tetto_righe_importazione` | Numero massimo di righe per singola importazione | 5.000 | amministratore di piattaforma | È un limite di protezione del servizio, non una funzione: alzarlo per un cliente in migrazione è una decisione operativa |
| `limite_frequenza_pagina_pubblica` | Richieste ammesse su un collegamento pubblico prima del blocco temporaneo | valore prudente | amministratore di piattaforma | È un parametro di sicurezza contro il tentativo di indovinare i gettoni: se lo vedesse il cliente, lo vedrebbe anche chi attacca |

## 3. Quote e deroghe

- **Metrica governata**: `crediti_monitorati` (natura `stock`).
- **Serve una deroga manuale?** **Sì.** Il caso è reale e ricorrente: un cliente nuovo importa lo storico dei crediti
  aperti e supera per qualche settimana il tetto del piano, mentre incassa e chiude quelli vecchi. Senza deroga, la
  scelta è fra bloccare la migrazione e vendergli un piano che non gli serve stabilmente.
- **Forma della deroga**: tetto alternativo **con data di scadenza obbligatoria**. Alla scadenza il tetto torna quello
  del piano; i crediti eccedenti già dentro **non** vengono cancellati, ma non se ne possono aggiungere finché il
  conteggio non rientra.
- **Tracciamento**: ogni deroga porta chi l'ha concessa, quando, fino a quando, con quale tetto e per quale motivo
  scritto.
- **Limite**: una deroga non è uno sconto e non cambia l'abbonamento. Se il cliente ha bisogno stabilmente di più
  crediti monitorati, passa di piano. Una deroga rinnovata tre volte è un problema commerciale travestito da problema
  tecnico.

## 4. Viste operative e diagnostiche

| Vista | Cosa mostra | A che domanda risponde | Dati esposti |
|---|---|---|---|
| **Stato dei canali di invio** | Per account: mittente di posta configurato sì/no, esito dell'ultima verifica e quando, fornitore di messaggi brevi collegato sì/no, esito dell'ultima verifica | «Perché il cliente dice che non partono i solleciti?» | Metadati: stato, istante, codice di errore del fornitore. **Nessun indirizzo, nessuna credenziale, nessun testo** |
| **Coda e arretrato degli invii** | Per account: quanti invii in coda, quanti slittati e per quale motivo (fuori finestra, frequenza massima, mittente non verificato), il più vecchio in coda | «C'è un accumulo? È un problema nostro o suo?» | Conteggi e motivi, nessun destinatario |
| **Esiti di trasmissione aggregati** | Per account e per periodo: percentuali di accettati, respinti, non recapitati | «Il recapito sta peggiorando?» | Percentuali e conteggi, nessun indirizzo |
| **Lavorazioni programmate** | Ultima esecuzione, durata, conteggi per account, errori delle lavorazioni quotidiane (scadenza dei crediti, maturazione degli invii, ricalcolo dei punteggi, fotografia della previsione) | «Le lavorazioni girano?» | Conteggi e istanti |
| **Uso della quota** | Per account: crediti monitorati, tetto, deroghe attive, quante volte è stato risposto `429` nell'ultimo mese | «Ha bisogno di cambiare piano o di una deroga?» | Conteggi |
| **Segnalazioni dalla pagina pubblica** | Per account: quante segnalazioni ricevute e quante ancora non verificate | «Il cliente si accorge di quello che gli arriva?» | Conteggi, nessun contenuto |

**Regola comune.** Nessuna di queste viste mostra debitori, recapiti, importi o testi. Quando l'assistenza ha bisogno di
un dettaglio che le viste non danno, la strada è chiedere al cliente di guardare e riferire, non guardare al posto suo.

## 5. Azioni di supporto

| Azione | Quando serve | Reversibile? | Tracciamento | Rischi |
|---|---|---|---|---|
| **Concedere una deroga alla quota** | Migrazione iniziale che supera il tetto | sì (scade da sola) | Riga con operatore, motivo, tetto e scadenza | Deroghe rinnovate all'infinito diventano uno sconto occulto |
| **Sospendere tutti gli invii di un account** | Segnalazioni di abuso, fornitore che minaccia il blocco, sospetto di configurazione errata su larga scala | sì | Riga con operatore e motivo; il cliente ne è informato | Ferma un servizio che il cliente paga: richiede un motivo scritto e una comunicazione |
| **Ripetere una lavorazione programmata fallita** | La lavorazione quotidiana è saltata per un guasto | sì, se idempotente — e lo è per costruzione | Riga con operatore e motivo | Se l'idempotenza si rompesse, invii doppi verso i debitori: è il rischio più serio dell'elenco e va provato, non assunto |
| **Revocare tutti i collegamenti pubblici di un account** | Sospetto che i gettoni siano stati esposti | **no** (i collegamenti già inviati smettono di funzionare) | Riga con operatore e motivo | I debitori che aprono un vecchio collegamento non vedono più nulla: va fatto solo con una ragione di sicurezza |

**Regole comuni.** Ogni azione richiede un motivo scritto; le azioni **irreversibili** o con effetti verso l'esterno
richiedono una conferma esplicita e non sono mai automatiche; nessuna azione dà accesso ai contenuti dell'account.
**Non esiste** e non deve esistere una azione «manda un sollecito di prova per conto del cliente»: sarebbe un messaggio
verso un terzo mandato da chi non è parte del rapporto.

## 6. Dati esposti alla console

| Dato | Genere | Contiene dati personali? | Perché serve |
|---|---|---|---|
| Conteggio dei crediti monitorati per account | metrica | no | Diagnosi delle quote e valutazione del piano |
| Conteggio degli invii in coda, slittati, falliti per account | metrica | no | Diagnosi degli accumuli |
| Percentuali di esito della trasmissione per account | metrica | no | Sorveglianza del recapito |
| Stato e istante dell'ultima verifica del mittente e del fornitore di messaggi brevi | metadato | **no**, purché **non** si mostri l'indirizzo del mittente | Diagnosi «non partono i solleciti» |
| Codice di errore restituito dal fornitore | metadato | no | Distinguere un problema di configurazione da un guasto |
| Numero di risposte `429` nell'ultimo mese | metrica | no | Capire se il cliente sta sbattendo contro il tetto |
| Esiti e istanti delle lavorazioni programmate | metadato | no | Sorveglianza del servizio |

**Verifica obbligatoria.** Nessuna riga di questa tabella contiene dati personali, e questa assenza è **una scelta da
proteggere**: l'indirizzo del mittente è il dato che più facilmente ci finirebbe dentro «perché è comodo per il
supporto», ed è un dato personale a tutti gli effetti. Se un giorno servisse davvero, va dichiarato nel manifesto e
motivato: l'accesso amministrativo è un trattamento come gli altri.

## 7. Punti aperti

- **Chi informa il cliente quando gli invii vengono sospesi d'ufficio**, e con quale testo: è una decisione operativa e
  di comunicazione che non spetta a un agente. La chiude lo sviluppatore.
- **Soglia di allarme sul tasso di mancato recapito** oltre la quale l'assistenza contatta il cliente: nessuna fonte
  consultata indica una soglia di riferimento. Da definire con i primi dati reali.
- **Se la console debba mostrare il numero di debitori per account**: è un conteggio, quindi non è un dato personale, ma
  è anche una informazione commerciale sul cliente. Da decidere insieme alle altre metriche di piattaforma.
