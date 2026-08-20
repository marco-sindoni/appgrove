import { useNavigate } from 'react-router-dom'
import { Badge, Button, Card, Icon, cn } from '@appgrove/design-system'
import { useTranslation, type Language } from '@appgrove/i18n'
import { useCurrentAccount, useInvitations, useMembers } from '../../api/hooks'
import { useAuthStore } from '../../auth/authStore'
import { useTwoFaStatus } from '../../auth/twoFaApi'
import { useMySubscriptions } from '../../billing/subscriptionsApi'
import { describeApp, tintFor, useAppCatalog, type CatalogApp } from '../../catalog/catalogApi'
import { findModule } from '../../registry/registry'
import {
  activeApps,
  buildAlerts,
  nextRenewal,
  quotaBar,
  type DashboardAlert,
} from './dashboardModel'
import { useAppQuota } from './quotaApi'
import { PendingInvitesSection } from './PendingInvitesSection'

/* Tinte di categoria: classi statiche, altrimenti Tailwind le rimuove non trovandole nel sorgente. */
const TILE_TINT: Record<string, string> = {
  green: 'bg-cat-green/15 text-cat-green',
  amber: 'bg-cat-amber/15 text-cat-amber',
  red: 'bg-cat-red/15 text-cat-red',
  blue: 'bg-cat-blue/15 text-cat-blue',
  violet: 'bg-cat-violet/15 text-cat-violet',
  teal: 'bg-cat-teal/15 text-cat-teal',
}

const ALERT_TINT: Record<DashboardAlert['tone'], string> = {
  danger: 'bg-danger/10 text-danger',
  warning: 'bg-warning/12 text-warning',
  info: 'bg-accent/10 text-accent',
}

const ALERT_ICON: Record<DashboardAlert['kind'], string> = {
  payment: 'warning',
  twofa: 'shield',
}

// Chiavi composte a runtime (etichetta dell'unità di quota, dichiarata dal manifest del modulo):
// opache alla union tipizzata di `t`, come già in billing/PaymentsPanel.
type TKey = never

/**
 * **Dashboard operativa** del workspace (UC 0097). Fino alla change 0078 questa pagina mostrava una
 * cosa sola — l'identificativo tecnico del workspace — cioè l'unico dato che l'utente non usa mai:
 * quel codice è ora in **Account**, dove serve a chi apre un ticket, e la pagina d'atterraggio dice
 * invece che cosa sta succedendo.
 *
 * <p>Nessun read-model nuovo: la pagina **compone** letture che esistono già (vetrina del catalogo,
 * abbonamenti, membri e inviti, stato legale, quota dei servizi delle app). Ne discende la regola che
 * governa tutta la pagina: **ogni sezione possiede i propri stati**, e il guasto di una fonte degrada
 * la sua sola sezione o card. Una pagina d'atterraggio tutta rossa perché una lettura secondaria non
 * risponde sarebbe il difetto peggiore di tutti.
 */
export function DashboardPage() {
  const { t, i18n } = useTranslation()
  const claims = useAuthStore((s) => s.claims)
  const canManage = useAuthStore((s) => !!s.claims?.roles?.some((r) => r === 'owner' || r === 'admin'))

  const account = useCurrentAccount()
  const catalog = useAppCatalog()
  const subscriptions = useMySubscriptions()
  const members = useMembers()
  const invitations = useInvitations()
  const twoFa = useTwoFaStatus()

  const language = i18n.language as Language
  const apps = activeApps(catalog.data?.apps)
  const alerts = buildAlerts({ apps: catalog.data?.apps, twoFaEnabled: twoFa.data })
  const renewal = nextRenewal(subscriptions.data?.subscriptions)
  const workspace = account.data?.name ?? ''

  return (
    <div className="space-y-[22px]">
      <header className="space-y-1">
        <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">
          {claims?.name
            ? t('dashboard.greetingNamed', { name: claims.name })
            : t('dashboard.greeting')}
        </h1>
        {workspace && (
          <p className="text-sm text-fg-muted">{t('dashboard.subtitle', { workspace })}</p>
        )}
      </header>

      {/* Inviti ricevuti (UC 0118): in TESTA, prima degli avvisi e delle applicazioni. Un invito a
          collaborare con un'altra azienda chiede una decisione, e una decisione non si mette in fondo. */}
      <PendingInvitesSection />

      {alerts.length > 0 && (
        <div className="flex flex-col gap-2.5" aria-label={t('dashboard.alertsLabel')}>
          {alerts.map((alert) => (
            <AlertStrip key={alert.kind} alert={alert} />
          ))}
        </div>
      )}

      <div className="grid gap-[18px] lg:grid-cols-[1fr_320px] lg:items-start">
        <YourApps
          apps={apps}
          language={language}
          canManage={canManage}
          isLoading={catalog.isLoading}
          isError={catalog.isError}
          onRetry={() => void catalog.refetch()}
        />

        <aside className="flex flex-col gap-3" aria-label={t('dashboard.glanceLabel')}>
          <Card className="px-5 py-4">
            <h2 className="pb-1.5 text-[13px] font-bold uppercase tracking-[0.06em] text-fg-muted">
              {t('dashboard.glance')}
            </h2>
            {/* Membri e inviti sono letture riservate a owner/admin: a un member non si mostra una
                riga rotta, si omette la riga. */}
            {canManage && (
              <>
                <GlanceRow
                  label={t('dashboard.glanceMembers')}
                  value={members.data?.totalElements}
                  isError={members.isError}
                />
                <GlanceRow
                  label={t('dashboard.glanceInvites')}
                  value={invitations.data?.totalElements}
                  isError={invitations.isError}
                />
              </>
            )}
            <GlanceRow
              label={t('dashboard.glanceApps')}
              value={catalog.isError ? undefined : apps.length}
              isError={catalog.isError}
            />
            <GlanceRow
              label={t('dashboard.glanceRenewal')}
              text={
                subscriptions.isError
                  ? undefined
                  : renewal
                    ? new Intl.DateTimeFormat(i18n.language, { dateStyle: 'medium' }).format(
                        new Date(renewal),
                      )
                    : t('dashboard.glanceNoRenewal')
              }
              isError={subscriptions.isError}
            />
          </Card>

          <div className="flex flex-col gap-2">
            {canManage && (
              <Shortcut icon="group" to="/members" label={t('dashboard.shortcutInvite')} />
            )}
            <Shortcut icon="credit_card" to="/billing" label={t('dashboard.shortcutPayments')} />
            <Shortcut icon="storefront" to="/catalog" label={t('dashboard.shortcutCatalog')} />
          </div>
        </aside>
      </div>
      {/* L'identificativo tecnico del workspace non vive più qui: è in Account, con il pulsante di
          copia, perché serve solo quando si apre un ticket di assistenza (UC 0097 §4.6). */}
    </div>
  )
}

function AlertStrip({ alert }: { alert: DashboardAlert }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const isPayment = alert.kind === 'payment'

  return (
    <div
      role="alert"
      className={cn(
        'flex flex-wrap items-center gap-3 rounded-[15px] px-[18px] py-3 text-[13.5px] font-semibold',
        ALERT_TINT[alert.tone],
      )}
    >
      <Icon name={ALERT_ICON[alert.kind]} size={18} />
      <span className="flex-1">
        {isPayment ? t('dashboard.alertPayment', { app: alert.appName ?? '' }) : t('dashboard.alertTwoFa')}
      </span>
      <Button variant="ghost" size="sm" onClick={() => navigate(isPayment ? '/billing' : '/security')}>
        {isPayment ? t('dashboard.alertPaymentCta') : t('dashboard.alertTwoFaCta')}
      </Button>
    </div>
  )
}

function YourApps({
  apps,
  language,
  canManage,
  isLoading,
  isError,
  onRetry,
}: {
  apps: CatalogApp[]
  language: Language
  canManage: boolean
  isLoading: boolean
  isError: boolean
  onRetry: () => void
}) {
  const { t } = useTranslation()

  return (
    <section className="flex flex-col gap-3" aria-label={t('dashboard.yourApps')}>
      <div className="space-y-0.5">
        <h2 className="text-[13px] font-bold uppercase tracking-[0.06em] text-fg-muted">
          {t('dashboard.yourApps')}
        </h2>
        <p className="text-sm text-fg-muted">{t('dashboard.yourAppsHint')}</p>
      </div>

      {isLoading && (
        <p role="status" className="text-sm text-fg-muted">
          {t('states.loading')}
        </p>
      )}

      {isError && (
        <div role="alert" className="space-y-3">
          <p className="text-sm text-danger">{t('dashboard.appsError')}</p>
          <Button variant="secondary" size="sm" onClick={onRetry}>
            {t('states.retry')}
          </Button>
        </div>
      )}

      {!isLoading && !isError && (
        <div className="grid gap-3.5 sm:grid-cols-2">
          {apps.map((app) => (
            <AppSummaryCard key={app.appSlug} app={app} language={language} canManage={canManage} />
          ))}
          <GetMoreApps standalone={apps.length === 0} />
        </div>
      )}
    </section>
  )
}

function AppSummaryCard({
  app,
  language,
  canManage,
}: {
  app: CatalogApp
  language: Language
  canManage: boolean
}) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const slug = app.appSlug ?? ''
  const name = app.name ?? slug
  const tint = tintFor(app)
  const module = findModule(slug)
  const quota = useAppQuota(slug, module?.quota)
  const bar = quota.data ? quotaBar(quota.data.used ?? 0, quota.data.limit) : null

  return (
    <Card className="flex flex-col gap-3 p-[18px]" role="article" aria-label={name}>
      <div className="flex items-center gap-3">
        <span
          aria-hidden="true"
          className={cn(
            'grid size-10 shrink-0 place-items-center rounded-xl text-base font-extrabold',
            TILE_TINT[tint],
          )}
        >
          {name.slice(0, 1).toUpperCase()}
        </span>
        <span className="min-w-0 flex-1 truncate text-[15px] font-bold text-fg">{name}</span>
        <Badge tone={app.state === 'trial' ? 'info' : 'success'}>
          {t(app.state === 'trial' ? 'catalog.state.trial' : 'catalog.state.active')}
        </Badge>
      </div>

      {/* La barra c'è solo quando il consumo si è potuto leggere davvero: un'app senza descrittore di
          quota, una lettura non concessa al ruolo o un servizio che non risponde lasciano la card
          senza barra — e con la sua descrizione, che resta utile. */}
      {bar ? (
        <div className="flex flex-col gap-1.5 text-xs text-fg-muted">
          <span>
            {bar.unlimited
              ? t('dashboard.quotaUnlimited', {
                  used: bar.used,
                  unit: t((module?.quota?.unitLabel ?? '') as TKey),
                })
              : t('dashboard.quotaUsage', {
                  used: bar.used,
                  limit: bar.limit,
                  unit: t((module?.quota?.unitLabel ?? '') as TKey),
                })}
          </span>
          {!bar.unlimited && (
            <span
              role="progressbar"
              aria-label={t('dashboard.quotaBarLabel', { app: name })}
              aria-valuenow={bar.percent}
              aria-valuemin={0}
              aria-valuemax={100}
              className="block h-[7px] overflow-hidden rounded-pill bg-surface-3"
            >
              <span
                className={cn(
                  'block h-full rounded-pill',
                  bar.warning ? 'bg-warning' : 'bg-accent',
                )}
                style={{ width: `${bar.percent}%` }}
              />
            </span>
          )}
        </div>
      ) : (
        <p className="flex-1 text-xs text-fg-muted">{describeApp(app, language)}</p>
      )}

      <div className="mt-auto flex gap-2">
        <Button size="sm" onClick={() => navigate(`/app/${slug}`)}>
          {t('dashboard.open')}
        </Button>
        {/* Il cambio piano è di chi può cambiarlo: a un member non si offre un'azione che il backend
            gli rifiuterebbe (UC 0097 §5). */}
        {canManage && (
          <Button variant="ghost" size="sm" onClick={() => navigate('/billing')}>
            {t('dashboard.managePlan')}
          </Button>
        )}
      </div>
    </Card>
  )
}

/** Invito al catalogo: ultima cella della griglia, oppure — a workspace vuoto — la sezione intera. */
function GetMoreApps({ standalone }: { standalone: boolean }) {
  const { t } = useTranslation()
  const navigate = useNavigate()

  return (
    <button
      type="button"
      onClick={() => navigate('/catalog')}
      className={cn(
        'flex flex-col items-center justify-center gap-2 rounded-[15px] border-[1.5px] border-dashed border-line-strong bg-surface-2 px-5 py-[22px] text-center text-fg-muted transition hover:bg-surface-3',
        standalone && 'sm:col-span-2',
      )}
    >
      <Icon name="add" size={26} className="text-accent" />
      <b className="text-sm text-fg">
        {standalone ? t('dashboard.noAppsTitle') : t('dashboard.getMoreApps')}
      </b>
      <span className="text-[12.5px]">
        {standalone ? t('dashboard.noAppsHint') : t('dashboard.getMoreAppsHint')}
      </span>
    </button>
  )
}

function GlanceRow({
  label,
  value,
  text,
  isError,
}: {
  label: string
  value?: number
  text?: string
  isError?: boolean
}) {
  const { t } = useTranslation()
  // Una riga che non si è potuta leggere lo dice con un trattino, non con un numero sbagliato né con
  // un errore che occuperebbe tutta la colonna.
  const shown = isError ? '—' : (text ?? (value == null ? '…' : String(value)))

  return (
    <div className="flex items-center justify-between border-b border-line py-[11px] text-[13px] last:border-b-0">
      <span className="font-semibold text-fg-muted">{label}</span>
      <span className="font-bold text-fg" title={isError ? t('states.error') : undefined}>
        {shown}
      </span>
    </div>
  )
}

function Shortcut({ icon, to, label }: { icon: string; to: string; label: string }) {
  const navigate = useNavigate()
  return (
    <button
      type="button"
      onClick={() => navigate(to)}
      className="flex w-full items-center gap-2.5 rounded-md border border-line bg-surface px-3 py-2.5 text-left text-[13px] font-semibold text-fg transition hover:bg-surface-2"
    >
      <Icon name={icon} size={16} className="text-accent" />
      {label}
    </button>
  )
}

export default DashboardPage
