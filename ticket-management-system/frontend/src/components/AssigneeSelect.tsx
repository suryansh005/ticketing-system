import { useUsers } from '../hooks/useUsers';
import { ApiErrorBanner } from './ApiErrorBanner';

interface AssigneeSelectProps {
  value: string;
  onChange: (assigneeId: string) => void;
  disabled?: boolean;
  error?: string;
  id?: string;
}

export function AssigneeSelect({
  value,
  onChange,
  disabled = false,
  error,
  id = 'assignee',
}: AssigneeSelectProps) {
  const { users, loading, error: loadError, reload } = useUsers();
  const knownIds = new Set(users.map((user) => user.id));
  const showUnknownOption = value !== '' && !knownIds.has(value);

  return (
    <div className="assignee-select">
      <ApiErrorBanner error={loadError} onRetry={() => void reload()} />
      <select
        id={id}
        value={value}
        disabled={disabled || loading}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
      >
        <option value="">Unassigned</option>
        {showUnknownOption && (
          <option value={value}>Current assignee (unknown)</option>
        )}
        {users.map((user) => (
          <option key={user.id} value={user.id}>
            {user.name}
          </option>
        ))}
      </select>
      {loading && <span className="field-hint">Loading users…</span>}
      {error && (
        <span id={`${id}-error`} className="field-error" role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
