import { Route, Routes } from 'react-router-dom'
import { FattureClientProvider } from './api/FattureClientProvider'
import { InvoiceListScreen } from './screens/InvoiceListScreen'
import { InvoiceCreateScreen } from './screens/InvoiceCreateScreen'
import { InvoiceDetailScreen } from './screens/InvoiceDetailScreen'

/**
 * Modulo **fatture** (app #1, UC 0052) montato dalla shell sotto `/app/fatture/*`. Autocontenuto:
 * client tipizzato co-locato (FattureClientProvider), route interne lazy, UI col design system. Legge
 * `tenant_id`/`user_id` solo dal contesto shell; l'enforcement quota è backend (banner = solo UX).
 */
export default function FattureModule() {
  return (
    <FattureClientProvider>
      <div data-testid="fatture-module">
        <Routes>
          <Route index element={<InvoiceListScreen />} />
          <Route path="new" element={<InvoiceCreateScreen />} />
          <Route path=":id" element={<InvoiceDetailScreen />} />
        </Routes>
      </div>
    </FattureClientProvider>
  )
}
