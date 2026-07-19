import { useNavigate } from 'react-router-dom'
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
import { useItems } from '../api/hooks'
import { QuotaBanner } from '../components/QuotaBanner'
import { ContactAvatar } from '../components/ContactAvatar'
import { StatusBadge } from '../components/StatusBadge'
import { formatAmount, t } from '../strings'

/** Schermata elenco: header con riquadro icona app, banner quota, tabella. */
export function ItemListScreen() {
  const navigate = useNavigate()
  const items = useItems()
  const rows = items.data?.content ?? []

  return (
    <div className="space-y-[22px]">
      <PageHeader
        title={t.title}
        subtitle={t.subtitle}
        icon="@@ICON@@"
        iconClassName="bg-@@ACCENT@@/15 text-@@ACCENT@@"
        actions={
          <Button
            className="bg-@@ACCENT@@ shadow-[0_6px_16px_-6px_rgb(var(--ag-@@ACCENT@@))]"
            onClick={() => navigate('new')}
          >
            <Icon name="add" size={19} />
            {t.newItem}
          </Button>
        }
      />

      <QuotaBanner />

      <QueryState
        isLoading={items.isLoading}
        isError={items.isError}
        onRetry={() => void items.refetch()}
      >
        {rows.length === 0 ? (
          <div className="rounded-lg border border-line bg-surface px-6 py-12 text-center shadow-sm">
            <Icon name="@@ICON@@" size={42} className="text-fg-faint" />
            <p className="mt-3 text-[15px] font-bold text-fg">{t.empty}</p>
          </div>
        ) : (
          <Table>
            <TableHead>
              <TableRow>
                <TableHeadCell>{t.colCode}</TableHeadCell>
                <TableHeadCell>{t.colContact}</TableHeadCell>
                <TableHeadCell>{t.colRecordedOn}</TableHeadCell>
                <TableHeadCell>{t.colStatus}</TableHeadCell>
                <TableHeadCell className="text-right">{t.colTotal}</TableHeadCell>
                <TableHeadCell className="text-right">{t.colActions}</TableHeadCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((item) => (
                <TableRow key={item.id} interactive onClick={() => navigate(String(item.id))}>
                  <TableCell className="font-mono font-semibold text-fg-muted">{item.code}</TableCell>
                  <TableCell>
                    <span className="flex items-center gap-3">
                      <ContactAvatar name={item.contactName} />
                      <span className="font-semibold">{item.contactName}</span>
                    </span>
                  </TableCell>
                  <TableCell className="text-fg-muted">
                    {item.recordedOn ? new Date(item.recordedOn).toLocaleDateString('it-IT') : '—'}
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={item.status} />
                  </TableCell>
                  <TableCell className="text-right font-mono font-bold">
                    {formatAmount(item.totalAmount, item.currency)}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation()
                        navigate(String(item.id))
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
    </div>
  )
}
