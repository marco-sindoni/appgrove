import type { ReactNode } from 'react'
import { Card, CardContent, CardHeader, CardTitle, Logo } from '@appgrove/design-system'

/** Chrome condiviso delle schermate auth: card centrata con logo + titolo. */
export function AuthLayout({
  title,
  children,
  footer,
}: {
  title: string
  children: ReactNode
  footer?: ReactNode
}) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-bg p-6">
      <div className="w-full max-w-sm space-y-4">
        <Card>
          <CardHeader className="items-center gap-2 text-center">
            <Logo size={32} />
            <CardTitle>{title}</CardTitle>
          </CardHeader>
          <CardContent>{children}</CardContent>
        </Card>
        {footer && <div className="text-center text-sm text-fg-muted">{footer}</div>}
      </div>
    </div>
  )
}
