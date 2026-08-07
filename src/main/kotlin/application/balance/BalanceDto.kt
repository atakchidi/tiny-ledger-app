package altak.ledger.application.balance

import altak.ledger.application.shared.BigDecimalSerializer
import altak.ledger.application.shared.CurrencySerializer
import altak.ledger.application.shared.InstantSerializer
import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.Currency
import kotlin.time.Instant

@Serializable
data class BalanceQueryDto(
    @JsonSchema.Description("An account id or the reference it is known by outside; every account if left out")
    @JsonSchema.Example("\"ACC-000123\"")
    val account: String? = null,

    @Serializable(with = InstantSerializer::class)
    @JsonSchema.Description("The moment to read the journal at, ISO-8601; now if left out")
    @JsonSchema.Example("\"2026-08-01T00:00:00Z\"")
    val onDate: Instant? = null,
)

@Serializable
data class ViewBalanceDto(
    @JsonSchema.Description("The moment the journal was read at")
    @JsonSchema.Example("\"2026-08-07T09:20:06.171504Z\"")
    val onDate: Instant,

    @JsonSchema.Description("The id of the account the balance belongs to")
    @JsonSchema.Example("\"019fdb85-c939-7780-9548-55fe6716fede\"")
    val accountId: String,

    @JsonSchema.Description("The reference that account is known by outside")
    @JsonSchema.Example("\"ACC-000123\"")
    val reference: String,

    @Serializable(with = CurrencySerializer::class)
    @JsonSchema.Description("The currency the balance is in, as an ISO 4217 code")
    @JsonSchema.Example("\"EUR\"")
    val currency: Currency,

    @Serializable(with = BigDecimalSerializer::class)
    @JsonSchema.Description("What the journal adds up to for this account, in the currency's own precision")
    @JsonSchema.Example("74.50")
    val amount: BigDecimal,
)
