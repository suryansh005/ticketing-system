import type { TicketStatus } from '../types/ticket';

const STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: 'Open',
  IN_PROGRESS: 'In progress',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
  CANCELLED: 'Cancelled',
};

interface StatusBadgeProps {
  status: TicketStatus;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span className={`badge badge-status badge-${status.toLowerCase()}`}>
      {STATUS_LABELS[status]}
    </span>
  );
}
