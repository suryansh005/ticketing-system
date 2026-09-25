import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, applyTransition, getTicket } from '../services/api';
import { useUsers } from '../hooks/useUsers';
import type { Ticket } from '../types/ticket';
import type { TransitionEvent } from '../types/transition';
import { ApiErrorBanner } from './ApiErrorBanner';
import { StatusBadge } from './StatusBadge';
import { TicketComments } from './TicketComments';
import { TicketEditForm } from './TicketEditForm';

const ACTIONS: Partial<
  Record<Ticket['status'], { label: string; event: TransitionEvent }[]>
> = {
  OPEN: [
    { label: 'Start progress', event: 'START_PROGRESS' },
    { label: 'Cancel', event: 'CANCEL' },
  ],
  IN_PROGRESS: [
    { label: 'Resolve', event: 'RESOLVE' },
    { label: 'Cancel', event: 'CANCEL' },
  ],
  RESOLVED: [{ label: 'Close', event: 'CLOSE' }],
};

function assigneeLabel(
  assigneeId: string | null,
  users: { id: string; name: string }[],
): string {
  if (!assigneeId) {
    return '—';
  }
  const match = users.find((user) => user.id === assigneeId);
  return match ? match.name : assigneeId;
}

export function TicketDetail() {
  const { ticketId } = useParams<{ ticketId: string }>();
  const { users } = useUsers();
  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [transitionError, setTransitionError] = useState<unknown>(null);
  const [transitioning, setTransitioning] = useState(false);

  const load = useCallback(async () => {
    if (!ticketId) {
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setTicket(await getTicket(ticketId));
    } catch (err) {
      setError(err);
      setTicket(null);
    } finally {
      setLoading(false);
    }
  }, [ticketId]);

  useEffect(() => {
    void load();
  }, [load]);

  const onTransition = async (event: TransitionEvent) => {
    if (!ticketId) {
      return;
    }
    setTransitionError(null);
    setTransitioning(true);
    try {
      const updated = await applyTransition(ticketId, event);
      setTicket(updated);
    } catch (err) {
      setTransitionError(err);
      if (err instanceof ApiError && err.code === 'TICKET_ILLEGAL_TRANSITION') {
        await load();
      }
    } finally {
      setTransitioning(false);
    }
  };

  if (!ticketId) {
    return <p>Invalid ticket id.</p>;
  }

  return (
    <section className="ticket-detail">
      <p>
        <Link to="/tickets">← Back to tickets</Link>
      </p>

      <ApiErrorBanner error={error} onRetry={() => void load()} />

      {loading && <div className="detail-skeleton" aria-busy="true" />}

      {!loading && ticket && (
        <>
          <header className="detail-header">
            <h1>{ticket.title}</h1>
            <StatusBadge status={ticket.status} />
            <span className="priority-badge">{ticket.priority}</span>
          </header>

          <dl className="detail-meta">
            <div>
              <dt>Requester</dt>
              <dd>{ticket.requesterId}</dd>
            </div>
            <div>
              <dt>Assignee</dt>
              <dd>{assigneeLabel(ticket.assigneeId, users)}</dd>
            </div>
            <div>
              <dt>Updated</dt>
              <dd>{new Date(ticket.updatedAt).toLocaleString()}</dd>
            </div>
            <div>
              <dt>Version</dt>
              <dd>{ticket.version}</dd>
            </div>
          </dl>

          <TicketEditForm ticket={ticket} onUpdated={setTicket} />

          {ACTIONS[ticket.status] && (
            <div className="status-actions">
              <h2>Status actions</h2>
              <ApiErrorBanner error={transitionError} />
              <div className="action-row">
                {ACTIONS[ticket.status]!.map(({ label, event }) => (
                  <button
                    key={event}
                    type="button"
                    className="btn btn-secondary"
                    disabled={transitioning}
                    onClick={() => void onTransition(event)}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>
          )}

          <TicketComments ticketId={ticket.id} />
        </>
      )}
    </section>
  );
}
