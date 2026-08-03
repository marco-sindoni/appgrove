# 0032 — Salute della lista

**Applicazione**: 16 — ReachGrove (`campaigns`) · **Epica**: 06 — Rendimento e salute della lista
**Storia**: `0032` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0008`, `0010`, `0011`, `0021`, `0030`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che non sa di essere a un passo dal blocco
> voglio una schermata che mi dica in che stato è la mia lista e quanto sono vicino alle soglie che i grandi
> fornitori di posta impongono
> così da accorgermene prima che i miei messaggi comincino a essere respinti, invece che dopo.

**Contesto.** I grandi fornitori di posta pretendono un tasso di segnalazione di posta indesiderata **sotto lo
0,3 %**, con lo 0,1 % come obiettivo, e chi non rispetta viene respinto al livello del protocollo, non spostato
nella cartella della posta indesiderata ([application-description.md](../application-description.md) §2.3 punto 5).
È un numero che il cliente non guarderà mai spontaneamente e che gli costa l'account quando lo supera; per di più
la reputazione di invio è **condivisa fra tutti gli account** della piattaforma, quindi il suo problema diventa
quello di tutti. Questa storia è il cruscotto di rischio: raccoglie ciò che le storie precedenti hanno prodotto e
lo mette in una schermata sola, con le azioni di igiene accanto ai numeri che le motivano.

## 2. Requisiti funzionali

1. **RF-1** — La schermata mostra la composizione della lista: iscritti attivi e inviabili, in attesa di conferma,
   in quarantena, disiscritti, recapiti soppressi — ognuno con il numero e con il collegamento all'elenco
   corrispondente.
2. **RF-2** — Mostra il **tasso di segnalazione** su una finestra mobile di 30 giorni, confrontato con la soglia
   dello 0,3 % e con l'obiettivo dello 0,1 %, dicendo in una frase cosa succede al superamento: blocco automatico
   degli invii dell'account (storia 0021).
3. **RF-3** — Mostra il **tasso di rimbalzo permanente** sulla stessa finestra, perché è il segnale che la lista
   contiene indirizzi vecchi o non verificati.
4. **RF-4** — Mostra gli **inattivi** con una definizione dichiarata in chiaro. Se la misurazione del
   comportamento è spenta (storia 0029), l'app **dice che non può sapere chi è inattivo** e usa il solo segnale di
   cui dispone — nessun evento in ingresso dall'iscritto, cioè nessuna nuova conferma e nessuna nuova iscrizione
   da un modulo, da un tempo dichiarato — segnalando che è un'approssimazione. Non inventa un dato che non ha.
5. **RF-5** — Accanto a ogni numero problematico propone **una** azione di igiene concreta: escludere gli inattivi
   dai segmenti, rimuovere gli iscritti mai confermati oltre un certo tempo, aprire l'elenco delle righe in
   quarantena per allegarne la prova. Ogni azione dice **quanti iscritti tocca** e si esegue solo dopo una
   conferma esplicita.
6. **RF-6** — Nessuna azione di igiene cancella una registrazione di consenso né svuota l'elenco di soppressione:
   la prima è prova, il secondo è un divieto. Le azioni tolgono iscritti dai segmenti o li portano fuori dallo
   stato inviabile, mai il contrario.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Tutti i conteggi e tutte le azioni di igiene filtrano per `tenant_id`
  preso dal token verificato. I tassi sono **per account**: la reputazione condivisa si sorveglia dalla console di
  amministrazione ([estensioni-admin.md](../estensioni-admin.md)), non da qui.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET /api/campaigns/v1/list-health` e
  `POST /api/campaigns/v1/list-health/actions/{action}`; corpo validato; errori in `application/problem+json`;
  definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova di dominio: i numeri si calcolano da `subscriber`,
  `consent_record`, `suppression` e `delivery_event`. Si aggiungono gli indici che rendono sostenibile il calcolo
  a partire da `tenant_id` e una tabella `list_hygiene_action` sullo schema `app_campaigns` — `tenant_id`, chiave
  primaria UUID versione 7, colonne di controllo, cancellazione logica — che conserva chi ha eseguito quale
  azione, quando e su quanti iscritti.
- **RT-4 — Modulo frontend (§3, §5).** Nuova sezione «Salute della lista» nel manifesto del modulo `campaigns`,
  con i tassi in evidenza, la composizione della lista e le azioni accanto ai numeri che le motivano; solo token
  del sistema di design; funziona in tema chiaro e scuro. Il superamento di una soglia si comunica con il colore
  funzionale di pericolo, che in questa app è riservato a questo: dire che qualcosa non si può fare o sta per
  rompersi.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe passano dallo spazio-nomi `campaigns` in `en, it, fr, es, de`,
  comprese le definizioni («che cosa vuol dire inattivo») e i testi di conferma delle azioni.
- **RT-6 — Varchi e quota (§6, §7).** Leggere il cruscotto non consuma la metrica `messages_sent` (natura `flow`).
  Le azioni di igiene richiedono ruolo `owner` o `admin`, altrimenti `403`. Con abbonamento in `past_due` la
  schermata resta accessibile: è quella che evita danni.
- **RT-7 — Esposizione conversazionale (§12).** Alimenta lo strumento di lettura `salute_della_lista`
  (storia 0034) con **lo stesso calcolo** della schermata. Le azioni di igiene **non** sono esposte alla chat:
  toccano lo stato di molti iscritti in un colpo solo e vanno viste prima di essere eseguite. Livello
  conversazionale non ancora implementato (UC 0061-0063).
- **RT-8 — Dati personali (§10).** Nessun campo nuovo che riguardi una persona: la schermata mostra conteggi. La
  tabella delle azioni di igiene registra l'identificativo di chi ha agito e va aggiunta a `exportData` e
  `purgeData`; la voce corrispondente nel manifesto `docs/compliance/manifests/campaigns.yaml` va scritta in
  italiano e inglese.
- **RT-9 — Registrazione eventi (§14).** «Soglia di segnalazione superata», «azione di igiene eseguita» con il
  numero di iscritti toccati, registrati con `tenant_id`, `app_id`, `user_id` e identificativo di correlazione;
  mai i recapiti.

## 4. Criteri di accettazione

**CA-1 — Il cruscotto dice dove si è**
- **Dato** un account con 4.000 iscritti, di cui 120 in quarantena e 40 mai confermati
- **Quando** apre «Salute della lista»
- **Allora** vede i conteggi per stato, il tasso di segnalazione e il tasso di rimbalzo permanente degli ultimi
  30 giorni, ciascuno accanto alla propria soglia

**CA-2 — Superamento della soglia**
- **Dato** un account il cui tasso di segnalazione degli ultimi 30 giorni è dello 0,4 %
- **Quando** apre la schermata
- **Allora** la soglia risulta superata in modo evidente, con scritto che gli invii sono bloccati e come si
  rimedia

**CA-3 — Onestà sugli inattivi**
- **Dato** un account che non ha mai misurato aperture né clic
- **Quando** guarda la voce «inattivi»
- **Allora** legge che il dato non è disponibile perché la misurazione è spenta, e vede l'approssimazione
  dichiarata come tale

**CA-4 — Un'azione di igiene dice quanti ne tocca**
- **Dato** 380 iscritti mai confermati da oltre sei mesi
- **Quando** si sceglie di rimuoverli
- **Allora** la conferma dice «380 iscritti», e l'azione avviene solo dopo il consenso esplicito

**CA-5 — L'igiene non tocca prove e divieti**
- **Dato** un'azione di igiene eseguita
- **Quando** si controllano registro dei consensi ed elenco di soppressione
- **Allora** sono invariati: nessuna registrazione cancellata, nessun recapito tolto dalla soppressione

**CA-6 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` legge il cruscotto
- **Allora** i numeri sono solo i propri, anche forzando l'identificativo dell'altro account nella richiesta

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (`backend`, `frontend`; l'intera suite prima del commit);
- [ ] prove di **unità** sul calcolo dei due tassi a finestra mobile e sulla definizione di inattivo con e senza
      misurazione, e di **integrazione** sulle rotte e sulle azioni di igiene;
- [ ] prova di **isolamento fra account** sui conteggi e sulle azioni;
- [ ] **prova end-to-end**: coprire ora — il percorso `[J-CAMPAIGNS]` (storia 0037) apre il cruscotto dopo la
      disiscrizione e verifica che i conteggi si siano mossi; voce aggiunta al registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, definizioni e conferme comprese;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per la tabella delle azioni di igiene;
- [ ] **registro delle decisioni** compilato, con annotato perché gli inattivi si dichiarano non calcolabili
      invece di essere stimati;
- [ ] contratto degli **strumenti conversazionali**: `salute_della_lista` alimentato dallo stesso calcolo, azioni
      di igiene **non** esposte con la motivazione scritta;
- [ ] controllo automatico di **accessibilità** verde sulla schermata, con il superamento di soglia comunicato
      anche a parole e non solo col colore;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storie `0008`, `0010` | Iscritti non confermati e righe in quarantena sono due delle voci del cruscotto |
| Storia `0011` | L'elenco di soppressione fornisce i conteggi e stabilisce cosa l'igiene non può toccare |
| Storia `0021` | Il tasso di segnalazione e il blocco automatico nascono lì |
| Storia `0030` | I numeri per campagna sono la base dei tassi a finestra mobile |

## 7. Fuori ambito

- la sorveglianza della reputazione **fra** gli account e la sospensione di un cliente: è della console di
  amministrazione ([estensioni-admin.md](../estensioni-admin.md));
- la verifica della validità di un indirizzo prima dell'invio tramite un servizio esterno: fuori, perché
  significherebbe mandare i recapiti a un fornitore in più;
- la campagna di ri-richiesta del consenso per gli iscritti in quarantena: non implementata, liceità non chiarita
  ([application-description.md](../application-description.md) §11.8).

## 8. Punti aperti

- **Da quanto tempo un iscritto mai confermato si può rimuovere** e **dopo quanti mesi senza eventi si è
  inattivi**: due valori di prodotto, proposti rispettivamente a sei e dodici mesi, che lo sviluppatore conferma.
- **Cosa mostrare a chi ha la misurazione spenta**, oltre all'approssimazione dichiarata: se il surrogato sia
  abbastanza utile da valere la schermata è una domanda che si risponde guardando i dati reali dopo qualche mese.
