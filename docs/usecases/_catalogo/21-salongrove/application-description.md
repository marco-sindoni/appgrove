# SalonGrove — descrizione dell'applicazione

**Numero di catalogo**: 21 · **Tipo**: verticale · beauty e wellness · **Stato del documento**: 🟡 bozza d'autore
**Scheda d'origine**: [catalogo, scheda 21](../appgrove-catalogo-applicazioni.md)
**Ultimo aggiornamento**: 2026-08-03
**Autore**: agente di catalogo (kit d'autore `_kit/`)

> Documento **di proposta**. Prezzi e classificazione dei dati personali sono proposte da confermare dallo
> sviluppatore, non decisioni prese. Vedi le avvertenze nelle sezioni 5 e 6.

---

## 0. La domanda che viene prima di tutte — SalonGrove è un'applicazione o è BookGrove più un dominio?

> ⚠️ **Punto da confermare — decisione dello sviluppatore, con conseguenze economiche dirette.** Questa è la prima
> applicazione **verticale** del catalogo, e la sua ragione d'essere dipende da come si risponde qui. Tutto il resto
> del documento è scritto in modo da restare valido con entrambe le risposte, ma la raccomandazione è una sola e va
> letta prima del resto.

### 0.1 Il fatto di partenza

L'applicazione **07 — BookGrove** (`prenotazioni`) è già scritta per intero: 7 epiche, 34 storie
([07-bookgrove/application-description.md](../07-bookgrove/application-description.md)). La sua stessa descrizione
dichiara al §10 che è «**la base riutilizzabile**» dei verticali beauty, ristorazione, clinica, fitness e
veterinaria, e che «le regole specifiche di un settore non vanno dentro BookGrove, o i verticali erediteranno un
motore pieno di casi particolari che non li riguardano».

Metto quindi le due colonne l'una accanto all'altra, senza sconti.

**Che cosa la scheda di catalogo 21 chiede e BookGrove fa già** — è la maggioranza:

| Richiesta della scheda 21 | Dove sta già in BookGrove |
|---|---|
| Agenda multi-operatore | epica 02 (risorse, orari, chiusure, calcolo degli spazi liberi) + storia `0013` (agenda multi-risorsa) |
| Scheda cliente e storico servizi | storia `0011` (anagrafica) — lo **storico degli appuntamenti** c'è già; manca solo la parte tecnica del trattamento |
| Prenotazione online | epica 04 intera (pagina pubblica, gettone di gestione, difese, lista d'attesa) |
| Promemoria anti mancata presentazione | epica 05 (canali e consenso, promemoria automatici, canale di messaggistica, politica di disdetta, acconto, indicatori di riempimento) |
| «Gestione dei no-show» | stati `non_presentato` e indicatori, storie `0015` e `0026` |

**Che cosa è davvero del settore e in BookGrove non c'è, né deve esserci:**

| Funzione specifica del beauty | Perché non è agenda ordinaria |
|---|---|
| **Servizi con tempi di posa e pause** | un colore è *applicazione 20′ → posa 35′ → finitura 25′*: durante la posa **l'operatore è libero e la poltrona no**. In BookGrove una prenotazione è un intervallo continuo con un vincolo di non sovrapposizione nel database (storia `0014`): il salone che lavora così non ci sta dentro |
| **Scheda tecnica del cliente** | base, ossidante, volume, tempo di posa applicato, risultato ottenuto, prodotto usato. È la memoria professionale del salone e la ragione per cui il cliente non cambia parrucchiere |
| **Prodotti consumati per trattamento** | il magazzino di cabina si scarica alla chiusura del servizio, non alla vendita; e il costo del prodotto (3-7 € su un colore venduto 55-120 €, §2.5) è ciò che trasforma il fatturato in margine |
| **Provvigioni degli operatori** | il *percentualista* è una figura ordinaria del settore (split 50/50, o 70/30 con l'affitto della poltrona, §2.5): il salone deve sapere ogni mese quanto ha prodotto ciascuno e quanto gli spetta |
| **Sedute a pacchetto** | «dieci sedute di pressoterapia» pagate in anticipo e scalate nel tempo — con una conseguenza fiscale che cambia il modello dati (buono monouso o multiuso, §2.3) |
| Chiusura del conto, rivendita al banco, fedeltà | il momento in cui l'appuntamento diventa denaro, che BookGrove dichiara **fuori dal proprio perimetro** |

**Il conto della sovrapposizione**: delle 7 epiche di BookGrove, **sei sarebbero da riscrivere quasi identiche** —
circa 25 storie su 34. Non è una stima al ribasso: il motore di disponibilità, la pagina pubblica, il gettone di
capacità, il motore dei promemoria e la sincronizzazione dei calendari sono i pezzi più costosi e più delicati di
quell'applicazione.

### 0.2 Che cosa la piattaforma permette davvero

Prima di scegliere, tre vincoli non negoziabili
([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §2, §8, §7):

1. **un'app non chiama un'altra app**: l'unica via fra servizi è asincrona a eventi;
2. **niente interrogazioni fra schemi diversi**: `app_salone` non può leggere `app_prenotazioni`;
3. **niente codice condiviso sopra `services/commons`**, che oggi contiene contesto del tenant, mappatura degli
   errori, entità di base e paginazione — non un motore di prenotazione.

Ne discende che **«app autonoma che riusa componenti condivisi» oggi non è realizzabile come descritto**: i
componenti condivisi da riusare *non esistono*. Le vie effettivamente aperte sono due, ed è bene chiamarle col loro
nome:

- **(a) App autonoma** → si **riscrive** il motore di prenotazione dentro `services/salone`, oppure si promuove
  quel motore a libreria condivisa (`services/commons-booking`), che è una **decisione di piattaforma** che nessuno
  ha ancora preso;
- **(b) Verticale di BookGrove** → nessuna app nuova: le funzioni beauty diventano **epiche aggiuntive del servizio
  `prenotazioni`**, con le loro tabelle nello schema `app_prenotazioni`, le loro sezioni nel modulo esistente e un
  **piano in più** nel listino `prenotazioni.yaml`, che accende le sezioni verticali attraverso il campo `features`
  del piano (il modello del listino lo prevede già).

### 0.3 La raccomandazione — **(b)**, verticale di BookGrove

Raccomando la via **(b)**, per quattro ragioni in ordine di peso.

1. **Il costo della via (a) è pagato due volte, sempre.** Scrivere 25 storie che esistono già è la prima metà del
   conto; la seconda è tenerle d'accordo per sempre. Il repository ha già un documento dedicato al fatto che due
   copie della stessa cosa divergono in silenzio senza che nulla diventi rosso
   ([docs/_PARITA-SCAFFOLD.md](../../../_PARITA-SCAFFOLD.md)): una correzione al calcolo degli spazi liberi andrebbe
   applicata in due punti, e il giorno in cui viene applicata in uno solo nessun collaudo se ne accorge.
2. **Quasi tutto ciò che è specifico è additivo.** Scheda tecnica, magazzino di cabina, pacchetti, provvigioni,
   chiusura del conto: sono tabelle nuove e sezioni nuove che **non toccano** il motore esistente. Convivono
   benissimo accanto a esso, e restano spente per chi non ha il piano che le accende.
3. **L'unica eccezione — i tempi di posa — non è un caso particolare del beauty.** L'occupazione a segmenti (una
   fase impegna l'operatore, quella dopo impegna solo la postazione) è la stessa cosa che serve al dentista mentre
   l'impronta prende, all'officina mentre la vernice asciuga, all'ambulatorio veterinario mentre la sedazione fa
   effetto. È quindi una **capacità che manca al motore di BookGrove**, non un capriccio di settore: va aggiunta là,
   dove serve a cinque verticali, non forkata qui, dove servirebbe a uno.
4. **La migrazione del cliente è gratuita in un verso e cara nell'altro.** Un salone che parte con l'agenda e poi
   vuole magazzino e provvigioni, sotto (b) **cambia piano**. Sotto (a) dovrebbe portare clienti e storico da uno
   schema all'altro — e poiché la piattaforma vieta le interrogazioni fra schemi, sarebbe un'esportazione e una
   reimportazione, cioè un progetto.

**Perché la sovrapposizione non è comunque uno spreco, se lo sviluppatore sceglie (a).** La via (a) resta difendibile
per una ragione che non è tecnica: **il mercato non cerca «prenotazioni», cerca «gestionale parrucchieri»**. La
disponibilità a pagare del beauty è più alta (19-49 €/mese contro i 12-25 € di un motore generico, §2.2), il
canale d'acquisto è diverso e il nome sulla porta conta. Se lo sviluppatore sceglie (a), lo spreco si riduce a una
condizione sola e va scritta nel piano di costruzione: **prima si estrae il motore in una libreria condivisa, poi si
scrive SalonGrove sopra**. Riscrivere il motore a mano dentro una seconda applicazione è invece uno spreco puro, e
su questo non ho esitazioni.

**La via di mezzo che chiude il conto commerciale.** Il bisogno di marchio della via (a) si può soddisfare senza il
costo della via (a): **una pagina di presentazione «SalonGrove» che vende il piano `salone` di BookGrove**. Oggi
però le pagine di presentazione sono **per applicazione** (skill `finalize-landing`, UC 0057), non per piano: se un
piano possa avere la propria pagina è un punto aperto di piattaforma, ed è la condizione che rende la
raccomandazione (b) davvero indolore (§11, punto 2).

### 0.4 Come leggere il resto del documento

| Sezione | Sotto la via (a) — app autonoma | Sotto la via (b) — verticale di BookGrove |
|---|---|---|
| §3 varco d'identità | si legge com'è scritta | `app_id`, porta, schema e colore restano quelli di `prenotazioni`; resta valida solo la metrica |
| §4 modello di dominio | tabelle nuove nello schema `app_salone`, **più** le tabelle di BookGrove da riscrivere | tabelle nuove nello schema `app_prenotazioni`, accanto a quelle che ci sono già |
| §5 listino | listino nuovo `salone.yaml` | un **piano in più** dentro `prenotazioni.yaml` |
| §8 epiche e storie | l'epica 01 è lo scaffolding di un'app nuova | l'epica 01 è l'estensione di un'app viva; le altre sei sono identiche |

Le epiche **02-07** sono le stesse in entrambi i casi: sono il lavoro che nessuno ha ancora fatto. È il motivo per
cui questo documento è utile prima ancora che la decisione sia presa.

---

## 1. Descrizione dell'applicazione

**Cosa fa.** SalonGrove porta un salone dalla poltrona alla cassa. Sopra l'agenda descrive i servizi come li
descrive davvero un parrucchiere — un colore non è «cinquanta minuti», è venti minuti di applicazione, trentacinque
di posa in cui l'operatore è libero e la poltrona no, venticinque di finitura — e da lì costruisce un'agenda in cui
tre poltrone reggono più di tre clienti per volta. Tiene la **scheda tecnica** di ciascun cliente (che formula, che
ossidante, che risultato) così che il servizio si possa ripetere identico fra sei settimane. Scarica dal magazzino
di cabina il prodotto consumato quando il servizio si chiude, e da lì sa quanto costa davvero un colore. Chiude il
conto, scala le sedute dei pacchetti prepagati, registra i punti fedeltà e attribuisce ogni riga all'operatore che
l'ha eseguita, così che a fine mese il prospetto delle provvigioni si legga invece di ricostruirlo.

**Per chi.** Il salone di quartiere con **due-quattro postazioni** — la fascia che le fonti di settore indicano
come tipica: il titolare più due o tre fra dipendenti e percentualisti, 280.000-580.000 € lordi annui (§2.5).
Parrucchieri, barbieri, centri estetici, studi di manicure e ciglia. Compra il titolare, che quasi sempre lavora
anche in poltrona; usano tutti i giorni tutti quelli che ci lavorano, dal telefono e da un tablet alla reception,
con le mani spesso occupate. Mercato globale con priorità europea.

**Quale problema toglie.** Oggi quel salone tiene tre cose separate e nessuna delle tre parla con le altre:
l'agenda (quaderno, o un portale), la scheda tecnica (una cartellina di fogli con le formule, che si perde e che
solo chi l'ha scritta sa leggere) e i conti (un registro di cassa, e a fine mese un'ora con la calcolatrice per le
percentuali di ciascuno). Il costo è di quattro tipi. Il **tempo morto**: un'agenda che non conosce i tempi di posa
tiene la poltrona occupata quando l'operatore è libero, e a fine giornata sono due clienti non fatti. La **memoria
persa**: la formula che non si ritrova costa un cliente, perché il colore «di quella volta» non torna. Il
**margine invisibile**: il prodotto di cabina si consuma senza che nessuno lo misuri, e le fonti di settore
indicano riduzioni del 25-40 % del consumo quando lo si misura per servizio (§2.5). Le **percentuali a mano**: il
prospetto del percentualista si ricostruisce a memoria e si discute. In più c'è il costo che il mercato ha già reso
famoso: i portali del beauty risolvono la prima metà del problema e si fanno pagare **una percentuale sul giro
d'affari**, fra 2.500 e 9.000 € l'anno per un salone medio italiano (§2.1).

**Cosa NON fa.**

- **Non emette documenti fiscali e non è un registratore di cassa.** Chiude il conto e registra l'importo; lo
  scontrino elettronico passa da un apparecchio omologato, che è certificazione di prodotto e non integrazione. È
  la ragione per cui l'app 29 ShopGrove è **esclusa** dal catalogo attivo
  ([_escluse/README.md](../_escluse/README.md)): SalonGrove non entra da quella porta.
- **Non calcola buste paga.** Calcola quanto ha prodotto ciascun operatore e quanto gli spetta secondo la regola
  concordata, e si ferma lì: il cedolino è materia riservata (app 10 PayGrove, **esclusa**).
- **Non incassa denaro** dei clienti del salone: né sui pacchetti né sugli acconti. Registra che è stato pagato e
  come, il denaro non passa da appgrove (stessa scelta di BookGrove, storia `0025` di quell'app).
- **Non è un portale né una vetrina**: non porta clienti nuovi, non mette il salone in un elenco confrontabile e
  non prende percentuali. È l'esatto contrario del modello dei portali del beauty, ed è la leva competitiva che la
  scheda di catalogo indica.
- **Non tiene dati sanitari**: nessun campo per patologie, terapie, farmaci, gravidanza, diagnosi. Non è una
  reticenza, è un perimetro con una ragione giuridica precisa — sezione 6, e va letta prima di scrivere codice.
- **Non fa campagne**: prepara l'elenco di chi non torna da troppo tempo e lo consegna a chi manda i messaggi (16
  ReachGrove, o il canale di messaggistica che BookGrove già usa per i promemoria). Non spedisce campagne da sé.
- **Non gestisce turni, presenze e timbrature** del personale: è materia dell'app 11 ShiftGrove, **esclusa** perché
  la rilevazione della prestazione lavorativa ha una disciplina propria. Qui si sa *chi ha eseguito cosa*, che è un
  fatto commerciale, non una timbratura.

**Rischio di sostituzione da parte dei modelli linguistici.** `neutra`, come dice la scheda di catalogo, e per una
ragione che vale la pena esplicitare: il valore di SalonGrove sta in **fatti proprietari che nessun modello può
indovinare** — la formula che è stata usata sei settimane fa, quanto prodotto c'è in cabina adesso, quante sedute
restano su quel pacchetto, quanto ha prodotto Sara questo mese. Un assistente generico può *raccontare* come si fa
un colore; non può dire che il colore di Anna era un 7.3 con ossigeno a 20 volumi e trenta minuti di posa. Il
livello conversazionale rende l'app più comoda — «che formula avevamo usato per Anna?» a mani bagnate è
esattamente il momento in cui una chat vale più di una tastiera — ma non la sostituisce.

---

## 2. Mercato e analisi in rete

> Compilata dopo **nove ricerche mirate** e cinque letture dirette di pagina
> ([GUIDA-AUTORE.md](../_kit/GUIDA-AUTORE.md) §4). Ciò che non è stato trovato è dichiarato al §2.7.

### 2.1 Concorrenti

| Prodotto | Dove | Cosa fa | Prezzo rilevato | Fonte |
|---|---|---|---|---|
| **AgileHair** | Italia | gestionale per parrucchieri ed estetiste: anagrafica, schede trattamento, listini, agenda, promemoria, prenotazione online, magazzino, fedeltà, statistiche | Free 0 €/mese (anagrafica, schede trattamento, listini, **un solo operatore**); Essential **19,90 €/mese** (operatori illimitati, agenda, promemoria automatici, app cliente, prenotazione online); Professional **34,90 €/mese** (magazzino, prima nota, fedeltà, statistiche). IVA esclusa; annuale = «2 mesi gratis»; **pacchetti di messaggi brevi a parte, 60 € per 1.000**; accesso programmatico 15-30 €/mese | [agilehair.it/prezzi](https://agilehair.it/prezzi-programma-parrucchieri-estetiste/) — **pagina ufficiale** |
| **Treatwell** | Europa, beauty | portale di prenotazione più gestionale | due piani (`Starter`, `Advanced`; `Advanced` aggiunge **cassa digitale e magazzino**); **25 % di commissione sui clienti nuovi** arrivati dalla vetrina, **0 % sulle prenotazioni ricorrenti**, **2 % sui prepagamenti in linea**. **I canoni mensili non sono pubblicati** | [treatwell.it/partners/prezzi](https://www.treatwell.it/partners/prezzi/) — **pagina ufficiale, ma senza importi** |
| **Fresha** | globale, forte in Europa | gestionale «gratuito» con incasso integrato e vetrina | piano `Team` **14,95 $ per membro prenotabile al mese**, `Independent` 19,95 $, prova di 7 giorni; **20 % una tantum** sul cliente nuovo che arriva dalla vetrina; incassi **2,29-3,30 % + 0,20 $**; **20 messaggi gratis al mese per membro**, poi a consumo | [costbench.com — scheda prezzi Fresha](https://costbench.com/software/salon-spa/fresha/) — **sito terzo**, che dichiara la verifica su `fresha.com/pricing` |
| **Booksy** | globale, presente in Italia | gestionale più vetrina di quartiere | canone **25-80 €/mese secondo il numero di operatori**, più **20-30 % sui clienti nuovi** dalla vetrina | [biutify.it — commissioni dei software beauty](https://www.biutify.it/guide/commissioni-prenotazione-beauty-quanto-costano) — **guida di settore, non pagina ufficiale** |
| **Uala** | Italia | agenda digitale più vetrina, molto diffusa | **listino non pubblicato**; dichiara oltre 600.000 appuntamenti gestiti al mese | ricerca in rete su `uala.it` / `business.uala.it` — **nessuna pagina di prezzo trovata** |
| **Zenoti, Vagaro, GlossGenius, SalonScale, Vish** | Stati Uniti | gestionali di fascia alta e strumenti specializzati sul consumo di prodotto per servizio | non rilevati (fuori dal segmento micro europeo) | [zenoti.com — magazzino](https://www.zenoti.com/salon-management-software/inventory-management) · [salonscale.com](https://www.salonscale.com/inventory-management) |

**Lettura.** Il mercato si divide esattamente come quello dell'agenda generica, ma con una differenza che conta.
Da una parte i **gestionali italiani a canone piatto** — AgileHair è l'esempio con il listino pubblico — che
costano 20-35 €/mese **per salone, con operatori illimitati**, e che tengono magazzino, fedeltà e statistiche
**nel piano alto**: è la conferma che quelle tre cose sono ciò per cui un salone paga di più. Dall'altra i
**portali** (Treatwell, Fresha, Booksy) che vendono la vetrina e si fanno pagare a percentuale: 2.500-9.000 €
l'anno per un salone medio italiano (§2.2). Lo spazio scoperto è quello che la scheda di catalogo indica: **canone
piatto, nessuna percentuale**, ma con dentro le funzioni verticali che i motori di prenotazione generici non hanno
— tempi di posa, scheda tecnica, cabina, pacchetti, provvigioni.

Una nota che vale come avvertimento: **AgileHair vende gli operatori illimitati a 19,90 €**. Un listino a
postazioni deve saperlo, perché è la prima obiezione che un salone farà (§5).

### 2.2 Prezzi praticati nel dominio

- **Unità di misura**: due scuole opposte. I gestionali italiani vendono **per salone** (AgileHair: operatori
  illimitati dal piano intermedio); i portali internazionali vendono **per operatore prenotabile** (Fresha 14,95 $
  a membro) o **a scaglioni sul numero di operatori** (Booksy 25-80 €).
- **Fasce rilevate su pagina ufficiale**: 19,90-34,90 €/mese, IVA esclusa (AgileHair, Italia). È l'unica fascia che
  ho potuto leggere su una pagina di prezzo ufficiale italiana.
- **Fasce rilevate su fonte terza**: 14,95-19,95 $/mese per membro (Fresha), 25-80 €/mese (Booksy).
- **Costo annuo reale per un salone medio italiano** (200 prenotazioni al mese, scontrino medio 40 €, 96.000 €
  l'anno), secondo la guida di settore: **Treatwell 7.000-9.000 €**, **Booksy 4.500-6.000 €**, **Fresha
  2.500-4.000 €**. Contro i **240-420 € l'anno** di un canone piatto italiano: un rapporto fra dieci e trenta volte.
- **Piano gratuito**: presente, e sempre limitato in modo che serva a provare — AgileHair lascia anagrafica, schede
  e listini ma **un solo operatore**, e toglie proprio l'agenda e la prenotazione in linea.
- **Prova gratuita**: 7 giorni su Fresha. Non ho trovato la durata dichiarata sugli altri.
- **Segnale che pesa sul listino**: i **messaggi si vendono fuori dal canone** in tutti e due i mondi — AgileHair
  60 € ogni 1.000 messaggi brevi, Fresha 20 messaggi gratis al mese per membro e poi a consumo. È lo stesso
  vincolo che BookGrove ha già isolato come punto aperto (§11 punto 5 di quell'app), e qui si conferma identico.
- La fascia indicata dalla scheda di catalogo — **19-49 €/mese per sede** — è coerente con quanto rilevato, e sta
  sopra il canone dei gestionali italiani di pari funzione: la differenza va giustificata con qualcosa, e la
  giustificazione più difendibile è **l'assenza totale di percentuali** insieme alla catena completa
  agenda→cabina→conto→provvigioni.
- La scheda di catalogo propone anche **«opzione 0,5-1 % sui pagamenti»**: è incompatibile con la piattaforma, che
  ammette **solo abbonamento ricorrente** e vieta l'addebito a consumo ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §7).
  Non la propongo, e lo segnalo come punto (§11, punto 3).

### 2.3 Obblighi normativi del settore

1. **Il confine estetico/sanitario è tracciato dalla legge, e traccia anche il nostro perimetro.** La legge 4
   gennaio 1990 n. 1, articolo 1 comma 3, dice: «*Sono escluse dall'attività di estetista le prestazioni dirette
   in linea specifica ed esclusiva a finalità di carattere terapeutico*». L'attività di estetista è definita al
   comma 1 come l'insieme dei trattamenti sulla superficie del corpo il cui scopo esclusivo o prevalente è
   mantenerlo in buone condizioni e migliorarne l'aspetto **estetico**. **Effetto sul prodotto**: il cliente tipo
   di SalonGrove è, per definizione di legge, un soggetto che **non eroga prestazioni terapeutiche** — è l'ancora
   che tiene l'applicazione fuori dal perimetro sanitario, ed è un'ancora che BookGrove, essendo orizzontale, non
   ha. Fonte: [confestetica.it — legge 1/1990](https://www.confestetica.it/legge-190-disciplina-dellattivita-di-estetista-4-gennaio-1990-n-1).
2. **Ma la pratica di settore raccoglie dati sulla salute, e questo l'ancora non lo toglie.** La scheda cliente
   professionale usata dalle estetiste raccoglie, secondo la manualistica di settore, «patologie in corso, farmaci
   attivi, allergie, gravidanza/allattamento, retinoidi orali, fotosensibilizzanti»; i gestionali italiani per
   parrucchieri tengono nella scheda tecnica «allergie, patch test, colore naturale, colore attuale». Sono **dati
   relativi alla salute**, categoria particolare dell'articolo 9, e ci arrivano *dalla pratica del mestiere*, non
   da una nostra scelta. È il punto più delicato dell'applicazione ed è trattato per esteso al §6. Fonti:
   [biutify.it — anamnesi cutanea, scheda cliente pre-trattamento](https://www.biutify.it/didattica/viso/anamnesi-cutanea-pre-trattamento) ·
   [help.icauno.com — scheda tecnica parrucchiere ed estetica](https://help.icauno.com/novita-gestione-scheda-tecnica/).
3. **Il consenso informato dell'estetista è una pratica, non un adempimento nominato.** La legge 1/1990 non
   nomina il «consenso informato», ma insieme al codice civile impone standard di informazione e diligenza verso
   il cliente. **Effetto sul prodotto**: SalonGrove non produce moduli di consenso e non ne certifica la validità
   — al massimo registra che il salone ne ha uno, come **fatto** e con la sua data. Fonte:
   [consavio.com — consenso informato per estetiste](https://www.consavio.com/altre-professioni/consenso-informato-estetista/).
4. **I pacchetti prepagati sono buoni-corrispettivo, e ne esistono due specie con conseguenze diverse.** Il
   decreto legislativo 141/2018, che recepisce la direttiva UE 2016/1065, distingue il **buono monouso** — quando
   al momento dell'emissione si conoscono già natura della prestazione, aliquota e luogo — dal **buono multiuso**,
   quando non si conoscono. Il monouso è rilevante ai fini dell'imposta **all'emissione**; il multiuso **all'atto
   dell'utilizzo**. **Effetto sul modello dati**: «dieci sedute di pressoterapia» e «200 € di credito da spendere
   come vuoi» **non sono la stessa entità** — il pacchetto deve registrare **quale delle due specie è**, la data
   della vendita e la data di ciascun utilizzo, perché è ciò che a valle decide quando l'operazione è rilevante.
   SalonGrove non calcola imposte (lo fanno le app 02 e 01): registra la distinzione e i fatti. Fonte:
   [fiscomania.com — buoni corrispettivo monouso e multiuso](https://fiscomania.com/buoni-corrispettivo-monouso-multiuso/).
5. **Le apparecchiature elettromeccaniche dell'estetista sono regolate** (legge 1/1990, art. 10: caratteristiche
   tecnico-dinamiche e meccanismi di regolazione, con elenco allegato). SalonGrove le può trattare come
   **risorse prenotabili** (una lampada, un macchinario) ma **non** attesta idoneità né manutenzioni obbligatorie:
   sarebbe vendere conformità altrui. Se un giorno servisse, è materia dell'app 52 SafetyGrove, **esclusa**.
6. **Conservazione.** **Non ho trovato** un obbligo generale di conservare le schede tecniche di trattamento per
   un periodo determinato: non sono documenti sanitari né fiscali. Le durate proposte al §6 nascono da un
   ragionamento di minimizzazione, non da una norma rilevata, e sono **da validare**.

### 2.4 Integrazioni attese dal cliente

In ordine di richiesta, ricavato dai listini dei concorrenti e da cosa mettono nei piani alti:

1. **Registratore di cassa telematico / cassa fiscale** — è la prima cosa che un salone chiede quando gli si parla
   di «cassa». **Fuori perimetro**: apparecchio omologato, certificazione di prodotto (motivo dell'esclusione di
   29 ShopGrove). Il conto chiuso in SalonGrove è un documento **interno**.
2. **Terminale di pagamento** — stessa risposta: l'importo si registra, il denaro non passa da qui.
3. **Messaggistica e messaggi brevi** — promemoria (già di BookGrove) e richiami. **Fornitore esterno che
   tratterebbe dati per nostro conto**, e con un costo per messaggio.
4. **Fornitori di prodotti professionali** (listini, ordini) — atteso per il riordino automatico. Nella prima
   versione **no**: il riordino produce una lista da ordinare, non un ordine trasmesso.
5. **Contabilità e fatturazione** — il conto chiuso emette un evento che a valle può diventare ricevuta o fattura
   (app 02, 01). Nessuna chiamata diretta.
6. **Anagrafica clienti condivisa** con il resto della suite — stesso punto aperto di BookGrove (§10).
7. **Fotografie** — la macchina fotografica del telefono, non un fornitore. Le immagini restano nostre e in Europa.

### 2.5 Aspettative funzionali dei clienti micro e piccoli

Cosa chiede davvero un salone di tre poltrone, ricavato dai listini (cosa sta nei piani alti) e dalle fonti di
settore:

- **Che l'agenda capisca i tempi di posa.** È la richiesta numero uno e la più fraintesa: le fonti di settore
  descrivono impostazioni di *processing time* che consentono di «gestire la doppia prenotazione durante servizi
  come il colore», e viste che mostrano insieme «operatori, postazioni, cabine e poltrone» per evitare le
  sovrapposizioni. Senza questo, un'agenda per saloni è un calendario con un altro nome. Fonti:
  [meevo.com — software di prenotazione per saloni](https://www.meevo.com/blog/salon-booking-software/) ·
  [consentz.com — evitare la doppia prenotazione](https://www.consentz.com/salon-scheduling-software/).
- **Che la formula si ritrovi.** Tutti i gestionali italiani esaminati tengono la scheda tecnica con base,
  ossidante, risultato, prodotti usati, storico e foto prima/dopo. È la funzione che distingue un gestionale per
  parrucchieri da un'agenda.
- **Che il magazzino di cabina si scarichi da solo.** Le fonti descrivono lo scarico automatico dei prodotti di
  cabina alla chiusura del servizio e indicano riduzioni del **25-40 % del prodotto consumato** quando il consumo
  si misura per servizio. Il numero è di parte (viene da fornitori di quegli strumenti) e va letto come ordine di
  grandezza, non come promessa. Fonti: [salonscale.com](https://www.salonscale.com/inventory-management) ·
  [zenoti.com](https://www.zenoti.com/salon-management-software/inventory-management).
- **Che le percentuali si calcolino da sole.** La figura del **percentualista** è ordinaria: le fonti di settore
  descrivono uno split «50/50 classico, 70/30 nei saloni che affittano solo l'utilizzo della sedia», e un salone
  con 2-4 postazioni fatto di «il titolare più 2-3 figure fra dipendenti e percentualisti».
- **Che si veda il margine, non solo l'incasso.** Sempre dalla stessa fonte: il costo materiale di un colore è
  3-7 € contro un prezzo finale di 55-120 €, e la rivendita vale il 5-10 % del fatturato di un salone medio
  (12-20 % nei saloni alti, con margine 45-55 %). Fonte:
  [biutify.it — quanto guadagna un parrucchiere in Italia](https://www.biutify.it/guide/quanto-guadagna-parrucchiere-italia).

Cosa rifiuta:

- **la percentuale sul proprio giro d'affari** — è la lamentela strutturale del settore, e il conto (2.500-9.000 €
  l'anno contro 240-420 €) la spiega da solo;
- **essere messo in vetrina accanto ai concorrenti**, con il confronto di prezzo che ne segue;
- **le configurazioni lunghe** — un salone che deve passare due ore a impostare servizi, fasi, dosi e percentuali
  torna al quaderno il giorno dopo. È un vincolo di progetto, non un desiderio: ogni funzione verticale deve
  **funzionare anche a vuoto**, con valori predefiniti sensati, e chiedere la configurazione solo a chi la vuole.

### 2.6 Fonti consultate

1. **AgileHair — prezzi ufficiali** — [https://agilehair.it/prezzi-programma-parrucchieri-estetiste/](https://agilehair.it/prezzi-programma-parrucchieri-estetiste/) —
   il solo listino **ufficiale italiano** che ho potuto leggere per intero: Free / 19,90 / 34,90 €/mese IVA
   esclusa, per salone con operatori illimitati dal piano intermedio, annuale con due mesi in regalo, messaggi
   brevi venduti a parte (60 € ogni 1.000). Magazzino, fedeltà e statistiche stanno nel piano alto: è la fonte
   principale della proposta di listino del §5 e della sua articolazione per funzioni.
2. **Treatwell — pagina prezzi per i partner** — [https://www.treatwell.it/partners/prezzi/](https://www.treatwell.it/partners/prezzi/) —
   pagina ufficiale: 25 % sui clienti nuovi dalla vetrina, 0 % sulle prenotazioni ricorrenti, 2 % sui prepagamenti;
   due piani in cui l'`Advanced` si distingue per **cassa digitale e magazzino**. Gli importi dei canoni **non
   sono pubblicati** (§2.7).
3. **Costbench — scheda prezzi di Fresha** — [https://costbench.com/software/salon-spa/fresha/](https://costbench.com/software/salon-spa/fresha/) —
   sito terzo che dichiara la verifica sulla pagina ufficiale: 14,95 $ per membro prenotabile, 19,95 $ per il
   professionista solo, 20 % una tantum sul cliente nuovo dalla vetrina, incassi 2,29-3,30 %, messaggi inclusi
   fino a 20 al mese per membro. È la fonte dell'unità di misura **per operatore prenotabile**.
4. **Biutify — quanto costano davvero le commissioni dei software beauty in Italia** —
   [https://www.biutify.it/guide/commissioni-prenotazione-beauty-quanto-costano](https://www.biutify.it/guide/commissioni-prenotazione-beauty-quanto-costano) —
   guida di settore (non pagina ufficiale): costo annuo per un salone medio italiano, canone di Booksy a scaglioni
   di operatori. È la fonte del confronto «canone piatto contro percentuale».
5. **Biutify — quanto guadagna un parrucchiere in Italia** —
   [https://www.biutify.it/guide/quanto-guadagna-parrucchiere-italia](https://www.biutify.it/guide/quanto-guadagna-parrucchiere-italia) —
   split 50/50 e 70/30, figura del percentualista, composizione del fatturato (colore 40-55 %, rivendita 5-10 %),
   costo materiale di un colore 3-7 € contro 55-120 € di prezzo, salone tipo con 2-4 postazioni. È la fonte dei
   requisiti su provvigioni, costo del servizio e margine (epiche 04 e 06).
6. **Confestetica — legge 4 gennaio 1990 n. 1** —
   [https://www.confestetica.it/legge-190-disciplina-dellattivita-di-estetista-4-gennaio-1990-n-1](https://www.confestetica.it/legge-190-disciplina-dellattivita-di-estetista-4-gennaio-1990-n-1) —
   articolo 1 comma 3 (esclusione delle finalità terapeutiche) e articolo 10 (apparecchi elettromeccanici). È
   l'ancora giuridica del §6 e del perimetro dichiarato al §1.
7. **Biutify — anamnesi cutanea, la scheda cliente pre-trattamento** —
   [https://www.biutify.it/didattica/viso/anamnesi-cutanea-pre-trattamento](https://www.biutify.it/didattica/viso/anamnesi-cutanea-pre-trattamento) —
   che cosa raccoglie davvero una scheda professionale: patologie, farmaci, allergie, gravidanza. È la prova che
   la pratica del mestiere raccoglie dati dell'articolo 9, e il motivo dell'avviso in testa al §6.
8. **Consavio — consenso informato per estetiste** —
   [https://www.consavio.com/altre-professioni/consenso-informato-estetista/](https://www.consavio.com/altre-professioni/consenso-informato-estetista/) —
   la legge 1/1990 non nomina il consenso informato ma impone informazione e diligenza: da qui la scelta di
   registrarlo come fatto e non come documento prodotto da noi.
9. **Fiscomania — buoni corrispettivo monouso e multiuso** —
   [https://fiscomania.com/buoni-corrispettivo-monouso-multiuso/](https://fiscomania.com/buoni-corrispettivo-monouso-multiuso/) —
   d.lgs. 141/2018 e direttiva UE 2016/1065: è il requisito che spacca in due l'entità «pacchetto» (storia `0020`).
10. **SalonScale e Zenoti — gestione del magazzino di cabina** —
    [https://www.salonscale.com/inventory-management](https://www.salonscale.com/inventory-management) ·
    [https://www.zenoti.com/salon-management-software/inventory-management](https://www.zenoti.com/salon-management-software/inventory-management) —
    scarico automatico alla chiusura del servizio, soglie di riavviso, riduzione del 25-40 % del consumo quando lo
    si misura per servizio. Fonti di parte, usate come ordine di grandezza.
11. **Meevo e Consentz — tempi di posa e doppia prenotazione** —
    [https://www.meevo.com/blog/salon-booking-software/](https://www.meevo.com/blog/salon-booking-software/) ·
    [https://www.consentz.com/salon-scheduling-software/](https://www.consentz.com/salon-scheduling-software/) —
    impostazioni di tempo di posa che consentono di servire un secondo cliente durante il colore, e vista unica di
    operatori, postazioni e cabine. È la fonte del requisito centrale dell'epica 02.
12. **icauno e Primoincloud — scheda tecnica nei gestionali italiani** —
    [https://help.icauno.com/novita-gestione-scheda-tecnica/](https://help.icauno.com/novita-gestione-scheda-tecnica/) ·
    [https://primoincloud.it/](https://primoincloud.it/) —
    che cosa contiene una scheda tecnica di mercato: base, ossidante, risultato, prodotti, foto, **e allergie e
    patch test**. Conferma sia la funzione attesa sia il rischio dell'articolo 9.

### 2.7 Cosa NON sono riuscito a determinare

- **I canoni mensili di Treatwell Italia e di Uala.** Treatwell pubblica la struttura dei piani e le percentuali
  ma **non gli importi**; per Uala non ho trovato nessuna pagina di prezzo pubblica, nonostante sia uno dei
  prodotti più diffusi in Italia. Il confronto commerciale del §2.2 su questi due poggia quindi su una guida di
  settore, non su una fonte primaria, e va riverificato prima di usarlo in una pagina di vendita.
- **Il prezzo di il-software.it** (gestionale italiano che i risultati di ricerca indicano a 12 €/mese): la pagina
  risponde con un errore di autorizzazione e **non l'ho letta**. Non uso quel numero.
- **La diffusione reale del percentualista.** La fonte descrive la figura e gli split praticati, ma **non dice
  quanti saloni italiani lavorino così**. Se fossero una minoranza, l'epica 06 sarebbe meno importante di quanto
  la sto pesando: è il dato che manca per dimensionare correttamente il listino.
- **Il tasso di mancata presentazione nel beauty**, con indagine indipendente. È lo stesso buco dichiarato da
  BookGrove (§2.7 di quell'app) e non l'ho colmato.
- **Come gli altri paesi europei traccino il confine estetico/sanitario.** L'articolo 1 comma 3 della legge 1/1990
  è italiano. Non ho verificato se Francia, Spagna e Germania abbiano una separazione equivalente: la classificazione
  del §6 assume che l'ancora giuridica valga, e questa assunzione **è da verificare** prima di vendere fuori
  dall'Italia (§11, punto 5).
- **Se il consumo di prodotto misurato per servizio regga la promessa del 25-40 %** fuori dai materiali dei
  fornitori che vendono bilance collegate. Uso il numero come indizio, non come argomento di vendita.

---

## 3. Varco d'identità — le risposte pronte per `new-application`

> Queste sei righe sono ciò che la skill `new-application` chiede **prima** di generare qualunque cosa
> ([step-01-identity.md](../../../../.claude/skills/new-application/step-01-identity.md)). L'identificativo
> dell'app finisce nel nome dello schema del database, nei nomi delle code, nella rotta pubblica e nell'istanza
> del modulo di infrastruttura: cambiarlo dopo **non è una rinomina, è una migrazione di dati**.
>
> ⚠️ **La tabella è compilata per la via (a) — applicazione autonoma.** Sotto la via (b) raccomandata al §0 questo
> varco **non si attraversa affatto**: non c'è nessuna applicazione da generare, e i valori validi restano quelli
> di `prenotazioni`. Le differenze sono elencate subito dopo la tabella.

| Voce | Valore proposto | Motivazione |
|---|---|---|
| **Identificativo dell'app** (`app_id`) | `salone` | Rispetta `^[a-z][a-z0-9_]{0,30}$` (6 caratteri, minuscolo, solo lettere). Segue la convenzione viva nel repository, dove le app portano un identificativo tecnico **in italiano** riferito a cosa l'app è, non al nome commerciale: `fatture`, `crm`, `prenotazioni`. `salone` copre parrucchiere, barbiere ed estetista senza sceglierne uno, ed è più stabile del marchio «SalonGrove». Schema del database `app_salone`, rotte `/api/salone/v1/*`. Ho scartato `beauty` (inglese) e `parrucchiere` (esclude estetiste e barbieri). |
| **Modello utente** | `multi` | Il salone tipo è «il titolare più due o tre fra dipendenti e percentualisti» (§2.5): **chi ha fatto cosa** non è un dettaglio, è la base su cui si calcolano le provvigioni e su cui si discute a fine mese. Un modello a utente singolo non saprebbe attribuire una riga di conto a un operatore, e l'epica 06 diventerebbe impossibile. Il professionista solo resta rappresentabile: è un account `multi` con un utente e una postazione. |
| **Porta locale** | `8121` | Convenzione del kit: 8100 + numero di catalogo. Da confermare con `./dev.sh services` al momento dello scaffolding. |
| **Metrica di quota** | `postazioni` | È la **sola** cosa che il piano limita: quante postazioni di lavoro il salone tiene aperte — una poltrona, una cabina, un lettino, un macchinario prenotabile. Cresce esattamente con il valore ricevuto (un salone con sei postazioni fattura circa sei volte uno con una) ed è l'unità con cui **una metà del mercato già si fa pagare**: Fresha vende «per membro prenotabile», Booksy fascia il canone «secondo il numero di operatori» (§2.1), quindi il cliente sa confrontarla. È deliberatamente la **stessa cosa** che BookGrove chiama `risorse_prenotabili`: sotto la via (b) le due metriche coincidono e non serve inventarne una seconda. |
| **Natura della metrica** | `stock` | Tetto su ciò che esiste **ora**: «il piano Salone tiene quattro postazioni aperte; per aprirne una quinta bisogna chiuderne una o passare di piano». Un consumo (`flow`) sarebbe sbagliato per due motivi. Il primo lo ha già argomentato BookGrove: le prenotazioni arrivano anche da una pagina pubblica, per mano di un cliente finale, e una quota esaurita gli risponderebbe `429` facendogli credere che il salone è pieno. Il secondo è nostro: nel beauty il momento in cui la quota si esaurirebbe — dicembre, la settimana prima di Ferragosto — è esattamente il momento in cui il salone **non deve** trovare il programma chiuso. Con un tetto a giacenza il limite si incontra quando si apre una postazione, cioè davanti a un utente autenticato che può capire il messaggio e cambiare piano. |
| **Colore-categoria e icona** | `red` · icona `scissors` (due lame incrociate) | Deve coincidere fra listino (`category`) e modulo frontend (`accentToken`). È l'**ultimo colore libero** fra le app di catalogo scritte: `green` è di 07 BookGrove e dell'app reale `fatture`, `blue` di 04 LeadGrove e del mini-CRM, `teal` di 02 BillGrove e 12 DeskGrove, `amber` di 03 CashGrove, 08 SpendGrove e 14 StockGrove, `violet` di 06 QuoteGrove, 13 FlowGrove e 16 ReachGrove. **L'obiezione va detta**: nel sistema di design `--cat-red` ha lo stesso valore di `--danger` (227 101 79), un terracotta caldo, e 16 ReachGrove ha scartato `red` proprio per non confondere il colore dell'app con quello dell'errore. Qui l'obiezione pesa meno — SalonGrove ha poche azioni distruttive e i suoi avvisi tipici sono di scorta bassa, che è ambra — e il terracotta caldo è **in tono col dominio**. Ma resta vero che con sei colori e sessanta app la collisione è strutturale: è un punto aperto di piattaforma (§11, punto 8). Sotto la via (b) la questione non si pone: si eredita `green` da BookGrove. |

**Come cambia il varco sotto la via (b) — verticale di BookGrove.**

| Voce | Valore sotto la via (b) |
|---|---|
| `app_id` | `prenotazioni` — nessuna app nuova, nessuna istanza del modulo di infrastruttura, nessuno schema nuovo |
| Modello utente | `multi`, già così |
| Porta locale | `8107`, già assegnata |
| Metrica di quota | `risorse_prenotabili` — **è la stessa cosa che qui chiamo `postazioni`**; il nome resta quello di BookGrove |
| Natura | `stock`, già così e già argomentata |
| Colore | `green`, già così |
| Listino | non un file nuovo, ma un **piano in più** dentro `prenotazioni.yaml`, che accende le sezioni verticali con il campo `features` del piano |
| Schema | tabelle nuove dentro `app_prenotazioni`, con prefisso di nome che le tenga riconoscibili (`salone_*`) |

---

## 4. Modello di dominio

**Entità principali.** Sono le entità **specifiche del verticale**. Le entità dell'agenda — `Servizio`, `Risorsa`,
`RegolaDisponibilita`, `Chiusura`, `Cliente`, `Prenotazione`, `ListaAttesa`, `Promemoria` — sono quelle di
BookGrove (§4 di quell'app) e **non si riscrivono qui**: sotto la via (b) sono le stesse righe delle stesse
tabelle, sotto la via (a) andrebbero replicate.

| Entità | Cosa rappresenta | Attributi rilevanti | Contiene dati di persone? |
|---|---|---|---|
| `FaseServizio` | un pezzo di un servizio, con la sua durata e chi impegna | servizio, ordine, durata, tipo di risorsa impegnata (operatore, postazione, macchinario), operatore libero durante la fase sì/no | no |
| `VarianteServizio` | il supplemento che cambia durata e prezzo per quel cliente | servizio, nome (lunghezza, ricrescita, doppia applicazione), minuti in più, importo in più | no |
| `SchedaTecnica` | come è stato eseguito un servizio, per poterlo rifare | cliente, servizio eseguito, data, operatore, formula (base, tono, ossidante e volume, minuti di posa applicati), risultato annotato, prodotti usati, esito | **sì**, è legata al cliente ed è il cuore della sezione 6 |
| `FotoTrattamento` | l'immagine prima/dopo di un servizio | scheda tecnica, momento, immagine, consenso registrato, scadenza | **sì**, ed è la voce più identificante dell'app |
| `Prodotto` | ciò che il salone compra | codice, marca, linea, formato, uso (cabina o rivendita), costo d'acquisto, prezzo di vendita | no |
| `Giacenza` | quanto ce n'è, dove | prodotto, deposito (cabina o rivendita), quantità, soglia di riavviso | no |
| `MovimentoMagazzino` | ogni variazione di giacenza | prodotto, deposito, tipo (carico, consumo per servizio, vendita, rettifica, reso), quantità, riferimento all'origine, chi | in via indiretta: l'operatore che l'ha causato |
| `DosePrevista` | quanto prodotto consuma di norma un servizio | servizio, prodotto, quantità predefinita, unità | no |
| `Conto` | il conto del cliente a fine servizio | cliente, prenotazioni collegate, righe, totale, sconto, stato, modo d'incasso dichiarato, data di chiusura | sì, per via del cliente |
| `RigaConto` | una voce del conto | conto, tipo (servizio o prodotto), riferimento, quantità, importo, **operatore attribuito**, origine (a listino, da pacchetto, da premio fedeltà) | sì, indirettamente (l'operatore) |
| `Pacchetto` | sedute o credito pagati in anticipo | cliente, **specie (a sedute determinate = buono monouso; a valore = buono multiuso)**, servizio o servizi ammessi, sedute totali e residue, valore residuo, prezzo pagato, data di vendita, scadenza, stato | sì, per via del cliente |
| `UtilizzoPacchetto` | ogni volta che si scala | pacchetto, conto, sedute o valore scalati, data, chi | sì, indirettamente |
| `TesseraFedelta` e `MovimentoPunti` | il saldo punti del cliente e la sua storia | cliente, saldo, movimento (maturato, speso, scaduto), riferimento al conto | sì, per via del cliente |
| `RegolaProvvigione` | come si calcola quanto spetta a un operatore | operatore (o ruolo), base (servizi, rivendita, entrambi), percentuale, scaglioni facoltativi, validità da/a | sì: riguarda una persona che lavora, ed è un dato del rapporto di lavoro |
| `ProspettoProvvigioni` | il conteggio chiuso di un periodo | periodo, operatore, base maturata per tipo, importo calcolato, stato (aperto, chiuso), chi l'ha chiuso e quando | **sì** |

**Relazioni e macchine a stati.**

Un `Servizio` (di BookGrove) ha da una a molte `FaseServizio` in ordine: è la novità che rende il verticale
possibile. Una `Prenotazione` di un servizio a fasi **occupa risorse diverse in momenti diversi** — l'operatore
nella prima e nella terza fase, la postazione per tutte e tre — e questo è ciò che permette a un operatore di
prendere un secondo cliente durante la posa del primo.

Il `Conto` nasce da una o più prenotazioni eseguite e ha una macchina a stati corta e severa:

```
aperto ──▶ chiuso ──▶ (nessun ritorno)
   │
   └──▶ annullato (solo finché è aperto)
```

Chiudere un conto è l'atto che fa scattare tre effetti insieme e **non si annulla**: scarica il magazzino di
cabina, scala le sedute del pacchetto, matura le provvigioni e i punti fedeltà. Una correzione dopo la chiusura si
fa con una **riga di rettifica**, che resta visibile, non cancellando il conto. È la stessa logica per cui una
scrittura contabile si storna e non si gomma.

Il `Pacchetto` ha la sua:

```
venduto ──▶ in uso ──▶ esaurito
    │          │
    │          └──▶ scaduto
    └──▶ annullato (solo se nessuna seduta è stata usata)
```

**Vincoli che discendono dalla piattaforma.** Ogni tabella porta `tenant_id`, chiave primaria UUID versione 7,
colonne di controllo (`created_at`, `updated_at`, `created_by`, `updated_by`) e cancellazione logica
(`deleted_at`); schema `app_salone` sotto la via (a), `app_prenotazioni` sotto la via (b); nessuna chiave esterna
verso altri schemi ([PRINCIPI-APPGROVE.md](../_kit/PRINCIPI-APPGROVE.md) §8).

**Una precisazione sui movimenti di magazzino.** `MovimentoMagazzino` è **immutabile**: non si modifica e non si
cancella logicamente: si aggiunge un movimento contrario. La giacenza è la somma dei movimenti, e tenerla anche
come colonna è una comodità di lettura, non la verità. Chi la tratta come verità prima o poi la trova diversa dalla
somma, e non sa più quale delle due credere.

---

## 5. Proposta di listino

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Prezzi, limiti dei piani e durata della
> prova gratuita non li fissa un agente. Quanto segue è una proposta motivata, da validare prima di scrivere il
> file `services/core/src/main/resources/pricing/salone.yaml` (via a) o di aggiungere il piano a
> `prenotazioni.yaml` (via b).

**Ragionamento.** Quattro numeri guidano la proposta.

1. L'unica **fascia ufficiale italiana** che ho letto è 19,90-34,90 €/mese, IVA esclusa, **per salone con
   operatori illimitati** (AgileHair, §2.1), con magazzino, fedeltà e statistiche nel piano alto.
2. La scheda di catalogo indica **19-49 €/mese per sede**: coerente, e sta un gradino sopra il gestionale
   italiano di pari funzione.
3. I portali costano a un salone medio italiano **2.500-9.000 € l'anno** (§2.2): un canone annuo di 300-500 € è un
   argomento di vendita che si spiega in una frase, e il confronto lo fa il cliente da solo.
4. La metrica è a giacenza sulle **postazioni**, quindi i piani si distinguono per quante postazioni il salone
   tiene aperte — che è come si fa pagare metà del mercato.

| Piano | Prezzo mensile | Prezzo annuale | Limite su `postazioni` | Prova gratuita | A chi è rivolto |
|---|---|---|---|---|---|
| `free` | — | — | 1 postazione | — | il professionista solo, o il salone che vuole provare: agenda, clienti, catalogo dei servizi a fasi e scheda tecnica. **Senza** magazzino, pacchetti, fedeltà, provvigioni e foto |
| `salone` | 29 € | 290 € (= 10× il mensile, «due mesi in regalo») | 4 postazioni | 14 giorni | il salone di quartiere: tutto il gratuito più magazzino di cabina e consumi, chiusura del conto, pacchetti prepagati, fedeltà, provvigioni e fotografie del trattamento |
| `sede` | 49 € | 490 € | 12 postazioni | 14 giorni | la sede grande o il salone con cabine e macchinari: come `salone`, più gli indicatori di rendimento e l'elenco dei clienti da richiamare |

**Note obbligate.**

- **Tre piani, non di più**: aggiungerne è facile, toglierne quando qualcuno ci sta sopra è difficile.
- Un limite lasciato vuoto significa **illimitato**, non zero: qui nessun limite è vuoto, tutti e tre hanno un
  tetto esplicito.
- **La prova gratuita accanto a un piano gratuito è in parte ridondante**, ma qui non del tutto: il piano gratuito
  esclude esattamente le funzioni per cui si paga (cabina, pacchetti, provvigioni), e quattordici giorni servono a
  farle toccare con mano dentro il proprio salone, con i propri prodotti. Se lo sviluppatore preferisce, si toglie
  dal piano `salone` e resta solo su `sede`.
- **Costo effettivo dell'incasso**: 29 € è ampiamente sopra la soglia dei ~5 €/mese sotto la quale la parte fissa
  per transazione diventa insostenibile. Nessun segnale su questo fronte.
- ⚠️ **L'obiezione che il salone farà, e a cui bisogna avere una risposta.** AgileHair vende **operatori
  illimitati a 19,90 €** e SalonGrove ne venderebbe quattro a 29 €. La risposta non può essere sul prezzo, deve
  essere sul contenuto: nessun concorrente italiano a canone piatto che ho esaminato mette insieme *tempi di
  posa + cabina misurata per servizio + pacchetti con la loro natura fiscale + provvigioni*. Se lo sviluppatore
  non se la sente di difendere quella differenza, l'alternativa onesta è **passare a un tetto per salone** (piano
  unico a 29 € con postazioni illimitate) — ma allora **la metrica di quota va ripensata**, perché una app senza
  metrica non sta in piedi nella piattaforma. È il punto 3 del §11.
- ⚠️ **L'opzione «0,5-1 % sui pagamenti» della scheda di catalogo non è proponibile**: la piattaforma ammette solo
  abbonamento ricorrente e vieta l'addebito a consumo. Se un giorno appgrove volesse incassare per conto dei
  clienti, non sarebbe una riga di listino, sarebbe un cambio di modello.
- ⚠️ **I messaggi restano il punto debole ereditato.** SalonGrove usa i promemoria e i richiami, e il messaggio si
  paga a consumo: tutti i concorrenti esaminati lo vendono fuori dal canone (AgileHair 60 € ogni 1.000; Fresha 20
  inclusi al mese per membro). Il punto è già aperto in BookGrove (§11 punto 5 di quell'app) e **non lo riapro
  qui**: qualunque decisione si prenda là vale anche qui, ed è una ragione in più per non avere due app.
- I prezzi sono **immutabili una volta vivi**: un cambio di prezzo si fa creando un prezzo nuovo, non modificando
  quello esistente.

---

## 6. Proposta di classificazione dei dati personali

> ⚠️ **Proposta da confermare — fermata di escalation dello sviluppatore.** Il manifesto dei dati si compila
> **insieme** allo sviluppatore: «niente contratto, niente produzione». Un manifesto inventato è **peggio** di uno
> assente, perché sembra conformità ed è finzione.

> 🛑 **Attenzione — categorie particolari (articolo 9). Questa applicazione è ammessa nel catalogo perché il beauty
> non è sanitario, ma alcuni suoi trattamenti sfiorano quel confine e la scheda tecnica del cliente lo attraversa
> di slancio se non la si progetta apposta. Va deciso prima di scrivere una riga di codice.**
>
> **Dove sta il confine, e perché stiamo dalla parte giusta.** La legge 4 gennaio 1990 n. 1, articolo 1 comma 3,
> esclude dall'attività di estetista «le prestazioni dirette in linea specifica ed esclusiva a finalità di
> carattere terapeutico» (§2.3, punto 1). Il cliente tipo di SalonGrove è quindi, **per definizione di legge**, un
> soggetto che non cura nessuno: è un'ancora che l'app 07 BookGrove non ha — essendo orizzontale, BookGrove può
> essere comprata da un poliambulatorio, e il nome del servizio prenotato («visita dermatologica») diventa da solo
> un dato sanitario. SalonGrove non ha quel problema: «colore» e «manicure» non rivelano niente sulla salute di
> nessuno.
>
> **Dove il confine però lo attraversiamo.** Non dal lato del servizio: dal lato della **scheda del cliente**. La
> pratica del mestiere raccoglie, e i gestionali di mercato memorizzano, «allergie, patch test, patologie in
> corso, farmaci attivi, gravidanza/allattamento, fotosensibilizzanti» (§2.3 punto 2). Sono **dati relativi alla
> salute**, articolo 9, senza sfumature: «allergica alla parafenilendiammina», «in gravidanza», «in terapia con
> retinoidi» sono informazioni sanitarie **anche se le scrive una parrucchiera su un cartoncino**. E non esiste
> una codifica furba che le tolga di mezzo: se registro «non idonea alla colorazione con ossidazione», da lì si
> risale a una condizione della persona, e l'articolo 9 copre anche il dato da cui la salute si **desume**. Chi
> propone di «scriverlo in modo neutro» sta proponendo una foglia di fico, non una tutela.
>
> **Le tre vie, e quella che raccomando.**
> **(a) Nessun dato sulla salute nella prima versione.** La scheda tecnica tiene la **formula** (base, tono,
> ossidante, volume, minuti di posa, risultato) e i **prodotti usati**: sono fatti sul *trattamento*, non sulla
> persona. Nessun campo per allergie, patologie, farmaci, gravidanza, esito di test cutanei. Il campo nota libera
> porta un **avviso a schermo** che dice di non scriverci informazioni sulla salute del cliente, e le informazioni
> di sicurezza restano dove stanno oggi, cioè sulla scheda cartacea del salone.
> **(b) Un modulo «sicurezza del trattamento» attivabile per account**, che tiene il minimo indispensabile —
> famiglia di prodotto da non usare, data ed esito del test cutaneo — sotto **consenso esplicito** dell'interessato
> raccolto dal salone (articolo 9 paragrafo 2 lettera a), cifrato a livello di campo, escluso dagli strumenti
> conversazionali e dalla console di amministrazione, con durata breve e valutazione d'impatto.
> **(c) Lasciar fare al campo nota libera.** È **la via peggiore** e va nominata proprio perché è quella che
> succede da sola se non si sceglie: il dato particolare c'è lo stesso, senza nessuna tutela e — peggio — senza
> che nessuno sappia di averlo.
>
> **Raccomando (a) per la prima versione, (b) come evoluzione governata**, esattamente come BookGrove raccomanda
> per sé. Con due avvertenze che non voglio nascondere. La prima: **(a) rende la scheda tecnica meno utile della
> carta che sostituisce** su un punto di sicurezza vero, e questo spinge il salone verso (c) di fatto — è il
> motivo per cui l'avviso sulla nota libera non è un dettaglio grafico ma un requisito (storia `0012`). La
> seconda: **(b) è articolo 9 a tutti gli effetti** e comporta base giuridica rafforzata, valutazione d'impatto,
> revisione legale e una posizione esplicita nelle condizioni d'uso. Non è una funzione che si aggiunge in una
> mattinata, ed è giusto che sia così.
>
> **Come si tiene l'app fuori dal perimetro sanitario — in concreto, cinque presidi.**
> 1. Le **condizioni d'uso** dichiarano che SalonGrove è per attività estetiche non terapeutiche; medicina
>    estetica, dermatologia, fisioterapia e podologia sanitaria sono fuori (è il perimetro di 23 CareGrove,
>    **esclusa** dal catalogo).
> 2. Il modello dati **non ha i campi**: non li nasconde, non li scoraggia — non esistono. Un campo che non c'è è
>    l'unico che non si riempie.
> 3. Il campo nota libera porta l'**avviso a schermo**, in tutte e cinque le lingue.
> 4. L'app **non attesta** idoneità di apparecchi, formazioni obbligatorie o consensi informati: registra al più
>    che il salone ha un consenso e da quando, come fatto e senza contenuto.
> 5. La **fotografia** del trattamento non è mai sottoposta a riconoscimento: nessuna elaborazione tecnica che
>    permetta di identificare univocamente una persona, che è ciò che trasformerebbe una foto in un dato
>    biometrico.

**Categorie trattate** (proposta). Le voci del cliente e dell'appuntamento — nome, contatti, lingua, note,
prenotazioni — sono quelle di BookGrove (§6 di quell'app) e non si ripetono qui: sotto la via (b) sono
letteralmente le stesse righe.

| Voce | Dove vive | Di chi è | Che dato è | A cosa serve | Perché è lecito | Per quanto si tiene |
|---|---|---|---|---|---|---|
| `scheda_tecnica.formula` | `scheda_tecnica.base`, `.tono`, `.ossidante`, `.volume`, `.minuti_posa` | il cliente del salone | tecnico, riferito al trattamento | rifare lo stesso servizio con lo stesso esito | esecuzione del contratto fra il salone e il suo cliente (appgrove è responsabile del trattamento) | 36 mesi dall'ultimo servizio (proposta: la formula invecchia, ma un cliente che torna dopo due anni la vuole) |
| `scheda_tecnica.risultato` | `scheda_tecnica.risultato`, `.note_tecniche` | cliente | **testo libero — vedi sotto** | annotazioni dell'operatore sul risultato | esecuzione del contratto | come sopra |
| `foto_trattamento.immagine` | `foto_trattamento.immagine` | cliente | **immagine di una persona: dato particolarmente identificante** | mostrare il prima e il dopo, ritrovare un risultato | **consenso** del cliente, revocabile, registrato con data | 24 mesi, oppure fino alla revoca (proposta) |
| `conto.cliente` | `conto.cliente_id` | cliente | economico | il conto del servizio | esecuzione del contratto | 24 mesi (proposta; il documento fiscale, che ha durate proprie, non lo emette questa app) |
| `pacchetto.cliente` | `pacchetto.cliente_id`, `.sedute_residue` | cliente | economico | sapere quante sedute restano e a chi | esecuzione del contratto | fino a esaurimento o scadenza, poi 24 mesi |
| `fedelta.saldo` | `tessera_fedelta.saldo`, `movimento_punti.*` | cliente | economico | maturare e spendere i punti | esecuzione del contratto | finché la tessera è attiva, poi 12 mesi |
| `riga_conto.operatore` | `riga_conto.operatore_id` | **chi lavora nel salone** | dato del rapporto di lavoro | attribuire il venduto e calcolare la provvigione | esecuzione del contratto di lavoro o di collaborazione, per conto del salone | 24 mesi (proposta) |
| `regola_provvigione` | `regola_provvigione.percentuale`, `.scaglioni` | chi lavora nel salone | **condizione economica del rapporto di lavoro** | calcolare quanto spetta | esecuzione del contratto di lavoro | finché la regola è valida, più 24 mesi |
| `prospetto_provvigioni` | `prospetto_provvigioni.*` | chi lavora nel salone | economico | il conteggio chiuso del periodo | esecuzione del contratto di lavoro | 24 mesi (proposta; il documento di paga, con le sue durate, non lo produce questa app) |
| `movimento_magazzino.operatore` | `movimento_magazzino.created_by` | chi lavora nel salone | dato di controllo | sapere chi ha rettificato una giacenza | legittimo interesse del salone alla correttezza del magazzino | 24 mesi |

⚠️ **Un avviso sul lavoro, non solo sulla salute.** Le ultime quattro righe riguardano **le persone che lavorano
nel salone**, non i clienti, e vanno lette con attenzione: il prospetto delle provvigioni è un trattamento
ordinario e lecito (serve a pagare il dovuto), ma **una classifica degli operatori per rendimento è un'altra
cosa**. È il confine che ha portato all'esclusione dell'app 11 ShiftGrove dal catalogo, per via della disciplina
sul controllo a distanza dell'attività lavorativa. La proposta è netta: **prospetto sì, classifica no** — gli
indicatori dell'epica 06 si leggono per **salone** e per **servizio**, e per operatore solo dove servono a
calcolare quanto gli spetta. Va confermato (§11, punto 6).

**Esportazione e cancellazione.** Devono comparire **sia** in `exportData` **sia** in `purgeData` del contratto
dati: `scheda_tecnica`, `foto_trattamento`, `conto`, `riga_conto`, `pacchetto`, `utilizzo_pacchetto`,
`tessera_fedelta`, `movimento_punti`, `regola_provvigione`, `prospetto_provvigioni`, `movimento_magazzino` (per la
sola colonna di chi l'ha causato), più le due tabelle che nascono nelle epiche 06 e 07 — `richiamo_esito` (storia
`0027`) e `bozza_operazione` (storia `0029`). L'elenco si chiude e si mette sotto controllo automatico nella storia
`0032`. La candidata a essere dimenticata è **`foto_trattamento`**, perché l'immagine
non sta in una colonna di testo e chi scrive l'esportazione pensa alle righe, non ai file. La seconda candidata è
`movimento_magazzino`, che sembra un registro tecnico e invece dice chi ha fatto cosa.

**Un caso specifico e scomodo — cancellare un cliente che ha un pacchetto aperto.** La cancellazione è **fisica**,
ma un pacchetto pagato e non consumato è un debito del salone verso una persona. La proposta è: la cancellazione
dei dati del cliente **chiude** il pacchetto lasciando un movimento senza intestatario con l'importo residuo, così
che il salone sappia di dover ancora qualcosa a qualcuno anche se non sa più a chi; le sedute future collegate si
annullano. È una proposta, e va validata — tocca un diritto dell'interessato e un rapporto economico insieme.

**Testo libero.** Ci sono **tre** campi nota in questo verticale: la nota interna sul cliente (di BookGrove), le
note tecniche della scheda e la nota di riga sul conto. Sono tutti e tre un ingresso non presidiato per categorie
particolari — «allergica al nichel», «viene dopo la chemioterapia», «non vuole l'operatore uomo». L'app non fa
rilevazione di contenuto e il presidio, se servirà, è un tema trasversale; **il campo delle note tecniche, però,
porta l'avviso a schermo obbligatorio** (storia `0012`), perché è quello dove il dato sanitario finirebbe per
inerzia professionale.

**Integrazioni esterne.** In questa stesura SalonGrove **non aggiunge fornitori esterni** rispetto a quelli già
elencati da BookGrove (posta transazionale, messaggistica, calendari). Le fotografie restano dentro
l'infrastruttura di appgrove, **a riposo in regione europea**, e non passano da nessun servizio di elaborazione
immagini di terzi. Se un giorno si aggiungesse il riordino automatico presso il fornitore di prodotti (§2.4 punto
4), quello sì sarebbe un fornitore nuovo, ma riceverebbe dati del salone e non dei suoi clienti.

**Classificazione della change.** Un verticale nuovo introduce finalità nuove (calcolo delle provvigioni,
misurazione dei consumi, pacchetti prepagati), una categoria di interessati che nella piattaforma è ancora poco
battuta (**chi lavora presso il cliente**) e una categoria di dati nuova (**le immagini delle persone**): è un
cambiamento **sostanziale**, senza letture alternative. Sotto la via (b) resta sostanziale: è la stessa app, ma
tratta cose che prima non trattava.

---

## 7. Strumenti per il livello conversazionale

> Requisito trasversale del catalogo: ogni funzione dev'essere comandabile da una chat. Il livello conversazionale
> **non esiste ancora** nel repository (epica `12-ready-for-ai-mcp`, UC 0061-0066, scritta e non implementata):
> qui si dichiara il **contratto**, che vive dentro il servizio dell'app.
> Regola di sicurezza: **lettura libera, scrittura con bozza e conferma umana** per gli effetti irreversibili.

| Strumento | Firma | Effetto | Natura | Conferma umana |
|---|---|---|---|---|
| `storico_servizi` | `(cliente, periodo?) → elenco dei servizi eseguiti, minimizzato` | che cosa abbiamo fatto e quando | lettura | no |
| `scheda_tecnica_cliente` | `(cliente, ultime_n?) → formule e prodotti usati` | la memoria tecnica: base, tono, ossidante, minuti di posa. **Mai le fotografie**, mai il campo nota libera | lettura | no |
| `giacenza_prodotti` | `(deposito?, sotto_soglia?) → elenco dei prodotti con quantità` | che cosa sta finendo in cabina | lettura | no |
| `stato_pacchetti` | `(cliente?) → pacchetti aperti con sedute residue e scadenza` | quante sedute restano e fino a quando | lettura | no |
| `provvigioni_periodo` | `(periodo, operatore?) → base maturata e importo calcolato` | il conteggio prima della chiusura | lettura | no |
| `clienti_da_richiamare` | `(giorni_di_assenza, servizio?) → elenco minimizzato` | chi non torna da troppo tempo | lettura | no |
| `apri_conto` | `(prenotazione) → bozza di conto` | mette insieme le righe del servizio eseguito | scrittura | **sì** |
| `aggiungi_riga_conto` | `(conto, servizio o prodotto, operatore) → bozza aggiornata` | attribuisce venduto a una persona | scrittura | **sì** |
| `chiudi_conto` | `(conto, modo d'incasso) → esito` | **fa scattare tre effetti insieme**: scarico del magazzino, decurtazione del pacchetto, maturazione di provvigioni e punti. Non si annulla: si rettifica | scrittura irreversibile | **sì, obbligatoria** |
| `rettifica_giacenza` | `(prodotto, deposito, quantità, motivo) → bozza di movimento` | corregge il magazzino a mano | scrittura | **sì** |
| `chiudi_prospetto_provvigioni` | `(periodo) → bozza del prospetto` | fissa quanto è dovuto alle persone che lavorano nel salone | scrittura irreversibile | **sì, obbligatoria** |

**Come si mappano le azioni chieste dalla scheda di catalogo.** La scheda 21 elenca quattro azioni:
`book_appointment` e `check_operator_availability` **sono già di BookGrove** (`crea_prenotazione` e
`verifica_disponibilita`, §7 di quell'app) e non si riscrivono; `get_client_history` diventa qui **due** strumenti
distinti, perché sono due domande diverse con due livelli di riservatezza diversi — `storico_servizi` (che cosa ha
fatto) e `scheda_tecnica_cliente` (come l'abbiamo fatto); `send_winback` **non diventa uno strumento di scrittura**,
diventa `clienti_da_richiamare`, di sola lettura, e l'invio resta all'app che possiede il canale. È una scelta
deliberata: mandare per sbaglio un messaggio a duecento clienti non si annulla, e non è il genere di cosa che si
concede a un assistente perché ha capito bene la frase.

**Lettura.** `scheda_tecnica_cliente` è la ragione per cui il livello conversazionale rende questa app più utile
delle sue concorrenti: «che formula avevamo fatto ad Anna a giugno?» è una domanda che oggi costa due minuti e le
mani asciutte, ed è di sola lettura. Tutto ciò che **muove denaro, magazzino o quanto spetta a una persona** passa
invece da una bozza e da un «sì» umano.

---

## 8. Indice delle epiche e delle storie

> La numerazione è progressiva a livello di applicazione e non si azzera a ogni epica. Le epiche **02-07** valgono
> identiche sotto entrambe le vie del §0; solo l'epica **01** cambia di natura.

### Epica 01 — Fondamenta

Alla fine di questa epica il verticale esiste, è acceso, vuoto e utilizzabile: le tabelle del salone ci sono, le
sezioni compaiono nella barra laterale, il piano che le accende funziona e in locale c'è un salone finto su cui
provare tutto.

| # | Storia | In una riga |
|---|---|---|
| [0001](01-fondamenta/0001-impianto-del-verticale.md) | Impianto del verticale | La decisione del §0 messa in pratica: app nuova oppure estensione di `prenotazioni` |
| [0002](01-fondamenta/0002-modello-dati-del-salone.md) | Modello dati del salone | Le prime tabelle verticali, `tenant_id`, colonne di controllo, movimenti immutabili |
| [0003](01-fondamenta/0003-guscio-del-modulo-frontend.md) | Guscio del modulo frontend | Sezioni del salone, cinque lingue, tema chiaro e scuro |
| [0004](01-fondamenta/0004-abbonamento-piano-e-quota.md) | Abbonamento, piano e quota | Catena dei varchi e tetto a giacenza su `postazioni` |
| [0005](01-fondamenta/0005-avvio-locale-e-dati-di-prova.md) | Avvio locale e dati di prova | Un salone finto con tre poltrone, servizi a fasi, prodotti e pacchetti |

### Epica 02 — Servizi del salone e agenda a fasi

È la differenza tecnica fra un'agenda e un'agenda per saloni: un servizio non è un blocco, è una sequenza di fasi
che impegnano risorse diverse in momenti diversi.

| # | Storia | In una riga |
|---|---|---|
| [0006](02-servizi-del-salone-e-agenda-a-fasi/0006-servizi-a-fasi-con-tempi-di-posa.md) | Servizi a fasi con tempi di posa | Applicazione, posa, finitura: durate e chi impegna ciascuna |
| [0007](02-servizi-del-salone-e-agenda-a-fasi/0007-occupazione-a-segmenti.md) | Occupazione a segmenti | Il motore che libera l'operatore durante la posa e tiene occupata la postazione |
| [0008](02-servizi-del-salone-e-agenda-a-fasi/0008-varianti-di-durata-e-prezzo.md) | Varianti di durata e prezzo | «Il colore di Anna dura venti minuti in più»: supplementi per cliente |
| [0009](02-servizi-del-salone-e-agenda-a-fasi/0009-servizi-in-sequenza-e-piu-operatori.md) | Servizi in sequenza e più operatori | Taglio con Marco, colore con Sara, shampoo con l'assistente: un solo appuntamento |

### Epica 03 — Scheda tecnica e storia del cliente

La memoria professionale del salone, ed è l'epica in cui si tiene la promessa del §6: essere utili senza tenere
dati sulla salute.

| # | Storia | In una riga |
|---|---|---|
| [0010](03-scheda-tecnica-e-storia-del-cliente/0010-scheda-tecnica-del-servizio.md) | Scheda tecnica del servizio | Base, tono, ossidante, minuti di posa, prodotti usati, risultato |
| [0011](03-scheda-tecnica-e-storia-del-cliente/0011-ripetizione-della-formula.md) | Ripetizione della formula | «Come l'ultima volta»: riportare una scheda passata sul servizio di oggi |
| [0012](03-scheda-tecnica-e-storia-del-cliente/0012-perimetro-senza-dati-sanitari.md) | Perimetro senza dati sanitari | I campi che non ci sono, l'avviso a schermo e il presidio della nota libera |
| [0013](03-scheda-tecnica-e-storia-del-cliente/0013-fotografie-del-trattamento.md) | Fotografie del trattamento | Prima e dopo, con consenso registrato, revoca e nessun riconoscimento |
| [0014](03-scheda-tecnica-e-storia-del-cliente/0014-manifesto-dati-e-diritti-dell-interessato.md) | Manifesto dati e diritti dell'interessato | Manifesto in due lingue, esportazione e cancellazione fisica del verticale |

### Epica 04 — Prodotti, consumi e magazzino

Dove il fatturato diventa margine: il prodotto di cabina si scarica quando il servizio si chiude, e da lì si sa
quanto costa davvero un colore.

| # | Storia | In una riga |
|---|---|---|
| [0015](04-prodotti-consumi-e-magazzino/0015-anagrafica-prodotti-e-depositi.md) | Anagrafica prodotti e depositi | Cabina e rivendita, costo d'acquisto e prezzo di vendita |
| [0016](04-prodotti-consumi-e-magazzino/0016-carichi-e-giacenze.md) | Carichi e giacenze | Movimenti immutabili, giacenza come somma, rettifiche con motivo |
| [0017](04-prodotti-consumi-e-magazzino/0017-consumo-di-cabina-per-servizio.md) | Consumo di cabina per servizio | Dosi previste, scarico alla chiusura del conto, scostamento fra previsto e usato |
| [0018](04-prodotti-consumi-e-magazzino/0018-soglie-e-lista-di-riordino.md) | Soglie e lista di riordino | Che cosa sta finendo e quanto ordinarne: una lista, non un ordine trasmesso |

### Epica 05 — Conto, pacchetti e fedeltà

Il momento in cui l'appuntamento diventa denaro — senza emettere documenti fiscali e senza muovere un euro.

| # | Storia | In una riga |
|---|---|---|
| [0019](05-conto-pacchetti-e-fedelta/0019-chiusura-del-conto.md) | Chiusura del conto | Righe, sconti, modo d'incasso dichiarato, e i tre effetti che scattano insieme |
| [0020](05-conto-pacchetti-e-fedelta/0020-pacchetti-di-sedute-prepagate.md) | Pacchetti di sedute prepagate | Sedute determinate o credito a valore: due specie diverse, con conseguenze diverse |
| [0021](05-conto-pacchetti-e-fedelta/0021-vendita-di-prodotti-al-banco.md) | Vendita di prodotti al banco | La rivendita sul conto, con lo scarico dal deposito giusto |
| [0022](05-conto-pacchetti-e-fedelta/0022-punti-e-premi-di-fedelta.md) | Punti e premi di fedeltà | Come si maturano, come si spendono, quando scadono |

### Epica 06 — Operatori, provvigioni e rendimento

Che cosa ha prodotto ciascuno e quanto gli spetta — fermandosi con precisione prima della busta paga e prima della
classifica.

| # | Storia | In una riga |
|---|---|---|
| [0023](06-operatori-provvigioni-e-rendimento/0023-attribuzione-del-venduto.md) | Attribuzione del venduto | Ogni riga di conto ha un operatore, anche quando il servizio è a più mani |
| [0024](06-operatori-provvigioni-e-rendimento/0024-regole-di-provvigione.md) | Regole di provvigione | Percentuali su servizi e rivendita, scaglioni, validità nel tempo |
| [0025](06-operatori-provvigioni-e-rendimento/0025-prospetto-delle-provvigioni.md) | Prospetto delle provvigioni | Il conteggio del periodo, la chiusura irreversibile e la rettifica |
| [0026](06-operatori-provvigioni-e-rendimento/0026-indicatori-del-salone.md) | Indicatori del salone | Riempimento, scontrino medio, margine sui servizi — per salone, non per persona |
| [0027](06-operatori-provvigioni-e-rendimento/0027-clienti-da-richiamare.md) | Clienti da richiamare | Chi non torna da troppo tempo: un elenco, non una campagna |

### Epica 07 — Esposizione conversazionale e prove end-to-end

Il contratto degli strumenti, i due percorsi che dimostrano che il verticale funziona davvero, e la chiusura del
contratto dei dati — che solo qui può essere completa, perché solo qui tutte le tabelle esistono.

| # | Storia | In una riga |
|---|---|---|
| [0028](07-esposizione-conversazionale-e-prove/0028-strumenti-di-lettura.md) | Strumenti di lettura | Storico, scheda tecnica, giacenze, pacchetti, provvigioni — minimizzati |
| [0029](07-esposizione-conversazionale-e-prove/0029-strumenti-di-scrittura-con-conferma.md) | Strumenti di scrittura con conferma | Bozza e «sì» umano per tutto ciò che muove denaro, magazzino o compensi |
| [0030](07-esposizione-conversazionale-e-prove/0030-percorso-end-to-end-del-salone.md) | Percorso end-to-end del salone | `[J-SALONGROVE]`: dal servizio a fasi al conto chiuso |
| [0031](07-esposizione-conversazionale-e-prove/0031-percorso-end-to-end-del-pacchetto.md) | Percorso end-to-end del pacchetto | `[J-SALONGROVE-PKG]`: pacchetto venduto, seduta scalata, provvigione maturata |
| [0032](07-esposizione-conversazionale-e-prove/0032-esportazione-e-cancellazione-complete.md) | Esportazione e cancellazione complete | Il contratto dati chiuso su tutte le epiche, con il controllo che impedisce di dimenticare una tabella |

**Totale**: 7 epiche, 32 storie (`0001`-`0032`).

---

## 9. Estensioni della console di amministrazione

Servono tre cose oltre allo standard: una **deroga temporanea** sul tetto delle postazioni per il salone che sta
migrando dal vecchio programma e per qualche giorno ne tiene aperte più del piano; una vista sulla **coerenza fra
giacenza e movimenti** (è l'unica classe di segnalazione che non si diagnostica dalla scheda dell'account, e si
guarda per conteggi, mai per contenuto); e un **interruttore per account** che governa la disponibilità delle
fotografie del trattamento, perché è la funzione con il profilo di rischio più alto e deve poter essere spenta
senza toccare il codice. Nessuna delle tre dà accesso ai dati del cliente.

Dettaglio: [estensioni-admin.md](estensioni-admin.md).

---

## 10. Dipendenze e sinergie con altre app del catalogo

| App del catalogo | Rapporto | Cosa condividono |
|---|---|---|
| **07 — BookGrove (prenotazioni e agenda)** | **è il rapporto costitutivo, ed è il §0** | Tutta l'agenda: servizi, risorse, disponibilità, clienti, pagina pubblica, promemoria, lista d'attesa, calendari esterni. O SalonGrove **è** BookGrove con un piano in più (via b), o **contiene una seconda copia** del suo motore (via a). Non esiste una terza possibilità finché la piattaforma non offre codice condiviso sopra `services/commons` |
| **14 — StockGrove (magazzino)** | **si sovrappone, e va coordinata** | L'epica 04 è un magazzino: prodotti, depositi, movimenti, soglie. Quello di SalonGrove è deliberatamente **piccolo e legato al servizio** (due depositi, dosi per trattamento, scarico alla chiusura del conto), quello di StockGrove è generale. Il confine proposto: se un salone ha bisogno di lotti, scadenze, inventari e più magazzini, quello è StockGrove; se gli basta sapere quanta tinta resta, è SalonGrove. **Costruire due volte lo stesso movimento di magazzino è lo spreco più prevedibile dopo quello del §0** |
| **03 — CashGrove (incassi)** e **02 — BillGrove (fatturazione)** | alimenta | Il conto chiuso emette un evento che a valle diventa ricevuta, fattura o riga di incasso. SalonGrove non emette documenti fiscali e non incassa |
| **29 — ShopGrove (punto cassa)** | **confine da non attraversare** | È **esclusa** dal catalogo perché i corrispettivi telematici passano da un apparecchio omologato. La «cassa» di SalonGrove è la chiusura di un conto interno: se un giorno diventasse emissione di scontrini, sarebbe la stessa esclusione |
| **10 — PayGrove (paghe)** e **11 — ShiftGrove (turni e presenze)** | **confine da non attraversare** | Entrambe **escluse**. SalonGrove calcola quanto ha prodotto un operatore e quanto gli spetta secondo la regola concordata; il cedolino e la rilevazione della prestazione lavorativa restano fuori (§6, avviso sul lavoro) |
| **05 — ChatGrove** e **16 — ReachGrove** | delega | I richiami ai clienti che non tornano: SalonGrove prepara l'elenco, l'invio è di chi possiede il canale. ChatGrove è **esclusa**, quindi in prima battuta il canale è quello che BookGrove già usa per i promemoria |
| **04 — LeadGrove (CRM)** | condivide dati con | L'anagrafica clienti condivisa, stesso punto aperto di BookGrove: le app non si chiamano fra loro e non esiste ancora un meccanismo di riconciliazione |
| **22 DineGrove, 23 CareGrove, 27 FitGrove, 58 VetGrove** | **sorelle nello stesso problema** | Sono gli altri verticali che poggiano su BookGrove. **La decisione del §0 vale per tutti e quattro**, e SalonGrove è il primo caso: se qui si sceglie la via (a), si sceglie di riscrivere il motore cinque volte. Tre di loro (23, 27, 58) sono **escluse** per l'articolo 9, quindi i verticali realmente in gioco sono SalonGrove e DineGrove — ma DineGrove è a sua volta esclusa, il che lascia SalonGrove **da sola a giustificare il costo di un motore duplicato** |

**Lettura.** Questa riga finale è il fatto economico più importante del documento e merita di essere detto senza
giri: delle cinque applicazioni per cui BookGrove doveva essere «la base riutilizzabile», **quattro sono escluse
dal catalogo attivo** (23 CareGrove, 27 FitGrove, 58 VetGrove, 22 DineGrove). Il riuso su cui la scelta
architetturale di BookGrove poggiava si è ridotto a **un solo verticale**: questo. Un motore condiviso che serve
cinque app è un investimento; un motore duplicato per servirne una è un costo. È l'argomento che, da solo, fa
pendere la bilancia verso la via (b).

**Sovrapposizioni da evitare.** Tre: il motore di prenotazione (con 07, ed è il §0), il magazzino (con 14) e
l'anagrafica clienti (con 04). In tutti e tre i casi il rischio non è tecnico ma di prodotto: costruire due volte
la stessa cosa e poi doverle tenere d'accordo.

---

## 11. Rischi e punti aperti

| # | Punto | Perché è aperto | Chi lo chiude |
|---|---|---|---|
| 1 | **App autonoma o verticale di BookGrove** (§0) | è la decisione con le conseguenze economiche più grandi di questa cartella: 25 storie di differenza e una manutenzione doppia per sempre. La raccomandazione è (b), ma la via (a) ha un argomento commerciale reale (marchio e canale d'acquisto) | **sviluppatore, prima di ogni altra cosa** |
| 2 | **Un piano può avere la propria pagina di presentazione?** (§0.3) | è la condizione che rende la via (b) indolore: senza, «SalonGrove» non esiste come nome sul mercato e il vantaggio commerciale della via (a) torna a pesare | sviluppatore, con l'epica delle pagine di presentazione (UC 0057) |
| 3 | **Prezzi, limiti dei piani, e se la metrica debba essere per postazione o per salone** (§5) | fermata di escalation della piattaforma; in più il concorrente italiano con listino pubblico vende **operatori illimitati** a un prezzo più basso, e la scheda di catalogo propone una percentuale sui pagamenti che la piattaforma non ammette | sviluppatore, prima dello scaffolding |
| 4 | **Articolo 9: fino a dove arriva la scheda tecnica** (§6) | tre vie (nessun dato sanitario, modulo attivabile con consenso esplicito, nota libera non presidiata); la raccomandazione è (a) ora e (b) come evoluzione, ma comporta base giuridica rafforzata, valutazione d'impatto e revisione legale | sviluppatore con supporto legale, **prima della storia `0010`** |
| 5 | **L'ancora giuridica è italiana** (§2.7) | l'articolo 1 comma 3 della legge 1/1990 è ciò che tiene SalonGrove fuori dal perimetro sanitario. Non ho verificato che Francia, Spagna e Germania traccino il confine allo stesso modo: se non lo fanno, il perimetro va difeso con le condizioni d'uso invece che con la legge | sviluppatore, prima della vendita fuori dall'Italia |
| 6 | **Provvigioni sì, classifiche no** (§6, avviso sul lavoro) | il prospetto è amministrazione ordinaria; una vista di rendimento per persona sfiora la disciplina che ha fatto escludere l'app 11. Propongo indicatori per salone e per servizio, e per operatore solo dove servono al calcolo | sviluppatore, prima della storia `0026` |
| 7 | **Il confine con il magazzino di StockGrove** (§10) | due app della stessa suite con lo stesso movimento di magazzino sono due volte lo stesso lavoro; ma un magazzino generale è troppo per un salone che vuole solo sapere quanta tinta resta | sviluppatore, in sede di sequenza di costruzione |
| 8 | **Il colore-categoria `red` coincide con il colore d'errore** (§3) | è l'ultimo libero, ma nel sistema di design `--cat-red` e `--danger` hanno lo stesso valore. Con sei colori e sessanta app la collisione è strutturale | epica di piattaforma, non ancora scritta |
| 9 | **Chi paga i messaggi** (§5) | ereditato da BookGrove (§11 punto 5 di quell'app): il messaggio si paga a consumo, la piattaforma vieta l'addebito a consumo e la metrica non limita i messaggi. Non lo riapro qui, ma vale anche qui | sviluppatore, insieme a BookGrove |
| 10 | **Il livello conversazionale non esiste** (§7) | epica `12-ready-for-ai-mcp` (UC 0061-0066), scritta e non implementata: le storie `0028` e `0029` dichiarano il contratto e si fermano lì | epica di piattaforma |

**Rischi noti**

- **Il verticale che non aggiunge abbastanza.** Se lo sviluppatore sceglie la via (a) e poi, per stringere i tempi,
  ne implementa solo l'agenda, il risultato è una copia peggiore di BookGrove venduta più cara. Attenuazione: le
  epiche 02, 04 e 06 — fasi, cabina, provvigioni — sono il verticale; senza almeno due delle tre, SalonGrove non
  esiste come prodotto.
- **La configurazione che nessuno completa.** Fasi, dosi previste, percentuali e regole di provvigione sono quattro
  configurazioni, e un salone che deve farle tutte prima di vedere qualcosa torna al quaderno (§2.5). Attenuazione:
  **ogni funzione verticale deve funzionare a vuoto** — un servizio senza fasi è un blocco unico come in BookGrove,
  un servizio senza dosi previste non scarica niente, un operatore senza regola di provvigione non matura nulla e
  non rompe niente.
- **Il dato sanitario che entra dalla nota libera.** È il rischio più concreto del §6 e non si chiude con un
  divieto: il salone ha bisogno di sapere che quella cliente non tollera quel prodotto, e se il campo apposito non
  c'è lo scrive dove può. Attenuazione: avviso a schermo (storia `0012`), e soprattutto **decidere il punto 4
  prima del rilascio**, non dopo.
- **Il conto chiuso che era sbagliato.** La chiusura fa scattare tre effetti insieme e non si annulla. Se la
  rettifica è scomoda, gli operatori impareranno a non chiudere i conti, e l'app perde il suo dato migliore.
  Attenuazione: la rettifica dev'essere un'operazione ordinaria e a portata di mano, non una procedura di
  emergenza (storia `0019`).
- **La classifica che arriva per gentilezza.** Il primo titolare che chiede «fammi vedere chi vende di più»
  sembrerà una richiesta innocua. Attenuazione: la risposta è scritta nel punto 6, e va scritta prima che la
  domanda arrivi.

**Fuori dimensionamento**: nessuno. 7 epiche (fascia raccomandata 4-7), da 4 a 5 storie per epica (fascia 4-8),
32 storie in tutto (fascia 20-45). Il conteggio **non comprende** le circa 25 storie di BookGrove che sotto la via
(a) andrebbero riscritte: se lo sviluppatore sceglie quella via, il lavoro reale di SalonGrove è di 32 storie più
un motore di prenotazione, ed è esattamente il numero che rende la decisione del §0 quello che è.
