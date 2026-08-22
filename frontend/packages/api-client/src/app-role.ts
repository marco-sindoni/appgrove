// Ruolo della persona su una APPLICAZIONE (UC 0098/0099) e suo confronto, lato browser (UC 0101).
//
// Vive qui e non nel design system perché è parte del CONTRATTO di piattaforma, non della
// presentazione: il design system deve poter restare ignaro di che cosa sia un ruolo. Il gemello lato
// servizio è `app.appgrove.commons.access.AppRole` (Java), dove l'ordinamento è dichiarato una volta e
// nessuna applicazione lo riscrive. Qui l'ordinamento è dichiarato una seconda volta perché il browser
// non può leggere l'enumerazione Java — e una sola volta per lato, mai una terza.

/** I tre ruoli di applicazione, ordinati dal meno al più capace. L'ordine è la dichiarazione. */
export const APP_ROLES = ['viewer', 'editor', 'admin'] as const

export type AppRole = (typeof APP_ROLES)[number]

/**
 * Il ruolo posseduto è almeno tanto capace di quello richiesto?
 *
 * `role` accetta `null`/`undefined` per il caso «non lo sappiamo ancora» (il ruolo si legge dalla rete)
 * e per «nessun accesso all'applicazione»: in entrambi i casi la risposta è **no**. È deliberato e non
 * difensivo — in assenza di informazione non si concede, e un comando abilitato «in attesa» sarebbe una
 * promessa che il servizio poi smentisce con un rifiuto (UC 0101 §6).
 *
 * **L'owner dell'account non è un quarto valore**: la fonte di verità gli attribuisce `admin` su tutte le
 * applicazioni dell'account (UC 0098 §5), quindi passa ogni confronto senza che questa funzione sappia
 * che esiste. Un valore non riconosciuto non è un permesso.
 */
export function appRoleAtLeast(role: string | null | undefined, required: AppRole): boolean {
  if (!role) return false
  const held = APP_ROLES.indexOf(role as AppRole)
  if (held < 0) return false
  return held >= APP_ROLES.indexOf(required)
}
