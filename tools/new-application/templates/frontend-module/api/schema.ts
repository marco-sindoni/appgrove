/**
 * Tipi dell'API @@APP_ID@@ — SEGNAPOSTO scritto dallo scaffolding (tools/new-application, UC 0046).
 *
 * ⚠️ Questo file è normalmente GENERATO dallo spec OpenAPI del servizio:
 *
 *     npm run gen:@@APP_ID@@
 *
 * Lo scaffolding non può generarlo, perché lo spec nasce solo dopo la prima compilazione del
 * backend (`quarkus.smallrye-openapi.store-schema-directory`). Quindi qui c'è una versione scritta a
 * mano, fedele al dominio segnaposto: basta a far compilare e girare il modulo appena creato.
 *
 * PRIMO GESTO dopo aver compilato il backend: rilanciare `npm run gen:@@APP_ID@@` e sostituire
 * questo file col generato. Da quel momento in poi lo spec è la fonte di verità e ogni divergenza
 * fra frontend e backend rompe `tsc` invece di arrivare in produzione (#10 G25).
 */

export interface paths {
  '/api/@@APP_ID@@/v1/items': {
    parameters: { query?: never; header?: never; path?: never; cookie?: never }
    get: {
      parameters: {
        query?: { page?: number; size?: number }
        header?: never
        path?: never
        cookie?: never
      }
      requestBody?: never
      responses: {
        200: {
          headers: { [name: string]: unknown }
          content: { 'application/json': components['schemas']['PageItemView'] }
        }
        401: { headers: { [name: string]: unknown }; content?: never }
        403: { headers: { [name: string]: unknown }; content?: never }
      }
    }
    put?: never
    post: {
      parameters: { query?: never; header?: never; path?: never; cookie?: never }
      requestBody: {
        content: { 'application/json': components['schemas']['CreateItem'] }
      }
      responses: {
        201: {
          headers: { [name: string]: unknown }
          content: { 'application/json': components['schemas']['ItemView'] }
        }
        400: { headers: { [name: string]: unknown }; content?: never }
        402: { headers: { [name: string]: unknown }; content?: never }
        429: { headers: { [name: string]: unknown }; content?: never }
      }
    }
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
  '/api/@@APP_ID@@/v1/items/{id}': {
    parameters: { query?: never; header?: never; path?: never; cookie?: never }
    get: {
      parameters: {
        query?: never
        header?: never
        path: { id: string }
        cookie?: never
      }
      requestBody?: never
      responses: {
        200: {
          headers: { [name: string]: unknown }
          content: { 'application/json': components['schemas']['ItemView'] }
        }
        404: { headers: { [name: string]: unknown }; content?: never }
      }
    }
    put?: never
    post?: never
    delete: {
      parameters: {
        query?: never
        header?: never
        path: { id: string }
        cookie?: never
      }
      requestBody?: never
      responses: {
        204: { headers: { [name: string]: unknown }; content?: never }
        404: { headers: { [name: string]: unknown }; content?: never }
      }
    }
    options?: never
    head?: never
    patch: {
      parameters: {
        query?: never
        header?: never
        path: { id: string }
        cookie?: never
      }
      requestBody: {
        content: { 'application/json': components['schemas']['UpdateItem'] }
      }
      responses: {
        200: {
          headers: { [name: string]: unknown }
          content: { 'application/json': components['schemas']['ItemView'] }
        }
        404: { headers: { [name: string]: unknown }; content?: never }
      }
    }
    trace?: never
  }
  '/api/@@APP_ID@@/v1/quota': {
    parameters: { query?: never; header?: never; path?: never; cookie?: never }
    get: {
      parameters: { query?: never; header?: never; path?: never; cookie?: never }
      requestBody?: never
      responses: {
        200: {
          headers: { [name: string]: unknown }
          content: { 'application/json': components['schemas']['QuotaStatusView'] }
        }
        403: { headers: { [name: string]: unknown }; content?: never }
      }
    }
    put?: never
    post?: never
    delete?: never
    options?: never
    head?: never
    patch?: never
    trace?: never
  }
}

export type webhooks = Record<string, never>

export interface components {
  schemas: {
    CreateItem: {
      contactName: string
      contactEmail?: string
      recordedOn?: components['schemas']['LocalDate']
      currency?: string
      lines?: components['schemas']['CreateLine'][]
    }
    CreateLine: {
      description: string
      quantity?: number
      unitAmount?: number
    }
    ItemView: {
      id?: components['schemas']['UUID']
      code?: string
      contactName?: string
      contactEmail?: string
      recordedOn?: components['schemas']['LocalDate']
      status?: string
      currency?: string
      totalAmount?: number
      tenantId?: string
      lines?: components['schemas']['LineView'][]
    }
    LineView: {
      id?: components['schemas']['UUID']
      description?: string
      quantity?: number
      unitAmount?: number
      lineAmount?: number
    }
    /** Format: date */
    LocalDate: string
    PageItemView: {
      content?: components['schemas']['ItemView'][]
      page?: number
      size?: number
      totalElements?: number
      totalPages?: number
    }
    QuotaStatusView: {
      metric?: string
      used?: number
      limit?: number
      remaining?: number
    }
    /** Format: uuid */
    UUID: string
    UpdateItem: {
      contactName?: string
      contactEmail?: string
      status?: string
    }
  }
  responses: never
  parameters: never
  requestBodies: never
  headers: never
  pathItems: never
}

export type $defs = Record<string, never>
export type operations = Record<string, never>
