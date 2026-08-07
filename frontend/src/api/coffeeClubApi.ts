import type {
  ApiErrorResponse,
  FieldErrorDetail,
  GroupOrderResponse,
  OrderLinePayload,
  PrepopulateResponse,
} from '../types';

/**
 * Relative base path. In dev the Vite server proxies it to Spring Boot on port 8080; in the
 * packaged JAR the API and the UI are served from the same origin, so it works unchanged.
 */
const API_BASE = '/api/orders';

/** An error carrying the structured detail the API returned, so the UI can highlight cells. */
export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: FieldErrorDetail[];

  constructor(message: string, status: number, fieldErrors: FieldErrorDetail[] = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function readError(response: Response): Promise<ApiError> {
  try {
    const payload = (await response.json()) as Partial<ApiErrorResponse>;
    return new ApiError(
      payload.message ?? 'The server rejected the request.',
      response.status,
      payload.fieldErrors ?? [],
    );
  } catch {
    return new ApiError(
      `The server responded with ${response.status}. Please try again.`,
      response.status,
    );
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      headers: { Accept: 'application/json', ...(init?.body ? { 'Content-Type': 'application/json' } : {}) },
      ...init,
    });
  } catch {
    throw new ApiError('Could not reach the coffee club server. Is the backend running?', 0);
  }

  if (!response.ok) {
    throw await readError(response);
  }
  return (await response.json()) as T;
}

/** Rows and lifetime balances needed to render the group order page. */
export function fetchPrepopulatedOrder(): Promise<PrepopulateResponse> {
  return request<PrepopulateResponse>('/prepopulate');
}

/** Validates and saves today's round, returning the confirmed payer and total. */
export function submitGroupOrder(lines: OrderLinePayload[]): Promise<GroupOrderResponse> {
  return request<GroupOrderResponse>('', {
    method: 'POST',
    body: JSON.stringify({ lines }),
  });
}
