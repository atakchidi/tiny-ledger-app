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
the whole ledger holds to `total assets == total liabilities`, which is asserted in the tests.

The alternative framing is the account holder's own books, where their account is an `ASSET` funded
by `EQUITY` (what GnuCash does). Either is defensible; the bank's framing was chosen because a
ledger *service* keeps money on behalf of others.

The cash account for a currency is opened on demand, the first time money in that currency moves.

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

## What is not enforced

- **Overdrafts are allowed.** A withdrawal may take a holder's balance below zero. A real ledger
  decides this per product, and refusing it here would have been an invented rule.
- **No idempotency.** A replayed deposit posts twice. Movements carry no external reference, and a
  reference without a uniqueness check would be decoration. This is the feature the brief excludes.
- **No atomicity.** See below.

## Transactions

Posting a movement writes two accounts and one journal entry. Those writes belong together, and the
brief excludes transactions, so the boundary is *named* rather than implemented: `TransactionManager`
is a port, each application service opens exactly one, and `InMemoryTransactionManager` simply runs
the work. A failure part-way through therefore leaves earlier writes in place — the one guarantee it
cannot give.

Two consequences worth stating plainly:

- The **running balance** on an account is derived state written separately from the journal. A crash
  between the two diverges them. In a database both writes are one transaction; a repair job can
  always recompute a balance from the journal.
- The **uniqueness check on an account reference** is check-then-save, so two concurrent opens with
  the same reference can both pass. In a database a unique index does the real work.

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

History and the account listing are **keyset paginated** (`Cursor(after, limit)`), not offset
paginated: entry ids are time-ordered, so `after` maps to `WHERE id > ? ORDER BY id LIMIT ?`, which
an index serves directly and which cannot skip or repeat rows as the journal grows. Neither
repository exposes an unpaged read.

A balance is a column read (`accounts.byId(...).balance`), not a fold over the journal — the thing
that would not survive millions of entries.

## Errors

Every rejection answers with the same shape:

```json
{"errors": ["name: must not be blank"]}
```

Exceptions live next to whatever raises them (`Money.MalformedAmount`, `JournalEntry.Unbalanced`,
`Cursor.InvalidLimit`, `Account.ForeignLine`) and share an abstract `LedgerException`, so
`StatusPages` needs three handlers rather than a dozen: validation and any `LedgerException` are
`400`, `AccountNotFound` is `404`, `AccountAlreadyOpen` is `409`.

Validation is split by who can judge it: Jakarta constraints on the DTOs for shape (`limit` at least
1, name length, reference format), the domain for meaning (`limit` at most 200, debits equal
credits, the currency can hold this amount).
