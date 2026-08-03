# 0008 — Elenco, ricerca e filtri

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 02 — Anagrafica di contatti e aziende
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come venditore che ha il cliente al telefono
> voglio trovare la sua scheda in pochi secondi scrivendo una parola qualsiasi
> così da non farlo aspettare mentre scorro un elenco.

**Contesto.** Con cinquanta schede l'elenco basta; con duemila — cioè dopo la prima importazione, storia 0025 —
diventa inutilizzabile. La ricerca non è un affinamento: è la funzione che decide se l'app viene usata mentre si
telefona o dopo, e «dopo» significa mai.

## 2. Requisiti funzionali

1. **RF-1** — Una casella di ricerca a testo libero cerca contemporaneamente su nome e cognome del contatto,
   denominazione dell'azienda, posta elettronica e telefono, restituendo i risultati ordinati per pertinenza.
2. **RF-2** — I filtri disponibili sono: responsabile, origine del contatto, azienda, e «archiviati sì/no».
3. **RF-3** — L'elenco è paginato con dimensione della pagina e totale, e mantiene ricerca e filtri quando si
   torna indietro da una scheda.
4. **RF-4** — Quando nessun risultato corrisponde, lo stato vuoto dice cosa fare (azzerare i filtri, creare la
   scheda) e non è un vicolo cieco.
5. **RF-5** — La ricerca ignora accenti e differenze fra maiuscole e minuscole, e trova anche una corrispondenza
   parziale all'inizio di parola.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** La ricerca filtra `WHERE tenant_id = :tid` **prima** di qualunque
  criterio di pertinenza: un difetto qui farebbe uscire schede altrui in cima ai risultati.
- **RT-2 — Interfaccia di programmazione (§2).** Parametri di ricerca e filtro su
  `GET /api/sales/v1/contacts` e `GET /api/sales/v1/companies`; paginazione a pagina/dimensione con totale;
  errori in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Indici di supporto alla ricerca sullo schema `app_sales`, tutti a partire da
  `tenant_id`; nessuna interrogazione fra schemi.
- **RT-4 — Modulo frontend (§3, §5).** Barra degli strumenti con ricerca, filtri e conteggio dei risultati; stato
  vuoto con azione; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette dei filtri, testo dello stato vuoto e conteggi presenti in
  `en, it, fr, es, de`, con le forme singolare e plurale corrette per ogni lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota; valgono i varchi ordinari.
- **RT-7 — Esposizione conversazionale (§12).** I parametri di ricerca definiti qui sono gli stessi che
  `list_contacts` esporrà nella storia 0034: si definiscono una volta sola.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo. La ricerca **non** registra i termini cercati: un
  termine di ricerca è spesso un nome, e finirebbe nei registri (§14).
- **RT-9 — Registrazione eventi (§14).** Si registrano il numero di risultati e la durata, **non** il testo
  cercato.

## 4. Criteri di accettazione

**CA-1 — Ricerca trasversale**
- **Dato** un contatto «Giulia Bianchi» dell'azienda «Alfa Utensili» con indirizzo `g.bianchi@alfa.test`
- **Quando** l'utente cerca «alfa»
- **Allora** trova sia l'azienda sia il contatto, e la scheda si apre dal risultato

**CA-2 — Accenti e maiuscole**
- **Dato** un contatto «Niccolò Verdi»
- **Quando** l'utente cerca «niccolo»
- **Allora** lo trova

**CA-3 — Nessun risultato**
- **Dato** una ricerca che non corrisponde a nulla
- **Quando** l'elenco si aggiorna
- **Allora** compare lo stato vuoto con l'azione per azzerare i filtri, e il conteggio dice zero

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B` con contatti omonimi
- **Quando** un utente di `A` cerca quel nome
- **Allora** trova solo il proprio, anche forzando l'identificativo di `B` nella richiesta

**CA-5 — I termini cercati non finiscono nei registri**
- **Dato** una ricerca per «Bianchi»
- **Quando** si ispezionano le righe di registro
- **Allora** contengono il conteggio dei risultati e non il termine cercato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla normalizzazione del testo cercato e di **integrazione** sulla ricerca paginata;
- [ ] prova di **isolamento fra account** sulla ricerca, con contatti omonimi nei due account;
- [ ] **prova end-to-end**: rimando alla storia 0037;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con plurali corretti;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di non registrare i termini di ricerca;
- [ ] contratto degli **strumenti conversazionali**: parametri allineati a quelli previsti per `list_contacts`;
- [ ] controllo automatico di **accessibilità** verde sulla barra degli strumenti;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0006` e `0007` | Serve qualcosa da cercare |

## 7. Fuori ambito

- la ricerca dentro le note e i testi liberi: deliberatamente esclusa, perché renderebbe cercabile testo che può
  contenere di tutto (§6 della descrizione dell'applicazione);
- la ricerca sulle trattative: storia 0018, dove serve il portafoglio del responsabile;
- il salvataggio di filtri preferiti: non previsto in questa proposta.

## 8. Punti aperti

- Nessuno.
