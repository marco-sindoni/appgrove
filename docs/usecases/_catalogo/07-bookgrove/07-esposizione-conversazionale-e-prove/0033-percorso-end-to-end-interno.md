# 0033 — Percorso end-to-end interno

**Applicazione**: 07 — BookGrove (`prenotazioni`) · **Epica**: 07 — Esposizione conversazionale e prove
**Storia**: `0033` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`, `0026`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come sviluppatore della piattaforma
> voglio una prova che percorra tutta l'applicazione dal lato dell'attività, sullo stack reale
> così da sapere che un cambiamento in un pezzo non ha rotto il percorso che il cliente usa ogni giorno.

**Contesto.** Il registro di copertura è sorvegliato da un controllo automatico: registro incoerente uguale suite
rossa. Ogni applicazione ha il proprio percorso in `tools/platform-e2e/journeys/`, e ogni test porta l'etichetta
del percorso in testa al titolo. Le storie precedenti hanno dichiarato via via «coperta ora» o «rimando»: questa
storia è il posto in cui quei rimandi vengono onorati per la parte interna.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il percorso `[J-BOOKGROVE]` in `tools/platform-e2e/journeys/J-BOOKGROVE.spec.ts`, eseguito
   sullo stack locale reale.
2. **RF-2** — Il percorso copre, in ordine: configurazione di un servizio e di una risorsa, orario settimanale,
   creazione di un appuntamento dall'agenda, spostamento, chiusura come eseguito, e lettura degli indicatori.
3. **RF-3** — Il percorso comprende almeno un caso negativo osservabile: il rifiuto per quota esaurita
   all'apertura di una risorsa in più.
4. **RF-4** — Ogni test porta l'etichetta `[J-BOOKGROVE]` in testa al titolo, e il registro
   `docs/testing/copertura-e2e.yaml` mappa le storie coperte.
5. **RF-5** — Il percorso non usa attese a tempo, accede in modo programmatico e usa dati inventati e
   deterministici.

## 3. Requisiti tecnici

- **RT-1 — Prove (§11).** Playwright senza finestra, sullo stack locale reale; niente attese a tempo; accesso
  programmatico; dati di prova deterministici e **inventati**, con indirizzi su dominio `*.test`.
- **RT-2 — Registro di copertura.** `docs/testing/copertura-e2e.yaml` aggiornato con le voci di questa
  applicazione; ogni storia che aveva dichiarato «rimando» sulla parte interna qui trova la sua copertura, oppure
  mantiene il rimando con motivo e storia proprietaria espliciti.
- **RT-3 — Isolamento fra account (§1).** Il percorso usa un account dedicato e non presuppone dati lasciati da
  altre prove.
- **RT-4 — Cinque lingue (§4).** Il percorso gira almeno in una lingua e verifica che nessuna chiave di
  traduzione compaia a schermo; la verifica completa delle cinque lingue resta alle prove del frontend.
- **RT-5 — Dati personali (§10).** Nessun dato vero, mai: è una regola, non una raccomandazione.
- **RT-6 — Accessibilità.** Il controllo automatico di accessibilità gira sulle schermate principali toccate dal
  percorso.

## 4. Criteri di accettazione

**CA-1 — Il percorso esiste e passa**
- **Dato** lo stack locale avviato · **Quando** si esegue `./run-tests.sh platform` · **Allora** il percorso
  `[J-BOOKGROVE]` è verde

**CA-2 — Copre il flusso di lavoro**
- **Dato** il percorso · **Quando** se ne leggono i passi · **Allora** si riconosce la giornata dell'attività:
  configurazione, appuntamento, spostamento, chiusura, indicatori

**CA-3 — Caso negativo**
- **Dato** un account al tetto delle risorse · **Quando** il percorso prova ad aprirne una in più · **Allora** il
  rifiuto `429` è osservato e verificato

**CA-4 — Registro coerente**
- **Dato** il registro di copertura · **Quando** si esegue il controllo dell'area `tooling` · **Allora** è verde:
  ogni voce punta a un test che esiste e ogni test etichettato è nel registro

**CA-5 — Nessuna attesa a tempo**
- **Dato** il codice del percorso · **Quando** lo si esamina · **Allora** non contiene attese a tempo fisso

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` completo;
- [ ] prove di **unità** e **integrazione**: non applicabile, questa storia è essa stessa una prova;
- [ ] prova di **isolamento fra account**: il percorso usa un account dedicato;
- [ ] **prova end-to-end**: **coperta ora** — è l'oggetto della storia, con
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni**: verificato che nessuna chiave compaia a schermo;
- [ ] **manifesto dei dati**: nessuna modifica;
- [ ] **registro delle decisioni** compilato: perimetro del percorso e rimandi mantenuti, con motivo;
- [ ] `run-tests.sh` esegue il nuovo percorso nell'area `platform`;
- [ ] documentazione dei test aggiornata.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storie da `0006` a `0015` | il percorso le attraversa |
| storia `0026` | il passo finale legge gli indicatori |

## 7. Fuori ambito

- il percorso del cliente finale sulla pagina pubblica: storia `0034`;
- le prove sui fornitori esterni (messaggistica, calendari): sono simulati e restano fuori dai percorsi.

## 8. Punti aperti

Nessuno.
