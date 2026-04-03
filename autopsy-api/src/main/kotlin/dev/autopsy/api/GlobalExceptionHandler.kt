package dev.autopsy.api

import com.stripe.exception.StripeException
import dev.autopsy.auth.GitHubAuthException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Bad request")
    }

    @ExceptionHandler(GitHubAuthException::class)
    fun handleAuthError(ex: GitHubAuthException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.message ?: "Authentication failed")
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found")
    }

    @ExceptionHandler(RepoLimitExceededException::class)
    fun handleRepoLimit(ex: RepoLimitExceededException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.message ?: "Repository limit exceeded")
    }

    @ExceptionHandler(StripeException::class)
    fun handleStripeError(ex: StripeException): ProblemDetail {
        logger.error("Stripe error: {}", ex.message)
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Payment service error")
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ProblemDetail {
        logger.error("Unexpected error", ex)
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred")
    }
}

class RepoLimitExceededException(message: String) : RuntimeException(message)
