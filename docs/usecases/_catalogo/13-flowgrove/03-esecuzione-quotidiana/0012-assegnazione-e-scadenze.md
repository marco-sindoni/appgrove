# 0012 — Assegnazione e scadenze

**Applicazione**: 13 — FlowGrove (`progetti`) · **Epica**: 03 — Esecuzione quotidiana
**Storia**: `0012` · **Taglia stimata**: una giornata · **Stato**: 🟡 bozza d'autore
**Dipende da**: `0004`, `0007`
**Ultimo aggiornamento**: 2026-08-03

## 1. Narrazione

> Come responsabile della commessa
> voglio dire chi fa cosa entro quando
> così da smettere di ripeterlo a voce e da poter attribuire le ore a chi le ha fatte.

**Contesto.** L'assegnazione è il punto in cui FlowGrove comincia a trattare **dati dei lavoratori** e in cui si
occupa un posto della quota (storia 0004). È anche il punto in cui va fissata una scelta di prodotto delicata:
l'app **non** ha una vista «rendimento per persona». Il carico per persona esiste — serve a non sovraccaricare
qualcuno — ma è per progetto e per periodo, e mostra ore assegnate, non giudizi. La ragione è duplice: giuridica
(l'articolo 4 dello Statuto dei lavoratori, [application-description.md](../application-description.md) §2.3) e
pratica (uno strumento percepito come misura delle persone viene compilato male, §2.5).

## 2. Requisiti funzionali

1. **RF-1** — Un'attività si assegna a **una** persona dell'account; l'assegnazione si può cambiare e togliere, e
   resta traccia di chi ha assegnato e quando.
2. **RF-2** — L'attività può avere una scadenza; il cambio di scadenza resta tracciato.
3. **RF-3** — Assegnare a una persona che non ha ancora un posto occupato ne occupa uno; se il tetto è raggiunto,
   la risposta è `429` con il messaggio della storia 0004.
4. **RF-4** — Esiste una vista **carico per persona** limitata a un progetto e a un periodo, che mostra quante
   attività aperte e quante ore stimate ha ciascuno. Non mostra ore lavorate, non ordina per «produttività», non
   assegna punteggi.
5. **RF-5** — Ogni persona vede sempre le proprie assegnazioni; l'app dichiara in chiaro, in una nota della
   schermata, che il responsabile vede le assegnazioni di tutti.
6. **RF-6** — Togliere l'ultima assegnazione aperta di una persona non libera automaticamente il posto: la
   liberazione è un'azione esplicita (storia 0004), perché deve essere una decisione, non un effetto collaterale.

## 3. Requisiti tecnici

- **RT-1 — Isolamento fra account (§1).** Ogni lettura e scrittura di `assignment` filtra per `tenant_id` dal
  token verificato; la persona assegnata deve appartenere allo stesso account.
- **RT-2 — Interfaccia di programmazione (§2).** Rotte `PUT|DELETE /api/progetti/v1/tasks/{id}/assignee` e
  `GET /api/progetti/v1/projects/{id}/workload?periodo=`; corpo validato; errori in `application/problem+json`;
  OpenAPI aggiornata nello stesso commit.
- **RT-3 — Persistenza (§8).** Migrazione `V6__assegnazioni.sql`: `assignment` con `tenant_id`, `task_id`,
  `user_id`, `assigned_by`, colonne di controllo e cancellazione logica; indice `(tenant_id, user_id, task_id)`.
- **RT-4 — Modulo frontend (§3, §5).** Selettore della persona sulla scheda dell'attività e sulla scheda della
  lavagna; riquadro del carico nella scheda del progetto; solo token del sistema di design; tema chiaro e scuro.
- **RT-5 — Cinque lingue (§4).** Etichette, messaggi di quota e la **nota di trasparenza** verso chi è assegnato,
  in `en, it, fr, es, de`. La nota di trasparenza è un testo delicato: va tradotta, non copiata.
- **RT-6 — Varchi e quota (§6, §7).** Prima di creare un'assegnazione verso una persona nuova il servizio prenota
  un posto sulla metrica `seats` (natura `stock`); a tetto raggiunto risponde `429` con il rimedio. Ruolo minimo
  per assegnare: `admin`; ogni `member` può assegnare a sé stesso.
- **RT-7 — Esposizione conversazionale (§12).** Strumento `assign_task(id_attività, persona)`, marcato
  **scrittura con bozza e conferma umana** (storia 0029): assegnare lavoro a un'altra persona non è mai
  automatico.
- **RT-8 — Dati personali (§10).** `assignment.user_id` e `assignment.assigned_by` sono dati personali di
  lavoratori: voci nuove nel manifesto in italiano e inglese, campi annotati `@PersonalData`, tabella
  `assignment` in `exportData` e `purgeData`.
- **RT-9 — Registrazione eventi (§14).** «Attività assegnata», «assegnazione rimossa», «assegnazione respinta per
  quota» con `tenant_id`, `app_id`, `user_id` e correlazione; identificativi, **mai** nomi.

## 4. Criteri di accettazione

**CA-1 — Assegnazione**
- **Dato** un'attività senza assegnatario e un account con posti disponibili
- **Quando** il responsabile la assegna a una persona
- **Allora** l'attività risulta assegnata, il posto risulta occupato e resta traccia di chi ha assegnato

**CA-2 — Quota esaurita**
- **Dato** un account con tutti i posti occupati
- **Quando** si assegna un'attività a una persona che non ne ha ancora uno
- **Allora** la risposta è `429`, nessuna assegnazione viene creata e il messaggio dice come rimediare

**CA-3 — Carico per persona**
- **Dato** un progetto con tre persone e attività stimate
- **Quando** si apre il riquadro del carico per il mese corrente
- **Allora** compaiono attività aperte e ore **stimate** per ciascuno, e **non** compaiono ore lavorate né
  ordinamenti per rendimento

**CA-4 — Trasparenza**
- **Dato** un utente con ruolo `member`
- **Quando** apre le proprie attività
- **Allora** vede la nota che dice chiaramente chi altro può vedere le sue assegnazioni

**CA-5 — Isolamento fra account**
- **Dato** due account `A` e `B`
- **Quando** un utente di `A` assegna un'attività a una persona di `B`
- **Allora** riceve `422` e nulla viene creato

## 5. Definizione di fatto

- [ ] tutti i requisiti funzionali soddisfatti e tutti i criteri di accettazione verdi;
- [ ] **prove verdi** con `./run-tests.sh` (aree `backend`, `frontend`, `compliance`);
- [ ] prove di **unità** sull'occupazione del posto e di **integrazione** sull'assegnazione con quota esaurita;
- [ ] prova di **isolamento fra account** su assegnazione e vista del carico;
- [ ] **prova end-to-end**: coprire ora — l'assegnazione è un passo di `[J-PROGETTI]` (storia 0031); voce nel
      registro di copertura;
- [ ] **traduzioni** presenti in tutte e cinque le lingue, compresa la nota di trasparenza;
- [ ] **manifesto dei dati** aggiornato in italiano e inglese per `assignment`;
- [ ] **registro delle decisioni** compilato, con annotato **perché non esiste una vista di rendimento per
      persona**;
- [ ] controllo automatico di **accessibilità** verde sul selettore e sul riquadro del carico;
- [ ] `./dev.sh services` e l'avvio locale continuano a funzionare.

## 6. Dipendenze

| Dipende da | Perché |
|---|---|
| Storia `0004` | L'assegnazione occupa un posto: senza il conteggio non si può rispondere `429` |
| Storia `0007` | Servono le attività |

## 7. Fuori ambito

- l'assegnazione a più persone contemporaneamente: una sola responsabile, per scelta; chi collabora dichiara le
  proprie ore comunque (storia 0017);
- la pianificazione di capacità e il livellamento: fuori perimetro dell'app (§1 della descrizione);
- le notifiche di assegnazione fuori dall'app: storia 0016 le tiene dentro.

## 8. Punti aperti

- **Chi vede le assegnazioni di chi.** Qui tutti i membri vedono tutto, come in quasi tutti gli strumenti della
  categoria per squadre piccole. In un'azienda di venti persone questa scelta potrebbe non reggere. Se dovesse
  nascere una visibilità ristretta, è una decisione di prodotto con impatti sulla privacy: va presa dallo
  sviluppatore, non allargata di nascosto in una storia successiva.
