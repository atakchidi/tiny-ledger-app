package altak.ledger.application.ledger

class InvalidCursor(cursor: String) : RuntimeException("Cursor '$cursor' is not an entry to continue from.")
