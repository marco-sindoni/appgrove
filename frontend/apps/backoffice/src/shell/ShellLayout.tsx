import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { ShellProvider } from '../registry/ShellContext'
import { EnforcementBanner } from '../billing/EnforcementBanner'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'
import { TwoFaNudge } from './TwoFaNudge'

/** Chrome permanente: sidebar (drawer su mobile) + topbar + area contenuti (`Outlet`). Responsive (#03 dec.12). */
export function ShellLayout() {
  const [drawerOpen, setDrawerOpen] = useState(false)

  return (
    <ShellProvider>
      <div className="flex h-screen overflow-hidden bg-bg">
        <aside className="hidden w-64 shrink-0 border-r border-line bg-surface lg:block">
          <Sidebar />
        </aside>

        {drawerOpen && (
          <div className="fixed inset-0 z-40 lg:hidden">
            <button
              type="button"
              aria-label="Chiudi menu"
              className="absolute inset-0 bg-black/40"
              onClick={() => setDrawerOpen(false)}
            />
            <aside className="absolute left-0 top-0 h-full w-64 border-r border-line bg-surface">
              <Sidebar onNavigate={() => setDrawerOpen(false)} />
            </aside>
          </div>
        )}

        <div className="flex min-w-0 flex-1 flex-col">
          <Topbar onOpenSidebar={() => setDrawerOpen(true)} />
          <EnforcementBanner />
          <TwoFaNudge />
          <main className="flex-1 overflow-auto p-6">
            <Outlet />
          </main>
        </div>
      </div>
    </ShellProvider>
  )
}
