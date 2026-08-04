# Come verificare a mano la change 0086

Questa change **non tocca codice eseguibile**: produce documenti e artefatti navigabili sotto
`docs/usecases/_catalogo/`. Non c'è nulla da avviare con `./app-start.sh` e nessuna pagina del prodotto
cambia aspetto. La verifica è quindi **di lettura e di navigazione**, e il suo scopo è uno solo: stabilire
se questi documenti reggono il peso che dovranno reggere — essere dati in pasto alla skill `new-application`
e diventare il piano di lavoro di un'applicazione vera.

Tempo indicativo per il giro completo: **un'ora**. Se hai poco tempo, fai i punti 1, 2 e 5 su **una sola**
applicazione: bastano a capire se il metodo tiene.

---

## 1. Gli artefatti navigabili si aprono e si navigano

Apri i file con un doppio clic, **senza connessione di rete se puoi**: devono funzionare lo stesso.

```
open docs/usecases/_catalogo/*/artefatto-ux.html
```

Per ciascuno, guarda con i tuoi occhi:

| Cosa fare | Cosa ti aspetti |
|---|---|
| Cliccare ogni voce della barra laterale | Ogni voce porta a una schermata vera, nessuna pagina vuota o «da fare» |
| Premere l'interruttore del tema | Chiaro e scuro entrambi leggibili, nessun testo che sparisce sul fondo |
| Restringere la finestra fino a ~390 pixel | La pagina **non scorre in orizzontale**; le tabelle scorrono dentro il loro riquadro |
| Aprire una riga di tabella | Si apre una scheda di dettaglio con dati coerenti con la riga cliccata |
| Cercare e filtrare | I conteggi cambiano di conseguenza; svuotando i filtri si torna indietro |
| Leggere i riquadri di nota | Spiegano **perché** una schermata è fatta così, non ripetono cosa si vede |

La domanda vera da farti mentre navighi: **è così che voglio che l'applicazione si comporti?** L'artefatto
serve a farti dire «no, questa parte no» adesso, quando cambiarla costa una riga di documento.

Tre artefatti meritano una sosta perché mostrano una decisione, non solo un'interfaccia:

- `17-repgrove/artefatto-ux.html`, schermata **Richieste di recensione** — l'opzione «invita solo i clienti
  soddisfatti» compare **barrata**, con la fonte del divieto accanto. Verifica che ti convinca come scelta
  di posizionamento: è la funzione che i concorrenti implementano di nascosto;
- `20-insightgrove/artefatto-ux.html`, schermata **Previsioni e scostamenti** — controlla che una stima non
  possa mai essere scambiata per un dato rilevato, anche guardando distrattamente;
- `31-auditgrove/artefatto-ux.html`, schermata **Integrità e sigilli** — è il cuore del prodotto: se non
  capisci in trenta secondi *perché* quel registro sarebbe credibile davanti a un terzo, la schermata non
  funziona.

---

## 2. Una descrizione di applicazione è davvero pronta per `new-application`

Prendi una qualsiasi `docs/usecases/_catalogo/NN-*/application-description.md` e verifica che contenga, già
scritte, le risposte che la skill pretende **prima** di generare:

- [ ] identificativo dell'applicazione conforme a `^[a-z][a-z0-9_]{0,30}$`, e che **non collida** con le app
      reali già presenti (`fatture`, il mini-CRM) né con le altre proposte;
- [ ] modello utente `single` o `multi`, **con la motivazione** — non una scelta a caso;
- [ ] porta locale proposta (convenzione `8100 + numero di catalogo`);
- [ ] **una sola** metrica di quota, con la natura `flow` o `stock` argomentata con un esempio nelle parole
      dell'applicazione;
- [ ] colore di categoria fra quelli ammessi;
- [ ] proposta di listino, **marcata come da confermare**;
- [ ] proposta di classificazione dei dati personali, **marcata come da confermare**, con avviso forte se
      entrano categorie particolari.

La prova del nove: **apri la skill e fai finta di rispondere alle sue domande leggendo solo questo file.**
Se ti fermi a cercare altrove, quel file non è pronto.

---

## 3. L'indice non mente

Per ogni applicazione, l'indice delle epiche e delle storie dentro `application-description.md` deve
combaciare con i file su disco. Controllo rapido:

```bash
for d in docs/usecases/_catalogo/[0-9]*/; do
  idx=$(grep -oE '\]\([0-9]{2}-[a-z0-9-]+/[0-9]{4}-[a-z0-9-]+\.md\)' "$d/application-description.md" | sed 's/^](//; s/)$//' | sort -u | wc -l)
  fs=$(find "$d" -name '0*.md' | wc -l)
  printf "%-24s indice:%-4s file:%-4s %s\n" "$(basename $d)" "$idx" "$fs" "$([ "$idx" = "$fs" ] && echo ok || echo DISALLINEATO)"
done
```

Poi apri **due o tre collegamenti a caso** dall'indice e verifica che portino davvero da qualche parte.

---

## 4. Una storia è implementabile

Apri una storia qualunque, meglio se di un'epica di dominio e non delle fondamenta. Verifica che:

- [ ] il titolo **non contenga congiunzioni** («creazione e invio e promemoria» significa tre storie);
- [ ] i requisiti funzionali siano al massimo sei o sette, e ognuno sia verificabile — non «funziona bene»;
- [ ] i requisiti tecnici **richiamino per nome** gli invarianti applicabili (filtro per conto, cinque
      lingue, esposizione conversazionale, prove);
- [ ] i criteri di accettazione siano in forma dato/quando/allora;
- [ ] la definizione di fatto porti le voci di piattaforma (suite verde, percorso end-to-end se tocca
      superficie, registro delle decisioni, manifesto dei dati se tratta dati di persone);
- [ ] **la domanda che conta**: la implementeresti in un giorno? Se no, la storia è troppo grande e va
      spezzata prima di arrivare in produzione.

---

## 5. Le fonti esistono e dicono quello che si dice

Nella sezione «Mercato e analisi in rete» di una descrizione, prendi **tre collegamenti a caso** e aprili.

- [ ] la pagina esiste;
- [ ] dice davvero quello che la riga accanto sostiene che dica;
- [ ] se è un prezzo, è una **pagina ufficiale del fornitore** e non un sito di comparazione — e se è un
      sito di comparazione, il documento lo dichiara.

Verifica anche che ci sia una sezione onesta su **ciò che non è stato trovato**. Una ricerca senza buchi
dichiarati è sospetta: significa che qualcuno ha colmato a intuito.

---

## 6. Le venti applicazioni escluse

Apri `docs/usecases/_catalogo/_escluse/README.md` e rileggi il criterio con calma, a mente fredda.

- [ ] sei ancora d'accordo con la soglia che hai scelto?
- [ ] c'è qualcuna delle venti che rivorresti dentro? (rientrare costa poco: si rilancia il drill-down di
      quella sola applicazione col kit d'autore)
- [ ] le due conservate — `01-invoicegrove` e `05-chatgrove` — sono complete e leggibili, e si capisce dal
      documento che sono **fuori dal piano di costruzione**?

---

## 7. I punti che tornano a te

Questa change ha lasciato tre decisioni in `docs/_BACKLOG.md`, tutte in coda al file. Leggile: non sono
dettagli, e due delle tre vanno chiuse **prima** di scaffoldare la prima applicazione.

- [ ] **superfici pubbliche senza autenticazione** — chi dice qual è il conto quando chi guarda la pagina
      non è un utente. Serve un'unica implementazione condivisa, non una per applicazione;
- [ ] **autenticazione di una macchina** — come si ricava il conto quando il chiamante è un agente o un
      servizio, e non una persona;
- [ ] **«prospetto sì, classifica no»** — se promuovere a invariante il divieto di aggregati per persona,
      oggi rispettato per convenzione da tre applicazioni su tre.

Infine, ogni cartella di applicazione ha la sua sezione «Rischi e punti aperti», e ogni proposta di listino
e di dati personali è marcata come **da confermare**: sono tue, nessun agente le ha decise.

---

## 8. La suite automatica

La change non tocca codice, ma il verde è la prova che non ha rotto nulla per sbaglio:

```bash
./run-tests.sh
```

Ci si aspetta **verde su tutte le aree**. Un rosso qui non riguarda i documenti: riguarda qualcosa che è
stato toccato senza volerlo.
