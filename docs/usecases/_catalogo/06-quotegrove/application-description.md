# QuoteGrove — descrizione dell'applicazione

**Numero di catalogo**: 06 · **Tipo**: orizzontale · vendite · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 06](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** QuoteGrove produce preventivi e proposte commerciali: si sceglie un modello, si aggiungono righe
prese dal catalogo di prodotti e servizi, il programma applica il listino giusto per quel cliente, calcola sconti
e imposte e genera un documento stampabile. Il documento si invia al cliente con un collegamento riservato; il
cliente lo apre, lo accetta o lo rifiuta dalla stessa pagina, e l'accettazione lascia una prova conservata
(chi, quando, da quale indirizzo di rete, su quale versione esatta del documento). Un preventivo accettato emette
un evento che le applicazioni a valle — ordini e fatturazione — possono raccogliere per continuare la catena.

**Per chi.** Micro-imprese da 1 a 10 addetti e piccole imprese fino a 50: artigiani, installatori, studi tecnici,
agenzie, consulenti, piccoli fornitori di servizi. Compra il titolare; usano tutti i giorni una o due persone —
chi vende e chi prepara i numeri. Mercato globale con priorità europea.

**Quale problema toglie.** Oggi il preventivo di una micro-impresa nasce in un foglio di calcolo, viene ricopiato
in un documento di videoscrittura, esportato in formato stampabile e mandato per posta elettronica. Da lì in poi
non si sa più niente: se il cliente l'ha aperto, se l'ha perso, quale versione ha in mano, se l'ha accettato per
telefono o per messaggio. Il costo è triplo: i prezzi si sbagliano perché il listino sta in testa a una persona;
i preventivi restano senza risposta perché nessuno li sollecita; e quando il cliente contesta («avevamo detto
un'altra cifra») non c'è nessuna prova di cosa fosse stato accettato. QuoteGrove chiude tutti e tre i buchi con
un solo documento vivo, tracciato e con la prova dell'accettazione allegata.

**Cosa NON fa.**

- **Non emette fatture** e non trasmette nulla a un'autorità fiscale: il preventivo accettato genera un evento,
  la fattura la fa l'applicazione di fatturazione (catalogo 02, con 01 come strato di conformità).
- **Non incassa denaro dei clienti del cliente**: l'acconto si scrive nel preventivo e se ne registra a mano
  l'avvenuto pagamento, ma il denaro non passa da appgrove (motivo nella sezione 5 e nella storia `0023`).
- **Non è una piattaforma di firma elettronica avanzata o qualificata**: raccoglie una accettazione elettronica
  semplice con la sua prova. La firma avanzata e qualificata è il perimetro di SignGrove (catalogo 15).
- **Non gestisce la trattativa commerciale**: opportunità, fasi di vendita e previsioni sono del programma che
  segue clienti e trattative — il CRM, sigla inglese per «gestione delle relazioni con i clienti» (catalogo 04).
- **Non fa computi metrici né analisi dei costi di cantiere**: quello è il verticale edilizia (catalogo 25).
- **Non gestisce il magazzino**: il catalogo prodotti qui è anagrafico e di prezzo, non di giacenza (catalogo 14).

**Rischio di sostituzione da parte dei modelli linguistici.** `rafforzata`. Un assistente generico sa scrivere il
testo di una proposta, ma non sa quale sia il prezzo concordato con quel cliente, non tiene il numero progressivo,
non conserva la prova di cosa è stato accettato e non può mandare al cliente un collegamento che valga come
documento. Il valore sta nei dati proprietari (listini, storico, versioni) e nella prova conservata: la parte che
il modello linguistico sostituisce — scrivere il testo — è quella che qui pesa meno.

---

## 2. Mercato e analisi in rete

> Compilata dopo dodici fra ricerche mirate e letture dirette di pagine ufficiali, con undici fonti citate
> ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4). Ciò che non è stato trovato è dichiarato al §2.7.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| PandaDoc | globale (Stati Uniti) | proposte, documenti, firma elettronica, configuratore di prezzo nei piani alti | gratuito (60 documenti l'anno); Starter 19 $/utente/mese; Business 49 $/utente/mese; Enterprise su richiesta | [pandadoc.com/pricing](https://www.pandadoc.com/pricing/) — pagina ufficiale |
| Proposify | globale (Canada) | proposte con **limite di invii al mese**, firma inclusa | Basic 19 $/utente/mese annuale (29 mensile), **10 invii/mese**; Team 41 $/utente/mese annuale, **30 invii/mese**; Business da 3 900 $/anno, 75 invii/mese; prova 14 giorni | [proposify.com/pricing](https://www.proposify.com/pricing) — pagina ufficiale |
| Qwilr | globale (Australia) | proposte come pagina web, firma, incasso integrato | Starter 35 $/utente/mese annuale (49 mensile); Growth 55 $ (minimo 5 utenti); Scale 75 $ (minimo 10); prova 14 giorni; commissione aggiuntiva sull'incasso 0,25 %→0,05 % | [qwilr.com/pricing](https://qwilr.com/pricing/) — pagina ufficiale |
| Better Proposals | globale (Regno Unito) | proposte da modelli, firma, sollecito automatico come componente aggiuntiva | **non rilevato su pagina ufficiale**: fonti terze indicano Starter 13-19 $/utente/mese, Premium 21-29 $, Enterprise 42-49 $, più ~10 $/utente/mese per i solleciti | [capterra.com — Better Proposals](https://www.capterra.com/p/153794/Better-Proposals/) — sito di comparazione, **non** pagina ufficiale |
| Prevy | Italia | preventivi per artigiani e piccole imprese, documento stampabile, anagrafica clienti | Free 5 preventivi/mese; Professional 9 €/mese, 50 preventivi/mese; Business 24 €/mese, 200 preventivi/mese | [prevy.it](https://prevy.it/) — pagina ufficiale |
| ePreventivo | Italia | preventivi con accettazione online e firma tracciata via codice usa e getta inviato per posta elettronica | Base 29 €/mese (50 documenti); Pro 100 32 €/mese; Pro 200 35 €/mese; Pro 300 38 €/mese | [epreventivo.it](https://www.epreventivo.it/) — pagina ufficiale |

**Lettura.** Il mercato è spaccato in due. I prodotti anglosassoni vendono **per utente** e sono pensati per
squadre di vendita: costano da 19 a 75 dollari a persona al mese e portano funzioni che una micro-impresa non usa
(flussi di approvazione, integrazione con i grandi CRM, spazi di trattativa). I prodotti italiani vendono **a
volume** — tanti preventivi al mese — e costano da 9 a 38 euro al mese in tutto, ma si fermano quasi sempre al
documento: nessuno dei due esaminati dichiara listini differenziati per cliente, versioni dell'offerta o
conversione automatica in fattura. Lo spazio scoperto è esattamente lì: **prezzo a volume come gli italiani,
profondità di flusso come gli anglosassoni**, senza il balzello per utente che le micro-imprese rifiutano.

### 2.2 Prezzi praticati nel dominio

- **Unità di misura prevalente**: posto a sedere (per utente) nel mondo anglosassone; **numero di preventivi o di
  documenti al mese** in quello italiano. Proposify è il caso interessante: vende per utente **e** limita gli
  invii mensili, segno che il costo percepito segue il documento inviato, non la persona.
- **Fasce rilevate su pagina ufficiale**: 19-49 $/utente/mese (PandaDoc), 19-41 $/utente/mese (Proposify),
  35-75 $/utente/mese (Qwilr), 9-24 €/mese piatti (Prevy), 29-38 €/mese piatti (ePreventivo).
- **Piano gratuito**: presente in PandaDoc (60 documenti l'anno) e in Prevy (5 preventivi al mese). Assente in
  Qwilr, Proposify ed ePreventivo.
- **Prova gratuita**: 14 giorni in Proposify e Qwilr; Qwilr dichiara «senza carta di credito».
- Le fasce del catalogo (12-29 €/mese piatti) sono **coerenti** con quanto rilevato sul mercato italiano e più
  basse di quello anglosassone: la scelta del catalogo di andare piatto regge.

### 2.3 Obblighi normativi del settore

1. **Obbligo del preventivo scritto per i professionisti (Italia).** La legge 4 agosto 2017 n. 124, art. 1 comma
   150, ha modificato l'art. 9 comma 4 del decreto-legge 1/2012: il professionista deve rendere noto al cliente,
   **in forma scritta o digitale**, il grado di complessità dell'incarico e tutte le informazioni utili sui costi
   prevedibili fino alla conclusione. La mancanza della prova del preventivo è un elemento di valutazione negativa
   in giudizio quando si tratta di determinare il compenso. **Effetto sul modello dati**: il documento deve poter
   contenere descrizione della complessità e voci di costo prevedibili, e deve esistere la **prova della
   consegna**, non solo del salvataggio. Fonte:
   [ordine degli ingegneri di Rovigo](https://rovigo.ordingegneri.it/blog/2017/09/18/obbligo-di-preventivo-in-forma-scritta-e-indicazione-titoli-professionali-legge-4-agosto-2017-n-124/).
2. **Valore dell'accettazione per via elettronica.** Il regolamento europeo eIDAS (regolamento 910/2014), art. 25,
   stabilisce che a una firma elettronica non possono essere negati effetti giuridici e ammissibilità come prova
   **solo** perché è in forma elettronica o perché non è qualificata; la firma qualificata ha invece l'effetto
   della firma autografa. In Italia l'art. 20 comma 1-bis del Codice dell'amministrazione digitale aggiunge che il
   valore probatorio del documento con firma elettronica **semplice** è liberamente valutato dal giudice in
   relazione alle caratteristiche di sicurezza, integrità e immodificabilità della soluzione adottata, e che
   l'onere della prova sta in capo a chi se ne vuole avvalere. **Effetto sul modello dati e sul prodotto**: un
   clic su «Accetto» vale come firma elettronica semplice ed è ammissibile, ma il suo peso dipende da quanto è
   solida la prova che conserviamo. Da qui l'obbligo, in questa app, di registrare per ogni accettazione:
   identità dichiarata, indirizzo di posta a cui il collegamento è stato inviato, momento esatto, indirizzo di
   rete, **impronta crittografica della versione accettata** e collegamento riservato usato. Fonti:
   [Article 25 eIDAS](https://service.betterregulation.com/document/472304) ·
   [studiolegaleconsolo.it — valore probatorio delle firme elettroniche](https://www.studiolegaleconsolo.it/firme-elettroniche-valore-giuridico-e-probatorio-di-un-contratto-sottoscritto-con-firma-elettronica/).
3. **Destinatario consumatore e contratti a distanza.** Se chi accetta è un consumatore e il contratto si conclude
   a distanza, valgono gli obblighi informativi precontrattuali e il **diritto di recesso di 14 giorni** (direttiva
   2011/83/UE, recepita nel Codice del consumo); se l'informazione sul recesso manca, il termine si estende di
   dodici mesi. **Effetto sul prodotto**: il preventivo deve poter marcare il destinatario come consumatore e
   allegare i testi che l'impresa cliente decide di usare. QuoteGrove **non** scrive quei testi: li ospita.
   Fonte: [MIMIT — diritto di recesso](https://www.mimit.gov.it/it/mercato-e-consumatori/tutela-del-consumatore/diritti-del-consumatore/diritto-di-recesso).
4. **Conservazione.** Il preventivo di per sé non è un documento fiscale e **non ho trovato** un obbligo europeo
   generale di conservarlo per un periodo determinato: diventa rilevante quando è la prova di un contratto, e
   allora il riferimento pratico è il termine di prescrizione ordinaria (dieci anni in Italia, art. 2946 codice
   civile). La durata di conservazione proposta al §6 nasce da qui ed è **da validare**, non rilevata.

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta, da quanto emerge dalle pagine dei concorrenti e dalle recensioni lette:

1. **Fatturazione e contabilità** — trasformare il preventivo accettato in fattura senza ridigitarlo. È la
   richiesta numero uno e la ragione dell'evento della storia `0025`. Dentro la suite è l'app 02; verso l'esterno
   sarebbe un fornitore terzo.
2. **CRM** — il preventivo nasce da un'opportunità e la chiude. Dentro la suite è l'app 04.
3. **Posta elettronica** — invio del collegamento riservato e dei solleciti. **Fornitore esterno che tratta dati
   per nostro conto** (già presente a livello di piattaforma per la posta transazionale, ma qui l'indirizzo del
   destinatario del preventivo è un dato nuovo).
4. **Incasso dell'acconto** — atteso dai clienti, ma per appgrove significherebbe muovere denaro fra il cliente e
   il suo cliente. **Escluso dal perimetro**, vedi §5 e storia `0023`.
5. **Firma elettronica avanzata o qualificata** — chiesta quando l'importo è alto o il destinatario è un ente.
   Dentro la suite è SignGrove (15); verso l'esterno sarebbe un **fornitore esterno** e un servizio fiduciario.
6. **Calendario e agenda** — data di inizio lavori promessa nel preventivo. Bassa priorità.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Cosa chiedono, dalle recensioni lette su un sito di comparazione:

- fare e mandare un preventivo in **pochi minuti** partendo da un modello, non da un foglio bianco;
- **sapere se il cliente l'ha aperto**: è la funzione che più spesso viene citata come motivo per pagare;
- **il sollecito automatico** su chi non risponde (Better Proposals lo vende come componente a parte, segno che ha
  valore di mercato riconosciuto);
- la **conversione in fattura** senza ridigitare.

Cosa rifiutano:

- il **prezzo per utente**: su Qwilr le recensioni segnalano che il modello per posto a sedere «rende difficile
  per una piccola impresa impegnarsi su pacchetti utente più grandi» e impedisce di distribuire il lavoro fra le
  persone. È il segnale più netto emerso ed è la ragione per cui la metrica di quota proposta **non** sono i posti;
- gli strumenti percepiti come **macchinosi**: su Proposify tornano i giudizi «goffo, difficile da usare»;
- i **configuratori di prodotto complessi** e i flussi di approvazione a più livelli: sono funzioni per squadre di
  vendita, non per tre persone.
  Fonti: [Capterra — recensioni Qwilr](https://www.capterra.com/p/143254/Qwilr/reviews/) ·
  [Capterra — Better Proposals](https://www.capterra.com/p/153794/Better-Proposals/).

### 2.6 Fonti consultate

1. **PandaDoc — pagina ufficiale dei prezzi** — https://www.pandadoc.com/pricing/ — fasce per utente 19/49 $ e
   piano gratuito a 60 documenti l'anno: conferma che il documento, non la persona, è l'unità che il mercato conta
   anche dove si vende per posto a sedere.
2. **Proposify — pagina ufficiale dei prezzi** — https://www.proposify.com/pricing — limite esplicito di
   **invii al mese** (10/30/75) e prova di 14 giorni: è la conferma più diretta che la metrica naturale del
   dominio è il preventivo **inviato**, con finestra mensile.
3. **Qwilr — pagina ufficiale dei prezzi** — https://qwilr.com/pricing/ — 35-75 $/utente/mese con minimi di
   utenti e commissione sull'incasso: mostra sia il livello di prezzo del segmento alto sia il costo nascosto
   dell'incasso integrato.
4. **Prevy — pagina ufficiale** — https://prevy.it/ — 0/9/24 €/mese con limite in preventivi al mese: il
   riferimento italiano diretto per la fascia di prezzo e per la forma della quota.
5. **ePreventivo — pagina ufficiale** — https://www.epreventivo.it/ — 29-38 €/mese, accettazione online e firma
   tracciata con codice usa e getta via posta elettronica: dimostra che l'accettazione elettronica semplice è già
   uno standard di mercato in Italia, e a che prezzo.
6. **Capterra — Better Proposals** — https://www.capterra.com/p/153794/Better-Proposals/ — prezzi indicativi e
   dato che il 93 % dei recensori è di piccola impresa; il sollecito automatico è venduto come componente a parte.
7. **Capterra — recensioni Qwilr** — https://www.capterra.com/p/143254/Qwilr/reviews/ — la critica ricorrente al
   prezzo per utente nel segmento piccolo.
8. **Ordine degli ingegneri di Rovigo — legge 124/2017** —
   https://rovigo.ordingegneri.it/blog/2017/09/18/obbligo-di-preventivo-in-forma-scritta-e-indicazione-titoli-professionali-legge-4-agosto-2017-n-124/
   — l'obbligo di preventivo scritto o digitale per i professionisti e il suo contenuto minimo.
9. **Testo dell'art. 25 eIDAS** — https://service.betterregulation.com/document/472304 — nessun effetto giuridico
   può essere negato a una firma solo perché elettronica; la qualificata equivale all'autografa.
10. **Studio legale Consolo — valore probatorio delle firme elettroniche** —
    https://www.studiolegaleconsolo.it/firme-elettroniche-valore-giuridico-e-probatorio-di-un-contratto-sottoscritto-con-firma-elettronica/
    — art. 20 comma 1-bis del Codice dell'amministrazione digitale: la firma semplice è liberamente valutata dal
    giudice e l'onere della prova sta a chi se ne avvale. È la ragione tecnica del registro delle prove.
11. **MIMIT — diritto di recesso** —
    https://www.mimit.gov.it/it/mercato-e-consumatori/tutela-del-consumatore/diritti-del-consumatore/diritto-di-recesso
    — i 14 giorni di recesso nei contratti a distanza e l'estensione a dodici mesi se manca l'informativa.

### 2.7 Cosa NON sono riuscito a determinare

- **Prezzi ufficiali di Better Proposals** — la pagina dei prezzi non è stata rilevata direttamente; i numeri
  riportati vengono da un sito di comparazione e divergono fra loro (13-19 $ per lo stesso piano). Per chiuderlo
  serve una lettura diretta della pagina del fornitore.
- **Quanti clienti micro europei accettino un preventivo con il solo clic**, senza firma avanzata — nessuna fonte
  con dati. È la domanda che decide se l'accettazione semplice basta o se serve subito l'aggancio a SignGrove (15).
- **Costo per documento di un fornitore di firma avanzata o qualificata** in Europa — non rilevato. Serve per
  capire se un piano alto con firma avanzata regge economicamente.
- **Tasso medio di accettazione dei preventivi** nel segmento micro — le cifre in circolazione sono citate dai
  fornitori senza indagine indipendente e non le uso: gli indicatori della storia `0026` misurano il dato del
  cliente, non lo confrontano con una media inventata.
- **Se esista un obbligo di conservazione del preventivo** distinto da quello della fattura in qualche
  giurisdizione europea — non trovato. La durata proposta al §6 è quindi un'ipotesi da validare.

---

## 3. Varco d'identità — le risposte pronte per `new-application`

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `preventivi` | Rispetta `^[a-z][a-z0-9_]{0,30}$`. Segue la convenzione già viva nel repository, dove l'app #1 è `fatture`: identificativo tecnico in italiano, nome commerciale in inglese («QuoteGrove»). Descrive **cosa l'app è** — la casa dei preventivi — e resta valido anche se domani il nome commerciale cambia. Alternativa scartata: `quotes`, più internazionale ma incoerente con `fatture` e ambiguo in inglese (quote = anche citazione). |
| **Modello utente** | `multi` | Il catalogo dice 1-3 persone, e in quelle tre persone i ruoli sono diversi: chi incontra il cliente prepara l'offerta, chi tiene i conti controlla margine e sconto, il titolare approva gli sconti fuori soglia. Un preventivo è un documento che **impegna l'azienda**: serve sapere chi l'ha scritto, chi l'ha inviato e chi ha concesso lo sconto, e con il modello a utente singolo quel «chi» non esiste. Attenzione: `multi` riguarda le persone dell'account, **non** la metrica di quota — i posti a sedere qui non si pagano (§2.5). |
| **Porta locale** | `8106` | Convenzione del kit: 8100 + 06. Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `preventivi_inviati` | È la sola cosa che il piano limita. Il valore che il cliente riceve si materializza quando il preventivo **parte verso il cliente finale**: fino a quel momento è un foglio di brutta copia, e far pagare le bozze punirebbe proprio il modo di lavorare che vogliamo incoraggiare (prova, correggi, versiona). È anche la metrica che il mercato usa davvero: Proposify limita gli *invii al mese*, Prevy i *preventivi al mese* (§2.2). Scartate: i posti a sedere (le recensioni li indicano come il motivo per cui le micro-imprese non comprano, §2.5) e i preventivi *creati* (penalizzano le versioni). |
| **Natura della metrica** | `flow` | È un consumo su una finestra che si azzera: «60 preventivi inviati al mese» significa che a marzo se ne possono inviare altri 60 comunque sia andato febbraio. Non è una giacenza: un preventivo inviato l'anno scorso non occupa nessun posto oggi, e cancellarlo non «libera» niente. Se lo trattassimo a giacenza, un cliente attivo da tre anni si troverebbe bloccato per sempre da documenti vecchi — l'errore più costoso possibile su questo listino. |
| **Colore-categoria e icona** | `violet` · icona `file-signature` | Deve essere lo stesso nel listino (`category`) e nel modulo frontend (`accentToken`). Nel repository i due colori già impegnati dalle app reali sono `green`/`blue` per `fatture` e `blue`/`teal` per `crm` (il listino e il manifesto oggi non coincidono fra loro: **è un disallineamento del repository, da verificare al momento dello scaffolding**). `violet` è libero da app reali — lo usa solo il modulo dimostrativo — ed è vicino ma distinto dal blu della fatturazione: QuoteGrove sta un passo prima nella stessa catena e si vuole che si riconosca come cosa diversa. |

---

## 4. Modello di dominio

**Entità principali**

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `Preventivo` | il documento offerto al cliente | numero progressivo, destinatario, stato, valuta, validità, totali, versione corrente | indirettamente: riferimenti al destinatario |
| `RigaPreventivo` | una voce dell'offerta | descrizione, quantità, unità, prezzo unitario, sconto di riga, aliquota, ordinamento | no |
| `VersionePreventivo` | una revisione congelata del documento | numero di versione, contenuto congelato, impronta crittografica, autore, motivo della revisione | no (l'autore è un utente dell'account) |
| `Destinatario` | l'impresa o la persona a cui si offre | ragione sociale, persona di riferimento, posta elettronica, telefono, indirizzo, identificativo fiscale, natura (impresa/consumatore) | **sì** |
| `VoceCatalogo` | prodotto o servizio riutilizzabile | codice, descrizione, unità di misura, prezzo base, aliquota predefinita | no |
| `Listino` + `VoceListino` | prezzi differenziati per cliente, valuta o quantità | validità, valuta, prezzo, scaglione di quantità | no |
| `RegolaSconto` | sconto ammesso e soglia di approvazione | tipo, valore massimo, soglia oltre la quale serve approvazione | no |
| `ModelloPreventivo` | intestazione, testi standard, condizioni | testi per lingua, condizioni di pagamento e validità predefinite | no |
| `InvioPreventivo` | l'atto di mandare il documento a qualcuno | destinatario dell'invio, momento, collegamento riservato, esito | **sì** (indirizzo di posta) |
| `EventoPreventivo` | cosa è successo al documento | tipo (aperto, scaricato, accettato, rifiutato), momento, indirizzo di rete | **sì** (indirizzo di rete) |
| `ProvaAccettazione` | la prova conservata dell'accettazione | identità dichiarata, posta elettronica, momento, indirizzo di rete, tipo di dispositivo, impronta della versione accettata | **sì** |
| `RichiestaAcconto` | l'acconto chiesto all'accettazione | importo o percentuale, scadenza, stato dichiarato dall'utente | no |
| `EsitoPreventivo` | com'è andata a finire | esito, motivo della perdita, concorrente indicato, note | no |

**Relazioni.** `Preventivo` 1→N `RigaPreventivo`; `Preventivo` 1→N `VersionePreventivo` (una sola corrente);
`Preventivo` N→1 `Destinatario`; `RigaPreventivo` N→1 `VoceCatalogo` (facoltativo: una riga può essere libera);
`Listino` 1→N `VoceListino` e N→1 `Destinatario` quando è dedicato; `Preventivo` 1→N `InvioPreventivo` 1→N
`EventoPreventivo`; `Preventivo` 1→0..1 `ProvaAccettazione`; `Preventivo` 1→0..1 `RichiestaAcconto`;
`Preventivo` 1→0..1 `EsitoPreventivo`.

**Macchina a stati del preventivo** — è il vincolo che tutte le storie devono rispettare:

```
bozza ──invio──▶ inviato ──apertura──▶ visto ──┬── accettazione ──▶ accettato ──▶ (evento verso le app a valle)
  ▲                  │                          ├── rifiuto ───────▶ rifiutato
  │                  │                          └── richiesta di modifica ──▶ in revisione ──▶ bozza (nuova versione)
  └──── nuova versione da un documento non ancora accettato ────────┘
                     └── scadenza della validità ──▶ scaduto ──(riapertura esplicita)──▶ bozza
```

Regole non negoziabili della macchina a stati: un preventivo **accettato è immutabile** (si può solo emettere una
nuova versione, che è un documento nuovo collegato al precedente); un preventivo **scaduto non è accettabile**
finché qualcuno dell'account non ne prolunga esplicitamente la validità; l'accettazione è sempre riferita a **una
versione precisa**, mai «al preventivo» in generale.

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`); schema `app_preventivi`; nessuna chiave esterna verso altri schemi
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8).

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/preventivi.yaml`.

**Ragionamento.** Il riferimento non è il mercato anglosassone per utente (19-75 $ a persona) ma quello italiano a
volume (9-38 €/mese piatti, §2.2), perché il cliente tipo sono tre persone che rifiutano il prezzo per posto a
sedere (§2.5). La fascia del catalogo — 12-29 €/mese piatti — è coerente con quel mercato e la confermo. I limiti
proposti nascono dal confronto diretto: Prevy dà 50 preventivi al mese a 9 € e 200 a 24 €; Proposify, che è un
prodotto più profondo, dà **10** invii al mese al piano base. Poiché QuoteGrove offre più di Prevy (listini,
versioni, prova dell'accettazione, catena verso la fattura) e costa meno per volume di Proposify, i limiti stanno
in mezzo. Non esiste un costo variabile per documento — nessun fornitore si paga a preventivo, l'accettazione è
elettronica semplice e la posta transazionale è già di piattaforma — quindi il margine è pulito e non c'è alcun
motivo tecnico per contare le bozze.

| Piano | Prezzo mensile | Prezzo annuale | Limite su `preventivi_inviati` (al mese) | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | 5 | — | chi vuole vedere il prodotto sul lavoro vero: bastano a capire se serve, non a farci l'anno |
| `pro` | 15 € | 150 € (= 10× il mensile, «due mesi in regalo») | 60 | 14 giorni | l'artigiano o lo studio che manda due o tre preventivi al giorno: è il piano di riferimento |
| `business` | 29 € | 290 € | 300 | 14 giorni | la piccola impresa con più persone che offrono, listini per cliente e volumi da campagna |

**Note obbligate.**

- Tre piani, non di più: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- Un limite lasciato vuoto significa **illimitato**, non zero. Qui nessun piano è illimitato: il tetto è la sola
  leva che distingue `pro` da `business`, e lasciarlo vuoto per sbaglio regalerebbe il piano alto.
- **La prova gratuita su un'app che ha già un piano gratuito è in parte ridondante**, ed è vero anche qui: il
  piano `free` mostra già il valore. La tengo lo stesso, a 14 giorni, per due motivi: è lo standard di piattaforma
  e i concorrenti la offrono (Proposify, Qwilr, Better Proposals); e serve a far provare il **volume**, che è
  esattamente ciò che il piano gratuito non fa provare. Se lo sviluppatore preferisce, disattivarla è legittimo.
- **Costo effettivo dell'incasso**: nessun piano è sotto i 5 €/mese, quindi la parte fissa per transazione non
  mangia il margine. L'annuale resta comunque quello da spingere.
- I prezzi sono **immutabili una volta vivi**: un cambio si fa creando un prezzo nuovo, non modificando l'esistente.
- 🛑 **Fermata di escalation aggiuntiva — l'incasso dell'acconto.** I clienti se lo aspettano (§2.4) e Qwilr lo
  vende con una commissione propria sopra a quella del fornitore di pagamento. Per appgrove significherebbe far
  transitare **denaro di terzi** — il cliente del nostro cliente — con tutto ciò che ne consegue in termini di
  regole sui servizi di pagamento e di rapporto con il venditore di riferimento oggi usato per gli abbonamenti.
  **Non lo propongo e non lo decido**: la storia `0023` si limita a scrivere l'acconto nel documento e a farne
  registrare l'incasso a mano. Se si vuole incassare davvero, è una decisione di direzione di prodotto e di
  conformità, non una funzione da aggiungere.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati
> (`docs/compliance/manifests/preventivi.yaml`) si compila **insieme** allo sviluppatore: «niente contratto, niente
> produzione». Un manifesto inventato è **peggio** di uno assente, perché sembra conformità ed è finzione.

**Categorie particolari (articolo 9): non previste.** Il dominio è commerciale — chi offre, a chi, a che prezzo —
e nessun campo dell'applicazione chiede o suggerisce dati su salute, biometria, genetica, opinioni politiche,
convinzioni religiose, orientamento sessuale o appartenenza sindacale. **Non ammorbidisco la classificazione: la
via d'ingresso esiste comunque ed è il testo libero** (vedi più sotto). Un preventivo per una fornitura sanitaria,
per un ausilio o per un intervento su un luogo di culto può contenere, nella descrizione di una riga, indicazioni
che riguardano la salute o le convinzioni di una persona. L'applicazione non fa e non deve fare analisi del
contenuto: il punto va dichiarato, non risolto qui.

**Categorie trattate**

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `destinatario.ragione_sociale` | `destinatario.ragione_sociale` | cliente dell'account (impresa o persona) | anagrafico — **personale quando il cliente è una ditta individuale o un professionista** | intestare il preventivo | esecuzione di misure precontrattuali richieste dall'interessato | durata del rapporto + 10 anni dall'ultimo documento |
| `destinatario.persona_riferimento` | `destinatario.persona_riferimento` | persona di contatto presso il cliente | anagrafico | sapere a chi ci si rivolge | legittimo interesse del titolare (il cliente di appgrove) a gestire il rapporto commerciale | come sopra |
| `destinatario.email` | `destinatario.email` | persona di contatto | contatto | recapitare il preventivo e i solleciti | esecuzione di misure precontrattuali | come sopra |
| `destinatario.telefono` | `destinatario.telefono` | persona di contatto | contatto | contatto commerciale | legittimo interesse | come sopra |
| `destinatario.indirizzo` | `destinatario.indirizzo` | cliente | anagrafico | intestazione e luogo della prestazione | esecuzione di misure precontrattuali | come sopra |
| `destinatario.identificativo_fiscale` | `destinatario.partita_iva`, `destinatario.codice_fiscale` | cliente | identificativo — **personale se persona fisica** | intestazione corretta e passaggio alla fatturazione | obbligo di legge in capo al titolare quando il documento diventa fattura | come sopra |
| `invio.email_destinatario` | `invio_preventivo.email_destinatario` | persona di contatto | contatto | prova di **a chi** è stato consegnato il documento | esecuzione di misure precontrattuali; prova in giudizio | 10 anni dall'invio |
| `evento.indirizzo_rete` | `evento_preventivo.indirizzo_rete` | persona che apre il collegamento | dato di connessione | dire al cliente se il preventivo è stato aperto | legittimo interesse del titolare, con informativa sulla pagina pubblica | 24 mesi (proposta: gli eventi di sola apertura invecchiano in fretta) |
| `prova.identita_dichiarata` | `prova_accettazione.nome_dichiarato` | persona che accetta | anagrafico | prova di **chi** ha accettato | necessità di far valere un diritto in giudizio | 10 anni dall'accettazione |
| `prova.email` | `prova_accettazione.email` | persona che accetta | contatto | legare l'accettazione al recapito | come sopra | 10 anni |
| `prova.indirizzo_rete` + `prova.agente_utente` | `prova_accettazione.*` | persona che accetta | dato di connessione | robustezza della prova ai sensi dell'art. 20 comma 1-bis del Codice dell'amministrazione digitale | come sopra | 10 anni |
| `riga.descrizione`, `preventivo.note` | testo libero | chiunque venga nominato nel testo | **imprevedibile** | descrivere l'offerta | esecuzione di misure precontrattuali | come il preventivo |

**Ruoli.** Su questi dati **appgrove è responsabile del trattamento** (tratta per conto del cliente, che è il
titolare). Le finalità e le basi giuridiche indicate sopra sono quelle del cliente-titolare: nel manifesto vanno
scritte come tali. Le durate proposte derivano dal ragionamento sulla prescrizione del §2.3 punto 4 e **non sono
un dato rilevato**: vanno validate.

**Esportazione e cancellazione.** Devono comparire **tutte** in `exportData` e `purgeData` del contratto
`PreventiviDataContract`, senza eccezioni: `destinatario`, `preventivo`, `riga_preventivo`, `versione_preventivo`,
`invio_preventivo`, `evento_preventivo`, `prova_accettazione`, `richiesta_acconto`, `esito_preventivo`,
`listino`/`voce_listino` (quando il listino è dedicato a un destinatario), `modello_preventivo` (i testi possono
nominare persone). La cancellazione è **fisica**: sostituire il nome del destinatario con un codice non è
cancellare. Attenzione al caso difficile — **la prova dell'accettazione è al tempo stesso un dato personale e la
prova di un contratto**: quando arriva una richiesta di cancellazione, il conflitto fra il diritto della persona e
il diritto del titolare a far valere un contratto **non lo risolve questa applicazione**; il contratto dati la
esporta e la cancella come le altre, e la decisione su eventuali eccezioni è un punto aperto (§11, punto 4).

**Testo libero.** Le descrizioni di riga, le note interne e i testi dei modelli sono campi liberi: sono l'unico
ingresso non presidiato per categorie particolari. L'applicazione non fa rilevazione di contenuto; l'interfaccia
avvisa («campo a testo libero: non inserire dati sensibili») e il punto resta trasversale.

**Integrazioni esterne.** Riceverebbero dati personali: il **fornitore di posta elettronica** (indirizzo del
destinatario e testo del messaggio — già fornitore di piattaforma, ma con una categoria di interessati nuova: i
clienti dei nostri clienti); in futuro un eventuale **fornitore di firma avanzata o qualificata** (identità del
firmatario) se si aggancia SignGrove (15) a un servizio fiduciario esterno. Vanno entrambi nell'elenco dei
fornitori e nell'informativa.

**Classificazione della change.** Una app nuova introduce finalità nuove e una categoria di interessati che la
piattaforma finora non tratta — **i clienti finali dei nostri clienti** — insieme a dati di connessione conservati
a fini di prova. È un cambiamento **sostanziale**: lo confermo senza attenuanti.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `elenca_preventivi` | `(stato?, destinatario?, periodo?, pagina?) → elenco minimizzato di preventivi` | numero, destinatario, stato, totale, validità | lettura | no |
| `leggi_preventivo` | `(id) → preventivo con righe, totali e versione corrente` | il documento come lo vede chi lavora | lettura | no |
| `elenca_preventivi_in_attesa` | `(giorni_senza_risposta?) → elenco` | i documenti inviati e non ancora accettati o rifiutati | lettura | no |
| `calcola_prezzo` | `(righe, listino?, sconto?, valuta?) → totali` | calcolo puro: non salva nulla | lettura | no |
| `crea_preventivo` | `(destinatario, righe, validita?, modello?) → bozza di preventivo` | crea un documento in stato `bozza` | scrittura | **sì** |
| `aggiorna_righe_preventivo` | `(id, righe) → nuova versione in bozza` | non tocca mai un documento accettato: apre una versione | scrittura | **sì** |
| `invia_preventivo` | `(id, destinatario_invio) → esito dell'invio` | **manda un messaggio a una persona fuori dall'azienda e consuma quota** | scrittura irreversibile verso l'esterno | **sì, obbligatoria** |
| `sollecita_preventivo` | `(id, testo?) → esito dell'invio` | secondo messaggio alla stessa persona | scrittura irreversibile verso l'esterno | **sì, obbligatoria** |
| `registra_esito` | `(id, esito, motivo?) → esito registrato` | chiude il documento come vinto o perso | scrittura | **sì** |

**Quello che non è e non sarà uno strumento: accettare un preventivo.** L'accettazione è un atto del destinatario,
compiuto sulla pagina pubblica, e la sua prova vale in quanto proviene da lui. Nessun assistente, nemmeno con
conferma, può accettare al posto di un cliente: sarebbe la fabbricazione di una prova. Vale anche per il rifiuto.

**Riga di lettura.** Il paio `elenca_preventivi_in_attesa` + `sollecita_preventivo` è il motivo per cui questa app
guadagna dal livello conversazionale più delle sue concorrenti: «chi non mi ha ancora risposto da più di una
settimana? sollecitali» è esattamente il lavoro che nelle micro-imprese non fa nessuno perché nessuno ha tempo di
aprire l'elenco — e il sollecito, che i concorrenti vendono come componente aggiuntiva a pagamento (§2.5), qui
diventa una frase detta a voce, con una conferma prima che parta.

---

## 8. Indice delle epiche e delle storie

### Epica 01 — Fondamenta

Alla fine dell'epica l'app esiste, è vuota, si accende dal catalogo, si apre nel backoffice in cinque lingue e
rifiuta con `429` chi supera il tetto di preventivi inviati del proprio piano.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-servizio.md) | Impianto del servizio | Il servizio `preventivi` nasce dallo scaffolding, risponde su `/api/preventivi/v1`, ha la sua istanza di infrastruttura |
| [0002](01-fondamenta/0002-modello-dati-multi-account.md) | Modello dati multi-account | Schema `app_preventivi`, prima migrazione, tabella dei preventivi con `tenant_id` e colonne di controllo |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Modulo registrato, sezioni, tema chiaro e scuro, cinque lingue, elenco vuoto navigabile |
| [0004](01-fondamenta/0004-abbonamento-e-quota.md) | Abbonamento e quota | Catena dei varchi completa e metrica `preventivi_inviati` con blocco a `429` |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | `./dev.sh services` vede l'app; un insieme di dati inventati la rende dimostrabile in un minuto |

### Epica 02 — Anagrafica, catalogo e listini

Alla fine dell'epica l'app sa **a chi** offre, **cosa** offre e **a che prezzo**, e sa rispondere a una richiesta
di esportazione o cancellazione dei dati di una persona.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-anagrafica-catalogo-e-listini/0006-anagrafica-dei-destinatari.md) | Anagrafica dei destinatari | Chi riceve il preventivo: impresa o consumatore, con persona di riferimento e recapiti |
| [0007](02-anagrafica-catalogo-e-listini/0007-manifesto-dati-e-diritti-dell-interessato.md) | Manifesto dei dati e diritti dell'interessato | Manifesto in italiano e inglese e contratto di esportazione e cancellazione dell'app |
| [0008](02-anagrafica-catalogo-e-listini/0008-catalogo-di-prodotti-e-servizi.md) | Catalogo di prodotti e servizi | Le voci riutilizzabili con unità di misura, prezzo base e aliquota |
| [0009](02-anagrafica-catalogo-e-listini/0009-listini-e-prezzi-differenziati.md) | Listini e prezzi differenziati | Prezzi per listino, per cliente e per scaglione di quantità, con validità nel tempo |
| [0010](02-anagrafica-catalogo-e-listini/0010-sconti-e-soglie-di-approvazione.md) | Sconti e soglie di approvazione | Sconto di riga e di documento, con la soglia oltre la quale serve l'approvazione di chi può |
| [0011](02-anagrafica-catalogo-e-listini/0011-imposte-e-valuta.md) | Imposte e valuta | Aliquote per riga, esenzioni con motivazione, valuta del documento |

### Epica 03 — Redazione dell'offerta

Alla fine dell'epica una persona compone un preventivo completo, lo rivede in più versioni e ne ottiene un
documento stampabile identico a quello che vedrà il cliente.

| # | Storia | In una riga |
|---|---|---|
| [0012](03-redazione-dell-offerta/0012-creazione-del-preventivo-e-delle-righe.md) | Creazione del preventivo e delle righe | Numero progressivo, destinatario, righe dal catalogo o libere, riordino |
| [0013](03-redazione-dell-offerta/0013-calcolo-di-totali-sconti-e-imposte.md) | Calcolo di totali, sconti e imposte | Il motore di calcolo: imponibile, sconti, imposte, arrotondamenti, totale |
| [0014](03-redazione-dell-offerta/0014-modelli-di-preventivo-e-testi-standard.md) | Modelli di preventivo e testi standard | Intestazioni, condizioni e testi ricorrenti riusabili, per lingua |
| [0015](03-redazione-dell-offerta/0015-versioni-dell-offerta.md) | Versioni dell'offerta | Ogni revisione congela una versione con la sua impronta; si confrontano fra loro |
| [0016](03-redazione-dell-offerta/0016-documento-stampabile-e-anteprima.md) | Documento stampabile e anteprima | Il documento generato dalla versione corrente, uguale per chi lo scrive e per chi lo riceve |

### Epica 04 — Invio, accettazione e firma

Alla fine dell'epica il preventivo esce dall'azienda, il cliente lo apre da un collegamento riservato, e chi ha
offerto sa se è stato letto, accettato o rifiutato — con la prova di cosa è stato accettato.

| # | Storia | In una riga |
|---|---|---|
| [0017](04-invio-accettazione-e-firma/0017-invio-al-cliente-con-collegamento-riservato.md) | Invio al cliente con collegamento riservato | L'invio consuma quota, genera il collegamento a scadenza e registra a chi è andato |
| [0018](04-invio-accettazione-e-firma/0018-pagina-pubblica-e-tracciamento-della-visualizzazione.md) | Pagina pubblica e tracciamento della visualizzazione | La pagina senza accesso che il cliente vede, e la registrazione di quando l'ha vista |
| [0019](04-invio-accettazione-e-firma/0019-accettazione-elettronica-con-prova.md) | Accettazione elettronica con prova | Il clic che accetta, e il registro di prova che lo rende difendibile |
| [0020](04-invio-accettazione-e-firma/0020-rifiuto-e-richiesta-di-modifica.md) | Rifiuto e richiesta di modifica | Le due risposte diverse da «sì», e cosa succede al documento |
| [0021](04-invio-accettazione-e-firma/0021-validita-e-scadenza-dell-offerta.md) | Validità e scadenza dell'offerta | Un'offerta scaduta non si accetta; prolungarla è un atto esplicito e tracciato |
| [0022](04-invio-accettazione-e-firma/0022-promemoria-automatici.md) | Promemoria automatici | I solleciti a chi non ha risposto, con limite, sospensione e disattivazione |

### Epica 05 — Esito, acconti e catena del documento

Alla fine dell'epica il preventivo accettato smette di essere un fatto isolato: chiede l'acconto, dichiara com'è
andata, avvisa le applicazioni a valle e alimenta gli indicatori di conversione.

| # | Storia | In una riga |
|---|---|---|
| [0023](05-esito-acconti-e-catena-del-documento/0023-acconto-richiesto-e-registrazione-manuale.md) | Acconto richiesto e registrazione manuale | L'acconto si scrive e si segna incassato a mano: appgrove non muove denaro di terzi |
| [0024](05-esito-acconti-e-catena-del-documento/0024-esito-del-preventivo-e-motivo-della-perdita.md) | Esito del preventivo e motivo della perdita | Vinto o perso, e perché: è il dato che nessuno registra e che serve a tutti |
| [0025](05-esito-acconti-e-catena-del-documento/0025-evento-preventivo-accettato.md) | Evento «preventivo accettato» | L'evento asincrono che apre la catena verso ordini e fatturazione |
| [0026](05-esito-acconti-e-catena-del-documento/0026-indicatori-di-conversione.md) | Indicatori di conversione | Tasso di accettazione, tempo medio di risposta, valore in trattativa |

### Epica 06 — Esposizione conversazionale e prove end-to-end

Alla fine dell'epica ogni funzione dell'app è comandabile da una chat con la regola «l'assistente prepara, la
persona approva», e due percorsi automatici — quello interno e quello del cliente — dimostrano che la catena
regge davvero.

| # | Storia | In una riga |
|---|---|---|
| [0027](06-esposizione-conversazionale-e-prove/0027-strumenti-di-lettura.md) | Contratto degli strumenti di lettura | Elencare, leggere, calcolare: quattro strumenti liberi, con dati minimizzati |
| [0028](06-esposizione-conversazionale-e-prove/0028-strumenti-di-scrittura-con-conferma.md) | Strumenti di scrittura con bozza e conferma | Creare, modificare, inviare, sollecitare: bozza sempre, conferma umana per ciò che esce |
| [0029](06-esposizione-conversazionale-e-prove/0029-percorso-end-to-end-interno.md) | Percorso end-to-end interno | Dalla creazione all'invio, sullo stack locale reale, etichettato `[J-PREVENTIVI]` |
| [0030](06-esposizione-conversazionale-e-prove/0030-percorso-end-to-end-del-destinatario.md) | Percorso end-to-end del destinatario | Il cliente apre il collegamento e accetta; registro di copertura aggiornato |

**Totale**: 6 epiche, 30 storie.

---

## 9. Estensioni della console di amministrazione

Servono tre cose oltre lo standard: una **vista diagnostica sugli invii** (quanti partiti, quanti respinti dal
fornitore di posta, per account e senza contenuti), una **deroga temporanea al tetto di preventivi inviati** per
il cliente che migra il primo mese, e la possibilità di **revocare un collegamento pubblico** compromesso senza
entrare nell'account. Tutto il resto — abilitazioni, fatturazione, richieste di assistenza — è già di piattaforma.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| **02 — BillGrove (Fatturazione)** | alimenta | **La catena del documento contabile**: un preventivo accettato è l'origine della fattura. QuoteGrove emette l'evento `preventivo.accettato` (storia `0025`); BillGrove lo raccoglie. Nessuna chiamata diretta fra le due app: solo eventi asincroni. |
| **01 — InvoiceGrove (conformità e-invoicing)** | a valle, indiretta | Riceve il documento tramite 02, di cui il catalogo (§6) dice che è uno strato di conformità e non un prodotto autonomo. QuoteGrove non lo tocca mai. |
| **03 — CashGrove (incasso crediti)** | a valle, indiretta | Chiude la catena preventivo → ordine → fattura → incasso. L'acconto dichiarato in `0023` è il primo anello che 03 vorrebbe vedere. |
| **04 — LeadGrove (CRM)** | condivide dati con | **Anagrafica clienti condivisa**: il destinatario del preventivo è lo stesso contatto del CRM. Il preventivo nasce da un'opportunità e la chiude, ma la trattativa resta al CRM. Finché la suite non esiste, QuoteGrove tiene la propria anagrafica (`0006`) — con la consapevolezza che quella tabella è la prima candidata a diventare condivisa. |
| **14 — StockGrove (magazzino)** e **29 — ShopGrove (retail)** | condividono dati con | **Catalogo prodotti e listini** (`0008`, `0009`): stesse voci, stessi prezzi. Qui sono anagrafici; là hanno anche giacenza e movimentazione. |
| **15 — SignGrove (contratti e firma)** | complementare, confine da presidiare | QuoteGrove si ferma alla **accettazione elettronica semplice con prova** (`0019`): è ciò che serve al 90 % dei preventivi micro e ciò che i concorrenti italiani già fanno (§2.1). Quando serve una firma **avanzata o qualificata** — importi alti, controparte pubblica, contratto vero e proprio dietro l'offerta — il documento va passato a SignGrove, che ha i servizi fiduciari e l'archivio a norma. **Il confine è netto e va tenuto**: se QuoteGrove si mettesse a fare firma qualificata costruiremmo due volte la stessa cosa, e la costruiremmo male. Il collegamento fra le due app è un punto aperto (§11, punto 3): oggi SignGrove non esiste. |
| **25 — BuildGrove (edilizia)** | si sovrappone a | Fa preventivi anche lui, ma con computo metrico, voci di capitolato e costi di cantiere. La regola proposta: BuildGrove è un **verticale** che sostituisce QuoteGrove per chi lavora in edilizia, non un'aggiunta. Da confermare quando si scriverà la scheda 25. |
| **12 — app di supporto** e verticali con vendita (21, 22, 24) | potenziale consumatore | Chiunque debba mandare un'offerta può farlo con questa app invece di riscriverla. |

**Riga di lettura.** QuoteGrove **ha senso da sola**: un artigiano che vuole solo mandare preventivi decenti e
sapere se sono stati letti compra questa e basta — è la definizione di applicazione piccola e autosufficiente,
venduta ad abbonamento, che il catalogo persegue. Ma è
anche **il primo anello della catena del documento contabile** (6 → 2 → 1 → 3), che il catalogo indica come
l'argomento di vendita più forte della suite: per questo l'evento `preventivo.accettato` di `0025` va scritto
bene fin dall'inizio anche se oggi non lo ascolta nessuno. Un evento pensato male dopo non si cambia più.

**Sovrapposizioni da evitare.** Tre, e tutte e tre sono già state chiuse sopra: la firma avanzata è di SignGrove
(15); la trattativa commerciale è del CRM (04); il computo metrico è di BuildGrove (25). La quarta, meno ovvia:
**la fatturazione non comincia qui**. La tentazione di aggiungere «e poi fai anche la fattura» è forte e sbagliata:
la fattura ha obblighi di trasmissione e conservazione che sono un mestiere a parte.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **Prezzi, limiti e prova gratuita** (§5) | è una fermata di escalation: nessun agente fissa i prezzi | sviluppatore |
| 2 | **Il collegamento pubblico senza autenticazione** rompe l'abitudine dell'invariante «`tenant_id` solo dal token» | la pagina che il cliente apre non ha un token di accesso: il `tenant_id` deve arrivare da un **gettone di capacità firmato dal server**, monouso nello scopo, a scadenza, revocabile, che dà accesso a **un solo** preventivo in sola lettura più l'atto di accettare. È una deviazione da come funziona il resto della piattaforma e va approvata, non decisa da una storia | sviluppatore, prima della storia `0018` |
| 3 | **Rapporto con SignGrove (15)** per la firma avanzata e qualificata | SignGrove non esiste; il confine è tracciato (§10) ma il modo in cui un preventivo «passa» a una richiesta di firma va progettato quando esisteranno entrambe | epica del catalogo 15 |
| 4 | **Cancellazione della prova di accettazione** | è al tempo stesso dato personale e prova di un contratto: cosa prevale quando l'interessato chiede la cancellazione non lo decide questa app | sviluppatore con revisione legale |
| 5 | **Incasso dell'acconto** | i clienti se lo aspettano ma comporta il transito di denaro di terzi (§5) | sviluppatore — direzione di prodotto |
| 6 | **Durata di conservazione** dei documenti e delle prove | proposta a 10 anni per analogia con la prescrizione ordinaria italiana; non è un dato rilevato e cambia per giurisdizione (§2.7) | sviluppatore con revisione legale |
| 7 | **Colore-categoria `violet`** | nel repository il listino e il manifesto delle due app reali oggi non concordano fra loro sul colore: prima di scegliere va capito quale delle due fonti comanda | sviluppatore, al momento dello scaffolding |
| 8 | **Anagrafica destinatari condivisa** con il CRM (04) | oggi QuoteGrove tiene la propria; quando nascerà l'anagrafica condivisa della suite servirà una migrazione | epica della suite |

**Rischi noti**

- **Il collegamento pubblico è la superficie esposta dell'app** — se il gettone è indovinabile, riusabile o senza
  scadenza, un estraneo legge il preventivo di un'altra azienda; sarebbe una violazione di dati con impatto
  diretto. Attenuazione: gettone lungo e casuale, legato a un solo preventivo, con scadenza propria, revocabile
  dalla console (§9) e mai indicizzabile dai motori di ricerca.
- **Il sollecito automatico può diventare molestia** — mandare tre messaggi a un cliente che ha già detto no per
  telefono danneggia la reputazione del nostro cliente. Attenuazione: numero massimo di solleciti, sospensione
  automatica al primo segnale di risposta, disattivazione per singolo preventivo (storia `0022`).
- **Il testo libero è la porta d'ingresso per dati particolari** (§6) — attenuazione: avviso in interfaccia; il
  presidio vero, se servirà, è trasversale.
- **La catena verso la fatturazione non ha oggi nessun ascoltatore** — si rischia di progettare un evento che
  quando servirà non andrà bene. Attenuazione: l'evento porta il documento congelato e la sua impronta, non un
  riferimento da risolvere; è la forma che invecchia meglio.
- **Concorrenza a prezzo più basso in Italia** — Prevy sta a 9 €/mese. Attenuazione: non si compete sul documento
  stampabile ma su prova dell'accettazione, listini e catena verso la fattura, che Prevy non ha (§2.1).

**Fuori dimensionamento**: non applicabile. Sei epiche (fascia 4-7), da quattro a sei storie per epica (fascia
4-8), trenta storie in tutto (fascia 20-45).
