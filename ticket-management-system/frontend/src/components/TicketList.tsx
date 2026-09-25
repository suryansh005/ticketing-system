import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { listTickets } from '../services/api';
import type { Ticket, TicketStatus } from '../types/ticket';
import { TICKET_STATUSES } from '../types/ticket';
import { ApiErrorBanner } from './ApiErrorBanner';
import { StatusBadge } from './StatusBadge';

const SEARCH_DEBOUNCE_MS = 300;

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString();
}

export function TicketList() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Number(searchParams.get('page') ?? '0') || 0;
  const statusFilter = (searchParams.get('status') as TicketStatus | null) ?? '';
  const urlSearch = searchParams.get('search') ?? '';

  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [searchInput, setSearchInput] = useState(urlSearch);
  const [debouncedSearch, setDebouncedSearch] = useState(urlSearch);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    setSearchInput(urlSearch);
    setDebouncedSearch(urlSearch);
  }, [urlSearch]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setDebouncedSearch(searchInput);
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          if (searchInput.trim()) {
            next.set('search', searchInput.trim());
          } else {
            next.delete('search');
          }
          next.set('page', '0');
          return next;
        },
        { replace: true },
      );
    }, SEARCH_DEBOUNCE_MS);
    return () => window.clearTimeout(timer);
  }, [searchInput, setSearchParams]);

  const loadTickets = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await listTickets({
        page,
        size: 20,
        status: statusFilter || undefined,
        search: debouncedSearch || undefined,
      });
      setTickets(result.content);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
    } catch (err) {
      setError(err);
      setTickets([]);
    } finally {
      setLoading(false);
    }
  }, [page, statusFilter, debouncedSearch]);

  useEffect(() => {
    void loadTickets();
  }, [loadTickets]);

  const updatePage = (nextPage: number) => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        next.set('page', String(nextPage));
        return next;
      },
      { replace: true },
    );
  };

  const updateStatus = (status: TicketStatus | '') => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);
        if (status) {
          next.set('status', status);
        } else {
          next.delete('status');
        }
        next.set('page', '0');
        return next;
      },
      { replace: true },
    );
  };

  const clearFilters = () => {
    setSearchInput('');
    setSearchParams({}, { replace: true });
  };

  const showEmpty = !loading && !error && tickets.length === 0;

  return (
    <section className="ticket-list">
      <header className="page-header">
        <h1>Tickets</h1>
        <Link to="/tickets/new" className="btn btn-primary">
          New ticket
        </Link>
      </header>

      <div className="toolbar">
        <label className="field">
          <span className="field-label">Search</span>
          <input
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder="Search title or description"
            aria-label="Search tickets"
          />
        </label>
        <label className="field">
          <span className="field-label">Status</span>
          <select
            value={statusFilter}
            onChange={(e) => updateStatus(e.target.value as TicketStatus | '')}
            aria-label="Filter by status"
          >
            <option value="">All statuses</option>
            {TICKET_STATUSES.map((s) => (
              <option key={s} value={s}>
                {s.replace('_', ' ')}
              </option>
            ))}
          </select>
        </label>
      </div>

      <ApiErrorBanner error={error} onRetry={() => void loadTickets()} />

      {loading && (
        <div className="table-skeleton" aria-busy="true">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="skeleton-row" />
          ))}
        </div>
      )}

      {showEmpty && (
        <div className="empty-state">
          <p>No tickets match your filters.</p>
          <button type="button" className="btn btn-secondary" onClick={clearFilters}>
            Clear filters
          </button>
        </div>
      )}

      {!loading && !error && tickets.length > 0 && (
        <>
          <p className="results-meta">
            {totalElements} ticket{totalElements === 1 ? '' : 's'}
          </p>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th scope="col">Title</th>
                  <th scope="col">Status</th>
                  <th scope="col">Priority</th>
                  <th scope="col">Assignee</th>
                  <th scope="col">Updated</th>
                  <th scope="col">Created</th>
                </tr>
              </thead>
              <tbody>
                {tickets.map((ticket) => (
                  <tr key={ticket.id}>
                    <td>
                      <Link to={`/tickets/${ticket.id}`}>{ticket.title}</Link>
                    </td>
                    <td>
                      <StatusBadge status={ticket.status} />
                    </td>
                    <td>{ticket.priority}</td>
                    <td>{ticket.assigneeId ?? '—'}</td>
                    <td>{formatDate(ticket.updatedAt)}</td>
                    <td>{formatDate(ticket.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <nav className="pagination" aria-label="Ticket list pagination">
              <button
                type="button"
                className="btn btn-secondary"
                disabled={page <= 0}
                onClick={() => updatePage(page - 1)}
              >
                Previous
              </button>
              <span>
                Page {page + 1} of {totalPages}
              </span>
              <button
                type="button"
                className="btn btn-secondary"
                disabled={page >= totalPages - 1}
                onClick={() => updatePage(page + 1)}
              >
                Next
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  );
}
