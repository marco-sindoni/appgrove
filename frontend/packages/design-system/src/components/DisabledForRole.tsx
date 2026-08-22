import { cloneElement, useId, type ReactElement, type ReactNode } from 'react'
import { cn } from '../lib/cn'

export type DisabledForRoleProps = {
  /**
   * Il ruolo di chi guarda basta per questo comando? Il confronto NON si fa qui: si fa con
   * `appRoleAtLeast` di `@appgrove/api-client`, dove l'ordinamento dei ruoli è dichiarato una volta.
   * Questo involucro riceve l'esito, così il design system resta ignaro di che cosa sia un ruolo.
   */
  allowed: boolean
  /**
   * La spiegazione, **già tradotta** dal chiamante (chiave `roles.needsRole` di `@appgrove/i18n`). Deve
   * dire due cose: quale ruolo serve e a chi chiederlo. «Non hai i permessi» non è una spiegazione.
   */
  reason: string
  /** Il comando: un solo elemento che accetti `disabled` (bottone, voce di menu, campo). */
  children: ReactElement<{
    disabled?: boolean
    'aria-disabled'?: boolean
    'aria-describedby'?: string
    tabIndex?: number
  }>
  className?: string
}

/**
 * Come l'interfaccia dice «non puoi» quando manca il ruolo (UC 0101 §6): il comando resta **presente ma
 * disabilitato**, con la spiegazione al passaggio del puntatore e leggibile dagli strumenti di assistenza.
 *
 * <p>**Perché disabilitato e non assente.** Nascondere un comando fa credere che la funzione non esista:
 * chi lo cerca pensa che il prodotto non lo faccia, e nessuno chiede l'abilitazione a nessuno. Un comando
 * disabilitato che spiega *quale ruolo serve* e *a chi chiederlo* è una via d'uscita; un comando sparito è
 * un vicolo cieco. La regola opposta vale per gli **ambiti che non competono al ruolo** — fatturazione,
 * gestione delle persone dell'account: quelli sono **assenti** dalla navigazione, ed è di UC 0107.
 *
 * <p>**Perché un involucro condiviso e non tre righe per schermata.** Perché l'accessibilità di «questo
 * comando è disabilitato, e per questo motivo» non è una riga: serve `aria-disabled`, un testo collegato
 * con `aria-describedby` e il comando fuori dal percorso di tabulazione. Ripetuto in ogni modulo, si
 * perderebbe in tre punti su quattro — e chi usa un lettore di schermo sentirebbe solo «bottone,
 * disabilitato», che non dice niente.
 *
 * <p>Quando il ruolo **basta**, l'involucro non aggiunge nulla: rende il comando così com'è, senza
 * contenitori in più che sporcherebbero la disposizione.
 *
 * <p>**Stato di caricamento**: finché il ruolo non è noto, si passa `allowed={false}` — mai abilitato «in
 * attesa» (UC 0101 §6). Un comando abilitato per un istante è una promessa che il servizio smentisce con
 * un rifiuto.
 */
export function DisabledForRole({
  allowed,
  reason,
  children,
  className,
}: DisabledForRoleProps): ReactNode {
  const explanationId = useId()

  if (allowed) return children

  return (
    <span className={cn('inline-flex', className)} title={reason}>
      {cloneElement(children, {
        disabled: true,
        'aria-disabled': true,
        'aria-describedby': explanationId,
        // Fuori dal percorso di tabulazione: un comando che non fa niente non deve rubare il fuoco.
        tabIndex: -1,
      })}
      <span id={explanationId} className="sr-only">
        {reason}
      </span>
    </span>
  )
}
