import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { ApiError, addComment, listComments } from '../services/api';
import type { Comment } from '../types/comment';
import { ApiErrorBanner } from './ApiErrorBanner';

interface FieldErrors {
  body?: string;
}

interface TicketCommentsProps {
  ticketId: string;
}

function formatTimestamp(iso: string): string {
  try {
    return new Date(iso).toLocaleString();
  } catch {
    return iso;
  }
}

export function TicketComments({ ticketId }: TicketCommentsProps) {
  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<unknown>(null);
  const [body, setBody] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<unknown>(null);
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const page = await listComments(ticketId);
      setComments(page.content);
    } catch (err) {
      setLoadError(err);
      setComments([]);
    } finally {
      setLoading(false);
    }
  }, [ticketId]);

  useEffect(() => {
    void load();
  }, [load]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitError(null);
    setFieldErrors({});

    const trimmed = body.trim();
    if (!trimmed) {
      setFieldErrors({ body: 'Comment is required' });
      return;
    }

    setSubmitting(true);
    try {
      const created = await addComment(ticketId, { body: trimmed });
      setComments((prev) => [...prev, created]);
      setBody('');
    } catch (err) {
      setSubmitError(err);
      if (err instanceof ApiError && err.errors.length > 0) {
        const next: FieldErrors = {};
        for (const e of err.errors) {
          if (e.field === 'body') {
            next.body = e.message;
          }
        }
        setFieldErrors(next);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="ticket-comments">
      <h2>Comments</h2>

      <ApiErrorBanner error={loadError} onRetry={() => void load()} />

      {loading && <p className="comments-loading">Loading comments…</p>}

      {!loading && !loadError && comments.length === 0 && (
        <p className="empty-thread">No comments yet.</p>
      )}

      {!loading && comments.length > 0 && (
        <ul className="comment-list">
          {comments.map((comment) => (
            <li key={comment.id} className="comment-item">
              <div className="comment-meta">
                <span className="comment-author">{comment.authorId}</span>
                <time dateTime={comment.createdAt}>
                  {formatTimestamp(comment.createdAt)}
                </time>
              </div>
              <p className="comment-body">{comment.body}</p>
            </li>
          ))}
        </ul>
      )}

      <form
        className="comment-form"
        onSubmit={(e) => void handleSubmit(e)}
        noValidate
      >
        <h3>Add comment</h3>
        <ApiErrorBanner error={submitError} />
        <label className="field">
          <span className="field-label">Comment</span>
          <textarea
            value={body}
            rows={4}
            disabled={submitting}
            onChange={(e) => setBody(e.target.value)}
            aria-invalid={fieldErrors.body ? true : undefined}
          />
          {fieldErrors.body && (
            <span className="field-error" role="alert">
              {fieldErrors.body}
            </span>
          )}
        </label>
        <div className="form-actions">
          <button
            type="submit"
            className="btn btn-primary"
            disabled={submitting}
          >
            {submitting ? 'Posting…' : 'Post comment'}
          </button>
        </div>
      </form>
    </section>
  );
}
