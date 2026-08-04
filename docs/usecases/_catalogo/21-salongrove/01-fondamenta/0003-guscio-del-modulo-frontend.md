# 0003 — Guscio del modulo frontend

**Applicazione**: 21 — SalonGrove (`salone`) · **Epica**: 01 — Fondamenta
**Storia**: `0003` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0001`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare di un salone che ha appena attivato l'applicazione
> voglio trovare nella barra laterale le sezioni del mio mestiere, con i nomi che uso io
> così da capire in dieci secondi dove sta la cabina, dove i pacchetti e dove le percentuali.

**Contesto.** Il guscio non contiene funzioni, ma decide tre cose che dopo non si cambiano a costo zero: **quali
sezioni esistono**, **come si chiamano** e **in che ordine stanno**. Nel beauty il vocabolario conta più che
altrove — «postazione» e non «risorsa», «cabina» e non «deposito interno», «scheda tecnica» e non «annotazioni» —
perché è il vocabolario con cui il cliente parla del proprio lavoro. Sotto la via (b) del §0 le sezioni si
aggiungono a un modulo che esiste già; sotto la via (a) il modulo nasce qui.

## 2. Requisiti funzionali

1. **RF-1** — Il modulo dichiara nel manifesto identificativo, nome, icona, colore-categoria, sezioni, risorse e
   metrica di quota, ed è registrato nell'elenco dei moduli della shell.
2. **RF-2** — Le sezioni sono: **Agenda** (che sotto la via (b) è quella esistente), **Servizi**, **Clienti e
   schede**, **Cabina e prodotti**, **Conti**, **Pacchetti**, **Provvigioni**, **Andamento**. Ognuna è raggiungibile
   e mostra uno stato vuoto che dice cosa ci finirà dentro.
3. **RF-3** — Le sezioni che appartengono a un piano superiore compaiono **spente e spiegate**, non nascoste: chi
   ha il piano gratuito deve vedere che la cabina esiste e sapere come si accende.
4. **RF-4** — Tutti i testi visibili passano dallo spazio-nomi del modulo e sono presenti in tutte e cinque le
   lingue; nessuna stringa scritta a mano dentro un componente.
5. **RF-5** — Il modulo funziona in tema chiaro e in tema scuro usando solo i token del sistema di design, e supera
   il controllo automatico di accessibilità sulle schermate introdotte.

## 3. Requisiti tecnici

- **RT-1 — Modulo frontend (§3).** Modulo React caricato su richiesta sotto
  `frontend/apps/backoffice/src/modules/<id>/`, con `manifest.ts` che dichiara
  `{ id, name, icon, accentToken, sections[], resources, quota, component }`, aggiunto all'elenco `MODULES` del
  registro. Il modulo non gestisce l'autenticazione e non conosce il `tenant_id` se non dal contesto della shell.
- **RT-2 — Sistema di design (§5).** Solo token; colore-categoria `red` (via a) o `green` ereditato (via b), lo
  stesso nel manifesto (`accentToken`) e nel listino (`category`). Nessun colore scritto a mano; vietate le
  librerie con aspetto proprio marcato.
- **RT-3 — Cinque lingue (§4).** Traduzioni accanto al modulo in `i18n/{en,it,fr,es,de}.ts`, sotto lo spazio-nomi
  che coincide con l'identificativo del modulo. **La storia non è conclusa se ne manca una.**
- **RT-4 — Abilitazione (§6).** La barra laterale mostra il modulo quando **registro ∩ abilitazione** dicono di
  sì; finché l'abilitazione reale non esiste, il modulo si accende nello stub locale.
- **RT-5 — Client delle interfacce (§3).** Le sezioni leggono i dati con il client generato dalla definizione
  OpenAPI: nessuna chiamata scritta a mano.
- **RT-6 — Dati personali (§10).** Nessun dato personale nuovo: il guscio non mostra dati.
- **RT-7 — Esposizione conversazionale (§12).** Nessuno strumento in questa storia.

## 4. Criteri di accettazione

**CA-1 — Il modulo compare**
- **Dato** un account abilitato al verticale
- **Quando** si apre il backoffice
- **Allora** il modulo è nella barra laterale sotto «Le tue app», con il suo colore e tutte le sue sezioni

**CA-2 — Le sezioni del piano superiore sono visibili e spiegate**
- **Dato** un account sul piano gratuito
- **Quando** si apre la sezione «Cabina e prodotti»
- **Allora** la sezione c'è, è spenta, e dice in una frase cosa fa e quale piano la accende — non è nascosta e non
  dà un errore

**CA-3 — Cinque lingue**
- **Dato** l'interfaccia in ciascuna delle cinque lingue
- **Quando** si percorrono tutte le sezioni
- **Allora** non compare nessuna chiave di traduzione al posto di un testo, in nessuna lingua

**CA-4 — Tema e accessibilità**
- **Dato** il tema chiaro e il tema scuro
- **Quando** si esegue il controllo automatico di accessibilità sulle sezioni
- **Allora** non ci sono violazioni, e nessun colore è scritto fuori dai token

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh frontend` (compreso il controllo dei tipi `tsc --noEmit`) e la suite
      intera prima del commit;
- [ ] prove di **unità** sul manifesto e sulla resa delle sezioni con strato di rete finto;
- [ ] prova di **isolamento fra account**: non applicabile lato frontend — dichiarato;
- [ ] **prova end-to-end**: *rimando* — passo del percorso `[J-SALONGROVE]` della storia `0030`;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato: nomi e ordine delle sezioni, scelta del vocabolario di settore,
      sezioni spente invece che nascoste;
- [ ] avvio locale invariato e modulo abilitato nello stub locale.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0001` | serve sapere se il modulo nasce o si estende |
| stub locale dell'abilitazione | finché l'abilitazione reale non esiste, è l'unico modo di vedere il modulo |

## 7. Fuori ambito

- il contenuto delle sezioni: sta nelle epiche di dominio;
- il varco a `429` sulla quota: storia `0004`;
- l'artefatto navigabile di proposta ([artefatto-ux.html](../artefatto-ux.html)) non è codice: è materiale di
  partenza per questa storia, non il suo esito.

## 8. Punti aperti

**Il vocabolario è una scelta di prodotto, non di traduzione.** «Postazione», «cabina», «scheda tecnica» sono
termini italiani del mestiere: le altre quattro lingue vanno rese da chi conosce il settore in quel paese, non
tradotte alla lettera. Finché non c'è quella verifica, le traduzioni non italiane sono **provvisorie e vanno
marcate come tali** nel registro delle decisioni.
