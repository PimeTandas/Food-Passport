export function Loading({ text = 'Loading…' }: { text?: string }) {
  return <p className="muted" role="status">{text}</p>
}

export function ErrorMessage({ error, onRetry }: { error: Error; onRetry?: () => void }) {
  return (
    <div className="alert row" role="alert">
      <span>{error.message}</span>
      {onRetry && <button className="btn ghost small" onClick={onRetry}>Try again</button>}
    </div>
  )
}
