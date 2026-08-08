# ledger

A small double-entry ledger: open accounts, record deposits and withdrawals, read a balance and a
history. Every movement is a balanced journal entry, so what the ledger holds always equals what it
owes. Data lives in memory and is gone when the process stops.

## Running it

### With Docker

```sh
docker build -t ledger .
docker run --rm -d -P --name ledger ledger
docker port ledger 80          # e.g. 0.0.0.0:55007 — the address to open
```

### Without Docker

Needs a JDK 21 ([SDKMAN!](https://sdkman.io) is the least intrusive way to get one):

```sh
curl -s "https://get.sdkman.io" | bash && source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 21.0.5-tem
./gradlew run                  # http://localhost:8080
```

## Using it

The server fills itself with sample data at start-up — five holders across three currencies and
eighteen entries dated across four months — so every endpoint has something to show before anything
is typed in, including balances read as of a past date. The data
lives in `src/main/resources/seed.json` and is posted to the server's own API once it is listening,
so a record that no request could have made fails start-up instead of reaching the books. None of the
seeded references is the one the documentation uses in its examples, so a first **Try it out** on
`POST /accounts` still opens an account rather than colliding with a seeded one.

Open the server's address in a browser for **Swagger UI** — every endpoint with its parameters and
responses, and a **Try it out** button on each one, so the whole ledger can be driven from the page.
The document behind it is generated from the live routing tree, so it cannot drift from the routes
that actually exist.

| Path | Description |
|------|-------------|
| `/` | Redirects to the documentation |
| `/swagger` | Swagger UI |
| `/openapi.json` | The OpenAPI document itself, as JSON |

## The API

| | |
|---|---|
| `POST /accounts` | Open an account |
| `GET /accounts` | List accounts, the cash accounts behind them included |
| `GET /accounts/{account}` | One account, by id or by reference |
| `POST /journal/entries` | Record a deposit or a withdrawal |
| `GET /journal/entries` | The entries of one account |
| `GET /journal/balances` | What the ledger owes and holds, on a date |

`{account}` is either the id or the reference — `ACC-ALICE` and its uuid address the same account.
Amounts come back as strings, so nothing is lost to a float on the way through.

```sh
# Open an account
curl -X POST localhost:8080/accounts -H 'content-type: application/json' \
  -d '{"name":"Alice","currency":"EUR","reference":"ACC-000123"}'

# Pay in, and take out — occurredOn and description are optional
curl -X POST localhost:8080/journal/entries -H 'content-type: application/json' \
  -d '{"account":"ACC-000123","type":"DEPOSIT","amount":2400.00,"description":"Salary"}'

curl -X POST localhost:8080/journal/entries -H 'content-type: application/json' \
  -d '{"account":"ACC-000123","type":"WITHDRAWAL","amount":1200.00,"occurredOn":"2026-06-03"}'

# What it owes now, and what it owed then
curl 'localhost:8080/journal/balances?account=ACC-000123'
curl 'localhost:8080/journal/balances?account=ACC-000123&onDate=2026-06-01'

# The history, newest day first, three at a time
curl 'localhost:8080/journal/entries?account=ACC-000123&sort=occurredOn&direction=DESC&limit=3'
```

Every listing answers a page and the cursor to the next one, or `null` on the last:

```json
{ "data": [ … ], "nextCursor": "019fdb85-c939-7780-9548-55fe6716fede" }
```

Pass that back as `?after=` to read on. `limit` holds up to 200 records; `sort` takes `id` or
`occurredOn` for entries, `id` or `reference` for accounts and balances, with `direction=ASC|DESC`.

A rejection always answers the same shape, whatever refused it:

```json
{ "errors": ["10.505 is finer than EUR can hold"] }
```

## Development

The Gradle wrapper is committed, so from the project root (or docker container):

| Task | Description |
|------|-------------|
| `./gradlew run`   | Run the server    |
| `./gradlew test`  | Run the tests     |
| `./gradlew build` | Build the project |

### Wiring

`src/main/resources/application.conf` names the module the server boots and the seed file it fills
itself from. `src/test/resources/application.conf` shadows it: no seed, and a second module —
`testDependencies` — listed after the first, where a test swaps a dependency for something it can
control. `ktor.di.conflictPolicy = OverridePrevious` is what lets the later declaration win, so
production wiring never has to know that tests exist.

### Time zone

The books keep one calendar, taken from the machine's zone, so an accounting day is the same day for
every caller. The Docker image sets `TZ=Europe/Riga`; override it with `-e TZ=…`.

### Port

The server listens on `8080`, or on `80` inside the Docker image. Override it with the `PORT`
environment variable:

```sh
PORT=9090 ./gradlew run
docker run --rm -e PORT=9090 -p 9090:9090 ledger
```

A non-numeric `PORT` fails at startup rather than falling back to the default, so a typo can't leave
the server listening somewhere unexpected.

## Design

[docs/decisions.md](docs/decisions.md) — how the books are modelled, what is deliberately not
enforced, and why.
