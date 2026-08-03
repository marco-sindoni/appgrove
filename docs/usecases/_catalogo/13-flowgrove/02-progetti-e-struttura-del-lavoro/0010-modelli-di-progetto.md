# 0010 — Modelli di progetto

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 02 — Progetti e struttura del lavoro
**Storia**: `0010` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0009`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di uno studio che fa dodici volte l'anno lo stesso tipo di lavoro
> voglio salvare la struttura di un progetto e rigenerarla su un cliente nuovo
> così da non riscrivere quindici attività ogni volta, e da non dimenticarne una.

**Contesto.** Il cliente tipo di FlowGrove ripete lo stesso lavoro con clienti diversi: la pratica, il cantiere,
la campagna, l'installazione. Riscrivere le attività a mano è la parte di lavoro che fa abbandonare lo strumento
dopo il terzo progetto. Il modello è anche il modo per **portare dentro il metodo**: le stime in ore che stanno
nel modello diventano il budget di partenza del progetto nuovo (storia 0021).

## 2. Requisiti funzionali

1. **RF-1** — Da un progetto esistente si crea un **modello**, che conserva la struttura delle attività e delle
   sotto-attività, le stime in ore, i traguardi e gli scarti fra le date (per esempio «il collaudo cade 10 giorni
   dopo l'inizio»).
2. **RF-2** — Il modello **non** conserva nulla di specifico: né cliente, né referente, né assegnatari, né ore
   dichiarate, né allegati, né commenti.
3. **RF-3** — Da un modello si crea un progetto nuovo indicando titolo, cliente e data d'inizio; le date delle
   attività e dei traguardi si calcolano dagli scarti.
4. **RF-4** — I modelli si elencano, si rinominano e si cancellano; cancellare un modello non tocca i progetti
   già generati.
5. **RF-5** — La generazione mostra un'anteprima di quante attività e quanti traguardi verranno creati, e chiede
   conferma.
6. **RF-6** — Il modello si può modificare direttamente (aggiungere, togliere, rinominare attività prototipo)
   senza dover generare un progetto.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** I modelli sono dell'account: ogni lettura e scrittura di
  `project_template` filtra per `tenant_id` dal token verificato. Non esistono modelli condivisi fra account, e
  non esistono modelli precaricati dalla piattaforma.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/progetti/v1/project-templates`,
  `PATCH|DELETE /api/progetti/v1/project-templates/{id}` e
  `POST /api/progetti/v1/project-templates/{id}/instantiate`; errori in `application/problem+json`; OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V5__modelli.sql`: `project_template` e `project_template_item` con
  `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica. La generazione
  avviene in **una sola transazione**: o nasce tutto il progetto, o non nasce niente.
- **RT-4 — Modulo frontend (§3, §5).** Sezione *Progetti → Modelli*; anteprima prima della conferma; solo token
  del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe in `en, it, fr, es, de`; i titoli delle attività prototipo
  restano nella lingua in cui il cliente li ha scritti e non si traducono.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota: né i modelli né i progetti generati occupano posti.
  Ruolo minimo per creare un modello e generare: `admin`.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia: generare un progetto da un
  modello crea decine di righe in un colpo solo, ed è un'operazione che ha senso fare guardando l'anteprima. Se in
  futuro si esponesse, sarebbe **scrittura con bozza e conferma obbligatoria**.
- **RT-8 — Dati personali (§10).** Nessun dato personale: il modello, per costruzione (RF-2), non contiene
  riferimenti a persone. È una proprietà da **verificare con una prova**, non da assumere.
- **RT-9 — Registrazione eventi (§14).** «Modello creato», «progetto generato da modello» con `tenant_id`,
  `app_id`, `user_id`, correlazione e numero di righe create; mai i titoli.

## 4. Criteri di accettazione

**CA-1 — Creazione del modello**
- **Dato** un progetto con 12 attività, 3 sotto-attività e 2 traguardi
- **Quando** se ne crea un modello
- **Allora** il modello contiene 15 attività prototipo e 2 traguardi prototipo, e nessun assegnatario, ora,
  allegato o commento

**CA-2 — Generazione con date calcolate**
- **Dato** un modello in cui il collaudo cade 10 giorni dopo l'inizio
- **Quando** si genera un progetto con inizio 1 settembre
- **Allora** l'attività di collaudo ha scadenza 11 settembre e il progetto nasce in stato `bozza`

**CA-3 — Anteprima e annullamento**
- **Dato** un modello con 15 attività
- **Quando** si avvia la generazione e non si conferma
- **Allora** nessun progetto e nessuna attività vengono creati

**CA-4 — Nessun dato di persone nel modello**
- **Dato** un progetto con assegnatari e ore dichiarate
- **Quando** se ne crea un modello e si ispeziona il contenuto
- **Allora** non compare nessun identificativo di persona né alcuna riga di ore

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` tenta di generare da un modello di `B`
- **Allora** riceve `404` e nulla viene creato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** sul calcolo degli scarti di data e di **integrazione** sulla generazione transazionale;
- [ ] prova di **isolamento fra account** su tutte le rotte introdotte;
- [ ] **prova end-to-end**: nessun impatto — `[J-PROGETTI]` crea il progetto a mano; motivo registrato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova, con la prova automatica che il modello non contiene dati di
      persone;
- [ ] **registro delle decisioni** compilato, con annotato **cosa il modello non conserva** e perché;
- [ ] controllo automatico di **accessibilità** verde sulla sezione dei modelli;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0007` | Serve la struttura delle attività da modellizzare |
| Storia `0009` | I traguardi fanno parte del modello |

## 7. Fuori ambito

- una raccolta di modelli predefiniti per settore: sarebbe contenuto editoriale, non software, e va deciso a
  parte;
- la generazione ricorrente automatica («ogni lunedì crea il progetto della settimana»): non prevista.

## 8. Punti aperti

- Nessuno.
