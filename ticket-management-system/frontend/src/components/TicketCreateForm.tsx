import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiError, createTicket } from '../services/api';
import type { TicketPriority } from '../types/ticket';
import { TICKET_PRIORITIES } from '../types/ticket';
import { AssigneeSelect } from './AssigneeSelect';
import { ApiErrorBanner } from './ApiErrorBanner';

interface FieldErrors {
  title?: string;
  description?: string;
  priority?: string;
  assigneeId?: string;
}

export function TicketCreateForm() {
  const navigate = useNavigate();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM');
  const [assigneeId, setAssigneeId] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [apiError, setApiError] = useState<unknown>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
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
      const created = await createTicket({
        title: title.trim(),
        description: description.trim(),
        priority,
        assigneeId: assigneeId ? assigneeId : null,
      });
      navigate(`/tickets/${created.id}`);
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
    <section className="ticket-create">
      <header className="page-header">
        <h1>New ticket</h1>
      </header>

      <ApiErrorBanner error={apiError} />

      <form className="ticket-form" onSubmit={(e) => void handleSubmit(e)} noValidate>
        <label className="field">
          <span className="field-label">Title</span>
          <input
            type="text"
            value={title}
            maxLength={200}
            onChange={(e) => setTitle(e.target.value)}
            aria-invalid={fieldErrors.title ? true : undefined}
            aria-describedby={fieldErrors.title ? 'title-error' : undefined}
          />
          {fieldErrors.title && (
            <span id="title-error" className="field-error" role="alert">
              {fieldErrors.title}
            </span>
          )}
        </label>

        <label className="field">
          <span className="field-label">Description</span>
          <textarea
            value={description}
            rows={6}
            onChange={(e) => setDescription(e.target.value)}
            aria-invalid={fieldErrors.description ? true : undefined}
            aria-describedby={fieldErrors.description ? 'description-error' : undefined}
          />
          {fieldErrors.description && (
            <span id="description-error" className="field-error" role="alert">
              {fieldErrors.description}
            </span>
          )}
        </label>

        <label className="field">
          <span className="field-label">Priority</span>
          <select
            value={priority}
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
          <span className="field-label" id="create-assignee-label">
            Assignee
          </span>
          <AssigneeSelect
            id="create-assignee"
            value={assigneeId}
            disabled={submitting}
            error={fieldErrors.assigneeId}
            onChange={setAssigneeId}
          />
        </div>

        <div className="form-actions">
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Creating…' : 'Create ticket'}
          </button>
        </div>
      </form>
    </section>
  );
}
