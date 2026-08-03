# 0021 — Note sulle schede

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 04 — Attività e storico della relazione
**Storia**: `0021` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0006`, `0007`, `0013`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come venditore
> voglio scrivere due righe su com'è andata la conversazione, sulla scheda giusta
> così che fra quattro mesi io — o un collega — sappia cosa ci eravamo detti.

**Contesto.** La nota è il campo più usato e meno progettato di qualunque CRM: è lì che finisce tutto ciò che non
ha un campo. È anche, per la stessa ragione, la principale via d'ingresso non presidiata per dati che non
dovrebbero esserci ([application-description.md](../application-description.md) §6). Questa storia la tratta per
quello che è: utilissima, e da accompagnare con un avviso invece che con un controllo.

## 2. Requisiti funzionali

1. **RF-1** — Un utente con un posto può aggiungere una nota a un contatto, un'azienda o una trattativa; la nota
   porta autore e momento.
2. **RF-2** — Una nota si può modificare **entro un tempo breve** da chi l'ha scritta, e si può cancellare
   logicamente; le modifiche non riscrivono la storia in silenzio, la scheda mostra «modificata».
3. **RF-3** — Le note si vedono in ordine cronologico inverso sulla scheda, con le più recenti in cima.
4. **RF-4** — Accanto al campo compare l'avviso di **non inserire dati sensibili** (salute, appartenenza
   sindacale, convinzioni personali), con parole comprensibili e non giuridiche.
5. **RF-5** — Il testo è semplice: niente formattazione ricca, niente allegati.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Le note filtrano per `tenant_id` dal token verificato; il riferimento
  indicato deve appartenere allo stesso account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST|GET|PATCH|DELETE /api/sales/v1/notes[/{id}]` con il
  riferimento nel corpo; validazione della lunghezza massima; errori in `application/problem+json`; OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `note` già creata dalla storia 0002; indice su
  `(tenant_id, reference_type, reference_id, created_at)`.
- **RT-4 — Modulo frontend (§3, §5).** Blocco «Note» nelle schede, con campo di inserimento sempre visibile in
  cima; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, avviso sui dati sensibili e messaggi in `en, it, fr, es, de`; il testo
  della nota resta nella lingua in cui è stato scritto.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Chi non ha un posto riceve `403`.
- **RT-7 — Esposizione conversazionale (§12).** Le note recenti entrano in `summarize_account` (storia 0036) in
  **lettura**. La scrittura di note dalla chat **non** è esposta in questa proposta: una nota è memoria di ciò che
  una persona ha detto, e farla scrivere a un assistente ne cambierebbe la natura probatoria. Scelta dichiarata,
  con il punto aperto qui sotto.
- **RT-8 — Dati personali (§10).** `note.body` è già dichiarata nel manifesto come testo libero: qui si valorizza,
  quindi vanno verificati annotazione `@PersonalData` e presenza in `exportData` e `purgeData`. Nessuna
  rilevazione automatica di contenuto.
- **RT-9 — Registrazione eventi (§14).** «Nota creata/modificata/cancellata» con identificativi e autore; **mai**
  il testo.

## 4. Criteri di accettazione

**CA-1 — Aggiunta e lettura**
- **Dato** un venditore sulla scheda di un'azienda
- **Quando** scrive una nota
- **Allora** compare in cima all'elenco con autore e momento

**CA-2 — Modifica visibile**
- **Dato** una nota scritta poco fa dallo stesso autore
- **Quando** la modifica
- **Allora** il testo cambia e la scheda indica che è stata modificata

**CA-3 — Modifica non consentita**
- **Dato** una nota scritta da un altro utente
- **Quando** si tenta di modificarla
- **Allora** l'operazione è rifiutata

**CA-4 — L'avviso c'è**
- **Dato** il campo di inserimento della nota
- **Quando** la schermata si apre
- **Allora** l'avviso sui dati sensibili è visibile senza dover scorrere e in tutte e cinque le lingue

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede le note di una scheda di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla finestra di modificabilità e di **integrazione** sulla risorsa;
- [ ] prova di **isolamento fra account** sulle note;
- [ ] **prova end-to-end**: nessun impatto sul percorso minimo; coperta da prove d'integrazione, con il motivo nel
      registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, avviso compreso;
- [ ] **manifesto dei dati** verificato per `note.body`, con annotato che è una via d'ingresso non presidiata;
- [ ] **registro delle decisioni** compilato, con annotata la scelta di non far scrivere note dalla chat;
- [ ] contratto degli **strumenti conversazionali**: sola lettura, con la motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sul blocco delle note;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0006`, `0007`, `0013` | Le note si agganciano a schede che devono esistere |

## 7. Fuori ambito

- allegati e file: fuori perimetro in questa proposta (aprirebbero archiviazione, antivirus e limiti di spazio);
- menzioni e notifiche fra colleghi: non previste;
- la rilevazione automatica di contenuti sensibili: tema trasversale di piattaforma.

## 8. Punti aperti

- **Scrittura di note dalla chat.** Escluderla protegge il valore probatorio della nota, ma toglie una comodità
  reale («annota che ha chiesto lo sconto»). È una decisione di prodotto dello sviluppatore; se accolta, la nota
  scritta dall'assistente dev'essere marcata come tale.
