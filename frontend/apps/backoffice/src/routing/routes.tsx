import type { RouteObject } from 'react-router-dom'
import { ShellLayout } from '../shell/ShellLayout'
import { ProtectedRoute, AppModuleHost, requireAuth, requireAnyRole } from './guards'
import { Dashboard } from '../pages/Dashboard'
import { Account } from '../pages/Account'
import { Billing } from '../pages/Billing'
import { Settings } from '../pages/Settings'
import { SecurityPage } from '../pages/SecurityPage'
import { PrivacyPage } from '../pages/privacy/PrivacyPage'
import { SupportPage } from '../pages/support/SupportPage'
import { MembersPage } from '../pages/members/MembersPage'
import { Forbidden } from '../pages/Forbidden'
import { NotFound } from '../pages/NotFound'
import { LoginPage } from '../pages/auth/LoginPage'
import { OnboardingWizard } from '../pages/auth/OnboardingWizard'
import { VerifyEmailPage } from '../pages/auth/VerifyEmailPage'
import { ForgotPasswordPage } from '../pages/auth/ForgotPasswordPage'
import { ResetPasswordPage } from '../pages/auth/ResetPasswordPage'
import { AcceptInvitePage } from '../pages/auth/AcceptInvitePage'

/**
 * Albero route: **pubbliche auth** (login/signup/verify/forgot/reset/accept + forbidden) e area shell
 * protetta (`requireAuth`) con pagine nested e host dei moduli app **lazy** (`/app/:appId/*`).
 * Le route auth landing dei link email (`/verify|/reset|/accept`) sono dettate da auth-local (UC 0058).
 */
export const routes: RouteObject[] = [
  { path: '/login', element: <LoginPage /> },
  { path: '/signup', element: <OnboardingWizard /> },
  { path: '/verify', element: <VerifyEmailPage /> },
  { path: '/forgot', element: <ForgotPasswordPage /> },
  { path: '/reset', element: <ResetPasswordPage /> },
  { path: '/accept', element: <AcceptInvitePage /> },
  { path: '/forbidden', element: <Forbidden /> },
  {
    element: <ProtectedRoute guard={requireAuth} />,
    children: [
      {
        element: <ShellLayout />,
        children: [
          { index: true, element: <Dashboard /> },
          { path: 'account', element: <Account /> },
          { path: 'billing', element: <Billing /> },
          { path: 'settings', element: <Settings /> },
          { path: 'security', element: <SecurityPage /> },
          { path: 'privacy', element: <PrivacyPage /> },
          { path: 'support', element: <SupportPage /> },
          {
            element: <ProtectedRoute guard={requireAnyRole(['owner', 'admin'])} />,
            children: [{ path: 'members', element: <MembersPage /> }],
          },
          { path: 'app/:appId/*', element: <AppModuleHost /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFound /> },
]
