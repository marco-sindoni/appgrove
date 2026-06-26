// Drift guard del contratto OpenAPI (#10 G25).
// Questi alias riferiscono path/tipi precisi dello spec del core. Se lo spec cambia in modo
// incompatibile, la rigenerazione di `schema.ts` (script `gen`) rimuove o cambia questi tipi e
// **`tsc` rompe la build**: è il segnale per aggiornare il client di pari passo allo spec.

import type { paths } from './schema'

export type UserView =
  paths['/api/platform/v1/users/me']['get']['responses']['200']['content']['application/json']

export type AccountView =
  paths['/api/platform/v1/accounts/me']['get']['responses']['200']['content']['application/json']
