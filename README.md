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
