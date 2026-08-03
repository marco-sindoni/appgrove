# 0017 — Verifica del dominio mittente

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 04 — Spedizione e canali
**Storia**: `0017` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`, `0003`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che vuole che i suoi messaggi arrivino davvero
> voglio dichiarare il dominio da cui parto e sapere se è configurato bene
> così da non scoprire dopo la prima campagna che nessuno l'ha ricevuta.

**Contesto.** Google, Yahoo, Microsoft e Apple impongono a chi manda posta in volume l'**autenticazione del
mittente**: tre configurazioni pubblicate nel sistema dei nomi di dominio — SPF (l'elenco dei server autorizzati a
spedire per quel dominio), DKIM (la firma crittografica che dimostra che il messaggio non è stato alterato) e
DMARC (la regola che dice al destinatario cosa fare quando SPF e DKIM non tornano) — con **allineamento** al
dominio che compare nella riga «Da:» del messaggio. Chi non le rispetta non finisce nella cartella della posta
indesiderata: viene **respinto dal server ricevente**
([application-description.md](../application-description.md) §2.3 punto 5).

È la prima storia dell'epica perché è il presupposto di tutte le altre: senza dominio verificato non parte niente,
nemmeno un messaggio di prova. Ed è anche il presidio più economico contro il rischio numero uno dell'app — la
reputazione di invio condivisa fra tutti gli account (§11, rischi noti): chi vuole spedire a una lista comprata
deve prima possedere un dominio e saperlo configurare, che è già una barriera.

## 2. Requisiti funzionali

1. **RF-1** — L'account dichiara uno o più domini mittenti. Per ciascuno l'app mostra i **tre record da
   pubblicare** nel sistema dei nomi di dominio (SPF, DKIM, DMARC), con nome, tipo e valore da copiare, e una
   spiegazione in una riga di che cosa fa ciascuno.
2. **RF-2** — La verifica si può chiedere in qualunque momento («verifica adesso») e viene comunque ripetuta
   **periodicamente**: un dominio verificato oggi può decadere domani, perché il cliente cambia fornitore di
   dominio o qualcuno riscrive i record.
3. **RF-3** — Lo stato del dominio è uno di: `da configurare`, `in verifica`, `verificato`, `decaduto`. È visibile
   nella sezione dei domini e ripetuto nell'intestazione della campagna, dove serve davvero.
4. **RF-4** — Il dominio è verificato **solo** se tutti e tre i controlli passano **con allineamento** al dominio
   della riga «Da:». Due su tre non basta: lo stato resta `da configurare` e l'esito dice quale manca e perché.
5. **RF-5** — Nessun invio parte da un dominio che non sia `verificato`, **in nessun piano compreso il gratuito**.
   Il tentativo viene respinto con un messaggio che dice cosa manca e come rimediare.
6. **RF-6** — Quando un dominio passa a `decaduto`, le campagne programmate che lo usano passano a `bloccata` e il
   cliente riceve un avviso; le campagne `in corso` si mettono in pausa.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `sender_domain` filtra per `tenant_id` preso
  dal token verificato; un `tenant_id` che arrivasse dal corpo o dai parametri viene ignorato. Due account possono
  dichiarare **lo stesso** dominio: la verifica è per account, perché la prova di possesso è per account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/campaigns/v1/sender-domains`,
  `POST /api/campaigns/v1/sender-domains/{id}/verify`, `DELETE /api/campaigns/v1/sender-domains/{id}` (solo
  cancellazione logica). Corpo validato, errori in `application/problem+json`, definizione OpenAPI aggiornata nello
  stesso commit.
- **RT-3 — Persistenza (§8).** Tabella `sender_domain` già creata dalla storia `0002`; qui si aggiungono le colonne
  di esito (`spf_ok`, `dkim_ok`, `dmarc_ok`, `aligned`, `last_checked_at`, `status`) e l'indice per
  `tenant_id, status`. Schema `app_campaigns`, chiave primaria UUID versione 7, colonne di controllo e
  cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione «Domini mittenti» del modulo `campaigns`: elenco con stato, scheda
  del dominio con i tre record da copiare, pulsante di verifica, storico degli esiti. Dati letti con il client
  generato dalla definizione OpenAPI; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe visibili — compresi i nomi dei tre controlli, le spiegazioni e
  i motivi di fallimento — passano dallo spazio-nomi `campaigns` e sono presenti in `en, it, fr, es, de`. I
  **valori dei record** da pubblicare non si traducono: sono configurazione tecnica.
- **RT-6 — Varchi e quota (§6, §7).** Dichiarare e verificare un dominio **non consuma** la metrica
  `messages_sent` (natura `flow`): non è un invio. Serve però l'abbonamento attivo — con abbonamento `canceled` la
  sezione risponde `402`; con `past_due` resta accessibile.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento nuovo di scrittura. Lo stato dei domini entra in
  `salute_della_lista` (storia `0034`), perché «non ti parte niente» ha spessissimo questa causa. Dipendenza
  dichiarata: UC 0061-0063, livello conversazionale di piattaforma non ancora implementato.
- **RT-8 — Dati personali (§10).** **Nessun dato personale nuovo**: un dominio è un dato dell'azienda, non di una
  persona. Attenzione a un punto: l'indirizzo mittente completo (`nome@dominio`) *può* essere un recapito di
  persona fisica in una ditta individuale; si conserva quindi come dato dell'account e si dichiara nel manifesto
  `docs/compliance/manifests/campaigns.yaml` la voce `sender_domain.from_address` con finalità «identificare il
  mittente delle comunicazioni», in italiano e inglese.
- **RT-9 — Registrazione eventi (§14).** «Dominio dichiarato», «verifica eseguita con esito», «dominio decaduto»
  con `tenant_id`, `app_id` (`campaigns`), `user_id` e identificativo di correlazione. Si registra il **nome del
  dominio** (dato dell'azienda) e mai un indirizzo di posta di una persona.

## 4. Criteri di accettazione

**CA-1 — Verifica riuscita**
- **Dato** un dominio dichiarato con i tre record pubblicati correttamente e allineati
- **Quando** l'utente chiede «verifica adesso»
- **Allora** lo stato diventa `verificato`, l'esito mostra i tre controlli verdi e il momento del controllo

**CA-2 — Due controlli su tre non bastano**
- **Dato** un dominio con SPF e DKIM corretti ma senza record DMARC
- **Quando** si chiede la verifica
- **Allora** lo stato resta `da configurare`, l'esito indica **quale** controllo manca e mostra il record da
  pubblicare, e nessuna campagna può usare quel dominio

**CA-3 — Nessun invio da dominio non verificato, gratuito compreso**
- **Dato** un account sul piano gratuito con un dominio in stato `da configurare`
- **Quando** tenta di portare una campagna oltre lo stato «in verifica»
- **Allora** riceve un rifiuto in `application/problem+json` che nomina il dominio e dice cosa pubblicare; la
  campagna resta in `bozza`

**CA-4 — Decadimento**
- **Dato** un dominio `verificato` con una campagna `programmata`, e i record rimossi dal cliente presso il proprio
  fornitore di dominio
- **Quando** la verifica periodica gira
- **Allora** il dominio passa a `decaduto`, la campagna programmata passa a `bloccata` e viene registrato l'evento
  con il motivo

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` che hanno dichiarato **lo stesso** dominio
- **Quando** un utente di `A` chiede l'elenco dei domini e prova a forzare l'identificativo del dominio di `B`
- **Allora** vede solo il proprio, con il proprio stato di verifica; la richiesta sull'altro risponde `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla regola di allineamento e sul calcolo dello stato a partire dai tre esiti; prove di
      **integrazione** sulla risorsa con database effimero, migrazioni vere e interrogazione del sistema dei nomi
      di dominio sostituita da un doppio deterministico;
- [ ] prova di **isolamento fra account** sui domini mittenti, compreso il caso dello stesso dominio su due account;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-CAMPAIGNS]` (storia `0037`) parte dalla verifica del
      dominio, perché senza di essa nessun passo successivo è raggiungibile; voce aggiunta al registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml);
- [ ] **traduzioni** presenti in tutte e cinque le lingue, con i valori dei record esclusi dalla traduzione;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `sender_domain.from_address`, campo annotato
      `@PersonalData`, tabella presente in `exportData` e `purgeData`;
- [ ] **registro delle decisioni** `changes/NNNN-*/decisions.json` compilato, con annotato perché la verifica è
      richiesta anche sul piano gratuito;
- [ ] contratto degli **strumenti conversazionali**: nessuno nuovo; lo stato dei domini entra in
      `salute_della_lista`, con la scelta scritta;
- [ ] controllo automatico di **accessibilità** verde sulla sezione dei domini;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0001` | Il servizio e le rotte devono esistere |
| Storia `0002` | La tabella `sender_domain` nasce nel modello dati |
| Storia `0003` | Serve il guscio del modulo per avere una sezione |
| Scelta del fornitore di consegna della posta ([application-description.md](../application-description.md) §11.2) | I valori dei record SPF e DKIM da far pubblicare al cliente **dipendono dal fornitore**: cambiarlo dopo significa far riconfigurare tutti i clienti |

## 7. Fuori ambito

- il **controllo pre-volo** completo della campagna: è la storia `0018`, che usa questo stato come uno dei suoi
  controlli bloccanti;
- l'invio vero e proprio: è la storia `0019`;
- la reputazione dell'indirizzo di invio condiviso e il tasso di segnalazione: sono la storia `0021`;
- la **registrazione del dominio** presso un fornitore di nomi a dominio: non la facciamo noi, il cliente arriva
  con il suo;
- l'acquisto di un indirizzo di invio dedicato per account: è una scelta di costo di piattaforma
  ([application-description.md](../application-description.md) §5), non di questa storia.

## 8. Punti aperti

- **Frequenza della verifica periodica.** Ogni quanto ricontrollare un dominio verificato è un compromesso fra
  costo e prontezza nel rilevare un decadimento. Proposta: una volta al giorno, e comunque prima di ogni
  spedizione programmata. Chiude lo sviluppatore.
- **Che cosa fare delle campagne `bloccate` per decadimento quando il dominio torna verificato.** Riportarle da
  sole a `programmata` sarebbe comodo e pericoloso: significherebbe far partire un invio senza che nessuno lo abbia
  riautorizzato. La proposta di questa storia è **non** riattivarle: tornano a `bozza` e ripassano dal controllo
  pre-volo. Va confermato.
- **Fornitore di consegna della posta**: fermata di escalation dello sviluppatore
  ([application-description.md](../application-description.md) §11.2), insieme di scelta di costo e di conformità.
