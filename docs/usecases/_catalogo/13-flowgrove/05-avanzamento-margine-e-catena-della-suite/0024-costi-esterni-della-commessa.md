# 0024 — Costi esterni della commessa

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 05 — Avanzamento, margine e catena della suite
**Storia**: `0024` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un'impresa che compra materiale per il cantiere
> voglio imputare alla commessa quello che ho speso, oltre alle ore
> così che il margine non racconti una bugia.

**Contesto.** Un margine calcolato sulle sole ore è falso per chiunque compri qualcosa: materiali, subappalti,
trasferte, noleggi. Questa storia introduce il costo esterno, con due sorgenti — l'inserimento a mano e l'evento
dall'app delle note spese (08 SpendGrove), quando c'è. È la parte meno vistosa dell'epica ed è quella senza cui
la storia 0026 non vale niente.

## 2. Requisiti funzionali

1. **RF-1** — Su un progetto si registra un costo con descrizione, importo, data, categoria (materiali,
   subappalto, trasferta, noleggio, altro) e un interruttore **riaddebitabile al cliente** sì/no.
2. **RF-2** — I costi riaddebitabili entrano nel lotto fatturabile (storia 0022) come righe distinte dalle ore;
   quelli non riaddebitabili restano solo nel costo della commessa.
3. **RF-3** — Un costo si modifica e si cancella finché non è stato consegnato alla fatturazione; dopo, no.
4. **RF-4** — L'app consuma l'evento «spesa approvata» dall'app delle note spese quando la spesa porta un
   riferimento di commessa: il costo nasce già compilato, in stato `da confermare`, e va confermato da una
   persona prima di entrare nel conto.
5. **RF-5** — La scheda del progetto mostra il totale dei costi per categoria, con la distinzione fra
   riaddebitabile e no.
6. **RF-6** — L'arrivo dello stesso evento due volte non crea due costi: l'elaborazione è idempotente sul
   riferimento della spesa.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `project_cost` filtra per `tenant_id` dal
  token verificato; l'evento porta il `tenant_id` e non può creare costi altrove.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/progetti/v1/projects/{id}/costs` e
  `PATCH|DELETE /api/progetti/v1/costs/{id}`; errori in `application/problem+json`; OpenAPI aggiornata nello
  stesso commit. La sorgente esterna è un consumatore di eventi, non una chiamata sincrona.
- **RT-3 — Persistenza (§8).** Migrazione `V16__costi.sql`: `project_cost` con `tenant_id`, `project_id`,
  importo in **centesimi**, valuta, categoria, `billable_to_customer`, `source`, `source_ref`, stato, colonne di
  controllo e cancellazione logica; tabella di idempotenza degli eventi consumati.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro dei costi nella scheda del progetto, con la coda dei costi «da
  confermare» in evidenza; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Categorie, etichette e messaggi in `en, it, fr, es, de`; gli importi si
  formattano secondo la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Ruolo minimo per registrare e confermare costi:
  `admin` — è informazione economica.
- **RT-7 — Esposizione conversazionale (§12).** I costi entrano nel risultato di `get_project_margin(id)`
  (**lettura**, storia 0028), con il filtro di ruolo. Nessuno strumento di scrittura: registrare un costo dalla
  chat, senza il documento davanti, è un invito all'errore.
- **RT-8 — Dati personali (§10).** Di norma nessun dato personale: il costo riguarda fornitori e materiali. La
  **descrizione è testo libero** e può contenere il nome di una persona (un professionista incaricato): va
  dichiarata come tale nel manifesto in italiano e inglese, e la tabella deve comparire in `exportData` e
  `purgeData`.
- **RT-9 — Registrazione eventi (§14).** «Costo registrato», «costo confermato da evento», «evento duplicato
  ignorato» con `tenant_id`, `app_id`, `user_id`, progetto e importo; mai la descrizione.

## 4. Criteri di accettazione

**CA-1 — Registrazione a mano**
- **Dato** un progetto attivo
- **Quando** si registra un costo di 450 € per materiali, riaddebitabile
- **Allora** compare nel riquadro dei costi e concorre al totale della categoria

**CA-2 — Costo da evento**
- **Dato** un evento «spesa approvata» con riferimento a un progetto esistente
- **Quando** l'app lo elabora
- **Allora** nasce un costo in stato `da confermare`, che **non** concorre al conto finché non viene confermato

**CA-3 — Riaddebito**
- **Dato** un costo riaddebitabile di 450 € e uno non riaddebitabile di 200 €
- **Quando** si compone il lotto fatturabile (storia 0022)
- **Allora** il lotto contiene una riga da 450 € e non la riga da 200 €

**CA-4 — Modifica impedita dopo la consegna**
- **Dato** un costo già consegnato alla fatturazione
- **Quando** si tenta di modificarlo
- **Allora** la risposta è `409` e nulla cambia

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` registra un costo su un progetto di `B`
- **Allora** riceve `404` e nulla viene creato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sul totale per categoria e di **integrazione** sul consumo dell'evento, compresa la
      consegna doppia;
- [ ] prova di **isolamento fra account** su rotte ed evento;
- [ ] **prova end-to-end**: coprire ora — `[J-PROGETTI]` registra un costo riaddebitabile e verifica che compaia
      nel lotto (storia 0031); voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato per `project_cost` con la marcatura «testo libero» sulla descrizione;
- [ ] **registro delle decisioni** compilato, con annotata la conferma umana obbligatoria sui costi da evento;
- [ ] controllo automatico di **accessibilità** verde sul riquadro dei costi;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0006` | Il costo vive su un progetto |
| Storia `0022` | I costi riaddebitabili entrano nel lotto: le due storie vanno tenute coerenti |
| 08 SpendGrove | Deve pubblicare l'evento «spesa approvata» con un riferimento di commessa: contratto da concordare |

## 7. Fuori ambito

- la gestione dei documenti di spesa (ricevute, fatture di acquisto): è di 08 SpendGrove;
- il ricarico automatico sul costo riaddebitato: si riaddebita l'importo, senza margine aggiunto; se servisse,
  è una decisione di prodotto;
- l'ammortamento e i costi indiretti: fuori perimetro.

## 8. Punti aperti

- **Il riferimento di commessa nell'app delle note spese** oggi non è detto che esista. Se 08 SpendGrove non lo
  prevede, l'evento non arriva mai e resta solo l'inserimento a mano: è una richiesta da portare a quell'app, non
  una cosa risolvibile qui.
