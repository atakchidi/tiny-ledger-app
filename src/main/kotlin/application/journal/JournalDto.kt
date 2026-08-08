package altak.ledger.application.journal

import altak.ledger.application.shared.BigDecimalSerializer
import altak.ledger.application.shared.CurrencySerializer
import altak.ledger.application.shared.LocalDateSerializer
import altak.ledger.application.shared.UuidSerializer
import altak.ledger.domain.journal.Direction
import io.ktor.openapi.JsonSchema
import altak.ledger.domain.journal.MovementType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Size
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.Currency
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class RecordAccountEntryDto(
    @JsonSchema.Description("The id of the account to move, or the reference it is known by outside")
    @JsonSchema.Example("ACC-000123")
    val account: String,

    @JsonSchema.Description("Which way the money goes: a DEPOSIT pays in, a WITHDRAWAL takes out")
    val type: MovementType,

    @field:DecimalMin(value = "0", inclusive = false, message = "must be a positive amount")
    @field:Digits(integer = 16, fraction = 4, message = "must be an amount a currency can hold")
    @Serializable(with = BigDecimalSerializer::class)
    @JsonSchema.Description(
        "How much to move, in the account's own currency and no finer than that currency allows",
    )
    @JsonSchema.Example("10.50")
    val amount: BigDecimal,

    @field:Size(max = 140, message = "must be at most 140 characters")
    @JsonSchema.Description("What the movement was for; the ledger names it Deposit or Withdrawal if you say nothing")
    val description: String? = null,

    @Serializable(with = LocalDateSerializer::class)
    @JsonSchema.Description(
        "The date the movement happened, which is what balances are read against; today if left out. " +
            "It may be backdated but not dated ahead of today.",
    )
    @JsonSchema.Example("2026-06-01")
    val occurredOn: LocalDate? = null,
)

@Serializable
data class EntryQueryDto(
    @JsonSchema.Description("The id of an account, or the reference it is known by outside")
    @JsonSchema.Example("ACC-000123")
    val account: String,
)

@Serializable
data class ViewEntryDto(
    @Serializable(with = UuidSerializer::class)
    val id: Uuid,

    @JsonSchema.Description("What the movement was for")
    val description: String,

    @Serializable(with = LocalDateSerializer::class)
    @JsonSchema.Description("The date the movement happened; balances are read against this")
    @JsonSchema.Example("2026-06-01")
    val occurredOn: LocalDate,

    @JsonSchema.Description("The moment the books recorded it")
    val createdAt: Instant,

    val updatedAt: Instant,

    @Serializable(with = CurrencySerializer::class)
    @JsonSchema.Description("The currency every line of the entry is in, as an ISO 4217 code")
    @JsonSchema.Example("EUR")
    val currency: Currency,

    @JsonSchema.Description("What the entry debits in total; always equal to what it credits")
    @JsonSchema.Example("10.50")
    val totalDebit: String,

    @JsonSchema.Description("What the entry credits in total; always equal to what it debits")
    @JsonSchema.Example("10.50")
    val totalCredit: String,

    @JsonSchema.Description("Both sides of the entry: what was debited and what was credited, always equal")
    val lines: List<ViewEntryLineDto>,
)

@Serializable
data class ViewEntryLineDto(
    @JsonSchema.Description("The account this side of the entry lands on")
    @JsonSchema.Example("019fdb85-c939-7780-9548-55fe6716fede")
    val accountId: String,

    @JsonSchema.Description("The reference that account is known by outside")
    @JsonSchema.Example("ACC-000123")
    val reference: String,

    @JsonSchema.Description("Which side of the account the amount lands on")
    val direction: String,

    @JsonSchema.Example("10.50")
    val amount: String,
)

@Serializable
data class BalanceQueryDto(
    @JsonSchema.Description("An account id or the reference it is known by outside; every account if left out")
    @JsonSchema.Example("ACC-000123")
    val account: String? = null,

    @Serializable(with = LocalDateSerializer::class)
    @JsonSchema.Description("The date to read the journal as of, as YYYY-MM-DD; today if left out")
    @JsonSchema.Example("2026-06-01")
    val onDate: LocalDate? = null,
)

@Serializable
data class ViewBalanceDto(
    @Serializable(with = LocalDateSerializer::class)
    @JsonSchema.Description("The date the journal was read as of")
    @JsonSchema.Example("2026-06-01")
    val onDate: LocalDate,

    @JsonSchema.Description("The id of the account the balance belongs to")
    @JsonSchema.Example("\"019fdb85-c939-7780-9548-55fe6716fede\"")
    val accountId: String,

    @JsonSchema.Description("The reference that account is known by outside")
    @JsonSchema.Example("ACC-000123")
    val reference: String,

    @Serializable(with = CurrencySerializer::class)
    @JsonSchema.Description("The currency the balance is in, as an ISO 4217 code")
    @JsonSchema.Example("EUR")
    val currency: Currency,

    @JsonSchema.Example("10.50")
    val amount: String,
)
