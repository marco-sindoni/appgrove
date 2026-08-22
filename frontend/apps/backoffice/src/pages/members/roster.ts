import type { InvitationView, UserAppView, UserView } from '@appgrove/api-client'

/**
 * Le persone dell'account e gli inviti in attesa fusi in **un solo elenco** (UC 0100).
 *
 * Prima di questa storia la schermata «Members» teneva due tabelle: i membri e gli inviti in attesa.
 * Erano due elenchi della stessa cosa — le persone del gruppo di lavoro — e chi guardava doveva sommare
 * a mente per sapere quante ne aveva. Una persona con l'invito in attesa **occupa già un posto**: sta
 * arrivando, non è un'altra categoria di essere.
 *
 * La fusione vive qui, fuori dal componente, perché è la parte con le regole (ordinamento, stati, righe
 * intoccabili) e le regole si provano meglio da sole che attraverso un rendering.
 */

/**
 * Stato di una riga dell'elenco. **Quattro** valori da UC 0104: la cessazione programmata è il quarto, che
 * la storia 0100 aveva elencato e la change 0096 aveva lasciato fuori perché nessun dato poteva produrlo.
 *
 * `ending` **non sostituisce** `suspended`: le due cose sono ortogonali (una riguarda il posto, l'altra
 * l'accesso) e una persona può essere entrambe. Per questo lo stato porta il valore più informativo e la
 * data vive in `endingAt`, che si legge accanto — non invece.
 */
export type RosterStatus = 'active' | 'suspended' | 'invited' | 'ending'

export interface RosterRow {
  /** Chiave stabile per il rendering: il tipo di riga più l'identificativo. */
  key: string
  /** `person` = appartenenza viva (attiva o sospesa) · `invitation` = invito in attesa. */
  kind: 'person' | 'invitation'
  /** Identificativo della persona (per una riga `person`) o dell'invito (per una riga `invitation`). */
  id: string
  email: string
  /** Nome visualizzato, assente per un invito: chi non è ancora entrato non ha ancora scelto un nome. */
  displayName?: string
  status: RosterStatus
  /** Applicazioni su cui la persona è abilitata; vuoto per un invito e per chi non è abilitato a nulla. */
  apps: UserAppView[]
  /** Data di ingresso nel gruppo di lavoro; assente per un invito, che non è ancora un ingresso. */
  joinedAt?: string
  /** Scadenza dell'invito: l'informazione utile su una riga che non è ancora una persona. */
  expiresAt?: string
  /**
   * Data di **cessazione programmata** (UC 0104), assente per chi non è indicato. «In cessazione» senza il
   * quando non dice nulla: la data non è un dettaglio dell'etichetta, è l'etichetta.
   */
  endingAt?: string
  /** Vero quando la persona è **sospesa**, anche se è al contempo in cessazione (i due stati convivono). */
  suspended: boolean
  /**
   * La riga si può **indicare per la cessazione** (UC 0104): solo le persone, non l'owner, non gli inviti
   * in attesa (che si revocano, non si cessano) e non chi è già indicato. È il gating a schermo; il
   * rifiuto vero arriva dal servizio.
   */
  selectable: boolean
  /** L'owner dell'account: va in testa e non si tocca. */
  isOwner: boolean
  /** Vero quando la riga è quella di chi sta guardando. */
  isSelf: boolean
  /**
   * Vero quando le azioni distruttive vanno disabilitate: su di sé, e sull'**ultimo owner** (che non si
   * sospende e non si rimuove — il rifiuto vero arriva dal servizio, questo è la cortesia).
   */
  locked: boolean
}

export interface BuildRosterInput {
  members?: UserView[]
  invitations?: InvitationView[]
  /** Identificativo della persona in sessione, per riconoscere la propria riga. */
  meId?: string
}

const byEmail = (a: RosterRow, b: RosterRow) => a.email.localeCompare(b.email)

/**
 * Costruisce l'elenco unico. Ordine: **l'owner in testa**, poi le persone per indirizzo, poi gli inviti
 * in attesa per indirizzo.
 *
 * Perché non un unico ordinamento per indirizzo: l'elenco è uno, ma «questa riga è una persona che c'è»
 * e «questa riga è una persona che sta arrivando» è la prima distinzione che si cerca guardandolo, e
 * mescolarle costringerebbe a leggere la colonna dello stato riga per riga.
 */
export function buildRoster({ members = [], invitations = [], meId }: BuildRosterInput): RosterRow[] {
  const owners = members.filter((m) => m.role === 'owner').length

  const people: RosterRow[] = members.map((m) => {
    const isOwner = m.role === 'owner'
    const isSelf = !!meId && m.id === meId
    const suspended = m.status === 'suspended'
    const ending = !!m.endingAt
    return {
      key: `person:${m.id}`,
      kind: 'person',
      id: m.id as string,
      email: m.email ?? '',
      displayName: m.displayName,
      // La cessazione programmata vince sull'etichetta perché è l'informazione che scade: chi guarda
      // deve accorgersene senza cercarla. La sospensione non si perde — resta in `suspended`, e la
      // schermata mostra le due cose insieme quando convivono.
      status: ending ? 'ending' : suspended ? 'suspended' : 'active',
      apps: m.apps ?? [],
      joinedAt: m.joinedAt,
      endingAt: m.endingAt,
      suspended,
      selectable: !isOwner && !ending,
      isOwner,
      isSelf,
      locked: isSelf || (isOwner && owners <= 1),
    }
  })

  const pending: RosterRow[] = invitations.map((i) => ({
    key: `invitation:${i.id}`,
    kind: 'invitation',
    id: i.id as string,
    email: i.email ?? '',
    status: 'invited',
    apps: [],
    expiresAt: i.expiresAt,
    suspended: false,
    // Un invito in attesa non si «cessa»: si revoca, ed è un'operazione immediata e gratuita.
    selectable: false,
    isOwner: false,
    isSelf: false,
    locked: false,
  }))

  const ownerRows = people.filter((r) => r.isOwner).sort(byEmail)
  const otherRows = people.filter((r) => !r.isOwner).sort(byEmail)
  return [...ownerRows, ...otherRows, ...pending.sort(byEmail)]
}
