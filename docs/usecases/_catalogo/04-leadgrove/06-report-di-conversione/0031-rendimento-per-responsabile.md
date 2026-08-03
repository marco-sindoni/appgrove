# 0031 — Rendimento per responsabile

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 06 — Report di conversione
**Storia**: `0031` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0018`, `0030`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una squadra di quattro venditori
> voglio vedere quante trattative ciascuno ha in carico e quante ne chiude
> così da capire chi ha bisogno di aiuto, non chi va licenziato.

**Contesto.** È la seconda ragione per cui il modello utente è `multi`. È anche la storia con il rischio d'uso più
alto dell'app: un numero per persona diventa facilmente una classifica, e una classifica in una squadra di quattro
è una cosa che si legge in due minuti e brucia in sei mesi. La posizione proposta è mostrare i numeri **con il
contesto** — carico, non solo esito — e non ordinare per «migliore».

## 2. Requisiti funzionali

1. **RF-1** — Il rapporto mostra per ciascun responsabile: trattative aperte in carico, valore in corso, chiuse
   nel periodo, vinte, tasso di chiusura, tempo mediano di chiusura.
2. **RF-2** — L'elenco è ordinato per nome, non per rendimento; l'ordinamento si può cambiare, ma non è quello
   predefinito.
3. **RF-3** — Il rapporto mostra anche il numero di trattative **ferme** per responsabile (storia 0023): è
   l'indicatore che dice «questa persona è sommersa», che è un'informazione diversa da «chiude poco».
4. **RF-4** — I responsabili con meno di dieci trattative chiuse nel periodo sono marcati come non significativi.
5. **RF-5** — Il rapporto è accessibile solo a ruolo `owner` o `admin`; un `member` vede **solo i propri** numeri.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** L'aggregazione comprende solo membri e trattative dell'account del token
  verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/sales/v1/reports/by-owner` con periodo e
  pipeline; errori in `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova; usa l'indice per responsabile della storia 0018.
- **RT-4 — Modulo frontend (§3, §5).** Sezione Rapporti → Per responsabile, in forma di tabella; solo token del
  sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Intestazioni, avvisi di non significatività e note in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota; matrice dei ruoli come da RF-5.
- **RT-7 — Esposizione conversazionale (§12).** Rientra in `conversion_report` (storia 0034) con il parametro di
  raggruppamento per responsabile, **rispettando la stessa matrice dei ruoli**: un `member` che lo chiede in chat
  ottiene solo i propri numeri, non quelli dei colleghi. È il punto in cui il livello conversazionale potrebbe
  aggirare un'autorizzazione se non ci si sta attenti.
- **RT-8 — Dati personali (§10).** Il rapporto riguarda i **membri dell'account**, non i contatti: sono dati di
  persone che lavorano per il cliente, e il loro trattamento a fini di misurazione del rendimento è del cliente
  come titolare. Nessuna voce nuova nel manifesto perché non si conservano dati nuovi, ma il fatto va scritto
  nella descrizione del manifesto: è un uso che merita di essere dichiarato.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo.

## 4. Criteri di accettazione

**CA-1 — Numeri per responsabile**
- **Dato** tre venditori con trattative chiuse nel trimestre
- **Quando** il titolare apre il rapporto
- **Allora** vede per ciascuno carico, chiuse, vinte, tasso e tempo mediano, ordinati per nome

**CA-2 — Un membro vede solo sé**
- **Dato** un utente con ruolo `member`
- **Quando** apre il rapporto, anche chiedendo un altro responsabile
- **Allora** vede solo i propri numeri

**CA-3 — Non significativo**
- **Dato** un venditore con 3 trattative chiuse nel periodo
- **Quando** si legge la sua riga
- **Allora** il tasso è marcato come non significativo

**CA-4 — Anche dalla chat vale la matrice dei ruoli**
- **Dato** un `member` che chiede in chat il rendimento della squadra
- **Quando** lo strumento risponde
- **Allora** restituisce solo i suoi numeri

**CA-5 — Isolamento fra account**
- **Dato** due account con squadre diverse
- **Quando** un titolare di `A` apre il rapporto
- **Allora** vede solo i membri di `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sull'aggregazione per responsabile e di **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli**, compresa quella applicata allo strumento
      conversazionale;
- [ ] **prova end-to-end**: nessun impatto sul percorso minimo; coperta da prove d'integrazione, con il motivo nel
      registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, ma l'uso per misurazione del rendimento dichiarato nella
      descrizione del manifesto;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di non ordinare per rendimento;
- [ ] contratto degli **strumenti conversazionali**: raggruppamento per responsabile con matrice dei ruoli
      rispettata;
- [ ] controllo automatico di **accessibilità** verde sulla tabella;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0018` | Serve il responsabile sulla trattativa |
| Storia `0030` | Riusa gli stessi calcoli di tasso e tempo mediano |

## 7. Fuori ambito

- gli obiettivi individuali e il loro raggiungimento: non previsti in questa proposta;
- il calcolo delle provvigioni: fuori perimetro, è materia contabile;
- la classifica: esclusa per scelta.

## 8. Punti aperti

- **Uso del rapporto per valutare le persone.** In alcuni ordinamenti la misurazione sistematica del rendimento
  dei lavoratori con strumenti informatici ha vincoli propri, distinti dalla disciplina generale sui dati
  personali, e sono vincoli del **cliente** come datore di lavoro. Segnalarlo nella documentazione è prudente. Il
  punto va portato alla revisione legale: non è una decisione di questa storia.
