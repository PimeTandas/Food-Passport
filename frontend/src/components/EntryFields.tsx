import { House, Utensils } from 'lucide-react'

export interface EntryValues {
  dishName: string
  placeName: string
  homemade: boolean
  priceAmount: string
  priceCurrency: string
}

interface Props {
  values: EntryValues
  onChange: (values: EntryValues) => void
  errors: Record<string, string>
}

const COMMON_CURRENCIES = ['EUR', 'GBP', 'USD', 'JPY', 'CNY', 'KRW', 'THB', 'INR', 'MXN', 'AUD', 'CAD', 'CHF', 'TRY', 'VND']

/** The shared details of a food entry: used when stamping and when editing. */
export function EntryFields({ values, onChange, errors }: Props) {
  const set = <K extends keyof EntryValues>(key: K, value: EntryValues[K]) => onChange({ ...values, [key]: value })
  const priceError = errors['price.amount'] ?? errors['price.currency'] ?? errors.price
  return (
    <>
      <label className="field">
        <span className="label">Dish</span>
        <input className="input" placeholder="Tonkotsu ramen" value={values.dishName} maxLength={200}
               onChange={(e) => set('dishName', e.target.value)} />
        {errors.dishName && <span className="field-error">{errors.dishName}</span>}
      </label>

      <div className="field">
        <span className="label">Where was it made?</span>
        <div className="choice-tiles" role="group" aria-label="Eaten out or homemade">
          <button type="button" className="choice-tile" aria-pressed={!values.homemade} onClick={() => set('homemade', false)}>
            <Utensils strokeWidth={1.5} />Eaten out
          </button>
          <button type="button" className="choice-tile" aria-pressed={values.homemade} onClick={() => set('homemade', true)}>
            <House strokeWidth={1.5} />Homemade
          </button>
        </div>
      </div>

      <label className="field">
        <span className="label">{values.homemade ? 'Kitchen' : 'Place'}</span>
        <input className="input" value={values.placeName} maxLength={200}
               placeholder={values.homemade ? "Mum's kitchen" : 'Ichiran, Shibuya'}
               onChange={(e) => set('placeName', e.target.value)} />
      </label>

      <div className="field">
        <span className="label">Price · optional</span>
        <div className="input-group">
          <input className="input" type="number" inputMode="decimal" min="0" step="0.01" placeholder="0.00"
                 aria-label="Amount" value={values.priceAmount} onChange={(e) => set('priceAmount', e.target.value)} />
          <input className="input" list="currencies" maxLength={3} aria-label="Currency" placeholder="EUR"
                 value={values.priceCurrency} onChange={(e) => set('priceCurrency', e.target.value.toUpperCase())} />
          <datalist id="currencies">
            {COMMON_CURRENCIES.map((c) => <option key={c} value={c} />)}
          </datalist>
        </div>
        {priceError && <span className="field-error">{priceError}</span>}
      </div>
    </>
  )
}

/** Turn the form's strings into the API's shape. Price is only sent if an amount was typed. */
export function toEntryRequest(v: EntryValues) {
  const amount = v.priceAmount.trim()
  return {
    dishName: v.dishName,
    placeName: v.placeName || undefined,
    homemade: v.homemade,
    price: amount ? { amount: Number(amount), currency: v.priceCurrency || 'EUR' } : undefined,
  }
}
