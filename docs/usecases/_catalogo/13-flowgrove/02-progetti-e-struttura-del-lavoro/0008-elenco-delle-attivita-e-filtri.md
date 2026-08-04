# 0008 — Elenco delle attività e filtri

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 02 — Progetti e struttura del lavoro
**Storia**: `0008` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile che segue sei commesse insieme
> voglio un elenco di tutte le attività, filtrabile e ordinabile, che attraversi i progetti
> così da rispondere a «cosa è in ritardo» e «cosa manca per chiudere» senza aprire sei schede.

**Contesto.** La vista dentro il progetto (storia 0007) risponde alla domanda «come sta questo lavoro»; manca la
domanda trasversale, che è quella del responsabile. È anche la vista che alimenta la ricerca conversazionale
(`search_tasks`, storia 0028): conviene che il filtro sia uno solo, condiviso fra interfaccia e strumenti, invece
di due implementazioni che divergono.

## 2. Requisiti funzionali

1. **RF-1** — L'elenco mostra le attività di **tutti** i progetti dell'account, con progetto, titolo, stato,
   assegnatario, scadenza e stima.
2. **RF-2** — Si filtra per progetto, stato, assegnatario, intervallo di scadenza e per la condizione «in
   ritardo» (scadenza superata e attività non terminata).
3. **RF-3** — Si cerca a testo libero su titolo e descrizione.
4. **RF-4** — Si ordina per scadenza, per data di creazione o per progetto; l'ordinamento predefinito è per
   scadenza crescente, con le attività senza scadenza in fondo.
5. **RF-5** — L'elenco è paginato con totale, e i filtri scelti restano fra una visita e l'altra.
6. **RF-6** — Quando nessuna attività corrisponde ai filtri, la schermata lo dice e offre di azzerarli: mai una
   pagina vuota senza uscita.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'interrogazione filtra sempre per `tenant_id` dal token verificato,
  prima di qualunque altro filtro.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/progetti/v1/tasks` estesa con i parametri di
  filtro, ricerca e ordinamento; paginazione a pagina e dimensione con totale; errori in
  `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova. Indici mirati su `(tenant_id, due_date)`,
  `(tenant_id, status)` e `(tenant_id, project_id)`; la ricerca a testo libero usa un indice testuale, non una
  scansione.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Progetti → Tutte le attività*; stato del server con la libreria di
  interrogazione, filtri conservati localmente; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette dei filtri, nomi degli stati e messaggi di elenco vuoto in
  `en, it, fr, es, de`; le date si formattano secondo la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Un utente con ruolo `member` vede tutte le attività
  dell'account: FlowGrove non ha visibilità per persona, e la ragione è scritta nella storia 0012.
- **RT-7 — Esposizione conversazionale (§12).** Lo strumento `search_tasks(testo?, progetto?, stato?, scadenza?)`
  è **lettura** e usa lo stesso filtro di questa rotta, non una seconda implementazione (storia 0028).
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: l'elenco mostra il nome dell'assegnatario, già
  dichiarato dalla storia 0012.
- **RT-9 — Registrazione eventi (§14).** Nessun evento di dominio; le interrogazioni lente vanno registrate con
  durata e forma del filtro, mai con il testo cercato, che può contenere qualsiasi cosa.

## 4. Criteri di accettazione

**CA-1 — Filtro «in ritardo»**
- **Dato** un account con tre attività scadute e non terminate e cinque in regola
- **Quando** si attiva il filtro «in ritardo»
- **Allora** l'elenco mostra esattamente le tre, con il totale corretto

**CA-2 — Ordinamento predefinito**
- **Dato** attività con e senza scadenza
- **Quando** si apre l'elenco senza toccare nulla
- **Allora** compaiono ordinate per scadenza crescente, con quelle senza scadenza in fondo

**CA-3 — Nessun risultato**
- **Dato** un filtro che non corrisponde a nulla
- **Quando** l'elenco si carica
- **Allora** compare lo stato vuoto con il pulsante per azzerare i filtri, e nessun errore

**CA-4 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` filtra per un progetto di `B`
- **Allora** l'elenco è vuoto e nessuna attività di `B` compare, in nessuna combinazione di filtri

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sulla costruzione del filtro e di **integrazione** sulla rotta con dati numerosi;
- [ ] prova di **isolamento fra account** su ogni combinazione di filtro;
- [ ] **prova end-to-end**: nessun impatto — il percorso `[J-PROGETTI]` passa dall'elenco dentro il progetto, non
      da questa vista; motivo registrato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di condividere il filtro con `search_tasks`;
- [ ] controllo automatico di **accessibilità** verde sull'elenco e sui filtri;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` | Servono le attività da elencare |
| Storia `0012` | L'assegnatario compare in elenco: se 0012 non c'è ancora, la colonna resta vuota |

## 7. Fuori ambito

- le viste salvate con un nome («i miei filtri»): rimandate, perché aggiungono configurazione a un'app che ha
  fatto della poca configurazione la sua promessa;
- l'esportazione dell'elenco: storia 0027.

## 8. Punti aperti

- Nessuno.
