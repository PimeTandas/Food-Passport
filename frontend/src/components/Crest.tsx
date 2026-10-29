import { Utensils } from 'lucide-react'

/**
 * The passport crest: double ring, text set on a circle, utensils in the middle.
 * Drawn in currentColor, so it's burgundy in the header and gold foil on the cover.
 */
export function Crest({ className, withText = false }: { className?: string; withText?: boolean }) {
  return (
    <svg className={className} viewBox="0 0 120 120" fill="none" stroke="currentColor" aria-hidden>
      <circle cx="60" cy="60" r="57" strokeWidth="2" />
      <circle cx="60" cy="60" r="52" strokeWidth="1" />
      {withText && (
        <>
          <defs>
            <path id="crest-arc" d="M60 60 m-42 0 a42 42 0 1 1 84 0 a42 42 0 1 1 -84 0" />
          </defs>
          <text fill="currentColor" stroke="none" fontFamily="EB Garamond, Georgia, serif" fontSize="9.5" letterSpacing="3.6">
            <textPath href="#crest-arc" startOffset="0">FOOD PASSPORT · BON APPÉTIT ·</textPath>
          </text>
          <circle cx="60" cy="60" r="32" strokeWidth="1" />
        </>
      )}
      <Utensils x={withText ? 42 : 34} y={withText ? 42 : 34} width={withText ? 36 : 52} height={withText ? 36 : 52}
                strokeWidth={withText ? 1.4 : 2} />
    </svg>
  )
}
