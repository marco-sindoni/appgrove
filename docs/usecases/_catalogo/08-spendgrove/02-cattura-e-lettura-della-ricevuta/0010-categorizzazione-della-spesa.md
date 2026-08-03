# 0010 — Categorizzazione della spesa

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 02 — Cattura e lettura della ricevuta
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come collaboratore che rivede la ventesima ricevuta della settimana
> voglio che l'app proponga la categoria giusta, imparando dall'esercente che ho già classificato
> così da non dover scegliere ogni volta dallo stesso elenco la stessa voce.

**Contesto.** Il catalogo mette la «categorizzazione automatica» fra i casi d'uso principali. Va fatta **dopo** la
revisione e non prima, per una ragione precisa: la proposta di categoria si basa sull'esercente, e l'esercente è un
campo che la revisione può aver corretto. Una proposta costruita su un nome letto male è una proposta sbagliata che
sembra intelligente. La regola che l'app applica è deliberatamente semplice e spiegabile — «questo esercente
l'ultima volta l'hai messo in *vitto*» — perché una regola che l'utente capisce è una regola di cui si fida e che
può correggere.

## 2. Requisiti funzionali

1. **RF-1** — Il cliente gestisce le proprie categorie: crea, rinomina, disattiva (mai cancella, se sono già usate),
   e assegna a ciascuna un codice per la contabilità.
2. **RF-2** — In revisione, l'app **propone** una categoria quando l'esercente è già stato classificato in
   precedenza nello stesso account; la proposta dice **perché** è tale («l'ultima volta hai messo *Bar Trecolli* in
   Vitto»).
3. **RF-3** — La proposta è sempre modificabile e non si applica da sola: una spesa non si conferma con la categoria
   proposta se l'utente non ha guardato il campo.
4. **RF-4** — Si può fissare una **regola per esercente** («d'ora in poi *Rifornimento Ovest* è sempre Carburante»),
   visibile e disattivabile in un elenco delle regole: nessuna automazione invisibile.
5. **RF-5** — Le categorie in uso non si possono cancellare; disattivandole spariscono dalle scelte future ma
   restano leggibili sulle spese passate.
6. **RF-6** — Una categoria può portare un valore predefinito per l'imposta (aliquota tipica, indetraibilità
   tipica), che la storia `0024` userà come punto di partenza.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Categorie e regole per esercente sono **per account**: filtro
  `WHERE tenant_id = :tid` su ogni interrogazione, `tenant_id` dal token verificato. Nessuna categoria condivisa
  fra account, nessun apprendimento che attraversi gli account — che sarebbe un uso secondario dei dati dei clienti,
  vietato.
- **RT-2 — Interfaccia di programmazione (§2).** `GET|POST|PATCH /api/notespese/v1/categorie` e
  `GET|POST|DELETE /api/notespese/v1/regole-esercente`; errori in `application/problem+json`; definizione OpenAPI
  aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V7__regole_esercente.sql`: tabella delle regole con `tenant_id`, chiave
  UUID versione 7, esercente normalizzato, categoria, autore, stato attivo, colonne di controllo e cancellazione
  logica.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione *Impostazioni*, la gestione delle categorie e l'elenco delle
  regole; nella schermata di revisione, la proposta con la sua spiegazione. Solo token del sistema di design; tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Le categorie **predefinite** sono chiavi tradotte in tutte e cinque le lingue; le
  categorie **create dal cliente** sono contenuto e restano nella lingua in cui le ha scritte. La distinzione va
  spiegata nell'interfaccia, altrimenti sembra un difetto.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo nuovo: la categorizzazione avviene dentro la revisione, e la
  quota si consuma alla conferma.
- **RT-7 — Esposizione conversazionale (§12).** La storia dichiara
  `categorizza_spesa(id_spesa, categoria) → spesa aggiornata`, marcato **scrittura reversibile**: richiede conferma
  leggera e non è ammesso su spese già dentro una nota approvata. Dipendenza: UC 0061-0063.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: categorie e regole descrivono esercenti, non
  persone. Va però detto nel manifesto che la **regola per esercente**, letta insieme alle spese, racconta abitudini
  di una persona: resta nel perimetro già dichiarato di `spesa`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `categoria assegnata`, `regola creata`, `regola disattivata`
  portano `tenant_id`, `app_id`, `user_id` e identificativo di correlazione, con il **codice** della categoria e
  **non** il nome dell'esercente.

## 4. Criteri di accettazione

**CA-1 — Proposta motivata**
- **Dato** un account in cui *Bar Trecolli* è già stato classificato come *Vitto*
- **Quando** l'utente apre in revisione una nuova spesa dello stesso esercente
- **Allora** la categoria proposta è *Vitto* e accanto compare la ragione della proposta

**CA-2 — La proposta non si applica da sola**
- **Dato** una spesa con categoria proposta · **Quando** l'utente conferma senza aver toccato il campo
- **Allora** la categoria salvata è quella proposta **e** resta registrato che è stata accettata, non scelta: la
  differenza serve a misurare quanto la proposta è buona

**CA-3 — Regola per esercente**
- **Dato** una regola attiva «*Rifornimento Ovest* → Carburante»
- **Quando** arriva una spesa di quell'esercente
- **Allora** la categoria è precompilata con Carburante, la regola è citata, e disattivandola le spese future
  tornano senza proposta

**CA-4 — Categoria in uso**
- **Dato** una categoria usata da dodici spese · **Quando** si tenta di cancellarla
- **Allora** l'operazione è respinta con `409` e viene offerta la disattivazione; le spese passate continuano a
  mostrarla

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` che hanno entrambi un esercente con lo stesso nome
- **Quando** `A` crea una regola per quell'esercente
- **Allora** le spese di `B` non ricevono nessuna proposta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sulla normalizzazione del nome dell'esercente e sulla scelta della proposta; di
      **integrazione** sulle due risorse con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su categorie e regole, compreso il caso di esercenti omonimi;
- [ ] **prova end-to-end**: *rimando* alla storia `0031`, che nel percorso `[J-NOTESPESE]` verifica la proposta di
      categoria; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato lì;
- [ ] **traduzioni** delle categorie predefinite e dei testi di spiegazione in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna categoria nuova, ma la nota sulla regola per esercente è scritta;
- [ ] **registro delle decisioni** compilato, con la scelta di una regola spiegabile invece di un modello statistico;
- [ ] contratto dello strumento `categorizza_spesa` dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0008` | La proposta si basa sull'esercente **dopo** l'eventuale correzione umana |

## 7. Fuori ambito

- Qualunque modello statistico o apprendimento automatico sulla categorizzazione: una regola spiegabile
  «stesso esercente, stessa categoria» copre la quasi totalità dei casi di una micro-impresa, si corregge in un
  clic e non richiede di trattare i dati per addestrare niente.
- La ripartizione di una spesa su più categorie (una cena con due voci diverse): rimandata, perché richiede righe
  di spesa figlie e cambierebbe il modello. Se servirà, sarà una storia a sé.

## 8. Punti aperti

- **Codici contabili delle categorie**: quale codifica usare dipende dal gestionale del commercialista del cliente.
  L'app li tiene come testo libero e li porta nell'esportazione (storia `0025`): non ne impone nessuna.
