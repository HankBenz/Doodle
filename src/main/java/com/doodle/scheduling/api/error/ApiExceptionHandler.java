package com.doodle.scheduling.api.error;

import com.doodle.scheduling.application.SchedulingMetrics;
import com.doodle.scheduling.application.error.ErrorCode;
import com.doodle.scheduling.application.error.SchedulingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final SchedulingMetrics metrics;

    public ApiExceptionHandler(SchedulingMetrics metrics) {
        this.metrics = metrics;
    }

    @ExceptionHandler(SchedulingException.class)
    ProblemDetail handleScheduling(SchedulingException ex, HttpServletRequest request) {
        return problem(HttpStatus.valueOf(ex.code().status()), ex.code().name(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleInvalid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (detail.isBlank()) {
            detail = "Request is invalid.";
        }
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST.name(), detail, request);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            ConstraintViolationException.class,
            HandlerMethodValidationException.class
    })
    ProblemDetail handleBadRequest(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST.name(), "Request is invalid.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = ex.getMostSpecificCause().getMessage();
        String text = message == null ? "" : message;
        log.warn("Data integrity violation: {}", text);
        if (text.contains("users_email_unique")) {
            return problem(
                    HttpStatus.CONFLICT,
                    ErrorCode.EMAIL_TAKEN.name(),
                    "A user with this email already exists.",
                    request);
        }
        if (text.contains("time_slots_no_overlap") || text.contains("exclusion constraint")) {
            metrics.slotOverlap();
            return problem(
                    HttpStatus.CONFLICT,
                    ErrorCode.SLOT_OVERLAP.name(),
                    "The time overlaps an existing slot.",
                    request);
        }
        return problem(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with existing data.", request);
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, QueryTimeoutException.class})
    ProblemDetail handleLock(RuntimeException ex, HttpServletRequest request) {
        log.warn("Schedule lock was not acquired: {}", ex.getMessage());
        return problem(
                HttpStatus.CONFLICT,
                ErrorCode.LOCK_TIMEOUT.name(),
                "The schedule is busy. Retry the request.",
                request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimistic(ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                ErrorCode.VERSION_CONFLICT.name(),
                "The slot was updated concurrently. Retry the request.",
                request);
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
