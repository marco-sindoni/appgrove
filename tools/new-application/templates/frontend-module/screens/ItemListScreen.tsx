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
import { useTranslation } from '@appgrove/i18n'
import { QueryState } from '../../../shell/QueryState'
import { useItems } from '../api/hooks'
import { QuotaBanner } from '../components/QuotaBanner'
import { ContactAvatar } from '../components/ContactAvatar'
import { StatusBadge } from '../components/StatusBadge'
import { formatAmount, formatDate, use@@APP_CLASS@@Messages } from '../i18n'

/** Schermata elenco: header con riquadro icona app, banner quota, tabella. */
export function ItemListScreen() {
  const navigate = useNavigate()
  const items = useItems()
  const rows = items.data?.content ?? []
  const m = use@@APP_CLASS@@Messages()
  const { i18n } = useTranslation()

  return (
    <div className="space-y-[22px]">
      <PageHeader
        title={m.title}
        subtitle={m.subtitle}
        icon="@@ICON@@"
        iconClassName="bg-@@ACCENT@@/15 text-@@ACCENT@@"
        actions={
          <Button
            className="bg-@@ACCENT@@ shadow-[0_6px_16px_-6px_rgb(var(--ag-@@ACCENT@@))]"
            onClick={() => navigate('new')}
          >
            <Icon name="add" size={19} />
            {m.newItem}
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
            <p className="mt-3 text-[15px] font-bold text-fg">{m.empty}</p>
          </div>
        ) : (
          <Table>
            <TableHead>
              <TableRow>
                <TableHeadCell>{m.colCode}</TableHeadCell>
                <TableHeadCell>{m.colContact}</TableHeadCell>
                <TableHeadCell>{m.colRecordedOn}</TableHeadCell>
                <TableHeadCell>{m.colStatus}</TableHeadCell>
                <TableHeadCell className="text-right">{m.colTotal}</TableHeadCell>
                <TableHeadCell className="text-right">{m.colActions}</TableHeadCell>
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
                    {formatDate(item.recordedOn, i18n.language)}
                  </TableCell>
                  <TableCell>
                    <StatusBadge status={item.status} />
                  </TableCell>
                  <TableCell className="text-right font-mono font-bold">
                    {formatAmount(item.totalAmount, item.currency, i18n.language)}
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
                      {m.detailTitle}
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
