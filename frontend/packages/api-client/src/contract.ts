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
