package altak.ledger.infrastructure.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Validation
import jakarta.validation.ValidatorFactory
import jakarta.validation.constraints.PastOrPresent
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import kotlinx.datetime.toKotlinLocalDate
import org.hibernate.validator.HibernateValidator
import org.hibernate.validator.HibernateValidatorConfiguration
import kotlin.time.Clock
import kotlin.time.toJavaInstant
import java.time.LocalDate as JavaLocalDate

/**
 * Hibernate ships validators for the java.time types only, and dates temporal constraints from the
 * virtual machine's clock. Either would put validation on a different calendar from the books, so it
 * is taught [LocalDate] and handed the same clock and zone `LedgerCalendar` keeps.
 */
fun validatorFactory(clock: Clock, zone: TimeZone): ValidatorFactory =
    Validation.byProvider(HibernateValidator::class.java)
        .configure()
        .clockProvider { java.time.Clock.fixed(clock.now().toJavaInstant(), zone.toJavaZoneId()) }
        .teach(PastOrPresent::class.java, NotAfterToday::class.java)
        .buildValidatorFactory()

private fun <A : Annotation, V : ConstraintValidator<A, *>> HibernateValidatorConfiguration.teach(
    constraint: Class<A>,
    validator: Class<V>,
): HibernateValidatorConfiguration {
    val mapping = createConstraintMapping()

    mapping.constraintDefinition(constraint)
        .includeExistingValidators(true)
        .validatedBy(validator)

    return addMapping(mapping)
}

class NotAfterToday : ConstraintValidator<PastOrPresent, LocalDate> {

    override fun isValid(value: LocalDate?, context: ConstraintValidatorContext) =
        value == null || value <= JavaLocalDate.now(context.clockProvider.clock).toKotlinLocalDate()
}
