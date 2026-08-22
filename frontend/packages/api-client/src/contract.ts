// Drift guard del contratto OpenAPI (#10 G25).
// Questi alias riferiscono path/tipi precisi dello spec del core. Se lo spec cambia in modo
// incompatibile, la rigenerazione di `schema.ts` (script `gen`) rimuove o cambia questi tipi e
// **`tsc` rompe la build**: è il segnale per aggiornare il client di pari passo allo spec.

import type { components, paths } from './schema'

export type UserView =
  paths['/api/platform/v1/users/me']['get']['responses']['200']['content']['application/json']

/**
 * Una applicazione su cui una persona dell'account è abilitata, col ruolo che vi ha (UC 0100). Serve
 * all'elenco unico della schermata «Members»: il numero in colonna e il dettaglio a richiesta.
 */
export type UserAppView = components['schemas']['UserAppView']

export type AccountView =
  paths['/api/platform/v1/accounts/me']['get']['responses']['200']['content']['application/json']

/** Appartenenze della persona in sessione e account attivo (UC 0117). */
export type MyMembershipsView =
  paths['/api/platform/v1/me/memberships']['get']['responses']['200']['content']['application/json']

// NB: l'OpenAPI del core per `POST /invitations` non documenta il body di risposta (200 senza content),
// benché il servizio ritorni `InvitationView` col token grezzo → usiamo il componente schema, non il path.
// Gap backend tracciato in UC 0013 (response body + status 201 da annotare).
export type InvitationView = components['schemas']['InvitationView']

/**
 * Il riquadro dei posti dell'account (UC 0103): posti usati e loro composizione, dovuto attuale, fascia
 * applicata e **costo del posto successivo**.
 *
 * Ogni numero arriva dal servizio: l'interfaccia non somma scaglioni, non sottrae la franchigia e non
 * decide se la tariffa scende. È la sola forma in cui cinque traduzioni e un servizio dicono lo stesso
 * importo — e su un importo che il cliente confronta con la fattura non c'è margine per due versioni.
 */
export type SeatSummaryView =
  paths['/api/platform/v1/me/seats']['get']['responses']['200']['content']['application/json']

/**
 * La **riduzione dei posti in attesa** (UC 0104): data di esecuzione, persone indicate, posti e dovuto
 * dopo, e la composizione degli scaglioni che si applicherà.
 *
 * Arriva **dentro** il riquadro dei posti e non da una lettura a sé, così che l'avviso si possa disegnare
 * senza una seconda chiamata. Il tipo è esportato comunque, perché la schermata lo maneggia come una cosa
 * propria: chi legge il codice deve poter dire «questa è la riduzione», non «questo è un pezzo del
 * riquadro».
 */
export type SeatReductionView = components['schemas']['ReductionView']

/**
 * L'**effetto prima della conferma** (UC 0104 §4.2): che cosa cambierebbe indicando quelle persone.
 *
 * Dipende da chi si è appena selezionato, quindi è l'unica lettura della sezione che cambia a ogni casella
 * spuntata — ed è una lettura, non un atto: chiederla non programma nulla.
 */
export type SeatReductionPreview = components['schemas']['ReductionPreview']
