# Decisions and assumptions

The brief asked for deposits, withdrawals, a balance and a history, backed by in-memory data, and
said explicitly not to expect authentication, logging, monitoring or atomic operations. What follows
is what was decided beyond that, and what was deliberately left out.

## Whose books these are

The ledger keeps **the bank's books**, not the account holder's. An account holder's money is money
the ledger owes them, so their account is a `LIABILITY`; the cash the ledger received in exchange is
an `ASSET`. A deposit increases both sides, a withdrawal decreases both:

```
Deposit 10.50    DEBIT  Cash EUR        (ASSET     +10.50)
                 CREDIT Alice           (LIABILITY +10.50)

Withdrawal 4.00  DEBIT  Alice           (LIABILITY  -4.00)
                 CREDIT Cash EUR        (ASSET      -4.00)
```

Both sides of every entry are real accounts on the books — there is no "external" placeholder — and
every entry balances, so the whole ledger holds to `total debits == total credits`, which is
asserted in the tests. In balance-sheet terms that is
`assets + expenses == liabilities + equity + revenue`; with only holders and cash in play it reduces
to assets equalling liabilities.

The alternative framing is the account holder's own books, where their account is an `ASSET` funded
by `EQUITY` (what GnuCash does). Either is defensible; the bank's framing was chosen because a
ledger *service* keeps money on behalf of others.

The cash account for a currency is opened on demand, the first time money in that currency moves.

## How a movement becomes an entry

A movement names one **subject** account and an amount. Everything else is derived, by one rule:

> Post the subject on the side producing the requested **effect** — increase means its own normal
> side, decrease the opposite. Post the counterpart on the **opposite** debit/credit side. Whether
> the counterpart's own balance rises or falls follows from *its* type.

| event | subject | effect | subject side | counterpart | counterpart side | counterpart effect |
|---|---|---|---|---|---|---|
| deposit | holder (LIABILITY) | increase | CREDIT | Cash (ASSET) | DEBIT | increase |
| withdrawal | holder (LIABILITY) | decrease | DEBIT | Cash (ASSET) | CREDIT | decrease |
| fee charged | holder (LIABILITY) | decrease | DEBIT | Fee revenue (REVENUE) | CREDIT | increase |
| interest paid | holder (LIABILITY) | increase | CREDIT | Interest expense (EXPENSE) | DEBIT | increase |

Three pieces carry it, so none of them has to branch per movement:

- **`AccountRole`** names a well-known account by its purpose and derives its reference, name and
  type — `CASH` gives `CASH-EUR`, an `ASSET`. **`ChartOfAccounts`** resolves a role and currency to
  that account, opening it if it has never been used.
- **`MovementType`** is the rule table above: a counterpart role and an effect on the subject.
- **`PostingFactory`** applies the rule and returns a `Posting` — the journal entry plus the accounts
  it touches. **`PostingStore`** writes all of it, which is also how an account first used as a
  counterpart comes into existence.

Only `DEPOSIT` and `WITHDRAWAL` exist, because only those have endpoints. The last two rows are what
extension looks like: one row in `AccountRole`, one in `MovementType`, and a use case that names it.
Nothing in the factory, the store or the aggregates changes.

`AccountType` carries all five standard categories (`ASSET`, `LIABILITY`, `EQUITY`, `REVENUE`,
`EXPENSE`) with their normal sides, which is what makes the single rule work for any counterpart.

## Amounts

Amounts are stored as **integer minor units** (`Money(minorUnits: Long, currency: Currency)`).
A ledger only ever adds and subtracts posted amounts, which is exactly where integers beat decimals:
the arithmetic is exact and there is no rounding policy to get wrong. This is what Stripe, Adyen and
TigerBeetle do. `Money.plus` uses `Math.addExact`, so an overflow raises instead of wrapping.

Decimals live at the edge: the API speaks `BigDecimal` (`10.50`, not `1050`), converted using the
currency's own `defaultFractionDigits`, so JPY is whole units and EUR is cents. An amount finer than
the currency allows — `10.505` in EUR — is **rejected**, not rounded, because a ledger should not
silently invent or lose a fraction.

`BigDecimal` would be the better internal choice the day interest, FX rates or per-unit pricing
arrive, since those produce sub-minor-unit intermediates. Nothing here does.

## Two dates on an entry

Real books date an entry twice, and conflating the two is a modelling bug rather than a
simplification:

| | type | what it means |
|---|---|---|
| `occurredOn` | `LocalDate` | the **effective date** — when the event happened. Balances, periods and "as of" reads are computed from this. |
| `createdAt` | `Instant` | the **entry date** — when the books received it. Audit trail; also what ids sort by. |

Backdating within an open period is ordinary accounting: an invoice arriving on the 5th for work done
on the 28th is posted with last month's effective date. `POST /journal/entries` therefore accepts
`occurredOn`, defaulting to today, and `?onDate=` on balances filters on it. Dating an entry **after**
today is refused (`JournalEntryFactory.FutureDated`) — the common rule, and the one that keeps a
balance "as of today" meaningful.

The effective date is a `LocalDate`, not an `Instant`, because that is what it is: books are kept per
accounting day, and no rule cares that a sale happened at 14:32. Modelling it as an instant also
makes `onDate=2026-06-01` silently exclude everything that happened *during* the 1st, which is never
what the question means. `Instant` stays where precision is real — the recording trail.

Dates need a zone, and a `Clock` deliberately has none: it hands out instants, which have no calendar
until something says where midnight falls. `LedgerCalendar(clock, zone)` is that something, and the
only place in the app a date is derived from a moment. The zone is a single injected token
(`TimeZone.currentSystemDefault()`), so the books keep **one** calendar rather than one per caller —
an accounting day is the same day for everyone. Deployment picks it: the image sets
`TZ=Europe/Riga`.

## What is not enforced

- **Overdrafts are allowed.** A withdrawal may take a holder's balance below zero. A real ledger
  decides this per product, and refusing it here would have been an invented rule.
- **No idempotency.** A replayed deposit posts twice. Movements carry no external reference, and a
  reference without a uniqueness check would be decoration. This is the feature the brief excludes.
- **No atomicity.** See below.
- **No period closing.** Any past date may be backdated to. Real books lock a closed period and take
  a correction in the current one instead; that is a period aggregate and an authority check, not a
  change to the entry.
- **An entry may predate its account.** Nothing checks `occurredOn` against when the account was
  opened, since an account record is administrative and the event is not.

## Transactions

Posting a movement writes two accounts and one journal entry. Those writes belong together, and the
brief excludes transactions, so the boundary is *named* rather than implemented: `TransactionManager`
is a port, each application service opens exactly one, and `InMemoryTransactionManager` simply runs
the work. A failure part-way through therefore leaves earlier writes in place — the one guarantee it
cannot give.

One consequence worth stating plainly: the **uniqueness check on an account reference** is
check-then-save, so two concurrent opens with the same reference can both pass. In a database a
unique index does the real work.

## Identity and references

Three separate things, deliberately not conflated:

| | what it is |
|---|---|
| `id` | internal surrogate key, a UUIDv7 (`Uuid.generateV7NonMonotonicAt`), taken from the injected clock so it sorts by creation time |
| `reference` | the external natural key — `ACC-000123`, `CASH-EUR` — normalized to upper case, unique, quoted back by other systems |
| `name` | a free-text display label, mutable, not unique |

Every account-scoped route accepts either the id or the reference. Ids are validated to be v7 on
construction, so a v4 UUID from elsewhere is rejected rather than silently looked up and missed.

## Reading

History, balances and the account listing are **keyset paginated** (`Cursor(after, limit)`), not offset
paginated: entry ids are time-ordered, so `after` maps to `WHERE id > ? ORDER BY id LIMIT ?`, which
an index serves directly and which cannot skip or repeat rows as the journal grows. Neither
repository exposes an unpaged read.

Since ids follow `createdAt`, the default order is **recording order**, which is what a journal is: a
chronological record of postings, each showing the date it happened.

**Ordering travels in the cursor** (`Cursor(limit, after, sorting)`), because a cursor that does not
know the order it was cut from cannot resume. `Sorting(field: String, direction)` is deliberately
flat — a field name, the way an ORM takes one — so no aggregate needs a parallel enum of its own
columns and `Cursor` needs no second type parameter. The id always comes last in the order, whatever
the field: two entries share a date, and a page has to resume from the record *after* the one it
handed back, which only the id can identify.

The in-memory adapter reads the field off the record by name, so it can order by anything a record
carries. That is more than a caller should reach into, so **the API publishes the fields it will
answer for** — `Sortable("occurredOn")`, declared once per route and used twice: it
fills the `enum` on the `sort` parameter in the OpenAPI document, and rejects anything else with a
400 before the service is called. The two cannot drift, because they are the same value.

`nextCursor` stays an opaque id under any ordering: the adapter finds the anchor in the ordered list
rather than encoding the sort key into the cursor. In SQL that is a subselect for the anchor row; the
alternative — packing `(key, id)` into the cursor string — is the faster one and remains open.

**The journal answers what a balance is.** `BalancesCalculator` folds an account's entry lines up to
a moment and returns a `Balances` value object:

```kotlin
entries.linesOf(id, until = onDate)
    .fold(Money.zero(currency)) { running, line -> running + line.signedAgainst(type.normalSide) }
```

`GET /journal/balances` always asks the journal, which is why `?onDate=…` falls out for free — a
balance *is* a question about the journal on a date, so asking it of the past costs nothing extra in
code. The fold filters on `occurredOn`, so a backdated entry counts from the day it happened, not the
day it was keyed in.

`Account.balance` also exists, but as a **read model**, not as truth. `PostingFactory` projects each
line onto the accounts it touches and `PostingStore` saves them, so a listing can show every holder's
balance without folding the journal once per row. Nothing computes *from* it; it exists so
`GET /accounts` is observable at a glance.

Because it is derived, it can in principle drift — so a test asserts it does not: after a run of
movements, every account's projected balance is compared against what `BalancesCalculator` folds
from the journal. If the two ever disagree, the journal wins and the projection is rebuilt from it.

The price of the authoritative path is honest: folding is O(entries) per account, and asking for all
accounts folds the journal. Fine at this size, wrong at millions of entries. The standard fix keeps
the journal authoritative and makes the projection load-bearing: roll balances up per account and
period so a fold only ever covers the tail. That is an adapter and a job, not a change to the model.

## Errors

Every rejection answers with the same shape:

```json
{"errors": ["name: must not be blank"]}
```

Exceptions live next to whatever raises them (`Money.MalformedAmount`, `JournalEntry.Unbalanced`,
`Cursor.InvalidLimit`, `AccountReference.Malformed`) and share an abstract `LedgerException`, so
`StatusPages` needs three handlers rather than a dozen: validation and any `LedgerException` are
`400`, `AccountNotFound` is `404`, `AccountAlreadyOpen` is `409`.

Validation is split by who can judge it: Jakarta constraints on the DTOs for shape (`limit` at least
1, name length, reference format), the domain for meaning (`limit` at most 200, debits equal
credits, the currency can hold this amount).
