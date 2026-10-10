package dan.edu.user_service.common;

import java.net.URI;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleNotFound(ResourceNotFoundException exception) {
		return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException exception) {
		return problem(HttpStatus.CONFLICT, "Conflict", exception.getMessage());
	}

	@ExceptionHandler(BadCredentialsException.class)
	ProblemDetail handleBadCredentials() {
		return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid credentials or refresh token");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
		String detail = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.distinct()
				.reduce((first, next) -> first + "; " + next)
				.orElse("The request contains invalid fields");
		return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail handleUnreadableRequest() {
		return problem(HttpStatus.BAD_REQUEST, "Invalid request", "The request body is malformed or contains an invalid value");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleConstraintViolation() {
		return problem(HttpStatus.CONFLICT, "Conflict", "The request conflicts with an existing record");
	}

	private ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setType(URI.create("about:blank"));
		return problem;
	}
}
