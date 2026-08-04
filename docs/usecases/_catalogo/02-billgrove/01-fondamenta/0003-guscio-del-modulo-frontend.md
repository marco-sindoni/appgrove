# 0003 — Guscio del modulo frontend

**Applicazione**: 02 — BillGrove (`billing`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`, `0002`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di una micro-impresa che ha appena attivato BillGrove
> voglio trovare l'app nella barra laterale del backoffice e vedere una schermata che mi dice come sta andando
> così da capire in tre secondi che l'app è mia, è accesa e funziona, anche quando è ancora vuota.

**Contesto.** Senza il guscio del modulo l'app esiste solo per chi sa chiamare le rotte. Questa storia mette
BillGrove nella barra laterale, apre la panoramica e prepara i posti dove le storie successive appenderanno le loro
schermate. Va fatta ora perché ogni storia di dominio ha una parte visibile, e senza guscio ognuna dovrebbe
inventarsene uno.

## 2. Requisiti funzionali

1. **RF-1** — Esiste il modulo `frontend/apps/backoffice/src/modules/billing/` con il suo manifesto, registrato
   nell'elenco dei moduli.
2. **RF-2** — L'app compare nella barra laterale **solo** quando registro e abilitazione dicono di sì.
3. **RF-3** — Il modulo dichiara le sezioni previste dall'app: Panoramica, Documenti, Clienti, Catalogo,
   Impostazioni; quelle non ancora costruite sono presenti ma inattive, con l'indicazione che arriveranno.
4. **RF-4** — La Panoramica mostra i tre numeri del titolare (fatturato del periodo, da incassare, scaduto) letti
   dalle rotte esistenti, e lo stato vuoto quando non c'è nulla.
5. **RF-5** — La Panoramica mostra il consumo della quota `documenti` con il tetto del piano.
6. **RF-6** — Tutti i testi visibili sono presenti in `en, it, fr, es, de`.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Il modulo non conosce il `tenant_id` se non attraverso il contesto che la
  shell gli passa, e non lo invia mai nelle richieste.
- **RT-2 — Interfaccia di programmazione (§2).** I dati si leggono con il client generato dalla definizione
  OpenAPI; nessuna chiamata scritta a mano.
- **RT-4 — Modulo frontend (§3, §5).** Manifesto `{ id: 'billing', name, icon, accentToken: 'teal', sections[],
  resources, quota, component }`, aggiunto all'elenco `MODULES` del registro. Caricamento su richiesta. Solo token
  del sistema di design, nessun colore scritto a mano; funziona in tema chiaro e in tema scuro. Vietate le librerie
  con aspetto proprio marcato.
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe passano dallo spazio-nomi `billing` e stanno in
  `modules/billing/i18n/{en,it,fr,es,de}.ts`. Nessun testo scritto a mano nei componenti. La storia non è conclusa
  se manca una lingua.
- **RT-6 — Varchi e quota (§6).** Quando l'abbonamento non dà accesso, il modulo mostra il messaggio di attivazione
  invece della schermata; quando la quota è vicina al tetto, mostra l'avviso **prima** che l'utente cominci a
  lavorare, non dopo il salvataggio.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento qui. La Panoramica però mostra un riquadro che
  spiega che le stesse funzioni saranno richiamabili da chat: è la promessa dell'app, e va vista subito.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo: il modulo mostra dati già dichiarati altrove.
- **RT-9 — Registrazione eventi (§14).** Nessun evento nuovo lato servizio.

## 4. Criteri di accettazione

**CA-1 — L'app compare per chi è abbonato**
- **Dato** un account abbonato a BillGrove e un utente con ruolo `member`
- **Quando** apre il backoffice
- **Allora** vede «BillGrove» nella barra laterale, con la tinta `teal`, e cliccandola atterra sulla Panoramica

**CA-2 — L'app non compare per chi non è abbonato**
- **Dato** un account senza abbonamento a BillGrove · **Quando** l'utente apre il backoffice
- **Allora** la voce non compare nella barra laterale

**CA-3 — Primo avvio**
- **Dato** un account abbonato e nessun documento
- **Quando** apre la Panoramica
- **Allora** vede uno stato vuoto con un titolo, una spiegazione e un'azione («crea il primo documento»), non una
  schermata di zeri

**CA-4 — Cinque lingue**
- **Dato** l'interfaccia impostata su ciascuna delle cinque lingue
- **Quando** si apre la Panoramica
- **Allora** nessuna stringa compare come chiave non tradotta, in nessuna delle cinque

**CA-5 — Tema chiaro e scuro**
- **Dato** il tema scuro attivo · **Quando** si apre il modulo
- **Allora** tutti i colori vengono dai token e il controllo automatico di accessibilità sulle schermate principali
  passa

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno `frontend`; l'intera suite prima del commit), compreso il
      controllo dei tipi `tsc --noEmit`;
- [ ] prove di **unità** con Vitest e Testing Library sul componente della Panoramica, con lo strato di rete finto;
- [ ] prova di **isolamento fra account**: non applicabile lato frontend, dichiarato;
- [ ] **prova end-to-end**: *coprire ora, parzialmente* — primo passo del percorso `[J-BILLING]`: accesso, l'app
      compare nella barra laterale, si apre la Panoramica. Registro di copertura
      [docs/testing/copertura-e2e.yaml](../../../../testing/copertura-e2e.yaml) aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna modifica, dichiarato;
- [ ] **registro delle decisioni** compilato;
- [ ] contratto degli **strumenti conversazionali**: nessuno qui, dichiarato;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali;
- [ ] modulo abilitato nello stub locale dell'abilitazione, finché quella reale non esiste.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | Serve la definizione OpenAPI da cui si genera il client |
| storia `0002` | Servono le rotte di elenco dei documenti per popolare la Panoramica |

## 7. Fuori ambito

- le schermate di dominio (documenti, clienti, catalogo): rispettive storie delle epiche 02, 03 e 04;
- la stampa del documento: storia `0016`;
- il calcolo vero dei tre numeri del titolare: storia `0021` (qui si mostrano i conteggi grezzi disponibili).

## 8. Punti aperti

Nessuno.
