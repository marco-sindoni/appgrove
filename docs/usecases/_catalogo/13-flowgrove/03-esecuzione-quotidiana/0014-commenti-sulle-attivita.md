# 0014 — Commenti sulle attività

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 03 — Esecuzione quotidiana
**Storia**: `0014` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0012`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come collaboratore che ha scoperto un problema sul cantiere
> voglio scriverlo sull'attività invece che nel gruppo di messaggistica
> così da ritrovarlo fra tre mesi, quando il cliente contesta il conto.

**Contesto.** Nelle micro-imprese la discussione sul lavoro vive nella messaggistica, dove è irrecuperabile: si
perde nel flusso e non è collegata a niente. Attaccarla all'attività è la parte facile; la parte che conta è che
il commento diventa **contenuto scritto da una persona su un'altra persona o su un cliente** — un ingresso non
presidiato per dati che l'app non vorrebbe (§6 della descrizione). Da qui l'avviso in linea e la presenza dei
commenti nell'esportazione e nella cancellazione.

## 2. Requisiti funzionali

1. **RF-1** — Su ogni attività si scrivono commenti in ordine cronologico, con autore e data.
2. **RF-2** — L'autore può modificare e cancellare il proprio commento entro un tempo breve dalla scrittura;
   dopo, resta ma può essere cancellato da chi ha ruolo `admin`, e la cancellazione lascia una riga «commento
   rimosso» con la data, senza il contenuto.
3. **RF-3** — Si può citare una persona dell'account con la chiocciola; la citazione produce un avviso dentro
   l'app (storia 0016), non fuori.
4. **RF-4** — Il campo porta l'avviso in linea «non inserire dati sensibili» e non fa alcuna analisi del
   contenuto.
5. **RF-5** — I commenti si vedono nella scheda dell'attività e nella cronologia del progetto, con il conteggio
   sulla scheda della lavagna.
6. **RF-6** — I commenti seguono l'attività: se l'attività si archivia, i commenti restano leggibili.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `comment` filtra per `tenant_id` dal token
  verificato; l'attività citata deve appartenere allo stesso account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/progetti/v1/tasks/{id}/comments` e
  `PATCH|DELETE /api/progetti/v1/comments/{id}`; corpo validato con un limite di lunghezza; errori in
  `application/problem+json`; OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V7__commenti.sql`: `comment` con `tenant_id`, `task_id`, `author_id`,
  `body`, colonne di controllo e cancellazione logica; indice `(tenant_id, task_id, created_at)`.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro dei commenti nella scheda dell'attività; solo token del sistema
  di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, avviso sui dati sensibili e messaggi in `en, it, fr, es, de`. Il
  contenuto scritto dagli utenti resta nella loro lingua e non si traduce.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Ruolo minimo per commentare: `member`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento di scrittura per i commenti in questa stesura:
  far scrivere a un assistente commenti a nome di una persona è precisamente il genere di cosa che va evitata. I
  commenti compaiono, in sola lettura, nel risultato di `get_project_progress` solo come conteggio.
- **RT-8 — Dati personali (§10).** `comment.author_id` e `comment.body` sono dati personali: voci nuove nel
  manifesto in italiano e inglese, campi annotati `@PersonalData`, tabella `comment` in `exportData` e
  `purgeData`. Il corpo è **testo libero**: dichiararlo come tale nel manifesto, perché può contenere di tutto.
- **RT-9 — Registrazione eventi (§14).** «Commento scritto», «commento rimosso» con `tenant_id`, `app_id`,
  `user_id` e correlazione; **mai** il testo del commento.

## 4. Criteri di accettazione

**CA-1 — Scrittura e lettura**
- **Dato** un'attività
- **Quando** una persona scrive un commento
- **Allora** compare in fondo alla discussione con il suo nome e l'ora, e il conteggio sulla scheda si aggiorna

**CA-2 — Cancellazione tracciata**
- **Dato** un commento scritto due giorni fa
- **Quando** un utente `admin` lo rimuove
- **Allora** al suo posto resta «commento rimosso» con la data, e il contenuto non è più recuperabile
  dall'interfaccia

**CA-3 — Citazione**
- **Dato** una persona dell'account
- **Quando** viene citata in un commento
- **Allora** riceve un avviso dentro l'app, e nessuna comunicazione esce verso l'esterno

**CA-4 — Esportazione**
- **Dato** una persona che chiede l'esportazione dei propri dati
- **Quando** l'esportazione viene prodotta
- **Allora** contiene i commenti da lei scritti

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede i commenti di un'attività di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sul limite di modifica e di **integrazione** sulle rotte;
- [ ] prova di **isolamento fra account** su lettura e scrittura dei commenti;
- [ ] **prova end-to-end**: nessun impatto — `[J-PROGETTI]` non passa dai commenti; motivo registrato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `comment`, con la marcatura «testo libero»;
- [ ] **registro delle decisioni** compilato, con annotato **perché non esiste uno strumento conversazionale di
      scrittura** per i commenti;
- [ ] controllo automatico di **accessibilità** verde sul riquadro dei commenti;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` | Il commento vive su un'attività |
| Storia `0012` | La citazione con la chiocciola riguarda le persone dell'account |

## 7. Fuori ambito

- la conversazione in tempo reale con presenza degli altri: non è una messaggistica;
- la formattazione ricca e le immagini in linea: il testo è semplice, gli allegati sono la storia 0015;
- il rilevamento automatico di dati sensibili nel testo: è un tema trasversale di piattaforma, non di questa app.

## 8. Punti aperti

- **Per quanto si conservano i commenti** dopo la chiusura del progetto: la proposta della descrizione (§6) è
  «vita del progetto + 24 mesi», ma è una durata da confermare insieme a quella delle righe di ore (§11.5).
