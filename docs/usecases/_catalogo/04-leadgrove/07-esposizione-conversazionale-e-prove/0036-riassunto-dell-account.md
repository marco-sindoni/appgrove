# 0036 — Riassunto dell'account

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0036` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0022`, `0034`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che sta per entrare da un cliente
> voglio chiedere «a che punto siamo con Alfa Utensili» e avere la risposta in due frasi
> così da non entrare impreparato dopo aver letto in fretta tre schermate diverse.

**Contesto.** È lo strumento che, insieme all'agenda, giustifica da solo il livello conversazionale per questa
app: è la domanda che nessun elenco risponde, perché la risposta sta sparsa fra trattative, attività, note e
consensi. La storia esiste separata dalla 0034 perché non è una semplice lettura: è una **composizione**, e le
scelte su cosa includere e cosa lasciare fuori sono decisioni di prodotto e di protezione dei dati.

## 2. Requisiti funzionali

1. **RF-1** — `summarize_account` accetta un'azienda (per identificativo o per nome) e restituisce, in forma
   strutturata: trattative aperte con fase e valore, trattative chiuse di recente con esito, prossime attività,
   ultime note, stato delle preferenze di contatto, momento dell'ultimo contatto effettivo.
2. **RF-2** — Il riassunto dice esplicitamente **da quanto tempo non succede nulla**, che è l'informazione più
   utile e la più facile da non notare.
3. **RF-3** — Se il nome indicato corrisponde a più aziende, restituisce l'elenco delle candidate invece di
   sceglierne una: sbagliare cliente è peggio che chiedere.
4. **RF-4** — Il riassunto include il numero di contatti dell'azienda ma **non** i loro recapiti, salvo richiesta
   esplicita.
5. **RF-5** — Le note incluse sono le più recenti fino a un numero massimo, con l'avvertenza che sono testo
   scritto da persone e possono contenere di tutto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il riassunto compone dati del solo account del chiamante; ogni sorgente
  filtra per `tenant_id` prima di essere unita.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/sales/v1/companies/{id}/summary`, che è anche la
  sorgente dello strumento: la stessa logica serve l'interfaccia e la chat, non due copie che divergono.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: il riassunto legge la cronologia unificata della storia
  0022.
- **RT-4 — Modulo frontend (§3, §5).** Blocco «A che punto siamo» in testa alla scheda dell'azienda, con gli
  stessi contenuti del riassunto: quello che l'assistente dice e quello che l'app mostra devono coincidere.
- **RT-5 — Cinque lingue (§4).** Il blocco dell'interfaccia è nelle cinque lingue; lo strumento restituisce dati
  strutturati e non frasi, così che la lingua la scelga chi risponde.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota; valgono i varchi ordinari e la matrice dei ruoli.
- **RT-7 — Esposizione conversazionale (§12).** È lo strumento `summarize_account` dichiarato nella storia 0034,
  in **sola lettura**. Dipendenza dichiarata: UC 0061-0063.
- **RT-8 — Dati personali (§10).** La minimizzazione di RF-4 è una misura di protezione: un riassunto che include
  tutti i recapiti di tutti i contatti di un'azienda fa uscire molti più dati di quanti la domanda ne richiedesse.
  Nessuna voce nuova nel manifesto.
- **RT-9 — Registrazione eventi (§14).** «Riassunto richiesto» con identificativo dell'azienda e autore; **mai**
  il contenuto restituito.

## 4. Criteri di accettazione

**CA-1 — Riassunto completo**
- **Dato** un'azienda con 2 trattative aperte, 1 persa, 3 note e un'attività fra due giorni
- **Quando** si chiede il riassunto
- **Allora** contiene tutte e quattro le informazioni, più lo stato dei consensi e il momento dell'ultimo contatto

**CA-2 — Da quanto non succede nulla**
- **Dato** un'azienda con l'ultima attività completata 45 giorni fa
- **Quando** si chiede il riassunto
- **Allora** l'informazione «nessun contatto da 45 giorni» è presente in modo esplicito

**CA-3 — Nome ambiguo**
- **Dato** due aziende il cui nome contiene «Alfa»
- **Quando** si chiede il riassunto per nome
- **Allora** si ottiene l'elenco delle candidate, non un riassunto scelto a caso

**CA-4 — Nessun recapito senza richiesta**
- **Dato** un'azienda con quattro contatti
- **Quando** si chiede il riassunto senza chiedere i recapiti
- **Allora** compare il numero dei contatti e nessun indirizzo o telefono

**CA-5 — Interfaccia e chat coincidono**
- **Dato** la stessa azienda
- **Quando** si confronta il blocco della scheda con il risultato dello strumento
- **Allora** le informazioni sono le stesse

**CA-6 — Isolamento fra account**
- **Dato** due account con aziende omonime
- **Quando** un utente di `A` chiede il riassunto per nome
- **Allora** ottiene solo la propria

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione e sul calcolo dell'inattività, di **integrazione** sulla rotta;
- [ ] prova di **isolamento fra account** sul riassunto, con aziende omonime nei due account;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-SALES]` (storia 0037) verifica che il riassunto mostri
      quanto costruito nei passi precedenti; voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per il blocco dell'interfaccia;
- [ ] **manifesto dei dati**: nessuna voce nuova; minimizzazione dichiarata;
- [ ] **registro delle decisioni** compilato, con annotato cosa il riassunto include e cosa lascia fuori;
- [ ] contratto degli **strumenti conversazionali**: `summarize_account` completato;
- [ ] controllo automatico di **accessibilità** verde sul blocco;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0022` | La cronologia unificata è la sorgente |
| Storia `0034` | Lo strumento è dichiarato nel contratto |

## 7. Fuori ambito

- il riassunto **generato in linguaggio naturale**: il servizio restituisce dati, la frase la compone il livello
  conversazionale;
- il riassunto di un contatto invece che di un'azienda: `get_contact` basta;
- i dati provenienti da altre app della suite (fatture aperte, ticket): dipendono dal contratto degli eventi
  condivisi ([application-description.md](../application-description.md) §11.4). È **il** motivo per cui questo
  strumento sarà molto più utile quando la suite esisterà.

## 8. Punti aperti

- Nessuno.
