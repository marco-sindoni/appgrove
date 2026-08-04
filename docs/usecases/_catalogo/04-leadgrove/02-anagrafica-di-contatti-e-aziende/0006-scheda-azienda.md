# 0006 — Scheda azienda

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 02 — Anagrafica di contatti e aziende
**Storia**: `0006` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0003`, `0004` — è la prima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come venditore di una micro-impresa
> voglio registrare l'organizzazione con cui sto trattando, con i suoi dati essenziali
> così da avere un posto solo dove sta scritto chi è, invece di ricostruirlo ogni volta dalla posta.

**Contesto.** L'azienda è il perno dell'anagrafica condivisa che il catalogo (§6) indica come cuore della suite:
è a lei che si agganciano contatti, trattative e — nelle altre app — preventivi e fatture. Si fa prima del
contatto perché un contatto senza azienda è una rubrica, non un archivio commerciale. I campi restano pochi di
proposito: l'analisi di mercato dice che l'eccesso di campi è la prima causa di abbandono
([application-description.md](../application-description.md) §2.5).

## 2. Requisiti funzionali

1. **RF-1** — Un utente con un posto può creare un'azienda indicando la denominazione (obbligatoria) e,
   facoltativamente, settore, sito, telefono, indirizzo e partita IVA.
2. **RF-2** — Può modificarla e archiviarla; l'archiviazione è una cancellazione **logica** e la scheda resta
   consultabile nello storico, non sparisce.
3. **RF-3** — Ogni azienda ha un responsabile (`owner_user_id`), impostato di norma su chi la crea e
   modificabile.
4. **RF-4** — La scheda mostra i contatti collegati e le trattative collegate, con i loro conteggi.
5. **RF-5** — Non si possono creare due aziende attive con la stessa denominazione nello stesso account senza una
   conferma esplicita: l'avviso appare **prima** del salvataggio, non dopo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `Company` filtra per `tenant_id` preso dal
  token verificato; un `tenant_id` dal corpo o dai parametri viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST|GET|PATCH|DELETE /api/sales/v1/companies[/{id}]`;
  oggetti di trasferimento al bordo con validazione dichiarativa; errori in `application/problem+json`; paginazione
  a pagina/dimensione con totale; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `company` già creata dalla storia 0002; qui si aggiunge l'indice per la
  ricerca su denominazione, sempre a partire da `tenant_id`.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Aziende del modulo `sales`: elenco, scheda di dettaglio e modulo
  di inserimento; dati letti con il client generato; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili, etichette dei campi e messaggi di errore compresi,
  presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: la metrica è `seats`, non il numero di aziende. Chi
  non ha un posto riceve `403`; con abbonamento non attivo `402`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo in questa storia: la lettura delle aziende
  entra in `list_contacts`/`summarize_account` nell'epica 07. Dipendenza dichiarata: UC 0061-0063.
- **RT-8 — Dati personali (§10).** `company.name` è già dichiarata nel manifesto (storia 0002) perché una ditta
  individuale ha come denominazione il nome di una persona. Nessuna voce nuova, ma la tabella deve restare in
  `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** «Azienda creata», «azienda modificata», «azienda archiviata» registrati
  con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, **senza** la denominazione.

## 4. Criteri di accettazione

**CA-1 — Creazione**
- **Dato** un utente con un posto attivo
- **Quando** crea un'azienda con la sola denominazione
- **Allora** la scheda esiste, il responsabile è l'utente stesso e compare nell'elenco

**CA-2 — Denominazione mancante**
- **Dato** lo stesso utente
- **Quando** salva senza denominazione
- **Allora** riceve `400` in `application/problem+json` con l'indicazione del campo, e nulla viene creato

**CA-3 — Possibile doppione**
- **Dato** un'azienda «Alfa Utensili» già presente
- **Quando** l'utente ne crea una seconda con la stessa denominazione
- **Allora** l'interfaccia avverte prima del salvataggio e chiede conferma esplicita

**CA-4 — Archiviazione logica**
- **Dato** un'azienda con due contatti collegati
- **Quando** l'utente la archivia
- **Allora** sparisce dagli elenchi attivi, i contatti restano, e la riga porta `deleted_at` valorizzato

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`, ciascuno con le proprie aziende
- **Quando** un utente di `A` chiede l'elenco forzando l'identificativo di `B`
- **Allora** vede solo le proprie

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla rilevazione del possibile doppione e di **integrazione** sulla risorsa, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** sulla risorsa `companies`;
- [ ] **prova end-to-end**: rimando alla storia 0037, proprietaria del percorso `[J-SALES]`, dove la creazione di
      un'azienda è il primo passo;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova; verificato che `company` resti in esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali**: nessuno in questa storia, con il rimando all'epica 07;
- [ ] controllo automatico di **accessibilità** verde su elenco e modulo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0002` | Serve la tabella `company` |
| Storia `0004` | Senza posti nessuno può scrivere |

## 7. Fuori ambito

- la ricerca e i filtri dell'elenco: storia 0008 (qui basta l'elenco paginato);
- i campi personalizzati: storia 0009;
- l'unione di due aziende doppie: storia 0010 (qui ci si limita ad avvisare);
- l'evento «azienda creata» verso le altre app della suite: dipende dal contratto degli eventi condivisi, che non
  esiste ancora ([application-description.md](../application-description.md) §11.4).

## 8. Punti aperti

- **Chi possiede l'anagrafica quando la suite sarà integrata** — se anche l'app di fatturazione può modificare una
  azienda, serve una regola di precedenza. Non è una decisione di questa storia: la chiude l'architettura di
  piattaforma.
