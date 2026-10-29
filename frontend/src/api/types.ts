// These mirror the Java records in the backend (StampDtos, AuthDtos, ...).
// If you change a DTO in Spring, change it here too.

export interface UserSummary {
  id: number
  username: string
}

export interface TokenResponse {
  token: string
  user: UserSummary
}

export interface Country {
  code: string
  name: string
  continent: string
  flag: string
}

export interface CountryListItem extends Country {
  stampCount: number
}

export interface Price {
  amount: number
  currency: string
}

export interface Photo {
  id: number
  url: string
  contentType: string
  uploadedBy: UserSummary | null
  createdAt: string
}

export interface FoodEntry {
  id: number
  dishName: string
  placeName: string | null
  homemade: boolean
  price: Price | null
  createdBy: UserSummary | null
  with: UserSummary[]
  photos: Photo[]
}

export interface Stamp {
  id: number
  stampedAt: string
  rating: number | null
  notes: string | null
  country: Country
  entry: FoodEntry
}

export interface CountryPage {
  country: Country
  stamps: Stamp[]
}

export interface PassportSummary {
  totalStamps: number
  countriesStamped: number
  totalCountries: number
  countriesStampedPerContinent: Record<string, number>
}

export interface CreateStampRequest {
  dishName: string
  placeName?: string
  homemade: boolean
  price?: Price
  rating?: number
  notes?: string
  withFriendIds: number[]
}

export interface UpdateEntryRequest {
  dishName: string
  placeName?: string
  homemade: boolean
  price?: Price
}
