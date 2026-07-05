import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { useTranslation } from '@appgrove/i18n'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'

/** Chrome permanente della console admin: sidebar (drawer su mobile) + topbar + area contenuti (`Outlet`). */
export function ShellLayout() {
  const { t } = useTranslation()
  const [drawerOpen, setDrawerOpen] = useState(false)

  return (
    <div className="flex h-screen overflow-hidden bg-bg">
      <aside className="hidden w-[262px] shrink-0 border-r border-line bg-surface lg:block">
        <Sidebar />
      </aside>

      {drawerOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <button
            type="button"
            aria-label={t('admin.closeMenu')}
            className="absolute inset-0 bg-black/40"
            onClick={() => setDrawerOpen(false)}
          />
          <aside className="absolute left-0 top-0 h-full w-[262px] border-r border-line bg-surface">
            <Sidebar onNavigate={() => setDrawerOpen(false)} />
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar onOpenSidebar={() => setDrawerOpen(true)} />
        <main className="flex-1 overflow-y-auto">
          <div className="mx-auto w-full max-w-[1240px] px-[30px] pb-[60px] pt-[28px]">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
