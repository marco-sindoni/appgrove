import { useState } from 'react'
import { Button, Card, Icon } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useAcceptMyInvitation, useMyInvitations, useRejectMyInvitation } from '../../api/hooks'

/**
 * **Inviti ricevuti** dalla persona in sessione (UC 0118), in testa al cruscotto.
 *
 * Perché qui e non un pulsante nell'intestazione — che era la prima ipotesi, scartata: un invito a
 * collaborare con un'altra azienda merita una **decisione consapevole**, e un invito non risposto non
 * è un dettaglio, è un rapporto di lavoro in sospeso. Un pulsantino nell'intestazione passa
 * inosservato. Perché resti visibile anche da un'altra schermata, la voce «Dashboard» del menu porta
 * il **numero** degli inviti in attesa (`Sidebar`).
 *
 * Accettare crea una **appartenenza** in più e non una seconda identità: nessuna parola d'accesso da
 * scegliere, nessun nome da ridare. Il nome dell'azienda che invita non è un ornamento — è ciò che
 * rende consapevole il consenso.
 *
 * Dopo l'accettazione l'applicazione **ricarica**, come per il cambio di account (UC 0117): l'account
 * appena accettato è quello attivo, ed è il ricaricamento a far nascere il token con il claim nuovo.
 * Mezza applicazione con l'account nuovo e mezza col vecchio è il modo peggiore di sbagliare.
 *
 * Non ha stati di caricamento né di errore propri quando non c'è nulla da mostrare: se la lettura non
 * riesce, la sezione **non compare** — un guasto di rete non deve annunciare inviti che non sappiamo
 * se esistono, né occupare la testa del cruscotto con un errore su una lettura secondaria.
 */
export function PendingInvitesSection() {
  const { t } = useTranslation()
  const { data } = useMyInvitations()
  const accept = useAcceptMyInvitation()
  const reject = useRejectMyInvitation()
  const [failed, setFailed] = useState(false)

  const invites = (data?.invitations ?? []).flatMap((i) =>
    i.id ? [{ id: i.id, accountName: i.accountName ?? i.accountId ?? '' }] : [],
  )
  if (invites.length === 0) {
    return null
  }

  const busy = accept.isPending || reject.isPending

  const onAccept = async (id: string) => {
    setFailed(false)
    try {
      await accept.mutateAsync(id)
      // Ricaricamento sulla radice: l'account accettato è ora quello attivo, e la rotta aperta può
      // non esistere lì.
      window.location.assign('/')
    } catch {
      setFailed(true)
    }
  }

  const onReject = async (id: string) => {
    setFailed(false)
    try {
      await reject.mutateAsync(id)
    } catch {
      setFailed(true)
    }
  }

  return (
    <section className="flex flex-col gap-3" aria-label={t('myInvites.title')}>
      <h2 className="text-[13px] font-bold uppercase tracking-[0.06em] text-fg-muted">
        {t('myInvites.title')}
      </h2>
      {invites.map((invite) => (
        <Card key={invite.id} className="flex flex-wrap items-center gap-3 p-[18px]">
          <Icon name="group_add" size={22} className="shrink-0 text-accent" />
          <div className="min-w-[12rem] flex-1">
            <p className="text-[14.5px] font-bold text-fg">
              {t('myInvites.body', { account: invite.accountName })}
            </p>
            {/* Il posto è dell'account che invita: la prima reazione di chi legge è «e chi lo paga?»
                (UC 0118 §7). Dirlo qui costa una riga e risparmia un ticket. */}
            <p className="mt-0.5 text-[12.5px] text-fg-muted">{t('myInvites.seatNote')}</p>
          </div>
          <div className="flex gap-2">
            <Button size="sm" disabled={busy} onClick={() => void onAccept(invite.id)}>
              {accept.isPending ? t('myInvites.accepting') : t('myInvites.accept')}
            </Button>
            <Button variant="secondary" size="sm" disabled={busy} onClick={() => void onReject(invite.id)}>
              {t('myInvites.reject')}
            </Button>
          </div>
        </Card>
      ))}
      {failed && (
        <p role="alert" className="text-sm text-danger">
          {t('myInvites.error')}
        </p>
      )}
    </section>
  )
}
