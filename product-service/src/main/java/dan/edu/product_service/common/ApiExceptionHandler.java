package dan.edu.product_service.common;

import java.net.URI;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleNotFound(ResourceNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
	}

	@ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
	ProblemDetail handleConflict(Exception exception) {
		String detail = exception instanceof ConflictException
				? exception.getMessage()
				: "A product with the same SKU already exists";
		return problem(HttpStatus.CONFLICT, "Conflict", detail);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
		String detail = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.distinct()
				.collect(Collectors.joining("; "));
		return problem(HttpStatus.BAD_REQUEST, "Validation failed",
				detail.isBlank() ? "The request contains invalid fields" : detail);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableRequest() {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request",
				"The request body is malformed or contains an invalid value");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setType(URI.create("about:blank"));
		return problem;
	}
}
