// @appgrove/api-client — entry pubblico (UC 0020).
// Tipi generati dallo spec OpenAPI del core (`schema.ts`, script `gen`) + client openapi-fetch
// con middleware auth (Bearer + 401→refresh→retry) e mapping problem+json.

export { createApiClient, createTypedClient, type ApiClient, type ApiClientConfig } from './client'
export {
  authMiddleware,
  RETRY_HEADER,
  type AuthMiddlewareConfig,
} from './auth-middleware'
export {
  ApiError,
  toApiError,
  unwrap,
  refusalMessage,
  type ProblemDetail,
  type FetchResult,
} from './problem'
export type { UserView, UserAppView, AccountView, InvitationView, MyMembershipsView } from './contract'
export type { paths, components } from './schema'

// Ruolo della persona su un'applicazione e suo confronto (UC 0101): parte del contratto di piattaforma,
// non della presentazione — il design system riceve un booleano, non un ruolo.
export { APP_ROLES, appRoleAtLeast, type AppRole } from './app-role'
