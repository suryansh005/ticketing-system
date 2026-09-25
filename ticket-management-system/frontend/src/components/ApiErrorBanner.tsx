import { ApiError } from '../services/api';

interface ApiErrorBannerProps {
  error: unknown;
  onRetry?: () => void;
}

function formatApiError(error: ApiError): string {
  if (error.code === 'VALIDATION_ERROR' && error.errors.length > 0) {
    return error.errors.map((e) => `${e.field}: ${e.message}`).join('; ');
  }
  if (error.code === 'TICKET_ILLEGAL_TRANSITION') {
    return error.message;
  }
  return error.title ? `${error.title} — ${error.message}` : error.message;
}

export function ApiErrorBanner({ error, onRetry }: ApiErrorBannerProps) {
  if (!error) {
    return null;
  }

  const message =
    error instanceof ApiError
      ? formatApiError(error)
      : error instanceof Error
        ? error.message
        : 'Something went wrong';

  const code = error instanceof ApiError ? error.code : undefined;

  return (
    <div className="error-banner" role="alert">
      <div>
        {code && <span className="error-code">{code}</span>}
        <p>{message}</p>
      </div>
      {onRetry && (
        <button type="button" className="btn btn-secondary" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  );
}
