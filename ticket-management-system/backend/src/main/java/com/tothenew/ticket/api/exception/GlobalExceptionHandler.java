package com.tothenew.ticket.api.exception;

import com.tothenew.ticket.api.dto.ValidationErrorDetail;
import com.tothenew.ticket.application.InvalidAssigneeException;
import com.tothenew.ticket.application.TicketNotFoundException;
import com.tothenew.ticket.application.TicketTerminalException;
import com.tothenew.ticket.domain.InvalidStateTransitionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI VALIDATION_TYPE = URI.create("https://api.example.com/problems/validation");
    private static final URI ILLEGAL_TRANSITION_TYPE =
            URI.create("https://api.example.com/problems/ticket-illegal-transition");
    private static final URI NOT_FOUND_TYPE = URI.create("https://api.example.com/problems/ticket-not-found");
    private static final URI TERMINAL_TYPE = URI.create("https://api.example.com/problems/ticket-terminal");

    @ExceptionHandler(InvalidAssigneeException.class)
    public ResponseEntity<ProblemDetail> handleInvalidAssignee(
            InvalidAssigneeException ex, HttpServletRequest request) {
        String detail = "Assignee does not exist: " + ex.getAssigneeId();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(VALIDATION_TYPE);
        problem.setTitle("Constraint violation");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "VALIDATION_ERROR");
        problem.setProperty(
                "errors",
                List.of(
                        new ValidationErrorDetail(
                                "assigneeId", "User not found", "NotFound")));
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ValidationErrorDetail> errors =
                ex.getBindingResult().getFieldErrors().stream().map(this::toValidationError).toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Constraint violation");
        problem.setType(VALIDATION_TYPE);
        problem.setTitle("Constraint violation");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "VALIDATION_ERROR");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex, HttpServletRequest request) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, safeDetail(ex));
        problem.setType(VALIDATION_TYPE);
        problem.setTitle("Constraint violation");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "VALIDATION_ERROR");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ProblemDetail> handleIllegalTransition(
            InvalidStateTransitionException ex, HttpServletRequest request) {
        String detail =
                String.format(
                        "Cannot apply %s to ticket in status %s.",
                        ex.getEvent(), ex.getFromStatus());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        problem.setType(ILLEGAL_TRANSITION_TYPE);
        problem.setTitle("Illegal state transition");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "TICKET_ILLEGAL_TRANSITION");
        problem.setProperty("fromStatus", ex.getFromStatus().name());
        problem.setProperty("event", ex.getEvent().name());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(TicketTerminalException.class)
    public ResponseEntity<ProblemDetail> handleTerminal(
            TicketTerminalException ex, HttpServletRequest request) {
        String detail =
                String.format(
                        "Cannot update ticket %s in terminal status %s.",
                        ex.getTicketId(), ex.getStatus());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        problem.setType(TERMINAL_TYPE);
        problem.setTitle("Ticket is terminal");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "TICKET_TERMINAL");
        problem.setProperty("status", ex.getStatus().name());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            TicketNotFoundException ex, HttpServletRequest request) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Ticket not found");
        problem.setType(NOT_FOUND_TYPE);
        problem.setTitle("Ticket not found");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "TICKET_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    private ValidationErrorDetail toValidationError(FieldError fieldError) {
        String code =
                fieldError.getCode() != null ? fieldError.getCode() : "INVALID";
        return new ValidationErrorDetail(fieldError.getField(), fieldError.getDefaultMessage(), code);
    }

    private static String safeDetail(Exception ex) {
        String message = ex.getMessage();
        return message != null ? message : "Invalid request";
    }
}
