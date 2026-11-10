package io.batchstats.api;

import io.batchstats.domain.BatchNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps domain errors to RFC 9457 problem details. Spring's base class already covers
 * malformed JSON, failed bean validation and bad path variables.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BatchNotFoundException.class)
    ProblemDetail batchNotFound(BatchNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setTitle("Batch not found");
        problem.setProperty("batchId", e.batchId());
        return problem;
    }

    @ExceptionHandler(InvalidNumbersException.class)
    ProblemDetail invalidNumbers(InvalidNumbersException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setTitle("Invalid numbers");
        return problem;
    }
}
