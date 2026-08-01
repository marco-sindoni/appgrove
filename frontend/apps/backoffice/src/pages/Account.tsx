import { useState } from 'react'
import { Button, Card, CardContent, CardHeader, CardTitle, Icon } from '@appgrove/design-system'
import { useTranslation } from '@appgrove/i18n'
import { useCurrentAccount, useCurrentUser } from '../api/hooks'
import { useAuthStore } from '../auth/authStore'
import { QueryState } from '../shell/QueryState'

/**
 * Pagina **Account**: il profilo dell'utente (`GET /users/me`) e i dati del workspace, compreso il suo
 * **identificativo tecnico** — che fino alla change 0078 stava parcheggiato in Dashboard, dove era
 * l'unica cosa mostrata e non serviva a nulla. Qui invece è al suo posto, con il pulsante di copia:
 * è il codice da incollare quando si apre una richiesta di assistenza (UC 0097 §4.6).
 */
export function Account() {
  const { t } = useTranslation()
  const query = useCurrentUser()
  const account = useCurrentAccount()
  // Il workspace del token verificato: la stessa fonte che la shell usa per ogni chiamata. Non si
  // legge da un parametro né da un campo modificabile (invariante #1).
  const tenantId = useAuthStore((s) => s.claims?.tenantId ?? '')

  return (
    <div className="space-y-[22px]">
      <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">{t('nav.account')}</h1>

      <Card>
        <CardHeader>
          <CardTitle>{t('account.profileTitle')}</CardTitle>
        </CardHeader>
        <CardContent>
          <QueryState
            isLoading={query.isLoading}
            isError={query.isError}
            onRetry={() => void query.refetch()}
          >
            <dl className="grid grid-cols-[8rem_1fr] gap-2 text-sm">
              <dt className="text-fg-muted">Email</dt>
              <dd className="font-mono">{query.data?.email}</dd>
              <dt className="text-fg-muted">{t('settings.displayName')}</dt>
              <dd>{query.data?.displayName}</dd>
              <dt className="text-fg-muted">Role</dt>
              <dd>{query.data?.role}</dd>
            </dl>
          </QueryState>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>{t('account.workspaceTitle')}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-1">
            <p className="text-sm text-fg-muted">{t('account.workspaceName')}</p>
            {/* Il nome è modificabile in Impostazioni: qui si legge soltanto, per non avere due
                posti dove cambiarlo. */}
            <p className="font-semibold text-fg">{account.data?.name ?? '—'}</p>
          </div>
          <WorkspaceId tenantId={tenantId} />
        </CardContent>
      </Card>
    </div>
  )
}

/** Identificativo del workspace in carattere a larghezza fissa, con copia e conferma visibile. */
function WorkspaceId({ tenantId }: { tenantId: string }) {
  const { t } = useTranslation()
  const [copied, setCopied] = useState(false)

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(tenantId)
      setCopied(true)
      // La conferma svanisce da sola: un segno di spunta permanente farebbe credere che ci sia
      // qualcosa da annullare.
      window.setTimeout(() => setCopied(false), 2000)
    } catch {
      // Nessun accesso agli appunti (permesso negato, contesto non sicuro): il codice resta
      // selezionabile a mano, che è esattamente ciò che si faceva prima del pulsante.
    }
  }

  return (
    <div className="space-y-1">
      <p className="text-sm text-fg-muted">{t('account.workspaceId')}</p>
      <div className="flex flex-wrap items-center gap-3">
        <code className="select-all break-all rounded-md bg-surface-2 px-2.5 py-1.5 font-mono text-[13px] text-fg">
          {tenantId}
        </code>
        <Button variant="ghost" size="sm" aria-label={t('account.copyIdLabel')} onClick={() => void copy()}>
          <Icon name={copied ? 'check' : 'content_copy'} size={16} className="mr-1.5" />
          {copied ? t('account.copied') : t('account.copyId')}
        </Button>
      </div>
      <p className="text-xs text-fg-muted">{t('account.workspaceIdHint')}</p>
    </div>
  )
}
