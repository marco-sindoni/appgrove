# 0007 — Scheda contatto

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 02 — Anagrafica di contatti e aziende
**Storia**: `0007` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come venditore
> voglio registrare la persona con cui parlo davvero, con il suo ruolo e i suoi recapiti
> così da sapere chi chiamare senza cercare nella posta di sei mesi fa.

**Contesto.** L'azienda non risponde al telefono: risponde una persona. Il contatto è l'entità su cui si regge
tutto il resto — attività, note, consensi — ed è anche la prima che tratta **dati personali** in senso pieno.
Questa storia è quindi il punto in cui il manifesto dei dati smette di essere teoria.

## 2. Requisiti funzionali

1. **RF-1** — Un utente con un posto può creare un contatto indicando almeno il nome, e facoltativamente cognome,
   ruolo, posta elettronica, telefono e azienda di appartenenza.
2. **RF-2** — Ogni contatto porta l'**origine** (inserito a mano, importato, arrivato dal modulo web), impostata
   dal sistema e non modificabile a mano: è la prima domanda di ogni verifica sulla liceità del contatto.
3. **RF-3** — Può modificare e archiviare il contatto; l'archiviazione è logica.
4. **RF-4** — Un contatto può esistere senza azienda (una persona conosciuta a una fiera) e può essere collegato a
   un'azienda in un secondo momento.
5. **RF-5** — La scheda mostra azienda, trattative collegate, ultime attività e lo stato delle preferenze di
   contatto (che arrivano dalla storia 0011: fino ad allora la sezione dichiara che non è ancora disponibile).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `Contact` filtra per `tenant_id` preso dal
  token verificato; un `tenant_id` dal corpo viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST|GET|PATCH|DELETE /api/sales/v1/contacts[/{id}]`;
  validazione dichiarativa (formato della posta elettronica, lunghezze); errori in `application/problem+json`;
  paginazione con totale; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `contact` già creata dalla storia 0002; qui si aggiungono gli indici per
  posta elettronica e per azienda, sempre a partire da `tenant_id`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Contatti: elenco, scheda, modulo di inserimento; client generato;
  solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: il numero di contatti **non** è la metrica, di
  proposito (§3 della descrizione dell'applicazione). Chi non ha un posto riceve `403`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo qui; `list_contacts` e `get_contact`
  arrivano nella storia 0034, `create_lead` nella 0035. Dipendenza dichiarata: UC 0061-0063.
- **RT-8 — Dati personali (§10).** È la storia che rende vere le voci `contact.name`, `contact.email`,
  `contact.phone`, `contact.role`, `contact.source` del manifesto: campi annotati `@PersonalData`, voci in
  italiano e inglese, tabella presente in `exportData` e `purgeData` con cancellazione **fisica**.
- **RT-9 — Registrazione eventi (§14).** «Contatto creato», «contatto modificato», «contatto archiviato»
  registrati con identificativi soltanto: **mai** nome, indirizzo di posta o numero di telefono nei registri.

## 4. Criteri di accettazione

**CA-1 — Creazione minima**
- **Dato** un utente con un posto
- **Quando** crea un contatto con il solo nome
- **Allora** la scheda esiste, l'origine è «inserito a mano» e compare nell'elenco

**CA-2 — Posta elettronica non valida**
- **Dato** lo stesso utente
- **Quando** salva un contatto con un indirizzo malformato
- **Allora** riceve `400` in `application/problem+json` con l'indicazione del campo, e nulla viene creato

**CA-3 — Origine non falsificabile**
- **Dato** una richiesta di creazione che tenta di impostare l'origine a «modulo web»
- **Quando** la richiesta arriva al servizio
- **Allora** il valore imposto dal client viene ignorato e l'origine resta «inserito a mano»

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con i propri contatti
- **Quando** un utente di `A` chiede la scheda di un contatto di `B` per identificativo
- **Allora** riceve `404`, non `403`: l'esistenza di una scheda altrui non si rivela

**CA-5 — I registri non contengono dati personali**
- **Dato** la creazione di un contatto
- **Quando** si ispezionano le righe di registro prodotte
- **Allora** contengono `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e l'identificativo del
  contatto, e **nessun** nome o recapito

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla validazione e di **integrazione** sulla risorsa, con database effimero;
- [ ] prova di **isolamento fra account** sulla risorsa `contacts`;
- [ ] **prova end-to-end**: rimando alla storia 0037, dove la creazione del contatto è il secondo passo del
      percorso `[J-SALES]`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** verificato in italiano e inglese, campi annotati `@PersonalData`, tabella `contact`
      presente in esportazione e cancellazione, con prova che la cancellazione è fisica;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali**: rimando all'epica 07;
- [ ] controllo automatico di **accessibilità** verde su elenco e modulo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0006` | Il contatto si aggancia all'azienda |
| Conferma della classificazione dei dati personali | È la prima storia che tratta dati di persone in senso pieno |

## 7. Fuori ambito

- consensi e preferenze di contatto: storia 0011;
- ricerca e filtri: storia 0008;
- unione dei doppioni: storia 0010;
- importazione massiva: storia 0025.

## 8. Punti aperti

- Nessuno che spetti a questa storia. La durata di conservazione dei contatti resta quella del manifesto, decisa
  dallo sviluppatore.
