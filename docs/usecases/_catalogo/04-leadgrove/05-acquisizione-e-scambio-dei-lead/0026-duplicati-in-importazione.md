# 0026 — Duplicati in importazione

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 05 — Acquisizione e scambio dei lead
**Storia**: `0026` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0010`, `0025`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che carica il secondo file della sua rubrica
> voglio che l'app riconosca chi ho già dentro e mi chieda cosa fare
> così da non ritrovarmi con l'archivio raddoppiato dopo dieci minuti di lavoro.

**Contesto.** La prima importazione è pulita; la seconda no. Chi importa lo fa più volte — la rubrica del
telefono, poi l'elenco di una fiera, poi il vecchio foglio ritrovato — e ogni volta l'archivio esistente è il
problema. La differenza con la fusione (storia 0010) è il momento: qui si decide **prima** di scrivere, quindi si
può evitare il danno invece di ripararlo.

## 2. Requisiti funzionali

1. **RF-1** — Nell'anteprima dell'importazione, ogni riga che corrisponde a un contatto o a un'azienda già in
   archivio è segnalata, con l'indicazione di quale scheda ha trovato e per quale criterio.
2. **RF-2** — L'utente sceglie una politica valida per tutto il caricamento: **saltare** le righe già presenti,
   **aggiornare** i campi vuoti della scheda esistente, oppure **creare comunque**.
3. **RF-3** — La politica «aggiorna» non sovrascrive mai un valore già presente con uno diverso: riempie solo i
   campi vuoti. Sovrascrivere in blocco è una perdita di dati travestita da comodità.
4. **RF-4** — La singola riga può essere trattata diversamente dalla politica generale, direttamente
   nell'anteprima.
5. **RF-5** — Il riepilogo finale dice quante righe sono state create, quante saltate, quante hanno aggiornato una
   scheda esistente e quante scartate.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il confronto avviene solo con l'archivio dell'account del token
  verificato.
- **RT-2 — Interfaccia di programmazione (§2).** La politica e le eccezioni per riga viaggiano nel corpo di
  `POST /api/sales/v1/imports/{id}/confirm`; l'anteprima di
  `GET /api/sales/v1/imports/{id}/preview` riporta le corrispondenze trovate; errori in
  `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova; ogni `import_row` registra l'esito («creata», «saltata»,
  «aggiornata», «scartata») e l'identificativo della scheda coinvolta.
- **RT-4 — Modulo frontend (§3, §5).** Segnalazione nell'anteprima con il motivo della corrispondenza e il
  selettore della politica; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Nomi delle politiche, motivi di corrispondenza e riepilogo in
  `en, it, fr, es, de`, con i plurali corretti.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota; valgono i vincoli di ruolo della storia 0025.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: l'importazione non è esposta alla chat. La
  logica di corrispondenza però è la stessa che `create_lead` (storia 0035) usa per avvisare «esiste già un
  contatto simile» prima di creare: si scrive una volta sola.
- **RT-8 — Dati personali (§10).** Nessuna voce nuova. Nota: la politica «aggiorna» modifica dati di persone già
  in archivio, quindi va tracciata nella cronologia della scheda (storia 0022) come evento di sistema.
- **RT-9 — Registrazione eventi (§14).** Il riepilogo si registra con i quattro conteggi, senza contenuti.

## 4. Criteri di accettazione

**CA-1 — Riconoscimento**
- **Dato** un archivio con il contatto `g.bianchi@alfa.test` e un file che lo contiene di nuovo
- **Quando** si apre l'anteprima
- **Allora** la riga è segnalata come già presente, con il criterio «stesso indirizzo di posta elettronica»

**CA-2 — Politica «salta»**
- **Dato** la politica «salta» e 10 righe di cui 3 già presenti
- **Quando** l'utente conferma
- **Allora** vengono creati 7 contatti, 3 risultano saltati e nessuna scheda esistente viene toccata

**CA-3 — Politica «aggiorna» non sovrascrive**
- **Dato** una scheda con telefono valorizzato e una riga in arrivo con un telefono **diverso** e un ruolo che la
  scheda non ha
- **Quando** si applica la politica «aggiorna»
- **Allora** il ruolo viene riempito e il telefono resta quello originale

**CA-4 — Eccezione per riga**
- **Dato** la politica «salta» e una riga che l'utente marca come «crea comunque»
- **Quando** conferma
- **Allora** quella riga crea una scheda nuova e le altre corrispondenti restano saltate

**CA-5 — Isolamento fra account**
- **Dato** un contatto con lo stesso indirizzo nell'account `B`
- **Quando** l'account `A` importa quella riga
- **Allora** non viene segnalata alcuna corrispondenza e il contatto viene creato in `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sui criteri di corrispondenza e sulla regola «non sovrascrivere», di **integrazione**
      sull'esecuzione con le tre politiche;
- [ ] prova di **isolamento fra account** sul confronto con l'archivio;
- [ ] **prova end-to-end**: nessun impatto sul percorso minimo; coperta da prove d'integrazione, con il motivo nel
      registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che l'aggiornamento lasci traccia nella cronologia;
- [ ] **registro delle decisioni** compilato, con annotata la regola «aggiorna riempie, non sovrascrive»;
- [ ] contratto degli **strumenti conversazionali**: logica di corrispondenza condivisa con `create_lead`;
- [ ] controllo automatico di **accessibilità** verde sull'anteprima;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0025` | Il riconoscimento vive dentro la procedura di importazione |
| Storia `0010` | Riusa i criteri di somiglianza già definiti per la fusione |

## 7. Fuori ambito

- la fusione di schede già in archivio: storia 0010;
- il riconoscimento fra righe **dentro** lo stesso file: già coperto dalla storia 0025;
- la corrispondenza per somiglianza fonetica del nome: fuori perimetro, produce troppi falsi positivi.

## 8. Punti aperti

- Nessuno.
