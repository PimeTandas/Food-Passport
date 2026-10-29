# Food-Passport
This app has been vibe coded from my phone on the train.

Every country has a page in your passport. Eat something from a country and you **stamp its page**.
The stamp is dated automatically and holds a **food entry**: the dish, where you had it, whether it was
homemade or eaten out, the price, photos, and which friends were with you. Tagged friends get the stamp
in their passport too, and everyone keeps their own rating and notes.

## Data model

```
User ──< Friendship (PENDING → ACCEPTED) >── User

Country (249 preloaded: code, name, continent, flag)
   ▲
FoodEntry   shared event: dish, place, homemade?, price + currency, created by
   ▲    └──< Photo (bytes in S3 / local disk)
   │
Stamp       personal: owner, auto date, own rating (1-5), own notes
            one per person at the meal, all pointing at the same FoodEntry
```

## Deploying

See [DEPLOY.md](DEPLOY.md): AWS (one EC2 server, Terraform), HTTPS via Caddy, and GitHub Actions
that test every pull request and deploy every merge to `main`.

## Running everything locally

You need Docker, Java 21 + Maven, and Node 20+. Use three terminals:

```bash
docker compose up -d                    # 1. PostgreSQL
cd backend && mvn spring-boot:run       # 2. API on :8080
cd frontend && npm install && npm run dev   # 3. website on http://localhost:5173
```

Open http://localhost:5173 (on your phone too, if it's on the same Wi-Fi: run `npm run dev -- --host`
and use your computer's IP address).

## Frontend (React + TypeScript + Vite)

Phone-first website in `frontend/`. In development, Vite forwards every `/api/...` call to Spring on
port 8080, so the browser only ever talks to one address.

```
src/
  api/        types.ts (mirrors the Java DTOs) and client.ts (one fetch wrapper that adds the token)
  auth/       AuthContext: who is logged in; the token is kept in localStorage
  components/ Layout (top bar + login guard), InkStamp, StarRating, EntryFields, FriendPicker
  pages/      AuthPage, PassportPage, CountryPage, NewStampPage, StampDetailPage
  styles.css  design tokens at the top (colours, inks, radius), with a light and a dark set
  theme.ts    light / dark / follow-the-device, remembered per browser
```

The look is a real passport, done with restraint:

- **One brand colour** (passport burgundy) on warm paper neutrals; hairlines instead of shadows.
- **Three typefaces, one job each** (self-hosted via Fontsource): EB Garamond for titles,
  IBM Plex Sans for the interface, IBM Plex Mono for passport data such as `29 OCT 2026`.
- **Passport details**: the home screen is your *data page* (holder, passport number, date of issue)
  with a machine-readable zone that uses the real ICAO 9303 check-digit algorithm (`format.ts`);
  every country has a page number; stamps have an ink texture made from SVG fractal noise.
- **Icons** from Lucide; flags are the only emoji.
- **Light and warm dark mode**: the header button cycles device → light → dark.

Screens: sign up / log in → passport (summary, search, continent filters, country grid) →
country page (its stamps) → stamp the page (dish, place, homemade, price, rating, notes, friends) →
stamp detail (edit your review, edit the shared meal, change who was there, remove).

Not in the website yet: managing friends (use the API) and photos.

## Backend (Spring Boot 3, Java 21)

```bash
docker compose up -d        # PostgreSQL on localhost:5432
cd backend
mvn spring-boot:run         # http://localhost:8080
mvn test                    # tests use in-memory H2 + local disk, no Docker needed
```

Flyway builds the database schema from `backend/src/main/resources/db/migration`.

### Configuration (environment variables)

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | local compose database | PostgreSQL connection |
| `JWT_SECRET` | dev-only value | Signs login tokens. **Set a long random value in production.** |
| `STORAGE_TYPE` | `local` | `local` (saves to `./uploads`) or `s3` |
| `S3_BUCKET`, `AWS_REGION` | none / `eu-west-1` | S3 bucket for photos |
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | none | AWS credentials (or `~/.aws`, or an IAM role) |

### API

Every endpoint except `/api/auth/**` needs the header `Authorization: Bearer <token>`.
Errors come back as JSON, for example `{"status": 404, "detail": "Stamp 7 not found", ...}`.

| Method | URL | What it does |
|---|---|---|
| POST | `/api/auth/register` | `{email, username, password}` → `{token, user}` |
| POST | `/api/auth/login` | `{email, password}` → `{token, user}` |
| GET | `/api/users/me` | Who am I |
| GET | `/api/friends` | My friends |
| DELETE | `/api/friends/{userId}` | Unfriend |
| GET | `/api/friends/requests` | Pending requests `{incoming, outgoing}` |
| POST | `/api/friends/requests` | `{username}`: send a request |
| POST | `/api/friends/requests/{id}/accept` | Accept |
| DELETE | `/api/friends/requests/{id}` | Decline or cancel |
| GET | `/api/passport` | Totals and countries stamped per continent |
| GET | `/api/countries` | All countries with my stamp count |
| GET | `/api/countries/{code}` | A country page with my stamps on it |
| POST | `/api/countries/{code}/stamps` | Stamp the page (example below) |
| GET | `/api/stamps/{id}` | One of my stamps with its food entry |
| PUT | `/api/stamps/{id}/review` | My own `{rating, notes}` |
| PUT | `/api/stamps/{id}/entry` | Shared details (any participant) |
| PUT | `/api/stamps/{id}/companions` | `{friendIds}`: who was there (entry creator only) |
| DELETE | `/api/stamps/{id}` | Remove my stamp (the entry goes when its last stamp does) |
| POST | `/api/stamps/{id}/photos` | Multipart `file` (JPEG/PNG/WebP/HEIC, max 10MB, 20 per entry) |
| GET | `/api/photos/{id}` | The image (with S3: a redirect to a signed URL that lasts 10 minutes) |
| DELETE | `/api/photos/{id}` | Uploader or entry creator |

Stamping a page:

```bash
curl -X POST localhost:8080/api/countries/JP/stamps \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"dishName": "Tonkotsu ramen", "placeName": "Ichiran, Tokyo", "homemade": false,
       "price": {"amount": 1290, "currency": "JPY"}, "rating": 5, "notes": "Life changing",
       "withFriendIds": [2]}'
```
