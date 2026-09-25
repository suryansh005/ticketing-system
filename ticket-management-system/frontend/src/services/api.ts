import type {
  Comment,
  CreateCommentRequest,
} from '../types/comment';
import type {
  CreateTicketRequest,
  ListTicketsParams,
  PageResponse,
  Ticket,
  TicketStatus,
  UpdateTicketRequest,
} from '../types/ticket';
import type { User } from '../types/user';
import type { TransitionEvent } from '../types/transition';

export interface ValidationErrorDetail {
  field: string;
  message: string;
  code: string;
}

export interface ApiErrorBody {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  errors?: ValidationErrorDetail[];
  fromStatus?: TicketStatus;
  event?: TransitionEvent;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | undefined;
  readonly title: string | undefined;
  readonly errors: ValidationErrorDetail[];
  readonly fromStatus: TicketStatus | undefined;
  readonly event: TransitionEvent | undefined;

  constructor(status: number, body: ApiErrorBody) {
    const message = body.detail ?? body.title ?? `Request failed (${status})`;
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = body.code;
    this.title = body.title;
    this.errors = body.errors ?? [];
    this.fromStatus = body.fromStatus;
    this.event = body.event;
  }
}

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

const TICKETS_PATH = `${API_BASE}/api/v1/tickets`;
const USERS_PATH = `${API_BASE}/api/v1/users`;

async function parseJson<T>(response: Response): Promise<T | undefined> {
  const text = await response.text();
  if (!text) {
    return undefined;
  }
  try {
    return JSON.parse(text) as T;
  } catch {
    return undefined;
  }
}

async function request<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  });

  if (response.ok) {
    if (response.status === 204) {
      return undefined as T;
    }
    const data = await parseJson<T>(response);
    return data as T;
  }

  const body = (await parseJson<ApiErrorBody>(response)) ?? {};
  if (body.status === undefined) {
    body.status = response.status;
  }
  throw new ApiError(response.status, body);
}

function buildListQuery(params: ListTicketsParams): string {
  const searchParams = new URLSearchParams();
  searchParams.set('page', String(params.page ?? 0));
  searchParams.set('size', String(params.size ?? 20));
  if (params.status) {
    searchParams.set('status', params.status);
  }
  if (params.search?.trim()) {
    searchParams.set('search', params.search.trim());
  }
  return searchParams.toString();
}

export async function listUsers(): Promise<User[]> {
  return request<User[]>(USERS_PATH);
}

export async function listTickets(
  params: ListTicketsParams = {},
): Promise<PageResponse<Ticket>> {
  const query = buildListQuery(params);
  return request<PageResponse<Ticket>>(`${TICKETS_PATH}?${query}`);
}

export async function createTicket(
  payload: CreateTicketRequest,
): Promise<Ticket> {
  return request<Ticket>(TICKETS_PATH, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function getTicket(ticketId: string): Promise<Ticket> {
  return request<Ticket>(`${TICKETS_PATH}/${ticketId}`);
}

export async function applyTransition(
  ticketId: string,
  event: TransitionEvent,
): Promise<Ticket> {
  return request<Ticket>(`${TICKETS_PATH}/${ticketId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ event }),
  });
}

export async function updateTicket(
  ticketId: string,
  payload: UpdateTicketRequest,
): Promise<Ticket> {
  return request<Ticket>(`${TICKETS_PATH}/${ticketId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export async function listComments(
  ticketId: string,
  page = 0,
  size = 50,
): Promise<PageResponse<Comment>> {
  const searchParams = new URLSearchParams();
  searchParams.set('page', String(page));
  searchParams.set('size', String(size));
  searchParams.set('sort', 'createdAt,asc');
  return request<PageResponse<Comment>>(
    `${TICKETS_PATH}/${ticketId}/comments?${searchParams.toString()}`,
  );
}

export async function addComment(
  ticketId: string,
  payload: CreateCommentRequest,
): Promise<Comment> {
  return request<Comment>(`${TICKETS_PATH}/${ticketId}/comments`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}
