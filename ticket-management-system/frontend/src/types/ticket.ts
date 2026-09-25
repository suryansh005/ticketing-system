export type TicketStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'CLOSED'
  | 'CANCELLED';

export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface Ticket {
  id: string;
  title: string;
  description: string;
  priority: TicketPriority;
  status: TicketStatus;
  requesterId: string;
  assigneeId: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  priority: TicketPriority;
  assigneeId?: string | null;
}

export interface UpdateTicketRequest {
  title: string;
  description: string;
  priority: TicketPriority;
  assigneeId?: string | null;
}

export function isTerminalStatus(status: TicketStatus): boolean {
  return status === 'CLOSED' || status === 'CANCELLED';
}

export interface ListTicketsParams {
  page?: number;
  size?: number;
  status?: TicketStatus;
  search?: string;
}

export const TICKET_STATUSES: TicketStatus[] = [
  'OPEN',
  'IN_PROGRESS',
  'RESOLVED',
  'CLOSED',
  'CANCELLED',
];

export const TICKET_PRIORITIES: TicketPriority[] = [
  'LOW',
  'MEDIUM',
  'HIGH',
  'URGENT',
];
