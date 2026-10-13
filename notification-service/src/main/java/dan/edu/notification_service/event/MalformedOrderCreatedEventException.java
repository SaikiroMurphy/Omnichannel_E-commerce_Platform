package dan.edu.notification_service.event;

public class MalformedOrderCreatedEventException extends RuntimeException {

	public MalformedOrderCreatedEventException(String message, Throwable cause) {
		super(message, cause);
	}
}