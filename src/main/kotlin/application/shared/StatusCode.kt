package altak.ledger.application.shared

/**
 * The answer an exception deserves at the API. Anything unannotated is a failure of this code, and
 * answers 500 without saying more.
 *
 * The code is an [Int] because Ktor's `HttpStatusCode` is a data class, and an annotation argument
 * has to be a compile-time constant — primitives, strings, enums and classes only.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@java.lang.annotation.Inherited
annotation class StatusCode(val value: Int)
