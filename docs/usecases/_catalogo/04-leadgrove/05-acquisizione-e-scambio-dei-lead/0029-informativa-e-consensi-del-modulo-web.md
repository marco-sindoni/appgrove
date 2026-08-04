# 0029 — Informativa e consensi del modulo web

**Applicazione**: 04 — LeadGrove (`sales`) · **Epica**: 05 — Acquisizione e scambio dei lead
**Storia**: `0029` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0011`, `0028`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che raccoglie richieste dal proprio sito
> voglio che il modulo mostri la mia informativa e raccolga i consensi separati, conservandone la prova
> così da poter dimostrare, se qualcuno me lo chiede, che quel contatto l'ho raccolto in modo lecito.

**Contesto.** È la storia che recepisce l'obbligo normativo più concreto dell'intera applicazione. Chi raccoglie
dati direttamente dalla persona deve, **al momento della raccolta**, dire chi è, perché tratta i dati, su quale
base e per quanto; e il consenso al marketing dev'essere separato dalle altre finalità, non pre-spuntato,
documentabile e revocabile
([application-description.md](../application-description.md) §2.3; fonte
[Cyber Security 360](https://www.cybersecurity360.it/legal/privacy-dati-personali/marketing-e-campagne-di-lead-generation-nel-rispetto-del-gdpr-linee-guida/)).
Un modulo pubblicato senza queste cose non è una funzione incompleta: è una raccolta illecita fatta con il nostro
strumento.

## 2. Requisiti funzionali

1. **RF-1** — Ogni modulo web ha un campo obbligatorio per l'**informativa del cliente**: il testo o l'indirizzo
   della sua pagina informativa. Senza, il modulo non si può attivare.
2. **RF-2** — Il modulo pubblico mostra sempre, prima del pulsante di invio, il rimando all'informativa e una
   casella **obbligatoria e non pre-spuntata** per la richiesta di contatto.
3. **RF-3** — La casella per le **comunicazioni commerciali** è separata, **facoltativa** e non pre-spuntata:
   negarla non impedisce l'invio.
4. **RF-4** — Ogni invio conserva la prova: quale testo è stato mostrato, quali caselle sono state spuntate, il
   momento e l'indirizzo dell'informativa in quel momento.
5. **RF-5** — Il contatto creato riceve automaticamente le registrazioni di preferenza corrispondenti (storia
   0011), con origine «modulo web» e base giuridica «consenso».
6. **RF-6** — Se il cliente cambia i testi, i moduli già inviati conservano **quelli di allora**: la prova è
   com'era, non com'è.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Testi e prove appartengono all'account del modulo, ricavato dalla chiave
  pubblica; nessun parametro dell'invio può cambiarne l'attribuzione.
- **RT-2 — Interfaccia di programmazione (§2).** I testi viaggiano nella configurazione del modulo
  (`PATCH /api/sales/v1/web-forms/{id}`), che rifiuta l'attivazione senza informativa con `422` e una spiegazione;
  la rotta pubblica di invio rifiuta gli invii senza la casella obbligatoria; errori in `application/problem+json`;
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Versionamento dei testi del modulo: ogni invio punta alla **versione** dei testi
  mostrata, con migrazione `V<N>__web_form_texts_version.sql`. Sovrascrivere i testi senza versionarli
  distruggerebbe la prova.
- **RT-4 — Modulo frontend (§3, §5).** Sezione di configurazione con i tre testi (informativa, consenso al
  contatto, consenso al marketing) e un'anteprima di come appariranno; solo token del sistema di design; tema
  chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** L'interfaccia di configurazione è nelle cinque lingue; i **testi dei consensi e
  dell'informativa sono del cliente** e restano nella lingua in cui li ha scritti — non si traducono mai
  automaticamente, perché sono dichiarazioni giuridiche e una traduzione automatica ne cambierebbe il significato.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. La configurazione richiede ruolo `owner` o `admin`.
- **RT-7 — Esposizione conversazionale (§12).** Non esposta alla chat in nessuna forma: sono testi con valore
  giuridico e il loro contenuto è responsabilità del cliente.
- **RT-8 — Dati personali (§10).** È la condizione di **liceità** del trattamento della storia 0028. Voci nel
  manifesto per i testi versionati e per le prove del consenso raccolte dal modulo, in italiano e inglese; le
  tabelle entrano in `exportData` e `purgeData`. La prova del consenso **sopravvive alla revoca** e viene meno solo
  con la cancellazione dei dati dell'interessato (stessa regola della storia 0011).
- **RT-9 — Registrazione eventi (§14).** «Consenso raccolto dal modulo» con identificativo del modulo, versione
  dei testi e quali caselle; **mai** il contenuto inviato né il recapito.

## 4. Criteri di accettazione

**CA-1 — Non si pubblica senza informativa**
- **Dato** un modulo web senza informativa
- **Quando** l'amministratore tenta di attivarlo
- **Allora** riceve `422` con la spiegazione, e il modulo resta non attivo

**CA-2 — Consenso obbligatorio non pre-spuntato**
- **Dato** il modulo pubblico aperto
- **Quando** un visitatore lo apre
- **Allora** la casella della richiesta di contatto è **vuota**, e l'invio senza spuntarla è rifiutato

**CA-3 — Consenso al marketing facoltativo**
- **Dato** un visitatore che spunta solo la casella obbligatoria
- **Quando** invia
- **Allora** l'invio riesce, il contatto nasce con «marketing negato» e «contatto ammesso»

**CA-4 — La prova è quella di allora**
- **Dato** un invio fatto con la versione 1 dei testi
- **Quando** il cliente modifica i testi e si consulta la prova dell'invio
- **Allora** compare la versione 1, non quella nuova

**CA-5 — Le preferenze nascono da sole**
- **Dato** un invio con entrambe le caselle spuntate
- **Quando** il contatto viene creato
- **Allora** ha due registrazioni di preferenza con base giuridica «consenso», origine «modulo web» e il momento
  dell'invio

**CA-6 — Isolamento fra account**
- **Dato** due account con moduli attivi
- **Quando** si invia al modulo di `A`
- **Allora** la prova e le preferenze nascono in `A`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`, `compliance`; l'intera suite prima del commit);
- [ ] prove di **unità** sul versionamento dei testi e di **integrazione** sull'invio con e senza consensi;
- [ ] prova di **isolamento fra account** su testi e prove;
- [ ] **prova end-to-end**: coprire ora — l'invio con consensi e la verifica della prova sono parte del passo del
      modulo pubblico nel percorso `[J-SALES]` (storia 0037); voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue per la configurazione, con i testi del cliente esclusi
      dalla traduzione;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per testi versionati e prove, presenti in
      esportazione e cancellazione;
- [ ] **registro delle decisioni** compilato, con annotato perché i testi si versionano;
- [ ] contratto degli **strumenti conversazionali**: non esposta, con la motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sul modulo pubblico, caselle di consenso comprese;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0028` | I testi vivono sul modulo web; le due vanno rilasciate insieme |
| Storia `0011` | Le prove raccolte diventano registrazioni di preferenza |
| Revisione legale (`docs/_REVISIONE-LEGALE.md`) | I testi predefiniti proposti al cliente non sono consulenza legale e vanno guardati da chi di dovere |

## 7. Fuori ambito

- **scrivere l'informativa al posto del cliente**: l'app offre al massimo un testo di partenza marcato
  chiaramente come esempio da adattare, mai un testo presentato come conforme;
- la gestione del doppio consenso con conferma via posta elettronica: sarebbe un invio verso l'esterno;
- il consenso ai cookie sulla pagina del cliente: non è cosa nostra, il nostro modulo non traccia.

## 8. Punti aperti

- **Testo di esempio dell'informativa.** Offrirne uno aiuta molto i clienti piccoli e ci espone: se il testo è
  sbagliato, il cliente dirà che gliel'abbiamo dato noi. Decisione dello sviluppatore, da portare alla revisione
  legale.
- **Confine di responsabilità**: il cliente è titolare, noi responsabili. Dove finisce il nostro dovere quando il
  cliente pubblica un modulo senza informativa valida va scritto nel contratto di trattamento.
