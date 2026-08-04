# 0023 — Abbinamento fra movimento e spesa

**Applicazione**: 08 — SpendGrove (`notespese`) · **Epica**: 05 — Riconciliazione e uscita verso la contabilità
**Storia**: `0023` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0022`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come chi tiene l'amministrazione
> voglio vedere quali movimenti di carta hanno la loro ricevuta e quali no, con la proposta di abbinamento già
> fatta
> così da chiudere il mese sapendo che non manca nessun giustificativo, invece di scoprirlo a marzo dell'anno dopo.

**Contesto.** Da una parte i movimenti importati, dall'altra le spese confermate: finché restano due elenchi
separati, nessuno sa quali pagamenti sono scoperti. L'abbinamento li congiunge e produce l'informazione che l'app
esiste per dare — **l'elenco dei movimenti orfani**, cioè i soldi usciti senza una ricevuta. La proposta di
abbinamento è automatica ma la conferma no: due importi uguali nello stesso giorno possono essere due caffè, e
decidere quale sia quale non è un lavoro da automatismo.

## 2. Requisiti funzionali

1. **RF-1** — L'app propone gli abbinamenti confrontando importo e data entro una tolleranza configurabile, e li
   ordina per verosimiglianza mostrando **perché** propone quell'accoppiata.
2. **RF-2** — L'abbinamento si conferma uno per uno o in blocco, ma sempre con un gesto esplicito: nessuna coppia
   si abbina da sola.
3. **RF-3** — Un abbinamento si può disfare finché la spesa non è entrata in una nota approvata; disfacendolo,
   movimento e spesa tornano liberi.
4. **RF-4** — Da un **movimento orfano** si può creare la spesa mancante in un clic, con data, importo e esercente
   già valorizzati dal movimento; nasce in `da_rivedere` e va confermata come tutte le altre.
5. **RF-5** — Esiste la vista «movimenti orfani» per periodo, con il totale non giustificato: è la domanda di
   chiusura del mese.
6. **RF-6** — Esiste la vista opposta, «spese senza movimento», utile per le spese pagate con mezzi propri: non è
   un'anomalia, ma va distinta.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Proposte, abbinamenti e viste filtrano per `tenant_id` preso dal token
  verificato; il confronto avviene **solo dentro l'account**. Dentro l'account vale la visibilità per ruolo: chi
  `sostiene` vede al più i movimenti della propria carta.
- **RT-2 — Interfaccia di programmazione (§2).** `GET /api/notespese/v1/abbinamenti/proposte`,
  `POST /api/notespese/v1/movimenti/{id}/abbina`, `DELETE .../abbina`,
  `POST /api/notespese/v1/movimenti/{id}/crea-spesa`; errori in `application/problem+json` con `409` per
  movimento o spesa già abbinati; definizione OpenAPI aggiornata.
- **RT-3 — Persistenza (§8).** Migrazione `V20__abbinamenti.sql`: colonne di abbinamento su `movimento_carta` e
  `spesa` con vincolo di unicità **da entrambe le parti** (un movimento un solo abbinamento, e viceversa), più chi
  ha abbinato e quando; colonne di controllo.
- **RT-4 — Modulo frontend (§3, §5).** Nella sezione *Riconciliazione*, la vista a due colonne movimenti/spese con
  le proposte evidenziate, e i due elenchi «orfani» e «senza movimento». Solo token del sistema di design; tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Testi delle proposte, delle spiegazioni e degli errori passano dallo spazio-nomi
  `notespese` e sono presenti in `en, it, fr, es, de`.
- **RT-6 — Varchi e quota (§6, §7).** Abbinare non consuma quota; **creare una spesa da un movimento orfano** la
  consuma alla conferma, come ogni altra spesa (storia `0004`).
- **RT-7 — Esposizione conversazionale (§12).** La storia dichiara `elenca_movimenti_orfani(periodo?) → elenco
  minimizzato`, marcato **lettura**. L'abbinamento **non** è esposto in scrittura: è una decisione di
  corrispondenza che una persona prende guardando due elenchi, e sbagliarla sposta soldi da una riga all'altra
  della contabilità. Dipendenza: UC 0061-0063.
- **RT-8 — Dati personali (§10).** Nessuna categoria nuova; l'abbinamento lega dati già dichiarati. Voce
  **aggiornata** nel manifesto in italiano e inglese, con la nota che l'abbinamento rende il quadro delle spese di
  una persona più completo e quindi più sensibile alla minimizzazione.
- **RT-9 — Registrazione eventi (§14).** Gli eventi `abbinamento confermato`, `abbinamento disfatto`, `spesa creata
  da movimento` portano `tenant_id`, `app_id`, `user_id`, identificativo di correlazione e identificativi — mai
  importi né descrizioni.

## 4. Criteri di accettazione

**CA-1 — Proposta motivata e confermata**
- **Dato** un movimento di 38,00 € del 12/07 e una spesa confermata di 38,00 € del 12/07
- **Quando** si aprono le proposte
- **Allora** la coppia è proposta con la spiegazione «stesso importo, stessa data», e la conferma esplicita li
  abbina

**CA-2 — Nessun abbinamento automatico**
- **Dato** una proposta di abbinamento · **Quando** nessuno la conferma
- **Allora** movimento e spesa restano liberi: il tempo che passa non abbina nulla

**CA-3 — Movimento orfano trasformato in spesa**
- **Dato** un movimento di 21,50 € senza spesa corrispondente
- **Quando** l'amministrazione crea la spesa dal movimento
- **Allora** nasce una spesa in `da_rivedere` con data, importo ed esercente precompilati e già abbinata al
  movimento; la quota si consuma solo alla conferma

**CA-4 — Abbinamento unico**
- **Dato** un movimento già abbinato · **Quando** si tenta di abbinarlo a una seconda spesa
- **Allora** l'operazione è respinta con `409`

**CA-5 — Blocco dopo l'approvazione**
- **Dato** una spesa abbinata ed entrata in una nota **approvata** · **Quando** si tenta di disfare l'abbinamento
- **Allora** l'operazione è respinta con `409` e il messaggio spiega perché

**CA-6 — Isolamento fra account**
- **Dato** due account con movimenti di pari importo e data
- **Quando** l'uno apre le proposte
- **Allora** non gli viene proposta nessuna spesa dell'altro

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno backend e frontend; l'intera suite prima del commit);
- [ ] prove di **unità** sull'algoritmo di proposta (tolleranze di importo e data, ordinamento); di **integrazione**
      sull'abbinamento con database effimero e migrazioni vere, compresa la prova concorrente sull'unicità;
- [ ] prova di **isolamento fra account** sulle proposte e sugli abbinamenti;
- [ ] **prova end-to-end**: *coprire ora* il passo «abbino il movimento alla ricevuta e vedo l'orfano che resta» nel
      percorso `[J-NOTESPESE]`; registro
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese;
- [ ] **registro delle decisioni** compilato, con la scelta di non abbinare mai automaticamente;
- [ ] contratto dello strumento `elenca_movimenti_orfani` dichiarato, marcato lettura; l'abbinamento non esposto,
      con la motivazione scritta nel contratto;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| `0022` | Senza movimenti importati non c'è nulla da abbinare |

## 7. Fuori ambito

- La riconciliazione del conto corrente nel suo complesso: ReconGrove (catalogo 49).
- L'abbinamento parziale (un movimento che copre due spese, o viceversa): caso reale ma minoritario nel ciclo
  della nota spese; richiederebbe un legame molti a molti e va deciso a parte.

## 8. Punti aperti

- **Tolleranza predefinita** su importo e data della proposta: proposta di zero centesimi e due giorni, ma va
  tarata sui dati veri — le carte registrano il movimento con qualche giorno di ritardo e talvolta con importo
  diverso per il cambio valuta.
