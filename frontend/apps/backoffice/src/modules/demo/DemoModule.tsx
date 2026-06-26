import { Route, Routes } from 'react-router-dom'
import { Card, CardContent, CardHeader, CardTitle } from '@appgrove/design-system'
import { useShellContext } from '../../registry/ShellContext'

/**
 * Modulo demo montato dalla shell sotto `/app/demo/*`. Dimostra il contratto Context: legge
 * `tenant_id`/`user_id`/ruoli **dal contesto** (mai altrove) e gestisce le proprie route interne.
 */
export default function DemoModule() {
  const shell = useShellContext()

  return (
    <div data-testid="demo-module" className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold text-fg">Demo app</h1>
        <p className="text-sm text-fg-muted">
          Tenant <span className="font-mono">{shell.tenantId}</span> · utente{' '}
          <span className="font-mono">{shell.userId}</span>
        </p>
      </header>

      <Routes>
        <Route
          index
          element={
            <Card>
              <CardHeader>
                <CardTitle>Overview</CardTitle>
              </CardHeader>
              <CardContent>
                Modulo montato dal registry tramite contratto shell↔modulo (UC 0020).
              </CardContent>
            </Card>
          }
        />
        <Route
          path="items"
          element={
            <Card>
              <CardHeader>
                <CardTitle>Items</CardTitle>
              </CardHeader>
              <CardContent>Sezione interna del modulo (route lazy nested).</CardContent>
            </Card>
          }
        />
      </Routes>
    </div>
  )
}
