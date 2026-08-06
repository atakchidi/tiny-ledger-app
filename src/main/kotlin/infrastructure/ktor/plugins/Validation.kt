package altak.infrastructure.ktor.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidation
import io.ktor.server.plugins.requestvalidation.ValidationResult
import jakarta.validation.Validation

fun Application.configureValidation() {
    val validatorFactory = Validation.buildDefaultValidatorFactory()
    monitor.subscribe(ApplicationStopped) { validatorFactory.close() }

    install(RequestValidation) {
        // Matches every received body, so any DTO carrying Jakarta constraints is checked
        // without registering it here.
        validate<Any> { body ->
            val violations = validatorFactory.validator.validate(body)

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
