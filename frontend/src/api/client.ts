import type {
  CountryListItem, CountryPage, CreateStampRequest, PassportSummary, Stamp, TokenResponse,
  UpdateEntryRequest, UserSummary,
} from './types'

const TOKEN_KEY = 'foodpassport.token'

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token: string | null) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    else localStorage.removeItem(TOKEN_KEY)
  } catch {
    // storage blocked (private mode): you'll just need to log in again next visit
  }
}

/** An error the server sent back, e.g. 409 "That username is taken". */
export class ApiError extends Error {
  status: number
  fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

/** Called when the server says our token is bad or expired (401). The auth layer sets this. */
let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

/**
 * Every API call goes through here. It:
 *  1. attaches "Authorization: Bearer <token>" if we're logged in
 *  2. sends/receives JSON
 *  3. turns error responses into an ApiError with the server's message
 */
async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const response = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (response.status === 401 && token) {
    onUnauthorized()
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  // Spring sends RFC 9457 "problem details": {"status": 400, "detail": "...", "errors": {"field": "message"}}
  try {
    const problem = await response.json()
    return new ApiError(response.status, problem.detail ?? problem.title ?? 'Something went wrong', problem.errors ?? {})
  } catch {
    return new ApiError(response.status, response.status === 401 ? 'Please log in' : 'Something went wrong')
  }
}

export const api = {
  register: (email: string, username: string, password: string) =>
    request<TokenResponse>('POST', '/api/auth/register', { email, username, password }),
  login: (email: string, password: string) =>
    request<TokenResponse>('POST', '/api/auth/login', { email, password }),
  me: () => request<UserSummary & { email: string; createdAt: string }>('GET', '/api/users/me'),

  passport: () => request<PassportSummary>('GET', '/api/passport'),
  countries: () => request<CountryListItem[]>('GET', '/api/countries'),
  countryPage: (code: string) => request<CountryPage>('GET', `/api/countries/${code}`),

  createStamp: (code: string, body: CreateStampRequest) =>
    request<Stamp>('POST', `/api/countries/${code}/stamps`, body),
  stamp: (id: number) => request<Stamp>('GET', `/api/stamps/${id}`),
  review: (id: number, rating: number | null, notes: string) =>
    request<Stamp>('PUT', `/api/stamps/${id}/review`, { rating, notes }),
  updateEntry: (id: number, body: UpdateEntryRequest) =>
    request<Stamp>('PUT', `/api/stamps/${id}/entry`, body),
  setCompanions: (id: number, friendIds: number[]) =>
    request<Stamp>('PUT', `/api/stamps/${id}/companions`, { friendIds }),
  deleteStamp: (id: number) => request<void>('DELETE', `/api/stamps/${id}`),

  friends: () => request<UserSummary[]>('GET', '/api/friends'),
}
