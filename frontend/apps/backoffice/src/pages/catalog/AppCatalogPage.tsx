import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Badge, Button, Card, EmptyIllustration, Icon, Input, cn } from '@appgrove/design-system'
import { useTranslation, type Language } from '@appgrove/i18n'
import { useAuthStore } from '../../auth/authStore'
import { CheckoutFlow } from '../../billing/CheckoutFlow'
import { formatPrice } from '../../billing/checkoutMachine'
import {
  describeApp,
  filterApps,
  paginate,
  tintFor,
  useAppCatalog,
  type CatalogApp,
  type CatalogAppState,
} from '../../catalog/catalogApi'
import { QueryState } from '../../shell/QueryState'

/* Tinte di categoria: classi statiche, altrimenti Tailwind le rimuove non trovandole nel sorgente. */
const TILE_TINT: Record<string, string> = {
  green: 'bg-cat-green/15 text-cat-green',
  amber: 'bg-cat-amber/15 text-cat-amber',
  red: 'bg-cat-red/15 text-cat-red',
  blue: 'bg-cat-blue/15 text-cat-blue',
  violet: 'bg-cat-violet/15 text-cat-violet',
  teal: 'bg-cat-teal/15 text-cat-teal',
}
const HEADER_TINT: Record<string, string> = {
  green: 'bg-cat-green/10',
  amber: 'bg-cat-amber/10',
  red: 'bg-cat-red/10',
  blue: 'bg-cat-blue/10',
  violet: 'bg-cat-violet/10',
  teal: 'bg-cat-teal/10',
}

/** Tono del badge di stato: il colore dice quanto la cosa è seria, non solo che cos'è. */
const STATE_TONE: Record<Exclude<CatalogAppState, 'available'>, 'success' | 'info' | 'warning' | 'danger'> =
  {
    active: 'success',
    trial: 'info',
    payment_pending: 'warning',
    cancellation_scheduled: 'warning',
    disabled_by_platform: 'danger',
  }

/**
 * Vetrina del catalogo (UC 0095): qui si **scoprono e si attivano** le app: in Billing resta ciò che
 * riguarda i pagamenti. L'elenco viene dal catalogo reale del backend — non più dai moduli che il
 * frontend si trova impacchettati dentro — e ogni card mostra lo stato che l'app ha **per questo
 * account**, compresa la disabilitazione di piattaforma, che prima era invisibile.
 *
 * <p>Ricerca e paginazione lavorano sull'elenco intero, in locale: il catalogo è piccolo e limitato, e
 * una ricerca che non trovasse ciò che sta a pagina tre sarebbe peggio di nessuna ricerca.
 */
export function AppCatalogPage() {
  const { t, i18n } = useTranslation()
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(1)
  const [checkoutSlug, setCheckoutSlug] = useState<string | null>(null)
  const catalog = useAppCatalog()

  // Selettore che ritorna un primitivo: un oggetto nuovo a ogni render rifarebbe montare la pagina.
  const canSubscribe = useAuthStore((s) => !!s.claims?.roles?.includes('owner'))

  const language = i18n.language as Language
  const apps = useMemo(() => catalog.data?.apps ?? [], [catalog.data])
  const matching = useMemo(() => filterApps(apps, query, language), [apps, query, language])
  const { items, page: current, pages } = paginate(matching, page)

  if (checkoutSlug) {
    return (
      <div className="space-y-[22px]">
        <PageHeading />
        <CheckoutFlow appSlug={checkoutSlug} onBack={() => setCheckoutSlug(null)} />
      </div>
    )
  }

  return (
    <div className="space-y-[22px]">
      <PageHeading />

      <QueryState
        isLoading={catalog.isLoading}
        isError={catalog.isError}
        onRetry={() => void catalog.refetch()}
      >
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative min-w-[230px] max-w-[420px] flex-1">
            <Icon
              name="search"
              size={16}
              className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-fg-faint"
            />
            <Input
              type="search"
              className="pl-9"
              aria-label={t('catalog.searchLabel')}
              placeholder={t('catalog.searchPlaceholder')}
              value={query}
              onChange={(e) => {
                setQuery(e.target.value)
                setPage(1) // una nuova ricerca riparte dalla prima pagina
              }}
            />
          </div>
          <span className="text-[12.5px] font-semibold text-fg-muted" role="status">
            {t('catalog.results', { count: matching.length })}
          </span>
        </div>

        {items.length === 0 ? (
          <Card>
            <div className="space-y-2 px-6 py-11 text-center">
              {/* Figura on-brand degli stati vuoti (UC 0087): decorativa — la ricerca
                  senza esiti è già raccontata dal testo qui sotto. */}
              <EmptyIllustration className="mx-auto mb-4 max-w-[200px]" />
              <p className="text-[15px] font-semibold text-fg">{t('catalog.empty')}</p>
              <p className="text-sm text-fg-muted">{t('catalog.emptyHint')}</p>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => {
                  setQuery('')
                  setPage(1)
                }}
              >
                {t('catalog.clearSearch')}
              </Button>
            </div>
          </Card>
        ) : (
          <section
            aria-label={t('catalog.gridLabel')}
            className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3"
          >
            {items.map((app) => (
              <AppCard
                key={app.appSlug}
                app={app}
                language={language}
                canSubscribe={canSubscribe}
                onSubscribe={() => setCheckoutSlug(app.appSlug ?? null)}
              />
            ))}
          </section>
        )}

        <nav aria-label={t('catalog.pagination')} className="flex items-center justify-center gap-3 pt-1">
          <Button
            variant="ghost"
            size="sm"
            disabled={current <= 1}
            onClick={() => setPage(current - 1)}
          >
            {t('catalog.previous')}
          </Button>
          <span className="text-[12.5px] font-bold text-fg-muted">
            {t('catalog.pageOf', { page: current, pages })}
          </span>
          <Button
            variant="ghost"
            size="sm"
            disabled={current >= pages}
            onClick={() => setPage(current + 1)}
          >
            {t('catalog.next')}
          </Button>
        </nav>
      </QueryState>
    </div>
  )
}

function PageHeading() {
  const { t } = useTranslation()
  return (
    <header className="space-y-1">
      <h1 className="text-[27px] font-extrabold tracking-[-0.025em] text-fg">{t('catalog.title')}</h1>
      <p className="text-sm text-fg-muted">{t('catalog.subtitle')}</p>
    </header>
  )
}

function AppCard({
  app,
  language,
  canSubscribe,
  onSubscribe,
}: {
  app: CatalogApp
  language: Language
  canSubscribe: boolean
  onSubscribe: () => void
}) {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const tint = tintFor(app)
  const state = app.state ?? 'available'
  const name = app.name ?? app.appSlug ?? ''
  const date = (iso?: string) =>
    iso ? new Intl.DateTimeFormat(i18n.language, { dateStyle: 'medium' }).format(new Date(iso)) : ''

  return (
    <Card className="flex flex-col overflow-hidden p-0" role="article" aria-label={name}>
      <div className={cn('flex h-[86px] items-center justify-center', HEADER_TINT[tint])}>
        <span
          aria-hidden="true"
          className={cn(
            'grid size-[46px] place-items-center rounded-xl text-[19px] font-extrabold',
            TILE_TINT[tint],
          )}
        >
          {name.slice(0, 1).toUpperCase()}
        </span>
      </div>
      <div className="flex flex-1 flex-col gap-2 px-[18px] pb-[18px] pt-4">
        <div className="flex items-center gap-2">
          <span className="text-[15.5px] font-bold text-fg">{name}</span>
          {state !== 'available' && (
            <Badge className="ml-auto" tone={STATE_TONE[state]}>
              {t(`catalog.state.${state}`)}
            </Badge>
          )}
        </div>
        <p className="flex-1 text-[13px] text-fg-muted">{describeApp(app, language)}</p>
        <div className="mt-1.5 flex items-center gap-2.5">
          {state === 'available' && (
            <>
              <Button size="sm" disabled={!canSubscribe} onClick={onSubscribe}>
                {t('checkout.subscribe')}
              </Button>
              {!canSubscribe && <span className="text-xs text-fg-muted">{t('catalog.ownerOnly')}</span>}
              <span className="ml-auto whitespace-nowrap text-[12.5px] font-semibold text-fg-muted">
                {app.startingPrice
                  ? t('catalog.fromPrice', {
                      // Importo nella lingua attiva: un prezzo scritto con le convenzioni sbagliate
                      // si legge male quanto un testo non tradotto.
                      price: formatPrice(
                        app.startingPrice.amount ?? 0,
                        app.startingPrice.currency ?? 'EUR',
                        i18n.language,
                      ),
                    })
                  : t('catalog.free')}
              </span>
            </>
          )}
          {(state === 'active' || state === 'trial') && (
            <>
              <Button variant="ghost" size="sm" onClick={() => navigate(`/app/${app.appSlug}`)}>
                {t('catalog.open')}
              </Button>
              {/*
                App già in uso grazie alla sola fascia gratuita: senza questa azione il piano a pagamento
                non era comprabile da nessuna parte — la card diceva "attiva" e in Billing non c'è alcuna
                card di abbonamento su cui agire. È il buco che UC 0095 aveva lasciato aperto a UC 0096.
              */}
              {app.upgradeAvailable && (
                <Button size="sm" disabled={!canSubscribe} onClick={onSubscribe}>
                  {t('catalog.upgrade')}
                </Button>
              )}
              <span className="ml-auto text-[12.5px] text-fg-muted">
                {state === 'trial' && app.trialEndsAt
                  ? t('catalog.trialEnds', { date: date(app.trialEndsAt) })
                  : (app.planName ?? '')}
              </span>
            </>
          )}
          {state === 'payment_pending' && (
            <Button size="sm" onClick={() => navigate('/billing')}>
              {t('catalog.fixPayment')}
            </Button>
          )}
          {state === 'cancellation_scheduled' && (
            <>
              <Button variant="ghost" size="sm" onClick={() => navigate('/billing')}>
                {t('catalog.undoCancellation')}
              </Button>
              {app.cancelAt && (
                <span className="ml-auto text-[12.5px] text-fg-muted">
                  {t('catalog.until', { date: date(app.cancelAt) })}
                </span>
              )}
            </>
          )}
          {state === 'disabled_by_platform' && (
            <>
              <Button variant="ghost" size="sm" onClick={() => navigate('/support')}>
                {t('catalog.contactSupport')}
              </Button>
              <span className="ml-auto text-xs text-fg-muted">{t('catalog.disabledHint')}</span>
            </>
          )}
        </div>
      </div>
    </Card>
  )
}

export default AppCatalogPage
