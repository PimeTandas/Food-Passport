export function Loading({ text = 'Loading…' }: { text?: string }) {
  return <main className="page"><p className="muted" role="status">{text}</p></main>
}

export function ErrorMessage({ error, onRetry }: { error: Error; onRetry?: () => void }) {
  return (
    <main className="page">
      <div className="alert spread" role="alert">
        <span>{error.message}</span>
        {onRetry && <button className="text-btn" onClick={onRetry}>Try again</button>}
      </div>
    </main>
  )
}
