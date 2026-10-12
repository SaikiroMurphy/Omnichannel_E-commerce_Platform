package dan.edu.order_service.common;

public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
