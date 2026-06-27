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
  type ProblemDetail,
  type FetchResult,
} from './problem'
export type { UserView, AccountView, InvitationView } from './contract'
export type { paths, components } from './schema'
