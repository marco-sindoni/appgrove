# 0008 — Importazione dei crediti da file

**Applicazione**: 03 — CashGrove (`crediti`) · **Epica**: 02 — Portafoglio crediti
**Storia**: `0008` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che prova CashGrove per la prima volta
> voglio caricare in un colpo solo lo scadenzario che esporto dal mio gestionale
> così da vedere il valore dell'app in cinque minuti invece che dopo due ore di digitazione.

**Contesto.** Nessuna micro-impresa inserisce a mano ottanta fatture per provare un programma: o entra tutto subito, o
il prodotto viene abbandonato al primo tentativo. È la lamentela ricorrente rilevata sui prodotti della categoria
([documento capofila](../application-description.md) §2.5). Tutti i gestionali — Fatture in Cloud, TeamSystem, Aruba,
Xero, QuickBooks — sanno esportare lo scadenzario in un file tabellare: quello è il ponte, e resta utile anche il
giorno in cui esisterà un innesto automatico, perché l'innesto non copre mai tutti i gestionali del mondo.

## 2. Requisiti funzionali

1. **RF-1** — L'utente carica un file tabellare (valori separati da virgola o punto e virgola, e foglio di calcolo) e
   associa le colonne del file ai campi dell'app, con un riconoscimento automatico proposto e correggibile.
2. **RF-2** — Prima di scrivere qualsiasi cosa l'app mostra un'**anteprima**: quante righe verranno create, quante
   aggiornate, quante scartate e per quale motivo, riga per riga.
3. **RF-3** — I debitori non ancora presenti vengono creati durante l'importazione, riconosciuti per identificativo
   fiscale e, in mancanza, per denominazione esatta.
4. **RF-4** — Una riga già importata in precedenza (stesso debitore e stesso numero di documento) **aggiorna** il
   credito esistente invece di crearne un doppione.
5. **RF-5** — L'importazione è una operazione unica: o va a buon fine per tutte le righe valide, o non lascia nulla a
   metà; le righe scartate sono scaricabili in un file di riepilogo per essere corrette e ricaricate.
6. **RF-6** — Se l'importazione supererebbe il tetto della quota, l'app lo dice **nell'anteprima** — non a metà
   scrittura — indicando quante righe eccedono.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni riga creata o aggiornata porta il `tenant_id` preso dal token
  verificato; una colonna del file che pretendesse di indicare un account viene ignorata, non interpretata.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `POST /api/crediti/v1/importazioni` (caricamento e anteprima) e
  `POST /api/crediti/v1/importazioni/{id}/conferma`; errori in `application/problem+json`; il file non supera un limite
  dichiarato di dimensione e di numero di righe, altrimenti `413` con il limite scritto nel messaggio.
- **RT-3 — Persistenza (§8).** Migrazione per la tabella `importazione` (istante, nome del file, esito, conteggi,
  autore) sullo schema `app_crediti`, con `tenant_id`, chiave UUID versione 7, colonne di controllo e cancellazione
  logica. Il **contenuto** del file non si conserva oltre il tempo necessario alla conferma.
- **RT-4 — Modulo frontend (§3, §5).** Percorso guidato in tre passi (carica → associa le colonne → conferma) dentro la
  sezione *Crediti*; solo token del sistema di design; funziona in tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Tutti i messaggi di errore riga per riga passano dallo spazio-nomi `crediti` e sono
  presenti in `en, it, fr, es, de`: sono i testi più letti di questa storia e non possono essere scritti a mano.
- **RT-6 — Varchi e quota (§6, §7).** L'importazione prenota tante unità di `crediti_monitorati` quante sono le righe
  che creerebbero crediti nuovi; se il tetto non basta risponde `429` **in fase di anteprima**, con il numero di righe
  eccedenti e il rimedio.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento: caricare un file non è una operazione da chat, e
  esporla darebbe a un assistente il potere di creare centinaia di righe in un colpo. Scelta esplicita, annotata nel
  contratto.
- **RT-8 — Dati personali (§10).** Il file caricato contiene dati personali dei debitori: va trattato come tale — non
  conservato oltre il necessario, non scritto nei registri, non incluso nelle diagnostiche. La tabella `importazione`
  contiene solo metadati e conteggi ed è aggiunta a `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** Gli eventi «importazione avviata», «importazione confermata», «righe scartate»
  sono registrati con `tenant_id`, `app_id`, `user_id`, conteggi e identificativo di correlazione, **senza il contenuto
  delle righe**.

## 4. Criteri di accettazione

**CA-1 — Importazione felice**
- **Dato** un file con 40 righe valide e un account con quota sufficiente
- **Quando** l'utente conferma dopo l'anteprima
- **Allora** vengono creati 40 crediti e i debitori mancanti, e il riepilogo dice esattamente quanti di ciascuno

**CA-2 — Righe scartate**
- **Dato** un file con 40 righe di cui 3 senza data di scadenza
- **Quando** si arriva all'anteprima
- **Allora** le 3 righe sono elencate con il motivo dello scarto, le altre 37 sono confermabili, e il file di riepilogo
  degli scarti è scaricabile

**CA-3 — Nessun doppione**
- **Dato** un file già importato · **Quando** lo si importa di nuovo · **Allora** nessun credito viene duplicato: le
  righe risultano tutte «aggiornate» e il conteggio dei monitorati non cambia

**CA-4 — Quota insufficiente**
- **Dato** un account con 10 unità di quota libere e un file con 25 crediti nuovi
- **Quando** si arriva all'anteprima
- **Allora** l'app risponde `429` dicendo che 15 righe eccedono il tetto, e **nulla** viene scritto

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B` · **Quando** `A` importa un file che contiene l'identificativo fiscale di un debitore
  già presente in `B` · **Allora** in `A` viene creato un debitore nuovo e nulla di `B` viene letto o modificato

**CA-6 — Nessuno stato a metà**
- **Dato** un guasto simulato durante la scrittura · **Quando** l'importazione fallisce a metà · **Allora** il
  portafoglio crediti è identico a prima dell'operazione

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (backend, frontend, compliance);
- [ ] prove di **unità** sull'interpretazione delle righe e sul riconoscimento delle colonne, di **integrazione**
      sull'operazione unica con database effimero;
- [ ] prova di **isolamento fra account** sull'importazione;
- [ ] **prova end-to-end**: *coprire ora* — il caricamento del file è il primo passo realistico di ogni cliente e
      diventa il primo passo del percorso `[J-CREDITI]` quando la storia `0031` lo crea; qui si copre con una prova di
      integrazione completa e si registra la voce `da-coprire` nel registro di copertura, con proprietaria la
      storia `0031`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati** aggiornato con la tabella `importazione` in italiano e inglese, presente in esportazione e
      cancellazione;
- [ ] **registro delle decisioni** compilato, in particolare sulla non conservazione del file caricato;
- [ ] contratto degli **strumenti conversazionali**: esclusione deliberata, annotata con il motivo;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0007` | Serve la creazione del credito, di cui l'importazione è la versione di massa |

## 7. Fuori ambito

- L'innesto automatico sui gestionali (Fatture in Cloud, Xero, QuickBooks): fuori dalle 31 storie, è il punto aperto
  n. 9 del documento capofila §11.
- L'importazione degli incassi: rimandata alla storia `0009`, che introduce l'entità.
- L'importazione periodica programmata: non serve finché il caricamento è manuale.

## 8. Punti aperti

Il **limite di righe per file** è una scelta di dimensionamento che dipende dai piani: 5.000 righe è un valore
ragionevole ma va confermato dallo sviluppatore insieme ai tetti di piano.
