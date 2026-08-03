# 0017 — Cruscotto e riquadri

**Applicazione**: 20 — InsightGrove (`insights`) · **Epica**: 04 — Cruscotti e avvisi
**Storia**: `0017` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0015`, `0016`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che ha tre minuti il lunedì mattina
> voglio una pagina che mi dica come va e che cosa richiede attenzione
> così da non dover aprire cinque applicazioni per farmi un'idea.

**Contesto.** È la pagina che il cliente guarda tutti i giorni, e la sola misura di successo di questa app.
Il materiale consultato è concorde: **5-10 indicatori, non di più**, e sempre gli stessi (§2.5 della
[descrizione](../application-description.md), fonte 5). La tentazione è di dare all'utente la libertà di
costruire quello che vuole; la fonte 5 avverte che «un eccesso di dati può risultare controproducente», e le
lamentele rilevate sui concorrenti riguardano proprio la fatica di configurare. La forma scelta è quindi:
pochi riquadri, tutti costruiti sullo stesso motore di calcolo, ciascuno con la sua ricevuta.

## 2. Requisiti funzionali

1. **RF-1** — Un account ha uno o più **cruscotti**; ciascuno contiene fino a dodici **riquadri**, e oltre quel
   numero l'interfaccia dice che sta diventando troppo.
2. **RF-2** — Un riquadro dichiara: metrica, periodo, confronto, forma (numero singolo, andamento nel tempo,
   ripartizione per dimensione) ed eventuale dimensione di scomposizione.
3. **RF-3** — Ogni riquadro mostra il valore, l'unità, il confronto se richiesto, il contrassegno di
   completezza e l'accesso alla **scheda del numero** (storia 0016).
4. **RF-4** — I riquadri si riordinano; il cruscotto ha un titolo; esiste un cruscotto **predefinito** che è
   quello che si apre entrando nell'app.
5. **RF-5** — Un riquadro che riferisce una metrica **ritirata** o una fonte scollegata non sparisce e non mostra
   un numero vecchio: mostra il motivo per cui non produce valori, con l'azione per rimediare.
6. **RF-6** — Un cruscotto che contiene metriche economiche è visibile a `owner` e `admin`; per un `member` i
   riquadri economici sono sostituiti dal messaggio di ruolo insufficiente, e il resto del cruscotto funziona
   (storia 0014).

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `cruscotto` e `riquadro` filtra per
  `tenant_id` preso dal gettone verificato; un `tenant_id` dal corpo della richiesta viene ignorato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `GET|POST /api/insights/v1/cruscotti`,
  `GET|PUT|DELETE /api/insights/v1/cruscotti/{id}`, con i riquadri come sotto-risorsa; corpo validato; errori in
  `application/problem+json`; definizione OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V<N>__cruscotti.sql` sullo schema `app_insights`: tabelle `cruscotto`
  e `riquadro` con `tenant_id`, chiave primaria UUID versione 7, colonne di controllo e cancellazione logica.
- **RT-4 — Modulo frontend (§3, §5).** Sezione `Cruscotto` del modulo `insights`; dati letti con il client
  generato; solo token del sistema di design; funziona in tema chiaro e scuro. **Nota specifica**: i colori
  `amber`, `red` e `green` restano riservati a significare lo **stato dei numeri**, non l'accento dell'app
  (§3 della descrizione).
- **RT-5 — Cinque lingue (§4).** Tutte le stringhe della sezione, comprese le forme dei riquadri e i messaggi di
  indisponibilità, esistono in `en, it, fr, es, de`.
- **RT-6 — Varchi e ruoli (§6).** Modificare un cruscotto richiede `owner` o `admin`; un `member` lo legge.
  I riquadri economici seguono la storia 0014. **Nessun consumo di quota**: i cruscotti sono illimitati in ogni
  piano.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo, salvo le etichette di dimensione già dichiarate
  se la via (A) è stata scelta.
- **RT-11 — Prove (§11).** Prove frontend con finto strato di rete; controllo automatico di accessibilità sulla
  schermata; prova di isolamento fra account e matrice dei ruoli sulle risorse.

## 4. Criteri di accettazione

**CA-1 — Il cruscotto mostra i numeri**
- **Dato** un account con fonti collegate e un cruscotto con sei riquadri
- **Quando** l'utente `owner` apre la sezione Cruscotto
- **Allora** vede i sei valori con unità e confronto, e ciascuno ha l'accesso alla scheda del numero

**CA-2 — Un riquadro senza dati non mente**
- **Dato** un riquadro su una metrica la cui fonte è stata scollegata
- **Quando** si apre il cruscotto
- **Allora** quel riquadro mostra «non calcolabile — la fonte magazzino non è collegata» con il rimando, e
  **non** l'ultimo valore noto

**CA-3 — Un `member` vede il cruscotto ridotto**
- **Dato** un cruscotto con quattro riquadri operativi e due economici
- **Quando** lo apre un utente `member`
- **Allora** vede i quattro operativi con i loro numeri e, al posto dei due economici, il messaggio di ruolo
  insufficiente

**CA-4 — Troppi riquadri**
- **Dato** un cruscotto con dodici riquadri
- **Quando** si prova ad aggiungerne un tredicesimo
- **Allora** l'interfaccia lo consente ma avverte che oltre una decina di indicatori il cruscotto smette di
  servire, citando il motivo

**CA-5 — Isolamento fra account**
- **Dato** due account con cruscotti propri
- **Quando** un utente di `A` chiede il cruscotto di `B` per identificativo
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (almeno le aree toccate; l'intera suite prima del commit);
- [ ] prove di **unità** sulla composizione del riquadro e di **integrazione** sulle risorse, con database
      effimero e migrazioni vere;
- [ ] prova di **isolamento fra account** e **matrice dei ruoli** su cruscotti e riquadri;
- [ ] **prova end-to-end**: *coprire ora* — il percorso `[J-INSIGHTS]` include «apri il cruscotto e leggi un
      indicatore»; registro di copertura aggiornato;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con il tetto morbido dei dodici riquadri e il perché;
- [ ] contratto degli **strumenti conversazionali**: nessuna funzione nuova qui; la creazione di un cruscotto da
      chat resta fuori ambito (vedi §7);
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare senza passi manuali.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| storia `0015` | i riquadri mostrano valori calcolati su periodi |
| storia `0016` | ogni riquadro dà accesso alla scheda del numero |
| storia `0014` | i riquadri economici seguono la classe di riservatezza |

## 7. Fuori ambito

- il cruscotto già pieno al primo accesso: storia 0018;
- gli avvisi: storie 0019-0021;
- la **creazione di cruscotti da chat**: rimandata: comporre una disposizione visiva a voce è più faticoso che
  farlo con il dito, e nessuna fonte l'ha indicata come richiesta. Se servirà, sarà una storia dell'epica 07.

## 8. Punti aperti

- **Quanti cruscotti per account?** Uno solo è più semplice e coerente con «5-10 numeri»; più di uno serve a chi
  vuole separare vendite da operazioni. Raccomandazione: **fino a cinque**, senza farne una funzione da vendere.
  Chiude: **sviluppatore**.
- **La condivisione di un cruscotto verso l'esterno** (un collegamento pubblico per il commercialista) è
  **fuori ambito** e resta un punto aperto della descrizione (punto 4): è un effetto verso l'esterno su dati
  economici, e non lo decide un agente.
