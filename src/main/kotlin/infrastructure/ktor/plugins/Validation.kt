package altak.ledger.infrastructure.ktor.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory

fun Application.configureValidation() {
    val validatorFactory: ValidatorFactory by dependencies
    val validator: Validator by dependencies

    monitor.subscribe(ApplicationStopped) { validatorFactory.close() }

    install(RequestValidation) {
        // Matches every received body, so a DTO carrying Jakarta constraints needs no registering here.
        validate<Any> { body ->
            val violations = validator.validate(body)

            when {
                violations.isEmpty() -> ValidationResult.Valid
                // Hibernate returns violations in an unspecified order.
                else -> ValidationResult.Invalid(
                    violations.map { "${it.propertyPath}: ${it.message}" }.sorted()
                )
            }
        }
    }
}
