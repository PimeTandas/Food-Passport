import type { Price } from './api/types'

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: 'numeric' })
}

export function formatPrice(price: Price): string {
  try {
    return new Intl.NumberFormat(undefined, { style: 'currency', currency: price.currency }).format(price.amount)
  } catch {
    return `${price.amount} ${price.currency}`
  }
}

const INKS = ['--ink-red', '--ink-blue', '--ink-green', '--ink-purple']

/** Each stamp gets a stable ink colour and tilt based on its id, so it looks the same every visit. */
export function stampLook(id: number): { ink: string; tilt: string } {
  const tilt = ((id * 37) % 13) - 6 // -6..6 degrees
  return { ink: `var(${INKS[id % INKS.length]})`, tilt: `${tilt}deg` }
}
