# 0025 — Avanzamento del progetto

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 05 — Avanzamento, margine e catena della suite
**Storia**: `0025` · **Taglia stimata**: mezza giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0009`, `0021`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come titolare che il cliente chiama chiedendo «a che punto siamo»
> voglio una schermata che risponda in cinque secondi
> così da non dover aprire tre viste e fare i conti a mente mentre parlo al telefono.

**Contesto.** «A che punto siamo» è una delle due domande per cui questa app esiste (l'altra è «ci abbiamo
guadagnato», storia 0026). La difficoltà non è tecnica ma di onestà del numero: la percentuale di completamento
calcolata sul conteggio delle attività è quasi sempre bugiarda, perché le attività non sono uguali fra loro.
Questa storia sceglie di mostrare **più misure accostate**, ciascuna dichiarata per quello che è, invece di una
sola percentuale che sembra una verità e non lo è.

## 2. Requisiti funzionali

1. **RF-1** — La schermata di avanzamento mostra, accostate: attività terminate su totali; **ore dichiarate su
   ore stimate**; consumo del budget (storia 0021); traguardi raggiunti su totali con la loro data.
2. **RF-2** — Ogni misura porta l'etichetta di che cosa misura: non esiste una «percentuale di avanzamento» unica
   e senza qualificazione.
3. **RF-3** — La schermata segnala i **ritardi**: attività scadute e non terminate, traguardi a rischio, e il
   numero di giorni di scostamento rispetto alla fine prevista.
4. **RF-4** — Quando mancano i dati per una misura (nessuna stima, nessun budget, nessun traguardo) la schermata
   lo dice e propone come rimediare, invece di mostrare zero.
5. **RF-5** — L'avanzamento è visibile a tutti i ruoli; le **misure economiche** (budget in importo) solo a chi ha
   ruolo `admin`.
6. **RF-6** — La schermata è quella che risponde allo strumento conversazionale `get_project_progress`: la stessa
   fonte, non un secondo calcolo.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Tutte le aggregazioni filtrano per `tenant_id` dal token verificato.
- **RT-2 — Interfaccia di programmazione (§2).** Rotta `GET /api/progetti/v1/projects/{id}/progress`; il corpo
  della risposta contiene le misure con la loro qualificazione; errori in `application/problem+json`; OpenAPI
  aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Nessuna tabella nuova: tutto è **derivato**. Se le aggregazioni risultassero lente
  su progetti grandi, la soluzione è un indice o una vista materializzata ricalcolabile, mai un totale scritto a
  mano che può divergere dai dati.
- **RT-4 — Modulo frontend (§3, §5).** Riquadro di avanzamento nella scheda del progetto; solo token del sistema
  di design; tema chiaro e scuro; le barre non devono essere l'unico veicolo dell'informazione.
- **RT-5 — Cinque lingue (§4).** Nomi delle misure, spiegazioni e messaggi di dato mancante in
  `en, it, fr, es, de`; date e numeri formattati secondo la lingua.
- **RT-6 — Varchi e quota (§6, §7).** Nessun consumo di quota. Con ruolo `member` la risposta omette le misure
  economiche invece di rispondere `403`: la parte non economica gli serve.
- **RT-7 — Esposizione conversazionale (§12).** Strumento `get_project_progress(id_progetto)`, **lettura**, che
  usa la stessa rotta e rispetta lo stesso filtro di ruolo (storia 0028). È l'azione che la scheda di catalogo
  indica come principale.
- **RT-8 — Dati personali (§10).** Nessun dato personale nuovo, e nessuna misura **per persona**: l'avanzamento è
  del progetto ([application-description.md](../application-description.md) §6).
- **RT-9 — Registrazione eventi (§14).** Nessun evento di dominio; le aggregazioni lente vanno registrate con
  durata e dimensione del progetto.

## 4. Criteri di accettazione

**CA-1 — Misure accostate**
- **Dato** un progetto con 12 attività su 20 terminate, 80 ore dichiarate su 120 stimate e 2 traguardi su 3
- **Quando** si apre l'avanzamento
- **Allora** compaiono le tre misure distinte e qualificate, e nessuna percentuale unica

**CA-2 — Dati mancanti**
- **Dato** un progetto senza stime in ore
- **Quando** si apre l'avanzamento
- **Allora** la misura sulle ore dice che mancano le stime e spiega dove si mettono, invece di mostrare 0 %

**CA-3 — Ritardi**
- **Dato** tre attività scadute e un traguardo a rischio
- **Quando** si apre l'avanzamento
- **Allora** i ritardi sono elencati in cima con lo scostamento in giorni

**CA-4 — Filtro di ruolo**
- **Dato** un utente con ruolo `member`
- **Quando** apre l'avanzamento
- **Allora** vede le misure di lavoro e non vede il budget in importo

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` chiede l'avanzamento di un progetto di `B`
- **Allora** riceve `404`

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`);
- [ ] prove di **unità** su ciascuna misura, compresi i casi di dato mancante, e di **integrazione** sulla rotta
      con un progetto di dimensione realistica;
- [ ] prova di **isolamento fra account** e prova della matrice dei ruoli sulle misure economiche;
- [ ] **prova end-to-end**: coprire ora — `[J-PROGETTI]` legge l'avanzamento dopo aver dichiarato le ore
      (storia 0031); voce nel registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue;
- [ ] **manifesto dei dati**: nessuna voce nuova;
- [ ] **registro delle decisioni** compilato, con annotato **perché non esiste una percentuale unica di
      avanzamento**;
- [ ] controllo automatico di **accessibilità** verde sul riquadro di avanzamento;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0009` | I traguardi sono una delle misure |
| Storia `0021` | Il consumo del budget è un'altra misura |
| Storia `0017` | Le ore dichiarate sono la misura più affidabile |

## 7. Fuori ambito

- l'avanzamento di portafoglio (tutti i progetti insieme): rimandato; per una micro-impresa l'elenco dei progetti
  con il loro consumo basta;
- la condivisione dell'avanzamento con il cliente: non c'è portale (§1 della descrizione), si esporta
  (storia 0027);
- la previsione della data di fine con metodi statistici: non prevista, si mostra lo scostamento osservato.

## 8. Punti aperti

- Nessuno.
