import { useCallback, useEffect, useState } from 'react'

/**
 * Load something from the API when a component appears (and again when `deps` change).
 * Returns { data, error, loading, reload } so pages don't each re-implement loading states.
 */
export function useAsync<T>(load: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<Error | null>(null)
  const [loading, setLoading] = useState(true)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let cancelled = false // ignore results that arrive after we've moved on
    setLoading(true)
    setError(null)
    load()
      .then((result) => { if (!cancelled) setData(result) })
      .catch((e: Error) => { if (!cancelled) setError(e) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, version])

  const reload = useCallback(() => setVersion((v) => v + 1), [])
  return { data, error, loading, reload, setData }
}
