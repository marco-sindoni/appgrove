# 0034 — Contratto degli strumenti di lettura

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 07 — Esposizione conversazionale e prove end-to-end
**Storia**: `0034` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`, `0013`, `0019`, `0030`, `0032` — è la prima dell'epica
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che comanda la propria azienda da una chat
> voglio poter chiedere «posso scrivere a questa persona?», «come è andata l'ultima campagna?» e «la mia lista è
> in salute?»
> così da avere la risposta nel momento in cui mi serve, che è quasi sempre mentre sto per fare qualcosa.

**Contesto.** Il requisito trasversale del catalogo è che ogni funzione sia comandabile da una chat. Il livello
conversazionale **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066): il compito di
questa storia non è costruire il server, ma **dichiarare il contratto** degli strumenti di lettura dentro il
servizio, versionato con esso. In questa app la lettura vale più che altrove: lo strumento `stato_iscritto`
risponde alla sola domanda che nessun assistente generico può risolvere, perché la risposta sta in un registro di
consensi che è dell'account ([application-description.md](../application-description.md) §7). Le letture vengono
prima delle scritture perché sono libere: nessuna di esse può mandare un messaggio a nessuno.

## 2. Requisiti funzionali

1. **RF-1** — Il servizio dichiara cinque strumenti di **sola lettura**: `elenca_campagne`,
   `statistiche_campagna`, `stato_iscritto`, `elenca_segmenti`, `salute_della_lista`.
2. **RF-2** — Ogni strumento dichiara nome stabile, descrizione in lingua naturale, schema dei parametri, schema
   del risultato, marcatura **lettura** e idempotenza. Il contratto è un artefatto del servizio, non una
   configurazione esterna.
3. **RF-3** — `stato_iscritto(recapito)` risponde «contattabile sì o no», **canale per canale**, con il motivo e
   con il riferimento alla registrazione che lo determina: consenso valido, consenso revocato, mai confermato, in
   quarantena, recapito soppresso. Non restituisce il profilo della persona: restituisce la risposta alla domanda.
4. **RF-4** — I risultati sono **minimizzati**: `elenca_campagne` restituisce nome, stato, canale, numero di
   destinatari e momento, e non i contenuti dei messaggi; `elenca_segmenti` restituisce nome, criteri leggibili e
   conteggio, e **non** gli iscritti che vi ricadono.
5. **RF-5** — `statistiche_campagna` e `salute_della_lista` riusano lo **stesso calcolo** delle schermate (storie
   0030 e 0032) e restituiscono «non misurate» dove la misurazione era spenta, mai zero.
6. **RF-6** — Ogni strumento rispetta la matrice dei ruoli dell'interfaccia e la catena dei varchi: quello che un
   utente non può vedere dallo schermo non lo vede nemmeno dalla chat.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Gli strumenti operano nell'account del **chiamante autenticato**:
  nessuno schema contiene un parametro di account, per costruzione. È il presidio più importante della storia,
  perché un parametro di account in uno strumento conversazionale è una porta aperta.
- **RT-2 — Interfaccia di programmazione (§2).** Il contratto vive nel pacchetto `app.appgrove.campaigns.tools`,
  versionato con il servizio; gli strumenti riusano i servizi applicativi esistenti e non interrogazioni proprie,
  altrimenti si duplicano le regole di autorizzazione. Errori nella stessa forma degli altri: `problem+json`.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova sullo schema `app_campaigns`.
- **RT-4 — Modulo frontend (§3, §5).** Nessuna schermata nuova. Nel riquadro «Dalla chat» della panoramica del
  modulo `campaigns` compaiono esempi di domande, come promemoria di cosa si può chiedere.
- **RT-5 — Cinque lingue (§4).** Le **descrizioni** degli strumenti sono in inglese, perché sono destinate a un
  modello linguistico e non a una persona; i testi visibili nell'interfaccia che le citano sono nelle cinque
  lingue `en, it, fr, es, de`. La distinzione va scritta, altrimenti sembra una dimenticanza.
- **RT-6 — Varchi e quota (§6, §7).** Ogni chiamata attraversa la stessa catena delle rotte: `401` senza token,
  `403` con app spenta o ruolo insufficiente, `402` senza abbonamento attivo. La lettura **non** consuma la
  metrica `messages_sent` (natura `flow`), perché non manda niente: la quota è degli invii, non delle domande.
- **RT-7 — Esposizione conversazionale (§12).** È la storia che realizza il requisito per la parte di lettura.
  Dipendenza dichiarata: UC 0061-0063 (architettura del server, autenticazione e consenso delegato, mappatura
  operazioni → strumenti), non ancora implementati: finché non esistono, il contratto è verificabile con prove ma
  non richiamabile da una chat vera.
- **RT-8 — Dati personali (§10).** La minimizzazione di RF-4 **è** una misura di protezione dei dati: un elenco di
  iscritti passato a un modello linguistico è un'uscita di dati personali molto più grande di quanto la domanda
  richiedesse. `stato_iscritto` riceve un recapito come parametro — è inevitabile, perché è la domanda — e per
  questo il recapito **non** finisce nei registri (RT-9). Nessuna voce nuova nel manifesto; il principio va
  scritto nella descrizione dell'app dentro `docs/compliance/manifests/campaigns.yaml`.
- **RT-9 — Registrazione eventi (§14).** Ogni chiamata è registrata con nome dello strumento, `tenant_id`,
  `app_id`, `user_id`, identificativo di correlazione e numero di elementi restituiti; **mai** i parametri, che
  qui contengono recapiti.

## 4. Criteri di accettazione

**CA-1 — Contratto completo**
- **Dato** il servizio avviato
- **Quando** si chiede l'elenco degli strumenti dichiarati
- **Allora** compaiono i cinque, ognuno con schema dei parametri, schema del risultato e marcatura «lettura»

**CA-2 — La domanda che conta ha una risposta motivata**
- **Dato** un iscritto che ha revocato il consenso alla posta elettronica il mese scorso
- **Quando** si chiama `stato_iscritto` con il suo recapito
- **Allora** la risposta è «non contattabile sulla posta elettronica», con il motivo «consenso revocato» e il
  riferimento alla registrazione che lo determina

**CA-3 — Minimizzazione**
- **Dato** una chiamata a `elenca_segmenti`
- **Quando** risponde
- **Allora** il risultato contiene nome, criteri e conteggio, e **nessun** recapito di iscritto

**CA-4 — Nessun parametro di account**
- **Dato** lo schema di ciascuno dei cinque strumenti
- **Quando** lo si ispeziona
- **Allora** non esiste alcun parametro che indichi l'account: l'account viene dal chiamante

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiama `statistiche_campagna` con l'identificativo di una campagna di `B`
- **Allora** riceve un esito «non trovato», identico a quello di un identificativo inesistente

**CA-6 — I registri non contengono recapiti**
- **Dato** una chiamata a `stato_iscritto`
- **Quando** si ispezionano le righe di registro
- **Allora** contengono il nome dello strumento e l'esito, non il recapito passato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`; l'intera suite prima del commit);
- [ ] prove di **unità** sulla minimizzazione dei risultati e di **integrazione** su ciascuno dei cinque
      strumenti, con database effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** su ogni strumento, con tentativi di indicare risorse altrui;
- [ ] prova della **matrice dei ruoli** applicata agli strumenti;
- [ ] **prova end-to-end**: rimando alla storia 0037 per la parte di superficie; qui la copertura è
      d'integrazione, con il motivo annotato nel registro di copertura;
- [ ] **traduzioni**: nessun testo visibile nuovo oltre agli esempi del riquadro «Dalla chat», presenti nelle
      cinque lingue, con la nota sulla lingua delle descrizioni degli strumenti;
- [ ] **manifesto dei dati**: nessuna voce nuova; principio di minimizzazione dichiarato nella descrizione;
- [ ] **registro delle decisioni** compilato, con annotata l'assenza di un parametro di account per costruzione e
      il divieto di registrare i recapiti passati a `stato_iscritto`;
- [ ] contratto degli **strumenti conversazionali** dichiarato e versionato dentro il servizio;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0007`, `0013`, `0019`, `0030`, `0032` | Gli strumenti espongono funzioni che devono già esistere: registro del consenso, segmenti, invii, rapporto, salute della lista |
| UC 0061-0063 (livello conversazionale di piattaforma) | Non implementati: il contratto si dichiara e si prova, ma non è richiamabile da una chat vera finché non esistono |

## 7. Fuori ambito

- il server del livello conversazionale: è di piattaforma;
- gli strumenti di scrittura: storia 0035;
- la generazione del testo di una campagna: storia 0036;
- l'esportazione dei rapporti dalla chat: deliberatamente non esposta (storia 0033).

## 8. Punti aperti

- **Come si comportano gli strumenti quando il livello conversazionale arriverà davvero.** Consenso delegato,
  limiti di frequenza per assistente e tracciamento delle chiamate sono di piattaforma (UC 0062, 0064, 0065): qui
  si dichiara il contratto, non le regole d'uso.
- **Se `stato_iscritto` debba accettare un recapito in chiaro** oppure la sola impronta crittografica: la seconda
  via proteggerebbe di più ma renderebbe lo strumento inservibile da una chat, dove la persona scrive l'indirizzo.
  Proposta: recapito in chiaro con divieto di registrazione; da confermare con la revisione legale insieme al
  punto §11.6b della descrizione.
