# 0030 — Due strati: catena e contenuto cifrato

**Applicazione**: 31 — AuditGrove (`agentaudit`) · **Epica**: 06 — Dati delle persone e diritti
**Storia**: `0030` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0002`, `0009`
**Ultimo aggiornamento**: 2026-08-03

> 🛑 **Fermata prima di cominciare — revisione legale bloccante.** Questa storia costruisce l'impianto tecnico su
> cui poggia la risposta al conflitto fra **dovere di prova** e **diritto alla cancellazione** (§6.2 della
> [descrizione dell'applicazione](../application-description.md)). Quel conflitto **non è risolto**: sono aperte
> tre domande che nessun agente e nessuno sviluppatore possono chiudere da soli — quale base giuridica regga la
> conservazione della catena, quali eccezioni al diritto di cancellazione siano invocabili e per quanto, e se la
> distruzione della chiave valga come cancellazione. **Nessuna riga di codice di questa epica va scritta prima
> della revisione legale.** Questa storia si può *scrivere* e *discutere*; non si può *implementare*.

## 1. Narrazione

> Come titolare del trattamento che usa AuditGrove per i propri agenti
> voglio che i contenuti eventualmente conservati stiano separati dalla catena di prova e sotto una chiave propria
> così da poter cancellare ciò che va cancellato senza distruggere la prova di ciò che è successo.

**Contesto.** Fino a qui il registro conserva forma e impronte dei parametri, mai i valori (storia 0009): finché è
così, il conflitto fra prova e cancellazione morde poco, perché non c'è quasi nulla da cancellare. Ma esistono casi
legittimi in cui il contenuto serve davvero (storia 0031), e nel momento in cui un contenuto entra, entra anche il
problema: quel contenuto può riguardare una persona che ha diritto di chiederne la cancellazione, mentre la riga
che lo accompagna è una prova che il cliente ha il dovere di conservare.

La risposta di progetto è **separare i due strati fin dall'inizio**, prima ancora che il primo contenuto esista.
Farlo dopo significherebbe avere contenuti dentro la catena e nessun modo di toglierli senza spezzarla: è
esattamente il genere di errore che non si corregge più.

## 2. Requisiti funzionali

1. **RF-1** — Esistono due strati distinti: la **catena di prova** (impronte, identificativi, esiti, momenti,
   forma dei parametri) e il **contenuto allegato** (valori dei parametri e del risultato), che vivono in tabelle
   separate e con regole diverse.
2. **RF-2** — Il contenuto allegato è **cifrato a riposo con una chiave per account e per periodo**; la chiave non
   sta accanto al testo cifrato.
3. **RF-3** — L'impronta di un'azione nella catena si calcola su ciò che sta **nella catena** e sull'impronta del
   contenuto, **mai sul contenuto in chiaro**: rendere illeggibile il contenuto non altera nessuna impronta e non
   spezza la catena.
4. **RF-4** — Una verifica di integrità (storia 0014) eseguita su un intervallo i cui contenuti sono stati resi
   illeggibili risponde comunque **«integra»**, e dichiara quanti contenuti dell'intervallo non sono più
   leggibili.
5. **RF-5** — Le chiavi hanno un ciclo di vita esplicito e tracciato: creazione, uso, distruzione — con momento e
   motivo. Una chiave distrutta non è recuperabile in nessun modo, nemmeno da chi amministra la piattaforma.
6. **RF-6** — Finché nessuno strumento ha la conservazione attiva (storia 0031), **nessun contenuto esiste** e lo
   strato è vuoto: la struttura c'è, il dato no.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Contenuti e chiavi portano `tenant_id` e si leggono solo con il
  `tenant_id` preso dal token verificato. **Una chiave di un account non può in nessun caso decifrare il contenuto
  di un altro**: è un caso di prova esplicito e obbligatorio.
- **RT-2 — Interfaccia di programmazione (§2).** Nessuna rotta pubblica nuova in questa storia: si costruiscono il
  deposito, la cifratura e il ciclo di vita delle chiavi. Le rotte che li usano sono le storie 0031 (attivazione),
  0025 (rivelazione) e 0032 (cancellazione).
- **RT-3 — Persistenza (§8).** Migrazione `V…__contenuti_e_chiavi.sql` sullo schema `app_agentaudit`: tabelle
  `attached_contents` e `content_keys`, con `tenant_id`, chiave primaria UUID versione 7 e colonne di controllo.
  Sulla tabella dei contenuti la **cancellazione fisica è ammessa** — a differenza della tabella delle azioni
  (storia 0002) — perché questo strato esiste proprio per poter essere cancellato: la deroga inversa rispetto alla
  catena va scritta nel registro delle decisioni, altrimenti il prossimo lettore penserà a un'incoerenza.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata in questa storia.
- **RT-5 — Cinque lingue (§4).** Nessun testo visibile introdotto.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo nuovo della metrica `actions`: il contenuto è un allegato di
  un'azione già contata. La conservazione dei contenuti ha però un costo di deposito reale, ed è una delle
  ragioni per cui si attiva a mano e per un solo strumento alla volta (storia 0031).
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia, e una regola che vale da qui in
  avanti: **il contenuto allegato non esce mai verso un assistente**, in nessuna forma e con nessun ruolo. La
  minimizzazione verso l'assistente è un requisito di piattaforma
  ([UC 0065](../../../12-ready-for-ai-mcp/0065-sicurezza-audit-invocazioni-ai.md)) e qui diventa un divieto secco,
  perché il contenuto è la parte più imprevedibile del sistema.
- **RT-8 — Dati personali (§10).** Questo è **il** punto di massima esposizione dell'app: il contenuto allegato può
  contenere dati di chiunque e di qualunque genere. Voce nel manifesto
  `docs/compliance/manifests/agentaudit.yaml` in italiano e inglese, con la categoria dichiarata per quello che è
  — *contenuto imprevedibile fornito dal titolare* — campo annotato `@PersonalData`, e le tabelle
  `attached_contents` e `content_keys` presenti in `exportData` e `purgeData` del contratto dati dell'app.
- **RT-9 — Registrazione eventi (§14).** Creazione e distruzione di una chiave sono registrate con `tenant_id`,
  `app_id`, `user_id` e identificativo di correlazione. **Nel registro tecnico non finisce mai** né la chiave né
  una porzione di contenuto: è la regola comune («nessun dato personale nei registri»), qui applicata con
  attenzione doppia.

## 4. Criteri di accettazione

**CA-1 — La catena non dipende dal contenuto**
- **Dato** un intervallo di azioni con contenuti allegati, verificato integro
- **Quando** si rendono illeggibili tutti i contenuti dell'intervallo distruggendo la chiave
- **Allora** la verifica di integrità risponde ancora «integra», e dichiara che i contenuti dell'intervallo non
  sono più leggibili

**CA-2 — Il contenuto è cifrato a riposo**
- **Dato** un contenuto allegato scritto
- **Quando** si ispeziona direttamente la tabella sulla base di dati
- **Allora** il valore non è leggibile, e la chiave non si trova nella stessa tabella

**CA-3 — Isolamento fra account sulle chiavi**
- **Dato** due account `A` e `B`, ciascuno con i propri contenuti e le proprie chiavi
- **Quando** si tenta di decifrare un contenuto di `B` con il contesto e la chiave di `A`
- **Allora** l'operazione fallisce, e il tentativo è registrato

**CA-4 — Una chiave distrutta non torna**
- **Dato** una chiave distrutta
- **Quando** si tenta in qualunque modo di leggere un contenuto che dipendeva da lei — dall'applicazione, dalla
  console di amministrazione, dal deposito
- **Allora** il contenuto non è recuperabile, e la risposta dice che è stato reso illeggibile e quando

**CA-5 — Lo strato è vuoto finché nessuno lo chiede**
- **Dato** un account senza alcuno strumento con conservazione attiva
- **Quando** si registrano mille azioni
- **Allora** la tabella dei contenuti resta vuota e nessuna chiave viene creata

## 5. Definizione di fatto

- [ ] **la revisione legale del §6.2 della descrizione dell'applicazione è stata fatta e il suo esito è
      recepito** — voce di sbarramento, prima di tutte le altre;
- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** su cifratura, calcolo delle impronte in presenza e in assenza di contenuto leggibile, e
      ciclo di vita della chiave; prove di **integrazione** sul deposito, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su contenuti e chiavi, compreso il tentativo di decifrare fra account;
- [ ] **prova end-to-end**: **nessun impatto** — la storia non introduce superficie utente; le storie 0031 e 0032
      la introducono e rispondono per sé;
- [ ] **traduzioni**: nessun testo visibile introdotto;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese con la voce del contenuto allegato e delle chiavi,
      campi annotati, tabelle presenti in `exportData` e `purgeData`;
- [ ] **registro delle decisioni** compilato, con le voci obbligatorie su: separazione dei due strati, deroga
      inversa sulla cancellazione fisica dei contenuti, divieto assoluto di esposizione dei contenuti verso il
      canale conversazionale;
- [ ] contratto degli **strumenti conversazionali**: nessuno, e il divieto è dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] documentazione aggiornata dove la storia cambia un comportamento descritto altrove.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0002` | Le impronte della catena devono già esistere per poterle rendere indipendenti dal contenuto |
| storia `0009` | La minimizzazione è l'impostazione predefinita: questo strato è l'eccezione governata |
| **Revisione legale (§6.2 della descrizione)** | È una dipendenza vera e bloccante, non un'avvertenza: senza il suo esito la storia non si implementa |
| Gestione delle chiavi di piattaforma | Dove vivono le chiavi e come si distruggono davvero è una capacità di infrastruttura: se la piattaforma non la offre, va deciso prima di scrivere codice |

## 7. Fuori ambito

- **l'attivazione della conservazione** per uno strumento: storia 0031. Qui si costruisce il contenitore, non si
  decide di riempirlo;
- **la cancellazione su richiesta**: storia 0032;
- **la rivelazione di un contenuto** nella scheda di un'azione: storia 0025;
- **la cifratura dell'intera base di dati**: è materia di infrastruttura di piattaforma, non di questa app, e non
  risolverebbe il problema che questa storia affronta — cifrare tutto con una chiave sola non permette di
  cancellare qualcosa in particolare.

## 8. Punti aperti

- **La granularità della chiave.** Propongo una chiave per account e per periodo (per esempio mensile): abbastanza
  fine da permettere una cancellazione mirata a un intervallo, abbastanza grossa da non moltiplicare le chiavi
  all'infinito. Ma se una richiesta di cancellazione riguarda **una sola persona** e non un periodo, questa
  granularità non basta: servirebbe una chiave per interessato, che è molto più complessa e presuppone di sapere
  chi compare in un contenuto — cosa che per costruzione non sappiamo. **È il limite più serio dell'impianto e va
  detto al legale**, perché potrebbe cambiare la risposta.
- **Dove vivono le chiavi e come si distruggono in modo dimostrabile.** Una chiave «cancellata» che resta in una
  copia di sicurezza non è distrutta. Il rapporto fra distruzione delle chiavi e copie di sicurezza è un punto
  che tocca l'infrastruttura di piattaforma e va chiuso con chi la presidia.
- **Quale algoritmo di cifratura e quale rotazione.** Da decidere con chi presidia la sicurezza, non qui.
