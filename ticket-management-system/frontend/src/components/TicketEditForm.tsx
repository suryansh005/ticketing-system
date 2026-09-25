import { useEffect, useState, type FormEvent } from 'react';
import { ApiError, updateTicket } from '../services/api';
import type { Ticket, TicketPriority } from '../types/ticket';
import { TICKET_PRIORITIES, isTerminalStatus } from '../types/ticket';
import { AssigneeSelect } from './AssigneeSelect';
import { ApiErrorBanner } from './ApiErrorBanner';

interface FieldErrors {
  title?: string;
  description?: string;
  priority?: string;
  assigneeId?: string;
}

interface TicketEditFormProps {
  ticket: Ticket;
  onUpdated: (ticket: Ticket) => void;
}

export function TicketEditForm({ ticket, onUpdated }: TicketEditFormProps) {
  const readOnly = isTerminalStatus(ticket.status);
  const [title, setTitle] = useState(ticket.title);
  const [description, setDescription] = useState(ticket.description);
  const [priority, setPriority] = useState<TicketPriority>(ticket.priority);
  const [assigneeId, setAssigneeId] = useState(ticket.assigneeId ?? '');
  const [submitting, setSubmitting] = useState(false);
  const [apiError, setApiError] = useState<unknown>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  useEffect(() => {
    setTitle(ticket.title);
    setDescription(ticket.description);
    setPriority(ticket.priority);
    setAssigneeId(ticket.assigneeId ?? '');
    setApiError(null);
    setFieldErrors({});
  }, [ticket]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (readOnly) {
      return;
    }

    setApiError(null);
    setFieldErrors({});

    const clientErrors: FieldErrors = {};
    if (!title.trim()) {
      clientErrors.title = 'Title is required';
    } else if (title.length > 200) {
      clientErrors.title = 'Title must be at most 200 characters';
    }
    if (!description.trim()) {
      clientErrors.description = 'Description is required';
    }
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }

    setSubmitting(true);
    try {
      const updated = await updateTicket(ticket.id, {
        title: title.trim(),
        description: description.trim(),
        priority,
        assigneeId: assigneeId ? assigneeId : null,
      });
      onUpdated(updated);
    } catch (err) {
      setApiError(err);
      if (err instanceof ApiError && err.errors.length > 0) {
        const next: FieldErrors = {};
        for (const e of err.errors) {
          if (
            e.field === 'title' ||
            e.field === 'description' ||
            e.field === 'priority' ||
            e.field === 'assigneeId'
          ) {
            next[e.field] = e.message;
          }
        }
        setFieldErrors(next);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="ticket-edit">
      <h2>Edit ticket</h2>
      {readOnly && (
        <p className="read-only-hint">
          This ticket is {ticket.status.toLowerCase().replace('_', ' ')} and can no
          longer be edited.
        </p>
      )}

      <ApiErrorBanner error={apiError} />

      <form
        className="ticket-form"
        onSubmit={(e) => void handleSubmit(e)}
        noValidate
      >
        <label className="field">
          <span className="field-label">Title</span>
          <input
            type="text"
            value={title}
            maxLength={200}
            disabled={readOnly || submitting}
            onChange={(e) => setTitle(e.target.value)}
            aria-invalid={fieldErrors.title ? true : undefined}
            aria-describedby={fieldErrors.title ? 'edit-title-error' : undefined}
          />
          {fieldErrors.title && (
            <span id="edit-title-error" className="field-error" role="alert">
              {fieldErrors.title}
            </span>
          )}
        </label>

        <label className="field">
          <span className="field-label">Description</span>
          <textarea
            value={description}
            rows={6}
            disabled={readOnly || submitting}
            onChange={(e) => setDescription(e.target.value)}
            aria-invalid={fieldErrors.description ? true : undefined}
            aria-describedby={
              fieldErrors.description ? 'edit-description-error' : undefined
            }
          />
          {fieldErrors.description && (
            <span
              id="edit-description-error"
              className="field-error"
              role="alert"
            >
              {fieldErrors.description}
            </span>
          )}
        </label>

        <label className="field">
          <span className="field-label">Priority</span>
          <select
            value={priority}
            disabled={readOnly || submitting}
            onChange={(e) => setPriority(e.target.value as TicketPriority)}
          >
            {TICKET_PRIORITIES.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
          {fieldErrors.priority && (
            <span className="field-error" role="alert">
              {fieldErrors.priority}
            </span>
          )}
        </label>

        <div className="field">
          <span className="field-label" id="edit-assignee-label">
            Assignee
          </span>
          <AssigneeSelect
            id="edit-assignee"
            value={assigneeId}
            disabled={readOnly || submitting}
            error={fieldErrors.assigneeId}
            onChange={setAssigneeId}
          />
        </div>

        <div className="form-actions">
          <button
            type="submit"
            className="btn btn-primary"
            disabled={readOnly || submitting}
          >
            {submitting ? 'Saving…' : 'Save changes'}
          </button>
        </div>
      </form>
    </section>
  );
}
