import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, refusalMessage } from '@appgrove/api-client'
import {
  Button,
  Icon,
  PageHeader,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeadCell,
  TableRow,
} from '@appgrove/design-system'
import { QueryState } from '../../../shell/QueryState'
import { useContacts } from '../api/hooks'
import { QuotaBanner } from '../components/QuotaBanner'
import { ContactAvatar } from '../components/ContactAvatar'
import { StageBadge } from '../components/StageBadge'
import { t } from '../strings'

const STAGES = ['lead', 'qualified', 'negotiating', 'won', 'lost'] as const

/** Elenco contatti: header, banner posti, ricerca + filtro stato, tabella. */
export function ContactListScreen() {
  const navigate = useNavigate()
  const [q, setQ] = useState('')
  const [stage, setStage] = useState('')
  const contacts = useContacts({ q, stage })
  const rows = contacts.data?.content ?? []
  const filtered = q !== '' || stage !== ''
  // Un 403 qui non è un guasto: è un varco che ha detto no. Ma i varchi sono TRE e parlano di cose
  // diverse — il posto (UC 0054), l'accesso all'applicazione e il ruolo su di essa (UC 0099) — e solo il
  // server sa quale ha risposto. Si mostra quindi la SUA frase, che nomina il varco e la via d'uscita;
  // il testo sul posto resta come ripiego per quando il server non ne manda una.
  const refused = (contacts.error as ApiError | null)?.status === 403
  const refusalText = refused ? refusalMessage(contacts.error, t.errorNoSeat) : null

  return (
    <div className="space-y-[22px]">
      <PageHeader
        title={t.title}
        subtitle={t.subtitle}
        icon="contacts"
        iconClassName="bg-cat-teal/15 text-cat-teal"
        actions={
          <Button
            className="bg-cat-teal shadow-[0_6px_16px_-6px_rgb(var(--ag-cat-teal))]"
            onClick={() => navigate('new')}
          >
            <Icon name="add" size={19} />
            {t.newContact}
          </Button>
        }
      />

      <QuotaBanner />

      {refused ? (
        <div
          role="status"
          className="rounded-lg border border-line bg-surface px-6 py-12 text-center shadow-sm"
        >
          <Icon name="lock" size={42} className="text-fg-faint" />
          <p className="mx-auto mt-3 max-w-md text-[15px] font-bold text-fg">{refusalText}</p>
          <div className="mt-4">
            <Button variant="secondary" size="sm" onClick={() => navigate('members')}>
              <Icon name="group" size={18} />
              {t.membersTitle}
            </Button>
          </div>
        </div>
      ) : (
      <>
      <div className="flex flex-wrap items-center gap-3">
        <label className="flex-1 min-w-[16rem]">
          <span className="sr-only">{t.searchPlaceholder}</span>
          <input
            type="search"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            placeholder={t.searchPlaceholder}
            className="h-10 w-full rounded-md border border-line bg-surface px-3 text-sm text-fg"
          />
        </label>
        <label>
          <span className="sr-only">{t.colStage}</span>
          <select
            value={stage}
            onChange={(e) => setStage(e.target.value)}
            className="h-10 rounded-md border border-line bg-surface px-3 text-sm text-fg"
          >
            <option value="">{t.filterAllStages}</option>
            {STAGES.map((s) => (
              <option key={s} value={s}>
                {t[`stage${s.charAt(0).toUpperCase()}${s.slice(1)}` as keyof typeof t] as string}
              </option>
            ))}
          </select>
        </label>
      </div>

      <QueryState
        isLoading={contacts.isLoading}
        isError={contacts.isError}
        onRetry={() => void contacts.refetch()}
      >
        {rows.length === 0 ? (
          <div className="rounded-lg border border-line bg-surface px-6 py-12 text-center shadow-sm">
            <Icon name="contacts" size={42} className="text-fg-faint" />
            <p className="mt-3 text-[15px] font-bold text-fg">{filtered ? t.emptySearch : t.empty}</p>
          </div>
        ) : (
          <Table>
            <TableHead>
              <TableRow>
                <TableHeadCell>{t.colName}</TableHeadCell>
                <TableHeadCell>{t.colOrganization}</TableHeadCell>
                <TableHeadCell>{t.colStage}</TableHeadCell>
                <TableHeadCell className="text-right">{t.colActions}</TableHeadCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((contact) => (
                <TableRow key={contact.id} interactive onClick={() => navigate(String(contact.id))}>
                  <TableCell>
                    <span className="flex items-center gap-3">
                      <ContactAvatar name={contact.displayName} />
                      <span className="font-semibold">{contact.displayName}</span>
                    </span>
                  </TableCell>
                  <TableCell className="text-fg-muted">{contact.organization || '—'}</TableCell>
                  <TableCell>
                    <StageBadge stage={contact.stage} />
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation()
                        navigate(String(contact.id))
                      }}
                    >
                      {t.detailTitle}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </QueryState>
      </>
      )}
    </div>
  )
}
